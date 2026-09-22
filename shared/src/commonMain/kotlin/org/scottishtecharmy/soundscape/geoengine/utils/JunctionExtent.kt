package org.scottishtecharmy.soundscape.geoengine.utils

import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Intersection
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayType
import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * How big a junction is on the ground.
 *
 * An Intersection is a single point where road *centre-lines* meet, because that is what a shared
 * vertex in the tile data is. Nobody walks to that point. A pedestrian coming up a road stops at
 * the kerb of the road crossing it, which is short of the centre-line node by that road's
 * half-width; on a wide junction that is 5-10m of a distance whose whole useful range is 50m.
 *
 * So "how far is the intersection" means "how far to the kerb", and this file works out the
 * difference between the two.
 */

/**
 * Metres from a road's centre-line to its kerb, by the tile schema's `class`.
 *
 * These are estimates, and deliberately the only numbers here worth arguing about - everything
 * else in this file is geometry. Note the schema collapses residential and unclassified into
 * `minor`, so that one covers most of a town.
 *
 * Tuning them is cheaper than it looks: formatDistanceAndDirection rounds to the nearest 5m below
 * 100m, so an error of a metre or two never reaches the user.
 */
private val halfWidthByClass = mapOf(
    "motorway" to 6.0,      // per carriageway - a dual is two Ways, each its own arm
    "trunk" to 6.0,
    "primary" to 5.5,
    "secondary" to 5.0,
    "tertiary" to 4.5,
    "minor" to 3.0,
    "busway" to 3.5,
    "service" to 2.0,
    "track" to 1.5,
    "path" to 1.0,
)

private const val defaultHalfWidthMetres = 3.0
private const val drivewayHalfWidthMetres = 1.5
private const val roundaboutHalfWidthMetres = 3.0

/**
 * Below this angle an arm is a fork or a continuation of the road being walked, not something
 * crossing it, so it has no kerb line to stop at.
 */
private const val collinearArmDegrees = 20.0

/**
 * The angle at which `halfWidth / sin(theta)` stops being allowed to grow. The true setback does
 * go to infinity as an arm approaches parallel, but a shallow arm is a merge rather than a
 * crossing long before that, and an unclamped value would announce a junction a block early.
 */
private const val shallowArmClampDegrees = 25.0

/** A single arm can never set the kerb back further than this multiple of its own half-width. */
private const val maxSetbackPerArmFactor = 2.5

/** Nor can the junction as a whole, however many arms it has or however wide they are. */
private const val maxJunctionSetbackMetres = 15.0

/**
 * Metres from this Way's centre-line to its kerb.
 *
 * Estimated from the road's classification. Where the map records the real width - a tagged
 * `width` or `lanes`, a separately drawn pavement whose offset can be measured, a crossing way
 * spanning the carriageway - that is better evidence than any table, and this is where it will be
 * read; none of it is in the tiles today.
 */
fun Way.halfWidth(): Double {
    // Tile-edge joiners are synthetic: zero length, no class, no properties. They are an artefact
    // of stitching two tiles together, not an arm of anything.
    if (wayType == WayType.JOINER) return 0.0

    if (properties?.get("junction") == "roundabout") return roundaboutHalfWidthMetres

    val base = halfWidthByClass[featureClass] ?: defaultHalfWidthMetres
    if ((featureClass == "service") &&
        (properties?.get("service") in setOf("driveway", "parking_aisle"))
    ) {
        return drivewayHalfWidthMetres
    }
    return base
}

/**
 * True for arms that have no kerb line worth stopping at: the approach itself, tile-edge joiners,
 * pavements and crossings, and the short unnamed stubs joining a road to its own pavement.
 */
private fun Way.isJunctionArm(
    intersection: Intersection,
    approach: Way,
    gridState: GridState,
    strings: LocalizedStrings?,
): Boolean {
    if (this === approach) return false
    if (wayType == WayType.JOINER) return false
    if (isSidewalkOrCrossing()) return false
    if (isSidewalkConnector(intersection, approach, gridState, strings)) return false
    return true
}

/**
 * How far back along [approach] from this intersection's node the kerb of the widest road
 * crossing it lies - i.e. how much shorter the walk is than the distance to the node.
 *
 * The kerb line of a crossing road runs parallel to it, offset by its half-width, so an approach
 * meeting it at angle theta reaches it `halfWidth / sin(theta)` before the node.
 *
 * Only the along-track component counts. A pedestrian on the pavement is already offset sideways
 * from the road centre-line, so although the kerb *corner* is further from the node than this
 * (sqrt(2) x the offset on a symmetric crossroads), they have already covered that part: the
 * lateral offset moves the point, not the distance left to walk.
 *
 * Returns 0.0 when nothing qualifies, which leaves the caller with the plain centre-line distance.
 */
fun Intersection.setbackAlong(
    approach: Way,
    gridState: GridState,
    strings: LocalizedStrings?,
): Double {
    // A roundabout's arms meet a ring, not each other, and what a pedestrian arrives at is the
    // give-way line rather than a kerb a half-width back. The model does not describe that shape,
    // and no setback is a much better wrong answer than a confident one.
    if (members.any { it.properties?.get("junction") == "roundabout" }) return 0.0

    val approachHeading = approach.heading(this)
    var setback = 0.0
    for (arm in members) {
        if (!arm.isJunctionArm(this, approach, gridState, strings)) continue

        val angle = calculateSmallestAngleBetweenLines(approachHeading, arm.heading(this))
        if (angle < collinearArmDegrees) continue

        val halfWidth = arm.halfWidth()
        val alongTrack = halfWidth / sin(toRadians(max(angle, shallowArmClampDegrees)))
        setback = max(setback, min(alongTrack, halfWidth * maxSetbackPerArmFactor))
    }
    return min(setback, maxJunctionSetbackMetres)
}
