package org.scottishtecharmy.soundscape.clipper

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sqrt

/**
 * The boolean operations the sweep supports. Only UNION is reachable from the public API, but
 * the others cost one `when` branch each in [MartinezClipper.interiorAt], and commonTest - an
 * associated compilation, so it can see `internal` - uses them to exercise the arrangement far
 * more sharply than UNION alone can.
 */
internal enum class BooleanOpType { INTERSECTION, UNION, DIFFERENCE, XOR }

/**
 * Boolean operations on polygons, by plane sweep, after Martinez, Rueda and Feito (2009),
 * "A new algorithm for computing Boolean operations on polygons".
 *
 * The sweep builds the **arrangement**: it splits every input edge at every crossing, so that
 * afterwards the pieces meet only at their endpoints and each piece lies wholly inside or
 * wholly outside each input polygon. Martinez-Rueda is the right sweep for this job because
 * the polygons being merged are two clippings of the same OSM way from adjacent MVT tiles, so
 * they share long runs of exactly coincident, identically directed edges - see PolygonClipper -
 * and it handles collinear overlaps natively where naive clippers fall over.
 *
 * Which pieces of the arrangement lie on the result's boundary is then decided geometrically in
 * [boundaryEdges] rather than from the sweep's own inside/outside transition flags. The paper
 * carries those flags through the sweep, and they are correct on paper, but they are subtle -
 * their meaning for vertical edges especially - and when a sweep-status ordering glitch makes
 * one wrong it is wrong silently, keeping an edge that is interior to the union and turning a
 * solid lobe into a phantom hole. Asking the input geometry directly cannot disagree with the
 * input geometry, and on polygons of a few dozen vertices it costs almost nothing.
 *
 * Everything here works on the integer lattice set up by PolygonClipper, so [signedArea] is
 * exact and the comparators are genuine total orders.
 */
internal object MartinezClipper {

    /**
     * Run [op] over two polygons, each given as a list of closed rings of lattice points.
     *
     * Returns the result as a flat list of closed rings, with no exterior/hole distinction -
     * PolygonClipper.assemble works that out afterwards. Returns null when the sweep exceeded
     * its budget or the surviving edges did not form closed rings; callers treat that as
     * "these did not merge" rather than propagating a failure.
     */
    fun run(
        subject: List<List<Pt>>,
        clip: List<List<Pt>>,
        op: BooleanOpType,
    ): List<List<Pt>>? {
        val queue = EventHeap()
        var contourId = 0
        for (ring in subject) contourId = fillQueue(ring, true, contourId, queue)
        for (ring in clip) contourId = fillQueue(ring, false, contourId, queue)

        if (queue.isEmpty()) return emptyList()

        // Each initial edge can only be split so many times before the geometry is exhausted.
        // Blowing through this means the sweep has lost an invariant, and spinning here would
        // hang the tile-loading thread, so give up and let the caller degrade.
        val budget = 16 * queue.size + 1000

        val arrangement = subdivideSegments(queue, budget) ?: return null
        return connectEdges(arrangement, subject, clip, op, budget)
    }

    /** Turn one closed ring into sweep events, returning the next free contour id. */
    private fun fillQueue(
        ring: List<Pt>,
        isSubject: Boolean,
        contourId: Int,
        queue: EventHeap,
    ): Int {
        for (i in 0 until ring.size - 1) {
            val start = ring[i]
            val end = ring[i + 1]
            // Repeated vertices are common in tile data and give zero-length edges, which have
            // no orientation and would poison the comparators.
            if (start.same(end)) continue

            val e1 = SweepEvent(start, false, null, isSubject)
            val e2 = SweepEvent(end, false, e1, isSubject)
            e1.otherEvent = e2
            e1.contourId = contourId
            e2.contourId = contourId

            if (compareEvents(e1, e2) > 0) e2.left = true else e1.left = true

            queue.push(e1)
            queue.push(e2)
        }
        return contourId + 1
    }

    // ------------------------------------------------------------ building the arrangement

    /**
     * Sweep left to right, splitting edges wherever they meet, and return every event in the
     * order the sweep saw it. Afterwards each edge's [SweepEvent.other] points at its final
     * endpoint, so the left events enumerate the arrangement's edges.
     */
    private fun subdivideSegments(queue: EventHeap, budget: Int): List<SweepEvent>? {
        val status = SweepStatus()
        val sortedEvents = ArrayList<SweepEvent>()
        var processed = 0

        while (!queue.isEmpty()) {
            if (processed++ > budget) return null
            val event = queue.pop()
            sortedEvents.add(event)

            if (event.left) {
                val index = status.insert(event)
                val prev = status.at(index - 1)
                val next = status.at(index + 1)
                if (next != null) possibleIntersection(event, next, queue)
                if (prev != null) possibleIntersection(prev, event, queue)
            } else {
                // A right endpoint: retire the edge and let its former neighbours meet.
                val left = event.other
                val index = status.indexOf(left)
                if (index >= 0) {
                    val prev = status.at(index - 1)
                    val next = status.at(index + 1)
                    status.removeAt(index)
                    if (prev != null && next != null) possibleIntersection(prev, next, queue)
                }
            }
        }
        return sortedEvents
    }

    /**
     * Split the edge starting at [event] in two at [p], which lies between its endpoints. Both
     * halves go back on the queue.
     */
    private fun divideSegment(event: SweepEvent, p: Pt, queue: EventHeap) {
        val right = SweepEvent(p, false, event, event.isSubject)
        val left = SweepEvent(p, true, event.otherEvent, event.isSubject)
        right.contourId = event.contourId
        left.contourId = event.contourId

        // Guard against a snapped split point that would land out of order, which would put a
        // left event after its own right event and corrupt the sweep.
        if (compareEvents(left, event.other) > 0) {
            event.other.left = true
            left.left = false
        }

        event.other.otherEvent = left
        event.otherEvent = right

        queue.push(left)
        queue.push(right)
    }

    /**
     * Split [se1] and [se2] wherever they meet - at a crossing, or at the ends of a collinear
     * run where they overlap.
     */
    private fun possibleIntersection(se1: SweepEvent, se2: SweepEvent, queue: EventHeap) {
        val inter = findIntersection(se1.point, se1.other.point, se2.point, se2.other.point)
        if (inter.isEmpty()) return

        if (inter.size == 1) {
            val point = inter[0]
            if (!se1.point.same(point) && !se1.other.point.same(point)) {
                divideSegment(se1, point, queue)
            }
            if (!se2.point.same(point) && !se2.other.point.same(point)) {
                divideSegment(se2, point, queue)
            }
            return
        }

        // A polygon overlapping itself along a run. Splitting would not terminate, and the
        // pieces classify the same either way, so leave it alone.
        if (se1.isSubject == se2.isSubject) return

        // Collinear overlap. Order the four endpoints along the shared line, then trim each
        // edge back so that the shared run is a piece in its own right.
        val leftCoincide = se1.point.same(se2.point)
        val rightCoincide = se1.other.point.same(se2.other.point)

        val events = ArrayList<SweepEvent>(4)
        if (!leftCoincide) {
            if (compareEvents(se1, se2) == 1) {
                events.add(se2)
                events.add(se1)
            } else {
                events.add(se1)
                events.add(se2)
            }
        }
        if (!rightCoincide) {
            if (compareEvents(se1.other, se2.other) == 1) {
                events.add(se2.other)
                events.add(se1.other)
            } else {
                events.add(se1.other)
                events.add(se2.other)
            }
        }

        if (leftCoincide) {
            // Identical edges need no split at all; sharing only the left end, one trim does.
            if (!rightCoincide && events.size >= 2) {
                divideSegment(events[1].other, events[0].point, queue)
            }
            return
        }
        if (rightCoincide) {
            if (events.size >= 2) divideSegment(events[0], events[1].point, queue)
            return
        }
        if (events.size < 4) return

        if (events[0] !== events[3].otherEvent) {
            // Partial overlap: trim each edge back to the shared run.
            divideSegment(events[0], events[1].point, queue)
            divideSegment(events[1], events[2].point, queue)
        } else {
            // One edge wholly contains the other.
            divideSegment(events[0], events[1].point, queue)
            divideSegment(events[3].other, events[2].point, queue)
        }
    }

    /**
     * Intersect the segments a1-a2 and b1-b2, returning zero, one or two lattice points. Two
     * points means the segments are collinear and overlap along the run between them.
     *
     * Results are snapped back onto the lattice before they are returned, so a computed
     * crossing can be compared to an existing vertex with exact equality rather than a
     * tolerance. Because both edges are split at the same snapped value, the classic "the same
     * intersection computed twice differs by an ulp" inconsistency cannot arise - it is ruled
     * out by construction rather than absorbed by an epsilon.
     */
    private fun findIntersection(a1: Pt, a2: Pt, b1: Pt, b2: Pt): List<Pt> {
        val vaX = a2.x - a1.x
        val vaY = a2.y - a1.y
        val vbX = b2.x - b1.x
        val vbY = b2.y - b1.y
        val eX = b1.x - a1.x
        val eY = b1.y - a1.y

        fun cross(x1: Double, y1: Double, x2: Double, y2: Double) = x1 * y2 - y1 * x2

        val kross = cross(vaX, vaY, vbX, vbY)
        if (kross != 0.0) {
            val s = cross(eX, eY, vbX, vbY) / kross
            if (s < 0.0 || s > 1.0) return emptyList()
            val t = cross(eX, eY, vaX, vaY) / kross
            if (t < 0.0 || t > 1.0) return emptyList()
            return listOf(snapPoint(a1.x + s * vaX, a1.y + s * vaY))
        }

        // Parallel. They only overlap if they are the same line.
        if (cross(eX, eY, vaX, vaY) != 0.0) return emptyList()

        val sqrLenA = vaX * vaX + vaY * vaY
        if (sqrLenA == 0.0) return emptyList()

        val sa = (eX * vaX + eY * vaY) / sqrLenA
        val sb = sa + (vbX * vaX + vbY * vaY) / sqrLenA
        val smin = min(sa, sb)
        val smax = max(sa, sb)
        if (smin > 1.0 || smax < 0.0) return emptyList()

        val lo = if (smin > 0.0) smin else 0.0
        val hi = if (smax < 1.0) smax else 1.0
        val p0 = snapPoint(a1.x + lo * vaX, a1.y + lo * vaY)
        val p1 = snapPoint(a1.x + hi * vaX, a1.y + hi * vaY)
        // Snapping can collapse a hair-thin overlap to a single lattice point.
        return if (p0.same(p1)) listOf(p0) else listOf(p0, p1)
    }

    private fun snapPoint(x: Double, y: Double) = Pt(round(x), round(y))

    // ------------------------------------------------- turning the arrangement into rings

    /**
     * Chain the boundary edges into closed rings.
     *
     * The boundary edges form a planar graph whose faces are the rings we want. Where only two
     * edges meet the continuation is forced, but result rings routinely pinch - a lobe of the
     * union touches itself at a single vertex, or a hairline hole meets the ring around it -
     * and four edges share that point. Pairing them wrongly there merges an outer ring with a
     * hole, or splits one solid ring into a ring plus a phantom hole. So edges are paired by
     * angular order: arriving along an edge we leave by the first edge clockwise from the way
     * we came, which is the standard face-traversal rule for a planar subdivision and cannot
     * produce a crossing. It works because [boundaryEdges] has directed every edge with the
     * result's interior on its left.
     */
    private fun connectEdges(
        arrangement: List<SweepEvent>,
        subject: List<List<Pt>>,
        clip: List<List<Pt>>,
        op: BooleanOpType,
        budget: Int,
    ): List<List<Pt>>? {
        val edges = boundaryEdges(arrangement, subject, clip, op)
        if (edges.isEmpty()) return emptyList()

        val outgoing = HashMap<Long, MutableList<DirectedEdge>>()
        for (edge in edges) {
            outgoing.getOrPut(latticeKey(edge.from)) { ArrayList() }.add(edge)
        }

        val rings = ArrayList<List<Pt>>()
        for (first in edges) {
            if (first.used) continue

            val ring = ArrayList<Pt>()
            var edge: DirectedEdge? = first
            var end = first.from
            var steps = 0

            while (edge != null && !edge.used) {
                if (steps++ > budget) return null
                edge.used = true
                ring.add(edge.from)
                end = edge.to
                edge = nextEdge(outgoing[latticeKey(edge.to)], edge)
            }

            // An open chain means the surviving edges weren't a closed boundary, which happens
            // when two edges pass within a fraction of a lattice unit of each other and the
            // probe in boundaryEdges cannot resolve which side is which. Abandon the operation
            // rather than return something that isn't a ring: the caller then reports that
            // these two polygons didn't merge, which is the same answer JTS gives for
            // polygons that don't combine and is the safe direction to be wrong in.
            if (ring.isEmpty() || !end.same(ring[0])) return null
            if (ring.size >= 3) rings.add(ring + ring[0])
        }
        return rings
    }

    /**
     * Pick out the arrangement edges that lie on the result's boundary, each directed so the
     * result's interior is on its left.
     *
     * This is where the sweep's work is cashed in. Because every edge has been split at every
     * crossing, each piece is wholly inside or wholly outside each input polygon, so a piece is
     * on the result's boundary exactly when its two sides disagree about being in the result.
     * We step a little off each side of the midpoint and evaluate the operation there against
     * the original rings. An edge answering the same on both sides is not a boundary and is
     * dropped, which sweeps up hairline stray edges for free.
     */
    private fun boundaryEdges(
        arrangement: List<SweepEvent>,
        subject: List<List<Pt>>,
        clip: List<List<Pt>>,
        op: BooleanOpType,
    ): List<DirectedEdge> {
        val edges = ArrayList<DirectedEdge>()
        val seen = HashSet<Pair<Long, Long>>()

        for (event in arrangement) {
            if (!event.left) continue

            val from = event.point
            val to = event.other.point
            val dx = to.x - from.x
            val dy = to.y - from.y
            val length = sqrt(dx * dx + dy * dy)
            if (length == 0.0) continue

            // Where the two polygons share a boundary run the arrangement holds that edge
            // twice, once from each. Coincident edges would give the traversal two ways to
            // leave the same vertex along the same line, so keep only the first.
            if (!seen.add(undirectedKey(from, to))) continue

            val leftInside = interiorAt(probe(from, to, length, PROBE), subject, clip, op)
            val rightInside = interiorAt(probe(from, to, length, -PROBE), subject, clip, op)
            if (leftInside == rightInside) continue

            edges.add(if (leftInside) DirectedEdge(from, to) else DirectedEdge(to, from))
        }
        return edges
    }

    /**
     * How far off an edge to look when deciding which side of it is inside the result, in
     * lattice units.
     *
     * Under a lattice unit, so a probe can never step over a neighbouring vertex, but not much
     * under: snapping leaves vertices sitting a fraction of a unit away from edges they are not
     * on, and a probe closer than that lands in the sub-lattice noise around them and reports
     * the wrong side. Measured against JTS over two thousand random polygon pairs, three
     * quarters of a unit is the sweet spot - it agrees with JTS on every pair it resolves, and
     * leaves fewer than one pair in four hundred unresolved (see PolygonClipperParityTest).
     */
    private const val PROBE = 0.75

    /** A point [distance] lattice units to the left of the edge's midpoint. */
    private fun probe(from: Pt, to: Pt, length: Double, distance: Double): Pt {
        val nx = -(to.y - from.y) / length * distance
        val ny = (to.x - from.x) / length * distance
        return Pt((from.x + to.x) / 2.0 + nx, (from.y + to.y) / 2.0 + ny)
    }

    /** Is [point] inside the result of [op], evaluated against the original input rings? */
    private fun interiorAt(
        point: Pt,
        subject: List<List<Pt>>,
        clip: List<List<Pt>>,
        op: BooleanOpType,
    ): Boolean {
        val inSubject = insideRings(subject, point)
        val inClip = insideRings(clip, point)
        return when (op) {
            BooleanOpType.UNION -> inSubject || inClip
            BooleanOpType.INTERSECTION -> inSubject && inClip
            BooleanOpType.DIFFERENCE -> inSubject && !inClip
            BooleanOpType.XOR -> inSubject != inClip
        }
    }

    /** Even-odd containment across a polygon's rings, so that holes subtract as they should. */
    private fun insideRings(rings: List<List<Pt>>, point: Pt): Boolean {
        var inside = false
        for (ring in rings) {
            for (i in 0 until ring.size - 1) {
                val a = ring[i]
                val b = ring[i + 1]
                if ((a.y > point.y) != (b.y > point.y)) {
                    val t = (point.y - a.y) / (b.y - a.y)
                    if (point.x < a.x + t * (b.x - a.x)) inside = !inside
                }
            }
        }
        return inside
    }

    /**
     * Leaving a vertex, the first unused edge clockwise from the direction we arrived from.
     *
     * Going back the way we came is the largest clockwise turn there is, so it is only ever
     * chosen when nothing else remains, which is the right behaviour at a dead end.
     */
    private fun nextEdge(candidates: List<DirectedEdge>?, incoming: DirectedEdge): DirectedEdge? {
        if (candidates == null) return null
        val back = atan2(incoming.from.y - incoming.to.y, incoming.from.x - incoming.to.x)
        var best: DirectedEdge? = null
        var bestTurn = Double.MAX_VALUE
        for (candidate in candidates) {
            if (candidate.used) continue
            var turn = back - candidate.angle
            while (turn <= 0.0) turn += 2.0 * PI
            while (turn > 2.0 * PI) turn -= 2.0 * PI
            if (turn < bestTurn) {
                bestTurn = turn
                best = candidate
            }
        }
        return best
    }

    private fun undirectedKey(a: Pt, b: Pt): Pair<Long, Long> {
        val first = latticeKey(a)
        val second = latticeKey(b)
        return if (first < second) first to second else second to first
    }

    private fun latticeKey(point: Pt): Long =
        (point.x.toLong() shl 32) or (point.y.toLong() and 0xFFFFFFFFL)

    /** One boundary edge, directed so the result's interior lies to its left. */
    private class DirectedEdge(val from: Pt, val to: Pt) {
        var used = false
        val angle: Double = atan2(to.y - from.y, to.x - from.x)
    }
}
