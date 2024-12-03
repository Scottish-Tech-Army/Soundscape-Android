package org.scottishtecharmy.soundscape

import androidx.test.platform.app.InstrumentationRegistry
import com.github.davidmoten.rtree2.Iterables
import org.junit.Test
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.InterpolatedPointsJoiner
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.vectorTileToGeoJson
import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geoengine.utils.getNearestPoi
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
        val distanceResults = Iterables.toList(tree.generateNearbyFeatureCollection(LngLatAlt(-4.316914, 55.941861), 10.0))
        end = System.currentTimeMillis()
        println("Search result in ${end-start}ms")
        for(dResult in distanceResults) {
            println(dResult.properties?.get("name"))
        }

        start = System.currentTimeMillis()
        val fc = getNearestPoi(LngLatAlt(-4.316914, 55.941861), featureCollection)
        end = System.currentTimeMillis()
        println("getNearestPoi result in ${end-start}ms : ${fc.features[0].properties}")
    }
}