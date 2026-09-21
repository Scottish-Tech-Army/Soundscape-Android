package org.scottishtecharmy.soundscape.clipper

import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Boolean operations on GeoJSON-shaped polygons, in pure Kotlin so that Android and iOS share
 * one implementation.
 *
 * This exists to merge POI and building polygons that the MVT encoder split at a z14 tile
 * boundary, so the geometry it sees is a specific and slightly unusual shape: two clippings of
 * the *same* OSM way. The tile maths that produces their coordinates is exact - offsets are
 * dyadic rationals over a 4096 extent, divided by a power-of-two zoom factor - so the shared
 * stretch of boundary comes back **bit-identical** from both tiles, and long runs of exactly
 * coincident, identically directed edges are the norm rather than an edge case. That is why
 * this is Martinez-Rueda and not Greiner-Hormann: overlapping collinear edges are handled
 * natively here and are exactly where naive clippers fail.
 *
 * What genuinely differs between the two clippings is where each tile's buffer cut the polygon
 * and which vertices each tile's simplification kept, so near-parallel hairline slivers turn
 * up too. Those are dealt with by snapping every coordinate onto a fixed lattice up front -
 * see [LATTICE_SCALE] - rather than by threading a tolerance through the comparators, which
 * would make them non-transitive and corrupt the sweep.
 */
object PolygonClipper {

    /**
     * Boolean union of two polygons.
     *
     * Each argument is a list of closed rings in the GeoJSON layout: ring 0 is the exterior
     * ring and rings 1..n are its holes. Input winding order is not significant - the sweep
     * derives containment itself - which matters because MvtToGeoJson classifies rings by
     * their tile-space winding and never normalises them to the GeoJSON right-hand rule.
     *
     * Returns one entry per result polygon, in the same ring layout, with exterior rings wound
     * counter-clockwise and holes clockwise per RFC 7946, and every ring explicitly closed.
     * A result of size greater than one means the inputs did not merge into a single connected
     * area; a result of size zero means they were empty, degenerate, or the sweep gave up.
     *
     * Never throws and never loops unboundedly - this runs on the tile-loading thread, where
     * a hang would be far worse than a missed merge.
     */
    fun union(
        subject: List<List<LngLatAlt>>,
        clip: List<List<LngLatAlt>>,
    ): List<List<List<LngLatAlt>>> = booleanOp(subject, clip, BooleanOpType.UNION)

    internal fun booleanOp(
        subject: List<List<LngLatAlt>>,
        clip: List<List<LngLatAlt>>,
        op: BooleanOpType,
    ): List<List<List<LngLatAlt>>> {
        // Everything downstream - the sweep's edge list and the containment tests that orient
        // result edges - assumes closed rings. GeoJSON rings are closed, but tolerate input
        // that isn't rather than silently losing the last edge.
        val subjectRings = subject.filter { it.size >= 3 }.map { closeRing(it) }
        val clipRings = clip.filter { it.size >= 3 }.map { closeRing(it) }
        if (subjectRings.isEmpty() && clipRings.isEmpty()) return emptyList()

        val frame = LatticeFrame.of(subjectRings, clipRings) ?: return emptyList()

        val latticeSubject = subjectRings.map { frame.toLattice(it) }
        val latticeClip = clipRings.map { frame.toLattice(it) }

        // Reconcile tile-edge clip vertices before the sweep, so the two polygons agree about
        // where their shared boundary runs - see [insertNearbyVertices].
        val snappedSubject = insertNearbyVertices(latticeSubject, latticeClip)
        val snappedClip = insertNearbyVertices(latticeClip, latticeSubject)

        val rings = MartinezClipper.run(snappedSubject, snappedClip, op) ?: return emptyList()
        return assemble(rings, frame)
    }

    /**
     * Group flat result rings into polygons, working out which ring is a hole of which
     * geometrically rather than from the sweep's own bookkeeping.
     *
     * A ring's nesting depth is simply the number of other rings that contain it. Even depths
     * are exterior rings; an odd-depth ring is a hole of the smallest containing ring one
     * level up. Depth two is an island sitting inside a hole, and correctly starts a polygon
     * of its own - which is the other reason [union] returns a list.
     */
    private fun assemble(rings: List<List<Pt>>, frame: LatticeFrame): List<List<List<LngLatAlt>>> {
        val closed = rings.flatMap { splitSelfTouching(it) }.mapNotNull { close(it) }
        if (closed.isEmpty()) return emptyList()

        val areas = closed.map { ringSignedArea(it) }
        // Zero-area rings are the sliver artefacts of near-parallel input edges. They are not
        // valid GeoJSON and carry no information, so drop them here rather than downstream.
        val kept = closed.indices.filter { areas[it] != 0.0 }.map { closed[it] }
        if (kept.isEmpty()) return emptyList()
        val keptAreas = kept.map { ringSignedArea(it) }

        // Which ring is inside which, decided pairwise. Result rings never cross, so a single
        // point of one that is off the other's boundary settles it.
        val inside = Array(kept.size) { BooleanArray(kept.size) }
        val depths = IntArray(kept.size)
        for (i in kept.indices) {
            for (j in kept.indices) {
                if (i != j && ringInsideRing(kept[i], kept[j])) {
                    inside[i][j] = true
                    depths[i]++
                }
            }
        }

        val polygons = ArrayList<ArrayList<List<LngLatAlt>>>()
        val polygonIndexForRing = HashMap<Int, Int>()

        // Exterior rings first, so every hole has a parent to attach to. An even nesting depth
        // means solid: depth 0 is an ordinary outer ring, depth 2 an island inside a hole,
        // which correctly becomes a result polygon of its own.
        for (i in kept.indices) {
            if (depths[i] % 2 != 0) continue
            polygonIndexForRing[i] = polygons.size
            polygons.add(arrayListOf(frame.toLngLat(kept[i], counterClockwise = true)))
        }

        // Then holes, each attached to the smallest ring one level out that contains it.
        for (i in kept.indices) {
            if (depths[i] % 2 == 0) continue
            var parent = -1
            var parentArea = Double.MAX_VALUE
            for (j in kept.indices) {
                if (i == j || depths[j] != depths[i] - 1 || !inside[i][j]) continue
                val area = abs(keptAreas[j])
                if (area < parentArea) {
                    parentArea = area
                    parent = j
                }
            }
            val polygonIndex = polygonIndexForRing[parent] ?: continue
            polygons[polygonIndex].add(frame.toLngLat(kept[i], counterClockwise = false))
        }

        return polygons
    }

    private fun closeRing(ring: List<LngLatAlt>): List<LngLatAlt> =
        if (ring.first() == ring.last()) ring else ring + ring.first()

    /**
     * Split a ring that revisits one of its own vertices into the separate simple rings it is
     * made of.
     *
     * The traversal can leave a ring that runs out to a point and straight back - a zero-width
     * spike, thrown up by two nearly parallel edges - or one that pinches at a vertex. Either
     * way the ring touches itself, which is not a valid GeoJSON polygon and confuses the
     * containment tests downstream. Cutting it at the repeated vertex recovers the pieces, and
     * because nesting is decided geometrically afterwards they get classified correctly; a
     * spike encloses no area and is dropped.
     */
    private fun splitSelfTouching(ring: List<Pt>): List<List<Pt>> {
        val result = ArrayList<List<Pt>>()
        val path = ArrayList<Pt>()
        val seenAt = HashMap<Long, Int>()

        for (point in ring) {
            if (path.isNotEmpty() && path[path.size - 1].same(point)) continue

            val key = latticeKey(point)
            val previous = seenAt[key]
            if (previous == null) {
                seenAt[key] = path.size
                path.add(point)
                continue
            }

            val loop = ArrayList<Pt>(path.size - previous + 1)
            for (i in previous until path.size) loop.add(path[i])
            loop.add(point)
            if (loop.size >= 4) result.add(loop)

            for (i in path.size - 1 downTo previous + 1) {
                seenAt.remove(latticeKey(path[i]))
                path.removeAt(i)
            }
        }

        if (path.size >= 3) {
            val remainder = ArrayList<Pt>(path.size + 1)
            remainder.addAll(path)
            remainder.add(path[0])
            result.add(remainder)
        }
        return result
    }

    private fun latticeKey(point: Pt): Long =
        (point.x.toLong() shl 32) or (point.y.toLong() and 0xFFFFFFFFL)

    /**
     * How far a vertex may be from another polygon's edge and still be taken to lie on it, in
     * lattice units.
     *
     * Every vertex of a real OSM way arrives on a grid the two tiles share - tile coordinates
     * are exact dyadic rationals - so the same vertex comes back bit-identical from both tiles
     * and needs no help at all. The exceptions are the vertices the MVT encoder created when it
     * clipped a way to a tile's edge: those sit at the true crossing rounded to that tile's
     * sample grid, which leaves them slightly off the line they were cut from. The DHL
     * warehouse in the Bristol test grid is off by 1.6 units, about 7mm.
     *
     * Five units, roughly 2-3cm, covers that with room to spare while staying an order of
     * magnitude below the spacing of the z14 sample grid itself - 90 units of longitude and
     * about 56 of latitude - so it can never pull two genuinely distinct grid vertices
     * together. Measured against JTS, 2 and 5 both merge every tile-split pair in the Bristol
     * grid with no loss of agreement anywhere; at 10 the synthetic parity cases start to drift,
     * which is the geometry being bent further than the tile encoder ever bent it.
     */
    private const val SNAP_TOLERANCE = 5.0

    /**
     * Bend [target]'s edges to pass exactly through any [other] vertex that already lies within
     * [SNAP_TOLERANCE] of them.
     *
     * This is what makes two clippings of one OSM way agree. Where a tile boundary cut the way,
     * one tile holds a clip vertex that should sit on the edge the other tile kept whole, but
     * rounding to that tile's sample grid left it a little to one side. The sweep then sees two
     * edges that nearly coincide instead of one shared boundary, and the hairline gap between
     * them is too narrow for the clipper to tell which side is inside - so it gives up and
     * reports that the two polygons did not merge.
     *
     * Inserting the vertex into the edge closes the gap exactly: the two boundaries become the
     * same chain of points rather than two chains a few millimetres apart. It moves the edge by
     * no more than the tolerance, which is smaller than the error the tile encoder introduced
     * when it rounded that vertex in the first place.
     */
    private fun insertNearbyVertices(
        target: List<List<Pt>>,
        other: List<List<Pt>>,
    ): List<List<Pt>> {
        val candidates = ArrayList<Pt>()
        for (ring in other) {
            for (i in 0 until ring.size - 1) candidates.add(ring[i])
        }
        if (candidates.isEmpty()) return target

        return target.map { ring ->
            val result = ArrayList<Pt>(ring.size)
            for (i in 0 until ring.size - 1) {
                val a = ring[i]
                val b = ring[i + 1]
                result.add(a)

                // Everything that belongs on this edge, ordered along it.
                var insertions: MutableList<Pair<Double, Pt>>? = null
                for (candidate in candidates) {
                    if (candidate.same(a) || candidate.same(b)) continue
                    val t = projectionOnSegment(candidate, a, b) ?: continue
                    if (distanceToSegment(candidate, a, b) > SNAP_TOLERANCE) continue
                    if (insertions == null) insertions = ArrayList()
                    insertions.add(t to candidate)
                }
                if (insertions != null) {
                    insertions.sortBy { it.first }
                    var previous: Pt? = null
                    for ((_, point) in insertions) {
                        if (previous?.same(point) == true) continue
                        result.add(point)
                        previous = point
                    }
                }
            }
            result.add(ring[ring.size - 1])
            result
        }
    }

    /**
     * Where [p] falls along the segment a-b as a fraction, or null when it projects onto an
     * endpoint or beyond. Only interior projections matter: a vertex near an endpoint is
     * already shared, or belongs to the neighbouring edge.
     */
    private fun projectionOnSegment(p: Pt, a: Pt, b: Pt): Double? {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val lengthSquared = dx * dx + dy * dy
        if (lengthSquared == 0.0) return null
        val t = ((p.x - a.x) * dx + (p.y - a.y) * dy) / lengthSquared
        return if (t <= 0.0 || t >= 1.0) null else t
    }

    private fun distanceToSegment(p: Pt, a: Pt, b: Pt): Double {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val lengthSquared = dx * dx + dy * dy
        if (lengthSquared == 0.0) {
            return sqrt((p.x - a.x) * (p.x - a.x) + (p.y - a.y) * (p.y - a.y))
        }
        var t = ((p.x - a.x) * dx + (p.y - a.y) * dy) / lengthSquared
        if (t < 0.0) t = 0.0
        if (t > 1.0) t = 1.0
        val cx = p.x - (a.x + t * dx)
        val cy = p.y - (a.y + t * dy)
        return sqrt(cx * cx + cy * cy)
    }

    /** Close a ring, dropping any repeated vertices, or null if too little is left of it. */
    private fun close(ring: List<Pt>): List<Pt>? {
        val points = ArrayList<Pt>(ring.size + 1)
        for (point in ring) {
            if (points.isEmpty() || !points[points.size - 1].same(point)) points.add(point)
        }
        while (points.size > 1 && points[0].same(points[points.size - 1])) {
            points.removeAt(points.size - 1)
        }
        if (points.size < 3) return null
        points.add(points[0])
        return points
    }

    /**
     * Twice the signed area of a closed lattice ring, positive when counter-clockwise.
     *
     * Each term subtracts the ring's first vertex before multiplying, so both factors stay
     * below 2^26 and each product is exact; the running sum stays far inside the exactly
     * representable integers for any polygon this code will ever see.
     */
    private fun ringSignedArea(ring: List<Pt>): Double {
        val origin = ring[0]
        var area = 0.0
        for (i in 0 until ring.size - 1) {
            val a = ring[i]
            val b = ring[i + 1]
            area += (a.x - origin.x) * (b.y - origin.y) - (b.x - origin.x) * (a.y - origin.y)
        }
        return area
    }

    /**
     * Is [inner] contained in [outer]?
     *
     * Decided by finding a point strictly inside [inner] that is not on [outer]'s boundary and
     * asking which side of [outer] it falls. Both halves of that matter: result rings touch one
     * another constantly - a hole's corner frequently sits exactly on the ring around it - and
     * a representative point that lands on the other ring's boundary would otherwise be
     * classified by whichever way the ray cast happened to break the tie, which silently
     * inverts a hole into an outer ring.
     */
    private fun ringInsideRing(inner: List<Pt>, outer: List<Pt>): Boolean {
        for (probe in containmentProbes(inner)) {
            if (pointOnRing(outer, probe)) continue
            return ringContains(outer, probe)
        }
        return false
    }

    /**
     * Points to test [ring] against another ring with, best first.
     *
     * Result rings never cross, so any point of one that is not on the other settles which
     * side it is on - which is why edge midpoints and bare vertices are usable probes and not
     * just points strictly inside. The interior centroids come first because they are furthest
     * from anything else, but a hairline sliver ring - the shape a pair of near-parallel input
     * edges leaves behind - can have no interior point the lattice can represent, and it is
     * exactly those rings that most need classifying correctly.
     */
    private fun containmentProbes(ring: List<Pt>): Sequence<Pt> = sequence {
        val n = ring.size - 1
        if (n < 3) return@sequence

        var start = 0
        for (i in 1 until n) {
            val candidate = ring[i]
            val best = ring[start]
            if (candidate.y < best.y || (candidate.y == best.y && candidate.x < best.x)) {
                start = i
            }
        }

        // The centroid of a vertex and its two neighbours, which lies strictly inside whenever
        // that vertex is convex. The lowest-then-leftmost vertex always is, so it comes first.
        for (offset in 0 until n) {
            val i = (start + offset) % n
            val prev = ring[(i + n - 1) % n]
            val here = ring[i]
            val next = ring[(i + 1) % n]
            val point = Pt(
                (prev.x + here.x + next.x) / 3.0,
                (prev.y + here.y + next.y) / 3.0,
            )
            if (ringContains(ring, point)) yield(point)
        }

        for (i in 0 until n) {
            val a = ring[i]
            val b = ring[i + 1]
            yield(Pt((a.x + b.x) / 2.0, (a.y + b.y) / 2.0))
        }

        for (i in 0 until n) yield(ring[i])
    }

    /**
     * Does [point] lie on [ring]'s boundary?
     *
     * The tolerance is in lattice units, where every real vertex separation is a whole number,
     * so it can be far below anything genuine while still absorbing the thirds introduced by
     * [interiorCandidates]. It only ever affects how rings are nested, never the sweep, so it
     * cannot make a comparator inconsistent.
     */
    private fun pointOnRing(ring: List<Pt>, point: Pt): Boolean {
        val tolerance = 1e-6
        for (i in 0 until ring.size - 1) {
            val a = ring[i]
            val b = ring[i + 1]
            if (point.x < min(a.x, b.x) - tolerance || point.x > max(a.x, b.x) + tolerance) continue
            if (point.y < min(a.y, b.y) - tolerance || point.y > max(a.y, b.y) + tolerance) continue
            val cross = (b.x - a.x) * (point.y - a.y) - (b.y - a.y) * (point.x - a.x)
            val length = max(abs(b.x - a.x), abs(b.y - a.y))
            if (abs(cross) <= tolerance * max(1.0, length)) return true
        }
        return false
    }

    /**
     * Crossing-number test for a point inside a closed ring. Only ever called with points known
     * not to be on the ring, so the boundary case does not arise.
     */
    private fun ringContains(ring: List<Pt>, point: Pt): Boolean {
        var inside = false
        for (i in 0 until ring.size - 1) {
            val a = ring[i]
            val b = ring[i + 1]
            if ((a.y > point.y) != (b.y > point.y)) {
                val t = (point.y - a.y) / (b.y - a.y)
                if (point.x < a.x + t * (b.x - a.x)) inside = !inside
            }
        }
        return inside
    }
}

/**
 * The mapping between lon/lat degrees and the clipper's working lattice.
 *
 * Two separate steps, both exact. First every coordinate is snapped onto a **global** lattice
 * - `round(degrees * scale)`, with no dependence on the inputs - which is what keeps the
 * bit-identity of shared tile vertices intact. That matters because
 * mergeAllPolygonsInFeatureCollection merges in a chain, A with B and then that result with C:
 * a lattice derived from each pair's own bounding box would snap the same input coordinate to
 * two different values on two different steps and throw the coincidence away. Second, lattice
 * coordinates are translated to the origin by subtracting an integer, which is exact and so
 * preserves everything the snap established, while keeping magnitudes small enough for
 * [signedArea] to stay exact.
 */
private class LatticeFrame(
    private val scale: Double,
    private val originX: Double,
    private val originY: Double,
) {
    /**
     * Lattice point back to the exact input coordinate it came from. Without this every output
     * vertex would be a snapped copy of its input, off by up to half a lattice unit - about
     * 3mm. Harmless in itself, but it means an untouched vertex would no longer compare equal
     * to the one that went in.
     */
    private val originalVertices = HashMap<Long, LngLatAlt>()

    fun toLattice(ring: List<LngLatAlt>): List<Pt> = ring.map { position ->
        val x = snap(position.longitude, scale) - originX
        val y = snap(position.latitude, scale) - originY
        originalVertices.getOrPut(key(x, y)) { position }
        Pt(x, y)
    }

    fun toLngLat(ring: List<Pt>, counterClockwise: Boolean): List<LngLatAlt> {
        val ordered = if (isCounterClockwise(ring) == counterClockwise) ring else ring.reversed()
        return ordered.map { point ->
            originalVertices[key(point.x, point.y)] ?: LngLatAlt(
                (point.x + originX) / scale,
                (point.y + originY) / scale,
            )
        }
    }

    private fun isCounterClockwise(ring: List<Pt>): Boolean {
        val origin = ring[0]
        var area = 0.0
        for (i in 0 until ring.size - 1) {
            val a = ring[i]
            val b = ring[i + 1]
            area += (a.x - origin.x) * (b.y - origin.y) - (b.x - origin.x) * (a.y - origin.y)
        }
        return area > 0.0
    }

    private fun key(x: Double, y: Double): Long =
        (x.toLong() shl 32) or (y.toLong() and 0xFFFFFFFFL)

    companion object {
        fun of(subject: List<List<LngLatAlt>>, clip: List<List<LngLatAlt>>): LatticeFrame? {
            var minLon = Double.MAX_VALUE
            var maxLon = -Double.MAX_VALUE
            var minLat = Double.MAX_VALUE
            var maxLat = -Double.MAX_VALUE
            var any = false
            for (rings in listOf(subject, clip)) {
                for (ring in rings) {
                    for (position in ring) {
                        if (position.longitude < minLon) minLon = position.longitude
                        if (position.longitude > maxLon) maxLon = position.longitude
                        if (position.latitude < minLat) minLat = position.latitude
                        if (position.latitude > maxLat) maxLat = position.latitude
                        any = true
                    }
                }
            }
            if (!any) return null

            // Only degrades for inputs several degrees across, which no POI or building is,
            // so in practice the lattice really is global and shared by every merge.
            val scale = chooseScale(maxOf(maxLon - minLon, maxLat - minLat))
            return LatticeFrame(scale, snap(minLon, scale), snap(minLat, scale))
        }
    }
}
