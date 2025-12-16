package org.scottishtecharmy.soundscape

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.internal.platform.PlatformRegistry.applicationContext
import org.junit.Assert.assertNotEquals

import org.junit.Test
import org.junit.runner.RunWith
import org.scottishtecharmy.soundscape.components.LocationSource
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.ProtomapsGridState
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.utils.geocoders.AndroidGeocoder
import org.scottishtecharmy.soundscape.geoengine.utils.geocoders.OfflineGeocoder
import org.scottishtecharmy.soundscape.geoengine.utils.geocoders.PhotonGeocoder
import org.scottishtecharmy.soundscape.geoengine.utils.geocoders.SoundscapeGeocoder
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.utils.Analytics
import org.scottishtecharmy.soundscape.utils.toLocationDescription
import java.text.Normalizer
import java.util.Locale
import kotlin.time.measureTime

@RunWith(AndroidJUnit4::class)
class GeocoderTest {

    private suspend fun describeLocation(
        geocoder: SoundscapeGeocoder,
        userGeometry: UserGeometry,
        localizedContext: Context
    ): LocationDescription? {
        val description = geocoder.getAddressFromLngLat(userGeometry, localizedContext)
        return description
    }

    private val apostrophes = setOf('\'', '’', '‘', '‛', 'ʻ', 'ʼ', 'ʹ', 'ꞌ', '＇')
    fun normalizeForSearch(input: String): String {
        // 1) Unicode normalize (decompose accents)
        val nfkd = Normalizer.normalize(input, Normalizer.Form.NFKD)

        val sb = StringBuilder(nfkd.length)
        var lastWasSpace = false

        for (ch in nfkd) {
            // Remove combining marks (diacritics)
            val type = Character.getType(ch)
            if (type == Character.NON_SPACING_MARK.toInt()) continue

            // Make apostrophes disappear completely (missing/extra apostrophes become irrelevant)
            if (ch in apostrophes) continue

            // Turn most punctuation into spaces (keeps token boundaries stable)
            val isLetterOrDigit = Character.isLetterOrDigit(ch)
            val outCh = when {
                isLetterOrDigit -> ch.lowercaseChar()
                Character.isWhitespace(ch) -> ' '
                else -> ' ' // punctuation -> space
            }

            if (outCh == ' ') {
                if (!lastWasSpace) {
                    sb.append(' ')
                    lastWasSpace = true
                }
            } else {
                sb.append(outCh)
                lastWasSpace = false
            }
        }

        return sb.toString().trim().lowercase(Locale.ROOT)
    }

    fun search(query: String, names: List<String>, limit: Int = 10): List<Pair<String, Float>> {
        val q = normalizeForSearch(query)
        return emptyList()
    }

    private fun estimateNumberOfPlacenames(gridState: GridState) {

        /**
         * This is looking ahead to search to see how large the dictionaries will be and how quickly
         * we can search them.
         */
        var count = 0
        val dictionary = mutableListOf<String>()
        var timed = measureTime {
            val pois = gridState.getFeatureTree(TreeId.POIS).getAllCollection()
            val roads = gridState.getFeatureTree(TreeId.ROADS_AND_PATHS).getAllCollection()
            pois.forEach { poi ->
                if (!(poi as MvtFeature).name.isNullOrEmpty()) {
                    dictionary.add(normalizeForSearch(poi.name!!))
                    count++
                }
            }
            roads.forEach { road ->
                if (!(road as Way).name.isNullOrEmpty()) {
                    dictionary.add(normalizeForSearch(road.name!!))
                    count++
                }
            }
        }
        Log.e("GeocoderTest", "Estimated number of placenames: $count, in $timed")
        var results = listOf<Pair<String, Float>>()
        timed = measureTime {
            val query = normalizeForSearch("10 Roselea Drive")
            results = search(query, dictionary, 10)
        }
        results.forEach { Log.e("GeocoderTest", "${it.first} ${it.second}") }
        Log.e("GeocoderTest", "Search took $timed")
    }

    private fun reverseGeocodeLocation(
        list: List<SoundscapeGeocoder>,
        location: LngLatAlt,
        gridState: GridState,
        settlementState: GridState,
        localizedContext: Context,
        nameForMatchedRoad: String = ""
    ) : List<LocationDescription?> {
        val cheapRuler = CheapRuler(location.latitude)
        return runBlocking {
            // Update the grid states for this location
            gridState.locationUpdate(location, emptySet())
            settlementState.locationUpdate(location, emptySet())

            estimateNumberOfPlacenames(gridState)

            // Find the nearby road so as we can pretend that we are map matched
            val roadTree = gridState.getFeatureTree(TreeId.ROADS)
            val roads = roadTree.getNearestCollection(location, 500.0, 10, gridState.ruler)
            var mapMatchedWay : Way? = null
            if(nameForMatchedRoad.isNotEmpty()) {
                for (road in roads) {
                    if ((road as Way).name == nameForMatchedRoad) {
                        mapMatchedWay = road
                        break
                    }
                }
            }
            val userGeometry = UserGeometry(
                location = location,
                mapMatchedWay = mapMatchedWay,
                mapMatchedLocation =
                    if(mapMatchedWay != null)
                        gridState.ruler.distanceToLineString(location, mapMatchedWay.geometry as LineString)
                    else
                        null
            )

            // Run the geocoders in parallel and wait for them all to complete
            val deferredResults = list.map { geocoder ->
                async { describeLocation(geocoder, userGeometry, localizedContext) }
            }
            val results = deferredResults.awaitAll()

            // Handle the results
            results.forEachIndexed { index, result ->
                if(result != null) {
                    val distance = cheapRuler.distance(result.location, location)
                    Log.e("GeocoderTest", "${distance}m from ${list[index]}: $result")
                }
            }
            // Return the results
            results
        }
    }

    /**
     * reverseGeocodeTest takes a handful of locations and runs them through all of our various geocoders
     * and prints out the results. This is to aid debugging of the OfflineGeocoder whilst also giving
     * a better understanding of what the Photon and Android geocoders provide.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun reverseGeocodeTest() {
        Analytics.getInstance(true)
        runBlocking {
            // Context of the app under test.
            val appContext = InstrumentationRegistry.getInstrumentation().targetContext

            val gridState = ProtomapsGridState()
            val settlementGrid = ProtomapsGridState(zoomLevel = 12, gridSize = 3, gridState.treeContext)

            val geocoderList = listOf(
                AndroidGeocoder(appContext),
                PhotonGeocoder(appContext),
                OfflineGeocoder(gridState, settlementGrid)
            )
            val local = 2

            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext)
            val extractsPath = sharedPreferences.getString(MainActivity.SELECTED_STORAGE_KEY, MainActivity.SELECTED_STORAGE_DEFAULT)!!
            val offlineExtractPath = extractsPath + "/" + Environment.DIRECTORY_DOWNLOADS
            gridState.validateContext = false
            gridState.start(ApplicationProvider.getApplicationContext(), offlineExtractPath)
            settlementGrid.validateContext = false
            settlementGrid.start(ApplicationProvider.getApplicationContext(), offlineExtractPath)

            val briarwellLaneLocation = LngLatAlt( 	-4.3067678, 55.9414919)
            var results = reverseGeocodeLocation(
                geocoderList,
                briarwellLaneLocation,
                gridState,
                settlementGrid,
                appContext,
                "Briarwell Lane")
            assertNotEquals("Dougalston Golf Course", results[local]!!.name)

            val roseleaLocation = LngLatAlt( 	-4.3056, 55.9466)
            reverseGeocodeLocation(
                geocoderList,
                roseleaLocation,
                gridState,
                settlementGrid,
                appContext,
                "Roselea Drive")

            // On Braeside Avenue opposite the numbered houses
            // House number mapped on OSM and Google
            val oppositeLocation = LngLatAlt(-4.3199636, 55.9369369)
            results = reverseGeocodeLocation(
                geocoderList,
                oppositeLocation,
                gridState,
                settlementGrid,
                appContext,
                "Braeside Avenue")
            assertEquals(true, results[local]!!.opposite)
            assertEquals("10 Braeside Avenue", results[local]!!.name)

            // Corner between 10 Craigdhu Road and 1 Ferguson Avenue Milngavie
            // House number mapped on OSM and Google
            val wellKnownLocation = LngLatAlt(-4.3215166, 55.9404307)
            results = reverseGeocodeLocation(
                geocoderList,
                wellKnownLocation,
                gridState,
                settlementGrid,
                appContext,
                "Craigdhu Road")
            assertEquals(false, results[local]!!.opposite)
            assertEquals("10 Craigdhu Road", results[local]!!.name)

            results = reverseGeocodeLocation(
                geocoderList,
                wellKnownLocation,
                gridState,
                settlementGrid,
                appContext,
                "Ferguson Avenue")
            assertEquals(false, results[local]!!.opposite)
            assertEquals("1 Ferguson Avenue", results[local]!!.name)

            // Corner of 28 Dougalston Gardens North, Milngavie.
            // House number not mapped on OSM, but Google has it
            val lessWellKnownLocation = LngLatAlt(-4.3078777, 55.9394283)
            reverseGeocodeLocation(
                geocoderList,
                lessWellKnownLocation,
                gridState,
                settlementGrid,
                appContext,
                "Dougalston Gardens North"
            )
            // Without matched way
            reverseGeocodeLocation(
                geocoderList,
                lessWellKnownLocation,
                gridState,
                settlementGrid,
                appContext
            )

            // Junction of driveway for Baldernock Lodge and Craigmaddie Road near Baldernock
            // OSM doesn't know much at all, Google has all the information
            val ruralLocation = LngLatAlt(-4.2791516, 55.9465324)
            reverseGeocodeLocation(geocoderList, ruralLocation, gridState, settlementGrid, appContext)

            // On A809 along from Queens View car park
            // OSM knows about the car park, but doesn't return the road. Google is accurate to the
            // point that the car park is returned in the second result because it's further away.
            val veryRuralLocation = LngLatAlt(-4.387525, 55.995528)
            reverseGeocodeLocation(geocoderList, veryRuralLocation, gridState, settlementGrid, appContext)

            // Next to St. Giles Cathedral on the Royal Mile in Edinburgh
            val busyLocation = LngLatAlt(-3.1917130, 55.9494934)
            reverseGeocodeLocation(geocoderList, busyLocation, gridState, settlementGrid, appContext)

            // Kamakura in 6-chome-3 Zaimokuza
            val japanLocation = LngLatAlt(139.55200432751576, 35.30598235172923)
            reverseGeocodeLocation(geocoderList, japanLocation, gridState, settlementGrid, appContext)
        }
    }

    /**
     * geocodeTest takes a handful of addresses and runs them through all of our various geocoders
     * and prints out the results. This is to aid debugging of the OfflineGeocoder whilst also giving
     * a better understanding of what the Photon and Android geocoders provide.
     */
    private fun geocodeLocation(
        list: List<SoundscapeGeocoder>,
        nearbyLocation: LngLatAlt,
        searchString: String,
        gridState: GridState,
        settlementState: GridState
    ) {
        val cheapRuler = CheapRuler(nearbyLocation.latitude)

        suspend fun findPlace(geocoder: SoundscapeGeocoder, searchString: String, nearbyLocation: LngLatAlt): List<LocationDescription>? {
            val description = geocoder.getAddressFromLocationName(searchString, nearbyLocation, applicationContext)
            println("findPlace complete")
            return description
        }

        runBlocking {
            // Update the grid states for this location
            gridState.locationUpdate(nearbyLocation, emptySet())
            settlementState.locationUpdate(nearbyLocation, emptySet())

            // Run the geocoders in parallel and wait for them all to either fail or complete
            val timeoutMillis = 10000L
            val results: List<List<LocationDescription>?>? = withTimeoutOrNull(timeoutMillis) {
                val deferredResults = list.map { geocoder ->
                    async {
                        try {
                            findPlace(geocoder, searchString, nearbyLocation)
                        } catch (e: Exception) {
                            // If a geocoder fails, log the error and return null for that result
                            Log.e("GeocoderTest", "Geocoding failed for ${geocoder::class.simpleName}", e)
                            null
                        }
                    }
                }
                deferredResults.awaitAll()
            }

            // Handle the results
            results?.forEachIndexed { index, result ->
                if(result != null) {
                    if(result.isNotEmpty()) {
                        val distance = cheapRuler.distance(result.first().location, nearbyLocation)
                        Log.e("GeocoderTest", "${distance}m from ${list[index]}: $result")
                    }
                }
            } ?: Log.e("GeocoderTest", "All geocoding operations timed out")
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun geocodeTest() {
        Analytics.getInstance(true)
        runBlocking {
            // Context of the app under test.
            val appContext = InstrumentationRegistry.getInstrumentation().targetContext

            val gridState = ProtomapsGridState()
            val settlementGrid = ProtomapsGridState(zoomLevel = 12, gridSize = 3, gridState.treeContext)

            val geocoderList = listOf(
                AndroidGeocoder(appContext),
                PhotonGeocoder(appContext),
                OfflineGeocoder(gridState, settlementGrid)
            )

            gridState.validateContext = false
            gridState.start(ApplicationProvider.getApplicationContext())
            settlementGrid.validateContext = false
            settlementGrid.start(ApplicationProvider.getApplicationContext())

            val milngavie = LngLatAlt(-4.317166334292434, 55.941822016283)
            geocodeLocation(geocoderList, milngavie, "Honeybee Bakery, Milngavie", gridState, settlementGrid)

            val lisbon = LngLatAlt(-9.145010116796168, 38.707989573367804)
            geocodeLocation(geocoderList, lisbon, "Taberna Tosca, Lisbon", gridState, settlementGrid)

            val tarland = LngLatAlt(-2.8581118922791124, 57.1274095150638)
            geocodeLocation(geocoderList, tarland, "Commercial Hotel, Tarland", gridState, settlementGrid)
            geocodeLocation(geocoderList, tarland, "234ksdfhn98yjkhbd", gridState, settlementGrid)
        }
    }

    @Test
    fun testAddressFormatting() {
        // Start with something that turns into JSON easily
        val honeybee = MvtFeature()
        honeybee.properties = hashMapOf()
        honeybee.properties?.let { properties ->
            properties["name"] = "The Honeybee Bakery"
            properties["street"] = "Station Road"
            properties["district"] = "Milngavie"
            properties["postcode"] = "G62 8AB"
            properties["countrycode"] = "GB"
        }
        honeybee.geometry = Point(0.0, 0.0)
        println(honeybee.toLocationDescription(LocationSource.UnknownSource))

        honeybee.properties?.let { properties ->
            properties["name"] = "The Honeybee Bakery"
            properties["housenumber"] = "48"
            properties["street"] = "Station Road"
            properties["city"] = "Glasgow"
            properties["postcode"] = "G62 8AB"
            properties["county"] = "East Dunbartonshire"
            properties["state"] = "Scotland"
            properties["country"] = "Alba / Scotland"
        }
        println(honeybee.toLocationDescription(LocationSource.UnknownSource))

        // Add some JSON breaking characters
        honeybee.properties?.let { properties ->
            properties["name"] = "The Honeybee 'Bakery"
            properties["street"] = "Station' Ro{}ad<>"
            properties["country"] = "Alba / Scotland"
        }
        println(honeybee.toLocationDescription(LocationSource.UnknownSource))

    }
}