package org.scottishtecharmy.soundscape

import org.junit.Test
import org.scottishtecharmy.soundscape.geoengine.MANIFEST_NAME
import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geoengine.utils.getDistanceToFeature
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.moshi.GeoJsonObjectMoshiAdapter
import java.io.FileInputStream
import java.util.zip.GZIPInputStream
import kotlin.String

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
            println("$highlight ${extract.properties} - $shortestDistance")
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
        val metadataFile = GZIPInputStream(FileInputStream(path + MANIFEST_NAME)).bufferedReader().use { it.readText() }

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
     *
     * Continents -> Countries -> Cities
     *                         -> Provinces/States
     *
     */
    private fun addCountry(continents: MutableMap<String, MutableMap<String, MutableMap<String, Feature>>>,
                           country: String,
                           continent: String,
                           countryFeature: Feature? = null) {

        val countriesWithinContinent = continents[continent]
        if(countriesWithinContinent != null) {
            if (!countriesWithinContinent.contains(country)) {
                countriesWithinContinent[country] = mutableMapOf()
            }
            if (countryFeature != null) {
                val regionMap = countriesWithinContinent[country] as MutableMap<String, Feature>
                regionMap["country"] = countryFeature
            }
        }
    }
    private fun addCityCluster(continents: MutableMap<String, MutableMap<String, MutableMap<String, Feature>>>,
                               country: String,
                               continent: String,
                               cityClusterFeature: Feature) {

        addCountry(continents, country, continent)
        val regionMap = (continents[continent]!!)[country]
        if(regionMap != null) {
            val cityClusterName = cityClusterFeature.properties?.get("name")
            regionMap[cityClusterName as String] = cityClusterFeature
        }
    }

    private fun addProvince(continents: MutableMap<String, MutableMap<String, MutableMap<String, Feature>>>,
                            country: String,
                            continent: String,
                            provinceFeature: Feature) {

        addCountry(continents, country, continent)
        val regionMap = (continents[continent]!!)[country]
        if(regionMap != null) {
            val provinceName = provinceFeature.properties?.get("name")
            if (provinceName != null)
                regionMap[provinceName as String] = provinceFeature
        }
    }

    @Test
    fun testHierarchyGenerationFromManifest() {
        val path = "src/test/res/org/scottishtecharmy/soundscape/"
        val metadataFile =
            GZIPInputStream(FileInputStream(path + MANIFEST_NAME)).bufferedReader().use { it.readText() }

        // Load in the metadata GeoJSON file
        val adapter = GeoJsonObjectMoshiAdapter()
        val collection = adapter.fromJson(metadataFile) as FeatureCollection

        // This code just makes a map of continents to countries to provinces

        val continents: MutableMap<String, MutableMap<String, MutableMap<String, Feature>>> = mutableMapOf()
        for (feature in collection) {
            var continent = feature.properties?.get("continent")
            val isoA2 = feature.properties?.get("iso_a2")
            if(continent == null) {
                continent = when(isoA2) {
                    "US","CA" -> "North America"
                    "CN","JP","IN","RU" -> "Asia"
                    "DE","PL" -> "Europe"
                    "BR" -> "South America"
                    "AU" -> "Oceania"
                    else -> null
                }
            }

            continent?. let { continent ->

                // Add any continents that we haven't previously seen
                if (!continents.contains(continent)) {
                    continents[continent as String] = mutableMapOf()
                }

                val featureType = feature.properties?.get("feature_type") as String
                when (featureType) {
                    "country" -> {
                        val name = feature.properties?.get("name") as String
                        addCountry(
                            continents,
                            name,
                            continent as String,
                            feature
                        )
                    }
                    "city_cluster" -> {
                        val countryName = feature.properties?.get("iso_a2") as String
                        addCityCluster(
                            continents,
                            countryName,
                            continent as String,
                            feature
                        )
                    }
                    else -> {
                        val countryName = feature.properties?.get("iso_a2") as String
                        addProvince(
                            continents,
                            countryName,
                            continent as String,
                            feature
                        )
                    }
                }
            }
        }

        // Dump out the hierarchy
        for (continent in continents) {
            println("${continent.key} ->")
            for(country in continent.value) {
                val countryMembers = country.value
                println("\t${country.key} ->")
                for (member in countryMembers) {
                    val feature = member.value
                    val extractSize = feature.properties?.get("extract-size") as Double
                    val formattedSize = String.format("%.2fMB", extractSize / 1024 / 1024)
                    val extractName = feature.properties?.get("filename")

                    val memberCities = StringBuilder()
                    val cities = (feature.properties?.get("city_names") as? List<*>)?.filterIsInstance<String>()
                    if(cities != null) {
                        if(cities.size > 1) {
                            memberCities.append(" (")
                            for ((index, city) in cities.withIndex()) {
                                if (index != 0)
                                    memberCities.append(", ")
                                memberCities.append(city)
                            }
                            memberCities.append(")")
                        }
                    }
                    println("\t\t${member.key}${memberCities}, $extractName $formattedSize")
                }
            }
        }
    }
}
