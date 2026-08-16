package org.scottishtecharmy.soundscape.geoengine.filters

import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Intersection
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayEnd
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayType
import org.scottishtecharmy.soundscape.geoengine.utils.PointAndDistanceAndHeading
import org.scottishtecharmy.soundscape.geoengine.utils.addSidewalk
import org.scottishtecharmy.soundscape.geoengine.utils.bearingFromTwoPoints
import org.scottishtecharmy.soundscape.geoengine.utils.calculateSmallestAngleBetweenLines
import org.scottishtecharmy.soundscape.geoengine.utils.clone
import org.scottishtecharmy.soundscape.geoengine.utils.findShortestDistance
import org.scottishtecharmy.soundscape.geoengine.utils.fromRadians
import org.scottishtecharmy.soundscape.geoengine.utils.getDestinationCoordinate
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.Ruler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import kotlin.math.asin
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sqrt

private const val FRECHET_QUEUE_SIZE = 12

// See MapMatchFilter.filter's follower-selection hysteresis - a challenger follower must beat the
// currently-matched one's frechetAverage by at least this fraction (i.e. be at least 25% lower)
// before the match is allowed to switch to it.
private const val FOLLOWER_SWITCH_HYSTERESIS_FACTOR = 0.75

// See MapMatchFilter.filter's raw-distance override of the hysteresis above - the incumbent's raw
// nearestPoint.distance must already be at least this far off for the override to be considered.
private const val RAW_DISTANCE_OVERRIDE_MINIMUM_METRES = 10.0

// See MapMatchFilter.filter's raw-distance override - the challenger's raw nearestPoint.distance
// must be under this fraction of the incumbent's (i.e. well under a third of it) for the override
// to kick in and bypass the frechetAverage hysteresis.
private const val RAW_DISTANCE_OVERRIDE_RATIO = 0.3

// See MapMatchFilter.isMatchConfident/updateMatchConfidence - how many ticks matchedWay can stay
// non-null without matchedFollower itself individually reaching LOCKED-level confidence (e.g.
// while its route is being handed off to a freshly-extended follower) before confidence is lost.
private const val GRACE_TICKS_AFTER_LOSING_CONFIDENCE = 5

enum class RoadFollowerState {
    LOCKED,
    UNLOCKED,
    ANGLED_AWAY,
    DIRECTION_CHANGED,
    DISTANT
}

data class RoadFollowerStatus(val frechetAverage: Double, val state: RoadFollowerState)

/**
 * Create a single LineString from our route which is a list of Ways. Two Arrays are created when
 * the line is created:
 *   * indices - this allows any point in the line to be referenced back to the route that created
 *     it. When a point is found on the line it can be immediately matched to the Way that it
 *     belongs to.
 *   * direction - the line segments all have to be in the same direction, and this involves
 *     reversing the lines from the Way to keep the line contiguous. We store this so that we can
 *     adjust the heading for a point on the line to match that of the Way rather than of the line.
 *
 *  We also create a hashCode as the line is created. This is so that RoadFollowers with identical
 *  lines can be de-duplicated.
 */
class IndexedLineString {

    var line: LineString? = null
    var indices: Array<Int>? = null
    var direction: Array<Boolean>? = null
    var hashCode: Int = 0

    fun getWayIndex(pointIndex: Int): Int? {

        indices?.let { indices ->
            for ((index, offset) in indices.withIndex()) {
                if (pointIndex < offset) {
                    return index
                }
            }
            return indices.size - 1
        }
        return null
    }

    fun updateFromRoute(route: List<Way>) {

        if (route.isEmpty()) {
            line = null
            return
        }

        indices = Array(route.size) { 0 }
        direction = Array(route.size) { true }
        if (route.size == 1) {
            val singleLine = route[0].geometry as LineString
            line = singleLine
            indices?.set(0, singleLine.coordinates.size)
            hashCode = singleLine.coordinates.hashCode()
            return
        }

        val newLine = LineString()
        line = newLine
        for ((index, way) in route.withIndex()) {

            var forwards: Boolean
            if (index < route.size - 1) {
                // Most of the Ways in the route
                val (nextIntersection, ourIndex) = route[index].doesIntersect(route[index + 1])
                if (nextIntersection == null) {
                    // We can get here if the tile grid is recalculated, and tiles rejoined and the route is
                    // left with out of date and disconnected ways.
                    line = null
                    return
                }
                forwards = (ourIndex == WayEnd.END.id)
            } else {
                // The last Way in the route
                val (firstIntersection, ourIndex) = route[index].doesIntersect(route[index - 1])
                if (firstIntersection == null) {
                    // We can get here if the tile grid is recalculated, and tiles rejoined and the route is
                    // left with out of date and disconnected ways.
                    line = null
                    return
                }
                forwards = (ourIndex == WayEnd.START.id)
            }

            // And add its coordinates to the LineString along with whether or not we reversed the
            // order of the coordinates. This results in the same coordinate being duplicated at
            // each intersection, but is simple.
            if (forwards) {
                newLine.coordinates.addAll((way.geometry as LineString).coordinates)
            } else {
                newLine.coordinates.addAll((way.geometry as LineString).coordinates.reversed())
            }
            direction?.set(index, forwards)

            // Note the index at which this Way ends
            indices?.set(index, newLine.coordinates.size)
        }
        hashCode = newLine.coordinates.hashCode()
    }
}

class RoadFollower(
    val parent: MapMatchFilter,
    var route: MutableList<Way>,
    var lastGpsLocation: LngLatAlt?,
    val colorIndex: Int
) {

    var radius = 2.0
    var lastChordPoints: Array<LngLatAlt> = arrayOf(LngLatAlt(), LngLatAlt())
    var nearestPoint: PointAndDistanceAndHeading? = null
    var lastMatchedLocation: PointAndDistanceAndHeading? = null
    var lastCenter = LngLatAlt()
    val frechetQueue = ArrayDeque<Double>(FRECHET_QUEUE_SIZE)

    /**
     * The LOCKED/UNLOCKED distinction (frechetQueue.size > FRECHET_QUEUE_SIZE/2) exists to avoid
     * treating a follower that's only had a couple of ticks to prove itself as being as
     * trustworthy as one with a full queue of consistent history. But a follower that's only just
     * been created (e.g. for a road discovered at a junction) still has SOME evidence the moment
     * it's within striking distance of the GPS track - and at driving speed, a well-established
     * LOCKED follower that's actually drifted away from the true road (e.g. the outer edge of a
     * bend, where matchedPoint.distance and radius are both still large) shouldn't beat a
     * brand-new follower whose very first sample landed almost exactly on the road, just because
     * the new one hasn't accumulated a big queue yet. So this is used for the actual competitive
     * frechetAverage instead of unconditionally disqualifying with Double.MAX_VALUE whenever the
     * queue isn't yet half full - only a follower with literally zero history (still
     * Double.MAX_VALUE here) can't win.
     */
    fun frechetAverageOrMax(): Double {
        if (frechetQueue.isEmpty()) return Double.MAX_VALUE
        return frechetQueue.average()
    }

    val k = 0.2
    var averagePointGap: Double = 0.0

    var ils = IndexedLineString()
    var currentNearestRoad: Way = route[0]

    // Instead of acting on the LineStrings of individual Ways, we want to create our own LineString
    // by concatenating those within the Ways to make a single line. Each point should link to
    // the Way that it is a member of so that at any point it's know that that's what's being
    // followed. This should solve the current issue where the following is poor near intersections,
    // and followers with the closest matching LineString should maintain a good lock.
    // This is particularly important where there are many short segments e.g. around
    //          https://www.openstreetmap.org/node/12580941684
    //
    // The LineString can be trimmed once it's members no longer form part of the Frechet queue.
    //
    // OR perhaps we can just have an ordered list of Ways which are treated as a single LineString
    // by the algorithm. How about:
    //
    // When a follower nears the end of it's current Way (measurable via the points of the way),
    // another Way can be queued up behind it. In the case of a single Way with multiple 'joins'
    // there would exist a single RoadFollower containing all of the segments of the Way.
    // If a Way splits at an Intersection, then there would need to exist a RoadFollower for each
    // member of the intersection. This sort of happens right now - but they are for the segment
    // and not a continuation. We can perhaps tighten up on ending RoadFollowers when their radius
    // is far greater than the chosen RoadFollower.
    //
    // We also still need to create RoadFollowers for Way that appear nearby in case a user
    // jumps to an un-connected Way e.g. uses an unmarked Way or goes across some grass/pedestrian
    // area where there is no Way.
    //

    val color: String
    var directionOnLine = 0.0
    var directionHysteresis = 5

    init {
        val colorArray = arrayOf(
            "#ff0000",
            "#00ff00",
            "#0000ff",
            "#ffff00",
            "#00ffff",
            "#ff00ff",
            "#800000",
            "#008000",
            "#000080",
            "#808000",
            "#008080",
            "#800080"
        )
        color = colorArray[colorIndex % colorArray.size]

        validateRoute()
        ils.updateFromRoute(route)
    }

    fun validateRoute() {
        val hashMap = HashMap<Intersection, Int>()
        for (way in route) {
            for (intersection in way.intersections) {
                if (intersection != null) {
                    hashMap[intersection] = (hashMap[intersection] ?: 0) + 1
                }
            }
        }
        for (count in hashMap) {
            if (count.value > 2) {
                println("Too many intersections")
                // We can get here 'legally' if we add a way which loops back and joins the current
                // way e.g. https://www.openstreetmap.org/way/945577262
//                assert(false)
            }
        }
    }

    /**
     * If a Way in a route is more than 60m away (further, if GPS updates are more widely spaced
     * than that - see [averagePointGap] - since driving covers much more ground between updates
     * than the walking pace this was originally tuned for), then trim it off. If we get close to
     * it again, we can add it back in.
     */
    fun trimRoute(location: LngLatAlt, ruler: CheapRuler): Boolean {
        var trimmed = false
        val trimDistance = max(60.0, averagePointGap * 2.0)

        // Trim start
        val iterator = route.listIterator()
        while (iterator.hasNext()) {
            val way = iterator.next()
            if (ruler.distanceToLineString(location, way.geometry as LineString).distance > trimDistance) {
                iterator.remove()
                trimmed = true
            } else {
                break
            }
        }

        // Go to end of list
        while (iterator.hasNext()) {
            iterator.next()
        }

        // Trim end
        while (iterator.hasPrevious()) {
            val way = iterator.previous()
            if (ruler.distanceToLineString(location, way.geometry as LineString).distance > trimDistance) {
                iterator.remove()
                trimmed = true
            } else {
                break
            }
        }
        if (trimmed) {
            validateRoute()
            ils.updateFromRoute(route)
        }
        return trimmed
    }

    fun getRouteIntersectionIndices(newWay: Way): Pair<Int, Int> {
        var minIndex = Int.MAX_VALUE
        var maxIndex = -1
        for ((index, way) in route.withIndex()) {
            if (way.doesIntersect(newWay).first != null) {
                if (index < minIndex) minIndex = index
                if (index > maxIndex) maxIndex = index
            }
        }
        return Pair(minIndex, maxIndex)
    }

    fun createExtendedFollower(
        extensionAddedAtStart: Boolean,
        newWay: Way,
        colorIndexOffset: Int
    ): RoadFollower {
        // Create a new follower which is a copy of this one, but with an extended route
        val newRoute = mutableListOf<Way>()

        val (minIndex, maxIndex) = getRouteIntersectionIndices(newWay)
        if (minIndex != maxIndex) {
            // newWay intersects with more than one of our ways, so we need to drop one.
            if (extensionAddedAtStart) {
                // Replace the first way
                newRoute.addAll(route)
                newRoute[0] = newWay
            } else {
                // Replace the last way
                newRoute.addAll(route)
                newRoute[maxIndex] = newWay
            }
        } else {
            if (extensionAddedAtStart) {
                // Insert as first way
                newRoute.add(newWay)
                newRoute.addAll(route)
            } else {
                // Append as last way
                newRoute.addAll(route)
                newRoute.add(newWay)
            }
        }

        val newFollower =
            RoadFollower(parent, newRoute, lastGpsLocation?.clone(), colorIndex + colorIndexOffset)

        // Clone all the data that we need
        newFollower.averagePointGap = averagePointGap
        newFollower.nearestPoint = nearestPoint?.clone()
        newFollower.radius = radius
        newFollower.lastCenter = lastCenter.clone()
        newFollower.currentNearestRoad = currentNearestRoad

        frechetQueue.forEach { newFollower.frechetQueue.add(it) }
        lastChordPoints.forEachIndexed { index, point ->
            newFollower.lastChordPoints[index] = point.clone()
        }
        newFollower.lastMatchedLocation = lastMatchedLocation?.clone()

        return newFollower
    }

    fun extendToNewWay(newWay: Way, ruler: Ruler): Boolean {
        val (minIndex, maxIndex) = getRouteIntersectionIndices(newWay)
        if (minIndex != maxIndex) {
            // We can't extend the route as the newWay intersects with more than one of the ways in
            // the route already.
            return false
        }

        val newRoute = mutableListOf<Way>()
        if (route.first().doesIntersect(newWay).first != null) {
            if (!route.first().intersections.contains(null)) {
                newRoute.add(newWay)
                newRoute.addAll(route)
            }
        } else if (route.last().doesIntersect(newWay).first != null) {
            if (!route.last().intersections.contains(null)) {
                newRoute.addAll(route)
                newRoute.add(newWay)
            }
        }
        if (newRoute.isNotEmpty()) {

            route = newRoute
            validateRoute()
            ils.updateFromRoute(route)
            if (ils.line != null) {
                nearestPoint?.let { point ->
                    nearestPoint = ruler.distanceToLineString(point.point, ils.line as LineString)
                }
            }
            directionOnLine = 0.0
            directionHysteresis = 5
            return true
        }
        return false
    }

    fun update(
        gpsLocation: LngLatAlt,
        collection: FeatureCollection,
        ruler: CheapRuler
    ): RoadFollowerStatus {

        if (ils.line == null)
            return RoadFollowerStatus(Double.MAX_VALUE, RoadFollowerState.DISTANT)
        if (trimRoute(gpsLocation, ruler)) {
            // The route was trimmed
            if (route.isEmpty() || ils.line == null) {
                return RoadFollowerStatus(Double.MAX_VALUE, RoadFollowerState.DISTANT)
            }
            // Reset nearestPoint so that the pointAlongLine is based on our newly trimmed line
            nearestPoint?.let { point ->
                nearestPoint = ruler.distanceToLineString(point.point, ils.line as LineString)
            }
            directionOnLine = 0.0
            directionHysteresis = 5
        }

        // Update radius
        var gpsHeading = Double.NaN
        var pointGap = 0.0
        var dMin = 0.0
        var directionChange = false
        lastGpsLocation?.let { lastLocation ->

            if (lastLocation == gpsLocation) {
                return RoadFollowerStatus(
                    frechetAverageOrMax(),
                    if (frechetQueue.size > FRECHET_QUEUE_SIZE / 2) {
                        RoadFollowerState.LOCKED
                    } else {
                        RoadFollowerState.UNLOCKED
                    }
                )
            }

            // Get the shortest D min value (distance from new location to previous chord ends)
            dMin = min(
                ruler.distance(gpsLocation, lastChordPoints[0]),
                ruler.distance(gpsLocation, lastChordPoints[1])
            )

            // Calculate the average point gap
            pointGap = ruler.distance(gpsLocation, lastLocation)
            if (averagePointGap == 0.0)
                averagePointGap = pointGap
            else {
                averagePointGap *= 0.9
                averagePointGap += (0.1 * pointGap)
            }
            gpsHeading = bearingFromTwoPoints(lastLocation, gpsLocation)
        }

        ils.line?.let { line ->

            val lastNearestPoint = nearestPoint
            // Get the nearest point on our accumulated LineString to the GPS location. Everything
            // returned is valid except for the heading. The heading is relative to the direction of
            // the accumulated LineString which may be the opposite direction to the line within the
            // Way.
            nearestPoint =
                ruler.distanceToLineString(gpsLocation, ils.line as LineString)
            nearestPoint?.let { nearestPoint ->

                if ((lastNearestPoint != null) &&
                    !lastNearestPoint.positionAlongLine.isNaN() &&
                    !nearestPoint.positionAlongLine.isNaN()
                ) {
                    val delta = nearestPoint.positionAlongLine - lastNearestPoint.positionAlongLine
                    if ((directionOnLine != 0.0) && ((sign(delta) != sign(directionOnLine)) || (delta == 0.0))) {
                        // Change of direction? Only report it once the sign mismatch has been
                        // sustained for several ticks in a row (the hysteresis counter reaching
                        // zero), not on every individual tick leading up to that - a single
                        // transient sign flip (GPS noise, or a duplicated coordinate at a junction
                        // node where this concatenated LineString has a kink - see
                        // IndexedLineString) shouldn't exclude an otherwise well-tracked follower
                        // from being selected for that one tick, before the hysteresis has even
                        // decided anything really changed.
                        --directionHysteresis
                        if (directionHysteresis == 0) {
                            directionHysteresis = 5
                            directionOnLine = delta
                            directionChange = true
                        }
                    } else {
                        directionHysteresis = 5
                        directionOnLine = delta
                    }
                } else {
                    directionHysteresis = 5
                }

                val routeIndex = ils.getWayIndex(nearestPoint.index)
                if (routeIndex != null) {
                    currentNearestRoad = route[routeIndex]

                    if (ils.direction?.get(routeIndex) == false) {
                        // The coordinates were reversed, so we need to reverse the heading
                        nearestPoint.heading = (nearestPoint.heading + 180.0) % 360.0
                    }
                }

                // Dispose of this if we're a long way away. 30m is generous for the dense GPS
                // samples typical of walking pace, but driving can easily produce an occasional
                // much larger gap between samples (e.g. a brief signal gap, or just a lower
                // sampling rate relative to speed) without actually having lost the road - so
                // this scales with how far apart consecutive samples actually are.
                if (nearestPoint.distance > max(30.0, pointGap * 1.5)) {
                    return RoadFollowerStatus(Double.MAX_VALUE, RoadFollowerState.DISTANT)
                }

                // Last point
                lastCenter = gpsLocation
                var matchedPoint = nearestPoint
                val previousGpsLocation = lastGpsLocation
                val previousMatchedLocation = lastMatchedLocation
                if (previousGpsLocation != null && previousMatchedLocation != null) {
                    val c1 = bearingFromTwoPoints(previousGpsLocation, previousMatchedLocation.point)
                    val d1 = ruler.distance(previousGpsLocation, previousMatchedLocation.point)
                    var ar = k.pow(pointGap / averagePointGap)
                    if (ar.isNaN()) ar = 1.0
                    lastCenter = getDestinationCoordinate(gpsLocation, c1, d1 * ar)
                    matchedPoint =
                        ruler.distanceToLineString(
                            lastCenter,
                            currentNearestRoad.geometry as LineString
                        )
                    radius = max(dMin, ruler.distance(gpsLocation, previousGpsLocation) * ar)
                }

                if (matchedPoint.distance > radius)
                    radius = matchedPoint.distance * 1.2

                // radius is a carried-over field, not recalculated from scratch each tick - after
                // a single large gap inflates it, it takes a few ticks to settle back down even
                // once samples return to their normal spacing. Comparing it against a threshold
                // scaled by the instantaneous pointGap (which snaps back immediately) would then
                // reject it as anomalous right as it's in the middle of legitimately catching up.
                // averagePointGap decays slowly, on the same sort of timescale as radius itself,
                // so it stays elevated for a few ticks after a big gap and gives radius room to
                // settle before this starts tightening again.
                if (radius > max(30.0, averagePointGap * 5.0)) {
                    return RoadFollowerStatus(Double.MAX_VALUE, RoadFollowerState.DISTANT)
                }

                val directionToNearestPoint = bearingFromTwoPoints(lastCenter, matchedPoint.point)
                val chordLength = sqrt(
                    (radius * radius) - (matchedPoint.distance * matchedPoint.distance)
                ) * 2

                // A chordLength of exactly zero is a degenerate numeric edge case (matchedPoint.
                // distance landing right at radius, e.g. from floating-point rounding) rather than
                // a sign the follower is genuinely a bad match - it can happen on an otherwise
                // well-tracked follower for one tick. Skip adding it rather than clearing the whole
                // queue, which would needlessly throw away an established, accumulated lock (this
                // used to matter less before frechetAverageOrMax/isMatchConfident started relying
                // on queue size/contents as a genuine confidence signal).
                if (chordLength != 0.0)
                    frechetQueue.addLast(chordLength)

                if (frechetQueue.size > FRECHET_QUEUE_SIZE)
                    frechetQueue.removeFirst()

                val chordAngle = asin(chordLength / (2 * radius))
                lastChordPoints[0] = getDestinationCoordinate(
                    lastCenter,
                    directionToNearestPoint + fromRadians(chordAngle),
                    radius
                )
                lastChordPoints[1] = getDestinationCoordinate(
                    lastCenter,
                    directionToNearestPoint - fromRadians(chordAngle),
                    radius
                )

                lastMatchedLocation = matchedPoint
            }
            lastGpsLocation = gpsLocation
        }

        val roadHeading = nearestPoint?.heading
        if (!gpsHeading.isNaN() && roadHeading != null) {
            val headingDifference = calculateSmallestAngleBetweenLines(gpsHeading, roadHeading)
            // gpsHeading is the straight-line bearing of the chord between this and the last GPS
            // fix, compared against the road's local tangent heading at the matched point. For
            // widely-spaced samples (e.g. driving), that chord can cut across a bend in the road,
            // so it legitimately differs more from the local tangent than it would for the dense,
            // closely-spaced samples this 45 degree tolerance was originally tuned for - widen it
            // in proportion to how much bigger than usual this particular gap is, capped at 90
            // degrees so a genuine reversal/wrong-way reading is still always rejected.
            val gapRatio = if (averagePointGap > 0.0) (pointGap / averagePointGap) else 1.0
            val toleranceDegrees = min(90.0, 45.0 * max(1.0, gapRatio))
            if (headingDifference > toleranceDegrees) {
                // Unreliable GPS heading - the GPS is moving in a very different direction
                // to the road.
                if (frechetQueue.size > (FRECHET_QUEUE_SIZE / 2))
                    return RoadFollowerStatus(frechetQueue.average(), RoadFollowerState.ANGLED_AWAY)

                return RoadFollowerStatus(Double.MAX_VALUE, RoadFollowerState.ANGLED_AWAY)
            }
        }
        return RoadFollowerStatus(
            frechetAverageOrMax(),
            when {
                directionChange -> RoadFollowerState.DIRECTION_CHANGED
                frechetQueue.size > (FRECHET_QUEUE_SIZE / 2) -> RoadFollowerState.LOCKED
                else -> RoadFollowerState.UNLOCKED
            }
        )
    }

    fun chosen(): PointAndDistanceAndHeading? {
        nearestPoint?.let { nearestPoint ->
            return nearestPoint
        }
        return null
    }
}

/**
 * @param networkTree overrides the vehicleMode-based road/path tree selection below, e.g. pass
 * TreeId.TRANSIT to get a filter that matches against the railway network instead of roads. A
 * given instance's followerList only ever holds Ways from one network at a time - a road and a
 * railway are different connectivity graphs, so mixing them in one follower list wouldn't make
 * sense. Create a separate MapMatchFilter instance for each network you want to track.
 */
class MapMatchFilter(private val networkTree: TreeId? = null) {

    //
    // This filter is partially based on the following paper:
    //
    // "An Improved Map-Matching Technique Based on the Fréchet Distance Approach for Pedestrian
    // Navigation Services" by Yoonsik Bang, Jiyoung Kim and Kiyun Yu.
    //
    // https://pmc.ncbi.nlm.nih.gov/articles/PMC5087552/
    //
    // The search radius value is calculated to try and keep an open free space path along the
    // current nearest road.
    //

    private fun matchTree(vehicleMode: Boolean): TreeId {
        return networkTree ?: if (vehicleMode) TreeId.ROADS else TreeId.WAYS_SELECTION
    }


    val followerList: MutableList<RoadFollower> = mutableListOf()
    var matchedLocation: PointAndDistanceAndHeading? = null
    var matchedWay: Way? = null
    var matchedFollower: RoadFollower? = null
    var lastLocation: LngLatAlt? = null

    /**
     * True once [matchedWay] has been tracked consistently for a real stretch, rather than just
     * this one tick - see RoadFollower's LOCKED state, which uses the same frechetQueue-size bar.
     * A momentary, coincidental proximity (e.g. a road crossing a railway line at a level
     * crossing, briefly pulling in a rail-network match) never builds up this kind of sustained
     * history before the two lines diverge again, whereas a genuine, continuous match - like
     * actually riding a train - holds it easily. Used to gate UserGeometry.probablyOnTrain so a
     * momentary rail-network coincidence isn't mistaken for genuinely being on a train.
     *
     * Backed by a grace window (see [updateMatchConfidence]), not just the current tick's
     * follower - colorIndex isn't a stable identity across ticks (an extended follower computes
     * its own colorIndex arithmetically from its parent's, so two unrelated followers can share
     * one), and the follower object actually selected as matchedFollower routinely gets replaced
     * by a fresh one - sometimes via a brief tick where there's no candidate at all - as its route
     * is extended onto a new Way, even mid-journey on a real, continuously-held train lock. The
     * fresh follower starts with an empty frechetQueue and has to rebuild it from scratch, which
     * would otherwise cost several ticks of lost confidence on every such handoff. A genuinely
     * sustained loss (no confident match for many ticks in a row) still falls out of the grace
     * window regardless, since the counter climbs continuously throughout it.
     */
    var isMatchConfident: Boolean = false
        private set
    private var ticksSinceConfidentMatch = GRACE_TICKS_AFTER_LOSING_CONFIDENCE + 1

    private fun updateMatchConfidence() {
        val individuallyConfident = (matchedFollower?.frechetQueue?.size ?: 0) > (FRECHET_QUEUE_SIZE / 2)
        // Also grace a tick where matchedWay itself goes null (not just a low-confidence
        // follower) - a real, continuous train journey can briefly lose every candidate for a
        // single tick (e.g. approaching points/a junction) and immediately reacquire one, and
        // that reacquired follower still has to rebuild its own frechetQueue from scratch. Without
        // grace here, that one-tick full loss would otherwise reset the count and force the whole
        // rebuild (several ticks) to run with no grace at all. A genuinely sustained loss (no
        // candidate for many ticks in a row) still correctly falls out of the grace window either
        // way, since the counter keeps climbing throughout it.
        ticksSinceConfidentMatch = if (individuallyConfident) 0 else ticksSinceConfidentMatch + 1
        isMatchConfident = individuallyConfident ||
            (ticksSinceConfidentMatch <= GRACE_TICKS_AFTER_LOSING_CONFIDENCE)
    }

    fun addWaysToFollowers(
        intersection: Intersection,
        follower: RoadFollower,
        iterator: MutableListIterator<RoadFollower>,
        ruler: Ruler
    ) {
        var extended = false
        var extensionAddedAtStart = false
        for (member in intersection.members.withIndex()) {

            // Don't add a Way more than once to the route
            if (follower.route.contains(member.value)) continue

            // Skip JOINERS
            if (member.value.wayType == WayType.JOINER) continue
            if (!extended) {
                // Try and extend the follower to this way
                if (follower.extendToNewWay(member.value, ruler)) {
                    extended = true
                    extensionAddedAtStart = follower.route.first() == member.value
                    continue
                }
            }
            // Create a new follower which adds this way to the route
            iterator.add(
                follower.createExtendedFollower(
                    extensionAddedAtStart,
                    member.value,
                    member.index + 1
                )
            )
        }
    }

    /**
     * extendFollowerList looks for nearby roads and ensures that they are included in the list
     * of RoadFollowers that we have. There should be a RoadFollower for every possible road segment
     * combination.
     */
    fun extendFollowerList(location: LngLatAlt, gridState: GridState, vehicleMode: Boolean) {
        val roadTree = gridState.getFeatureTree(matchTree(vehicleMode))

        // 20m is generous for the dense GPS samples typical of walking pace, but at driving speed
        // a new road/intersection can easily be more than 20m from any single sample - see the
        // equivalent scaling in RoadFollower.update/trimRoute.
        val searchRadius = lastLocation?.let { max(20.0, gridState.ruler.distance(location, it) * 1.5) }
            ?: 20.0
        val roads = roadTree.getNearestCollection(location, searchRadius, 8, gridState.ruler)

        if (followerList.isEmpty()) {
            if (roads.features.isNotEmpty()) {
                // Start off with a follower for the nearest road
                followerList.add(
                    RoadFollower(
                        this,
                        MutableList(1) { roads.first() as Way },
                        lastLocation,
                        colorIndex
                    )
                )
                ++colorIndex
            }
        }

        for (road in roads) {
            val way = road as Way
            var added = false

            for (follower in followerList) {
                if (follower.route.contains(way)) {
                    // We already have some followers that are following this Way, so don't add more
                    added = true
                    break
                }
                if ((way.properties?.containsKey("dead-end:forward") == true) ||
                    (way.properties?.containsKey("dead-end:backward") == true)
                ) {
                    // The way is a dead end, don't follow it if it's also short
                    if (way.length < 20.0) {
                        added = true
                        break
                    }
                }
            }
            if (!added) {

                // For each follower, see if we can append this way
                val iterator = followerList.listIterator()
                while (iterator.hasNext()) {
                    val follower = iterator.next()
                    val lastWayInRoute = follower.route.last()
                    val (intersection, _) = lastWayInRoute.doesIntersect(way)
                    if (intersection != null) {
                        // This road intersects with the last way, so it can either replace it, or
                        // be added on to it.
                        addWaysToFollowers(intersection, follower, iterator, gridState.ruler)
                        added = true
                    } else {
                        val firstWayInRoute = follower.route.first()
                        val (firstIntersection, _) = firstWayInRoute.doesIntersect(way)
                        if (firstIntersection != null) {
                            // This road intersects with the first way, so it can either replace it,
                            // or be added on to it.
                            addWaysToFollowers(
                                firstIntersection,
                                follower,
                                iterator,
                                gridState.ruler
                            )
                            added = true
                        }
                    }
                }
                // If no follower added this Way, then create a new follower for it
                if (!added) {
                    followerList.add(
                        RoadFollower(
                            this,
                            MutableList(1) { way },
                            lastLocation,
                            colorIndex
                        )
                    )
                    ++colorIndex
                }
            }
        }

        // De-duplicate list of followers
        val followerIterator = followerList.listIterator()
        val followerHashes = HashSet<Int>()
        while (followerIterator.hasNext()) {
            val follower = followerIterator.next()
            val hash = follower.ils.hashCode
            if (followerHashes.contains(hash))
                followerIterator.remove()
            else
                followerHashes.add(hash)
        }
    }

    var colorIndex = 0
    fun filter(
        location: LngLatAlt,
        gridState: GridState,
        collection: FeatureCollection,
        dump: Boolean,
        strings: LocalizedStrings?,
        vehicleMode: Boolean = false
    ): Triple<LngLatAlt?, Feature?, String> {

        extendFollowerList(location, gridState, vehicleMode)

        var lowestFrechet = Double.MAX_VALUE
        var lowestFollower: RoadFollower? = null
        val previouslyMatchedFollower = matchedFollower
        var previousMatchFrechet: Double? = null
        // The follower whose raw (unsmoothed) GPS-to-road distance is the smallest this tick,
        // regardless of its frechetAverage - see the raw-distance override below, which needs
        // this rather than just lowestFollower, since a follower that's clearly the best
        // positional match isn't always the frechetAverage-argmin (e.g. it may still be building
        // up its own history after just being created at a junction).
        var closestByRawDistance: RoadFollower? = null
        var closestRawDistance = Double.MAX_VALUE
        var closestByRawDistanceFrechet = Double.MAX_VALUE
        val followerIterator = followerList.listIterator()
        while (followerIterator.hasNext()) {
            val follower = followerIterator.next()
            val frechetStatus = follower.update(location, collection, gridState.ruler)

            if (frechetStatus.state == RoadFollowerState.DISTANT) {
                followerIterator.remove()
                continue
            }
            if ((frechetStatus.state == RoadFollowerState.ANGLED_AWAY) ||
                (frechetStatus.state == RoadFollowerState.DIRECTION_CHANGED)
            ) {
                continue
            }

            val way = follower.currentNearestRoad
            if (vehicleMode && way.isPath()) {
                // A car/bus can't be on a footway/cycleway - don't let a follower that's drifted
                // onto one (e.g. during a momentary low-speed reading near a junction, which
                // briefly widens the search to TreeId.WAYS_SELECTION - see matchTree) be selected
                // as the match, even though it's still tracked here in the follower list.
                continue
            }

            if (follower === previouslyMatchedFollower) {
                previousMatchFrechet = frechetStatus.frechetAverage
            }

            follower.nearestPoint?.distance?.let { rawDistance ->
                if (rawDistance < closestRawDistance) {
                    closestRawDistance = rawDistance
                    closestByRawDistance = follower
                    closestByRawDistanceFrechet = frechetStatus.frechetAverage
                }
            }

            if (frechetStatus.frechetAverage < lowestFrechet) {
                var skip = false
                matchedWay?.let { matched ->
                    if (matched != way) {
                        // Can we get to this followers matched location from the last matched
                        // location via the road/path network?

                        // First check that we aren't just hopping between footway=sidewalk for the
                        // same Way. These aren't usually well inter-connected and so running
                        // Dijkstra on them will result in a longer distance than it really is. For
                        // example, crossing the road here:
                        // https://www.openstreetmap.org/query?lat=55.941074&lon=-4.320473
                        // is really moving between two sidewalks and is easily done regardless of
                        // the Dijkstra distance.

                        var useDijkstra = true
                        if (matched.isSidewalkOrCrossing() || way.isSidewalkOrCrossing()) {
                            // We're matching on a sidewalk, see if the other way is either the
                            // associated way or another sidewalk for the associated way
                            val roadTree = gridState.getFeatureTree(matchTree(vehicleMode))
                            addSidewalk(matched, roadTree, gridState.ruler, strings)
                            addSidewalk(way, roadTree, gridState.ruler, strings)

                            val matchedPavement = matched.properties?.get("pavement")
                            val matchedName = matched.name
                            val wayPavement = way.properties?.get("pavement")
                            val wayName = way.name

                            if ((matchedPavement != null) &&
                                ((matchedPavement == wayName) || (matchedPavement == wayPavement))
                            ) {
                                // The matched way is a sidewalk, and the other way is either the
                                // associated way or another sidewalk for the associated way
                                useDijkstra = false
                            } else if ((wayPavement != null) && (wayPavement == matchedName)) {
                                // The other way is a sidewalk, and the matched way is its
                                // associated way
                                useDijkstra = false
                            }
                        }
                        if (useDijkstra) {
                            // matchedWay is normally only ever set alongside matchedLocation, and
                            // every follower in the list has just been given a chosen point by
                            // update(). If that invariant is ever violated we can't run the
                            // Dijkstra check, so treat this candidate as not a good enough match
                            // rather than crash.
                            val fromPoint = matchedLocation?.point
                            val toPoint = follower.chosen()?.point
                            if (fromPoint == null || toPoint == null) {
                                skip = true
                            } else {
                                val testDistance = (follower.averagePointGap * 8) + 15.0
                                val shortestDistance = findShortestDistance(
                                    fromPoint,
                                    matched,
                                    toPoint,
                                    way,
                                    null,
                                    null,
                                    testDistance
                                )
                                if (shortestDistance.distance >= testDistance)
                                    skip = true
                                shortestDistance.tidy()
                            }
                        }
                    }
                }
                if (!skip) {
                    lowestFrechet = frechetStatus.frechetAverage
                    lowestFollower = follower
                }
            }
        }

        // Prefer sticking with the previously-matched follower unless a challenger clearly beats
        // it. At driving speed, radius (see RoadFollower.update) has to grow large enough to
        // tolerate the bigger gap between GPS fixes, and a large enough radius can end up
        // containing a nearby parallel/service road as well as the true road - at which point
        // their frechetAverage values are within noise of each other, and the bare "lowest wins"
        // comparison above would flip the match between them on essentially a coin toss from one
        // tick to the next (see e.g. a service road to a fire station right next to the real
        // road). Only switch away from the current match once a challenger beats it by a real
        // margin, not by an arbitrarily small amount - a genuine transition (the vehicle actually
        // turning off) shows up as a much bigger gap than that, or as the old follower going
        // DISTANT/ANGLED_AWAY/DIRECTION_CHANGED (and so having no previousMatchFrechet to compare
        // against) rather than merely being a fraction worse.
        //
        // But frechetAverage (a chordLength derived from radius) isn't a reliable proxy for raw
        // proximity - two followers can end up with similar-looking frechetAverage values even
        // though one's actual GPS-to-road distance is a metre and the other's is 40 (e.g. right
        // after a junction, where an initial ambiguous tick locks onto the wrong branch, and the
        // wrong follower's radius/chordLength then look "reasonable" for a long stretch even as
        // every subsequent fix lands almost exactly on the real road). So the hysteresis above is
        // overridden in favour of closestByRawDistance (not necessarily lowestFollower - the
        // clearly-best positional match is often a follower that's still building up its own
        // frechetAverage history, e.g. one created moments earlier at a junction, so it isn't
        // always the frechetAverage-argmin either) when its raw nearestPoint.distance is
        // overwhelmingly better than the incumbent's. This only kicks in once the incumbent is
        // already a long way off (RAW_DISTANCE_OVERRIDE_MINIMUM_METRES), so it doesn't reopen the
        // original close-parallel-road ambiguity this hysteresis exists to prevent, where both
        // roads' raw distances are small.
        val previousMatchDistance = previouslyMatchedFollower?.nearestPoint?.distance
        val challengerRawDistanceWins = (previousMatchDistance != null) &&
            (closestByRawDistance != null) && (closestByRawDistance !== previouslyMatchedFollower) &&
            (previousMatchDistance > RAW_DISTANCE_OVERRIDE_MINIMUM_METRES) &&
            (closestRawDistance < previousMatchDistance * RAW_DISTANCE_OVERRIDE_RATIO)
        if (challengerRawDistanceWins) {
            lowestFollower = closestByRawDistance
            lowestFrechet = closestByRawDistanceFrechet
        } else if ((previousMatchFrechet != null) && (lowestFollower !== previouslyMatchedFollower) &&
            (lowestFrechet >= previousMatchFrechet * FOLLOWER_SWITCH_HYSTERESIS_FACTOR)
        ) {
            lowestFollower = previouslyMatchedFollower
            lowestFrechet = previousMatchFrechet
        }

        lastLocation = location
        if (lowestFollower != null) {
            matchedLocation = lowestFollower.chosen()
            matchedFollower = lowestFollower
            matchedWay = lowestFollower.currentNearestRoad
            val color = lowestFollower.color
            matchedLocation?.let { matchedLocation ->
                updateMatchConfidence()
                return Triple(matchedLocation.point, matchedWay, color)
            }
        }
        matchedLocation = null
        matchedFollower = null
        matchedWay = null

        updateMatchConfidence()
        return Triple(null, null, "")
    }
}
