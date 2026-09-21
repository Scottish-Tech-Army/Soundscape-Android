package org.scottishtecharmy.soundscape.geoengine

import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.utils.PointAndDistanceAndHeading
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests for UserGeometry.cursorOn - where along a Way the user is, which is the origin every
 * along-way lookahead is measured from.
 */
class UserGeometryCursorTest {

    private val origin = LngLatAlt(-4.3231, 55.9461)
    private val ruler = CheapRuler(origin.latitude)

    private fun east(metres: Double) = ruler.offset(origin, metres, 0.0)

    /** A straight west-to-east Way [northMetres] north of the origin. */
    private fun straightWay(name: String, northMetres: Double): Way {
        val start = ruler.offset(east(0.0), 0.0, northMetres)
        val end = ruler.offset(east(1000.0), 0.0, northMetres)
        return Way().apply {
            this.name = name
            geometry = LineString(start, end)
            length = ruler.distance(start, end)
        }
    }

    /**
     * The map-matched point stands in for the raw fix only on the Way it was matched to.
     *
     * mapMatchedLocation is a point on mapMatchedWay - the road. It is the better answer there,
     * having already been smoothed against the road network. On any other Way it is not an answer
     * at all: it carries the road matcher's own offset, and a road running beside a railway can be
     * tens of metres from it and offset along it too. That offset would go straight into the
     * cursor's distance along the line, and so into the station and crossing lookaheads a train
     * passenger's callouts are built from.
     */
    @Test
    fun theCursorOnAnotherWayIsProjectedFromTheRawLocation() {
        val railway = straightWay("North Clyde Line", 0.0)
        val road = straightWay("Great Western Road", 50.0)

        // The fix says the user is 200m along the line. The road matcher has snapped them to a
        // point on the road 50m to the north and 40m further east - an offset in both axes, as a
        // real match against a road at an angle to the line gives.
        val location = east(200.0)
        val matchedPoint = ruler.offset(east(240.0), 0.0, 50.0)
        val userGeometry = UserGeometry(
            location = location,
            speed = 15.0,
            mapMatchedWay = road,
            mapMatchedLocation = PointAndDistanceAndHeading(
                point = matchedPoint,
                distance = ruler.distance(location, matchedPoint),
                heading = 90.0
            ),
            mapMatchedRailway = railway
        )

        // On the railway the raw fix is what counts, so 200m along it - not the 240m the road
        // match would project to.
        val railwayCursor = userGeometry.cursorOn(railway)
        assertNotNull(railwayCursor)
        assertEquals(200.0, railwayCursor.distanceFromStart, 1.0)

        // ...while the road itself still gets the smoothed point, which is the whole reason
        // mapMatchedLocation is preferred there.
        val roadCursor = userGeometry.cursorOn(road)
        assertNotNull(roadCursor)
        assertEquals(240.0, roadCursor.distanceFromStart, 1.0)
    }

    @Test
    fun theCursorFallsBackToTheRawLocationWithNoMatch() {
        val road = straightWay("Great Western Road", 0.0)
        val userGeometry = UserGeometry(location = east(300.0), speed = 15.0, mapMatchedWay = road)

        val cursor = userGeometry.cursorOn(road)
        assertNotNull(cursor)
        assertEquals(300.0, cursor.distanceFromStart, 1.0)
    }
}
