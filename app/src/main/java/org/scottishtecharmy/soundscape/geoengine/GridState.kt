package org.scottishtecharmy.soundscape.geoengine

import android.app.Application
import android.util.Log
import com.squareup.moshi.Moshi
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.scottishtecharmy.soundscape.dto.BoundingBox
import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geoengine.utils.TileGrid
import org.scottishtecharmy.soundscape.geoengine.utils.TileGrid.Companion.getTileGrid
import org.scottishtecharmy.soundscape.geoengine.utils.getNearestRoad
import org.scottishtecharmy.soundscape.geoengine.utils.pointIsWithinBoundingBox
import org.scottishtecharmy.soundscape.geoengine.utils.processTileFeatureCollection
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
    MAX_COLLECTION_ID(8),
}

open class GridState {

    // HTTP connection to tile server
    internal lateinit var tileClient: TileClient

    private var centralBoundingBox = BoundingBox()
    private var featureTrees = Array(TreeId.MAX_COLLECTION_ID.id) { FeatureTree(null) }
    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    val treeContext = newSingleThreadContext("TreeContext")
    var validateContext = true

    open fun start(application: Application, soundscapeService: SoundscapeService) {}
    fun stop() {}
    open fun fixupCollectionAndCreateTrees(trees: Array<FeatureTree>,
                                           featureCollections: Array<FeatureCollection>) {}

    /**
     * The tile grid service is called each time the location changes. It checks if the location
     * has moved away from the center of the current tile grid and if it has calculates a new grid.
     */
    suspend fun locationUpdate(location: LngLatAlt) {
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

                val localTrees = Array(TreeId.MAX_COLLECTION_ID.id) { FeatureTree(null) }
                fixupCollectionAndCreateTrees(localTrees, featureCollections)

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

            } else {
                // Updating the tile grid failed, due to a lack of cached tile and then
                // a lack of network/server issue. There's nothing that we can do, so
                // simply retry on the next location update.
            }
        }
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
                                     location: LngLatAlt,
                                     left: LngLatAlt,
                                     right: LngLatAlt
    ): FeatureByRoad? {

        checkContext()
        val nearestFeature = featureTrees[id.id].getNearestFeatureWithinTriangle(
            location,
            left,
            right)
        if (nearestFeature != null) {
            val featureLocation = nearestFeature.geometry as Point

            // Confirm which road the feature is on
            val nearestRoad = getNearestRoad(
                featureLocation.coordinates,
                featureTrees[TreeId.ROADS_AND_PATHS.id]
            )
            if(nearestRoad != null) {
                // We found a feature and the road that it is on
                val distance = location.distance(featureLocation.coordinates)
                return FeatureByRoad(nearestFeature, nearestRoad, distance)
            }
        }

        return null
    }

    companion object {
        fun createFromFeatureCollection(featureCollection: FeatureCollection) : GridState {

            val gridState = GridState()

            val collections = processTileFeatureCollection(featureCollection)
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