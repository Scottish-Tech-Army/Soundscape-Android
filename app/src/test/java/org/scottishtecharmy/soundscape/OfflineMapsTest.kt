package org.scottishtecharmy.soundscape

import org.junit.Test
import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geoengine.utils.getDistanceToFeature
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
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
    fun testManifestInFeatureTree() {
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

    /**
     * This test demonstrates the organising of the manifest by continent and country
     */
    @Test
    fun testHierarchyGenerationFromManifest() {
        val path = "src/test/res/org/scottishtecharmy/soundscape/"
        val metadataFile =
            FileInputStream(path + "manifest.geojson").bufferedReader().use { it.readText() }

        // Load in the metadata GeoJSON file
        val adapter = GeoJsonObjectMoshiAdapter()
        val collection = adapter.fromJson(metadataFile) as FeatureCollection

        // This code just makes a map of continents to countries to provinces

        val continents: MutableMap<String, MutableMap<String, Any>> = mutableMapOf()
        for (feature in collection) {
            val continent = feature.properties?.get("continent")
            continent?. let { continent ->

                // Add any continents that we haven't previously seen
                if (!continents.contains(continent)) {
                    continents[continent as String] = mutableMapOf()
                }

                if (feature.properties?.get("feature_type") == "country") {
                    // Add new country
                    val name = feature.properties?.get("name") as String
                    val countriesMap = continents[continent]!!
                    countriesMap[name] = feature
                }
                else {
                    // This Feature is not a country, but is a province of some kind
                    val countryName = feature.properties?.get("country_name") as String
                    val countriesMap = continents[continent]!!

                    if (!countriesMap.contains(countryName)) {
                        countriesMap[countryName] = mutableMapOf<String, MutableMap<String, Feature>>()
                    }
                    val regionMap = countriesMap[countryName] as MutableMap<String, Feature>
                    val name = feature.properties?.get("name")
                    if (name == null)
                        println("Bug with unusual Russian region")
                    else
                        regionMap[name as String] = feature
                }
            }
        }

        // Dump out the hierarchy
        for (continent in continents) {
            println("${continent.key} ->")
            for(country in continent.value) {
                val states = country.value
                if(states is MutableMap<*, *>) {
                    println("\t${country.key} ->")
                    for (state in states) {
                        val feature = state.value as Feature
                        val extractSize = feature.properties?.get("extract-size") as Double
                        val formattedSize = String.format("%.2fMB", extractSize / 1024 / 1024)
                        val extractName = feature.properties?.get("filename")
                        println("\t\t${state.key}, $extractName $formattedSize")
                    }
                } else {
                    val feature = country.value as Feature
                    val extractSize = feature.properties?.get("extract-size") as Double
                    val formattedSize = String.format("%.2fMB", extractSize / 1024 / 1024)
                    val extractName = feature.properties?.get("filename")
                    println("\t${country.key}, $extractName $formattedSize")
                }
            }
        }
    }
}
