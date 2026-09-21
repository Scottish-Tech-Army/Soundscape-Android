package org.scottishtecharmy.soundscape.clipper

/**
 * One endpoint of one edge, as processed by the plane sweep. Edges are represented as a pair
 * of events that point at each other through [otherEvent]; the one the sweep reaches first is
 * the [left] one.
 *
 * There is no inside/outside bookkeeping here. The sweep's only job is to build the
 * arrangement - to split every edge at every crossing - and which of the resulting pieces lie
 * on the result's boundary is worked out afterwards, geometrically, in
 * MartinezClipper.boundaryEdges.
 */
internal class SweepEvent(
    var point: Pt,
    var left: Boolean,
    var otherEvent: SweepEvent?,
    val isSubject: Boolean,
) {
    /** Which input ring this edge came from. Only used to break comparator ties. */
    var contourId: Int = 0

    val other: SweepEvent
        get() = otherEvent ?: error("SweepEvent used before its partner was set")

    /** True when this edge passes below [p]. */
    fun isBelow(p: Pt): Boolean {
        val p0 = point
        val p1 = other.point
        return if (left) {
            (p0.x - p.x) * (p1.y - p.y) - (p1.x - p.x) * (p0.y - p.y) > 0
        } else {
            (p1.x - p.x) * (p0.y - p.y) - (p0.x - p.x) * (p1.y - p.y) > 0
        }
    }

    fun isAbove(p: Pt): Boolean = !isBelow(p)

    override fun toString(): String =
        "${if (left) "L" else "R"}$point->${otherEvent?.point} ${if (isSubject) "subj" else "clip"}"
}

/**
 * Order in which the sweep visits events: left to right, then bottom to top. Right endpoints
 * sort before left ones at the same point so an edge is removed from the sweep status before
 * the next one is added at the same place.
 */
internal fun compareEvents(e1: SweepEvent, e2: SweepEvent): Int {
    val p1 = e1.point
    val p2 = e2.point

    if (p1.x > p2.x) return 1
    if (p1.x < p2.x) return -1
    if (p1.y != p2.y) return if (p1.y > p2.y) 1 else -1

    // Same point, one a left endpoint and one a right endpoint: the right one goes first.
    if (e1.left != e2.left) return if (e1.left) 1 else -1

    // Same point, same side, not collinear: the lower edge goes first.
    if (signedArea(p1, e1.other.point, e2.other.point) != 0.0) {
        return if (e1.isBelow(e2.other.point)) -1 else 1
    }

    // Same point, same side, collinear: break the tie consistently.
    return if (!e1.isSubject && e2.isSubject) 1 else -1
}

/**
 * Order of edges within the sweep status, bottom to top at the current sweep position.
 *
 * Only ever applied to edges that are simultaneously in the status, and by then every crossing
 * between them has been split out, so no two of them cross. That is what makes this a
 * consistent total order and what makes the binary search in [SweepStatus] sound.
 */
internal fun compareSegments(le1: SweepEvent, le2: SweepEvent): Int {
    if (le1 === le2) return 0

    if (signedArea(le1.point, le1.other.point, le2.point) != 0.0 ||
        signedArea(le1.point, le1.other.point, le2.other.point) != 0.0
    ) {
        // Not collinear. Sharing a left endpoint, the right endpoints decide.
        if (le1.point.same(le2.point)) return if (le1.isBelow(le2.other.point)) -1 else 1

        // Starting at the same x, the lower start decides.
        if (le1.point.x == le2.point.x) return if (le1.point.y < le2.point.y) -1 else 1

        // Otherwise whichever was inserted first decides which is below the other.
        if (compareEvents(le1, le2) == 1) return if (le2.isAbove(le1.point)) -1 else 1
        return if (le1.isBelow(le2.point)) -1 else 1
    }

    // Collinear edges of different polygons: keep the subject below, consistently.
    if (le1.isSubject != le2.isSubject) return if (le1.isSubject) -1 else 1

    // Collinear edges of the same polygon.
    if (le1.point.same(le2.point)) {
        if (le1.other.point.same(le2.other.point)) return 0
        return if (le1.contourId > le2.contourId) 1 else -1
    }
    return if (compareEvents(le1, le2) == 1) 1 else -1
}
