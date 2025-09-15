package org.scottishtecharmy.soundscape

import org.junit.Test
import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geoengine.utils.getDistanceToFeature
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.moshi.GeoJsonObjectMoshiAdapter
import java.io.FileInputStream

class OfflineMapsTest {

    fun checkLocation(location: LngLatAlt, tree: FeatureTree) {
        println("Checking location: $location")

        val extracts = tree.getContainingPolygons(location)

        var mostCentral = 0
        var shortestDistance = Double.MAX_VALUE
        for((index, extract) in extracts.withIndex()) {
            val distanceToLocation = getDistanceToFeature(
                location,
                extract,
                CheapRuler(location.latitude)
            )
            if(distanceToLocation.distance < shortestDistance) {
                shortestDistance = distanceToLocation.distance
                mostCentral = index
            }
        }

        for((index, extract) in extracts.withIndex()) {
            var highlight = ""
            if(index == mostCentral)
                highlight = "*"
            println("$highlight ${extract.properties} - ${shortestDistance}")
        }

        val simple = tree.getNearestCollection(location, 250000.0, 5, CheapRuler(location.latitude))
        if(simple.features.isNotEmpty()) {
            println(simple.features[0].properties?.get("name"))
        }
    }

    /**
     * This test demonstrates loading in the extract GeoJSON and using it to find nearby extracts
     */
    @Test
    fun loadExtractMetadata() {
        val path = "src/test/res/org/scottishtecharmy/soundscape/"
        val metadataFile = FileInputStream(path + "manifest.geojson").bufferedReader().use { it.readText() }

        // Load in the metadata GeoJSON file
        val adapter = GeoJsonObjectMoshiAdapter()
        val collection = adapter.fromJson(metadataFile) as FeatureCollection

        // Add it to a FeatureTree for searching
        val tree = FeatureTree(collection)

        // Try out some locations to see which extracts they are in
        checkLocation( LngLatAlt(-0.1277653, 51.5074456), tree) // London
        checkLocation( LngLatAlt(-13.6872370, 57.5962764), tree) // Rockall!
        checkLocation( LngLatAlt(106.7021468, 10.7756293), tree)
    }
}
