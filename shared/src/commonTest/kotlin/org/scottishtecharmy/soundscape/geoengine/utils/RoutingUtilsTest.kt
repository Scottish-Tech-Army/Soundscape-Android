package org.scottishtecharmy.soundscape.geoengine.utils

import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Intersection
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayEnd
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.createCheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoutingUtilsTest {

    private fun intersection(lng: Double, lat: Double): Intersection =
        Intersection().apply { location = LngLatAlt(lng, lat) }

    private fun connect(a: Intersection, b: Intersection, length: Double): Way {
        val way = Way().apply {
            this.length = length
            intersections[WayEnd.START.id] = a
            intersections[WayEnd.END.id] = b
        }
        a.members.add(way)
        b.members.add(way)
        return way
    }

    // --- dijkstraOnWaysWithLoops ---

    @Test
    fun sumsWeightsAlongOnlyAvailablePath() {
        val a = intersection(0.0, 0.0)
        val b = intersection(0.0, 0.001)
        val c = intersection(0.0, 0.002)

        connect(a, b, 1.0)
        connect(b, c, 1.0)

        val ruler = LngLatAlt(0.0, 0.0).createCheapRuler()
        val distance = dijkstraOnWaysWithLoops(a, c, ruler)

        assertEquals(2.0, distance, 0.0001)
    }

    /**
     * Pins down a fix for a genuine bug that used to be in [dijkstraOnWaysWithLoops]: it used to
     * return as soon as it *relaxed* an edge reaching `end`, rather than only once `end` is popped
     * off the priority queue with a guaranteed-minimal distance. Here `a` has both a short path via
     * `b` (1.0 + 1.0 = 2.0) and a longer direct edge straight to `c` (5.0), with `a`'s edges visited
     * in insertion order so the direct a-c edge is relaxed before the search explores onward from
     * `b`. All three intersections share one coordinate so the priority queue's straight-line
     * heuristic to `end` is zero everywhere and ordering is driven purely by accumulated distance -
     * isolating this from the separate, real-usage assumption that `way.length` and geographic
     * distance are on comparable scales (not true of these arbitrary test weights).
     */
    @Test
    fun findsShorterPathEvenWhenLongerDirectEdgeIsRelaxedFirst() {
        val a = intersection(0.0, 0.0)
        val b = intersection(0.0, 0.0)
        val c = intersection(0.0, 0.0)

        connect(a, b, 1.0)
        connect(b, c, 1.0)
        connect(a, c, 5.0) // Direct edge is longer than going via b.

        val ruler = LngLatAlt(0.0, 0.0).createCheapRuler()
        val distance = dijkstraOnWaysWithLoops(a, c, ruler)

        // The true shortest distance is 2.0 (a -> b -> c), not the longer direct edge, even though
        // the direct edge is relaxed before the shorter path is found.
        assertEquals(2.0, distance, 0.0001)
    }

    @Test
    fun usesDirectEdgeWhenItIsShortest() {
        val a = intersection(0.0, 0.0)
        val b = intersection(0.0, 0.001)
        val c = intersection(0.0, 0.002)

        connect(a, b, 10.0)
        connect(b, c, 10.0)
        connect(a, c, 3.0) // Direct edge is now the shortest

        val ruler = LngLatAlt(0.0, 0.0).createCheapRuler()
        val distance = dijkstraOnWaysWithLoops(a, c, ruler)

        assertEquals(3.0, distance, 0.0001)
    }

    @Test
    fun returnsMaxValueWhenNoPathExists() {
        val a = intersection(0.0, 0.0)
        val isolated = intersection(1.0, 1.0) // Not connected to anything

        val ruler = LngLatAlt(0.0, 0.0).createCheapRuler()
        val distance = dijkstraOnWaysWithLoops(a, isolated, ruler)

        assertEquals(Double.MAX_VALUE, distance)
    }

    // --- getPathWays ---

    @Test
    fun getPathWaysReturnsWaysFromEndToStart() {
        val a = intersection(0.0, 0.0)
        val b = intersection(0.0, 0.001)
        val c = intersection(0.0, 0.002)

        val wayAB = connect(a, b, 1.0)
        val wayBC = connect(b, c, 1.0)

        val ruler = LngLatAlt(0.0, 0.0).createCheapRuler()
        dijkstraOnWaysWithLoops(a, c, ruler)

        val path = getPathWays(c)

        // getPathWays walks backwards from the end node, so the way nearest the end comes first
        assertEquals(listOf(wayBC, wayAB), path)
    }

    @Test
    fun getPathWaysOnUnreachedNodeReturnsEmptyList() {
        val a = intersection(0.0, 0.0)
        val isolated = intersection(1.0, 1.0)

        val ruler = LngLatAlt(0.0, 0.0).createCheapRuler()
        dijkstraOnWaysWithLoops(a, isolated, ruler)

        assertTrue(getPathWays(isolated).isEmpty())
    }

    // --- findShortestDistance / ShortestDistanceResults.tidy ---

    @Test
    fun findShortestDistanceRoutesThroughSharedIntersectionAndTidyRestoresGraph() {
        val pointA = LngLatAlt(-4.0, 55.0)
        val pointB = LngLatAlt(-4.0, 55.001)
        val pointC = LngLatAlt(-4.0, 55.002)

        val intA = intersection(pointA.longitude, pointA.latitude)
        val intB = intersection(pointB.longitude, pointB.latitude)
        val intC = intersection(pointC.longitude, pointC.latitude)

        val ruler = pointA.createCheapRuler()

        val wayAB = Way().apply {
            geometry = LineString(pointA, pointB)
            length = ruler.distance(pointA, pointB)
            intersections[WayEnd.START.id] = intA
            intersections[WayEnd.END.id] = intB
        }
        intA.members.add(wayAB)
        intB.members.add(wayAB)

        val wayBC = Way().apply {
            geometry = LineString(pointB, pointC)
            length = ruler.distance(pointB, pointC)
            intersections[WayEnd.START.id] = intB
            intersections[WayEnd.END.id] = intC
        }
        intB.members.add(wayBC)
        intC.members.add(wayBC)

        // Start half way along AB, end half way along BC - the only connecting point is intB.
        val startLocation = LngLatAlt(
            (pointA.longitude + pointB.longitude) / 2,
            (pointA.latitude + pointB.latitude) / 2,
        )
        val endLocation = LngLatAlt(
            (pointB.longitude + pointC.longitude) / 2,
            (pointB.latitude + pointC.latitude) / 2,
        )

        val expectedDistance =
            ruler.distance(startLocation, pointB) + ruler.distance(pointB, endLocation)

        val debugFeatureCollection = FeatureCollection()
        val results = findShortestDistance(
            startLocation = startLocation,
            startWay = wayAB,
            endLocation = endLocation,
            endWay = wayBC,
            endIntersection = null,
            debugFeatureCollection = debugFeatureCollection,
        )

        assertEquals(expectedDistance, results.distance, 0.01)
        assertTrue(debugFeatureCollection.features.isNotEmpty())

        results.tidy()

        // After tidy(), the temporary intersections/ways created for routing should have been
        // removed, restoring the original graph structure at intA/intB/intC.
        assertEquals(listOf(wayAB), intA.members)
        assertEquals(setOf(wayAB, wayBC), intB.members.toSet())
        assertEquals(2, intB.members.size)
        assertEquals(listOf(wayBC), intC.members)
    }

    @Test
    fun findShortestDistanceToExistingIntersection() {
        val pointA = LngLatAlt(-4.0, 55.0)
        val pointB = LngLatAlt(-4.0, 55.001)

        val intA = intersection(pointA.longitude, pointA.latitude)
        val intB = intersection(pointB.longitude, pointB.latitude)

        val ruler = pointA.createCheapRuler()

        val wayAB = Way().apply {
            geometry = LineString(pointA, pointB)
            length = ruler.distance(pointA, pointB)
            intersections[WayEnd.START.id] = intA
            intersections[WayEnd.END.id] = intB
        }
        intA.members.add(wayAB)
        intB.members.add(wayAB)
        // Give intB a second member so it isn't pruned by the <=2 members rule elsewhere - not
        // required here, but keeps this closer to a realistic intersection.
        val otherWay = Way().apply { length = 1.0 }
        intB.members.add(otherWay)

        val startLocation = LngLatAlt(
            (pointA.longitude + pointB.longitude) / 2,
            (pointA.latitude + pointB.latitude) / 2,
        )

        val expectedDistance = ruler.distance(startLocation, pointB)

        val results = findShortestDistance(
            startLocation = startLocation,
            startWay = wayAB,
            endLocation = null,
            endWay = null,
            endIntersection = intB,
            debugFeatureCollection = null,
        )

        assertEquals(expectedDistance, results.distance, 0.01)
        assertEquals(intB, results.endIntersection)
    }
}
