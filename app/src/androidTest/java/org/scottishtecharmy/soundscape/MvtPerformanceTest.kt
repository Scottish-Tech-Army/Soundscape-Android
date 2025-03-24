package org.scottishtecharmy.soundscape

import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.github.davidmoten.rtree2.Iterables
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.scottishtecharmy.soundscape.geoengine.ProtomapsGridState
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.InterpolatedPointsJoiner
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.vectorTileToGeoJson
import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import vector_tile.VectorTile

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

        return vectorTileToGeoJson(tileX, tileY, tile, cropPoints, 15)
    }

    @Test
    fun testRtree() {

        val joiner = InterpolatedPointsJoiner()

        // Make a large grid to aid analysis
        val featureCollection = FeatureCollection()
        for (x in 15990..15992) {
            for (y in 10212..10213) {
                val geojson = vectorTileToGeoJsonFromFile(x, y, "${x}x${y}.mvt")
                for (feature in geojson) {
                    val addFeature = joiner.addInterpolatedPoints(feature)
                    if (addFeature) {
                        featureCollection.addFeature(feature)
                    }
                }
            }
        }
        // Add lines to connect all of the interpolated points
        joiner.addJoiningLines(featureCollection)

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
            gridState.updateTile(x, y, featureCollections)
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
}