package org.scottishtecharmy.soundscape.geoengine.utils

import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.Ruler

/**
 * OpenMapTiles' `rank` attribute for a POI, or null if the tile didn't carry one.
 *
 * This is a *per-tile ordinal*, not a global importance score. It restarts at 1 in every zoom 14
 * tile, ordered by OpenMapTiles' class importance with un-named POIs pushed to the end, so a dense
 * city tile runs 1..1000+ while a suburban one runs 1..150 and a park runs 1..11. Measured on our
 * own Glasgow extract, the median rank of a *named* POI is 58 in the city centre, 20 in Milngavie
 * and 2 in a park - the same shop is a different number depending on how busy its tile is. Raw
 * values are therefore never comparable between tiles, which is why nothing here compares them
 * against fixed thresholds; see [bucketsFor].
 *
 * It arrives as an MVT sint in the untyped property map, because MvtToGeoJson's tag loop only names
 * name/ref/class/subclass/housenumber/street and puts everything else in `properties`. Nothing
 * needs to change there for this to work, and MvtFeature.copyProperties()'s callers all copy the
 * property map alongside it, so rank survives being duplicated into entrances and merged polygons.
 */
val MvtFeature.omtRank: Int?
    get() = when (val rank = properties?.get("rank")) {
        is Int -> rank
        is Number -> rank.toInt()
        else -> null
    }

/**
 * How much of a say [omtRank] gets when choosing which POI to speak.
 *
 * The strategies exist to be compared against each other on real map data - see
 * PoiRankStrategyTest - and one of them will become the default once there's evidence for it.
 */
enum class PoiRankStrategy(val key: String) {
    /** Distance only: exactly the behaviour that predates any of this. */
    Off("off"),

    /** Drop the least prominent candidates, then nearest-first as before. */
    Filter("filter"),

    /** Nearest-first, but with the least prominent candidates pushed back. */
    Weighted("weighted"),

    /** Most prominent first, distance only breaking ties. */
    RankFirst("rankfirst");

    companion object {
        /**
         * The fallback for an unrecognised preference value, and for callers with no preferences
         * at all - which is every test that builds an AutoCallout or OfflineGeocoder directly.
         * Kept the same as PreferenceDefaults.POI_RANK_STRATEGY on purpose, so that the callout
         * fixtures exercise what actually ships rather than a strategy nothing uses.
         */
        val default = Off
        val keys = entries.map { it.key }

        fun fromPreference(value: String?): PoiRankStrategy =
            entries.firstOrNull { it.key == value } ?: default
    }
}

/**
 * Where a candidate sits in the prominence order of the candidates it's being compared against.
 */
enum class RankBucket {
    /** No usable rank, or deliberately exempt from ranking. Always treated as prominent. */
    Unranked,
    Prominent,
    Ordinary,
    Background
}

/**
 * OpenMapTiles puts the transit classes at the very top of every tile's ranking, so taking rank at
 * face value makes the answer to "what's near me" a bus stop every single time - measured over
 * central Glasgow, ranking by rank alone returned a bus stop in all four quadrants. Transit is
 * therefore exempted rather than promoted. It matters at both call sites: TreeId.POIS has
 * TRANSIT_STOPS folded into it, and TreeId.SELECTED_SUPER_CATEGORIES includes the mobility POIs.
 */
private val unrankedClasses = setOf("railway", "bus", "ferry", "ferry_terminal", "aerialway")

/**
 * A candidate POI with the numbers used to order it.
 */
data class RankedPoi(
    val feature: Feature,
    /**
     * True distance in metres. Callers gating on a range - AutoCallout's per-category trigger
     * distances, say - must use this and never [score], which is a sort key rather than a distance.
     */
    val distance: Double,
    val bucket: RankBucket,
    /** Ascending sort key. Equal to [distance] for every strategy but [PoiRankStrategy.Weighted]. */
    val score: Double
)

/**
 * Buckets the candidates *relative to each other* rather than against fixed rank values.
 *
 * Fixed thresholds don't survive contact with the data. The obvious ones to reuse are the
 * boundaries MapStyleBuilder renders with (rank < 7 at z14, < 20 at z15), but measured at six real
 * locations at AutoCallout's 50m search distance they changed the chosen POI in none of them:
 * ordinary high street POIs sit at rank 27-121 in a dense tile, so everything in range lands in the
 * same bucket and the weighting cancels out. Splitting the candidates in hand into thirds adapts to
 * whatever density the caller is actually standing in, and needs nothing computed at tile load.
 *
 * The cost is that there's always a least prominent third, even where every candidate is genuinely
 * interesting - [PoiRankStrategy.Filter] will still drop some of them.
 */
private const val MINIMUM_RANKED_CANDIDATES = 3

private fun bucketsFor(candidates: List<Pair<Feature, Double>>): Map<Feature, RankBucket> {
    val ranks = mutableMapOf<Feature, Int>()
    for ((feature, _) in candidates) {
        val mvt = feature as? MvtFeature ?: continue
        // A marker is somewhere the user saved themselves. It has no rank, and must never be
        // demoted in favour of something the map thinks is more important.
        if (mvt.superCategory == SuperCategoryId.MARKER) continue
        if (mvt.featureClass in unrankedClasses) continue
        ranks[feature] = mvt.omtRank ?: continue
    }
    // Thirds of one or two candidates aren't thirds of anything: the integer division puts every
    // ranked candidate in the bottom bucket, which would have Filter throw away the only ranked
    // things it was given and answer with whatever unranked candidate was left. Below the minimum,
    // nothing is bucketed and every strategy degrades to plain distance order.
    if (ranks.size < MINIMUM_RANKED_CANDIDATES) {
        return candidates.associate { it.first to RankBucket.Unranked }
    }

    val ordered = ranks.entries.sortedBy { it.value }
    val third = ordered.size / 3
    val positions = ordered.withIndex().associate { (position, entry) -> entry.key to position }
    return candidates.associate { (feature, _) ->
        val position = positions[feature]
        feature to when {
            position == null -> RankBucket.Unranked
            position < third -> RankBucket.Prominent
            position < third * 2 -> RankBucket.Ordinary
            else -> RankBucket.Background
        }
    }
}

/**
 * Weights are never above 1.0, so an effective distance of `distance / weight` is always at least
 * the true distance. That makes the weighting penalty-only: a candidate can be pushed back, but
 * never pulled in front of something that's genuinely nearer. Everything unranked - user markers,
 * transit, POIs from tiles with no rank at all - weighs 1.0 and so keeps its true distance, which
 * is what guarantees a marker can only ever be beaten by something closer than it.
 */
private fun weightFor(bucket: RankBucket) = when (bucket) {
    RankBucket.Unranked, RankBucket.Prominent -> 1.0
    RankBucket.Ordinary -> 0.7
    RankBucket.Background -> 0.4
}

private fun tierFor(bucket: RankBucket) = when (bucket) {
    RankBucket.Unranked, RankBucket.Prominent -> 0
    RankBucket.Ordinary -> 1
    RankBucket.Background -> 2
}

/**
 * Orders POI candidates for "which of these is worth speaking?".
 *
 * The input is expected to be the output of a nearest-first tree query; [PoiRankStrategy.Off]
 * returns it in plain distance order, so it reproduces the behaviour of every caller that used to
 * walk the query results directly. No strategy adds a candidate the query didn't return.
 */
fun orderPoisForSpeech(
    features: Iterable<Feature>,
    location: LngLatAlt,
    ruler: Ruler,
    strategy: PoiRankStrategy
): List<RankedPoi> {
    val withDistances = features.map { it to getDistanceToFeature(location, it, ruler).distance }
    if (strategy == PoiRankStrategy.Off) {
        return withDistances
            .sortedBy { it.second }
            .map { (feature, distance) ->
                RankedPoi(feature, distance, RankBucket.Unranked, distance)
            }
    }

    val buckets = bucketsFor(withDistances)
    val ranked = withDistances.map { (feature, distance) ->
        val bucket = buckets[feature] ?: RankBucket.Unranked
        val score = if (strategy == PoiRankStrategy.Weighted) distance / weightFor(bucket) else distance
        RankedPoi(feature, distance, bucket, score)
    }

    return when (strategy) {
        PoiRankStrategy.Filter -> {
            val kept = ranked.filter { it.bucket != RankBucket.Background }
            // Somewhere where everything nearby is street furniture would otherwise have nothing
            // left to say at all, which is worse than saying the least bad thing.
            (kept.ifEmpty { ranked }).sortedBy { it.score }
        }

        PoiRankStrategy.RankFirst ->
            ranked.sortedWith(compareBy({ tierFor(it.bucket) }, { it.distance }))

        else -> ranked.sortedBy { it.score }
    }
}

/**
 * The single best candidate, or null if [accept] rules them all out. [accept] is applied after
 * ordering, so a caller wanting "the best *named* POI" gets the best named one rather than nothing
 * when the best one happens to be un-named.
 */
fun bestPoiForSpeech(
    features: Iterable<Feature>,
    location: LngLatAlt,
    ruler: Ruler,
    strategy: PoiRankStrategy,
    accept: (Feature) -> Boolean = { true }
): RankedPoi? = orderPoisForSpeech(features, location, ruler, strategy).firstOrNull { accept(it.feature) }
