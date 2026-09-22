package org.scottishtecharmy.soundscape

import org.junit.Assert
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Intersection
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayEnd
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayType
import org.scottishtecharmy.soundscape.geoengine.utils.getDestinationCoordinate
import org.scottishtecharmy.soundscape.geoengine.utils.halfWidth
import org.scottishtecharmy.soundscape.geoengine.utils.setbackAlong
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import kotlin.math.sin
import kotlin.test.Test

/**
 * The kerb setback is a formula, so it is tested as one: junctions are built by hand at chosen
 * angles rather than taken from tile fixtures, because a fixture would leave it unclear whether
 * an answer came from the geometry or from one of the heuristics around it.
 */
class JunctionExtentTest {

    private val node = LngLatAlt(-4.3174, 55.9397)

    private fun gridState() = GridState().also { it.validateContext = false }

    /** An arm leaving [node] on [heading], of the given tile-schema class. */
    private fun arm(
        heading: Double,
        roadClass: String = "minor",
        name: String? = null,
        footway: String? = null,
        junction: String? = null,
    ) = Way().also {
        it.name = name
        it.featureClass = roadClass
        it.featureType = "highway"
        it.length = 100.0
        it.geometry = LineString(node, getDestinationCoordinate(node, heading, 100.0))
        footway?.let { f -> it.setProperty("footway", f) }
        junction?.let { j -> it.setProperty("junction", j) }
    }

    private fun junctionOf(vararg arms: Way) = Intersection().also { intersection ->
        intersection.location = node
        for (a in arms) {
            a.intersections[WayEnd.START.id] = intersection
            intersection.members.add(a)
        }
    }

    /**
     * The kerb line of a crossing road is parallel to it, so an approach meeting it at angle theta
     * reaches it halfWidth/sin(theta) before the node.
     */
    @Test
    fun setbackFollowsOneOverSineOfTheAngle() {
        for (angle in listOf(90.0, 60.0, 45.0, 30.0)) {
            val approach = arm(180.0)
            val crossing = arm(angle, roadClass = "primary")
            val junction = junctionOf(approach, crossing)

            val expected = crossing.halfWidth() / sin(Math.toRadians(angle))
            Assert.assertEquals(
                "setback at $angle degrees",
                expected,
                junction.setbackAlong(approach, gridState(), null),
                0.01
            )
        }
    }

    /** A wider crossing road sets the kerb further back. */
    @Test
    fun setbackScalesWithTheCrossingRoadsClass() {
        val minor = junctionOf(arm(180.0), arm(90.0, roadClass = "minor"))
        val primary = junctionOf(arm(180.0), arm(90.0, roadClass = "primary"))

        val minorSetback = minor.setbackAlong(minor.members[0], gridState(), null)
        val primarySetback = primary.setbackAlong(primary.members[0], gridState(), null)
        Assert.assertTrue(
            "primary $primarySetback should exceed minor $minorSetback",
            primarySetback > minorSetback
        )
    }

    /** The widest arm decides, not the first or the last. */
    @Test
    fun theWidestArmDecides() {
        val approach = arm(180.0)
        val junction = junctionOf(approach, arm(90.0, "minor"), arm(270.0, "primary"))
        val widest = junctionOf(arm(180.0), arm(270.0, "primary"))

        Assert.assertEquals(
            widest.setbackAlong(widest.members[0], gridState(), null),
            junction.setbackAlong(approach, gridState(), null),
            0.01
        )
    }

    /**
     * A shallow arm is a fork or a merge, not something to stop at the kerb of - and an unclamped
     * 1/sin would run away to infinity as it approaches parallel.
     */
    @Test
    fun aNearlyParallelArmIsNotACrossing() {
        val approach = arm(180.0)
        val junction = junctionOf(approach, arm(10.0, roadClass = "primary"))
        Assert.assertEquals(0.0, junction.setbackAlong(approach, gridState(), null), 0.001)
    }

    @Test
    fun setbackIsCappedForShallowButCrossingArms() {
        val approach = arm(180.0)
        val crossing = arm(22.0, roadClass = "motorway")
        val junction = junctionOf(approach, crossing)

        val setback = junction.setbackAlong(approach, gridState(), null)
        Assert.assertTrue("uncapped setback $setback", setback <= 15.0)
        Assert.assertTrue("setback $setback should be positive", setback > 0.0)
    }

    /** Pavements, crossings and tile-edge joiners are not arms of a junction. */
    @Test
    fun pavementsAndJoinersAreNotArms() {
        val approach = arm(180.0)
        val pavement = arm(90.0, roadClass = "path", footway = "sidewalk")
        val crossing = arm(270.0, roadClass = "path", footway = "crossing")
        val joiner = arm(45.0, roadClass = "primary").also { it.wayType = WayType.JOINER }

        val junction = junctionOf(approach, pavement, crossing, joiner)
        Assert.assertEquals(0.0, junction.setbackAlong(approach, gridState(), null), 0.001)
        Assert.assertEquals(0.0, joiner.halfWidth(), 0.001)
    }

    /**
     * A roundabout's arms meet a ring rather than each other, and a pedestrian arrives at the
     * give-way line. The model does not describe that, so it declines to guess.
     */
    @Test
    fun roundaboutsGetNoSetback() {
        val approach = arm(180.0)
        val ring = arm(90.0, roadClass = "primary", junction = "roundabout")
        val junction = junctionOf(approach, ring)
        Assert.assertEquals(0.0, junction.setbackAlong(approach, gridState(), null), 0.001)
    }

    /** A driveway is not as wide as the service-road default. */
    @Test
    fun drivewaysAreNarrowerThanServiceRoads() {
        val service = arm(90.0, roadClass = "service")
        val driveway = arm(90.0, roadClass = "service").also { it.setProperty("service", "driveway") }
        Assert.assertTrue(driveway.halfWidth() < service.halfWidth())
    }

    /** An unknown class still yields a usable estimate rather than zero or a crash. */
    @Test
    fun anUnknownClassFallsBackToADefault() {
        val unknown = arm(90.0, roadClass = "something_new")
        Assert.assertTrue(unknown.halfWidth() > 0.0)
    }
}
