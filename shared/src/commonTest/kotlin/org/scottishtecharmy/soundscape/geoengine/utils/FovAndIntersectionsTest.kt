package org.scottishtecharmy.soundscape.geoengine.utils

import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Intersection
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FovAndIntersectionsTest {

    // --- checkWhetherIntersectionIsOfInterest ---

    @Test
    fun returnsZeroWhenTestNearestRoadIsNull() {
        val intersection = Intersection().apply {
            members = mutableListOf(Way(), Way(), Way())
        }
        assertEquals(0, checkWhetherIntersectionIsOfInterest(intersection, null))
    }

    @Test
    fun returnsMinusOneForIntersectionsWithTwoOrFewerWays() {
        val nearestRoad = Way().apply { name = "Main Street" }

        val zeroWays = Intersection().apply { members = mutableListOf() }
        val oneWay = Intersection().apply { members = mutableListOf(Way()) }
        val twoWays = Intersection().apply { members = mutableListOf(Way(), Way()) }

        assertEquals(-1, checkWhetherIntersectionIsOfInterest(zeroWays, nearestRoad))
        assertEquals(-1, checkWhetherIntersectionIsOfInterest(oneWay, nearestRoad))
        assertEquals(-1, checkWhetherIntersectionIsOfInterest(twoWays, nearestRoad))
    }

    @Test
    fun countsDistinctNamedWaysExcludingTheRoadWeAreOn() {
        val nearestRoad = Way().apply { name = "Main Street" }
        val sameRoad = Way().apply { name = "Main Street" } // On our road, ignored
        val elmStreet = Way().apply { name = "Elm Street" }
        val oakStreet = Way().apply { name = "Oak Street" }

        val intersection = Intersection().apply {
            members = mutableListOf(sameRoad, elmStreet, oakStreet)
        }

        assertEquals(2, checkWhetherIntersectionIsOfInterest(intersection, nearestRoad))
    }

    @Test
    fun unnamedWaysContributeNoPoints() {
        val nearestRoad = Way().apply { name = "Main Street" }
        val sameRoad = Way().apply { name = "Main Street" }
        val unnamed = Way() // name == null
        val elmStreet = Way().apply { name = "Elm Street" }

        val intersection = Intersection().apply {
            members = mutableListOf(sameRoad, unnamed, elmStreet)
        }

        assertEquals(1, checkWhetherIntersectionIsOfInterest(intersection, nearestRoad))
    }

    @Test
    fun duplicateRoadNamesAreOnlyCountedOnce() {
        val nearestRoad = Way().apply { name = "Main Street" }
        val elmStreet1 = Way().apply { name = "Elm Street" }
        val elmStreet2 = Way().apply { name = "Elm Street" }
        val oakStreet = Way().apply { name = "Oak Street" }

        val intersection = Intersection().apply {
            members = mutableListOf(elmStreet1, elmStreet2, oakStreet)
        }

        assertEquals(2, checkWhetherIntersectionIsOfInterest(intersection, nearestRoad))
    }

    // --- makeTriangles ---

    @Test
    fun makeTrianglesReturnsOneFeaturePerSegmentInOrder() {
        val location = LngLatAlt(-4.25, 55.86)
        val userGeometry = UserGeometry(location = location, fovDistance = 50.0)
        val segments = arrayOf(
            Segment(0.0, 90.0),
            Segment(90.0, 90.0),
            Segment(180.0, 90.0),
        )

        val result = makeTriangles(segments, userGeometry)

        assertEquals(3, result.features.size)
        for ((index, feature) in result.features.withIndex()) {
            assertEquals(index, feature.properties?.get("Direction"))
            assertEquals("Polygon", feature.geometry.type)
        }
    }

    @Test
    fun makeTrianglesGeometryMatchesDirectlyComputedTriangle() {
        val location = LngLatAlt(-4.25, 55.86)
        val userGeometry = UserGeometry(location = location, fovDistance = 50.0)
        val segment = Segment(45.0, 90.0)

        val result = makeTriangles(arrayOf(segment), userGeometry)

        val expectedPolygon = createPolygonFromTriangle(
            Triangle(
                location,
                getDestinationCoordinate(location, segment.left, userGeometry.fovDistance),
                getDestinationCoordinate(location, segment.right, userGeometry.fovDistance),
            )
        )

        val actualPolygon = result.features[0].geometry as Polygon
        assertEquals(expectedPolygon.coordinates, actualPolygon.coordinates)
    }

    @Test
    fun makeTrianglesWithNoSegmentsReturnsEmptyCollection() {
        val userGeometry = UserGeometry(location = LngLatAlt(0.0, 0.0), fovDistance = 50.0)
        val result = makeTriangles(emptyArray(), userGeometry)
        assertTrue(result.features.isEmpty())
    }
}
