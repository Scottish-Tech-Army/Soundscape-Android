package org.scottishtecharmy.soundscape.geoengine

import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Intersection
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayEnd
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayType
import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geoengine.utils.getDestinationCoordinate
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.locationprovider.StaticLocationProvider
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [StreetPreview], the state machine that drives "Street Preview" mode: starting on the
 * nearest road, snapping to its nearest junction, then repeatedly offering the roads available at
 * each junction and following the one closest to the current heading until a dead end (or an
 * unmapped road end) is reached.
 *
 * All fixtures are hand-built Way/Intersection graphs (no MVT tile pipeline), following the
 * pattern in IntersectionUtilsTest.kt's buildTJunctionFixture / RoutingUtilsTest.kt.
 */
class StreetPreviewTest {

    // ---- fixture helpers -------------------------------------------------------------------

    private fun intersectionAt(location: LngLatAlt): Intersection =
        Intersection().apply {
            this.location = location
            geometry = Point(location)
        }

    private fun way(
        name: String? = null,
        geometry: LineString,
        start: Intersection? = null,
        end: Intersection? = null,
        wayType: WayType = WayType.REGULAR,
    ): Way =
        Way().apply {
            this.name = name
            this.featureType = "highway"
            this.featureValue = "residential"
            this.geometry = geometry
            this.wayType = wayType
            if (start != null) intersections[WayEnd.START.id] = start
            if (end != null) intersections[WayEnd.END.id] = end
        }

    private fun gridStateWith(ways: List<Way>, intersections: List<Intersection>): GridState {
        val gridState = GridState()
        // getNearestFeature is normally only called from within gridState.treeContext - see
        // FileGridState/IntersectionsTestMvt.kt and IntersectionUtilsTest.kt for the same pattern
        // against real tile data.
        gridState.validateContext = false
        gridState.featureTrees[TreeId.ROADS_AND_PATHS.id] = FeatureTree(
            FeatureCollection().apply { ways.forEach { addFeature(it) } }
        )
        gridState.featureTrees[TreeId.INTERSECTIONS.id] = FeatureTree(
            FeatureCollection().apply { intersections.forEach { addFeature(it) } }
        )
        return gridState
    }

    private fun userGeometry(location: LngLatAlt, phoneHeading: Double? = null) =
        UserGeometry(location = location, phoneHeading = phoneHeading)

    /**
     * StreetPreview's internal state defaults to INITIAL even before start() is called, so a bare
     * go() call always runs the nearest-road/nearest-intersection search rather than the AT_NODE
     * junction-choice logic. Tests that want to exercise AT_NODE behaviour must first drive the
     * state machine there via a real INITIAL->AT_NODE transition, exactly as the production code
     * would - this helper does that "priming" step, snapping onto `atLocation` (which must be
     * exactly the location of a junction Intersection already wired into gridState).
     */
    private fun primeAtNode(
        sp: StreetPreview,
        gridState: GridState,
        atLocation: LngLatAlt,
        locationProvider: StaticLocationProvider,
    ) {
        sp.start()
        val primed = sp.go(userGeometry(atLocation), gridState, locationProvider, null)
        assertEquals(atLocation, primed)
    }

    // ===========================================================================================
    // start() / stop()
    // ===========================================================================================

    @Test
    fun startSetsRunningTrue() {
        val sp = StreetPreview()
        assertEquals(false, sp.running)
        sp.start()
        assertEquals(true, sp.running)
    }

    @Test
    fun stopSetsRunningFalse() {
        val sp = StreetPreview()
        sp.start()
        sp.stop()
        assertEquals(false, sp.running)
    }

    @Test
    fun startResetsStateMachineBackToInitial() {
        val fixture = buildPassThroughFixture()
        val sp = StreetPreview()
        val locationProvider = StaticLocationProvider(fixture.j1Location)

        // Drive INITIAL -> AT_NODE via a real go() call, exactly as production code would (see
        // primeAtNode's kdoc): this lands on j1 and leaves the state machine sitting AT_NODE there.
        primeAtNode(sp, fixture.gridState, fixture.j1Location, locationProvider)

        // Calling start() again should discard that progress and go back to doing a fresh
        // nearest-road/nearest-intersection search, rather than the next go() call still being
        // treated as "AT_NODE" wherever it happens to have last landed.
        sp.start()
        val southOfJ1 = getDestinationCoordinate(fixture.j1Location, 180.0, 20.0)
        val secondResult = sp.go(
            userGeometry(southOfJ1, phoneHeading = 0.0),
            fixture.gridState,
            locationProvider,
            null,
        )

        // If start() had NOT reset the state to INITIAL, this call would instead run the AT_NODE
        // branch: it looks for an INTERSECTIONS-tree feature within 1m of southOfJ1, and there is
        // none (it's 20m from j1), so it would return null. Getting j1 back instead confirms the
        // INITIAL nearest-road search actually ran again.
        assertEquals(fixture.j1.location, secondResult)
    }

    // ===========================================================================================
    // INITIAL state: snapping onto the nearest road/junction
    // ===========================================================================================

    private class ApproachFixture(
        val gridState: GridState,
        val origin: Intersection,
        val originLocation: LngLatAlt,
        val userLocation: LngLatAlt,
    )

    private fun buildApproachFixture(): ApproachFixture {
        val originLocation = LngLatAlt(-2.657, 51.430)
        val southEnd = getDestinationCoordinate(originLocation, 180.0, 30.0)
        val userLocation = getDestinationCoordinate(originLocation, 180.0, 20.0)

        val origin = intersectionAt(originLocation)
        val approachWay = way(
            name = "Approach Road",
            geometry = LineString(southEnd, originLocation),
            end = origin,
        )
        origin.members = mutableListOf(approachWay)

        val gridState = gridStateWith(listOf(approachWay), listOf(origin))
        return ApproachFixture(gridState, origin, originLocation, userLocation)
    }

    @Test
    fun initialStateSnapsToNearestIntersectionAndTransitionsToAtNode() {
        val fixture = buildApproachFixture()
        val sp = StreetPreview()
        sp.start()
        val locationProvider = StaticLocationProvider(fixture.userLocation)

        val result = sp.go(userGeometry(fixture.userLocation), fixture.gridState, locationProvider, null)

        assertEquals(fixture.originLocation, result)

        val updated = locationProvider.locationFlow.value
        assertNotNull(updated)
        assertEquals(fixture.originLocation.latitude, updated.latitude, 1e-9)
        assertEquals(fixture.originLocation.longitude, updated.longitude, 1e-9)
        assertEquals(true, updated.hasAccuracy)
        // No phoneHeading was supplied, so it defaults to 0.0.
        assertEquals(0.0f, updated.bearing)
    }

    @Test
    fun initialStateUsesPhoneHeadingAsTheInitialBearing() {
        val fixture = buildApproachFixture()
        val sp = StreetPreview()
        sp.start()
        val locationProvider = StaticLocationProvider(fixture.userLocation)

        sp.go(userGeometry(fixture.userLocation, phoneHeading = 42.0), fixture.gridState, locationProvider, null)

        assertEquals(42.0f, locationProvider.locationFlow.value?.bearing)
    }

    @Test
    fun initialStateWithNoRoadsNearbyReturnsNullAndDoesNotMove() {
        val gridState = GridState()
        gridState.validateContext = false
        val sp = StreetPreview()
        sp.start()
        val locationProvider = StaticLocationProvider(LngLatAlt(0.0, 0.0))

        val result = sp.go(userGeometry(LngLatAlt(0.0, 0.0)), gridState, locationProvider, null)

        assertNull(result)
        assertNull(locationProvider.locationFlow.value)
    }

    @Test
    fun initialStateWithRoadHavingNoMappedIntersectionsReturnsNull() {
        val origin = LngLatAlt(-2.657, 51.430)
        val end = getDestinationCoordinate(origin, 0.0, 30.0)
        // Neither end of this way is wired to an Intersection object at all.
        val lonelyWay = way(name = "Lonely Road", geometry = LineString(origin, end))
        val gridState = gridStateWith(listOf(lonelyWay), emptyList())
        val sp = StreetPreview()
        sp.start()
        val locationProvider = StaticLocationProvider(origin)

        val result = sp.go(userGeometry(origin), gridState, locationProvider, null)

        assertNull(result)
        assertNull(locationProvider.locationFlow.value)
    }

    // ===========================================================================================
    // AT_NODE state: being offered choices, and following the closest-heading one
    // ===========================================================================================

    private class PassThroughFixture(
        val gridState: GridState,
        val j1: Intersection,
        val j1Location: LngLatAlt,
        val wayNorth: Way,
        val wayContinue: Way,
        val j2: Intersection,
        val j3: Intersection,
    )

    /**
     * j1 (a real 3-way junction) --wayNorth (heading N)--> j2 (a plain 2-member pass-through node,
     * same name/class either side) --wayContinue (turns to heading E)--> j3 (a real 3-way
     * junction, where following stops).
     */
    private fun buildPassThroughFixture(): PassThroughFixture {
        val j1Location = LngLatAlt(-2.657, 51.430)
        val southEnd = getDestinationCoordinate(j1Location, 180.0, 30.0)
        val westEnd = getDestinationCoordinate(j1Location, 270.0, 30.0)
        val j2Location = getDestinationCoordinate(j1Location, 0.0, 30.0)
        val j3Location = getDestinationCoordinate(j2Location, 90.0, 30.0)

        val j1 = intersectionAt(j1Location)
        val j2 = intersectionAt(j2Location)
        val j3 = intersectionAt(j3Location)

        val wayBack = way(name = "South Approach", geometry = LineString(southEnd, j1Location), end = j1)
        val wayLeftSpur = way(name = "West Spur", geometry = LineString(j1Location, westEnd), start = j1)
        val wayNorth =
            way(name = "Test Road", geometry = LineString(j1Location, j2Location), start = j1, end = j2)
        val wayContinue =
            way(name = "Test Road", geometry = LineString(j2Location, j3Location), start = j2, end = j3)
        val wayBranch1 = way(
            name = "Branch One",
            geometry = LineString(j3Location, getDestinationCoordinate(j3Location, 45.0, 20.0)),
            start = j3,
        )
        val wayBranch2 = way(
            name = "Branch Two",
            geometry = LineString(j3Location, getDestinationCoordinate(j3Location, 135.0, 20.0)),
            start = j3,
        )

        j1.members = mutableListOf(wayBack, wayLeftSpur, wayNorth)
        j2.members = mutableListOf(wayNorth, wayContinue)
        j3.members = mutableListOf(wayContinue, wayBranch1, wayBranch2)

        val gridState = gridStateWith(
            ways = listOf(wayBack, wayLeftSpur, wayNorth, wayContinue, wayBranch1, wayBranch2),
            intersections = listOf(j1, j2, j3),
        )
        return PassThroughFixture(gridState, j1, j1Location, wayNorth, wayContinue, j2, j3)
    }

    @Test
    fun atNodePicksClosestHeadingAndFollowsThroughPassThroughNodeToNextJunction() {
        val fixture = buildPassThroughFixture()
        val sp = StreetPreview()
        val locationProvider = StaticLocationProvider(fixture.j1Location)
        primeAtNode(sp, fixture.gridState, fixture.j1Location, locationProvider)

        // Heading 2 degrees is closest to wayNorth (heading ~0) rather than the south approach
        // (~180) or the west spur (~270), so wayNorth is chosen.
        val result = sp.go(
            userGeometry(fixture.j1Location, phoneHeading = 2.0),
            fixture.gridState,
            locationProvider,
            null,
        )

        // j2 is a plain 2-member pass-through node with matching name/class either side, so
        // followWays should walk straight through it and only stop at j3, a real 3-member
        // junction.
        assertEquals(fixture.j3.location, result)

        val updated = locationProvider.locationFlow.value
        assertNotNull(updated)
        assertEquals(fixture.j3.location.latitude, updated.latitude, 1e-9)
        assertEquals(fixture.j3.location.longitude, updated.longitude, 1e-9)
        assertEquals(1.0f, updated.speed)
    }

    @Test
    fun atNodeLastHeadingIsComputedFromTheLastWayActuallyFollowed() {
        // When the followed path bends through a pass-through junction (as above, where
        // wayNorth heads due north out of j1 and wayContinue then turns to head due east out of
        // j2), go() must compute `lastHeading` from the LAST way segment actually walked to
        // reach the returned node (wayContinue), not from `road.way` - the FIRST way segment
        // chosen at the original junction (wayNorth). Way.heading(x) doesn't validate that `x`
        // is actually one of the way's own two endpoints, so using the wrong way would silently
        // return a heading derived from the wrong segment's geometry even though j3 was reached
        // via wayContinue, not wayNorth.
        val fixture = buildPassThroughFixture()
        val sp = StreetPreview()
        val locationProvider = StaticLocationProvider(fixture.j1Location)
        primeAtNode(sp, fixture.gridState, fixture.j1Location, locationProvider)

        sp.go(userGeometry(fixture.j1Location, phoneHeading = 2.0), fixture.gridState, locationProvider, null)

        val actualLastHeading = sp.getLastHeading()
        val firstWayBasedHeading = (fixture.wayNorth.heading(fixture.j3) + 180.0) % 360.0
        val lastWayBasedHeading = (fixture.wayContinue.heading(fixture.j3) + 180.0) % 360.0

        assertEquals(lastWayBasedHeading, actualLastHeading, 0.0001)
        // The two differ by roughly a right angle here (wayNorth points north out of j1,
        // wayContinue points east out of j2) - confirming the heading reflects the direction
        // actually travelled on the last leg of the move, not the first.
        assertTrue(abs(actualLastHeading - firstWayBasedHeading) > 30.0)
    }

    private class NameChangeFixture(
        val gridState: GridState,
        val j1Location: LngLatAlt,
        val jMid: Intersection,
    )

    private fun buildNameChangeFixture(): NameChangeFixture {
        val j1Location = LngLatAlt(-2.657, 51.430)
        val southEnd = getDestinationCoordinate(j1Location, 180.0, 30.0)
        val jMidLocation = getDestinationCoordinate(j1Location, 0.0, 30.0)
        val farEnd = getDestinationCoordinate(jMidLocation, 0.0, 30.0)

        val j1 = intersectionAt(j1Location)
        val jMid = intersectionAt(jMidLocation)

        val wayBack = way(name = "South Approach", geometry = LineString(southEnd, j1Location), end = j1)
        val wayX =
            way(name = "First Street", geometry = LineString(j1Location, jMidLocation), start = j1, end = jMid)
        val wayY =
            way(name = "Second Street", geometry = LineString(jMidLocation, farEnd), start = jMid)

        j1.members = mutableListOf(wayBack, wayX)
        jMid.members = mutableListOf(wayX, wayY)

        val gridState = gridStateWith(listOf(wayBack, wayX, wayY), listOf(j1, jMid))
        return NameChangeFixture(gridState, j1Location, jMid)
    }

    @Test
    fun atNodeStopsAtPassThroughNodeWhenTheRoadNameChanges() {
        val fixture = buildNameChangeFixture()
        val sp = StreetPreview()
        val locationProvider = StaticLocationProvider(fixture.j1Location)
        primeAtNode(sp, fixture.gridState, fixture.j1Location, locationProvider)

        val result =
            sp.go(userGeometry(fixture.j1Location, phoneHeading = 0.0), fixture.gridState, locationProvider, null)

        // jMid only has 2 members - geometrically a plain pass-through node - but the road name
        // changes there ("First Street" -> "Second Street"), so followWays' stop-predicate should
        // halt right at jMid rather than continuing onto wayY/"Second Street".
        assertEquals(fixture.jMid.location, result)
    }

    private class JoinerFixture(
        val gridState: GridState,
        val j1Location: LngLatAlt,
        val jMid2: Intersection,
        val jEnd: Intersection,
    )

    /**
     * j1 --wayP ("Road P")--> jMid2 --wayJoinerOrRegular (unnamed)--> jMid3 --wayT ("Road T")-->
     * jEnd (a real 3-way junction). When the middle way is a WayType.JOINER, StreetPreview's
     * follow-predicate should bypass the name-change check both for it and for the way straight
     * after it, sailing through to jEnd despite the name changing twice. When it's a plain
     * REGULAR way instead, the first name change (wayP -> unnamed) should stop the walk at jMid2.
     */
    private fun buildJoinerFixture(useJoiner: Boolean): JoinerFixture {
        val j1Location = LngLatAlt(-2.657, 51.430)
        val southEnd = getDestinationCoordinate(j1Location, 180.0, 30.0)
        val jMid2Location = getDestinationCoordinate(j1Location, 0.0, 30.0)
        val jMid3Location = getDestinationCoordinate(jMid2Location, 0.0, 30.0)
        val jEndLocation = getDestinationCoordinate(jMid3Location, 0.0, 30.0)
        val branch1 = getDestinationCoordinate(jEndLocation, 45.0, 20.0)
        val branch2 = getDestinationCoordinate(jEndLocation, 135.0, 20.0)

        val j1 = intersectionAt(j1Location)
        val jMid2 = intersectionAt(jMid2Location)
        val jMid3 = intersectionAt(jMid3Location)
        val jEnd = intersectionAt(jEndLocation)

        val wayBack = way(name = "South Approach", geometry = LineString(southEnd, j1Location), end = j1)
        val wayP =
            way(name = "Road P", geometry = LineString(j1Location, jMid2Location), start = j1, end = jMid2)
        val wayJoinerOrRegular = way(
            name = null,
            geometry = LineString(jMid2Location, jMid3Location),
            start = jMid2,
            end = jMid3,
            wayType = if (useJoiner) WayType.JOINER else WayType.REGULAR,
        )
        val wayT = way(
            name = "Road T",
            geometry = LineString(jMid3Location, jEndLocation),
            start = jMid3,
            end = jEnd,
        )
        val wayR = way(name = "Road R", geometry = LineString(jEndLocation, branch1), start = jEnd)
        val wayS = way(name = "Road S", geometry = LineString(jEndLocation, branch2), start = jEnd)

        j1.members = mutableListOf(wayBack, wayP)
        jMid2.members = mutableListOf(wayP, wayJoinerOrRegular)
        jMid3.members = mutableListOf(wayJoinerOrRegular, wayT)
        jEnd.members = mutableListOf(wayT, wayR, wayS)

        val gridState = gridStateWith(
            listOf(wayBack, wayP, wayJoinerOrRegular, wayT, wayR, wayS),
            listOf(j1, jMid2, jMid3, jEnd),
        )
        return JoinerFixture(gridState, j1Location, jMid2, jEnd)
    }

    @Test
    fun atNodeJoinerWayBypassesNameChangeCheckAndReachesTheRealJunction() {
        val fixture = buildJoinerFixture(useJoiner = true)
        val sp = StreetPreview()
        val locationProvider = StaticLocationProvider(fixture.j1Location)
        primeAtNode(sp, fixture.gridState, fixture.j1Location, locationProvider)

        val result =
            sp.go(userGeometry(fixture.j1Location, phoneHeading = 0.0), fixture.gridState, locationProvider, null)

        // "Road P" -> unnamed JOINER -> "Road T": despite the name changing twice, a JOINER way is
        // always walked through, and the custom stop-predicate explicitly skips the
        // name/class/brunnel comparison whenever the *previous* way was a JOINER - so this should
        // sail past both intermediate pass-through nodes and stop only at the real junction, jEnd.
        assertEquals(fixture.jEnd.location, result)
    }

    @Test
    fun atNodeWithoutJoinerStopsAtTheFirstNameChangeInstead() {
        // Same shape as above but the middle way is a plain unnamed REGULAR way rather than a
        // JOINER - contrast case proving it really is the JOINER wayType that lets the previous
        // test sail through the name changes, not some other effect.
        val fixture = buildJoinerFixture(useJoiner = false)
        val sp = StreetPreview()
        val locationProvider = StaticLocationProvider(fixture.j1Location)
        primeAtNode(sp, fixture.gridState, fixture.j1Location, locationProvider)

        val result =
            sp.go(userGeometry(fixture.j1Location, phoneHeading = 0.0), fixture.gridState, locationProvider, null)

        assertEquals(fixture.jMid2.location, result)
        assertNotEquals(fixture.jEnd.location, result)
    }

    private class DeadEndFixture(
        val gridState: GridState,
        val j1Location: LngLatAlt,
        val wayToDeadEnd: Way,
        val jDead: Intersection,
    )

    private fun buildDeadEndFixture(): DeadEndFixture {
        val j1Location = LngLatAlt(-2.657, 51.430)
        val southEnd = getDestinationCoordinate(j1Location, 180.0, 30.0)
        val jDeadLocation = getDestinationCoordinate(j1Location, 0.0, 25.0)

        val j1 = intersectionAt(j1Location)
        val jDead = intersectionAt(jDeadLocation)

        val wayBack = way(name = "South Approach", geometry = LineString(southEnd, j1Location), end = j1)
        val wayToDeadEnd = way(
            name = "Dead End Lane",
            geometry = LineString(j1Location, jDeadLocation),
            start = j1,
            end = jDead,
        )

        j1.members = mutableListOf(wayBack, wayToDeadEnd)
        jDead.members = mutableListOf(wayToDeadEnd)

        val gridState = gridStateWith(listOf(wayBack, wayToDeadEnd), listOf(j1, jDead))
        return DeadEndFixture(gridState, j1Location, wayToDeadEnd, jDead)
    }

    @Test
    fun atNodeMovesToALiteralDeadEndNodeSuccessfully() {
        val fixture = buildDeadEndFixture()
        val sp = StreetPreview()
        val locationProvider = StaticLocationProvider(fixture.j1Location)
        primeAtNode(sp, fixture.gridState, fixture.j1Location, locationProvider)

        val result =
            sp.go(userGeometry(fixture.j1Location, phoneHeading = 0.0), fixture.gridState, locationProvider, null)

        // The dead end is still a real, mapped Intersection object (just with a single member
        // way), so reaching it is a successful move, not a "can't go anywhere" null result.
        assertEquals(fixture.jDead.location, result)
        val expectedHeading = (fixture.wayToDeadEnd.heading(fixture.jDead) + 180.0) % 360.0
        assertEquals(expectedHeading, sp.getLastHeading(), 0.0001)
    }

    private class OpenEndFixture(val gridState: GridState, val j1Location: LngLatAlt)

    private fun buildOpenEndFixture(): OpenEndFixture {
        val j1Location = LngLatAlt(-2.657, 51.430)
        val southEnd = getDestinationCoordinate(j1Location, 180.0, 30.0)
        val openEnd = getDestinationCoordinate(j1Location, 0.0, 25.0)

        val j1 = intersectionAt(j1Location)

        val wayBack = way(name = "South Approach", geometry = LineString(southEnd, j1Location), end = j1)
        // wayOpen's far end is never wired to an Intersection object at all - the road just isn't
        // mapped any further, unlike buildDeadEndFixture's jDead, which IS a mapped node.
        val wayOpen = way(name = "Unmapped Lane", geometry = LineString(j1Location, openEnd), start = j1)

        j1.members = mutableListOf(wayBack, wayOpen)

        val gridState = gridStateWith(listOf(wayBack, wayOpen), listOf(j1))
        return OpenEndFixture(gridState, j1Location)
    }

    @Test
    fun atNodeReturnsNullWhenTheChosenWayHasNoIntersectionAtItsFarEnd() {
        val fixture = buildOpenEndFixture()
        val sp = StreetPreview()
        val locationProvider = StaticLocationProvider(fixture.j1Location)
        primeAtNode(sp, fixture.gridState, fixture.j1Location, locationProvider)

        val result =
            sp.go(userGeometry(fixture.j1Location, phoneHeading = 0.0), fixture.gridState, locationProvider, null)

        assertNull(result)
    }

    @Test
    fun atNodeReturnsNullWhenNoIntersectionIsNearTheGivenLocation() {
        val fixture = buildPassThroughFixture()
        val sp = StreetPreview()
        val locationProvider = StaticLocationProvider(fixture.j1Location)
        primeAtNode(sp, fixture.gridState, fixture.j1Location, locationProvider)
        val farAway = getDestinationCoordinate(fixture.j1Location, 0.0, 500.0)

        val result = sp.go(userGeometry(farAway, phoneHeading = 0.0), fixture.gridState, locationProvider, null)

        assertNull(result)
    }

    @Test
    fun atNodeReturnsNullWhenNoHeadingIsAvailable() {
        val fixture = buildPassThroughFixture()
        val sp = StreetPreview()
        val locationProvider = StaticLocationProvider(fixture.j1Location)
        primeAtNode(sp, fixture.gridState, fixture.j1Location, locationProvider)

        // No phoneHeading, and no travel/head heading either, so UserGeometry.heading() is null -
        // go() can't pick a best choice even though choices exist at j1.
        val result = sp.go(
            userGeometry(fixture.j1Location, phoneHeading = null),
            fixture.gridState,
            locationProvider,
            null,
        )

        assertNull(result)
    }

    // ===========================================================================================
    // getDirectionChoices
    // ===========================================================================================

    @Test
    fun getDirectionChoicesReturnsOneChoicePerMemberWithMatchingWayAndHeading() {
        val fixture = buildPassThroughFixture()
        val sp = StreetPreview()

        val choices = sp.getDirectionChoices(fixture.gridState, fixture.j1Location, null)

        assertEquals(3, choices.size)
        for (choice in choices) {
            assertEquals(choice.way.name, choice.name)
            assertEquals(choice.way.heading(fixture.j1), choice.heading, 0.0001)
        }
        assertTrue(choices.any { it.way === fixture.wayNorth })
    }

    @Test
    fun getDirectionChoicesReturnsEmptyListWhenNoIntersectionIsNearby() {
        val fixture = buildPassThroughFixture()
        val sp = StreetPreview()
        val farAway = getDestinationCoordinate(fixture.j1Location, 0.0, 500.0)

        val choices = sp.getDirectionChoices(fixture.gridState, farAway, null)

        assertTrue(choices.isEmpty())
    }

    // ===========================================================================================
    // updateBestChoice / resetBestChoice
    // ===========================================================================================

    private fun makeChoice(heading: Double, name: String): StreetPreviewChoice {
        val w = Way().apply { this.name = name }
        return StreetPreviewChoice(heading = heading, name = name, way = w)
    }

    @Test
    fun updateBestChoiceReturnsTheClosestHeadingMatchThenNullWhenUnchanged() {
        val choiceA = makeChoice(heading = 0.0, name = "Road A")
        val choiceB = makeChoice(heading = 180.0, name = "Road B")
        val sp = StreetPreview()

        val first = sp.updateBestChoice(listOf(choiceA, choiceB), heading = 5.0)
        assertEquals(choiceA, first)

        // Same heading, same resulting best choice -> reported as "no change" (null).
        val second = sp.updateBestChoice(listOf(choiceA, choiceB), heading = 5.0)
        assertNull(second)

        // After a reset, the same best choice is reported again rather than suppressed.
        sp.resetBestChoice()
        val third = sp.updateBestChoice(listOf(choiceA, choiceB), heading = 5.0)
        assertEquals(choiceA, third)
    }

    @Test
    fun updateBestChoiceReportsAChangeWhenHeadingSwitchesToFavourAnotherChoice() {
        val choiceA = makeChoice(heading = 0.0, name = "Road A")
        val choiceB = makeChoice(heading = 180.0, name = "Road B")
        val sp = StreetPreview()

        sp.updateBestChoice(listOf(choiceA, choiceB), heading = 5.0)
        val switched = sp.updateBestChoice(listOf(choiceA, choiceB), heading = 175.0)

        assertEquals(choiceB, switched)
    }

    @Test
    fun updateBestChoiceReturnsNullForEmptyChoiceList() {
        val sp = StreetPreview()
        assertNull(sp.updateBestChoice(emptyList(), heading = 0.0))
    }
}
