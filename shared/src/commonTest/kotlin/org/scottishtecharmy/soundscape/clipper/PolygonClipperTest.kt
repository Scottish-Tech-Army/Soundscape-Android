package org.scottishtecharmy.soundscape.clipper

import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Known-answer tests for the clipper itself. The differential tests against JTS live in the
 * app module (PolygonClipperParityTest); these pin down the cases where we know the answer by
 * inspection, and in particular the degeneracies that tile-split polygons actually produce.
 */
class PolygonClipperTest {

    private fun ring(vararg coordinates: Pair<Double, Double>): List<LngLatAlt> =
        coordinates.map { LngLatAlt(it.first, it.second) }

    private fun box(x1: Double, y1: Double, x2: Double, y2: Double): List<List<LngLatAlt>> =
        listOf(ring(x1 to y1, x2 to y1, x2 to y2, x1 to y2, x1 to y1))

    /** Planar shoelace area of a result polygon, exterior minus holes. */
    private fun area(polygon: List<List<LngLatAlt>>): Double {
        var total = 0.0
        for ((index, r) in polygon.withIndex()) {
            val signed = abs(shoelace(r))
            total += if (index == 0) signed else -signed
        }
        return total
    }

    private fun shoelace(r: List<LngLatAlt>): Double {
        var sum = 0.0
        for (i in 0 until r.size - 1) {
            sum += (r[i].longitude * r[i + 1].latitude) - (r[i + 1].longitude * r[i].latitude)
        }
        return sum / 2.0
    }

    /** Every result must be structurally valid GeoJSON, whatever the case under test. */
    private fun assertWellFormed(result: List<List<List<LngLatAlt>>>) {
        for (polygon in result) {
            assertTrue(polygon.isNotEmpty(), "polygon with no rings")
            for ((index, r) in polygon.withIndex()) {
                assertTrue(r.size >= 4, "ring with only ${r.size} points")
                assertEquals(r.first(), r.last(), "ring is not closed")
                val signed = shoelace(r)
                if (index == 0) {
                    assertTrue(signed > 0.0, "exterior ring should be counter-clockwise")
                } else {
                    assertTrue(signed < 0.0, "hole should be clockwise")
                }
            }
        }
    }

    @Test
    fun squaresSharingAnEdgeFuseIntoOne() {
        // The tile-boundary case at its cleanest: zero overlapping area, one shared edge.
        // polygonFeaturesOverlap counts boundary points as overlapping, so pairs like this
        // really are handed to mergePolygons.
        val result = PolygonClipper.union(box(0.0, 0.0, 1.0, 1.0), box(1.0, 0.0, 2.0, 1.0))
        assertWellFormed(result)
        assertEquals(1, result.size)
        assertEquals(1, result[0].size)
        assertEquals(2.0, area(result[0]), 1e-9)
    }

    @Test
    fun overlappingSquaresFuseIntoOne() {
        val result = PolygonClipper.union(box(0.0, 0.0, 2.0, 1.0), box(1.0, 0.0, 3.0, 1.0))
        assertWellFormed(result)
        assertEquals(1, result.size)
        assertEquals(1, result[0].size)
        assertEquals(3.0, area(result[0]), 1e-9)
    }

    @Test
    fun identicalPolygonsAreIdempotent() {
        val result = PolygonClipper.union(box(0.0, 0.0, 1.0, 1.0), box(0.0, 0.0, 1.0, 1.0))
        assertWellFormed(result)
        assertEquals(1, result.size)
        assertEquals(1, result[0].size)
        assertEquals(1.0, area(result[0]), 1e-9)
    }

    @Test
    fun containedPolygonDisappearsIntoItsContainer() {
        val result = PolygonClipper.union(box(0.0, 0.0, 4.0, 4.0), box(1.0, 1.0, 2.0, 2.0))
        assertWellFormed(result)
        assertEquals(1, result.size)
        assertEquals(1, result[0].size)
        assertEquals(16.0, area(result[0]), 1e-9)
    }

    @Test
    fun disjointPolygonsStaySeparate() {
        val result = PolygonClipper.union(box(0.0, 0.0, 1.0, 1.0), box(5.0, 5.0, 6.0, 6.0))
        assertWellFormed(result)
        assertEquals(2, result.size)
    }

    @Test
    fun unionCanCreateAHole() {
        // A rectangle plus a reversed C that wraps around it: the enclosed gap becomes a hole.
        val rectangle = box(0.0, 0.0, 1.0, 2.0)
        val reversedC = listOf(
            ring(
                0.5 to 2.0, 4.0 to 2.0, 4.0 to 0.0, 0.5 to 0.0, 0.5 to 0.5,
                3.0 to 0.5, 3.0 to 1.5, 0.5 to 1.5, 0.5 to 2.0,
            )
        )

        val result = PolygonClipper.union(rectangle, reversedC)
        assertWellFormed(result)
        assertEquals(1, result.size)
        assertEquals(2, result[0].size, "expected one exterior ring and one hole")
    }

    @Test
    fun unionCanFillAHole() {
        val rectangle = box(0.0, 0.0, 1.0, 2.0)
        val reversedC = listOf(
            ring(
                0.5 to 2.0, 4.0 to 2.0, 4.0 to 0.0, 0.5 to 0.0, 0.5 to 0.5,
                3.0 to 0.5, 3.0 to 1.5, 0.5 to 1.5, 0.5 to 2.0,
            )
        )
        val withHole = PolygonClipper.union(rectangle, reversedC)[0]

        val result = PolygonClipper.union(withHole, box(0.0, 0.0, 4.0, 2.0))
        assertWellFormed(result)
        assertEquals(1, result.size)
        assertEquals(1, result[0].size, "the covering rectangle should fill the hole")
        assertEquals(8.0, area(result[0]), 1e-9)
    }

    @Test
    fun splittingADonutAndMergingItKeepsTheHole() {
        // The hole's right edge is exactly coincident with the added square's left edge, and
        // both polygons cross it the same way, so it must survive as SAME_TRANSITION.
        val donut = listOf(
            ring(0.0 to 0.0, 3.0 to 0.0, 3.0 to 3.0, 0.0 to 3.0, 0.0 to 0.0),
            ring(1.0 to 1.0, 1.0 to 2.0, 2.0 to 2.0, 2.0 to 1.0, 1.0 to 1.0),
        )
        val slab = box(2.0, 0.0, 3.0, 3.0)

        val result = PolygonClipper.union(donut, slab)
        assertWellFormed(result)
        assertEquals(1, result.size)
        assertEquals(2, result[0].size, "the donut's hole should survive")
        assertEquals(8.0, area(result[0]), 1e-9)
    }

    @Test
    fun unionIsCommutative() {
        val a = box(0.0, 0.0, 2.0, 1.0)
        val b = box(1.0, 0.0, 3.0, 1.0)
        val forward = PolygonClipper.union(a, b)
        val backward = PolygonClipper.union(b, a)
        assertEquals(forward.size, backward.size)
        assertEquals(area(forward[0]), area(backward[0]), 1e-9)
    }

    @Test
    fun repeatedAndUnclosedVerticesAreTolerated() {
        val sloppy = listOf(
            ring(
                0.0 to 0.0, 0.0 to 0.0, 1.0 to 0.0, 1.0 to 1.0, 1.0 to 1.0, 0.0 to 1.0,
            )
        )
        val result = PolygonClipper.union(sloppy, box(1.0, 0.0, 2.0, 1.0))
        assertWellFormed(result)
        assertEquals(1, result.size)
        assertEquals(2.0, area(result[0]), 1e-9)
    }

    @Test
    fun degenerateInputDoesNotThrow() {
        assertEquals(emptyList(), PolygonClipper.union(emptyList(), emptyList()))
        val collapsed = listOf(ring(1.0 to 1.0, 1.0 to 1.0, 1.0 to 1.0, 1.0 to 1.0))
        PolygonClipper.union(collapsed, collapsed)
        PolygonClipper.union(box(0.0, 0.0, 1.0, 1.0), collapsed)
    }

    @Test
    fun realTileSplitWarehouseMergesIntoOnePolygon() {
        // The two halves of an Amazon warehouse as they come out of two adjacent z14 tiles.
        // These three vertices are bit-identical in both, which is the coincident-edge case
        // the whole algorithm choice turns on:
        //   -2.641788125038147, 51.542920490515364
        //   -2.6419490575790405, 51.542963862172236
        //   -2.641710340976715,  51.54289880467142
        val part1 = listOf(
            ring(
                -2.641388475894928 to 51.54281206119232,
                -2.641863226890564 to 51.54281206119232,
                -2.641788125038147 to 51.542920490515364,
                -2.6419490575790405 to 51.542963862172236,
                -2.6418471336364746 to 51.54311232637706,
                -2.6416057348251343 to 51.543047269088504,
                -2.641710340976715 to 51.54289880467142,
                -2.641388475894928 to 51.54281206119232,
            )
        )
        val part2 = listOf(
            ring(
                -2.6413187384605408 to 51.54015462794914,
                -2.643338441848755 to 51.54069847186351,
                -2.6431426405906677 to 51.540978732447456,
                -2.643257975578308 to 51.54100876026479,
                -2.6431775093078613 to 51.54112553492164,
                -2.6430460810661316 to 51.54109050255605,
                -2.6425498723983765 to 51.541801153839955,
                -2.6426008343696594 to 51.54181449929781,
                -2.6425471901893616 to 51.54189290378369,
                -2.642509639263153 to 51.54188289470791,
                -2.641788125038147 to 51.542920490515364,
                -2.6419490575790405 to 51.542963862172236,
                -2.6419061422348022 to 51.54302558330498,
                -2.6416218280792236 to 51.54302558330498,
                -2.641710340976715 to 51.54289880467142,
                -2.6407554745674133 to 51.5426402418898,
                -2.6407313346862793 to 51.54267527306238,
                -2.640613317489624 to 51.54264357819311,
                -2.640637457370758 to 51.54260521069025,
                -2.639513611793518 to 51.54230327399525,
                -2.639618217945099 to 51.542153138981284,
                -2.639486789703369 to 51.54211810740676,
                -2.6406213641166687 to 51.54048827529291,
                -2.641012966632843 to 51.540593373699565,
                -2.6413187384605408 to 51.54015462794914,
            )
        )

        val result = PolygonClipper.union(part1, part2)
        assertWellFormed(result)
        assertEquals(1, result.size, "the two halves of one warehouse should merge")

        val merged = area(result[0])
        val a1 = area(part1)
        val a2 = area(part2)
        assertTrue(merged >= maxOf(a1, a2) - 1e-12, "union must not shrink either input")
        assertTrue(merged <= a1 + a2 + 1e-12, "union must not exceed the sum of the inputs")
    }
}
