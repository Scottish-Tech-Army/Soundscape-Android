package org.scottishtecharmy.soundscape.geoengine

import android.content.Context
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.scottishtecharmy.soundscape.MainActivity.Companion.MOBILITY_KEY
import org.scottishtecharmy.soundscape.MainActivity.Companion.PLACES_AND_LANDMARKS_KEY
import org.scottishtecharmy.soundscape.dto.BoundingBox
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Intersection
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.IntersectionType
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayEnd
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayType
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geoengine.utils.TileGrid
import org.scottishtecharmy.soundscape.geoengine.utils.TileGrid.Companion.getTileGrid
import org.scottishtecharmy.soundscape.geoengine.utils.getLatLonTileWithOffset
import org.scottishtecharmy.soundscape.geoengine.utils.getPoiFeatureCollectionBySuperCategory
import org.scottishtecharmy.soundscape.geoengine.utils.pointIsWithinBoundingBox
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.Ruler
import org.scottishtecharmy.soundscape.geoengine.utils.traverseIntersectionsConfectingNames
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.network.TileClient
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
    TRANSIT_STOPS(6),
    INTERPOLATIONS(7),
    INFORMATION_POIS(8),
    OBJECT_POIS(9),
    PLACE_POIS(10),
    LANDMARK_POIS(11),
    MOBILITY_POIS(12),
    SAFETY_POIS(13),
    PLACES_AND_LANDMARKS(14),
    SELECTED_SUPER_CATEGORIES(15),
    SETTLEMENTS(16),
    SETTLEMENT_AREAS(17),
    MAX_COLLECTION_ID(18),
}

open class GridState(
    val zoomLevel: Int = MAX_ZOOM_LEVEL,
    val gridSize: Int = GRID_SIZE) {

    // HTTP connection to tile server
    internal lateinit var tileClient: TileClient

    private var centralBoundingBox = BoundingBox()
    private var totalBoundingBox = BoundingBox()
    internal var ruler = CheapRuler(0.0)
    internal var featureTrees = Array(TreeId.MAX_COLLECTION_ID.id) { FeatureTree(null) }
    internal var gridIntersections: HashMap<LngLatAlt, Intersection> = HashMap<LngLatAlt, Intersection>()

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    val treeContext = newSingleThreadContext("TreeContext")
    var validateContext = true

    // This doesn't naturally belong in GridState, but it's where all the other geo info is. It's
    // a tree of Markers from the database.
    internal var markerTree : FeatureTree? = null

    open fun start(applicationContext: Context) {}
    fun stop() {
        // Clean up tile cache and feature trees
        clearTileCache()
        for(tree in featureTrees) {
            tree.tree = null
        }
        centralBoundingBox = BoundingBox()
    }
    open fun fixupCollections(featureCollections: Array<FeatureCollection>) {}

    fun isLocationWithinGrid(location: LngLatAlt): Boolean {
        return pointIsWithinBoundingBox(location, totalBoundingBox)
    }

    /** clearTileConnectionsFromGrid removes all joining ways from the grid.
     *
     */
    fun clearTileConnectionsFromGrid() {
        // Find all ways with type WayType.JOINER, remove them from their intersections at either
        // end and remove their intersection references. The result should be that they have no
        // remaining references and can be garbage collected.
        for(intersection in gridIntersections.values) {
            val iterator = intersection.members.listIterator()
            while(iterator.hasNext()) {
                val way = iterator.next()
                if(way.wayType == WayType.JOINER) {

                    // Remove far end too
                    val otherEnd = way.getOtherIntersection(intersection)
                    if(otherEnd != null) {
                        val members = otherEnd.members.listIterator()
                        while(members.hasNext()) {
                            val member = members.next()
                            if(member == way) {
                                members.remove()
                                break
                            }
                        }
                    }

                    way.intersections[WayEnd.START.id] = null
                    way.intersections[WayEnd.END.id] = null
                    iterator.remove()
                }
            }
        }
    }

    /**
     * processGridState is now called from within the single thread that can access the tile grid.
     * This makes it somewhat performance critical. However, by doing this it allows us to
     * disconnect and reconnect the tile grid
     */
    fun processGridState(
        featureCollections: Array<FeatureCollection>,
        enabledCategories: Set<String>,
        newGridIntersections: List<HashMap<LngLatAlt, Intersection>>,
        localTrees: Array<FeatureTree>,
        intersectionAccumulator: HashMap<LngLatAlt, Intersection>,
        grid: TileGrid
    ) {

        fixupCollections(featureCollections)

        classifyPois(featureCollections, enabledCategories)

        // Create rtrees for each feature collection
        for ((index, fc) in featureCollections.withIndex()) {
            localTrees[index] = FeatureTree(fc)
        }

        if(featureCollections[TreeId.ROADS_AND_PATHS.id].features.isNotEmpty()) {
            // We want to join up Ways that cross tile boundaries
            //
            // In our initial parsing we created Intersections at every line ending that is at the
            // tile boundary. Now that we have our grid, we can match up pairs of these intersections
            // in the same way that the InterpolatedPointsJoiner does - searching by distance. We add
            // a Way between each pair which is marked as a 'Tile Joiner'. These Ways can be followed
            // between the tiles, but are otherwise transparent. We should make it easy to dispose of
            // them when a new grid is made so that we can reuse the Tiles themselves for the new grid.

            // Make list of intersections to join
            if (grid.tiles.size > 1) {
                assert(grid.tiles.size == 4)

                // Center of grid is bottom right of first tile
                val gridCenter = getLatLonTileWithOffset(
                    grid.tiles[0].tileX,
                    grid.tiles[0].tileY,
                    zoomLevel,
                    1.0, 1.0
                )

                val tileEdgeList = emptyList<Intersection>().toMutableList()
                for (intersectionList in newGridIntersections) {
                    for (intersection in intersectionList) {
                        if (intersection.value.intersectionType == IntersectionType.TILE_EDGE) {
                            // We have an edge - check if it's an internal edge to the grid
                            if (
                                (intersection.value.location.longitude == gridCenter.longitude) or
                                (intersection.value.location.latitude == gridCenter.latitude)
                            ) {
                                // This intersection needs joining, so put add it to our list
                                tileEdgeList.add(intersection.value)
                            }
                        }
                    }
                }

                // We have our list of intersections to join, so join them
                for (intersection1 in tileEdgeList) {
                    for (intersection2 in tileEdgeList) {
                        // Don't join to ourselves
                        if (intersection1 != intersection2) {
                            // Don't join if already joined
                            if (intersection1.members.size < 2) {
                                // Join if within 1.0m
                                val distance =
                                    ruler.distance(intersection1.location, intersection2.location)
                                if (distance < 1.0) {
                                    // Join the intersections together
                                    val way = Way()
                                    way.geometry =
                                        LineString(intersection1.location, intersection2.location)
                                    way.wayType = WayType.JOINER
                                    way.intersections[WayEnd.START.id] = intersection1
                                    way.intersections[WayEnd.END.id] = intersection2
                                    intersection1.members.add(way)
                                    intersection2.members.add(way)
                                    break
                                }
                            }
                        }
                    }
                }
            }

            //
            // Confect names for un-named ways
            //
            // Start by traversing the way graph which is efficient and adds cut-through and
            // dead-end modifiers to the ways
            for (hashmap in newGridIntersections) {
                traverseIntersectionsConfectingNames(hashmap, intersectionAccumulator)
            }

// The other confection is done lazily as it's relatively time consuming to do the whole tile at once
//          //And then fill in any remaining names using rtree searches - this is slower and
//            // adds POIs and co-linear names
//            for (road in featureCollections[TreeId.ROADS_AND_PATHS.id]) {
//                confectNamesForRoad(road, featureTrees)
//          }
        }
    }

    /**
     * The tile grid service is called each time the location changes. It checks if the location
     * has moved away from the center of the current tile grid and if it has calculates a new grid.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun locationUpdate(location: LngLatAlt, enabledCategories: Set<String>) : Boolean {
        // Check if we're still within the central area of our grid
        if (!pointIsWithinBoundingBox(location, centralBoundingBox)) {
            //println("Update central grid area")
            // The current location has moved from within the central area, so get the
            // new grid and the new central area.
            val tileGrid = getTileGrid(location, zoomLevel, gridSize)

            // We have a new centralBoundingBox, so update the tiles
            val featureCollections = Array(TreeId.MAX_COLLECTION_ID.id) { FeatureCollection() }
            val newGridIntersections: MutableList<HashMap<LngLatAlt, Intersection>> =
                emptyList<HashMap<LngLatAlt, Intersection>>().toMutableList()
            if (updateTileGrid(tileGrid, featureCollections, newGridIntersections)) {
                // We have got a new grid, so create our new central region
                centralBoundingBox = tileGrid.centralBoundingBox
                totalBoundingBox = tileGrid.totalBoundingBox

                // Assign rtrees to our shared trees from within the treeContext. All
                // other accesses of featureTrees needs to be from within the same
                // context.
                runBlocking {
                    withContext(treeContext) {
                        val timeSource = TimeSource.Monotonic
                        val gridStartTime = timeSource.markNow()

                        ruler = CheapRuler(location.latitude)
                        clearTileConnectionsFromGrid()

                        processGridState(
                            featureCollections,
                            enabledCategories,
                            newGridIntersections,
                            featureTrees,
                            gridIntersections,
                            tileGrid
                        )

                        println("Time to process grid: ${timeSource.markNow() - gridStartTime}")
                    }
                }
                return true
            } else {
                // Updating the tile grid failed, due to a lack of cached tile and then
                // a lack of network/server issue. There's nothing that we can do, so
                // simply retry on the next location update.
                return false
            }
        }
        return false
    }

    // We keep a small cache of the FeatureCollections for the most recently used tiles. The main
    // aim of this is to re-use tiles which are shared between the old and new 2x2 grid. There is
    // almost always at least 1 tile shared, and often 2.
    val maxCachedTiles = 10
    data class CachedTile(
        var tileCollections: Array<FeatureCollection>,
        var intersectionMap: HashMap<LngLatAlt, Intersection> = hashMapOf(),
        var lastUsed: Long)
    val cachedTiles: HashMap<Pair<Int,Int>, CachedTile> = HashMap()

    fun clearTileCache() {
        for(tile in cachedTiles) {
            clearTile(tile.value)
        }
        cachedTiles.clear()
    }
    fun clearCachedTile(key: Pair<Int, Int>) {
        val data = cachedTiles.remove(key)!!
        clearTile(data)
    }

    fun clearTile(tile: CachedTile) {
        for(fc in tile.tileCollections) {
            fc.features.clear()
        }
        tile.tileCollections = emptyArray()

        // Remove intersection refs in every Way that makes up the
        // intersection (up to two)
        for(intersection in tile.intersectionMap.values) {
            // Remove all Way end references
            for(member in intersection.members) {
                member.intersections[WayEnd.START.id] = null
                member.intersections[WayEnd.END.id] = null
            }
            // Remove all Ways from this intersection
            intersection.members.clear()
        }
        tile.intersectionMap.clear()
    }

    private suspend fun updateTileGrid(
        tileGrid: TileGrid,
        featureCollections: Array<FeatureCollection>,
        gridIntersections: MutableList<HashMap<LngLatAlt, Intersection>>
    ): Boolean {
        for (tile in tileGrid.tiles) {

            var tileCollections: Array<FeatureCollection>?
            var intersectionMap: HashMap<LngLatAlt, Intersection> = hashMapOf()
            val key = Pair(tile.tileX, tile.tileY)
            if(cachedTiles.contains(key)) {
                val cachedTile = cachedTiles[key]!!
                tileCollections = cachedTile.tileCollections
                intersectionMap = cachedTile.intersectionMap
                cachedTile.lastUsed = System.currentTimeMillis()
                //println("Using cached value for ${tile.tileX},${tile.tileY}")
            } else {
                var ret = false
                tileCollections = Array(TreeId.MAX_COLLECTION_ID.id) { FeatureCollection() }
                for (retry in 1..5) {
                    ret = updateTile(tile.tileX, tile.tileY, tileCollections, intersectionMap)
                    if (ret) {
                        // Add new tile to the cache
                        cachedTiles[key] = CachedTile(
                            tileCollections,
                            intersectionMap,
                            System.currentTimeMillis()
                        )
                        //println("Adding ${tile.tileX},${tile.tileY} to cache")

                        if(cachedTiles.size > maxCachedTiles) {
                            // Remove the least recently used tile
                            var leastRecentlyUsed = Long.MAX_VALUE
                            var leastRecentlyUsedKey = Pair(0, 0)
                            for (cachedTile in cachedTiles) {
                                if (cachedTile.value.lastUsed < leastRecentlyUsed) {
                                    leastRecentlyUsed = cachedTile.value.lastUsed
                                    leastRecentlyUsedKey = cachedTile.key
                                }
                            }
                            if (leastRecentlyUsedKey != Pair(0, 0)) {
                                //println("Removing ${leastRecentlyUsedKey.first},${leastRecentlyUsedKey.second} from cache")
                                clearCachedTile(leastRecentlyUsedKey)
                            }
                            assert(cachedTiles.size <= maxCachedTiles)
                        }
                        break
                    }
                }
                if (!ret) {
                    return false
                }
            }
            // Add the tile FeatureCollections into the grid
            for ((index, collection) in tileCollections.withIndex()) {
                featureCollections[index].plusAssign(collection)
            }
            gridIntersections.add(intersectionMap)
        }
        return true
    }

    internal open suspend fun updateTile(x: Int,
                                         y: Int,
                                         featureCollections: Array<FeatureCollection>,
                                         intersectionMap: HashMap<LngLatAlt, Intersection>): Boolean {
        assert(false)
        return false
    }

    internal fun classifyPois(featureCollections: Array<FeatureCollection>,
                             enabledCategories: Set<String> = emptySet()) {
        // The FeatureCollection for POIS has been created, but we need to create sub-collections
        // for each of the super-categories along with one for the currently selected super-
        // categories.
        val superCategories = listOf(
            "information",
            "object",
            "place",
            "landmark",
            "mobility",
            "safety",
            "settlement",
            "settlement_area")
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

        // Settlement amd their area names
        category = superCategoryCollections["settlement"]
        featureCollections[TreeId.SETTLEMENTS.id] = category ?: FeatureCollection()
        category = superCategoryCollections["settlement_area"]
        featureCollections[TreeId.SETTLEMENT_AREAS.id] = category ?: FeatureCollection()

        // Create a merged collection of places and landmarks, as used by whatsAroundMe and aheadOfMe
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
            featureTrees[id.id].getAllCollection()
        } else {
            val ruler = CheapRuler(location.latitude)
            if(maxCount == 0) {
                featureTrees[id.id].getNearbyCollection(location, distance, ruler)
            } else {
                if (maxCount == 0) {
                    featureTrees[id.id].getNearbyCollection(location, distance, ruler)
                } else {
                    featureTrees[id.id].getNearestCollection(location, distance, maxCount, ruler)
                }
            }
        }
        return result
    }

    internal fun getNearestFeature(id: TreeId,
                                   ruler: Ruler,
                                   location: LngLatAlt = LngLatAlt(),
                                   distance : Double = Double.POSITIVE_INFINITY
    ): Feature? {
        checkContext()
        return featureTrees[id.id].getNearestFeature(location, ruler, distance)
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
        tileData[TreeId.TRANSIT_STOPS.id] = getTransitStopsFeatureCollectionFromTileFeatureCollection(tileFeatureCollection)
        tileData[TreeId.CROSSINGS.id] = getCrossingsFromTileFeatureCollection(tileFeatureCollection)
        tileData[TreeId.INTERPOLATIONS.id] = getInterpolationPointsFromTileFeatureCollection(tileFeatureCollection)

        // POIS includes bus stops and crossings
        tileData[TreeId.POIS.id].plusAssign(tileData[TreeId.TRANSIT_STOPS.id])
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
    private fun getTransitStopsFeatureCollectionFromTileFeatureCollection(
        tileFeatureCollection: FeatureCollection
    ): FeatureCollection{
        val transitStopFeatureCollection = FeatureCollection()
        for (feature in tileFeatureCollection) {
            val featureValue = feature.foreign?.get("feature_value")
            when(featureValue) {
                "bus_stop","tram_stop","subway","train_station","ferry_terminal" -> transitStopFeatureCollection.addFeature(feature)
            }
        }
        return transitStopFeatureCollection
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
        // Split out the intersections into their own intersections FeatureCollection
        for (feature in tileFeatureCollection) {
            feature.foreign?.let { foreign ->
                if (foreign["feature_type"] == "highway" && foreign["feature_value"] == "gd_intersection") {
                    val intersection = feature as Intersection
                    if(intersection.intersectionType != IntersectionType.TILE_EDGE) {
                        // Only add intersections that are not tile edges
                        intersectionsFeatureCollection.addFeature(feature)
                    }
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
        internal const val TAG = "GridState"
    }
}