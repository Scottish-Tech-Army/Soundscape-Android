package org.scottishtecharmy.soundscape.geoengine.utils

import org.scottishtecharmy.soundscape.geoengine.mvttranslation.AlongWayFeature
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.AlongWayKind
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Intersection
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayEnd
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayType
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for the along-way queries - the lookups that replace searching around the user for things
 * that are actually positioned along the road they're on.
 *
 * Fixtures are hand-built Way/Intersection graphs, following StreetPreviewTest.kt and
 * RoutingUtilsTest.kt, so nothing here depends on the MVT tile pipeline.
 */
class AlongWayTest {

    private val origin = LngLatAlt(-4.3231, 55.9461)
    private val ruler = CheapRuler(origin.latitude)

    /** A point [east] metres due east of the origin. */
    private fun east(metres: Double) = ruler.offset(origin, metres, 0.0)

    private fun intersectionAt(location: LngLatAlt) = Intersection().apply {
        this.location = location
        geometry = Point(location)
    }

    /** A straight west-to-east Way from [fromMetres] to [toMetres] east of the origin. */
    private fun straightWay(name: String, fromMetres: Double, toMetres: Double): Way {
        val start = east(fromMetres)
        val end = east(toMetres)
        return Way().apply {
            this.name = name
            featureType = "highway"
            featureValue = "residential"
            geometry = LineString(start, end)
            length = ruler.distance(start, end)
        }
    }

    /**
     * A straight Way covering the same ground as [straightWay] but digitised the other way round,
     * so its START is the eastern end. OSM says nothing about which way traffic runs by the order
     * of a way's nodes, so the pieces one road is split into routinely disagree like this.
     */
    private fun reversedWay(name: String, fromMetres: Double, toMetres: Double): Way {
        val start = east(toMetres)
        val end = east(fromMetres)
        return Way().apply {
            this.name = name
            featureType = "highway"
            featureValue = "residential"
            geometry = LineString(start, end)
            length = ruler.distance(start, end)
        }
    }

    /** Joins two Ways which meet END to END, as two pieces digitised towards each other do. */
    private fun joinEndToEnd(before: Way, after: Way): Intersection {
        val intersection = intersectionAt((after.geometry as LineString).coordinates.last())
        before.intersections[WayEnd.END.id] = intersection
        after.intersections[WayEnd.END.id] = intersection
        intersection.members.add(before)
        intersection.members.add(after)
        return intersection
    }

    private fun join(before: Way, after: Way): Intersection {
        val intersection = intersectionAt((after.geometry as LineString).coordinates.first())
        before.intersections[WayEnd.END.id] = intersection
        after.intersections[WayEnd.START.id] = intersection
        intersection.members.add(before)
        intersection.members.add(after)
        return intersection
    }

    private fun Way.addCrossing(
        atMetresAlong: Double,
        name: String,
        kind: AlongWayKind = AlongWayKind.WATERWAY_CROSSING
    ) {
        addAlongWayFeature(
            AlongWayFeature(
                distanceFromStart = atMetresAlong,
                point = ruler.along(geometry as LineString, atMetresAlong),
                kind = kind,
                name = name
            )
        )
    }

    private fun Way.addAlong(atMetresAlong: Double, name: String, kind: AlongWayKind) {
        addAlongWayFeature(
            AlongWayFeature(
                distanceFromStart = atMetresAlong,
                point = ruler.along(geometry as LineString, atMetresAlong),
                kind = kind,
                name = name
            )
        )
    }

    private fun Way.addStop(atMetresAlong: Double, name: String, side: Side) {
        addAlongWayFeature(
            AlongWayFeature(
                distanceFromStart = atMetresAlong,
                point = ruler.along(geometry as LineString, atMetresAlong),
                kind = AlongWayKind.TRANSIT_STOP,
                name = name,
                side = side
            )
        )
    }

    // ---- slicing -------------------------------------------------------------------------

    @Test
    fun featuresAreSlicedAroundADistance() {
        val way = straightWay("Main Street", 0.0, 300.0)
        way.addCrossing(50.0, "First")
        way.addCrossing(150.0, "Second")
        way.addCrossing(250.0, "Third")

        assertEquals(
            listOf("Second", "Third"),
            way.alongWayFeaturesAfter(100.0).map { it.name }
        )
        // Before is nearest-first, so descending - the order they're met heading END to START.
        assertEquals(
            listOf("Second", "First"),
            way.alongWayFeaturesBefore(200.0).map { it.name }
        )
        // The boundary is exclusive above and inclusive below, so a feature exactly at the cursor
        // counts as passed rather than still ahead.
        assertEquals(listOf("Third"), way.alongWayFeaturesAfter(150.0).map { it.name })
        assertEquals(
            listOf("Second", "First"),
            way.alongWayFeaturesBefore(150.0).map { it.name }
        )
        assertTrue(way.alongWayFeaturesAfter(300.0).isEmpty())
        assertTrue(way.alongWayFeaturesBefore(0.0).isEmpty())
    }

    @Test
    fun binarySearchAgreesWithAScan() {
        val way = straightWay("Long Road", 0.0, 1000.0)
        val distances = listOf(10.0, 10.0, 250.0, 251.0, 700.0, 999.0)
        for ((index, distance) in distances.withIndex()) {
            way.addCrossing(distance, "Crossing $index")
        }
        for (probe in listOf(-1.0, 0.0, 9.9, 10.0, 10.1, 250.5, 700.0, 998.0, 1000.0)) {
            assertEquals(
                way.alongWayFeatures.count { it.distanceFromStart <= probe },
                way.alongWayFeatures.firstIndexBeyond(probe),
                "firstIndexBeyond($probe)"
            )
        }
    }

    // ---- walking one Way -------------------------------------------------------------------

    @Test
    fun lookaheadMeasuresAlongTheWayInTheDirectionOfTravel() {
        val way = straightWay("Main Street", 0.0, 300.0)
        way.addCrossing(50.0, "Behind")
        way.addCrossing(200.0, "Ahead")

        val forwards = nextAlongWayFeature(WayCursor(way, 100.0, forwards = true), 500.0)
        assertEquals("Ahead", forwards?.feature?.name)
        assertEquals(100.0, forwards!!.distance, 0.5)

        val backwards = nextAlongWayFeature(WayCursor(way, 100.0, forwards = false), 500.0)
        assertEquals("Behind", backwards?.feature?.name)
        assertEquals(50.0, backwards!!.distance, 0.5)
    }

    @Test
    fun lookaheadStopsAtMaxDistance() {
        val way = straightWay("Main Street", 0.0, 300.0)
        way.addCrossing(200.0, "Ahead")

        assertNull(nextAlongWayFeature(WayCursor(way, 100.0, forwards = true), 99.0))
        assertEquals(
            "Ahead",
            nextAlongWayFeature(WayCursor(way, 100.0, forwards = true), 101.0)?.feature?.name
        )
    }

    @Test
    fun unknownDirectionLooksBothWaysNearestFirst() {
        val way = straightWay("Main Street", 0.0, 300.0)
        way.addCrossing(60.0, "Behind")
        way.addCrossing(200.0, "Ahead")

        // 40m back, 100m forward - so the one behind wins despite being behind.
        val found = nextAlongWayFeature(WayCursor(way, 100.0, forwards = null), 500.0)
        assertEquals("Behind", found?.feature?.name)
        assertEquals(40.0, found!!.distance, 0.5)
    }

    @Test
    fun kindFiltersTheSearch() {
        val way = straightWay("Main Street", 0.0, 300.0)
        way.addCrossing(120.0, "The River", AlongWayKind.WATERWAY_CROSSING)
        way.addCrossing(200.0, "The Line", AlongWayKind.RAILWAY_CROSSING)

        val cursor = WayCursor(way, 0.0, forwards = true)
        assertEquals(
            "The Line",
            nextAlongWayFeature(cursor, 500.0, AlongWayKind.RAILWAY_CROSSING)?.feature?.name
        )
        assertEquals("The River", nextAlongWayFeature(cursor, 500.0)?.feature?.name)
    }

    // ---- walking between Ways --------------------------------------------------------------

    @Test
    fun lookaheadContinuesIntoTheNextWay() {
        // 0---100---200---300 metres east, as three joined Ways.
        val first = straightWay("First", 0.0, 100.0)
        val second = straightWay("Second", 100.0, 200.0)
        val third = straightWay("Third", 200.0, 300.0)
        join(first, second)
        join(second, third)

        // The crossing is two Ways further on. This is the case the old crow-fly radius could only
        // reach by attaching the crossing to every piece of the road.
        third.addCrossing(50.0, "Far Crossing")

        val cursor = WayCursor(first, 20.0, forwards = true)
        val found = nextAlongWayFeature(cursor, 500.0)
        assertEquals("Far Crossing", found?.feature?.name)
        // 80m to the end of first, 100m across second, 50m into third.
        assertEquals(230.0, found!!.distance, 1.0)
        assertEquals(third, found.way)

        // ...and it is still out of reach when the lookahead is shorter than the walk.
        assertNull(nextAlongWayFeature(cursor, 200.0))
    }

    @Test
    fun lookaheadContinuesBackwardsIntoThePreviousWay() {
        val first = straightWay("First", 0.0, 100.0)
        val second = straightWay("Second", 100.0, 200.0)
        join(first, second)
        first.addCrossing(30.0, "Behind Crossing")

        // Travelling east-to-west along second, so its END is behind us.
        val found = nextAlongWayFeature(WayCursor(second, 40.0, forwards = false), 500.0)
        assertEquals("Behind Crossing", found?.feature?.name)
        // 40m back to the start of second, then 70m back along first.
        assertEquals(110.0, found!!.distance, 1.0)
    }

    @Test
    fun lookaheadStopsAtAJunction() {
        val first = straightWay("First", 0.0, 100.0)
        val second = straightWay("Second", 100.0, 200.0)
        val junction = join(first, second)
        second.addCrossing(50.0, "Past The Junction")

        // A third road meeting the same intersection makes it a real junction rather than a
        // pass-through node, and there's then no single road ahead to be looking down - see
        // Way.followWays, which is where this rule lives.
        val sideRoad = straightWay("Side Road", 100.0, 180.0)
        sideRoad.intersections[WayEnd.START.id] = junction
        junction.members.add(sideRoad)

        assertNull(nextAlongWayFeature(WayCursor(first, 20.0, forwards = true), 500.0))
    }

    @Test
    fun sameRoadFollowsTheRoadThroughAJunction() {
        val first = straightWay("Main Street", 0.0, 100.0)
        val second = straightWay("Main Street", 100.0, 200.0)
        val junction = join(first, second)
        second.addCrossing(50.0, "Past The Junction")

        // A side road makes this a real junction, which STRAIGHT_ON refuses to cross.
        val sideRoad = straightWay("Side Road", 100.0, 180.0)
        sideRoad.intersections[WayEnd.START.id] = junction
        junction.members.add(sideRoad)

        val cursor = WayCursor(first, 20.0, forwards = true)
        assertNull(nextAlongWayFeature(cursor, 500.0))

        // Following the road by name gets there - 80m to the junction, 50m beyond it.
        val found = nextAlongWayFeature(
            cursor, 500.0, continuation = WayContinuation.SAME_ROAD
        )
        assertEquals("Past The Junction", found?.feature?.name)
        assertEquals(130.0, found!!.distance, 1.0)
    }

    @Test
    fun sameRoadFollowsARefWhenThereIsNoName() {
        val first = straightWay("", 0.0, 100.0).apply { name = null; ref = "A81" }
        val second = straightWay("", 100.0, 200.0).apply { name = null; ref = "A81" }
        val junction = join(first, second)
        second.addCrossing(50.0, "Up The A81")

        val sideRoad = straightWay("Side Road", 100.0, 180.0)
        sideRoad.intersections[WayEnd.START.id] = junction
        junction.members.add(sideRoad)

        val found = nextAlongWayFeature(
            WayCursor(first, 20.0, forwards = true), 500.0,
            continuation = WayContinuation.SAME_ROAD
        )
        assertEquals("Up The A81", found?.feature?.name)
    }

    @Test
    fun sameRoadStopsWhenTheContinuationIsAmbiguous() {
        val first = straightWay("Main Street", 0.0, 100.0)
        val second = straightWay("Main Street", 100.0, 200.0)
        val junction = join(first, second)
        second.addCrossing(50.0, "Past The Junction")

        // A staggered junction where the other arm carries the same name too: there is no single
        // road ahead, so stopping beats guessing.
        val otherArm = straightWay("Main Street", 100.0, 180.0)
        otherArm.intersections[WayEnd.START.id] = junction
        junction.members.add(otherArm)

        assertNull(
            nextAlongWayFeature(
                WayCursor(first, 20.0, forwards = true), 500.0,
                continuation = WayContinuation.SAME_ROAD
            )
        )
    }

    @Test
    fun sameRoadCrossesATileJoiner() {
        // The shape a road takes across a tile boundary: it ends at a TILE_EDGE intersection, a
        // zero-length JOINER links that to the matching intersection in the next tile, and the
        // road resumes there. The joiner carries no name to match on, so it has to be followed on
        // the grounds that nothing else at that intersection continues the road.
        val first = straightWay("Main Street", 0.0, 100.0)
        val second = straightWay("Main Street", 100.0, 200.0)
        second.addCrossing(50.0, "Over The Tile Edge")

        val edgeA = intersectionAt(east(100.0))
        val edgeB = intersectionAt(east(100.0))
        val joiner = straightWay("", 100.0, 100.0).apply {
            name = null
            wayType = WayType.JOINER
            length = 0.0
        }
        first.intersections[WayEnd.END.id] = edgeA
        joiner.intersections[WayEnd.START.id] = edgeA
        joiner.intersections[WayEnd.END.id] = edgeB
        second.intersections[WayEnd.START.id] = edgeB
        edgeA.members.add(first)
        edgeA.members.add(joiner)
        edgeB.members.add(joiner)
        edgeB.members.add(second)

        // A side road at the tile edge stops STRAIGHT_ON, so the joiner has to be chosen rather
        // than simply being the only option.
        val sideRoad = straightWay("Side Road", 100.0, 180.0)
        sideRoad.intersections[WayEnd.START.id] = edgeA
        edgeA.members.add(sideRoad)

        val cursor = WayCursor(first, 20.0, forwards = true)
        assertNull(nextAlongWayFeature(cursor, 500.0))
        val found = nextAlongWayFeature(
            cursor, 500.0, continuation = WayContinuation.SAME_ROAD
        )
        assertEquals("Over The Tile Edge", found?.feature?.name)
        assertEquals(130.0, found!!.distance, 1.0)
    }

    @Test
    fun theNextStopIsFoundAlongTheLine() {
        // The shape a railway takes: one named line split into pieces, with railway=stop nodes
        // recorded against the piece each sits on. A stop node commonly lands exactly on the
        // boundary between two pieces, which is why the walk has to cross them rather than only
        // reading the piece the train is currently matched to.
        val first = straightWay("North Clyde Line", 0.0, 400.0)
        val second = straightWay("North Clyde Line", 400.0, 900.0)
        val junction = join(first, second)

        // A branch makes the join a real junction, so only SAME_ROAD gets past it.
        val branch = straightWay("Argyle Line", 400.0, 700.0)
        branch.intersections[WayEnd.START.id] = junction
        junction.members.add(branch)

        second.addAlong(120.0, "Singer", AlongWayKind.RAILWAY_STOP)
        branch.addAlong(50.0, "Wrong Line", AlongWayKind.RAILWAY_STOP)

        val cursor = WayCursor(first, 100.0, forwards = true)
        val found = nextAlongWayFeature(
            cursor, 500.0, AlongWayKind.RAILWAY_STOP, WayContinuation.SAME_ROAD
        )
        assertEquals("Singer", found?.feature?.name)
        // 300m to the end of the first piece, 120m into the second.
        assertEquals(420.0, found!!.distance, 1.0)
    }

    @Test
    fun aStopOnAnotherLineIsNotTheNextStop() {
        // Two lines meeting: following by name must not wander onto the branch, which is the whole
        // reason a stop node on the line beats the nearest station to the train.
        val main = straightWay("North Clyde Line", 0.0, 400.0)
        val branch = straightWay("Argyle Line", 400.0, 900.0)
        val other = straightWay("Argyle Line", 400.0, 700.0)
        val junction = join(main, branch)
        other.intersections[WayEnd.START.id] = junction
        junction.members.add(other)

        branch.addAlong(50.0, "Wrong Line", AlongWayKind.RAILWAY_STOP)

        assertNull(
            nextAlongWayFeature(
                WayCursor(main, 100.0, forwards = true), 500.0,
                AlongWayKind.RAILWAY_STOP, WayContinuation.SAME_ROAD
            )
        )
    }

    // ---- which way round the Way under the feature is -----------------------------------

    /**
     * A feature reports the walk's direction along *its own* Way, which is not the cursor's
     * direction once the walk has crossed into a piece digitised the other way round.
     *
     * This is what anything recorded relative to a Way's own direction has to be read against.
     * AlongWayFeature.side is the case that matters: a bus stop's kerb is stored relative to its
     * Way's START-to-END, so reading it against the cursor instead announces the stop across the
     * road - the one serving the opposite direction - for every stop on a reversed continuation.
     */
    @Test
    fun aFeatureCarriesTheWalkDirectionOfItsOwnWay() {
        // 0---100 east as "first", then 200---100 east as "second": the two pieces are digitised
        // towards each other and meet END to END.
        val first = straightWay("Main Street", 0.0, 100.0)
        val second = reversedWay("Main Street", 100.0, 200.0)
        joinEndToEnd(first, second)

        // 30m from second's START, which is its *eastern* end - so 170m east of the origin.
        second.addStop(30.0, "Far Piece Stop", Side.LEFT)

        val cursor = WayCursor(first, 20.0, forwards = true)
        val found = nextAlongWayFeature(
            cursor, 500.0, AlongWayKind.TRANSIT_STOP, WayContinuation.SAME_ROAD
        )
        assertEquals("Far Piece Stop", found?.feature?.name)
        // 80m to the end of first, then 70m back along second from the end it was entered by.
        assertEquals(150.0, found!!.distance, 1.0)
        assertEquals(second, found.way)

        // The cursor is travelling its own Way forwards, but the walk meets this feature going
        // against second's direction, and it is second's direction the recorded side refers to.
        assertTrue(cursor.forwards!!)
        assertFalse(found.forwards)
    }

    @Test
    fun aFeatureOnASameDirectionContinuationKeepsTheWalkDirection() {
        // The other half of the pair above: where the pieces agree, so does the reported
        // direction, and the walk direction is the cursor's.
        val first = straightWay("Main Street", 0.0, 100.0)
        val second = straightWay("Main Street", 100.0, 200.0)
        join(first, second)
        second.addStop(50.0, "Same Direction Stop", Side.LEFT)

        val ahead = nextAlongWayFeature(
            WayCursor(first, 20.0, forwards = true), 500.0,
            AlongWayKind.TRANSIT_STOP, WayContinuation.SAME_ROAD
        )
        assertEquals("Same Direction Stop", ahead?.feature?.name)
        assertTrue(ahead!!.forwards)

        // ...and a walk that sets off backwards reports backwards.
        val behind = nextAlongWayFeature(
            WayCursor(second, 80.0, forwards = false), 500.0,
            AlongWayKind.TRANSIT_STOP, WayContinuation.SAME_ROAD
        )
        assertEquals("Same Direction Stop", behind?.feature?.name)
        assertEquals(30.0, behind!!.distance, 1.0)
        assertFalse(behind.forwards)
    }

    /**
     * A feature sitting exactly on the node a road was split at is still ahead of a walk arriving
     * from the previous piece.
     *
     * The slice is exclusive at the cursor, because a feature at the user's own position has been
     * passed rather than reached. A Way boundary is not the user's position though, and
     * railway=stop nodes land on one routinely - a station throat, where the line splits as the
     * tracks diverge, puts the stop node on the split itself.
     */
    @Test
    fun aFeatureOnTheBoundaryOfTheNextWayIsStillAhead() {
        val first = straightWay("North Clyde Line", 0.0, 100.0)
        val second = straightWay("North Clyde Line", 100.0, 200.0)
        join(first, second)
        second.addAlong(0.0, "On The Boundary", AlongWayKind.RAILWAY_STOP)

        val found = nextAlongWayFeature(
            WayCursor(first, 20.0, forwards = true), 500.0,
            AlongWayKind.RAILWAY_STOP, WayContinuation.SAME_ROAD
        )
        assertEquals("On The Boundary", found?.feature?.name)
        // 80m to the end of the first piece, and the stop is on the boundary itself.
        assertEquals(80.0, found!!.distance, 1.0)
        assertEquals(second, found.way)
    }

    @Test
    fun aFeatureAtTheCursorItselfHasBeenPassed() {
        // The other side of the rule above: on the Way the walk starts from, the entry point is
        // the user, and something level with them is behind rather than ahead.
        val way = straightWay("Main Street", 0.0, 300.0)
        way.addAlong(20.0, "Level With Us", AlongWayKind.RAILWAY_STOP)

        assertNull(
            nextAlongWayFeature(
                WayCursor(way, 20.0, forwards = true), 500.0, AlongWayKind.RAILWAY_STOP
            )
        )
    }

    @Test
    fun anUnattachedWayIsWalkedOnItsOwn() {
        // No intersections at all, as built by the synthetic fixtures in the callout tests.
        val way = straightWay("Orphan", 0.0, 300.0)
        way.addCrossing(200.0, "Ahead")

        assertEquals(
            "Ahead",
            nextAlongWayFeature(WayCursor(way, 100.0, forwards = true), 500.0)?.feature?.name
        )
    }
}
