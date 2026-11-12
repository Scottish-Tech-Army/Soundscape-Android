package org.scottishtecharmy.soundscape.geoengine.callouts

import org.scottishtecharmy.soundscape.audio.AudioType
import org.scottishtecharmy.soundscape.audio.Earcons
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.PositionedString
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.formatDistanceAndDirection
import org.scottishtecharmy.soundscape.geoengine.filters.CalloutHistory
import org.scottishtecharmy.soundscape.geoengine.filters.TrackedCallout
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Intersection
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayEnd
import org.scottishtecharmy.soundscape.geoengine.utils.bearingFromTwoPoints
import org.scottishtecharmy.soundscape.geoengine.utils.Direction
import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geoengine.utils.IntersectionAhead
import org.scottishtecharmy.soundscape.geoengine.utils.Triangle
import org.scottishtecharmy.soundscape.geoengine.utils.WayCursor
import org.scottishtecharmy.soundscape.geoengine.utils.calculateHeadingOffset
import org.scottishtecharmy.soundscape.geoengine.utils.calculateSmallestAngleBetweenLines
import org.scottishtecharmy.soundscape.geoengine.utils.checkWhetherIntersectionIsOfInterest
import org.scottishtecharmy.soundscape.geoengine.utils.confectNamesForRoad
import org.scottishtecharmy.soundscape.geoengine.utils.createPolygonFromTriangle
import org.scottishtecharmy.soundscape.geoengine.utils.findShortestDistance
import org.scottishtecharmy.soundscape.geoengine.utils.forEachIntersectionAhead
import org.scottishtecharmy.soundscape.geoengine.utils.getCombinedDirectionSegments
import org.scottishtecharmy.soundscape.geoengine.utils.getFovTriangle
import org.scottishtecharmy.soundscape.geoengine.utils.getPathWays
import org.scottishtecharmy.soundscape.geoengine.utils.isJunctionArm
import org.scottishtecharmy.soundscape.geoengine.utils.polygonContainsCoordinates
import org.scottishtecharmy.soundscape.geoengine.utils.sameRoad
import org.scottishtecharmy.soundscape.geoengine.utils.setbackAlong
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.Ruler
import org.scottishtecharmy.soundscape.geoengine.utils.sortedByDistanceTo
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import org.scottishtecharmy.soundscape.i18n.StringKey
import kotlin.math.abs

/**
 * Below this the formatter has nothing useful left to say - it rounds to the nearest 5m under
 * 100m, so 2m becomes "0 metres" - and at that range the user is at the junction anyway.
 */
private const val minimumSpokenDistanceMetres = 5.0

/**
 * How far beyond a junction with nothing new to name one with a named road can be and still be
 * taken as the same junction - see walkForIntersection.
 */
private const val SAME_JUNCTION_METRES = 15.0

/**
 * How far beyond the junction being announced another can be and still be described as part of
 * it - see IntersectionDescription.alsoAt.
 */
private const val JUNCTION_CLUSTER_METRES = 8.0

/** Junction nodes are exact points, so a callout about one only needs to match that point. */
private const val SAME_NODE_METRES = 1.0

/** What every junction callout is matched against the callout history by - see above. */
private const val JUNCTION_DEDUP_TEXT = "junction"

data class IntersectionDescription(
    var nearestRoad: Way? = null,
    val userGeometry: UserGeometry = UserGeometry(),
    val intersection: Intersection? = null,
    /**
     * How far the user has to travel along the road network to reach [intersection]'s node, in
     * metres, or null where it could not be measured (no map match, or the intersection is not
     * reachable within the search limit).
     *
     * Centre-line to centre-line: this is the distance to the point where the road centre-lines
     * meet, which is not the point a pedestrian arrives at - they stop at the kerb, one
     * cross-road half-width short of it. Subtracting that setback is a separate concern; this
     * value is deliberately the raw network distance.
     *
     * [nearestRoad] is the Way the user approaches along, and is re-derived by
     * addIntersectionCalloutFromDescription when the matched Way is not itself a member of the
     * intersection, so it can be relied on as the approach arm once that has run.
     */
    val centreLineDistance: Double? = null,
    /**
     * The other nodes of the same junction, each with the Way the search arrived on: nodes a few
     * metres from [intersection] which someone walking through meets as part of one junction - a
     * footpath and a service road leaving a street 3m apart, say. Their arms are described along
     * with [intersection]'s - see [SAME_JUNCTION_METRES].
     */
    val alsoAt: List<Pair<Intersection, Way>> = emptyList(),
)

/**
 * How far the user still has to walk to reach the edge of [IntersectionDescription.intersection] -
 * the kerb of the road crossing theirs - or null when there is nothing to measure from.
 *
 * This is the number the user is told, and the number the announcement is timed off, so that what
 * is said and when it is said agree.
 */
fun IntersectionDescription.kerbDistance(
    gridState: GridState,
    strings: LocalizedStrings?,
): Double? {
    val junction = intersection ?: return null
    val distance = centreLineDistance ?: return null
    val setback = nearestRoad?.let { junction.setbackAlong(it, gridState, strings) } ?: 0.0
    return maxOf(0.0, distance - setback)
}

/**
 * A candidate intersection, with the priority that decides between candidates, the network
 * distance measured while reaching it, and the Way that arrives at it.
 *
 * The distance is worth carrying because it has already been computed by the time a candidate is
 * scored, and recomputing it later would mean a second Dijkstra run. Likewise arrivingWay: it is
 * whatever Way the search actually reached the candidate by - the walk's own cursor.way once it
 * has crossed zero or more pass-through nodes, or the legacy search's Dijkstra path - so
 * IntersectionDescription.nearestRoad never needs a separate re-derivation afterwards. Null only
 * when nothing usable was found (no map match at all).
 */
private data class IntersectionCandidate(
    val priority: Int,
    val intersection: Intersection,
    val distance: Double?,
    val arrivingWay: Way?,
    val via: List<Intersection> = emptyList(),
)

/**
 * The distance from [from] along [way] to whichever of [way]'s ends [intersection] is, or null if
 * it is neither.
 *
 * This is the common pedestrian case - already walking the Way that ends at the junction - and is
 * exactly the case getRoadsDescriptionFromFov skips Dijkstra for, so it needs its own (much
 * cheaper) measurement rather than inheriting one.
 */
private fun distanceAlongWayTo(
    way: Way,
    intersection: Intersection,
    from: LngLatAlt,
    ruler: Ruler,
): Double? {
    val along = way.distanceAlongWay(from, ruler)
    return when {
        way.intersections[WayEnd.END.id] === intersection -> way.length - along
        way.intersections[WayEnd.START.id] === intersection -> along
        else -> null
    }
}

/**
 * getRoadsDescriptionFromFov returns a description of the nearestRoad and also the 'best'
 * intersection within the field of view. The description includes the roads that join the
 * intersection, the location of the intersection and the name of the intersection.
 *
 * @param gridState The current GridState which is the state of the downloaded tiles
 * @param userGeometry This includes location, heading and other data
 * @param strings An optional LocalizedStrings used when confecting names for unnamed roads
 * @param minimumTier The least important road that makes an intersection worth describing - see
 * [intersectionMeetsRoadTier]. The default keeps every intersection.
 *
 * @return An IntersectionDescription containing all the data required for callouts to describe the
 * intersection.
 */
/**
 * Test-only: when true, [getRoadsDescriptionFromFov] always uses [legacyIntersectionSearch] rather
 * than walking the Way graph, so that tests can compare the current callouts with what the app
 * used to do. Never set in the app itself.
 */
var forceLegacyIntersectionSearch = false

/** Test-only: when set, [walkForIntersection] reports every candidate and its choice here. */
var junctionSearchTrace: ((String) -> Unit)? = null

fun getRoadsDescriptionFromFov(
    gridState: GridState,
    userGeometry: UserGeometry,
    strings: LocalizedStrings?,
    minimumTier: RoadTier = RoadTier.OTHER,
    calloutHistory: CalloutHistory? = null,
): IntersectionDescription {

    // Without a heading there's no telling which junction the user will reach next - the nearest
    // one may well be behind them - and the description of a junction is relative to the road it is
    // approached along. So say nothing about junctions rather than describe one that may be the
    // wrong one. With no intersection, addIntersectionCalloutFromDescription falls back to naming
    // the road ahead, which needs a heading too, so this is silent until there is one.
    val triangle = getFovTriangle(userGeometry) ?: return IntersectionDescription(
        nearestRoad = userGeometry.mapMatchedWay,
        userGeometry = userGeometry
    )

    val roadTree = gridState.getFeatureTree(TreeId.WAYS_SELECTION)
    val intersectionTree = gridState.getFeatureTree(TreeId.INTERSECTIONS)

    // Find roads within FOV
    val fovRoads = roadTree.getAllWithinTriangle(triangle)
    if (fovRoads.features.isEmpty()) return IntersectionDescription(
        nearestRoad = userGeometry.mapMatchedWay,
        userGeometry = userGeometry
    )

    var nearestRoad = userGeometry.mapMatchedWay
    if (nearestRoad == null) {
        if (userGeometry.inStreetPreview) {
            // In StreetPreview mode, the road we're on is that matching the heading into the
            // intersection that we're at.
            val intersection = intersectionTree.getNearestFeature(
                userGeometry.location,
                userGeometry.ruler
            ) as Intersection?
            val userHeading = userGeometry.heading()
            if ((userHeading != null) && (intersection != null)) {
                for (member in intersection.members) {
                    val wayHeading = (member.heading(intersection) + 180.0) % 360.0
                    if (abs(wayHeading - userHeading) < 1.0) {
                        nearestRoad = member
                        break
                    }
                }
            }
        } else {
            nearestRoad =
                roadTree.getNearestFeatureWithinTriangle(triangle, userGeometry.ruler) as Way?
        }
    }

    // If we're on a mapped sidewalk, use the associated road for intersection detection instead of
    // the sidewalk itself.
    if (nearestRoad?.isSidewalkOrCrossing() == true) {
        if (nearestRoad.properties?.get("pavement") == null) {
            // Confect the names for the sidewalk first, this should come up with the name of the
            // associated road.
            confectNamesForRoad(nearestRoad, gridState, strings)
        }
        // There could be multiple Ways which share the same pavement name, and we want to pick the
        // right one to use. We want the Way to be running in the same direction as the pavement is,
        // and the nearest of those.
        var bestRoad: Way? = null
        var bestRoadDistance = Double.MAX_VALUE
        for (road in fovRoads.features) {
            val way = road as Way
            if (nearestRoad.properties?.get("pavement") == way.name) {
                val matched = userGeometry.mapMatchedLocation
                if (matched?.point != null) {
                    val roadDistance =
                        userGeometry.ruler.distanceToLineString(
                            matched.point,
                            road.geometry as LineString
                        )
                    val snappedHeading = userGeometry.snappedHeading()
                    if (snappedHeading != null) {
                        val innerAngle =
                            calculateSmallestAngleBetweenLines(roadDistance.heading, snappedHeading)
                        if (innerAngle > 45.0) {
                            // This way is not at the angle of travel, so skip it
                            continue
                        }
                    }
                    if (roadDistance.distance < bestRoadDistance) {
                        bestRoad = road
                        bestRoadDistance = roadDistance.distance
                    }
                }
            }
        }
        nearestRoad = bestRoad
    }

    // The steady-state case: there's a genuine map match and we're not standing at a node
    // already, so walk the Way graph ahead of it rather than searching the intersection tree -
    // see walkForIntersection. Gated on mapMatchedWay specifically, not just nearestRoad: without
    // a real match nearestRoad is only ever the tree fallback's best guess a few lines up, good
    // enough to describe a road by but not to measure a confident network distance from - exactly
    // the distinction legacyIntersectionSearch's own matched-location guard makes below, and
    // MapMatchFilter always sets mapMatchedWay and mapMatchedLocation together, so this implies
    // that guard would have passed too. Street Preview steps from intersection to intersection,
    // sitting essentially at a node already (see comingFromBearing's own doc comment), which a
    // forward-walking cursor would step straight past rather than report - so it keeps the
    // tree+Dijkstra search below, as does the rare case there's no map-matched Way to build a
    // cursor on at all.
    if (!forceLegacyIntersectionSearch &&
        !userGeometry.inStreetPreview && (userGeometry.mapMatchedWay != null)) {
        val cursor = nearestRoad?.let {
            userGeometry.cursorOn(it, fallbackHeading = userGeometry.snappedHeading())
        }
        if (cursor != null) {
            return walkForIntersection(
                cursor, nearestRoad, userGeometry, gridState, strings, minimumTier, triangle,
                calloutHistory
            )
        }
    }

    return legacyIntersectionSearch(
        gridState, userGeometry, strings, minimumTier, intersectionTree, triangle, nearestRoad
    )
}

/**
 * Whether [intersection] has an arm with no name or ref which the user could take - a path or a
 * service road - other than the one they arrive on. A junction with nothing new to name is still
 * worth announcing for that, but not one whose other arms are all the road the user is on.
 */
private fun hasUnnamedArm(
    intersection: Intersection,
    arriving: Way?,
    gridState: GridState,
    strings: LocalizedStrings?,
): Boolean = intersection.members.any { arm ->
    (arm.name == null) && (arm.ref == null) &&
        arm.isJunctionArm(intersection, arriving, gridState, strings)
}

/** How far an arm can bend away from straight on and still be the road carrying on. */
private const val STRAIGHT_ON_DEGREES = 45.0

/**
 * Whether the road the user arrives at [intersection] on stops there, so that they have to turn:
 * no arm carries on within [STRAIGHT_ON_DEGREES] of straight on. Where the road they're on goes
 * off both left and right - "Minerva Street goes left, Minerva Street goes right" - there's nothing
 * new to name, but they still need to know.
 */
private fun roadEnds(
    intersection: Intersection,
    arriving: Way?,
    gridState: GridState,
    strings: LocalizedStrings?,
): Boolean {
    if (arriving == null) return false
    val straightOn = (arriving.heading(intersection) + 180.0) % 360.0
    val arms = intersection.members.filter { it.isJunctionArm(intersection, arriving, gridState, strings) }
    return arms.isNotEmpty() && arms.none {
        calculateHeadingOffset(straightOn, it.heading(intersection)) <= STRAIGHT_ON_DEGREES
    }
}

/**
 * The steady-state intersection search: walk the Way graph ahead of [cursor] (see
 * [org.scottishtecharmy.soundscape.geoengine.utils.forEachIntersectionAhead]) for candidates, then
 * choose the nearest along the walk which is worth announcing (see [hasUnnamedArm] and
 * [checkWhetherIntersectionIsOfInterest]). The walk only follows the way the user goes by not
 * turning, so the nearest junction along it is the next one they come to.
 *
 * No rtree search and no Dijkstra to measure that network distance or check reachability:
 * [nearestRoad] is already known - MapMatchFilter computes it every location tick regardless of
 * this call - and the distance to a junction found this way is the walk's own accumulated
 * distance. The Way the walk arrives on is also, by construction, a genuine member of whichever
 * intersection is chosen, so nothing downstream needs to re-derive it.
 *
 * [triangle] still gates which junctions count as candidates at all, the same field-of-view cone
 * [legacyIntersectionSearch] sources its own candidates from. The walk follows the road network
 * rather than a viewing cone, so without this a real but unrelated cluster of junctions - a
 * driveway loop, a staggered side road - a short unrelated hop away could out-rank the junction
 * actually ahead of the user; checking membership here is one point-in-polygon test per junction
 * the walk reports, nothing like the cost of the FeatureTree search the walk avoids for finding
 * them in the first place.
 */
private fun walkForIntersection(
    cursor: WayCursor,
    nearestRoad: Way,
    userGeometry: UserGeometry,
    gridState: GridState,
    strings: LocalizedStrings?,
    minimumTier: RoadTier,
    triangle: Triangle,
    calloutHistory: CalloutHistory?,
): IntersectionDescription {
    val fovPolygon = createPolygonFromTriangle(triangle)
    val userLocation = userGeometry.mapMatchedLocation?.point ?: userGeometry.location
    val candidates = mutableListOf<IntersectionCandidate>()
    // Announced junctions where the road ends or forks - see worthAnnouncing below.
    val turnsAt = mutableListOf<Intersection>()
    val forksAt = mutableListOf<Intersection>()
    forEachIntersectionAhead(cursor, userGeometry.fovDistance, gridState, strings) { found ->
        if (found.forks) forksAt.add(found.intersection)
        if (!polygonContainsCoordinates(found.intersection.location, fovPolygon)) {
            junctionSearchTrace?.invoke("  x ${found.intersection.location.latitude},${found.intersection.location.longitude} net=${found.distance.toInt()} outside field of view")
            return@forEachIntersectionAhead true
        }

        // Within 5m of the user there's nothing new to say - the same trim
        // legacyIntersectionSearch applies, now measured along the network rather than as the
        // crow flies.
        if (found.distance < 5.0) {
            // ...but one the user is about to turn at still stops the search looking past it.
            if ((calloutHistory?.covers(JUNCTION_DEDUP_TEXT, found.intersection.location) == true) &&
                (found.forks || roadEnds(found.intersection, found.arrivingWay, gridState, strings))
            ) turnsAt.add(found.intersection)
            return@forEachIntersectionAhead true
        }

        // Skip intersections which only offer roads less important than the verbosity setting
        // asks for, e.g. a service road or footpath off a street. Scored against the fixed
        // nearestRoad - the Way the user is actually known to be on - not found.arrivingWay: both
        // this and checkWhetherIntersectionIsOfInterest below use "does an arm's name match the
        // road we're on" to decide what to ignore, and that comparison means the road the user is
        // walking, not whichever Way happened to lead the search to this particular candidate
        // (which needn't even be named).
        if (!intersectionMeetsRoadTier(
                found.intersection, nearestRoad, minimumTier, gridState, strings,
                comingFromBearing(userGeometry, found.intersection.location)
            )
        ) {
            junctionSearchTrace?.invoke("  x ${found.intersection.location.latitude},${found.intersection.location.longitude} net=${found.distance.toInt()} below road tier")
            return@forEachIntersectionAhead true
        }

        // We aim to skip 'simple' intersections e.g. ones where the only roads involved have the
        // same name.
        val priority = checkWhetherIntersectionIsOfInterest(found.intersection, nearestRoad)
        candidates.add(
            IntersectionCandidate(
                priority, found.intersection, found.distance, found.arrivingWay, found.via
            )
        )
        true // Every candidate within range is needed before the crow-fly choice below can be made.
    }
    // The junction announced is the next one the user comes to that is worth announcing - the
    // nearest along the walk. Whether a junction is worth announcing is decided by the junction
    // alone. It used to depend on what else was in range: one with only unnamed paths off it was
    // announced when there was nothing better, but passed over for a junction with a named road
    // beyond it, so the user could be told about a junction they'd only reach after another.
    //
    // A junction which has already been announced is looked past, so that the next one can be
    // announced as early as any other rather than only once the user is through the first. The
    // order still holds: they are announced in the order the user reaches them.
    // Not past one where the road ends, though: the user has to turn there, so whatever lies
    // beyond it is somewhere they won't go. Nor past one where the road bends away while another
    // arm goes straight on: the walk followed the road, and the user is as likely to carry
    // straight on (in the replay traces, more likely), so what lies beyond may be on the branch
    // they don't take.
    val announced = candidates.filter { candidate ->
        calloutHistory?.covers(JUNCTION_DEDUP_TEXT, candidate.intersection.location) == true
    }
    announced.filter { c ->
        forksAt.any { it === c.intersection } ||
            roadEnds(c.intersection, c.arrivingWay, gridState, strings)
    }
        .forEach { turnsAt.add(it.intersection) }
    val worthAnnouncing = candidates.filter { candidate ->
        (candidate !in announced) && candidate.via.none { v -> turnsAt.any { it === v } }
    }.filter { candidate ->
        (candidate.priority > 0) ||
            ((candidate.priority == 0) && (
                hasUnnamedArm(candidate.intersection, candidate.arrivingWay, gridState, strings) ||
                    roadEnds(candidate.intersection, candidate.arrivingWay, gridState, strings)
                ))
    }
    val nearest = worthAnnouncing.minByOrNull { it.distance ?: Double.MAX_VALUE }
    // A junction with only unnamed paths or service roads off it, a few metres short of one with
    // a named road, is part of the same junction to someone walking through - a footpath stub at
    // the corner, say - and the named road is what describes it.
    val candidate = if ((nearest != null) && (nearest.priority <= 0)) {
        val reach = (nearest.distance ?: 0.0) + SAME_JUNCTION_METRES
        worthAnnouncing
            .filter { (it.priority > 0) && ((it.distance ?: Double.MAX_VALUE) <= reach) }
            .minByOrNull { it.distance ?: Double.MAX_VALUE } ?: nearest
    } else {
        nearest
    }
        // Where the road only changes name ahead, that node is still the one described - as before,
        // it says nothing (see addIntersectionCalloutFromDescription), rather than "Ahead <road>".
        ?: candidates.filter { it.priority < 0 }.minByOrNull { it.distance ?: Double.MAX_VALUE }
    junctionSearchTrace?.let { trace ->
        trace("road=${nearestRoad.name ?: nearestRoad.featureValue} matched=${userGeometry.mapMatchedWay?.name ?: userGeometry.mapMatchedWay?.featureValue}")
        for (c in candidates) {
            val l = c.intersection.location
            val crow = userGeometry.ruler.distance(userLocation, l).toInt()
            trace(
                (if (c === candidate) "* " else "  ") + "${l.latitude},${l.longitude} " +
                    "p=${c.priority} net=${c.distance?.toInt()} crow=$crow via=${c.via.size}" +
                    (if (c in worthAnnouncing) "" else " (not worth announcing)") +
                    " on=${c.arrivingWay?.name ?: c.arrivingWay?.featureValue} arms=" +
                    c.intersection.members.joinToString("|") { m ->
                        (m.name ?: m.featureValue ?: "?") +
                            (m.properties?.get("footway")?.let { "($it)" } ?: "") +
                            "/${m.length.toInt()}m" +
                            (if (m.isJunctionArm(c.intersection, c.arrivingWay, gridState, strings)) "" else "-")
                    }
            )
        }
    }
    if (candidate == null) return IntersectionDescription(nearestRoad, userGeometry)

    // The rest of the junction: the junctions worth announcing that lie just beyond this one, and
    // any just before it which gave way to it above. Someone walking through meets them all as one
    // junction, and announcing each separately said "Intersection in 30 metres" and then, a few
    // seconds later, "Intersection in 5 metres" for what is to them the same place.
    val alsoAt = if (candidate.priority >= 0) {
        val at = candidate.distance ?: 0.0
        worthAnnouncing.filter { other ->
            (other !== candidate) && (other.arrivingWay != null) && (other.distance != null) &&
                (
                    ((other.distance <= at + JUNCTION_CLUSTER_METRES) &&
                        other.via.any { it === candidate.intersection }) ||
                    ((other.distance >= at - SAME_JUNCTION_METRES) &&
                        candidate.via.any { it === other.intersection })
                )
        }.sortedBy { it.distance }.map { it.intersection to it.arrivingWay!! }
    } else {
        emptyList()
    }
    // The walk doesn't go on from a junction where the road ends, so it finds nothing beyond a
    // T junction - but a side road a few metres along either way is part of the same junction,
    // and the user is past it as soon as they've turned. So also take any junction one short arm
    // away, in any direction.
    val neighbours = if (candidate.priority >= 0) {
        candidate.intersection.members.mapNotNull { arm ->
            if ((arm === candidate.arrivingWay) || (arm.length > JUNCTION_CLUSTER_METRES) ||
                !arm.isJunctionArm(candidate.intersection, candidate.arrivingWay, gridState, strings)
            ) return@mapNotNull null
            val far = arm.getOtherIntersection(candidate.intersection) ?: return@mapNotNull null
            if ((far === candidate.intersection) || (far.members.size <= 2) ||
                alsoAt.any { it.first === far }
            ) return@mapNotNull null
            // The same filters every junction the walk finds goes through: one already announced
            // isn't described again, and one offering only roads below the verbosity setting
            // isn't described at all.
            if (calloutHistory?.covers(JUNCTION_DEDUP_TEXT, far.location) == true) return@mapNotNull null
            if (!intersectionMeetsRoadTier(
                    far, nearestRoad, minimumTier, gridState, strings,
                    comingFromBearing(userGeometry, far.location)
                )
            ) return@mapNotNull null
            val worth = (checkWhetherIntersectionIsOfInterest(far, nearestRoad) > 0) ||
                hasUnnamedArm(far, arm, gridState, strings)
            if (worth) far to arm else null
        }
    } else {
        emptyList()
    }

    return IntersectionDescription(
        candidate.arrivingWay ?: nearestRoad,
        userGeometry,
        candidate.intersection,
        candidate.distance,
        alsoAt + neighbours,
    )
}

/**
 * The tree-search + Dijkstra fallback, used only when there's no cursor to walk the Way graph
 * from - see [getRoadsDescriptionFromFov]'s own comment on when that is.
 */
private fun legacyIntersectionSearch(
    gridState: GridState,
    userGeometry: UserGeometry,
    strings: LocalizedStrings?,
    minimumTier: RoadTier,
    intersectionTree: FeatureTree,
    triangle: Triangle,
    nearestRoad: Way?,
): IntersectionDescription {
    // Find intersections within FOV
    val fovIntersections = intersectionTree.getAllWithinTriangle(triangle)
    if (fovIntersections.features.isEmpty()) return IntersectionDescription(
        nearestRoad,
        userGeometry
    )

    // Remove intersections which are only:
    //  1. Short paths leading to sidewalks of the road, or
    //  2. Direct intersections with sidewalks.
    //  3. Within a 5m radius of the current location
    val trimmedIntersections = FeatureCollection()
    for (i in fovIntersections.features) {
        val intersection = i as Intersection
        var add = true
        if (!userGeometry.inStreetPreview && userGeometry.ruler.distance(
                intersection.location,
                userGeometry.mapMatchedLocation?.point ?: userGeometry.location
            ) < 5.0
        )
            add = false
        else {
            // The same "is this a real arm" test the Way-graph walk uses (see
            // forEachIntersectionAhead): at least two real arms, or exactly one that doesn't
            // continue nearestRoad by name/ref - otherwise this is a pass-through, not a real
            // junction. Sharing the test with the walk also fixes a pre-existing bug here: the
            // previous version compared intersection.members[0]/[1] regardless of whether either
            // one had actually survived disposal.
            val realArms = intersection.members.filter {
                (it !== nearestRoad) && it.isJunctionArm(intersection, nearestRoad, gridState, strings)
            }
            add = (realArms.size >= 2) ||
                ((realArms.size == 1) &&
                    !sameRoad(realArms.single(), nearestRoad?.name, nearestRoad?.ref))
        }
        if (add)
            trimmedIntersections.features.add(intersection)
    }

    // Sort the FOV intersections by distance
    val sortedFovIntersections = sortedByDistanceTo(
        userGeometry.mapMatchedLocation?.point ?: userGeometry.location,
        trimmedIntersections
    )

    // Inspect each intersection so as to skip trivial ones
    val nonTrivialIntersections = mutableListOf<IntersectionCandidate>()

    for (intersection in sortedFovIntersections.features) {
        val intersectionLocation = (intersection.geometry as Point).coordinates
        val graphIntersection = gridState.gridIntersections[intersectionLocation]
        if (graphIntersection != null) {
            var distance: Double? = null
            // The Way that actually arrives at this candidate - nearestRoad unless the Dijkstra
            // path below finds otherwise. See IntersectionCandidate.arrivingWay.
            var arrivingWay: Way? = nearestRoad
            val matched = userGeometry.mapMatchedLocation
            if ((matched != null) && (nearestRoad != null)) {
                // The nearestRoad ends at this intersection, so the distance is simply what is
                // left of the Way ahead of us. The Dijkstra below is skipped in this case, so
                // this is the only chance to measure it.
                distance = distanceAlongWayTo(
                    nearestRoad,
                    graphIntersection,
                    matched.point,
                    userGeometry.ruler
                )
                // If our current matched way ends at this intersection, then we don't need to use
                // more elaborate (Dijkstra) pathfinding to check the connection.
                if (!nearestRoad.intersections.contains(graphIntersection)) {

                    // Check if we can get to the intersection from our current location within a
                    // short distance. If we can, check that we don't go through any other valid
                    // intersections first.
                    val shortestDistanceResults = findShortestDistance(
                        matched.point,
                        nearestRoad,
                        intersectionLocation,
                        (intersection as Intersection).members.first(),
                        null,
                        null,
                        50.0
                    )
                    if (shortestDistanceResults.distance < 50.0) {
                        distance = shortestDistanceResults.distance
                        var skip = false
                        val ways = getPathWays(graphIntersection)
                        // The first Way on the path back from the candidate is the one that
                        // actually arrives at it.
                        arrivingWay = ways.firstOrNull() ?: nearestRoad
                        var nextIntersection: Intersection? = graphIntersection
                        for (way in ways) {
                            val currentIntersection = nextIntersection ?: break
                            nextIntersection = way.getOtherIntersection(currentIntersection)

                            nextIntersection?.let { next ->
                                var count = 0
                                if (next.members.size > 2) {
                                    for (member in next.members) {
                                        if (member.properties != null) {
                                            if (member.isSidewalkOrCrossing() &&
                                                !member.isSidewalkConnector(
                                                    intersection,
                                                    nearestRoad,
                                                    gridState,
                                                    strings
                                                )
                                            ) {
                                                count++
                                            }
                                        }
                                    }
                                    if (count > 2)
                                        skip = true
                                    return@let
                                }
                            }
                            if (skip)
                                break
                        }
                        if (skip) {
                            // Skip this intersection, as it's not the nearest one of interest
                            shortestDistanceResults.tidy()
                            continue
                        }
                    } else {
                        shortestDistanceResults.tidy()
                        continue
                    }
                    shortestDistanceResults.tidy()
                }
            }

            // Skip intersections which only offer roads less important than the verbosity
            // setting asks for, e.g. a service road or footpath off a street.
            if (!intersectionMeetsRoadTier(
                    graphIntersection, nearestRoad, minimumTier, gridState, strings,
                    comingFromBearing(userGeometry, graphIntersection.location)
                ))
                continue

            // We aim to skip 'simple' intersections e.g. ones where the only roads involved have
            // the same name.
            val priority = checkWhetherIntersectionIsOfInterest(graphIntersection, nearestRoad)
            nonTrivialIntersections.add(
                IntersectionCandidate(priority, graphIntersection, distance, arrivingWay)
            )
        }
    }
    if (nonTrivialIntersections.isEmpty()) {
        return IntersectionDescription(nearestRoad, userGeometry)
    }

    // No intersection with a priority greater than zero, so just pick the highest
    val candidate = nonTrivialIntersections.firstOrNull { prioritised ->
        prioritised.priority > 0
    }
        ?: nonTrivialIntersections.maxByOrNull { prioritised ->
            prioritised.priority
        }
        ?: return IntersectionDescription(nearestRoad, userGeometry)

    // Find the bearing that we're coming in at - measured to the nearest intersection
    val heading = candidate.arrivingWay?.heading(candidate.intersection)
    if (heading != null) {
        // And use the polygons to describe the roads at the intersection
        return IntersectionDescription(
            candidate.arrivingWay,
            userGeometry,
            candidate.intersection,
            candidate.distance
        )
    }
    return IntersectionDescription(nearestRoad, userGeometry)
}

/**
 * addIntersectionCalloutFromDescription adds a callout to the results list for the intersection
 * described in the parameters. This will become more configurable e.g. whether to include the
 * distance or not.
 *
 * @param description The description of the intersection to callout
 * @param localized A LocalizedStrings for obtaining localized strings
 * @param calloutHistory An optional CalloutHistory to use so as to filter out recently played out
 * @param gridState The current gridState
 * @param speakDistance Whether to say how far away the intersection is - the
 * PreferenceKeys.DISTANCE_TO_INTERSECTION setting
 *
 * @return A TrackedCallout for the intersection if one was found, otherwise null.
 */
fun addIntersectionCalloutFromDescription(
    description: IntersectionDescription,
    localized: LocalizedStrings?,
    calloutHistory: CalloutHistory? = null,
    gridState: GridState,
    speakDistance: Boolean = true,
): TrackedCallout? {

    // Report nearby road
    if (description.intersection == null) {
        description.nearestRoad?.let { nearestRoad ->

            // Figure out which direction we're travelling along the way
            var direction: Boolean? = null
            val matched = description.userGeometry.mapMatchedLocation
            if (matched != null) {
                direction = when (description.userGeometry.snappedHeading()) {
                    matched.heading ->
                        true

                    (matched.heading + 180.0) % 360.0 ->
                        false

                    else ->
                        // If the direction of travel is more 'across' than 'along' then skip the
                        // description this time. Once the direction is better aligned we can call it
                        // out. This avoids calling out roads that the user is crossing over as
                        // 'ahead'.
                        null
                }
            }
            if (direction != null) {
                val roadName = nearestRoad.getName(direction, gridState, localized)
                // A template, not "ahead" + " " + name, so each language can put the name where
                // its word order needs it ("Moor Road ileride").
                val calloutText = localized?.get(StringKey.DirectionsAheadRoad, roadName)
                    ?: "Ahead $roadName"

                val trackedCallout = TrackedCallout(
                    description.userGeometry,
                    calloutText,
                    LngLatAlt(),
                    positionedStrings = List(1) {
                        PositionedString(
                            text = calloutText,
                            type = AudioType.STANDARD
                        )
                    },
                    isPoint = false,
                    isGeneric = false,
                    calloutHistory = calloutHistory
                )
                if (calloutHistory?.find(trackedCallout) != true) {
                    return trackedCallout
                }
            }
        }
        return null
    }

    val intersectionName = description.intersection.name

    // nearestRoad not being a member of the intersection used to be common - particularly where
    // sidewalks break up the road segments - and was rescued here with a second Dijkstra search.
    // Both getRoadsDescriptionFromFov's walk and its legacy tree+Dijkstra fallback now set
    // nearestRoad to whichever Way actually arrives at the intersection they chose (see
    // IntersectionCandidate.arrivingWay), so this should not happen via either path any more; a
    // defensive check rather than a silent guess if it somehow still does.
    if (description.nearestRoad?.containsIntersection(description.intersection) != true) {
        return null
    }
    val heading = description.nearestRoad?.heading(description.intersection) ?: return null

    if (description.intersection.members.size <= 2)
        return null

    // Check if we should be filtering out this callout
    val intersectionLocation = description.intersection.location

    // The distance is to the kerb rather than to the node where the centre-lines meet, which is
    // where the user will actually arrive - see JunctionExtent. Both the setback and the distance
    // it is taken from can be absent (an unmapped junction, or no map match to measure from), in
    // which case we say that there is an intersection without saying how far, rather than saying
    // a number we don't have.
    //
    // Note this formats the distance here rather than letting SpeakCallout's addDistanceAndHeading
    // do it: that path is only reached for a PositionedString with a location, and it would
    // measure the straight line to the centre-line node, which is the number being replaced.
    val kerbDistance = description.kerbDistance(gridState, localized)
    val approachText = if (speakDistance &&
        (kerbDistance != null) &&
        (kerbDistance >= minimumSpokenDistanceMetres)
    ) {
        val formatted = formatDistanceAndDirection(
            kerbDistance,
            null,
            localized,
            speed = description.userGeometry.speed
        )
        localized?.get(StringKey.IntersectionApproachingIntersectionDistance, formatted)
            ?: "Intersection in $formatted"
    } else {
        localized?.get(StringKey.IntersectionApproachingIntersection)
            ?: "Approaching intersection"
    }

    val trackedCallout = TrackedCallout(
        description.userGeometry,
        intersectionName ?: "",
        intersectionLocation,
        positionedStrings = List(1) {
            PositionedString(
                text = approachText,
                heading = -10000.0,
                earcon = Earcons.SENSE_POI,
                type = AudioType.STANDARD
            )
        },
        isPoint = true,
        isGeneric = false,
        calloutHistory = calloutHistory,
        // The callout covers every node of the junction it describes (see
        // IntersectionDescription.alsoAt), so none of the others is announced after it. Matched
        // by place alone, since each node has a name of its own.
        dedupText = JUNCTION_DEDUP_TEXT,
        matchRadiusMetres = SAME_NODE_METRES,
        alsoCovers = description.alsoAt.map { it.first.location },
    )
    if (calloutHistory?.find(trackedCallout) == true) {
        return null
    }

    // Report intersection is coming up

    // Report roads that join the intersection
    val incomingHeading = (heading + 180.0) % 360.0

    val directions = getCombinedDirectionSegments(incomingHeading)
    val intersectionResults = trackedCallout.positionedStrings.toMutableList()
    // The short Ways joining the nodes of one junction to each other are inside it, not roads
    // leaving it. Each node was reached along one of them, or - for a node before
    // description.intersection - is left along the one description.intersection is reached by.
    // Directions at every node are from the way the user walks into description.intersection.
    val insideJunction = description.alsoAt.map { it.second } +
        listOfNotNull(description.nearestRoad.takeIf { description.alsoAt.isNotEmpty() })
    val nodes = listOf(description.intersection) + description.alsoAt.map { it.first }
    for (node in nodes) for (way in node.members) {
        if ((node !== description.intersection) && insideJunction.any { it === way }) continue

        // Pavements aren't called out. A footway is only found to be one as its name is confected,
        // so that has to happen before asking - otherwise the first callout at a junction says
        // "Pavement next to Filton Road goes left" and every later one leaves it out.
        if (way.isSidewalkOrCrossing() && (way.properties?.get("pavement") == null))
            confectNamesForRoad(way, gridState, localized)
        if (way.properties?.get("pavement") != null)
            continue

        val wayHeading = way.heading(node)
        val direction = directions.indexOfFirst { segment ->
            segment.contains(wayHeading)
        }

        // Don't call out the road we are on (0) as part of the intersection
        if (direction != Direction.BEHIND.value) {
            val directionKey = when (direction) {
                Direction.BEHIND_LEFT.value, Direction.LEFT.value, Direction.AHEAD_LEFT.value ->
                    StringKey.DirectionsNameGoesLeft

                Direction.BEHIND_RIGHT.value, Direction.RIGHT.value, Direction.AHEAD_RIGHT.value ->
                    StringKey.DirectionsNameGoesRight

                else ->
                    StringKey.DirectionsNameContinuesAhead
            }
            var unlocalizedDirection = ""
            if (localized == null) {
                unlocalizedDirection = when (direction) {
                    Direction.BEHIND_LEFT.value, Direction.LEFT.value, Direction.AHEAD_LEFT.value ->
                        "goes left"

                    Direction.BEHIND_RIGHT.value, Direction.RIGHT.value, Direction.AHEAD_RIGHT.value ->
                        "goes right"

                    else ->
                        "continues ahead"
                }

            }

            val presentationHeading = incomingHeading + when (direction) {
                Direction.BEHIND_LEFT.value, Direction.LEFT.value, Direction.AHEAD_LEFT.value -> -90.0
                Direction.BEHIND_RIGHT.value, Direction.RIGHT.value, Direction.AHEAD_RIGHT.value -> 90.0
                else -> 0.0
            }

            val destinationText = way.getName(
                way.intersections[WayEnd.START.id] == node,
                gridState,
                localized
            )
            val intersectionCallout =
                localized?.get(directionKey, destinationText)
                    ?: "\t$destinationText $unlocalizedDirection"
            if (intersectionResults.any { it.text == intersectionCallout }) continue
            intersectionResults.add(
                PositionedString(
                    text = intersectionCallout,
                    type = AudioType.COMPASS,
                    heading = presentationHeading
                )
            )
        }
    }
    // Order intersection callout by heading from left to right
    intersectionResults.sortBy { it.heading }
    trackedCallout.positionedStrings = intersectionResults
    return trackedCallout
}

/**
 * The bearing from [intersectionLocation] back to the user, which says which way they arrived -
 * see [intersectionMeetsRoadTier].
 *
 * Null when they are standing on the intersection, where there is no direction to measure:
 * bearingFromTwoPoints of a point to itself is due north, and anything leaving the junction
 * northwards would then be taken for the way they came. Street Preview steps from intersection to
 * intersection, so that is its normal state rather than an edge case.
 */
private fun comingFromBearing(
    userGeometry: UserGeometry,
    intersectionLocation: LngLatAlt,
): Double? {
    val userLocation = userGeometry.mapMatchedLocation?.point ?: userGeometry.location
    if (userGeometry.ruler.distance(intersectionLocation, userLocation) < 1.0) return null
    return bearingFromTwoPoints(intersectionLocation, userLocation)
}
