package org.scottishtecharmy.soundscape.geoengine.utils

import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.createCheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The guarantees [orderPoisForSpeech] has to hold whatever the tile data looks like. These are
 * synthetic rather than tile-driven on purpose: the point is that the rules hold by construction,
 * not that they happen to hold in Glasgow. The comparison against real map data is
 * PoiRankStrategyTest over in the app module, where the offline extracts live.
 */
class PoiRankingTest {

    private val origin = LngLatAlt(-4.3092868, 55.9503336)
    private val ruler = origin.createCheapRuler()

    /** A POI [metres] due north of [origin]. */
    private fun poi(
        name: String,
        metres: Double,
        rank: Int? = null,
        featureClass: String? = "shop",
        superCategory: SuperCategoryId = SuperCategoryId.PLACE
    ) = MvtFeature().also { feature ->
        feature.name = name
        feature.featureClass = featureClass
        feature.superCategory = superCategory
        rank?.let { feature.setProperty("rank", it) }
        feature.geometry = Point(
            LngLatAlt(origin.longitude, origin.latitude + (metres / 110540.0))
        )
    }

    private fun firstName(features: List<MvtFeature>, strategy: PoiRankStrategy) =
        orderPoisForSpeech(features, origin, ruler, strategy)
            .firstOrNull()
            ?.let { (it.feature as MvtFeature).name }

    @Test
    fun omtRankReadsTheUntypedTileProperty() {
        assertEquals(42, poi("shop", 10.0, rank = 42).omtRank)
        // MVT sints arrive as Longs, which is what the tile parser actually produces.
        val fromTile = MvtFeature().also { it.setProperty("rank", 7L) }
        assertEquals(7, fromTile.omtRank)
        assertNull(MvtFeature().omtRank)
    }

    @Test
    fun offIsPlainDistanceOrder() {
        val features = listOf(
            poi("far but prominent", 40.0, rank = 1),
            poi("near but dull", 5.0, rank = 900),
        )
        assertEquals("near but dull", firstName(features, PoiRankStrategy.Off))
    }

    @Test
    fun offScoreIsTheTrueDistance() {
        val ordered = orderPoisForSpeech(
            listOf(poi("a", 5.0, rank = 900), poi("b", 40.0, rank = 1)),
            origin,
            ruler,
            PoiRankStrategy.Off
        )
        ordered.forEach { assertEquals(it.distance, it.score) }
    }

    /**
     * The weighting is penalty-only, so nothing can be promoted past something genuinely nearer.
     * This is what makes it safe to run over a candidate list that has user markers merged into it.
     */
    @Test
    fun weightedNeverPromotesPastSomethingNearer() {
        val features = listOf(
            poi("prominent, further away", 40.0, rank = 1),
            poi("background, right here", 5.0, rank = 900),
        )
        assertEquals("background, right here", firstName(features, PoiRankStrategy.Weighted))
        orderPoisForSpeech(features, origin, ruler, PoiRankStrategy.Weighted)
            .forEach { assertTrue(it.score >= it.distance) }
    }

    @Test
    fun weightedPushesBackTheLeastProminentOfSimilarlyDistantCandidates() {
        val features = listOf(
            poi("background", 20.0, rank = 900),
            poi("ordinary", 24.0, rank = 500),
            poi("prominent", 26.0, rank = 2),
        )
        // Nearest-first would say "background" - 20m beats 26m. Weighted pushes it back by 2.5x
        // (to an effective 50m) and the prominent one, unpenalised at 26m, wins.
        assertEquals("background", firstName(features, PoiRankStrategy.Off))
        assertEquals("prominent", firstName(features, PoiRankStrategy.Weighted))
    }

    /** A marker is somewhere the user saved themselves, and outranks whatever the map thinks. */
    @Test
    fun markersAreNeverDemoted() {
        val marker = poi("my marker", 20.0, superCategory = SuperCategoryId.MARKER)
        val features = listOf(marker, poi("prominent shop", 25.0, rank = 1))
        for (strategy in PoiRankStrategy.entries) {
            assertEquals("my marker", firstName(features, strategy), "strategy $strategy")
        }
    }

    /** ...but a marker isn't promoted either: something genuinely nearer still wins. */
    @Test
    fun markersAreNotPromotedEither() {
        val marker = poi("my marker", 25.0, superCategory = SuperCategoryId.MARKER)
        val features = listOf(marker, poi("prominent shop", 15.0, rank = 1))
        for (strategy in PoiRankStrategy.entries) {
            assertEquals("prominent shop", firstName(features, strategy), "strategy $strategy")
        }
    }

    /**
     * OpenMapTiles ranks the transit classes at the very top of every tile, so left to itself
     * rank-driven selection answers "bus stop" to everything. Transit opts out of ranking instead.
     */
    @Test
    fun transitIsNeutralRatherThanPromoted() {
        val features = listOf(
            poi("Some Bus Stop", 40.0, rank = 1, featureClass = "bus"),
            poi("nearer shop", 10.0, rank = 400),
        )
        for (strategy in PoiRankStrategy.entries) {
            assertEquals("nearer shop", firstName(features, strategy), "strategy $strategy")
        }
    }

    @Test
    fun filterDropsTheLeastProminentCandidates() {
        val features = listOf(
            poi("background", 5.0, rank = 900),
            poi("ordinary", 15.0, rank = 500),
            poi("prominent", 25.0, rank = 2),
        )
        assertEquals("background", firstName(features, PoiRankStrategy.Off))
        assertEquals("ordinary", firstName(features, PoiRankStrategy.Filter))
    }

    /**
     * Somewhere every candidate is street furniture, dropping them all would leave nothing to say,
     * which is worse than saying the least bad thing.
     */
    @Test
    fun filterFallsBackRatherThanSayingNothing() {
        val features = listOf(poi("only candidate", 5.0, rank = 900))
        assertEquals("only candidate", firstName(features, PoiRankStrategy.Filter))
    }

    @Test
    fun rankFirstPrefersProminenceOverDistance() {
        val features = listOf(
            poi("background", 5.0, rank = 900),
            poi("ordinary", 15.0, rank = 500),
            poi("prominent", 25.0, rank = 2),
        )
        assertEquals("prominent", firstName(features, PoiRankStrategy.RankFirst))
    }

    /** Nothing has a rank, so every strategy has to degrade to plain distance order. */
    @Test
    fun unrankedCandidatesFallBackToDistance() {
        val features = listOf(poi("further", 30.0), poi("nearer", 10.0))
        for (strategy in PoiRankStrategy.entries) {
            assertEquals("nearer", firstName(features, strategy), "strategy $strategy")
        }
    }

    @Test
    fun noStrategyInventsOrLosesCandidates() {
        val features = listOf(
            poi("a", 5.0, rank = 900),
            poi("b", 15.0, rank = 500),
            poi("c", 25.0, rank = 2),
        )
        for (strategy in PoiRankStrategy.entries) {
            val ordered = orderPoisForSpeech(features, origin, ruler, strategy)
            // Filter is allowed to drop candidates; nothing may ever add one.
            assertTrue(ordered.size <= features.size, "strategy $strategy")
            assertTrue(ordered.all { it.feature in features }, "strategy $strategy")
        }
    }

    @Test
    fun unknownPreferenceValueDegradesToTheDefault() {
        assertEquals(PoiRankStrategy.default, PoiRankStrategy.fromPreference(null))
        assertEquals(PoiRankStrategy.default, PoiRankStrategy.fromPreference("nonsense"))
        assertEquals(PoiRankStrategy.RankFirst, PoiRankStrategy.fromPreference("rankfirst"))
    }
}
