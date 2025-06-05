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
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import vector_tile.VectorTile
import kotlin.time.measureTime
import android.os.Debug
import org.scottishtecharmy.soundscape.dto.BoundingBox
import org.scottishtecharmy.soundscape.geoengine.MAX_ZOOM_LEVEL
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.geoengine.utils.findShortestDistance
import org.scottishtecharmy.soundscape.geoengine.utils.getLatLonTileWithOffset
import org.scottishtecharmy.soundscape.geoengine.utils.getXYTile

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
        val ruler = CheapRuler(55.941861)
        val distanceResults = Iterables.toList(tree.getNearbyCollection(LngLatAlt(-4.316914, 55.941861), 10.0, ruler))
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
        val startWay = roadTree.getNearestFeature(startLocation, gridState.ruler) as Way
        val endWay = roadTree.getNearestFeature(endLocation, gridState.ruler) as Way

        Debug.startMethodTracing("Test2")

        var shortestPath: Double
        val measureTime = measureTime {
            val result = findShortestDistance(
                startLocation,
                startWay,
                endLocation,
                endWay,
                null,
                null,
                200.0
            )
            shortestPath = result.distance
        }

        Debug.stopMethodTracing()

        println("shortestPath2: $shortestPath, $measureTime")
    }


    private fun testGridCache(boundingBox: BoundingBox, count: Int = 1 ) {

        // This test 'moves' from the center of one tile to the center of the next to check that
        // we can parse the contents of the bounding box
        val gridState = ProtomapsGridState()
        val directory = InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null)
        println(directory)
        gridState.validateContext = false
        gridState.start(ApplicationProvider.getApplicationContext())

        val (minX, minY) = getXYTile(LngLatAlt(boundingBox.westLongitude, boundingBox.northLatitude), MAX_ZOOM_LEVEL)
        val (maxX, maxY) = getXYTile(LngLatAlt(boundingBox.eastLongitude, boundingBox.southLatitude), MAX_ZOOM_LEVEL)

        for(i in 0 until count) {
            for (x in minX.toInt() until maxX.toInt()) {
                for (y in minY.toInt() until maxY.toInt()) {

                    // Get top left of tile
                    val location = getLatLonTileWithOffset(x, y, MAX_ZOOM_LEVEL, 0.0, 0.0)

                    println("Moving grid to $location")

                    runBlocking {
                        // Update the grid state
                        gridState.locationUpdate(
                            LngLatAlt(location.longitude, location.latitude),
                            emptySet()
                        )
                    }
                }
            }
            gridState.stop()
        }
    }
    @Test
    fun testSingleGridCache() {

        println("Start test")

        Thread.sleep(10000)

        val gridState = ProtomapsGridState()
        gridState.validateContext = false
        gridState.start(ApplicationProvider.getApplicationContext())

//        Debug.startMethodTracing("Memory")

        // The center of each grid
        // Get top left of tile
        val location = getLatLonTileWithOffset(16091, 10210, MAX_ZOOM_LEVEL, 0.0, 0.0)

        println("Moving grid to $location")

        runBlocking {
            // Update the grid state
            gridState.locationUpdate(
                LngLatAlt(location.longitude, location.latitude),
                emptySet()
            )
        }

//        Debug.stopMethodTracing()
        gridState.stop()

        Thread.sleep(10000)
    }

    @Test
    fun testMapAreas() {
        val newYork = BoundingBox(-74.0231755, 40.7120699, -73.9197845, 40.8303351)
        testGridCache(newYork)
        val yaounde = BoundingBox(11.4402869, 3.7493240, 11.6208422, 3.9353452)
        testGridCache(yaounde)
        val edinburgh = BoundingBox(-3.3568399, 55.9005448, -3.0921694, 55.9919155)
        testGridCache(edinburgh, 1)

        // London results simply take too long as it's a huge area
//        val london = BoundingBox(-0.5111412, 51.3083029, 0.1582387, 51.6369422)
//        testGridCache(london)
    }
}
