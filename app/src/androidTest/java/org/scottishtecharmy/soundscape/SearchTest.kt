package org.scottishtecharmy.soundscape

import android.os.Environment
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import ch.poole.geo.pmtiles.Reader
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.scottishtecharmy.soundscape.geoengine.MAX_ZOOM_LEVEL
import org.scottishtecharmy.soundscape.geoengine.ProtomapsGridState
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.utils.decompressTile
import org.scottishtecharmy.soundscape.geoengine.utils.geocoders.StreetDescription
import org.scottishtecharmy.soundscape.geoengine.utils.getXYTile
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.utils.Analytics
import org.scottishtecharmy.soundscape.utils.findExtractPaths
import org.scottishtecharmy.soundscape.utils.fuzzyCompare
import java.io.File
import java.text.Normalizer
import java.util.Collections
import java.util.Locale
import kotlin.time.measureTime

class SearchTest {

    val stringCache = Collections.synchronizedMap(mutableMapOf<Long, List<String>>())

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

    private fun localSearch(
        location: LngLatAlt,
        searchString: String
    ) : List<String> {

        val tileLocation = getXYTile(location, MAX_ZOOM_LEVEL)

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val path = sharedPreferences.getString(MainActivity.SELECTED_STORAGE_KEY, MainActivity.SELECTED_STORAGE_DEFAULT)
        val offlineExtractPath =  path + "/" + Environment.DIRECTORY_DOWNLOADS
        val extracts = findExtractPaths(offlineExtractPath).toMutableList()

        var reader : Reader? = null
        for(extract in extracts) {
            reader = Reader(File(extract))
            println("Try extract $extract")
            if(reader.getTile(MAX_ZOOM_LEVEL, tileLocation.first, tileLocation.second) != null)
                break
        }

        // We now have a PM tile reader
        var x = tileLocation.first
        var y = tileLocation.second

        var dx = 1 // Change in x per step
        var dy = 0 // Change in y per step

        var steps = 1 // Number of steps to take in the current direction
        var turnCount = 0
        var stepsTaken = 0

        // Set a limit to how far out you want to spiral
        val maxSearchRadius = 10
        val maxTurns = maxSearchRadius * 2

        val normalizedNeedle = normalizeForSearch(searchString)
        val searchResults = mutableListOf<String>()


        while (turnCount < maxTurns) {
            val tileIndex = x.toLong() + (y.toLong().shl(32))
            var cache = stringCache[tileIndex]
            if(cache == null) {

                // Load the tile and add all of its String to a cache
                println("Get tile: ($x, $y)")
                val tileData = reader?.getTile(MAX_ZOOM_LEVEL, x, y)
                if (tileData != null) {
                    val tile = decompressTile(reader.tileCompression, tileData)
                    if(tile != null) {
                        cache = mutableListOf()
                        for(layer in tile.layersList) {
                            if((layer.name == "transportation") || (layer.name == "poi") || (layer.name == "building")) {
                                for (value in layer.valuesList) {
                                    if (value.hasStringValue()) {
                                        cache.add(normalizeForSearch(value.stringValue))
                                    }
                                }
                            }
                        }
                        stringCache[tileIndex] = cache
                    }
                }
            }
            if(cache == null) {
                println("Failed to load tile")
                reader?.close()
                return emptyList()
            }
            for(string in cache) {

                val score = normalizedNeedle.fuzzyCompare(string, true)
                if(score < 0.3) {
                    println("Found $searchString as $string (score $score)")
                    searchResults += string
                }
            }
            // --- 2. Move to the next position in the spiral ---
            x += dx
            y += dy
            stepsTaken++

            // --- 3. Check if it's time to turn ---
            if (stepsTaken == steps) {
                stepsTaken = 0
                turnCount++

                // Rotate direction: (1,0) -> (0,1) -> (-1,0) -> (0,-1)
                val temp = dx
                dx = -dy
                dy = temp

                // After every two turns, increase the number of steps
                if (turnCount % 2 == 0) {
                    steps++
                }
            }
        }
        reader?.close()
        return emptyList()
    }

    @Test
    fun offlineSearch() {
        runBlocking {

//            val currentLocation = LngLatAlt(-4.3215166, 55.9404307)
            val currentLocation = LngLatAlt(-3.1917130, 55.9494934)
            var cacheSize = 0
            stringCache.forEach { set -> cacheSize += set.value.size }
            var time = measureTime {
                localSearch(currentLocation, "Milverton Avenue")
            }
            println("Time taken round 1: $time (cache size $cacheSize strings)")
            cacheSize = 0
            stringCache.forEach { set -> cacheSize += set.value.size }
            time = measureTime {
                localSearch(currentLocation, "Milverto Avenue")
            }
            println("Time taken round 2: $time (cache size $cacheSize strings)")

            cacheSize = 0
            stringCache.forEach { set -> cacheSize += set.value.size }
            time = measureTime {
                localSearch(currentLocation, "Roselea Dr")
            }
            println("$time (cache size $cacheSize strings)")

            cacheSize = 0
            stringCache.forEach { set -> cacheSize += set.value.size }
            time = measureTime {
                localSearch(currentLocation, "Dirleton Gate")
            }
            println("$time (cache size $cacheSize strings)")

            cacheSize = 0
            stringCache.forEach { set -> cacheSize += set.value.size }
            time = measureTime {
                localSearch(currentLocation, "Dirleton Gate")
            }
            println("$time (cache size $cacheSize strings)")

            // Final cache size
            var totalStringSize = 0
            stringCache.forEach { set ->
                for(string in set.value) {
                    totalStringSize += string.length
                }
            }
            println("Total string length in cache $totalStringSize")
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun streetDescription(location: LngLatAlt,
                          streetName: String,
                          describeLocation: LngLatAlt? = null) {

        Analytics.getInstance(true)

        val gridState = ProtomapsGridState()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val path = sharedPreferences.getString(MainActivity.SELECTED_STORAGE_KEY, MainActivity.SELECTED_STORAGE_DEFAULT)
        val offlineExtractPath =  path + "/" + Environment.DIRECTORY_DOWNLOADS
        gridState.validateContext = false
        gridState.start(ApplicationProvider.getApplicationContext(), offlineExtractPath)
        runBlocking {
            gridState.locationUpdate(location,emptySet())
        }

        val nearbyWays = gridState.getFeatureTree(TreeId.ROADS_AND_PATHS)
            .getNearbyCollection(
                location,
                100.0,
                gridState.ruler
            )
        var matchedWay: Way? = null
        for(way in nearbyWays) {
            if((way as Way).name == streetName) {
                matchedWay = way
                break
            }
        }
        if(matchedWay == null) return

        val duration = measureTime {
            val description = StreetDescription(streetName, gridState)
            description.createDescription(matchedWay, null)
            description.describeLocation(
                location,
                null,
                matchedWay,
                context
            )
            if (describeLocation != null) {
                val nearestWay = description.nearestWayOnStreet(describeLocation)
                if (nearestWay != null) {
                    val houseNumber =
                        description.getStreetNumber(nearestWay.first, describeLocation)
                    println("Interpolated address: ${if (houseNumber.second) "Opposite" else ""} ${houseNumber.first} ${nearestWay.first.name}")
                }
            }
        }
        println("Street description and lookup took $duration")
    }

    @Test
    fun testStreetDescription() {
        streetDescription(
            LngLatAlt(-4.3133672, 55.9439536),
            "Buchanan Street",
            LngLatAlt(-4.3130768, 55.9446026)
        )
        // Opposite test
        streetDescription(
            LngLatAlt(-4.3133672, 55.9439536),
            "Buchanan Street",
            LngLatAlt(-4.3135689, 55.9440448)
        )
        streetDescription(
            LngLatAlt(-4.3177683, 55.9415574),
            "Douglas Street",
            LngLatAlt(-4.3186897, 55.9410192)
        )
        streetDescription(
            LngLatAlt(-4.2627887, 55.8622846),
            "St Vincent Street",
            LngLatAlt(-4.2637612, 55.8622651)
        )
        streetDescription(
            LngLatAlt(-4.2627887, 55.8622846),
            "St Vincent Street",
            LngLatAlt(-4.2642336, 55.8624708)
        )
    }
}