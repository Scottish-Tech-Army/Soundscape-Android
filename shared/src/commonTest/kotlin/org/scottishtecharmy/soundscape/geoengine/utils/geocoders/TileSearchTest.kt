@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.scottishtecharmy.soundscape.geoengine.utils.geocoders

import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geoengine.utils.getDestinationCoordinate
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Builds a bare TileSearch instance for exercising the pure string-manipulation helpers
 * (addLastWords/generateWithoutSettlement/generateEndOfString/compareAndAddToResults) and
 * findNearestNamedWay. offlineExtractPath is never read by any of those - only by search()
 * (via findExtractPaths), which these tests deliberately don't call - so an empty path is fine.
 */
private fun newTileSearch(gridState: GridState = GridState()): TileSearch =
    TileSearch("", gridState, gridState)

class TileSearchTest {

    // ============================================================================================
    // isNameKey
    // ============================================================================================

    @Test
    fun isNameKey_plainNameKey_isTrue() {
        assertTrue(isNameKey("name"))
    }

    @Test
    fun isNameKey_languageTaggedVariants_areTrue() {
        assertTrue(isNameKey("name:en"))
        assertTrue(isNameKey("name:hi"))
        assertTrue(isNameKey("name:pa"))
        assertTrue(isNameKey("name:zh-Hans"))
    }

    @Test
    fun isNameKey_openMapTilesFallbackKeys_areTrue() {
        assertTrue(isNameKey("name_int"))
        assertTrue(isNameKey("int_name"))
    }

    @Test
    fun isNameKey_unrelatedKeys_areFalse() {
        assertFalse(isNameKey("ref"))
        assertFalse(isNameKey("housenumber"))
        assertFalse(isNameKey("old_name"))
        assertFalse(isNameKey("class"))
    }

    @Test
    fun isNameKey_similarButDistinctKeys_areFalse() {
        // Not exactly "name" and doesn't start with "name:" - these must not be mistaken for a
        // real name tag.
        assertFalse(isNameKey("names"))
        assertFalse(isNameKey("namex"))
        assertFalse(isNameKey("nickname"))
    }

    @Test
    fun isNameKey_isCaseSensitive() {
        // OSM/OpenMapTiles keys are always lowercase, so an uppercase variant should not match.
        assertFalse(isNameKey("Name"))
        assertFalse(isNameKey("NAME:EN"))
    }

    @Test
    fun isNameKey_bareNamePrefixWithNoSuffix_isTrueByPrefixCheck() {
        // Edge case: "name:" alone (no language suffix) satisfies startsWith("name:") even though
        // it isn't a real OSM tag in practice.
        assertTrue(isNameKey("name:"))
    }

    // ============================================================================================
    // addLastWords
    // ============================================================================================

    @Test
    fun addLastWords_joinsRequestedNumberOfTrailingWords() {
        val tileSearch = newTileSearch()
        assertEquals("b c", tileSearch.addLastWords(2, listOf("a", "b", "c")))
    }

    @Test
    fun addLastWords_oneWord_returnsJustTheLastWord() {
        val tileSearch = newTileSearch()
        assertEquals("c", tileSearch.addLastWords(1, listOf("a", "b", "c")))
    }

    @Test
    fun addLastWords_countEqualsListSize_returnsWholeListWithNoStrayWhitespace() {
        val tileSearch = newTileSearch()
        assertEquals("a b c", tileSearch.addLastWords(3, listOf("a", "b", "c")))
    }

    @Test
    fun addLastWords_singleWordList_returnsThatWord() {
        val tileSearch = newTileSearch()
        assertEquals("solo", tileSearch.addLastWords(1, listOf("solo")))
    }

    @Test
    fun addLastWords_countGreaterThanListSize_returnsWordsWithNoLeadingSpace() {
        val tileSearch = newTileSearch()
        // When wordCount exceeds the number of available words, the loop's break condition
        // (`--count == 0`) is never satisfied (count only ever reaches 1 and the words run out),
        // so without a final trim the trailing `result.insert(0, " ")` from the last processed
        // word would remain at the front of the result.
        assertEquals("a b c", tileSearch.addLastWords(5, listOf("a", "b", "c")))
    }

    // ============================================================================================
    // generateWithoutSettlement
    // ============================================================================================

    @Test
    fun generateWithoutSettlement_noSettlementMatches_returnsNull() {
        val tileSearch = newTileSearch()
        assertNull(tileSearch.generateWithoutSettlement("buchanan street", setOf("edinburgh")))
    }

    @Test
    fun generateWithoutSettlement_emptySettlementSet_returnsNull() {
        val tileSearch = newTileSearch()
        assertNull(tileSearch.generateWithoutSettlement("buchanan street glasgow", emptySet()))
    }

    @Test
    fun generateWithoutSettlement_singleWordSettlementMatchesLastWord_stripsIt() {
        val tileSearch = newTileSearch()
        val result = tileSearch.generateWithoutSettlement("buchanan street glasgow", setOf("glasgow"))
        assertEquals("buchanan street", result)
    }

    @Test
    fun generateWithoutSettlement_multiWordSettlementMatchesTrailingWords_stripsAllOfThem() {
        val tileSearch = newTileSearch()
        // "port glasgow" is a two-word settlement name; the single-last-word check ("glasgow"
        // alone) shouldn't match well enough, so this exercises the addLastWords(count, ...) loop
        // that tries progressively longer trailing word-groups.
        val result =
            tileSearch.generateWithoutSettlement("high street port glasgow", setOf("port glasgow"))
        assertEquals("high street", result)
    }

    @Test
    fun generateWithoutSettlement_matchIsFuzzyNotExact() {
        val tileSearch = newTileSearch()
        // One-character typo in the settlement name in the search string ("glasgo" vs "glasgow")
        // should still be recognised via fuzzyCompare's edit-distance tolerance.
        val result = tileSearch.generateWithoutSettlement("buchanan street glasgo", setOf("glasgow"))
        assertEquals("buchanan street", result)
    }

    @Test
    fun generateWithoutSettlement_shortNameCaseMismatchExceedsFuzzyThreshold_doesNotMatch() {
        val tileSearch = newTileSearch()
        // fuzzyCompare is case-sensitive (see StringExtTest.fuzzyCompare_isCaseSensitive), so for a
        // very short settlement name a case mismatch alone is enough edit distance to fail the
        // < 0.25 threshold: "AYR" vs "ayr" is 2 of 3 characters different, well over 0.25.
        assertNull(tileSearch.generateWithoutSettlement("main street AYR", setOf("ayr")))
    }

    @Test
    fun generateWithoutSettlement_stringIsExactlyASingleWordSettlementName_returnsEmpty() {
        val tileSearch = newTileSearch()
        // When the settlement match consumes every word (wordTarget == 0, i.e. the search string
        // is nothing but the settlement name), nothing should be left after stripping it.
        val result = tileSearch.generateWithoutSettlement("glasgow", setOf("glasgow"))
        assertEquals("", result)
    }

    @Test
    fun generateWithoutSettlement_firstMatchingSettlementNameWins() {
        val tileSearch = newTileSearch()
        // Only "glasgow" actually matches the trailing word; "edinburgh" and "dundee" don't, and
        // must not prevent the real match from being found regardless of Set iteration order.
        val result = tileSearch.generateWithoutSettlement(
            "buchanan street glasgow",
            setOf("edinburgh", "dundee", "glasgow")
        )
        assertEquals("buchanan street", result)
    }

    // ============================================================================================
    // generateEndOfString
    // ============================================================================================

    @Test
    fun generateEndOfString_maxLengthReachedAfterFirstWord_returnsJustThatWord() {
        val tileSearch = newTileSearch()
        // "street " (with the loop's trailing space) is exactly 7 characters - hitting the
        // maxLength=6 threshold as soon as that one word is appended.
        assertEquals("street", tileSearch.generateEndOfString("Buchanan Street", 6))
    }

    @Test
    fun generateEndOfString_maxLengthRequiresTwoWords_pullsInPrecedingWord() {
        val tileSearch = newTileSearch()
        assertEquals("buchanan street", tileSearch.generateEndOfString("Buchanan Street", 8))
    }

    @Test
    fun generateEndOfString_maxLengthLongerThanWholeString_returnsWholeNormalizedString() {
        val tileSearch = newTileSearch()
        assertEquals("buchanan street", tileSearch.generateEndOfString("Buchanan Street", 100))
    }

    @Test
    fun generateEndOfString_normalizesCaseAccentsAndPunctuationFirst() {
        val tileSearch = newTileSearch()
        assertEquals("bar", tileSearch.generateEndOfString("Café-Bar", 3))
    }

    @Test
    fun generateEndOfString_zeroMaxLength_stillReturnsAtLeastOneWord() {
        val tileSearch = newTileSearch()
        assertEquals("world", tileSearch.generateEndOfString("hello world", 0))
    }

    @Test
    fun generateEndOfString_emptyInput_returnsEmptyString() {
        val tileSearch = newTileSearch()
        assertEquals("", tileSearch.generateEndOfString("", 10))
    }

    @Test
    fun generateEndOfString_singleWordInput_returnsThatWord() {
        val tileSearch = newTileSearch()
        assertEquals("solo", tileSearch.generateEndOfString("Solo", 20))
    }

    // ============================================================================================
    // compareAndAddToResults
    // ============================================================================================

    @Test
    fun compareAndAddToResults_exactMatch_isAddedAndReturnsTrue() {
        val tileSearch = newTileSearch()
        val results = mutableListOf<TileSearch.TileSearchResult>()

        val added = tileSearch.compareAndAddToResults("house", "house", results, 8, 1, 2)

        assertTrue(added)
        assertEquals(1, results.size)
        assertEquals(0.0, results[0].score)
        assertEquals("house", results[0].string)
        assertEquals(1, results[0].tileX)
        assertEquals(2, results[0].tileY)
    }

    @Test
    fun compareAndAddToResults_scoreAtOrAboveThreshold_isNotAddedAndReturnsFalse() {
        val tileSearch = newTileSearch()
        val results = mutableListOf<TileSearch.TileSearchResult>()

        // "cat" vs "dog" - completely different 3-letter words, score 1.0, well over the 0.25 cutoff.
        val added = tileSearch.compareAndAddToResults("cat", "dog", results, 8, 0, 0)

        assertFalse(added)
        assertTrue(results.isEmpty())
    }

    @Test
    fun compareAndAddToResults_whenLimitFilledWithBetterMatches_rejectsWorseMatchWithoutInserting() {
        val tileSearch = newTileSearch()
        val results = mutableListOf<TileSearch.TileSearchResult>()
        val limit = 2

        assertTrue(tileSearch.compareAndAddToResults("house", "house", results, limit, 0, 0))  // score 0.0
        assertTrue(tileSearch.compareAndAddToResults("house", "houses", results, limit, 0, 0)) // score 0.01

        // Both existing entries already score better than "horse" would (0.2), so with the limit
        // already "full" of better matches this is rejected outright - it's never even inserted.
        val added = tileSearch.compareAndAddToResults("house", "horse", results, limit, 0, 0)

        assertFalse(added)
        assertEquals(2, results.size)
        assertEquals(setOf("house", "houses"), results.map { it.string }.toSet())
    }

    @Test
    fun compareAndAddToResults_betterMatchArrivingAfterListIsFull_trimsOutWorstEntry() {
        val tileSearch = newTileSearch()
        val results = mutableListOf<TileSearch.TileSearchResult>()
        val limit = 2

        assertTrue(tileSearch.compareAndAddToResults("house", "horse", results, limit, 0, 0))  // score 0.2
        assertTrue(tileSearch.compareAndAddToResults("house", "houses", results, limit, 0, 0)) // score 0.01

        // Unlike the rejection case above, here the new entry ("house", score 0.0) is better than
        // an *existing* entry ("horse", score 0.2), so it's inserted and the list is then trimmed
        // back down to the limit by dropping the worst entry.
        val added = tileSearch.compareAndAddToResults("house", "house", results, limit, 0, 0)

        assertTrue(added)
        assertEquals(2, results.size)
        assertEquals(listOf("house", "houses"), results.map { it.string })
        assertEquals(listOf(0.0, 0.01), results.map { it.score })
    }

    @Test
    fun compareAndAddToResults_resultsListStaysSortedByScoreAscending() {
        val tileSearch = newTileSearch()
        val results = mutableListOf<TileSearch.TileSearchResult>()
        val limit = 8

        tileSearch.compareAndAddToResults("house", "houses", results, limit, 0, 0) // 0.01
        tileSearch.compareAndAddToResults("house", "house", results, limit, 0, 0)  // 0.0
        tileSearch.compareAndAddToResults("house", "horse", results, limit, 0, 0)  // 0.2

        assertEquals(listOf("house", "houses", "horse"), results.map { it.string })
        assertEquals(true, results.zipWithNext().all { (a, b) -> a.score <= b.score })
    }

    // ============================================================================================
    // findNearestNamedWay
    // ============================================================================================

    private class NamedWayFixture(val gridState: GridState, val origin: LngLatAlt)

    /**
     * Builds a GridState whose ROADS tree contains three short Ways running due north from
     * [origin]: an unnamed one 5m away, "Low Street" 15m away and "High Street" 20m away - close
     * enough together that all three are within findNearestNamedWay's hardcoded 100m/10-result
     * search, but far enough apart that distance-sorted order is unambiguous.
     */
    private fun buildNamedWayFixture(): NamedWayFixture {
        val origin = LngLatAlt(-2.657, 51.430)
        val gridState = GridState()
        gridState.validateContext = false
        gridState.ruler = CheapRuler(origin.latitude)

        val unnamed = Way().apply {
            geometry = LineString(
                getDestinationCoordinate(origin, 0.0, 5.0),
                getDestinationCoordinate(origin, 0.0, 10.0)
            )
        }
        val lowStreet = Way().apply {
            name = "Low Street"
            geometry = LineString(
                getDestinationCoordinate(origin, 0.0, 15.0),
                getDestinationCoordinate(origin, 0.0, 18.0)
            )
        }
        val highStreet = Way().apply {
            name = "High Street"
            geometry = LineString(
                getDestinationCoordinate(origin, 0.0, 20.0),
                getDestinationCoordinate(origin, 0.0, 23.0)
            )
        }

        gridState.featureTrees[TreeId.ROADS.id] = FeatureTree(
            FeatureCollection().apply {
                addFeature(unnamed)
                addFeature(lowStreet)
                addFeature(highStreet)
            }
        )

        return NamedWayFixture(gridState, origin)
    }

    @Test
    fun findNearestNamedWay_nameNull_returnsNearestWayThatHasAnyName() {
        val fixture = buildNamedWayFixture()
        val tileSearch = newTileSearch(fixture.gridState)

        // The closest way of all (5m) is unnamed, so the nearest *named* way is "Low Street"
        // (15m) even though "High Street" exists further away.
        val result = tileSearch.findNearestNamedWay(fixture.origin, null)

        assertNotNull(result)
        assertEquals("Low Street", result.name)
    }

    @Test
    fun findNearestNamedWay_specificNameRequested_returnsExactMatchRegardlessOfDistance() {
        val fixture = buildNamedWayFixture()
        val tileSearch = newTileSearch(fixture.gridState)

        val result = tileSearch.findNearestNamedWay(fixture.origin, "High Street")

        assertNotNull(result)
        assertEquals("High Street", result.name)
    }

    @Test
    fun findNearestNamedWay_nameWithNoMatchingWay_returnsNull() {
        val fixture = buildNamedWayFixture()
        val tileSearch = newTileSearch(fixture.gridState)

        assertNull(tileSearch.findNearestNamedWay(fixture.origin, "Nonexistent Street"))
    }

    @Test
    fun findNearestNamedWay_nameMatchIsCaseSensitiveExactEquality() {
        val fixture = buildNamedWayFixture()
        val tileSearch = newTileSearch(fixture.gridState)

        // No fuzzy matching here (unlike compareAndAddToResults/generateWithoutSettlement) - a
        // case difference is a plain, exact non-match.
        assertNull(tileSearch.findNearestNamedWay(fixture.origin, "high street"))
    }

    @Test
    fun findNearestNamedWay_wayOutsideHundredMetreRadius_isNotFound() {
        val origin = LngLatAlt(-2.657, 51.430)
        val gridState = GridState()
        gridState.validateContext = false
        gridState.ruler = CheapRuler(origin.latitude)

        val farWay = Way().apply {
            name = "Far Street"
            geometry = LineString(
                getDestinationCoordinate(origin, 0.0, 150.0),
                getDestinationCoordinate(origin, 0.0, 160.0)
            )
        }
        gridState.featureTrees[TreeId.ROADS.id] = FeatureTree(
            FeatureCollection().apply { addFeature(farWay) }
        )
        val tileSearch = newTileSearch(gridState)

        assertNull(tileSearch.findNearestNamedWay(origin, "Far Street"))
    }

    @Test
    fun findNearestNamedWay_emptyRoadsTree_returnsNull() {
        val gridState = GridState()
        gridState.validateContext = false
        val tileSearch = newTileSearch(gridState)

        assertNull(tileSearch.findNearestNamedWay(LngLatAlt(-2.657, 51.430), null))
    }

    // ============================================================================================
    // refreshOfflineMaps
    // ============================================================================================

    @Test
    fun refreshOfflineMaps_clearsStringCache() {
        val tileSearch = newTileSearch()
        tileSearch.stringCache[1L] = listOf("some cached string")
        assertTrue(tileSearch.stringCache.isNotEmpty())

        tileSearch.refreshOfflineMaps()

        assertTrue(tileSearch.stringCache.isEmpty())
    }

    @Test
    fun refreshOfflineMaps_onAlreadyEmptyCache_isANoOp() {
        val tileSearch = newTileSearch()
        assertTrue(tileSearch.stringCache.isEmpty())

        tileSearch.refreshOfflineMaps()

        assertTrue(tileSearch.stringCache.isEmpty())
    }
}
