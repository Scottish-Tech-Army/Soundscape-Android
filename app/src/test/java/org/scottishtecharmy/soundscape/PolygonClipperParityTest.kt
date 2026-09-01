package org.scottishtecharmy.soundscape

import org.junit.Test
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.scottishtecharmy.soundscape.clipper.LATTICE_SCALE
import org.scottishtecharmy.soundscape.clipper.PolygonClipper
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.locationtech.jts.geom.Polygon as JtsPolygon

/**
 * Differential test of our pure-Kotlin polygon clipper against JTS, which is what mergePolygons
 * used on Android before the clipper existed. JTS stays in the build as a test-only dependency
 * purely to be this oracle, exactly as rtree2 does for RTreeParityTest.
 *
 * The generators manufacture the degeneracies real data produces rather than hoping random
 * polygons stumble into them: vertices snapped to the z14 MVT quantum so coincident vertices
 * and collinear runs are routine, translated copies to force long exactly shared boundaries,
 * and split-with-overlap rectangles shaped like the tile clippings this code actually merges.
 *
 * Two outcomes count as agreement. Either the clipper resolves the union, in which case it must
 * match JTS on polygon count, ring count, area and interior containment; or it declines to
 * resolve it and returns nothing, which mergePolygons reports as "these didn't merge". The
 * second happens when two edges pass within a fraction of a lattice unit and the clipper cannot
 * tell which side of an edge is inside. Each test bounds how often that is allowed to happen,
 * so a regression that starts declining ordinary polygons fails the build.
 */
class PolygonClipperParityTest {

    /** The z14 MVT quantum in degrees - 360 / (2^14 * 4096), about 0.37m at UK latitudes. */
    private val mvtQuantum = 360.0 / (1 shl 14) / 4096.0

    private val originLon = -2.64
    private val originLat = 51.54
    private val factory = GeometryFactory()

    private fun quantise(value: Double) = round(value / mvtQuantum) * mvtQuantum

    /**
     * A star-shaped polygon: sorted angles with random radii about a centre. Always simple, so
     * it is always valid input for both implementations.
     */
    private fun starPolygon(
        rng: Random,
        centreLon: Double,
        centreLat: Double,
        vertices: Int,
        maxRadiusQuanta: Double,
        radiusJitter: Double = 0.0,
    ): List<LngLatAlt> {
        val angles = List(vertices) { rng.nextDouble(0.0, 2 * PI) }.sorted()
        val ring = angles.map { angle ->
            val radius = rng.nextDouble(maxRadiusQuanta * 0.35, maxRadiusQuanta) * mvtQuantum
            val lon = quantise(centreLon + radius * cos(angle))
            val lat = quantise(centreLat + radius * sin(angle))
            if (radiusJitter == 0.0) {
                LngLatAlt(lon, lat)
            } else {
                // Perturbing along the radius keeps the ring star-shaped, and so simple, which
                // jittering each vertex independently would not.
                val extra = rng.nextDouble(-radiusJitter, radiusJitter)
                LngLatAlt(lon + extra * cos(angle), lat + extra * sin(angle))
            }
        }.toMutableList()
        ring.add(ring[0])
        return ring
    }

    private fun rectangle(lon1: Double, lat1: Double, lon2: Double, lat2: Double) = listOf(
        LngLatAlt(quantise(lon1), quantise(lat1)),
        LngLatAlt(quantise(lon2), quantise(lat1)),
        LngLatAlt(quantise(lon2), quantise(lat2)),
        LngLatAlt(quantise(lon1), quantise(lat2)),
        LngLatAlt(quantise(lon1), quantise(lat1)),
    )

    /**
     * Quantising a star polygon onto the MVT grid can collapse two vertices together and leave
     * a ring that touches itself, which is not something mergePolygons is ever handed and not
     * something JTS would accept either. Skip those the same way the comparisons do.
     */
    private fun isUsableInput(vararg rings: List<List<LngLatAlt>>): Boolean =
        rings.all { toJts(it)?.isValid == true }

    private fun toJts(rings: List<List<LngLatAlt>>): JtsPolygon? {
        if (rings.isEmpty()) return null
        return try {
            factory.createPolygon(
                factory.createLinearRing(
                    rings[0].map { Coordinate(it.longitude, it.latitude) }.toTypedArray()
                ),
                rings.drop(1).map { ring ->
                    factory.createLinearRing(
                        ring.map { Coordinate(it.longitude, it.latitude) }.toTypedArray()
                    )
                }.toTypedArray(),
            )
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private fun shoelace(ring: List<LngLatAlt>): Double {
        var sum = 0.0
        for (i in 0 until ring.size - 1) {
            sum += (ring[i].longitude * ring[i + 1].latitude) -
                (ring[i + 1].longitude * ring[i].latitude)
        }
        return sum / 2.0
    }

    private fun area(result: List<List<List<LngLatAlt>>>): Double {
        var total = 0.0
        for (polygon in result) {
            for ((index, ring) in polygon.withIndex()) {
                val signed = abs(shoelace(ring))
                total += if (index == 0) signed else -signed
            }
        }
        return total
    }

    private fun perimeter(result: List<List<List<LngLatAlt>>>): Double {
        var total = 0.0
        for (polygon in result) {
            for (ring in polygon) {
                for (i in 0 until ring.size - 1) {
                    val dx = ring[i + 1].longitude - ring[i].longitude
                    val dy = ring[i + 1].latitude - ring[i].latitude
                    total += sqrt(dx * dx + dy * dy)
                }
            }
        }
        return total
    }

    /**
     * Snapping onto the clipper's lattice moves each vertex by at most half a lattice unit, so
     * the area can differ from JTS's by at most the perimeter times that displacement. Four
     * times that for margin, with a floor of a few lattice cells so that near-zero-area results
     * don't demand impossible relative precision.
     */
    private fun areaTolerance(result: List<List<List<LngLatAlt>>>): Double =
        max(4.0 * perimeter(result) * (0.5 / LATTICE_SCALE), 16.0 / (LATTICE_SCALE * LATTICE_SCALE))

    private class Tally {
        var compared = 0
        var unresolved = 0
    }

    /**
     * Compare one union against JTS on the properties that matter, in increasing order of
     * strength: whether the inputs merged at all - the only property mergePolygons branches on -
     * then ring counts, then area, then interior containment.
     */
    private fun compare(
        label: String,
        subject: List<List<LngLatAlt>>,
        clip: List<List<LngLatAlt>>,
        rng: Random,
        tally: Tally,
    ) {
        val jtsSubject = toJts(subject) ?: return
        val jtsClip = toJts(clip) ?: return
        // Anything JTS itself calls invalid isn't a fair comparison, and mergePolygons is only
        // ever handed tile geometry.
        if (!jtsSubject.isValid || !jtsClip.isValid) return
        val jtsResult = try {
            jtsSubject.union(jtsClip)
        } catch (e: RuntimeException) {
            return
        }

        val ours = PolygonClipper.union(subject, clip)
        if (ours.isEmpty()) {
            tally.unresolved++
            return
        }
        tally.compared++

        assertEquals(
            jtsResult.numGeometries, ours.size,
            "$label: disagreed on how many polygons the union produces",
        )
        var jtsRings = 0
        for (i in 0 until jtsResult.numGeometries) {
            jtsRings += 1 + (jtsResult.getGeometryN(i) as JtsPolygon).numInteriorRing
        }
        assertEquals(jtsRings, ours.sumOf { it.size }, "$label: disagreed on ring count")
        assertEquals(jtsResult.area, area(ours), areaTolerance(ours), "$label: disagreed on area")

        // The strongest check: do the two results agree about what is inside them? Points
        // within a couple of lattice cells of either boundary are skipped, since that is
        // exactly the band where snapping is allowed to disagree.
        val oursAsJts = if (ours.size == 1) toJts(ours[0]) else null
        if (oursAsJts != null) {
            val envelope = jtsResult.envelopeInternal
            val band = 2.0 / LATTICE_SCALE
            repeat(100) {
                val lon = rng.nextDouble(envelope.minX, envelope.maxX)
                val lat = rng.nextDouble(envelope.minY, envelope.maxY)
                val point = factory.createPoint(Coordinate(lon, lat))
                if (point.distance(jtsResult.boundary) < band) return@repeat
                if (point.distance(oursAsJts.boundary) < band) return@repeat
                assertEquals(
                    jtsResult.contains(point), oursAsJts.contains(point),
                    "$label: disagreed on whether ($lon, $lat) is inside",
                )
            }
        }
    }

    private fun assertResolutionRate(tally: Tally, allowed: Double, label: String) {
        val total = tally.compared + tally.unresolved
        assertTrue(total > 0, "$label: nothing was compared")
        val rate = tally.unresolved.toDouble() / total
        assertTrue(
            rate <= allowed,
            "$label: declined to resolve ${tally.unresolved} of $total unions " +
                "(${(rate * 100).toInt()}%), above the ${(allowed * 100).toInt()}% budget",
        )
    }

    @Test
    fun matchesJtsForOverlappingStarPolygons() {
        val rng = Random(42)
        val tally = Tally()
        repeat(500) { case ->
            val a = starPolygon(rng, originLon, originLat, rng.nextInt(5, 14), 40.0)
            val b = starPolygon(
                rng,
                originLon + rng.nextInt(-30, 30) * mvtQuantum,
                originLat + rng.nextInt(-30, 30) * mvtQuantum,
                rng.nextInt(5, 14),
                40.0,
            )
            compare("star pair $case", listOf(a), listOf(b), rng, tally)
        }
        assertResolutionRate(tally, 0.02, "star pairs")
    }

    @Test
    fun matchesJtsForTranslatedCopies() {
        // A polygon and a copy of itself shifted by a whole number of MVT quanta, so the two
        // share long runs of bit-identical vertices - the degeneracy tile splits produce.
        val rng = Random(7)
        val tally = Tally()
        repeat(500) { case ->
            val a = starPolygon(rng, originLon, originLat, rng.nextInt(6, 16), 40.0)
            val shiftLon = rng.nextInt(-20, 20) * mvtQuantum
            val shiftLat = rng.nextInt(-20, 20) * mvtQuantum
            val b = a.map { LngLatAlt(it.longitude + shiftLon, it.latitude + shiftLat) }
            compare("translated copy $case", listOf(a), listOf(b), rng, tally)
        }
        assertResolutionRate(tally, 0.02, "translated copies")
    }

    @Test
    fun matchesJtsForTileBoundarySplits() {
        // The real shape of the problem: one rectangle cut by a vertical line, with each half
        // extended past the cut by a buffer, exactly as adjacent MVT tiles clip a polygon.
        val rng = Random(1234)
        val tally = Tally()
        repeat(500) { case ->
            val width = rng.nextInt(20, 80) * mvtQuantum
            val height = rng.nextInt(20, 80) * mvtQuantum
            val cut = rng.nextInt(5, 15) * mvtQuantum
            val buffer = rng.nextInt(0, 5) * mvtQuantum

            val left = rectangle(originLon, originLat, originLon + cut + buffer, originLat + height)
            val right = rectangle(originLon + cut - buffer, originLat, originLon + width, originLat + height)
            compare("tile split $case", listOf(left), listOf(right), rng, tally)
        }
        // This is the shape production actually merges, so it gets no slack at all.
        assertResolutionRate(tally, 0.0, "tile splits")
    }

    @Test
    fun matchesJtsForPolygonsWithHoles() {
        val rng = Random(99)
        val tally = Tally()
        repeat(300) { case ->
            val outer = rectangle(
                originLon, originLat,
                originLon + 60 * mvtQuantum, originLat + 60 * mvtQuantum,
            )
            // Wound the other way, as GeoJSON holes are.
            val hole = rectangle(
                originLon + 20 * mvtQuantum, originLat + 20 * mvtQuantum,
                originLon + 40 * mvtQuantum, originLat + 40 * mvtQuantum,
            ).reversed()
            val offset = rng.nextInt(-10, 70) * mvtQuantum
            val other = rectangle(
                originLon + offset, originLat + 10 * mvtQuantum,
                originLon + offset + 25 * mvtQuantum, originLat + 50 * mvtQuantum,
            )
            compare("donut $case", listOf(outer, hole), listOf(other), rng, tally)
        }
        assertResolutionRate(tally, 0.02, "donuts")
    }

    @Test
    fun matchesJtsForSubQuantumSlivers() {
        // Per-tile simplification can leave two clippings disagreeing by far less than one MVT
        // quantum, which is what snapping onto the lattice exists to absorb.
        val rng = Random(5)
        val tally = Tally()
        repeat(300) { case ->
            val a = starPolygon(rng, originLon, originLat, rng.nextInt(6, 12), 40.0)
            val b = starPolygon(rng, originLon, originLat, rng.nextInt(6, 12), 40.0, mvtQuantum / 50.0)
            compare("sliver $case", listOf(a), listOf(b), rng, tally)
        }
        assertResolutionRate(tally, 0.05, "sub-quantum slivers")
    }

    @Test
    fun unionIsNeverSmallerThanEitherInput() {
        // A cheap invariant that holds whatever JTS thinks, and which catches the failure mode
        // that matters most in production: quietly losing one polygon's geometry, which is
        // exactly what the old iOS stub did.
        val rng = Random(2024)
        repeat(500) {
            val a = starPolygon(rng, originLon, originLat, rng.nextInt(5, 12), 40.0)
            val b = starPolygon(
                rng,
                originLon + rng.nextInt(-25, 25) * mvtQuantum,
                originLat + rng.nextInt(-25, 25) * mvtQuantum,
                rng.nextInt(5, 12),
                40.0,
            )
            if (!isUsableInput(listOf(a), listOf(b))) return@repeat
            val result = PolygonClipper.union(listOf(a), listOf(b))
            if (result.isEmpty()) return@repeat

            val merged = area(result)
            val areaA = abs(shoelace(a))
            val areaB = abs(shoelace(b))
            val slack = areaTolerance(result)
            assertTrue(merged >= max(areaA, areaB) - slack, "union is smaller than an input")
            assertTrue(merged <= areaA + areaB + slack, "union exceeds the sum of the inputs")
        }
    }

    @Test
    fun resultsAreAlwaysValidPolygons() {
        val rng = Random(31337)
        repeat(500) {
            val a = starPolygon(rng, originLon, originLat, rng.nextInt(5, 14), 40.0)
            val b = starPolygon(
                rng,
                originLon + rng.nextInt(-30, 30) * mvtQuantum,
                originLat + rng.nextInt(-30, 30) * mvtQuantum,
                rng.nextInt(5, 14),
                40.0,
            )
            if (!isUsableInput(listOf(a), listOf(b))) return@repeat
            for (polygon in PolygonClipper.union(listOf(a), listOf(b))) {
                for ((index, ring) in polygon.withIndex()) {
                    assertTrue(ring.size >= 4, "ring with only ${ring.size} points")
                    assertEquals(ring.first(), ring.last(), "ring is not closed")
                    val signed = shoelace(ring)
                    if (index == 0) {
                        assertTrue(signed > 0.0, "exterior ring is not counter-clockwise")
                    } else {
                        assertTrue(signed < 0.0, "hole is not clockwise")
                    }
                }
                val asJts = toJts(polygon)
                assertTrue(asJts != null && asJts.isValid, "result is not a valid polygon")
            }
        }
    }
}
