@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.scottishtecharmy.soundscape.geoengine.callouts

import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.filters.CalloutHistory
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Intersection
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayEnd
import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geoengine.utils.PointAndDistanceAndHeading
import org.scottishtecharmy.soundscape.geoengine.utils.getDestinationCoordinate
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Fixture for a synthetic T-junction: the user is travelling north along "St Martins" which
 * T-ends into "Long Ashton Road", which continues both to the west (left) and east (right) of the
 * junction - mirroring the real-tile-data T1 scenario in app/src/test/.../IntersectionsTestMvt.kt,
 * but built entirely by hand so it can run without the MVT tile pipeline.
 *
 *            (west end) <---- Long Ashton Road ----> (east end)
 *                                  origin
 *                                    |
 *                                    | St Martins
 *                                    |
 *                              userLocation (20m south of origin)
 *                                    |
 *                              southEnd (30m south of origin)
 */
private class TJunctionFixture(
    val gridState: GridState,
    val origin: LngLatAlt,
    val userLocation: LngLatAlt,
    val wayOnRoad: Way,
    val wayLeft: Way,
    val wayRight: Way,
    val intersection: Intersection,
)

private fun buildTJunctionFixture(): TJunctionFixture {
    val origin = LngLatAlt(-2.657, 51.430)
    val southEnd = getDestinationCoordinate(origin, 180.0, 30.0)
    val westEnd = getDestinationCoordinate(origin, 270.0, 40.0)
    val eastEnd = getDestinationCoordinate(origin, 90.0, 40.0)
    val userLocation = getDestinationCoordinate(origin, 180.0, 20.0)

    val intersection = Intersection().apply {
        location = origin
        geometry = Point(origin)
    }

    val wayOnRoad = Way().apply {
        name = "St Martins"
        featureType = "highway"
        featureValue = "residential"
        geometry = LineString(southEnd, origin)
        intersections[WayEnd.END.id] = intersection
    }
    val wayLeft = Way().apply {
        name = "Long Ashton Road"
        featureType = "highway"
        featureValue = "residential"
        geometry = LineString(origin, westEnd)
        intersections[WayEnd.START.id] = intersection
    }
    val wayRight = Way().apply {
        name = "Long Ashton Road"
        featureType = "highway"
        featureValue = "residential"
        geometry = LineString(origin, eastEnd)
        intersections[WayEnd.START.id] = intersection
    }
    intersection.members = mutableListOf(wayOnRoad, wayLeft, wayRight)

    val gridState = GridState()
    // getRoadsDescriptionFromFov/addIntersectionCalloutFromDescription are normally called from
    // within gridState.treeContext (see AutoCallout.kt); calling them directly from a test thread
    // needs the context check disabled - see FileGridState in app/src/test's IntersectionsTestMvt.kt
    // for the same pattern against real tile data.
    gridState.validateContext = false
    gridState.featureTrees[TreeId.ROADS_AND_PATHS.id] = FeatureTree(
        FeatureCollection().apply {
            addFeature(wayOnRoad)
            addFeature(wayLeft)
            addFeature(wayRight)
        }
    )
    gridState.featureTrees[TreeId.INTERSECTIONS.id] = FeatureTree(
        FeatureCollection().apply { addFeature(intersection) }
    )
    gridState.gridIntersections[origin] = intersection

    return TJunctionFixture(gridState, origin, userLocation, wayOnRoad, wayLeft, wayRight, intersection)
}

class IntersectionUtilsTest {

    // ---- IntersectionDescription -------------------------------------------------------------

    @Test
    fun intersectionDescription_defaultsAreAllEmpty() {
        val description = IntersectionDescription()
        assertNull(description.nearestRoad)
        assertNull(description.intersection)
        assertNotNull(description.userGeometry)
    }

    // ---- getRoadsDescriptionFromFov ----------------------------------------------------------

    @Test
    fun getRoadsDescriptionFromFov_noRoadsInFov_fallsBackToMapMatchedWay() {
        val gridState = GridState()
        gridState.validateContext = false
        val mapMatchedWay = Way().apply { name = "Matched Way" }
        val userGeometry = UserGeometry(
            location = LngLatAlt(-2.657, 51.430),
            phoneHeading = 0.0,
            mapMatchedWay = mapMatchedWay,
        )

        val description = getRoadsDescriptionFromFov(gridState, userGeometry, null)

        assertEquals(mapMatchedWay, description.nearestRoad)
        assertNull(description.intersection)
    }

    @Test
    fun getRoadsDescriptionFromFov_roadButNoIntersectionInFov_returnsNearestRoadOnly() {
        val gridState = GridState()
        gridState.validateContext = false
        val origin = LngLatAlt(-2.657, 51.430)
        val northEnd = getDestinationCoordinate(origin, 0.0, 40.0)
        val way = Way().apply {
            name = "Lonely Road"
            geometry = LineString(origin, northEnd)
        }
        gridState.featureTrees[TreeId.ROADS_AND_PATHS.id] =
            FeatureTree(FeatureCollection().apply { addFeature(way) })

        val userGeometry = UserGeometry(location = origin, phoneHeading = 0.0)

        val description = getRoadsDescriptionFromFov(gridState, userGeometry, null)

        assertEquals(way, description.nearestRoad)
        assertNull(description.intersection)
    }

    @Test
    fun getRoadsDescriptionFromFov_tJunctionAhead_findsNearestRoadAndIntersection() {
        val fixture = buildTJunctionFixture()
        val userGeometry = UserGeometry(location = fixture.userLocation, phoneHeading = 0.0)

        val description = getRoadsDescriptionFromFov(fixture.gridState, userGeometry, null)

        assertEquals(fixture.wayOnRoad, description.nearestRoad)
        assertNotNull(description.intersection)
        assertEquals(fixture.intersection, description.intersection)
        assertEquals(3, description.intersection.members.size)
    }

    // ---- addIntersectionCalloutFromDescription: no intersection (nearby road only) -----------

    @Test
    fun addIntersectionCallout_noNearestRoadNoIntersection_returnsNull() {
        val description = IntersectionDescription(nearestRoad = null, intersection = null)
        assertNull(addIntersectionCalloutFromDescription(description, null, null, GridState()))
    }

    @Test
    fun addIntersectionCallout_nearestRoadButNoMapMatchedLocation_returnsNull() {
        val way = Way().apply { name = "Some Road" }
        val description = IntersectionDescription(
            nearestRoad = way,
            userGeometry = UserGeometry(location = LngLatAlt(-2.657, 51.430)),
            intersection = null,
        )
        assertNull(addIntersectionCalloutFromDescription(description, null, null, GridState()))
    }

    @Test
    fun addIntersectionCallout_travellingForwardAlongMatchedRoad_returnsAheadCallout() {
        val way = Way().apply { name = "Some Road" }
        val matched = PointAndDistanceAndHeading(heading = 0.0)
        val userGeometry = UserGeometry(
            location = LngLatAlt(-2.657, 51.430),
            phoneHeading = 0.0,
            mapMatchedLocation = matched,
        )
        val description = IntersectionDescription(nearestRoad = way, userGeometry = userGeometry, intersection = null)

        val callout = addIntersectionCalloutFromDescription(description, null, null, GridState())

        assertNotNull(callout)
        assertEquals(1, callout.positionedStrings.size)
        assertEquals("Ahead Some Road", callout.positionedStrings[0].text)
        assertEquals(false, callout.isPoint)
    }

    @Test
    fun addIntersectionCallout_travellingBackwardAlongMatchedRoad_returnsAheadCallout() {
        val way = Way().apply { name = "Some Road" }
        val matched = PointAndDistanceAndHeading(heading = 0.0)
        val userGeometry = UserGeometry(
            location = LngLatAlt(-2.657, 51.430),
            // 180 degrees away from matched.heading snaps to "matched.heading + 180", the other
            // valid direction of travel along the matched way.
            phoneHeading = 180.0,
            mapMatchedLocation = matched,
        )
        val description = IntersectionDescription(nearestRoad = way, userGeometry = userGeometry, intersection = null)

        val callout = addIntersectionCalloutFromDescription(description, null, null, GridState())

        assertNotNull(callout)
        assertEquals(1, callout.positionedStrings.size)
        assertEquals("Ahead Some Road", callout.positionedStrings[0].text)
    }

    @Test
    fun addIntersectionCallout_travellingAcrossMatchedRoad_returnsNull() {
        val way = Way().apply { name = "Some Road" }
        val matched = PointAndDistanceAndHeading(heading = 0.0)
        val userGeometry = UserGeometry(
            location = LngLatAlt(-2.657, 51.430),
            // Perpendicular to the matched way's heading - neither "forward" nor "backward".
            phoneHeading = 90.0,
            mapMatchedLocation = matched,
        )
        val description = IntersectionDescription(nearestRoad = way, userGeometry = userGeometry, intersection = null)

        assertNull(addIntersectionCalloutFromDescription(description, null, null, GridState()))
    }

    @Test
    fun addIntersectionCallout_aheadCallout_isSuppressedBySecondCalloutHistoryLookup() {
        val way = Way().apply { name = "Some Road" }
        val matched = PointAndDistanceAndHeading(heading = 0.0)
        val userGeometry = UserGeometry(
            location = LngLatAlt(-2.657, 51.430),
            phoneHeading = 0.0,
            mapMatchedLocation = matched,
        )
        val description = IntersectionDescription(nearestRoad = way, userGeometry = userGeometry, intersection = null)
        val history = CalloutHistory()

        val first = addIntersectionCalloutFromDescription(description, null, history, GridState())
        assertNotNull(first)
        history.add(first)

        val second = addIntersectionCalloutFromDescription(description, null, history, GridState())
        assertNull(second)
    }

    // ---- addIntersectionCalloutFromDescription: intersection present --------------------------

    @Test
    fun addIntersectionCallout_intersectionWithOnlyTwoMembers_returnsNull() {
        val origin = LngLatAlt(-2.657, 51.430)
        val behindEnd = getDestinationCoordinate(origin, 180.0, 30.0)
        val aheadEnd = getDestinationCoordinate(origin, 0.0, 30.0)
        val intersection = Intersection().apply {
            location = origin
            geometry = Point(origin)
        }
        val wayBehind = Way().apply {
            name = "Road A"
            geometry = LineString(behindEnd, origin)
            intersections[WayEnd.END.id] = intersection
        }
        val wayAhead = Way().apply {
            name = "Road A"
            geometry = LineString(origin, aheadEnd)
            intersections[WayEnd.START.id] = intersection
        }
        intersection.members = mutableListOf(wayBehind, wayAhead)

        val description = IntersectionDescription(
            nearestRoad = wayBehind,
            userGeometry = UserGeometry(location = behindEnd),
            intersection = intersection,
        )

        assertNull(addIntersectionCalloutFromDescription(description, null, null, GridState()))
    }

    @Test
    fun addIntersectionCallout_tJunction_describesLeftAndRightRoadsSortedByHeading() {
        val fixture = buildTJunctionFixture()
        val userGeometry = UserGeometry(location = fixture.userLocation, phoneHeading = 0.0)
        val description = IntersectionDescription(
            nearestRoad = fixture.wayOnRoad,
            userGeometry = userGeometry,
            intersection = fixture.intersection,
        )

        val callout = addIntersectionCalloutFromDescription(description, null, null, fixture.gridState)

        assertNotNull(callout)
        assertTrue(callout.isPoint)
        assertEquals(3, callout.positionedStrings.size)

        // Sorted by heading: the "approaching intersection" marker (-10000), then the left turn
        // (-90), then the right turn (+90).
        assertEquals("Approaching intersection", callout.positionedStrings[0].text)
        assertEquals(-10000.0, callout.positionedStrings[0].heading)

        assertEquals("\tLong Ashton Road goes left", callout.positionedStrings[1].text)
        assertEquals(-90.0, callout.positionedStrings[1].heading)

        assertEquals("\tLong Ashton Road goes right", callout.positionedStrings[2].text)
        assertEquals(90.0, callout.positionedStrings[2].heading)
    }

    @Test
    fun addIntersectionCallout_tJunction_isSuppressedBySecondCalloutHistoryLookup() {
        val fixture = buildTJunctionFixture()
        val userGeometry = UserGeometry(location = fixture.userLocation, phoneHeading = 0.0)
        val description = IntersectionDescription(
            nearestRoad = fixture.wayOnRoad,
            userGeometry = userGeometry,
            intersection = fixture.intersection,
        )
        val history = CalloutHistory()

        val first = addIntersectionCalloutFromDescription(description, null, history, fixture.gridState)
        assertNotNull(first)
        history.add(first)

        val second = addIntersectionCalloutFromDescription(description, null, history, fixture.gridState)
        assertNull(second)
    }
}
