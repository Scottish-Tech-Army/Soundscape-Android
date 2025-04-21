package org.scottishtecharmy.soundscape

import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.github.davidmoten.rtree2.Iterables
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.scottishtecharmy.soundscape.geoengine.ProtomapsGridState
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Intersection
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.vectorTileToGeoJson
import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geoengine.utils.dijkstraOnWaysWithLoops
import org.scottishtecharmy.soundscape.geoengine.utils.getPathWays
import org.scottishtecharmy.soundscape.geoengine.utils.getShortestRoute
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import vector_tile.VectorTile
import kotlin.time.measureTime
import android.os.Debug
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.utils.findShortestDistance
import org.scottishtecharmy.soundscape.geoengine.utils.findShortestDistance2

class MvtPerformanceTest {

    private fun vectorTileToGeoJsonFromFile(
        tileX: Int,
        tileY: Int,
        filename: String,
        cropPoints: Boolean = true
    ): FeatureCollection {

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val remoteTile = context.assets.open(filename)
        val tile: VectorTile.Tile = VectorTile.Tile.parseFrom(remoteTile)
        val intersectionMap:  HashMap<LngLatAlt, Intersection> = hashMapOf()

        return vectorTileToGeoJson(tileX, tileY, tile, intersectionMap, cropPoints, 15)
    }

    @Test
    fun testRtree() {

        // Make a large grid to aid analysis
        val featureCollection = FeatureCollection()
        for (x in 15990..15992) {
            for (y in 10212..10213) {
                val geojson = vectorTileToGeoJsonFromFile(x, y, "${x}x${y}.mvt")
                for (feature in geojson) {
                    featureCollection.addFeature(feature)
                }
            }
        }

        // Iterate through all of the features and add them to an Rtree
        var start = System.currentTimeMillis()
        val tree = FeatureTree(featureCollection)
        var end = System.currentTimeMillis()

        // We have all the points in an rtree
        println("Tree size: ${tree.tree!!.size()} - ${end-start}ms")

        start = System.currentTimeMillis()
        val distanceResults = Iterables.toList(tree.getNearbyCollection(LngLatAlt(-4.316914, 55.941861), 10.0))
        end = System.currentTimeMillis()
        println("Search result in ${end-start}ms")
        for(dResult in distanceResults) {
            println(dResult.properties?.get("name"))
        }
    }

    fun downloadAndParseTile(x: Int, y: Int, gridState: ProtomapsGridState) {
        println("Testing tile $x,$y")
        runBlocking {
            val featureCollections = Array(TreeId.MAX_COLLECTION_ID.id) { FeatureCollection() }
            val intersectionMap:  HashMap<LngLatAlt, Intersection> = hashMapOf()
            gridState.updateTile(x, y, featureCollections, intersectionMap)
        }
    }
    @Test
    fun testParsing() {

        val gridState = ProtomapsGridState()
        gridState.start(ApplicationProvider.getApplicationContext())

        // Test Edinburgh, because that's where many of our testers are!
        println("Test Edinburgh")
        for(x in 16090 until 16095) {
            for(y in 10207 until 10212) {
                downloadAndParseTile(x, y, gridState)
            }
        }

        // Test the capital of Cameroon because it's dense
        println("Test Yaoundé")
        for(x in 17430 until 17437) {
            for(y in 16029 until 16034) {
                downloadAndParseTile(x, y, gridState)
            }
        }
    }

    @Test
    fun testRouting() {
        val gridState = ProtomapsGridState()

        val directory = InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null)
        println(directory)
        gridState.validateContext = false
        gridState.start(ApplicationProvider.getApplicationContext())
        val location = LngLatAlt(-4.317357, 55.942527)
        runBlocking {
            gridState.locationUpdate(
                LngLatAlt(location.longitude, location.latitude),
                emptySet()
            )
        }

        val roadTree = gridState.getFeatureTree(TreeId.ROADS_AND_PATHS)
        val startLocation = LngLatAlt(-4.317351, 55.939856)
        val endLocation = LngLatAlt(-4.316699, 55.939225)

        // Find the nearest ways to each location
        val startWay = roadTree.getNearestFeature(startLocation) as Way
        val endWay = roadTree.getNearestFeature(endLocation) as Way

        Debug.startMethodTracing("Test2")

        var shortestPath = 0.0
        val measureTime = measureTime {
            shortestPath = findShortestDistance2(
                startLocation,
                endLocation,
                startWay,
                endWay,
                null,
                200.0
            )
        }

        Debug.stopMethodTracing()

        println("shortestPath2: $shortestPath, $measureTime")
    }
}