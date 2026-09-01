package org.scottishtecharmy.soundscape.clipper

import kotlin.math.round

/**
 * Units per degree for the clipper's working lattice.
 *
 * A power of two, so multiplying a coordinate in degrees by it is exact in IEEE-754 and
 * snapping is a single [round]. 1 / 2^24 degree is 5.96e-8 degrees, about 6.6mm at UK
 * latitudes - roughly one ninetieth of the z14 extent-4096 MVT quantum (5.4e-6 degrees, about
 * 0.37m). Genuine tile vertices are therefore hundreds of lattice units apart and never
 * collide, while the sub-millimetre disagreements that come out of computed intersection
 * points collapse onto a single lattice point.
 *
 * Public because PolygonClipperParityTest derives its comparison tolerances from it, and
 * hard-coding the number in the test would rot.
 */
const val LATTICE_SCALE: Double = (1 shl 24).toDouble()

/**
 * Largest translated lattice coordinate for which [signedArea] is still exact. Each of its
 * four differences must stay below 2^26 so that the products stay below 2^52 and their
 * difference below 2^53, the largest integer Double represents exactly.
 */
private const val MAX_LATTICE_EXTENT: Double = (1 shl 26).toDouble()

/**
 * Halve the lattice scale until a polygon of [extentDegrees] across fits within
 * [MAX_LATTICE_EXTENT] units, keeping [signedArea] exact. At the default scale that bound is
 * 4 degrees across, far larger than any POI or building polygon, so this is a guard rail
 * rather than a path we expect to take.
 */
internal fun chooseScale(extentDegrees: Double): Double {
    var scale = LATTICE_SCALE
    while (scale > 1.0 && extentDegrees * scale > MAX_LATTICE_EXTENT) {
        scale *= 0.5
    }
    return scale
}

/** Snap a coordinate in degrees onto the lattice, returning an integer-valued Double. */
internal fun snap(degrees: Double, scale: Double): Double = round(degrees * scale)

/**
 * Twice the signed area of the triangle (p0, p1, p2) - positive when p2 lies to the left of
 * p0 -> p1, zero when the three are collinear.
 *
 * Every coordinate here is an integer-valued Double below 2^26 (see [chooseScale]), so both
 * products are integers below 2^52 and their difference is an integer below 2^53: the whole
 * expression is exact, with no rounding at all. That exactness is not an optimisation, it is
 * the correctness argument for the sweep - it is what makes the sweep-status ordering a
 * genuine total order rather than an approximate one, and an approximate order is what makes
 * plane-sweep implementations corrupt themselves.
 */
internal fun signedArea(p0: Pt, p1: Pt, p2: Pt): Double =
    (p0.x - p2.x) * (p1.y - p2.y) - (p1.x - p2.x) * (p0.y - p2.y)

/**
 * A point on the clipper's working lattice. Both fields are integer-valued Doubles, translated
 * so that the combined bounding box of the two inputs starts at the origin.
 *
 * Deliberately not a data class: equality here must be the exact `==` on Double that the sweep
 * relies on, and it must never quietly acquire the -0.0 / NaN behaviour of the generated
 * equals. Points are compared with [same] and otherwise held by identity.
 */
internal class Pt(val x: Double, val y: Double) {
    fun same(other: Pt): Boolean = x == other.x && y == other.y

    override fun toString(): String = "($x, $y)"
}
