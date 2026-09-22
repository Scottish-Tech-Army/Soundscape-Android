package org.scottishtecharmy.soundscape

import org.junit.Assert
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Intersection
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayEnd
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import kotlin.test.Test

/**
 * Way.isSidewalkConnector decides whether a short unnamed stub joining a road to its own pavement
 * should be discounted when judging whether an intersection is worth announcing
 * (IntersectionUtils' disposalCount). The topology is built by hand here rather than taken from a
 * tile fixture, because the thing under test is one boolean about the far end of the stub, and a
 * fixture would leave it unclear which of the surrounding heuristics produced the answer.
 */
class SidewalkConnectorTest {

    private fun way(
        name: String? = null,
        from: LngLatAlt,
        to: LngLatAlt,
        sidewalk: Boolean = false,
        pavementOf: String? = null,
    ) = Way().also {
        it.name = name
        it.geometry = LineString(from, to)
        it.length = 10.0
        if (sidewalk) it.setProperty("footway", "sidewalk")
        pavementOf?.let { road -> it.setProperty("pavement", road) }
    }

    private fun join(intersection: Intersection, at: LngLatAlt, vararg ways: Pair<Way, WayEnd>) {
        intersection.location = at
        for ((way, end) in ways) {
            way.intersections[end.id] = intersection
            intersection.members.add(way)
        }
    }

    /**
     * Road, its separately drawn pavement, and an unnamed stub joining the two - the shape at
     * https://www.openstreetmap.org/way/958596881, which the function's own comment cites.
     */
    @Test
    fun stubJoiningARoadToItsOwnPavementIsAConnector() {
        val gridState = GridState().also { it.validateContext = false }
        val onRoad = LngLatAlt(-4.3174, 55.9397)
        val onPavement = LngLatAlt(-4.3174, 55.93975)

        val road = way("Station Road", LngLatAlt(-4.3180, 55.9397), LngLatAlt(-4.3168, 55.9397))
        val stub = way(from = onRoad, to = onPavement, sidewalk = true)
        val pavement = way(
            from = LngLatAlt(-4.3180, 55.93975), to = LngLatAlt(-4.3168, 55.93975),
            sidewalk = true, pavementOf = "Station Road"
        )

        val roadEnd = Intersection()
        join(roadEnd, onRoad, road to WayEnd.END, stub to WayEnd.START)
        val pavementEnd = Intersection()
        join(pavementEnd, onPavement, stub to WayEnd.END, pavement to WayEnd.START)

        Assert.assertTrue(stub.isSidewalkConnector(roadEnd, road, gridState, null))
    }

    /**
     * The same stub, but the far end carries on into a named road rather than stopping at the
     * pavement. It goes somewhere, so it is a real arm of the junction and must not be discounted.
     */
    @Test
    fun stubReachingAnotherRoadIsNotAConnector() {
        val gridState = GridState().also { it.validateContext = false }
        val onRoad = LngLatAlt(-4.3174, 55.9397)
        val farEnd = LngLatAlt(-4.3174, 55.93975)

        val road = way("Station Road", LngLatAlt(-4.3180, 55.9397), LngLatAlt(-4.3168, 55.9397))
        val stub = way(from = onRoad, to = farEnd, sidewalk = true)
        val otherRoad = way("Milngavie Road", LngLatAlt(-4.3180, 55.93975), LngLatAlt(-4.3168, 55.93975))

        val roadEnd = Intersection()
        join(roadEnd, onRoad, road to WayEnd.END, stub to WayEnd.START)
        val other = Intersection()
        join(other, farEnd, stub to WayEnd.END, otherRoad to WayEnd.START)

        Assert.assertFalse(stub.isSidewalkConnector(roadEnd, road, gridState, null))
    }

    /** A pavement belonging to a different road is not this road's connector. */
    @Test
    fun stubReachingSomeOtherRoadsPavementIsNotAConnector() {
        val gridState = GridState().also { it.validateContext = false }
        val onRoad = LngLatAlt(-4.3174, 55.9397)
        val onPavement = LngLatAlt(-4.3174, 55.93975)

        val road = way("Station Road", LngLatAlt(-4.3180, 55.9397), LngLatAlt(-4.3168, 55.9397))
        val stub = way(from = onRoad, to = onPavement, sidewalk = true)
        val pavement = way(
            from = LngLatAlt(-4.3180, 55.93975), to = LngLatAlt(-4.3168, 55.93975),
            sidewalk = true, pavementOf = "Milngavie Road"
        )

        val roadEnd = Intersection()
        join(roadEnd, onRoad, road to WayEnd.END, stub to WayEnd.START)
        val pavementEnd = Intersection()
        join(pavementEnd, onPavement, stub to WayEnd.END, pavement to WayEnd.START)

        Assert.assertFalse(stub.isSidewalkConnector(roadEnd, road, gridState, null))
    }
}
