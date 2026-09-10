package org.scottishtecharmy.soundscape.geoengine.utils.geocoders

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okio.Path.Companion.toPath
import org.scottishtecharmy.soundscape.components.LocationSource
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.MAX_ZOOM_LEVEL
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.convertGeometry
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.convertGeometryAndClipLineToTile
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.parseGeometry
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.pointIsOffTile
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.translateProperties
import org.scottishtecharmy.soundscape.geoengine.utils.decompressTile
import org.scottishtecharmy.soundscape.geoengine.utils.getCentroidOfPolygon
import org.scottishtecharmy.soundscape.geoengine.utils.getXYTile
import org.scottishtecharmy.soundscape.geoengine.utils.pmtiles.PmTilesReader
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon
import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.utils.findExtractPaths
import org.scottishtecharmy.soundscape.utils.fuzzyCompare
import org.scottishtecharmy.soundscape.utils.toLocationDescription
import vector_tile.Tile

class TileSearch(
    val offlineExtractPath: String,
    val gridState: GridState,
    val settlementGrid: GridState
) : TileSearcher {

    // Keyed only by tile (x, y), not by which extract the data came from, so this must be
    // cleared whenever the on-disk offline extracts change - otherwise a search can keep
    // returning strings from an extract that's since been replaced or deleted.
    val stringCache = mutableMapOf<Long, List<String>>()

    /**
     * Called when the on-disk offline map extracts have changed (a download completed, or an
     * extract was deleted) so that the next search re-reads tile content from the current
     * extracts instead of returning cached strings from a superseded one.
     */
    fun refreshOfflineMaps() {
        stringCache.clear()
    }

    private fun cacheIndex(x: Int, y: Int): Long {
        return x.toLong() + (y.toLong().shl(32))
    }

    private fun trimCache(keepSet: MutableSet<Long>) {
        // Remove all tiles which aren't in the keepSet
        stringCache.keys.removeAll { !keepSet.contains(it) }
    }

    fun findNearestNamedWay(location: LngLatAlt, name: String?): Way? {
        val nearestWays =
            gridState.getFeatureTree(TreeId.ROADS).getNearestCollection(
                location,
                100.0,
                10,
                gridState.ruler,

                )
        for (way in nearestWays) {
            val wayName = (way as MvtFeature?)?.name
            if (name != null) {
                if (wayName == name) {
                    return way as Way?
                }
            } else {
                if (wayName != null) {
                    return way as Way?
                }
            }
        }
        return null
    }

    /**
     * [houseNumber] is the word taken out of the search string as a house number to make this
     * match, or empty if the match was made with the search string as a whole.
     */
    data class TileSearchResult(
        var score: Double,
        var string: String,
        val tileX: Int,
        val tileY: Int,
        val houseNumber: String = "",
    )

    fun compareAndAddToResults(
        normalizedNeedle: String,
        haystackString: String,
        searchResults: MutableList<TileSearchResult>,
        searchResultLimit: Int,
        tileX: Int, tileY: Int,
        houseNumber: String = "",
    ): Boolean {
        // A haystack a quarter shorter than the needle is at least a quarter of the needle's length
        // from it, which is too far to match - and that's cheaper to see than the distance
        if (normalizedNeedle.isNotEmpty() && (haystackString.length * 4 <= normalizedNeedle.length * 3))
            return false
        val fuzzyScore = normalizedNeedle.fuzzyCompare(haystackString, true)
        if (fuzzyScore < 0.25) {
            // Taking a house number out of the search string makes the rest of it easier to match,
            // so a match made that way loses a tie with one made from the whole string - otherwise
            // "4a Avenida Sur" finds number 4a on 5a Avenida Sur.
            val score = if (houseNumber.isEmpty()) fuzzyScore else fuzzyScore + HOUSE_NUMBER_PENALTY

            // If we already have better search results, discard this one
            val countOfBetter = searchResults.count { it.score < score }
            if (countOfBetter < searchResultLimit) {
                searchResults += TileSearchResult(score, haystackString, tileX, tileY, houseNumber)
                searchResults.sortBy { it.score }
                if (searchResults.size > searchResultLimit)
                    searchResults.removeAt(searchResults.lastIndex)

                return true
            }
        }
        return false
    }

    /**
     * Compares [needle] with a string from a tile, and adds it to [searchResults] if it matches
     * as a whole, by its last words ("rivoli" in "rue de rivoli"), or with a settlement name taken
     * off the end of the needle.
     */
    private fun addMatches(
        needle: String,
        needleWithoutSettlement: String?,
        houseNumber: String,
        string: String,
        searchResults: MutableList<TileSearchResult>,
        searchResultLimit: Int,
        tileX: Int, tileY: Int,
    ) {
        if (compareAndAddToResults(needle, string, searchResults, searchResultLimit, tileX, tileY, houseNumber))
            return
        if (string.length > needle.length) {
            if (compareAndAddToResults(
                    needle,
                    endOfNormalizedString(string, needle.length),
                    searchResults,
                    searchResultLimit,
                    tileX, tileY,
                    houseNumber
                )
            )
                return

            // Chinese, Japanese and Thai don't put spaces between words, and Korean doesn't always,
            // so a word can start anywhere in a run of them - "梅田" in "大阪梅田". Only worth
            // looking for when the needle is written that way too.
            if (needle.isNotEmpty() && isUnspacedScript(codePointAt(needle, 0))) {
                val bestEnd = closestEndWithinWords(needle, string)
                if ((bestEnd != null) &&
                    compareAndAddToResults(needle, bestEnd, searchResults, searchResultLimit, tileX, tileY, houseNumber)
                )
                    return
            }
        }

        // Whether there are spaces between the words of a Chinese, Japanese, Korean or Thai name is
        // up to whoever typed it, in the map or in the search - "ホテル イビス 大阪 梅田" is looked
        // for as "ホテルイビス" - so compare those without them too
        if (needle.isNotEmpty() && isUnspacedScript(codePointAt(needle, 0))) {
            val joinedNeedle = joinUnspacedWords(needle)
            val joinedString = joinUnspacedWords(string)
            if ((joinedNeedle != needle) || (joinedString != string)) {
                if (compareAndAddToResults(joinedNeedle, joinedString, searchResults, searchResultLimit, tileX, tileY, houseNumber))
                    return
                val bestEnd = closestEndWithinWords(joinedNeedle, joinedString)
                if ((bestEnd != null) &&
                    compareAndAddToResults(joinedNeedle, bestEnd, searchResults, searchResultLimit, tileX, tileY, houseNumber)
                )
                    return
            }
        }
        if (needleWithoutSettlement != null) {
            compareAndAddToResults(
                needleWithoutSettlement,
                string,
                searchResults,
                searchResultLimit,
                tileX, tileY,
                houseNumber
            )
        }
    }

    fun addLastWords(wordCount: Int, words: List<String>): String {
        val result = StringBuilder()
        var count = wordCount
        for (word in words.reversed()) {
            result.insert(0, word)
            if (--count == 0)
                break
            result.insert(0, " ")
        }
        // If wordCount > words.size, the loop runs out of words before `count` ever reaches 0,
        // leaving the last-inserted leading separator in place.
        return result.toString().trim()
    }

    fun generateWithoutSettlement(string: String, settlementNames: Set<String>): String? {
        val hayStackWords = string.trim().split(" ")

        // Try and match the last words with settlements - non fuzzy!
        var wordTarget = hayStackWords.size
        for (settlementName in settlementNames) {
            if (settlementName.fuzzyCompare(hayStackWords.last(), true) < 0.25) {
                // We have a one word match
                wordTarget = hayStackWords.size - 1
                break
            } else {
                // Search for settlements with up to 5 words in name
                for (count in 1..5) {
                    if ((hayStackWords.size > count) && (settlementName.fuzzyCompare(
                            addLastWords(count, hayStackWords), true
                        ) < 0.25)
                    ) {
                        wordTarget = hayStackWords.size - count
                        break
                    }
                }
            }
            if (wordTarget != hayStackWords.size) break
        }
        // No matches found, so only search on full string
        if (wordTarget == hayStackWords.size) return null

        val finalWordsBuilder = StringBuilder()
        for (word in hayStackWords) {
            // Checked before appending (not after decrementing) so a wordTarget of 0 - the
            // whole string was consumed by the settlement match - correctly emits nothing,
            // instead of the post-decrement check (`--wordTarget == 0`) never firing once
            // wordTarget starts at 0 and only counts further downward.
            if (wordTarget == 0)
                break
            finalWordsBuilder.append(word)
            finalWordsBuilder.append(" ")
            --wordTarget
        }
        return finalWordsBuilder.toString().trim()
    }

    fun generateEndOfString(string: String, maxLength: Int): String =
        endOfNormalizedString(normalizeForSearch(string), maxLength)

    /**
     * The last words of [normalizedString] - as few as make [maxLength] characters, counting a space
     * after each word. The strings the search goes through are all normalized already, so they
     * don't need normalizing again for this.
     */
    private fun endOfNormalizedString(normalizedString: String, maxLength: Int): String {
        var start = normalizedString.lastIndexOf(' ') + 1
        while ((normalizedString.length - start + 1 < maxLength) && (start > 0)) {
            // Take in the word before, which ends with the space just before this one
            start = normalizedString.lastIndexOf(' ', start - 2) + 1
        }
        return normalizedString.substring(start)
    }

    /**
     * The rest of [normalizedString] from each character part-way through a run of a script which
     * doesn't put spaces between its words - Chinese, Japanese and Thai, and Korean, which doesn't
     * always. Where [generateEndOfString] finds "rivoli" at the end of "rue de rivoli", these are
     * what find "梅田" in "大阪梅田駅": the comparison is with the start of each of "阪梅田駅",
     * "梅田駅", "田駅" and "駅". An end never starts at a Thai vowel written after its consonant, or
     * at the consonant after a Thai vowel written before it, as either would split the two.
     */
    fun generateEndsWithinWords(normalizedString: String): List<String> =
        normalizedString.indices
            .filter { startsWithinWord(normalizedString, it) }
            .map { normalizedString.substring(it) }

    /** Whether one of [generateEndsWithinWords] starts at [index] in [string]. */
    private fun startsWithinWord(string: String, index: Int): Boolean {
        if (index == 0) return false
        // By code point, so that an ideograph beyond U+FFFF isn't split into its surrogates - the
        // second of the pair isn't in any of the scripts, so an end never starts there
        val codePoint = codePointAt(string, index)
        val previous = codePointBefore(string, index)
        // A voicing mark (U+3099) left separate by normalization belongs to the kana before it, as
        // a Thai tone mark or vowel sign does to its consonant, so it's part of the run but never
        // starts an end
        return isUnspacedScript(codePoint) && isUnspacedScript(previous) &&
            (string[index].category != CharCategory.NON_SPACING_MARK) &&
            (codePoint !in thaiVowelsAfterConsonant) && (previous !in thaiVowelsBeforeConsonant)
    }

    /** Whether [end] is one of the [generateEndsWithinWords] of [normalizedString], without making them all. */
    private fun isEndWithinWords(normalizedString: String, end: String): Boolean {
        val start = normalizedString.length - end.length
        return end.isNotEmpty() && (start > 0) && normalizedString.endsWith(end) &&
            startsWithinWord(normalizedString, start)
    }

    /**
     * Of the [generateEndsWithinWords] of [normalizedString] at least as long as [needle], the one
     * the needle is closest to, or null if there are none. The needle is compared with the start of
     * each, so that's all that's taken to compare - as long as the needle, and a character more to
     * tell an end which goes on beyond it from one which doesn't.
     */
    private fun closestEndWithinWords(needle: String, normalizedString: String): String? {
        var bestStart = -1
        var bestScore = Double.MAX_VALUE
        for (start in 1..(normalizedString.length - needle.length)) {
            if (!startsWithinWord(normalizedString, start)) continue
            val compared = normalizedString.substring(start, minOf(normalizedString.length, start + needle.length + 1))
            val score = needle.fuzzyCompare(compared, true)
            if (score < bestScore) {
                bestScore = score
                bestStart = start
            }
        }
        return if (bestStart < 0) null else normalizedString.substring(bestStart)
    }

    // The Thai vowels written after their consonant which aren't marks - sara a, sara aa, sara am
    // and lakkhangyao - and those written before it: sara e, sara ae, sara o and the two sara ai
    private val thaiVowelsAfterConsonant = setOf(0x0E30, 0x0E32, 0x0E33, 0x0E45)
    private val thaiVowelsBeforeConsonant = 0x0E40..0x0E44

    /**
     * [normalizedString] without the spaces between the words of a Chinese, Japanese, Korean or
     * Thai name, which are sometimes written and often not: "ホテル イビス 大阪 梅田" becomes
     * "ホテルイビス大阪梅田". A space with anything else on either side of it stays, as in
     * "hep five 梅田店".
     */
    fun joinUnspacedWords(normalizedString: String): String {
        // Most strings have no such space, and they're returned as they are rather than copied
        if (normalizedString.indices.none { isSpaceBetweenUnspacedWords(normalizedString, it) })
            return normalizedString
        val joined = StringBuilder(normalizedString.length)
        for ((index, ch) in normalizedString.withIndex()) {
            if (!isSpaceBetweenUnspacedWords(normalizedString, index)) joined.append(ch)
        }
        return joined.toString()
    }

    private fun isSpaceBetweenUnspacedWords(string: String, index: Int): Boolean =
        (string[index] == ' ') && (index > 0) && (index + 1 < string.length) &&
            isUnspacedScript(codePointBefore(string, index)) && isUnspacedScript(codePointAt(string, index + 1))

    /**
     * Whether [codePoint] is in a script which doesn't put spaces between its words, or doesn't
     * always: Chinese and Japanese kanji/hanzi, hiragana and katakana, Korean hangul, and Thai.
     */
    private fun isUnspacedScript(codePoint: Int): Boolean =
        (codePoint in 0x0E00..0x0E7F) ||     // Thai
            (codePoint in 0x3040..0x30FF) ||  // Hiragana and Katakana, including the voicing marks
            (codePoint in 0x3400..0x4DBF) ||  // CJK Unified Ideographs Extension A
            (codePoint in 0x4E00..0x9FFF) ||  // CJK Unified Ideographs
            (codePoint in 0xAC00..0xD7A3) ||  // Hangul syllables
            (codePoint in 0xF900..0xFAFF) ||  // CJK Compatibility Ideographs
            (codePoint in 0x20000..0x3FFFF)   // CJK Unified Ideographs Extensions B onwards

    /** A layer of a tile which the search looks in, with its strings normalized to compare with a match. */
    private class SearchedLayer(
        val layer: Tile.Layer,
        val nameTagIndices: Set<Int>,
        val normalizedValues: List<String?>,
        val joinedValues: List<String?>,
    )

    /** The layers of the tile at [tileX], [tileY] which the search looks in - none if it can't be read. */
    private fun searchedLayers(reader: PmTilesReader?, tileX: Int, tileY: Int): List<SearchedLayer> {
        val tileData = try { reader?.getTile(MAX_ZOOM_LEVEL, tileX, tileY) } catch (_: Exception) { null }
        if ((reader == null) || (tileData == null)) return emptyList()
        val tile = decompressTile(reader.tileCompression, tileData) ?: return emptyList()
        return tile.layers
            .filter { (it.name == "transportation") || (it.name == "poi") }
            .map { layer ->
                val normalizedValues = layer.values.map { value -> value.string_value?.let { normalizeForSearch(it) } }
                SearchedLayer(
                    layer,
                    layer.keys.withIndex().filter { (_, key) -> isNameKey(key) }.map { it.index }.toSet(),
                    normalizedValues,
                    normalizedValues.map { value -> value?.let { joinUnspacedWords(it) } },
                )
            }
    }

    /** Whether [feature] has the string at [stringKey] in its layer's values as one of its names. */
    private fun featureHasName(feature: Tile.Feature, nameTagIndices: Set<Int>, stringKey: Int): Boolean {
        val tags = feature.tags
        for (i in 0 until tags.size - 1 step 2) {
            if ((tags[i] in nameTagIndices) && (tags[i + 1] == stringKey)) return true
        }
        return false
    }

    private fun featureProperties(layer: Tile.Layer, feature: Tile.Feature): HashMap<String, Any?> {
        val properties = hashMapOf<String, Any?>()
        val tags = feature.tags
        for (i in 0 until tags.size - 1 step 2) {
            val raw = layer.values[tags[i + 1]]
            properties[layer.keys[tags[i]]] = raw.bool_value ?: raw.int_value ?: raw.sint_value
                ?: raw.float_value ?: raw.double_value ?: raw.string_value ?: raw.uint_value
        }
        return properties
    }

    private class FeatureLocation(val location: LngLatAlt, val distance: Double)

    /**
     * Where to put a search result for [feature] from the tile at [tileX], [tileY] - the point,
     * the middle of the part of the line that's within the tile, or the centre of the polygon - and
     * how far the feature is from [searchLocation]. Null if none of the feature is within the tile.
     *
     * A line's distance is to its nearest point rather than its middle, so that searching for the
     * street you're standing on ranks it as near as it is.
     */
    private fun featureLocation(
        feature: Tile.Feature,
        tileX: Int,
        tileY: Int,
        searchLocation: LngLatAlt,
        ruler: CheapRuler
    ): FeatureLocation? {
        when (feature.type) {
            Tile.GeomType.POINT -> {
                for (point in parseGeometry(true, feature.geometry)) {
                    if (point.isNotEmpty()) {
                        val coordinates = convertGeometry(tileX, tileY, MAX_ZOOM_LEVEL, point)
                        if (coordinates.isNotEmpty())
                            return FeatureLocation(coordinates[0], ruler.distance(searchLocation, coordinates[0]))
                    }
                }
            }

            Tile.GeomType.LINESTRING -> {
                for (line in parseGeometry(false, feature.geometry)) {
                    val interpolatedNodes: MutableList<LngLatAlt> = mutableListOf()
                    val clippedLines = convertGeometryAndClipLineToTile(
                        tileX,
                        tileY,
                        MAX_ZOOM_LEVEL,
                        line,
                        interpolatedNodes
                    )
                    for (clippedLine in clippedLines) {
                        val centreDistance = ruler.lineLength(clippedLine) / 2
                        return FeatureLocation(
                            ruler.along(clippedLine, centreDistance),
                            ruler.distanceToLineString(searchLocation, clippedLine).distance
                        )
                    }
                }
            }

            Tile.GeomType.POLYGON -> {
                val polygons = parseGeometry(false, feature.geometry)

                // If all of the polygon points are outside the tile, then we can immediately
                // discard it
                val allOutside = polygons.all { polygon ->
                    polygon.all { point -> pointIsOffTile(point.first, point.second) }
                }
                if (allOutside) return null

                for (polygon in polygons) {
                    val polygonGeo = Polygon(convertGeometry(tileX, tileY, MAX_ZOOM_LEVEL, polygon))
                    val centre = getCentroidOfPolygon(polygonGeo) ?: polygonGeo.coordinates[0][0]
                    return FeatureLocation(centre, ruler.distance(searchLocation, centre))
                }
            }

            else -> {}
        }
        return null
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun search(
        location: LngLatAlt,
        searchString: String,
        localizedStrings: LocalizedStrings?,
        settlementNames: Set<String>
    ): List<LocationDescription> {
        val tileLocation = getXYTile(location, MAX_ZOOM_LEVEL)
        val extracts = findExtractPaths(offlineExtractPath).toMutableList()
        var reader: PmTilesReader? = null
        for (extract in extracts) {
            // PmTilesReader closes its own file if it can't be opened
            val candidate = try { PmTilesReader(extract.toPath()) } catch (_: Exception) { continue }
            // Keep the last extract that could be read even if it doesn't have this tile, but close
            // the one it replaces
            try { reader?.close() } catch (_: Exception) {}
            reader = candidate
            try {
                if (candidate.getTile(MAX_ZOOM_LEVEL, tileLocation.first, tileLocation.second) != null)
                    break
            } catch (_: Exception) {
                try { candidate.close() } catch (_: Exception) {}
                reader = null
            }
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

        // Can we decode this into a street number and a street? A word starting with a digit may be
        // a house number ("21 Kersland Drive", "Avenida Corrientes 1155"), but it may just as well
        // be part of the name ("Avenida 9 de Julio", "4a Avenida Sur"), so when there is one the
        // search is made both ways: without the number, and with the string as a whole.
        var housenumber = ""
        val needleBuilder = StringBuilder()
        val words = searchString.split(" ")
        for (word in words) {
            if (word.isEmpty()) continue
            if (word.first().isDigit()) {
                housenumber = word
            } else {
                // All other parts we use as the needle
                needleBuilder.append(word)
                needleBuilder.append(" ")
            }
        }
        val normalizedNeedle = normalizeForSearch(needleBuilder.toString())
        val wholeNeedle = if (housenumber.isEmpty()) null else normalizeForSearch(searchString)

        data class DetailedSearchResult(
            var score: Double,
            var string: String,
            var location: LngLatAlt,
            var properties: HashMap<String, Any?> = hashMapOf(),
            val layer: String,
            val houseNumber: String,
            val distance: Double,
        )

        val searchResults = mutableListOf<TileSearchResult>()
        val searchResultLimit = 8
        val needleWithoutSettlement = generateWithoutSettlement(normalizedNeedle, settlementNames)
        val wholeNeedleWithoutSettlement = wholeNeedle?.let { generateWithoutSettlement(it, settlementNames) }
        val tilesUsed = mutableSetOf<Long>()
        while (turnCount < maxTurns) {
            val tileIndex = cacheIndex(x, y)
            var cache = stringCache[tileIndex]
            tilesUsed.add(tileIndex)
            if (cache == null) {
                // Load the tile and add all of its String to a cache
                cache = mutableListOf()
                val tileData = try { reader?.getTile(MAX_ZOOM_LEVEL, x, y) } catch (_: Exception) { null }
                val currentReader = reader
                if (tileData != null && currentReader != null) {
                    val tile = decompressTile(currentReader.tileCompression, tileData)
                    if (tile != null) {
                        for (layer in tile.layers) {
                            if ((layer.name == "transportation") || (layer.name == "poi")) {
                                for (value in layer.values) {
                                    val sv = value.string_value
                                    if (sv != null) {
                                        cache.add(normalizeForSearch(sv))
                                    }
                                }
                            }
                        }
                        stringCache[tileIndex] = cache
                    }
                }
            }
            for (string in cache) {
                addMatches(
                    normalizedNeedle,
                    needleWithoutSettlement,
                    housenumber,
                    string,
                    searchResults,
                    searchResultLimit,
                    x, y
                )
                // The number can only be part of the name if the name has a number in it
                if ((wholeNeedle != null) && string.any { it.isDigit() }) {
                    addMatches(
                        wholeNeedle,
                        wholeNeedleWithoutSettlement,
                        "",
                        string,
                        searchResults,
                        searchResultLimit,
                        x, y
                    )
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
        // Free up any tiles that we no longer use
        trimCache(tilesUsed)

        // We have some rough results, but we need to get precise locations for each and remove any
        // duplicates due to tile boundary overlap and roads crossing tiles
        val ruler = CheapRuler(location.latitude)
        val detailedResults = mutableListOf<DetailedSearchResult>()
        // Results are often in the same tile, so each tile is read, and the strings in its layers
        // normalized, only once
        val searchedLayersByTile = mutableMapOf<Long, List<SearchedLayer>>()
        for (result in searchResults) {
            val searchedLayers = searchedLayersByTile.getOrPut(cacheIndex(result.tileX, result.tileY)) {
                searchedLayers(reader, result.tileX, result.tileY)
            }
            for (searchedLayer in searchedLayers) {
                val layer = searchedLayer.layer

                // Every string in the layer which could have made this match - those which are the
                // match, and then those which end with it. They all have to be looked at, not just
                // the first one found, or a park called "Plazoleta Carlos Pellegrini" hides the
                // station called "Carlos Pellegrini".
                val exactKeys = mutableListOf<Int>()
                val endKeys = mutableListOf<Int>()
                for ((index, normalizedValue) in searchedLayer.normalizedValues.withIndex()) {
                    if (normalizedValue == null) continue
                    val joinedValue = searchedLayer.joinedValues[index] ?: normalizedValue
                    val valueLength = layer.values[index].string_value?.length ?: 0
                    if ((normalizedValue == result.string) || (joinedValue == result.string)) {
                        exactKeys.add(index)
                    } else if ((valueLength > result.string.length) &&
                        ((endOfNormalizedString(normalizedValue, result.string.length) == result.string) ||
                            isEndWithinWords(normalizedValue, result.string) ||
                            ((joinedValue != normalizedValue) && isEndWithinWords(joinedValue, result.string)))
                    ) {
                        endKeys.add(index)
                    }
                }

                var featuresFound = 0
                for (stringKey in exactKeys + endKeys) {
                    if (featuresFound == MAX_FEATURES_PER_LAYER) break
                    // Take the first feature with this name that's within the tile
                    for (feature in layer.features) {
                        if (!featureHasName(feature, searchedLayer.nameTagIndices, stringKey)) continue
                        val featureLocation =
                            featureLocation(feature, result.tileX, result.tileY, location, ruler)
                                ?: continue
                        detailedResults.add(
                            DetailedSearchResult(
                                result.score,
                                layer.values[stringKey].string_value ?: "",
                                featureLocation.location,
                                featureProperties(layer, feature),
                                layer.name,
                                result.houseNumber,
                                featureLocation.distance
                            )
                        )
                        featuresFound++
                        break
                    }
                }
            }
        }

        // Sort the results so far and deduplicate them
        val whittledResults = detailedResults
            .sortedWith { a, b ->
                if (a.score == b.score)
                    a.distance.compareTo(b.distance)
                else
                    a.score.compareTo(b.score)
            }
            .fold(mutableListOf<DetailedSearchResult>()) { accumulator, result ->
                // A feature is found once for each of its names that matches - a cathedral with
                // its name tagged in Spanish, Catalan and Portuguese is found three times - but
                // it's one place, so it only gets one result. That's named with the feature's own
                // name if that was one of the names which matched equally well.
                val sameFeature = accumulator.firstOrNull {
                    (it.layer == result.layer) && (it.properties == result.properties) &&
                        (ruler.distance(it.location, result.location) < 100.0)
                }
                if (sameFeature != null) {
                    if ((result.score == sameFeature.score) && (result.string == result.properties["name"]))
                        sameFeature.string = result.string
                    return@fold accumulator
                }

                // Check if we already have this exact name at approximately the same location
                val isDuplicate = accumulator.any {
                    it.string == result.string && ruler.distance(
                        it.location,
                        result.location
                    ) < 100.0
                }
                if (!isDuplicate) {
                    accumulator.add(result)
                }
                accumulator
            }

        val streetResults = whittledResults.map { result ->
            val mvt = MvtFeature()

            // Copy in the MVT properties. Use the exact name variant that was actually matched
            // during search (which may be a localized name:xx tag rather than plain "name"), so
            // the result surfaces the name that matched rather than always the Latin "name" tag.
            mvt.name = result.string
            mvt.featureClass = result.properties.get("class") as? String?
            mvt.featureSubClass = result.properties.get("subclass") as? String?
            mvt.properties = result.properties
            mvt.geometry = Point(result.location)
            translateProperties(mvt)

            // We've got results, see if we can improve the description from our GridState
            runBlocking {
                withContext(gridState.treeContext) {
                    if (gridState.isLocationWithinGrid(result.location)) {
                        val nearestWay = findNearestNamedWay(
                            result.location,
                            mvt.properties?.get("street") as String?
                        )
                        if (nearestWay != null) {
                            if (mvt.properties?.get("street") == null) {
                                mvt.properties?.set("street", nearestWay.name)
                            }
                            if (result.layer == "transportation") {
                                val sd = StreetDescription(result.string, gridState)
                                sd.createDescription(nearestWay, localizedStrings)
                                val numberResult = sd.getLocationFromStreetNumber(result.houseNumber)
                                if (numberResult != null) {
                                    mvt.properties?.set("housenumber", numberResult.second)
                                    result.location = numberResult.first
                                    mvt.geometry = Point(result.location)
                                    // We want the housenumber to appear in the LocationDescription,
                                    // so unset the name on the feature
                                    mvt.name = null
                                }
                            }
                        }
                    } else {
                        // We could go a step further here and decode the grids around each result,
                        // but that's a lot more work for the phone as it would have to do it for
                        // each result that's outside the current grid. It could likely be easily
                        // run in parallel, and we could perhaps make the results flow back and
                        // update dynamically which would mean that the time taken was less
                        // important.
                        if (result.layer == "transportation") {
                            if (mvt.name != null) {
                                mvt.properties?.set("street", mvt.name)
                            }
                        }
                    }
                    if (settlementGrid.isLocationWithinGrid(result.location)) {

                        // Get the nearest settlements. Nominatim uses the following proximities,
                        // so we do the same:
                        //
                        // cities, municipalities, islands | 15 km
                        // towns, boroughs                 |  4 km
                        // villages, suburbs               |  2 km
                        // hamlets, farms, neighbourhoods  |  1 km
                        //
                        var nearestDistrict: MvtFeature?
                        nearestDistrict = settlementGrid.getFeatureTree(TreeId.SETTLEMENT_HAMLET)
                            .getNearestFeature(
                                location,
                                settlementGrid.ruler,
                                1000.0
                            ) as MvtFeature?
                        if (nearestDistrict?.name == null) {
                            nearestDistrict =
                                settlementGrid.getFeatureTree(TreeId.SETTLEMENT_VILLAGE)
                                    .getNearestFeature(
                                        result.location,
                                        settlementGrid.ruler,
                                        2000.0
                                    ) as MvtFeature?
                            if (nearestDistrict?.name == null) {
                                nearestDistrict =
                                    settlementGrid.getFeatureTree(TreeId.SETTLEMENT_TOWN)
                                        .getNearestFeature(
                                            result.location,
                                            settlementGrid.ruler,
                                            4000.0
                                        ) as MvtFeature?
                                if (nearestDistrict?.name == null) {
                                    nearestDistrict =
                                        settlementGrid.getFeatureTree(TreeId.SETTLEMENT_CITY)
                                            .getNearestFeature(
                                                result.location,
                                                settlementGrid.ruler,
                                                15000.0
                                            ) as MvtFeature?
                                }
                            }
                        }
                        if (nearestDistrict?.name != null) {
                            mvt.properties?.set("city", nearestDistrict.name)
                        }
                    }
                }
            }
            Pair(mvt, result)
        }.fold(mutableListOf<Pair<MvtFeature, DetailedSearchResult>>()) { accumulator, result ->
            // Check if we already have this exact name at approximately the same location
            val isDuplicate = accumulator.any {
                it.second.string == result.second.string && ruler.distance(
                    it.second.location,
                    result.second.location
                ) < 100.0
            }
            if (!isDuplicate) {
                accumulator.add(result)
            }
            accumulator
        }
        try { reader?.close() } catch (_: Exception) {}
        return streetResults.map { (mvt, result) ->
            mvt.toLocationDescription(
                LocationSource.OfflineGeocoder,
                featureName = mvt.getText(localizedStrings)
            )
        }
    }

    companion object {
        // Added to the score of a match made by taking a house number out of the search string
        private const val HOUSE_NUMBER_PENALTY = 0.001

        // How many differently named features one match can find in each layer of a tile
        private const val MAX_FEATURES_PER_LAYER = 4
    }
}

// OSM/OpenMapTiles tags a feature's name under several keys depending on script/language: plain
// "name", BCP-47/ISO-639 language-tagged variants ("name:hi", "name:en", "name:pa", ...), and
// OpenMapTiles-generated fallbacks ("name_int", "int_name"). All of these count as a name tag when
// resolving a fuzzy search hit back to the feature that produced it.
fun isNameKey(key: String): Boolean =
    key == "name" || key.startsWith("name:") || key == "name_int" || key == "int_name"
