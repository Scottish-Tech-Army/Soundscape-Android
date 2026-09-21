package org.scottishtecharmy.soundscape

import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.experimental.categories.Category
import org.scottishtecharmy.soundscape.geoengine.GRID_SIZE
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.MAX_ZOOM_LEVEL
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.utils.PoiRankStrategy
import org.scottishtecharmy.soundscape.geoengine.utils.RankedPoi
import org.scottishtecharmy.soundscape.geoengine.utils.getDistanceToFeature
import org.scottishtecharmy.soundscape.geoengine.utils.omtRank
import org.scottishtecharmy.soundscape.geoengine.utils.orderPoisForSpeech
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Compares the [PoiRankStrategy] prototypes against real map data, so that which one becomes the
 * default is an evidence-based choice rather than a guess. The synthetic guarantees each strategy
 * has to hold regardless of the data are in the shared module's PoiRankingTest.
 *
 * The matrix test prints rather than asserts: it exists to be read. The rest of the tests here
 * pin the things that must not drift while the comparison is going on - above all that
 * [PoiRankStrategy.Off] is bit-for-bit the behaviour that predates any of this.
 */
class PoiRankStrategyTest {

    private data class Site(val label: String, val location: LngLatAlt)

    /**
     * Deliberately spans densities, because rank is a per-tile ordinal and so behaves completely
     * differently depending on how busy the tile is: the same shop is rank 58 in the city centre
     * and rank 2 in a park. POI counts are within AutoCallout's 50m walking search distance.
     */
    private val sites = listOf(
        Site("Glasgow, Hope St", LngLatAlt(-4.2580, 55.8590)),          // dense, 7 within 50m
        Site("Glasgow, Byres Rd", LngLatAlt(-4.2930, 55.8760)),         // dense, 23 within 50m
        Site("Glasgow, Sauchiehall St", LngLatAlt(-4.2660, 55.8655)),   // dense, 13 within 50m
        Site("Milngavie precinct", LngLatAlt(-4.3128, 55.9412)),        // suburban, 2 within 50m
        Site("James Gale Memorial", LngLatAlt(-4.3092868, 55.9503336)), // park, none within 50m
        Site("Kersland Drive", LngLatAlt(-4.3122090, 55.9436836)),      // residential, none
    )

    private fun gridFor(location: LngLatAlt) =
        getGridStateForLocation(location, MAX_ZOOM_LEVEL, GRID_SIZE)

    /**
     * The candidate set OfflineGeocoder's "you are near X" fallback works from - the same query,
     * including the named-POI predicate that makes the 10 item cap count named POIs.
     */
    private fun geocoderCandidates(gridState: GridState, location: LngLatAlt) =
        gridState.getFeatureTree(TreeId.POIS).getNearestCollection(
            location,
            300.0,
            10,
            gridState.ruler,
            include = { !(it as MvtFeature).name.isNullOrEmpty() }
        ).features

    /** The candidate set AutoCallout.buildCalloutForNearbyPOI works from, minus the markers. */
    private fun calloutCandidates(gridState: GridState, location: LngLatAlt) =
        gridState.getFeatureTree(TreeId.SELECTED_SUPER_CATEGORIES).getNearestCollection(
            location,
            UserGeometry(location).getSearchDistance(),
            10,
            gridState.ruler
        ).features

    private fun describe(poi: RankedPoi): String {
        val mvt = poi.feature as MvtFeature
        val name = mvt.name?.takeIf { it.isNotEmpty() } ?: "(${mvt.featureClass ?: "unnamed"})"
        return "$name ${poi.distance.toInt()}m rank=${mvt.omtRank ?: "-"} ${poi.bucket}"
    }

    /**
     * The artefact this whole exercise exists to produce. Read the output, pick a strategy, then
     * change PreferenceDefaults.POI_RANK_STRATEGY to match.
     */
    @Test
    @Category(NightlyOnlyTest::class)
    fun printStrategyComparison() = runBlocking {
        for (site in sites) {
            val gridState = gridFor(site.location)
            val geocoder = geocoderCandidates(gridState, site.location)
            val callout = calloutCandidates(gridState, site.location)
            println("=== ${site.label} (${site.location.longitude}, ${site.location.latitude})")
            println("    ${geocoder.size} geocoder candidates, ${callout.size} callout candidates")
            for (strategy in PoiRankStrategy.entries) {
                val nearby = orderPoisForSpeech(
                    geocoder, site.location, gridState.ruler, strategy
                ).firstOrNull()
                println("    my location  ${strategy.key.padEnd(10)} ${nearby?.let(::describe) ?: "-"}")
            }
            for (strategy in PoiRankStrategy.entries) {
                val top = orderPoisForSpeech(callout, site.location, gridState.ruler, strategy)
                    .take(3)
                    .joinToString(" | ") { describe(it) }
                println("    auto callout ${strategy.key.padEnd(10)} ${top.ifEmpty { "-" }}")
            }
        }
    }

    /**
     * The canary for the whole feature: rank arrives through MvtToGeoJson's untyped property map,
     * so a tile schema change or a new branch in that tag loop could silently turn every strategy
     * into a no-op without anything else failing.
     */
    @Test
    fun rankReachesTheAppFromRealTiles() {
        val gridState = gridFor(LngLatAlt(-4.2580, 55.8590))
        val pois = gridState.getFeatureTree(TreeId.POIS).getAllCollection().features
            .filterIsInstance<MvtFeature>()
        assertTrue(pois.isNotEmpty(), "expected POIs in a central Glasgow grid")

        val ranked = pois.mapNotNull { it.omtRank }
        assertTrue(
            ranked.size > pois.size / 2,
            "expected most POIs to carry a rank, got ${ranked.size} of ${pois.size}"
        )
        // A dense grid should span the whole spread, not sit at a single value.
        assertTrue(ranked.min() <= 5, "expected some prominent POIs, lowest rank was ${ranked.min()}")
        assertTrue(ranked.max() >= 50, "expected some background POIs, highest rank was ${ranked.max()}")
    }

    /**
     * Locks the baseline: Off has to be the plain nearest-first walk the code did before any of
     * this existed, so that turning the prototype off is a genuine no-op rather than approximately
     * one. Compared against a hand-rolled reimplementation rather than against itself.
     */
    @Test
    fun offIsExactlyNearestFirst() {
        for (site in sites) {
            val gridState = gridFor(site.location)
            val candidates = geocoderCandidates(gridState, site.location)

            val expected = candidates
                .map { it to getDistanceToFeature(site.location, it, gridState.ruler).distance }
                .sortedBy { it.second }
                .map { (it.first as MvtFeature).name }

            val actual = orderPoisForSpeech(
                candidates, site.location, gridState.ruler, PoiRankStrategy.Off
            ).map { (it.feature as MvtFeature).name }

            assertEquals(expected, actual, site.label)
        }
    }

    /**
     * The cap used to be applied before the name check, so ten un-named neighbours could hide the
     * eleventh, named POI and drop the geocoder through to its road and settlement fallbacks. The
     * predicate now goes into the query, so every candidate that comes back is usable.
     */
    @Test
    fun geocoderCandidatesAreAllNamed() {
        for (site in sites) {
            val gridState = gridFor(site.location)
            val candidates = geocoderCandidates(gridState, site.location)
            assertTrue(
                candidates.all { !(it as MvtFeature).name.isNullOrEmpty() },
                "${site.label} returned an un-named candidate"
            )
        }
    }

    /**
     * A strategy may reorder or drop candidates, but must never introduce one the tree query
     * didn't return - otherwise it could speak something outside the search radius.
     */
    @Test
    fun noStrategyInventsCandidates() {
        for (site in sites) {
            val gridState = gridFor(site.location)
            val candidates = calloutCandidates(gridState, site.location)
            for (strategy in PoiRankStrategy.entries) {
                val ordered = orderPoisForSpeech(
                    candidates, site.location, gridState.ruler, strategy
                )
                assertTrue(ordered.size <= candidates.size, "${site.label} $strategy")
                assertTrue(ordered.all { it.feature in candidates }, "${site.label} $strategy")
            }
        }
    }
}
