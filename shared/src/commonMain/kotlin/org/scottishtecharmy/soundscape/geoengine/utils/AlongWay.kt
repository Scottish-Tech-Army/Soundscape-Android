package org.scottishtecharmy.soundscape.geoengine.utils

import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.AlongWayFeature
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.AlongWayKind
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Intersection
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayEnd
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayType
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.Ruler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.i18n.LocalizedStrings

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
 * Binary search rather than a scan. A busy road carries a lot more transit stops and highway
 * junctions than it does bridges.
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
 * Index of the first entry at or beyond [distance] - the inclusive counterpart of
 * [firstIndexBeyond], for a [distance] which is a Way boundary rather than the user's position.
 */
internal fun List<AlongWayFeature>.firstIndexAtOrBeyond(distance: Double): Int {
    var low = 0
    var high = size
    while (low < high) {
        val mid = (low + high).ushr(1)
        if (this[mid].distanceFromStart >= distance) high = mid else low = mid + 1
    }
    return low
}

/**
 * The features on this Way beyond [distance], nearest first.
 *
 * Exclusive by default, so a feature exactly at the user's position counts as passed rather than
 * still ahead. [inclusive] is for the other caller: a walk entering this Way at one of its ends,
 * where [distance] is that boundary and a feature sitting exactly on it has not been reached yet.
 */
fun Way.alongWayFeaturesAfter(
    distance: Double,
    inclusive: Boolean = false
): List<AlongWayFeature> {
    val from = if (inclusive) {
        alongWayFeatures.firstIndexAtOrBeyond(distance)
    } else {
        alongWayFeatures.firstIndexBeyond(distance)
    }
    return alongWayFeatures.subList(from, alongWayFeatures.size)
}

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
    /**
     * Whether the walk is travelling [way]'s own START-to-END direction where it met the feature.
     *
     * Not the same as the cursor's own direction once the walk has left the Way it started on: the
     * Ways a road is split into aren't all digitised the same way round, so a walk that is going
     * forwards along one can be going backwards along the next. Anything recorded relative to a
     * Way's own direction - [AlongWayFeature.side] above all - has to be read against this rather
     * than against the cursor.
     */
    val forwards: Boolean,
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
    // A slip road is not the road it leaves or joins, even when it carries the same ref - and on
    // a motorway it usually does, so without this every junction would offer two continuations
    // and the walk would stop at the first one. Unless the walk starts on a slip road, when it is
    // the thing being followed.
    val followingRamp = cursor.way.isRamp
    var way = cursor.way
    var stepForwards = forwards
    // Where on the current Way the walk enters it: at the cursor to begin with, then at whichever
    // end we came in by.
    var entry = cursor.distanceFromStart
    var travelled = 0.0
    // The walk starts at the user, where a feature exactly at the cursor has been passed. Every
    // Way after this one is entered at one of its ends instead, and a feature sitting exactly on
    // that boundary is still ahead - a railway=stop on the node where a line is split, say, which
    // would otherwise be invisible to the walk arriving from the previous piece.
    var entryIsBoundary = false
    val visited = mutableSetOf<Way>()

    while (true) {
        if (!visited.add(way)) return
        if (visited.size > MAX_WAYS_WALKED) return

        val features = if (stepForwards) {
            way.alongWayFeaturesAfter(entry, inclusive = entryIsBoundary)
        } else {
            // Already inclusive of its own boundary: alongWayFeaturesBefore takes everything at
            // or before the entry, which on a backwards entry is the Way's END.
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
            if (!action(AlongWayFeatureAhead(feature, distance, way, stepForwards))) return
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
                val sameRoad = candidates.filter {
                    sameRoad(it, roadName, roadRef) && (followingRamp || !it.isRamp)
                }
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
        entryIsBoundary = true
        way = next
    }
}

/**
 * Whether a Way continues the road the walk started on, by name or by route number.
 *
 * Internal rather than private: IntersectionUtils' legacy (Street-Preview/no-map-match) junction
 * search shares this same-road test, so a two-arm junction is judged a pass-through by the same
 * rule whichever search finds it.
 */
internal fun sameRoad(candidate: Way, name: String?, ref: String?): Boolean {
    if (candidate.wayType == WayType.JOINER) return false
    if ((name != null) && (candidate.name == name)) return true
    if ((ref != null) && (candidate.ref == ref)) return true
    return false
}

/**
 * Whether two Ways are pieces of the same OSM way. WayGenerator splits a way at every node another
 * way joins, and each piece keeps the way's osmId.
 *
 * A tile-edge JOINER carries no osmId of its own, but it is the zero-length link between the two
 * halves of a way cut by a tile boundary (see GridState.joinTileEdgeIntersections), so it counts
 * as the same way as whatever it joins. Joiners are paired by location rather than by way, so in
 * the rare case that a different way ends within a metre of the boundary on the other side, that
 * is treated as the same road too. Without this, the far side of every tile boundary
 * would compare a road against a nameless joiner, and a side road or unnamed road carrying on
 * across the boundary would be reported there as a junction.
 */
private fun sameOsmWay(a: Way, b: Way): Boolean {
    if ((a.wayType == WayType.JOINER) || (b.wayType == WayType.JOINER)) return true
    return (a.osmId != 0L) && (a.osmId == b.osmId)
}

/**
 * An Intersection found by a junction-lookahead walk, with how far along the network it is from
 * the querying cursor.
 */
data class IntersectionAhead(
    val intersection: Intersection,
    /** Metres from the querying cursor, following the Ways rather than as the crow flies. */
    val distance: Double,
    /**
     * The Way the walk arrived on - by construction a member of [intersection], so it is directly
     * usable as the approach road, with no need to re-derive it afterwards.
     */
    val arrivingWay: Way,
    /** Whether the walk was travelling [arrivingWay]'s own START-to-END direction on arrival. */
    val arrivingForwards: Boolean,
    /**
     * The junctions the walk reported on its way here, nearest first. A junction can't be reached
     * without passing through these, so they come before it whatever their crow-fly distances.
     */
    val via: List<Intersection> = emptyList(),
)

/**
 * Walks the Way network ahead of [cursor], calling [action] for every "real" junction reached -
 * see [Way.isJunctionArm] - until [maxDistance] is exhausted, the road runs out, or [action]
 * returns false.
 *
 * A node is only reported when it has at least two arms that pass [Way.isJunctionArm], or exactly
 * one whose name/ref continues neither the road the walk started on nor the one it arrived on.
 * Anything short of that - a plain
 * pass-through, a tile-edge JOINER, or a node that exists only for a sidewalk/crossing stub off
 * the road - is walked through transparently, the same way [forEachAlongWayFeatureAhead] already
 * treats a plain two-Way node.
 *
 * When the cursor's direction is unknown, both directions are walked and the results interleaved
 * by distance, matching [forEachAlongWayFeatureAhead].
 */
fun forEachIntersectionAhead(
    cursor: WayCursor,
    maxDistance: Double,
    gridState: GridState,
    strings: LocalizedStrings?,
    continuation: WayContinuation = WayContinuation.SAME_ROAD,
    action: (IntersectionAhead) -> Boolean
) {
    when (cursor.forwards) {
        true, false -> walkIntersectionsOneDirection(
            cursor, cursor.forwards, maxDistance, gridState, strings, continuation, action
        )
        null -> {
            // Merge the two directions by distance so the nearest junction is still seen first.
            val both = mutableListOf<IntersectionAhead>()
            walkIntersectionsOneDirection(cursor, true, maxDistance, gridState, strings, continuation) {
                both.add(it); true
            }
            walkIntersectionsOneDirection(cursor, false, maxDistance, gridState, strings, continuation) {
                both.add(it); true
            }
            for (found in both.sortedBy { it.distance }) {
                if (!action(found)) return
            }
        }
    }
}

/**
 * Unlike [walkOneDirection] - which gives up rather than guess when it cannot identify a single
 * road to keep following (a staggered junction of two same-named arms, say) - a junction-lookahead
 * walk cannot afford to give up. It doesn't know which way the user will actually go, and roads
 * fork into same-named one-way carriageways or quietly rename at an unremarkable node constantly;
 * either would silently cut the search short of the junction actually being asked about (see the
 * "complex intersection" fixtures in app/src/test/.../ComplexIntersections.kt, and
 * [collectIntersectionsAhead]'s own comment). So at each junction it passes through it carries on
 * along every arm the user would take by not turning - each arm continuing the road, so both
 * carriageways where it splits, or else the arm nearest straight on - gathering everything found
 * within [maxDistance] and merging the results by distance. Side roads aren't walked: a junction
 * down one is only reached by turning, so it's never the next junction ahead. It never steps from
 * a road onto a pavement or crossing either, only across tile joiners, though a walk starting on a
 * path does. [MAX_WAYS_WALKED] bounds
 * the total.
 */
private fun walkIntersectionsOneDirection(
    cursor: WayCursor,
    forwards: Boolean,
    maxDistance: Double,
    gridState: GridState,
    strings: LocalizedStrings?,
    continuation: WayContinuation,
    action: (IntersectionAhead) -> Boolean
) {
    // Keyed by the Intersection's own reference identity - see WayGenerator's comment on why it
    // has none of its own - because the exhaustive fan-out below can reach the same physical
    // junction by more than one path (a small loop of short same-named arms, say), and scoring it
    // is only meaningful once, from whichever arrival is nearest.
    val found = LinkedHashMap<Intersection, IntersectionAhead>()
    collectIntersectionsAhead(
        way = cursor.way,
        stepForwards = forwards,
        entry = cursor.distanceFromStart,
        travelled = 0.0,
        // The identity of the road being followed, for WayContinuation.SAME_ROAD - see
        // walkOneDirection. Taken once, from the Way the walk starts on.
        roadName = cursor.way.name,
        roadRef = cursor.way.ref,
        maxDistance = maxDistance,
        gridState = gridState,
        strings = strings,
        continuation = continuation,
        onPath = cursor.way.isSidewalkOrCrossing() || cursor.way.isPath(),
        found = found,
    )
    for (result in found.values.sortedBy { it.distance }) {
        if (!action(result)) return
    }
}

/** One Way still to be walked by [collectIntersectionsAhead], and how the walk got to it. */
private class PendingWay(
    val way: Way,
    val stepForwards: Boolean,
    val entry: Double,
    /** Metres walked before stepping on to [way]. */
    val travelled: Double,
    /** The junctions reported on the way here, nearest first - see [IntersectionAhead.via]. */
    val via: List<Intersection>,
)

/**
 * Walks every Way reachable ahead of the starting one, nearest first: always taking next the
 * pending Way the walk reached soonest. Each Way is walked once - the shared visited set is what
 * keeps a fork that rejoins itself (a short one-way loop around a block, say) from being walked
 * forever, and [MAX_WAYS_WALKED] is a budget on the whole search, not on any one path through it -
 * so taking them nearest first means each is walked from its nearest approach. Depth first, the
 * first branch could go all the way round a loop and use up the Ways which were the direct route
 * to a junction - the other carriageway of a road which splits in two before meeting another, say
 * - so the junction would only be recorded as reached the long way round, through others.
 */
private fun collectIntersectionsAhead(
    way: Way,
    stepForwards: Boolean,
    entry: Double,
    travelled: Double,
    roadName: String?,
    roadRef: String?,
    maxDistance: Double,
    gridState: GridState,
    strings: LocalizedStrings?,
    continuation: WayContinuation,
    onPath: Boolean,
    found: MutableMap<Intersection, IntersectionAhead>,
) {
    val visited = mutableSetOf<Way>()
    val pending = mutableListOf(PendingWay(way, stepForwards, entry, travelled, emptyList()))
    while (pending.isNotEmpty()) {
        val next = pending.minBy { it.travelled }
        pending.remove(next)
        if (!visited.add(next.way)) continue
        if (visited.size > MAX_WAYS_WALKED) return
        walkOneWayForIntersections(
            next, roadName, roadRef, maxDistance, gridState, strings, continuation, onPath,
            found, pending
        )
    }
}

/**
 * Walks [step]'s Way to its far end, records the junction there if it's a real one, and queues
 * the Ways leading on from it in [pending].
 */
private fun walkOneWayForIntersections(
    step: PendingWay,
    roadName: String?,
    roadRef: String?,
    maxDistance: Double,
    gridState: GridState,
    strings: LocalizedStrings?,
    continuation: WayContinuation,
    onPath: Boolean,
    found: MutableMap<Intersection, IntersectionAhead>,
    pending: MutableList<PendingWay>,
) {
    val way = step.way
    val stepForwards = step.stepForwards
    val via = step.via
    val travelledToExit = step.travelled + if (stepForwards) way.length - step.entry else step.entry
    if (travelledToExit > maxDistance) return

    // Walked here rather than by calling Way.followWays for the same reason walkOneDirection is:
    // that seeds from the intersection behind the first Way, which a Way at the end of the mapped
    // network doesn't have.
    val exit = if (stepForwards) {
        way.intersections[WayEnd.END.id]
    } else {
        way.intersections[WayEnd.START.id]
    } ?: return

    fun continueOnto(next: Way, nextVia: List<Intersection> = via) {
        val nextForwards = (next.intersections[WayEnd.START.id] === exit)
        pending.add(
            PendingWay(
                next, nextForwards, if (nextForwards) 0.0 else next.length, travelledToExit,
                nextVia
            )
        )
    }

    // "Real" arms - excludes the approach's own sidewalk/crossing stubs and tile-edge joiners, the
    // same exclusion Way.setbackAlong sizes a kerb setback from. Computed regardless of the total
    // member count: a node with only two members overall (the approach and one other) is still a
    // real junction to report when that other arm doesn't continue the current road - a name
    // change is itself an intersection worth describing (see legacyIntersectionSearch and
    // checkWhetherIntersectionIsOfInterest, which score such a node rather than discard it
    // outright) - so member count alone can never be the shortcut for "nothing to report here".
    val candidates = exit.members.filter { it !== way }
    val realArms = candidates.filter { it.isJunctionArm(exit, way, gridState, strings) }
    // A single real arm is a pass-through if it continues either the road the walk started on or
    // the one it is on now. The second matters once the walk has turned off at a junction to look
    // further: a side road carrying on past the node where the main road's pavement crosses it is
    // not a junction, yet it never continues the main road by name, so it was being reported as one
    // - and, sitting on the pavement, it could be nearer the user than the junction it lies beyond.
    // An unnamed road has no name or ref to compare, so it is also the same road if it is another
    // piece of the same OSM way - which is what an unnamed service road cut in two by a pavement
    // crossing it is.
    val singleArmIsSameRoad = (realArms.size == 1) &&
        (sameRoad(realArms.single(), roadName, roadRef) ||
            sameRoad(realArms.single(), way.name, way.ref) ||
            sameOsmWay(realArms.single(), way))
    when {
        realArms.size >= 2 || (realArms.size == 1 && !singleArmIsSameRoad) -> {
            val ahead = IntersectionAhead(exit, travelledToExit, way, stepForwards, via)
            val alreadyFound = found[exit]
            if (alreadyFound == null) {
                found[exit] = ahead
            } else {
                // Reached again by another route - e.g. along the other carriageway of a road
                // that splits in two before the junction. Keep the nearer arrival, but only the
                // junctions which every route found so far passes through: one that some route
                // avoids isn't one the user has to come to first.
                val nearer = if (ahead.distance < alreadyFound.distance) ahead else alreadyFound
                val other = if (nearer === ahead) alreadyFound else ahead
                found[exit] = nearer.copy(
                    via = nearer.via.filter { passed -> other.via.any { it === passed } }
                )
            }
            if (continuation == WayContinuation.STRAIGHT_ON) return
            // Carry on only the way the user would go if they didn't turn here. A junction down a
            // side road is one they only come to by turning, so it's never the next one ahead,
            // however near it is - but the search used to walk every arm, and such a junction
            // could be chosen over the one actually ahead.
            val viaHere = via + exit
            for (next in continuingArms(exit, way, realArms, roadName, roadRef)) {
                continueOnto(next, viaHere)
            }
        }
        // The one real arm continues the same road - walk through without reporting, same as a
        // plain pass-through node.
        realArms.size == 1 -> continueOnto(realArms.single())
        // Nothing real here: only pavements, crossings, tile joiners, or a genuine dead end. A
        // walk along a road only crosses tile joiners. Stepping onto the pavements and crossings
        // took it on to junctions of the pavement network, which were then described as junctions
        // on the road ("Path goes left") and never reached. A walk that starts on a path or a
        // pavement can carry on along pavements and crossings, which is where it's going.
        else -> {
            val onward = if (onPath) {
                candidates
            } else {
                candidates.filter { it.wayType == WayType.JOINER }
            }
            for (next in onward) continueOnto(next)
        }
    }
}

/** How far an arm can bend away from straight on and still be where the road goes. */
private const val STRAIGHT_ON_DEGREES = 45.0

/**
 * The arms at [junction] which carry on from [arriving] for someone who doesn't turn: those
 * continuing the road by name, ref or OSM way - more than one where it splits into two
 * carriageways - or, where none does, the arm nearest straight on, if one is within
 * [STRAIGHT_ON_DEGREES]. A road which renames at a junction carries on that way.
 */
private fun continuingArms(
    junction: Intersection,
    arriving: Way,
    realArms: List<Way>,
    roadName: String?,
    roadRef: String?,
): List<Way> {
    // A node reported only because one way gives way to another has nowhere else to go, however
    // sharply it bends
    if (realArms.size == 1) return realArms
    val same = realArms.filter {
        sameRoad(it, roadName, roadRef) || sameRoad(it, arriving.name, arriving.ref) ||
            sameOsmWay(it, arriving)
    }
    if (same.isNotEmpty()) return same
    val straightOn = (arriving.heading(junction) + 180.0) % 360.0
    val straightest = realArms.minByOrNull {
        calculateHeadingOffset(straightOn, it.heading(junction))
    } ?: return emptyList()
    return if (calculateHeadingOffset(straightOn, straightest.heading(junction)) <= STRAIGHT_ON_DEGREES) {
        listOf(straightest)
    } else {
        emptyList()
    }
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
