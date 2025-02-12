package org.scottishtecharmy.soundscape.geoengine

import android.app.Application
import android.util.Log
import com.squareup.moshi.Moshi
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.scottishtecharmy.soundscape.MainActivity.Companion.MOBILITY_KEY
import org.scottishtecharmy.soundscape.MainActivity.Companion.PLACES_AND_LANDMARKS_KEY
import org.scottishtecharmy.soundscape.dto.BoundingBox
import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geoengine.utils.TileGrid
import org.scottishtecharmy.soundscape.geoengine.utils.TileGrid.Companion.getTileGrid
import org.scottishtecharmy.soundscape.geoengine.utils.Triangle
import org.scottishtecharmy.soundscape.geoengine.utils.getNearestRoad
import org.scottishtecharmy.soundscape.geoengine.utils.getPoiFeatureCollectionBySuperCategory
import org.scottishtecharmy.soundscape.geoengine.utils.pointIsWithinBoundingBox
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeoMoshi
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.network.TileClient
import org.scottishtecharmy.soundscape.services.SoundscapeService
import kotlin.time.TimeSource

enum class TreeId(
    val id: Int,
) {
    ROADS(0),
    ROADS_AND_PATHS(1),
    INTERSECTIONS(2),
    ENTRANCES(3),
    CROSSINGS(4),
    POIS(5),
    BUS_STOPS(6),
    INTERPOLATIONS(7),
    INFORMATION_POIS(8),
    OBJECT_POIS(9),
    PLACE_POIS(10),
    LANDMARK_POIS(11),
    MOBILITY_POIS(12),
    SAFETY_POIS(13),
    PLACES_AND_LANDMARKS(14),
    SELECTED_SUPER_CATEGORIES(15),
    MAX_COLLECTION_ID(16),
}

open class GridState {

    // HTTP connection to tile server
    internal lateinit var tileClient: TileClient

    private var centralBoundingBox = BoundingBox()
    private var totalBoundingBox = BoundingBox()
    internal var featureTrees = Array(TreeId.MAX_COLLECTION_ID.id) { FeatureTree(null) }
    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    val treeContext = newSingleThreadContext("TreeContext")
    var validateContext = true

    open fun start(application: Application) {}
    fun stop() {}
    open fun fixupCollections(featureCollections: Array<FeatureCollection>) {}

    fun isLocationWithinGrid(location: LngLatAlt): Boolean {
        return pointIsWithinBoundingBox(location, totalBoundingBox)
    }

    /**
     * The tile grid service is called each time the location changes. It checks if the location
     * has moved away from the center of the current tile grid and if it has calculates a new grid.
     */
    suspend fun locationUpdate(location: LngLatAlt, enabledCategories: Set<String>) : Boolean {
        val timeSource = TimeSource.Monotonic
        val gridStartTime = timeSource.markNow()

        // Check if we're still within the central area of our grid
        if (!pointIsWithinBoundingBox(location, centralBoundingBox)) {
            Log.d(TAG, "Update central grid area")
            // The current location has moved from within the central area, so get the
            // new grid and the new central area.
            val tileGrid = getTileGrid(location)

            // We have a new centralBoundingBox, so update the tiles
            val featureCollections = Array(TreeId.MAX_COLLECTION_ID.id) { FeatureCollection() }
            if (updateTileGrid(tileGrid, featureCollections)) {
                // We have got a new grid, so create our new central region
                centralBoundingBox = tileGrid.centralBoundingBox
                totalBoundingBox = tileGrid.totalBoundingBox

                fixupCollections(featureCollections)
                classifyPois(featureCollections, enabledCategories)

                // Create rtrees for each feature collection
                val localTrees = Array(TreeId.MAX_COLLECTION_ID.id) { FeatureTree(null) }
                for ((index, fc) in featureCollections.withIndex()) {
                    localTrees[index] = FeatureTree(fc)
                }

                // Assign rtrees to our shared trees from within the treeContext. All
                // other accesses of featureTrees needs to be from within the same
                // context.
                runBlocking {
                    withContext(treeContext) {
                        for (fc in featureCollections.withIndex()) {
                            featureTrees[fc.index] = localTrees[fc.index]
                        }
                    }
                }

                val gridFinishTime = timeSource.markNow()
                Log.e(TAG, "Time to populate grid: ${gridFinishTime - gridStartTime}")

                return true
            } else {
                // Updating the tile grid failed, due to a lack of cached tile and then
                // a lack of network/server issue. There's nothing that we can do, so
                // simply retry on the next location update.
                return false
            }
        }
        return true
    }

    private suspend fun updateTileGrid(
        tileGrid: TileGrid,
        featureCollections: Array<FeatureCollection>,
    ): Boolean {
        for (tile in tileGrid.tiles) {
            Log.d(TAG, "Tile quad key: ${tile.quadkey}")
            var ret = false
            for (retry in 1..5) {
                ret = updateTile(tile.tileX, tile.tileY, featureCollections)
                if (ret) {
                    break
                }
            }
            if (!ret) {
                return false
            }
        }
        return true
    }

    internal open suspend fun updateTile(x: Int, y: Int, featureCollections: Array<FeatureCollection>): Boolean {
        assert(false)
        return false
    }

    internal fun classifyPois(featureCollections: Array<FeatureCollection>,
                             enabledCategories: Set<String> = emptySet()) {
        // The FeatureCollection for POIS has been created, but we need to create sub-collections
        // for each of the super-categories along with one for the currently selected super-
        // categories.
        val timeSource = TimeSource.Monotonic
        val gridStartTime = timeSource.markNow()

        val superCategories = listOf("information", "object", "place", "landmark", "mobility", "safety")
        val superCategoryCollections = superCategories.associateWith { superCategory ->
            getPoiFeatureCollectionBySuperCategory(superCategory, featureCollections[TreeId.POIS.id])
        }

        // Create super category feature collections
        var category = superCategoryCollections["information"]
        featureCollections[TreeId.INFORMATION_POIS.id] = category ?: FeatureCollection()
        category = superCategoryCollections["object"]
        featureCollections[TreeId.OBJECT_POIS.id] = category ?: FeatureCollection()
        category = superCategoryCollections["place"]
        featureCollections[TreeId.PLACE_POIS.id] = category ?: FeatureCollection()
        category = superCategoryCollections["landmark"]
        featureCollections[TreeId.LANDMARK_POIS.id] = category ?: FeatureCollection()
        category = superCategoryCollections["mobility"]
        featureCollections[TreeId.MOBILITY_POIS.id] = category ?: FeatureCollection()
        category = superCategoryCollections["safety"]
        featureCollections[TreeId.SAFETY_POIS.id] = category ?: FeatureCollection()

        // Create a merged collection of places and landmarks, as used by whatsAroundMe
        featureCollections[TreeId.PLACES_AND_LANDMARKS.id].plusAssign(featureCollections[TreeId.PLACE_POIS.id])
        featureCollections[TreeId.PLACES_AND_LANDMARKS.id].plusAssign(featureCollections[TreeId.LANDMARK_POIS.id])

        // Create merged collection of currently selected super categories
        if(enabledCategories.contains(PLACES_AND_LANDMARKS_KEY)) {
            featureCollections[TreeId.SELECTED_SUPER_CATEGORIES.id].plusAssign(
                featureCollections[TreeId.PLACE_POIS.id]
            )
            featureCollections[TreeId.SELECTED_SUPER_CATEGORIES.id].plusAssign(
                featureCollections[TreeId.LANDMARK_POIS.id]
            )
        }
        if(enabledCategories.contains(MOBILITY_KEY)) {
            featureCollections[TreeId.SELECTED_SUPER_CATEGORIES.id].plusAssign(
                featureCollections[TreeId.MOBILITY_POIS.id]
            )
        }
        val gridFinishTime = timeSource.markNow()
        println("Time to classify grid: ${gridFinishTime - gridStartTime}")
    }

    // All functions which access the featureTrees need to be running within the treeContext,
    // so assert that this is the case.
    private fun checkContext() {
        // TODO: It feels like there should be a way to check the context, but coroutineContext
        //  is a suspend function and we are not. We can at least check that the current thread
        //  name matches. This should spot any calls using featureTrees which aren't running in
        //  the treeContext.
        if(validateContext) {
            assert(Thread.currentThread().name.startsWith("TreeContext"))
        }
    }

    fun getFeatureTree(id: TreeId): FeatureTree {
        checkContext()
        return featureTrees[id.id]
    }

    internal fun getFeatureCollection(id: TreeId,
                                      location: LngLatAlt = LngLatAlt(),
                                      distance : Double = Double.POSITIVE_INFINITY,
                                      maxCount : Int = 0): FeatureCollection {
        checkContext()
        val result = if(distance == Double.POSITIVE_INFINITY) {
            featureTrees[id.id].generateFeatureCollection()
        } else {
            if(maxCount == 0) {
                featureTrees[id.id].generateNearbyFeatureCollection(location, distance)
            } else {
                if (maxCount == 0) {
                    featureTrees[id.id].generateNearbyFeatureCollection(location, distance)
                } else {
                    featureTrees[id.id].generateNearestFeatureCollection(location, distance, maxCount)
                }
            }
        }
        return result
    }

    internal fun getNearestFeature(id: TreeId,
                                   location: LngLatAlt = LngLatAlt(),
                                   distance : Double = Double.POSITIVE_INFINITY
    ): Feature? {
        checkContext()
        return featureTrees[id.id].getNearestFeature(location, distance)
    }

    data class FeatureByRoad(val feature: Feature,
                             val road: Feature,
                             val distance: Double = Double.POSITIVE_INFINITY)
    fun getNearestFeatureOnRoadInFov(id: TreeId,
                                     triangle: Triangle
    ): FeatureByRoad? {

        checkContext()
        val nearestFeature = featureTrees[id.id].getNearestFeatureWithinTriangle(triangle)
        if (nearestFeature != null) {
            val featureLocation = nearestFeature.geometry as Point

            // Confirm which road the feature is on
            val nearestRoad = getNearestRoad(
                featureLocation.coordinates,
                featureTrees[TreeId.ROADS_AND_PATHS.id]
            )
            if(nearestRoad != null) {
                // We found a feature and the road that it is on
                val distance = triangle.origin.distance(featureLocation.coordinates)
                return FeatureByRoad(nearestFeature, nearestRoad, distance)
            }
        }

        return null
    }

    /**
     * Parses out roads, paths, intersections, entrances, pois, bus stops and crossings from a tile string.
     * @return a TileData object with the string parsed into separate strings.
     */

    internal fun processTileString(tileString: String): Array<FeatureCollection> {
        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val tileFeatureCollection = moshi.adapter(FeatureCollection::class.java)
            .fromJson(tileString)
        if(tileFeatureCollection == null)
            return emptyArray()

        return processTileFeatureCollection(tileFeatureCollection)
    }

    internal fun processTileFeatureCollection(tileFeatureCollection: FeatureCollection): Array<FeatureCollection> {

        val tileData = Array(TreeId.MAX_COLLECTION_ID.id) { FeatureCollection() }

        // We have separate collections for the different types of Feature. ROADS_AND_PATHS adds PATHS
        // to the ROADS features already contained in ROADS. This slight extra cost in terms of memory
        // is made up for by the ease of searching a single collection.
        tileData[TreeId.ROADS.id] = getRoadsFeatureCollectionFromTileFeatureCollection(tileFeatureCollection)
        tileData[TreeId.ROADS_AND_PATHS.id] = getPathsFeatureCollectionFromTileFeatureCollection(tileFeatureCollection)
        tileData[TreeId.ROADS_AND_PATHS.id].plusAssign(tileData[TreeId.ROADS.id])
        tileData[TreeId.INTERSECTIONS.id] = getIntersectionsFeatureCollectionFromTileFeatureCollection(tileFeatureCollection)
        tileData[TreeId.ENTRANCES.id] = getEntrancesFeatureCollectionFromTileFeatureCollection(tileFeatureCollection)
        tileData[TreeId.POIS.id] = getPointsOfInterestFeatureCollectionFromTileFeatureCollection(tileFeatureCollection)
        tileData[TreeId.BUS_STOPS.id] = getBusStopsFeatureCollectionFromTileFeatureCollection(tileFeatureCollection)
        tileData[TreeId.CROSSINGS.id] = getCrossingsFromTileFeatureCollection(tileFeatureCollection)
        tileData[TreeId.INTERPOLATIONS.id] = getInterpolationPointsFromTileFeatureCollection(tileFeatureCollection)

        // POIS includes bus stops and crossings
        tileData[TreeId.POIS.id].plusAssign(tileData[TreeId.BUS_STOPS.id])
        tileData[TreeId.POIS.id].plusAssign(tileData[TreeId.CROSSINGS.id])

        return  tileData
    }

    /**
     * Given a valid Tile feature collection this will parse the collection and return a roads
     * feature collection. Uses the "highway" feature_type to extract roads from GeoJSON.
     * @param tileFeatureCollection
     * A FeatureCollection object.
     * @return A FeatureCollection object that contains only roads.
     */
    private fun getRoadsFeatureCollectionFromTileFeatureCollection(
        tileFeatureCollection: FeatureCollection
    ): FeatureCollection {

        val roadsFeatureCollection = FeatureCollection()

        // Original Soundscape excludes the below feature_value (s) even though they have the
        // feature_type == highway
        // and creates a separate Paths Feature Collection for them
        // "footway", "path", "cycleway", "bridleway"
        // gd_intersection are a special case and get their own Intersections Feature Collection


        for (feature in tileFeatureCollection) {
            feature.foreign?.let { foreign ->
                if (foreign["feature_type"] == "highway"
                    && foreign["feature_value"] != "gd_intersection"
                    && foreign["feature_value"] != "footway"
                    && foreign["feature_value"] != "path"
                    && foreign["feature_value"] != "cycleway"
                    && foreign["feature_value"] != "bridleway"
                    && foreign["feature_value"] != "bus_stop"
                    && foreign["feature_value"] != "crossing") {
                    // We're only going to add linestrings to the roads feature collection
                    when(feature.geometry.type) {
                        "LineString", "MultiLineString" ->
                            roadsFeatureCollection.addFeature(feature)
                    }
                }
            }
        }
        return roadsFeatureCollection
    }

    /**
     * Given a valid Tile feature collection this will parse the collection and return a bus stops
     * feature collection. Uses the "bus_stop" feature_value to extract bus stops from GeoJSON.
     * @param tileFeatureCollection
     * A FeatureCollection object.
     * @return A FeatureCollection object that contains only bus stops.
     */
    private fun getBusStopsFeatureCollectionFromTileFeatureCollection(
        tileFeatureCollection: FeatureCollection
    ): FeatureCollection{
        val busStopFeatureCollection = FeatureCollection()
        for (feature in tileFeatureCollection) {
            feature.foreign?.let { foreign ->
                if (foreign["feature_type"] == "highway" && foreign["feature_value"] == "bus_stop") {
                    busStopFeatureCollection.addFeature(feature)
                }
            }
        }

        return busStopFeatureCollection
    }

    /**
     * Given a valid Tile feature collection this will parse the collection and return a crossing
     * feature collection. Uses the "crossing" feature_value to extract crossings from GeoJSON.
     * @param tileFeatureCollection
     * A FeatureCollection object.
     * @return A FeatureCollection object that contains only crossings.
     */
    private fun getCrossingsFromTileFeatureCollection(tileFeatureCollection: FeatureCollection): FeatureCollection{
        val crossingsFeatureCollection = FeatureCollection()
        for (feature in tileFeatureCollection) {
            feature.foreign?.let { foreign ->
                if (foreign["feature_type"] == "highway" && foreign["feature_value"] == "crossing") {
                    crossingsFeatureCollection.addFeature(feature)
                }
            }
        }
        return crossingsFeatureCollection
    }

    /**
     * Given a valid Tile feature collection this will parse the collection and return an interpolation
     * points feature collection. Uses the "edgePoint" feature_value to extract crossings from GeoJSON.
     * @param tileFeatureCollection
     * A FeatureCollection object.
     * @return A FeatureCollection object that contains only edgePoints
     */
    private fun getInterpolationPointsFromTileFeatureCollection(tileFeatureCollection: FeatureCollection): FeatureCollection{
        val interpolationPointsFeatureCollection = FeatureCollection()
        for (feature in tileFeatureCollection) {
            feature.properties?.let { properties ->
                if (properties["class"] == "edgePoint") {
                    interpolationPointsFeatureCollection.addFeature(feature)
                }
            }
        }
        return interpolationPointsFeatureCollection
    }

    /**
     * Given a valid Tile feature collection this will parse the collection and return a paths
     * feature collection. Uses the "footway", "path", "cycleway", "bridleway" feature_value to extract
     * Paths from Feature Collection.
     * @param tileFeatureCollection
     * A FeatureCollection object.
     * @return A FeatureCollection object that contains only paths.
     */
    private fun getPathsFeatureCollectionFromTileFeatureCollection(
        tileFeatureCollection: FeatureCollection
    ): FeatureCollection{
        val pathsFeatureCollection = FeatureCollection()

        for(feature in tileFeatureCollection) {
            feature.foreign?.let { foreign ->
                // We're only going to add linestrings to the roads feature collection
                when(feature.geometry.type) {
                    "LineString", "MultiLineString" -> {
                        if (foreign["feature_type"] == "highway")
                            when (foreign["feature_value"]) {
                                "footway" -> pathsFeatureCollection.addFeature(feature)
                                "path" -> pathsFeatureCollection.addFeature(feature)
                                "cycleway" -> pathsFeatureCollection.addFeature(feature)
                                "bridleway" -> pathsFeatureCollection.addFeature(feature)
                            }
                    }
                }
            }
        }
        return pathsFeatureCollection
    }

    /**
     * Parses out all the Intersections in a tile FeatureCollection using the "gd_intersection" feature_value.
     * @param tileFeatureCollection
     * A FeatureCollection object.
     * @return a Feature collection object that only contains intersections.
     */
    private fun getIntersectionsFeatureCollectionFromTileFeatureCollection(
        tileFeatureCollection: FeatureCollection
    ): FeatureCollection {
        val intersectionsFeatureCollection = FeatureCollection()
        // split out the intersections into their own intersections FeatureCollection
        for (feature in tileFeatureCollection) {
            feature.foreign?.let { foreign ->
                if (foreign["feature_type"] == "highway" && foreign["feature_value"] == "gd_intersection") {
                    intersectionsFeatureCollection.addFeature(feature)
                }
            }
        }
        return intersectionsFeatureCollection
    }

    /**
     * Parses out all the Entrances in a tile FeatureCollection using the "gd_entrance_list" feature_type.
     * @param tileFeatureCollection
     * A FeatureCollection object.
     * @return a feature collection object that contains only entrances.
     */
    private fun getEntrancesFeatureCollectionFromTileFeatureCollection(
        tileFeatureCollection: FeatureCollection
    ): FeatureCollection {
        val entrancesFeatureCollection = FeatureCollection()
        for (feature in tileFeatureCollection) {
            feature.foreign?.let { foreign ->
                if (foreign["feature_type"] == "entrance") {
                    entrancesFeatureCollection.addFeature(feature)
                }
            }
        }
        return entrancesFeatureCollection
    }

    /**
     * Parses out all the Points of Interest (POI) in a tile FeatureCollection.
     * @param tileFeatureCollection
     * A FeatureCollection object.
     * @return a Feature collection object that contains only POI.
     */
    private fun getPointsOfInterestFeatureCollectionFromTileFeatureCollection(
        tileFeatureCollection: FeatureCollection
    ): FeatureCollection {
        val poiFeaturesCollection = FeatureCollection()
        for (feature in tileFeatureCollection) {
            var add = true
            feature.foreign?.let { foreign ->
                if (foreign["feature_type"] == "highway" ||
                    foreign["feature_type"] == "gd_entrance_list"
                ) {
                    add = false
                }
            }
            feature.properties?.let { properties ->
                if (properties["class"] == "edgePoint") {
                    add = false
                }
            }
            if (add) poiFeaturesCollection.addFeature(feature)
        }

        return poiFeaturesCollection
    }

    companion object {
        fun createFromFeatureCollection(featureCollection: FeatureCollection) : GridState {

            val gridState = ProtomapsGridState()

            val collections = gridState.processTileFeatureCollection(featureCollection)
            gridState.fixupCollections(collections)
            gridState.classifyPois(collections)
            for ((index, collection) in collections.withIndex()) {
                gridState.featureTrees[index] = FeatureTree(collection)
            }
            // Because the gridState is static and is not being updated by another thread, we don't
            // need to run it in a separate context, so disable checking.
            gridState.validateContext = false

            return gridState
        }

        fun createFromGeoJson(geoJson: String) : GridState {

            val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
            val featureCollection = moshi.adapter(FeatureCollection::class.java)
                .fromJson(geoJson)

            return createFromFeatureCollection(featureCollection!!)
        }

        internal const val TAG = "GridState"
    }
}