package org.scottishtecharmy.soundscape.geoengine.utils

import org.scottishtecharmy.soundscape.geoengine.mvttranslation.AlongWayFeature
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.AlongWayKind
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayEnd
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayType
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.Ruler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString

/**
 * Converts the result of [Ruler.distanceToLineString] into a distance in metres from the start of
 * that line.
 *
 * [PointAndDistanceAndHeading.positionAlongLine] is a *fractional vertex index* (segment index +
 * how far along that segment the point falls), not a distance, so turning it into metres means
 * walking the vertices up to that segment and adding the fraction of the final one. Shared by
 * StreetDescription (distance along a chain of Ways making up a street) and Way.distanceAlongWay
 * (position of a feature along a single Way).
 */
fun distanceAlongLineString(
    line: LineString,
    pdh: PointAndDistanceAndHeading,
    ruler: Ruler
): Double {
    if (pdh.index < 0 || pdh.positionAlongLine.isNaN()) return 0.0

    var distance = 0.0
    for (i in 0 until pdh.index) {
        distance += ruler.distance(line.coordinates[i], line.coordinates[i + 1])
    }
    distance += (pdh.positionAlongLine - pdh.index) * ruler.distance(
        line.coordinates[pdh.index],
        line.coordinates[pdh.index + 1]
    )
    return distance
}

/**
 * Where something is along the Way network: which Way, how far along it from that Way's START
 * intersection, and which way it's heading.
 *
 * This is the cursor the along-way queries below work from. Holding the user's own position in
 * this form is what lets "what's the next crossing?" and "did I pass anything since the last
 * update?" be answered by walking the Way graph, rather than by a radius search around the user
 * which can't tell the road ahead from the road alongside.
 *
 * @param forwards true when travelling from the Way's START intersection towards its END, false
 * for the reverse, and null when the direction isn't known - stationary, or no travel heading yet.
 * A null direction makes the queries below look both ways rather than guess.
 */
data class WayCursor(
    val way: Way,
    val distanceFromStart: Double,
    val forwards: Boolean?,
)

/**
 * How far a walk along the Way network is willing to follow the road.
 *
 * A single road is split into many Ways - at every junction, and at every tile boundary - so any
 * question about what lies ahead has to decide what counts as "ahead" once the road stops being
 * one Way.
 */
enum class WayContinuation {
    /**
     * Through intersections joining exactly two Ways, stopping at any real junction. The same
     * definition of "straight on" that Way.followWays uses and Street Preview follows, and the
     * honest answer for something the user is about to arrive at: past a junction there is no
     * single road ahead to be looking down.
     */
    STRAIGHT_ON,

    /**
     * Through junctions too, taking whichever Way continues the same road by name or ref. Needed
     * to see any distance up a road that is worth naming: an urban main road has a side street
     * every fifty metres, so STRAIGHT_ON stops almost immediately and a hundred-metre lookahead
     * would never reach anything.
     */
    SAME_ROAD,
}

/**
 * Index of the first entry strictly beyond [distance] in a list sorted by distanceFromStart.
 *
 * Binary search rather than a scan. The lists are short while only crossings are recorded, but
 * this is the primitive transit stops and highway junctions will use too, and a busy road carries
 * a lot more of those than it does bridges.
 */
internal fun List<AlongWayFeature>.firstIndexBeyond(distance: Double): Int {
    var low = 0
    var high = size
    while (low < high) {
        val mid = (low + high).ushr(1)
        if (this[mid].distanceFromStart > distance) high = mid else low = mid + 1
    }
    return low
}

/**
 * The features on this Way strictly beyond [distance], nearest first.
 */
fun Way.alongWayFeaturesAfter(distance: Double): List<AlongWayFeature> =
    alongWayFeatures.subList(alongWayFeatures.firstIndexBeyond(distance), alongWayFeatures.size)

/**
 * The features on this Way at or before [distance], nearest first - so in descending order of
 * distanceFromStart, which is the order they're met travelling END to START.
 */
fun Way.alongWayFeaturesBefore(distance: Double): List<AlongWayFeature> =
    alongWayFeatures.subList(0, alongWayFeatures.firstIndexBeyond(distance)).asReversed()

/** An [AlongWayFeature] found by a query, with how far along the network it is from the cursor. */
data class AlongWayFeatureAhead(
    val feature: AlongWayFeature,
    /** Metres from the querying cursor, following the Ways rather than as the crow flies. */
    val distance: Double,
    /** The Way the feature is recorded on, which needn't be the cursor's own Way. */
    val way: Way,
)

/**
 * Walks the Way network from [cursor] in the direction of travel, calling [action] for each
 * along-way feature met, nearest first, until [maxDistance] is exhausted or [action] returns false.
 *
 * [continuation] decides how far the walk is willing to follow the road - see [WayContinuation].
 *
 * When the cursor's direction is unknown, both directions are walked and the results interleaved
 * by distance, so a caller still gets "how far away is this, along the road" rather than a
 * crow-flies guess.
 */
fun forEachAlongWayFeatureAhead(
    cursor: WayCursor,
    maxDistance: Double,
    continuation: WayContinuation = WayContinuation.STRAIGHT_ON,
    action: (AlongWayFeatureAhead) -> Boolean
) {
    when (cursor.forwards) {
        true, false -> walkOneDirection(cursor, cursor.forwards, maxDistance, continuation, action)
        null -> {
            // Merge the two directions by distance so the nearest feature is still seen first.
            val both = mutableListOf<AlongWayFeatureAhead>()
            walkOneDirection(cursor, true, maxDistance, continuation) { both.add(it); true }
            walkOneDirection(cursor, false, maxDistance, continuation) { both.add(it); true }
            for (found in both.sortedBy { it.distance }) {
                if (!action(found)) return
            }
        }
    }
}

/**
 * The most Ways one walk will cross before giving up, however much [maxDistance] is left. Guards
 * against a pathological chain - a run of zero-length JOINER ways at a tile boundary, say - the
 * same job Way.followWays' depth limit does.
 */
private const val MAX_WAYS_WALKED = 32

private fun walkOneDirection(
    cursor: WayCursor,
    forwards: Boolean,
    maxDistance: Double,
    continuation: WayContinuation,
    action: (AlongWayFeatureAhead) -> Boolean
) {
    // The identity of the road being followed, for WayContinuation.SAME_ROAD. Taken once from the
    // Way the walk starts on: a road keeps its name and ref across the Ways it is split into, and
    // that is what makes them the same road.
    val roadName = cursor.way.name
    val roadRef = cursor.way.ref
    var way = cursor.way
    var stepForwards = forwards
    // Where on the current Way the walk enters it: at the cursor to begin with, then at whichever
    // end we came in by.
    var entry = cursor.distanceFromStart
    var travelled = 0.0
    val visited = mutableSetOf<Way>()

    while (true) {
        if (!visited.add(way)) return
        if (visited.size > MAX_WAYS_WALKED) return

        val features = if (stepForwards) {
            way.alongWayFeaturesAfter(entry)
        } else {
            way.alongWayFeaturesBefore(entry)
        }
        for (feature in features) {
            val distance = travelled + if (stepForwards) {
                feature.distanceFromStart - entry
            } else {
                entry - feature.distanceFromStart
            }
            // Features come out nearest-first and travelled only grows, so nothing later in this
            // walk can be nearer than one that has already overshot.
            if (distance > maxDistance) return
            if (!action(AlongWayFeatureAhead(feature, distance, way))) return
        }

        travelled += if (stepForwards) way.length - entry else entry
        if (travelled > maxDistance) return

        // Walked here rather than by calling Way.followWays because that seeds from the
        // intersection *behind* the first Way, which a Way at the end of the mapped network
        // doesn't have.
        val exit = if (stepForwards) {
            way.intersections[WayEnd.END.id]
        } else {
            way.intersections[WayEnd.START.id]
        } ?: return

        val candidates = exit.members.filter { it !== way }
        val next = when {
            // A pass-through node: one road in, one road out, nothing to choose between.
            candidates.size == 1 -> candidates.first()
            continuation == WayContinuation.STRAIGHT_ON -> return
            // A real junction, and we're following the road rather than stopping at it. Exactly
            // one continuation has to identify itself as the same road, otherwise there's no
            // single answer and guessing would be worse than stopping - which is what a staggered
            // junction of two same-named arms looks like from here.
            else -> {
                val sameRoad = candidates.filter { sameRoad(it, roadName, roadRef) }
                sameRoad.singleOrNull()
                // A JOINER carries no name to match on - it's the synthetic zero-length link
                // across a tile boundary (see GridState.joinTileEdgeIntersections) - so it's the
                // fallback when nothing else here continues the road, not a rival to something
                // that does.
                    ?: candidates.filter { it.wayType == WayType.JOINER }
                        .takeIf { sameRoad.isEmpty() }?.singleOrNull()
                    ?: return
            }
        }

        stepForwards = (next.intersections[WayEnd.START.id] === exit)
        entry = if (stepForwards) 0.0 else next.length
        way = next
    }
}

/** Whether a Way continues the road the walk started on, by name or by route number. */
private fun sameRoad(candidate: Way, name: String?, ref: String?): Boolean {
    if (candidate.wayType == WayType.JOINER) return false
    if ((name != null) && (candidate.name == name)) return true
    if ((ref != null) && (candidate.ref == ref)) return true
    return false
}

/**
 * The next along-way feature of [kind] ahead of [cursor] within [maxDistance], or null.
 *
 * This is the "when is the next crossing?" query - and, once stops and junctions are recorded, the
 * "where is the next bus stop?" one.
 */
fun nextAlongWayFeature(
    cursor: WayCursor,
    maxDistance: Double,
    kind: AlongWayKind? = null,
    continuation: WayContinuation = WayContinuation.STRAIGHT_ON
): AlongWayFeatureAhead? {
    var found: AlongWayFeatureAhead? = null
    forEachAlongWayFeatureAhead(cursor, maxDistance, continuation) {
        if ((kind == null) || (it.feature.kind == kind)) {
            found = it
            false
        } else {
            true
        }
    }
    return found
}
