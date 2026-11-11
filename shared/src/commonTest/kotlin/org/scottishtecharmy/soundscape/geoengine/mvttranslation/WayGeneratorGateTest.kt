package org.scottishtecharmy.soundscape.geoengine.mvttranslation

import org.scottishtecharmy.soundscape.geoengine.utils.dijkstraOnWaysWithLoops
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.createCheapRuler
import org.scottishtecharmy.soundscape.geoengine.utils.traverseIntersectionsConfectingNames
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import org.scottishtecharmy.soundscape.i18n.PluralKey
import org.scottishtecharmy.soundscape.i18n.StringKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Encodes (key, args) as "Key(args)" so a test can see which template was chosen */
private class GateFakeLocalizedStrings : LocalizedStrings {
    override fun get(key: StringKey, vararg args: Any?): String =
        if (args.isEmpty()) key.name else "${key.name}(${args.joinToString(",")})"

    override fun getOrNull(key: StringKey, vararg args: Any?): String? = get(key, *args)
    override fun getPlural(key: PluralKey, quantity: Int, vararg args: Any?): String = key.name
    override fun resolveFeatureClass(key: String): String? = null
}

/**
 * Gates which can't be got through end the Way at the gate, which is what stops the name
 * confection, the junction walk and Dijkstra from going through them. These tests drive
 * WayGenerator as MvtToGeoJson does, with lines in tile coordinates.
 *
 * At zoom 15 and latitude 56 one tile sample is about 0.17m, so 300 samples is about 50m.
 */
class WayGeneratorGateTest {

    private val tileX = 15991
    private val tileY = 10170
    private val tileZoom = 15
    private val strings = GateFakeLocalizedStrings()

    private class Graph(
        val ways: FeatureCollection,
        val intersections: HashMap<LngLatAlt, Intersection>
    )

    private fun line(vararg points: Pair<Int, Int>) = arrayListOf(*points)

    private fun build(
        lines: List<Pair<String?, ArrayList<Pair<Int, Int>>>>,
        gates: List<Pair<Pair<Int, Int>, GateKind>> = emptyList()
    ): Graph {
        val generator = WayGenerator()
        for ((name, points) in lines) {
            generator.addLine(points)
            generator.addFeature(MvtFeature().apply {
                this.name = name
                featureClass = if (name == null) "service" else "minor"
                featureType = "highway"
                featureValue = if (name == null) "service" else "residential"
                properties = hashMapOf()
                geometry = LineString(convertGeometry(tileX, tileY, tileZoom, points))
            })
        }
        for ((point, kind) in gates)
            generator.addBarrier(point, kind)

        val ways = FeatureCollection()
        val intersectionMap = hashMapOf<LngLatAlt, Intersection>()
        generator.generateWays(
            FeatureCollection(),
            ways,
            FeatureCollection(),
            FeatureCollection(),
            intersectionMap,
            tileX, tileY, tileZoom
        )
        traverseIntersectionsConfectingNames(intersectionMap)
        return Graph(ways, intersectionMap)
    }

    private fun location(point: Pair<Int, Int>) =
        convertGeometry(tileX, tileY, tileZoom, arrayListOf(point)).first()

    private fun Graph.intersectionAt(point: Pair<Int, Int>) = intersections[location(point)]

    private fun Graph.unnamedWayFrom(point: Pair<Int, Int>): Pair<Way, Boolean> {
        val intersection = assertNotNull(intersectionAt(point))
        val way = intersection.members.first { it.name == null }
        return way to (way.intersections[WayEnd.START.id] == intersection)
    }

    private fun Graph.nameLeaving(point: Pair<Int, Int>): String {
        val (way, forwards) = unnamedWayFrom(point)
        return way.getName(forwards, null, strings)
    }

    // A service road from Main Street, at y=2000, to Back Lane, at y=3200
    private val mainStreet = "Main Street" to line(100 to 2000, 1000 to 2000, 2000 to 2000)
    private val backLane = "Back Lane" to line(100 to 3200, 1000 to 3200, 2000 to 3200)
    private val serviceRoad =
        null to line(1000 to 2000, 1000 to 2300, 1000 to 2600, 1000 to 2900, 1000 to 3200)

    @Test
    fun withoutAGateTheServiceRoadLeadsToTheOtherRoad() {
        val graph = build(listOf(mainStreet, backLane, serviceRoad))
        assertEquals("ConfectNameTo(Service,Back Lane)", graph.nameLeaving(1000 to 2000))
    }

    @Test
    fun lockedGateMidWayEndsTheWayFromBothSides() {
        val graph = build(
            listOf(mainStreet, backLane, serviceRoad),
            listOf((1000 to 2600) to GateKind.LOCKED)
        )
        // No intersection at the gate
        assertNull(graph.intersectionAt(1000 to 2600))

        val (fromMain, forwards) = graph.unnamedWayFrom(1000 to 2000)
        val farEnd = if (forwards) WayEnd.END else WayEnd.START
        assertNull(fromMain.intersections[farEnd.id])
        assertEquals(
            GateKind.LOCKED.tag,
            fromMain.properties?.get(if (forwards) BARRIER_END else BARRIER_START)
        )

        assertEquals("ConfectNameToLockedGate(Service)", graph.nameLeaving(1000 to 2000))
        assertEquals("ConfectNameToLockedGate(Service)", graph.nameLeaving(1000 to 3200))
    }

    @Test
    fun noAccessGateIsDescribedAsNoAccess() {
        val graph = build(
            listOf(mainStreet, backLane, serviceRoad),
            listOf((1000 to 2600) to GateKind.NO_ACCESS)
        )
        assertEquals("ConfectNameToNoAccessGate(Service)", graph.nameLeaving(1000 to 2000))
    }

    @Test
    fun shortApproachToAGateIsCalledJustTheGate() {
        // The gate is 60 samples, about 10m, from Main Street
        val graph = build(
            listOf(
                mainStreet,
                backLane,
                null to line(1000 to 2000, 1000 to 2060, 1000 to 2600, 1000 to 3200)
            ),
            listOf((1000 to 2060) to GateKind.LOCKED)
        )
        assertEquals("ConfectNameLockedGate", graph.nameLeaving(1000 to 2000))
        assertEquals("ConfectNameToLockedGate(Service)", graph.nameLeaving(1000 to 3200))
    }

    @Test
    fun gateWhereTwoWaysMeetEndToEndSplitsThemWithNoIntersection() {
        // As at the Tesco delivery road: one way ends at the gate and the next starts there
        val graph = build(
            listOf(
                mainStreet,
                backLane,
                null to line(1000 to 2000, 1000 to 2300, 1000 to 2600),
                null to line(1000 to 2600, 1000 to 2900, 1000 to 3200)
            ),
            listOf((1000 to 2600) to GateKind.LOCKED)
        )
        assertNull(graph.intersectionAt(1000 to 2600))
        assertEquals("ConfectNameToLockedGate(Service)", graph.nameLeaving(1000 to 2000))
        assertEquals("ConfectNameToLockedGate(Service)", graph.nameLeaving(1000 to 3200))
    }

    @Test
    fun dijkstraDoesNotRouteThroughAGate() {
        val ruler = location(1000 to 2000).createCheapRuler()

        val open = build(listOf(mainStreet, backLane, serviceRoad))
        val openDistance = dijkstraOnWaysWithLoops(
            assertNotNull(open.intersectionAt(1000 to 2000)),
            assertNotNull(open.intersectionAt(1000 to 3200)),
            ruler
        )
        assertTrue(openDistance < 1000.0, "distance was $openDistance")

        val gated = build(
            listOf(mainStreet, backLane, serviceRoad),
            listOf((1000 to 2600) to GateKind.LOCKED)
        )
        val gatedDistance = dijkstraOnWaysWithLoops(
            assertNotNull(gated.intersectionAt(1000 to 2000)),
            assertNotNull(gated.intersectionAt(1000 to 3200)),
            ruler
        )
        assertEquals(Double.MAX_VALUE, gatedDistance)
    }

    @Test
    fun gateAtAJunctionIsIgnored() {
        // There's no telling which of the ways a gate on a junction node stands across. Main
        // Street runs through this node and the service road starts at it, so it's a T junction
        // of two lines, which mustn't be mistaken for two lines meeting end to end.
        val graph = build(
            listOf(mainStreet, backLane, serviceRoad),
            listOf((1000 to 2000) to GateKind.LOCKED)
        )
        assertNotNull(graph.intersectionAt(1000 to 2000))
        assertEquals("ConfectNameTo(Service,Back Lane)", graph.nameLeaving(1000 to 2000))
    }

    @Test
    fun gateKindFromTileProperties() {
        assertEquals(GateKind.LOCKED, gateKind(mapOf("locked" to "yes")))
        assertEquals(GateKind.NO_ACCESS, gateKind(mapOf("access" to "no")))
        // Locked wins when it's both
        assertEquals(GateKind.LOCKED, gateKind(mapOf("locked" to "yes", "access" to "no")))
        // Pedestrians are allowed through, however locked it is
        assertNull(gateKind(mapOf("locked" to "yes", "access" to "no", "foot" to "yes")))
        assertNull(gateKind(mapOf("locked" to "yes", "foot" to "designated")))
        assertNull(gateKind(mapOf("access" to "no", "foot" to "permissive")))
        // Neither private nor a restriction we don't describe stops the road
        assertNull(gateKind(mapOf("access" to "private", "foot" to "private")))
        assertNull(gateKind(mapOf("locked" to "no", "access" to "yes")))
        // Closed to pedestrians is no access for anyone Soundscape guides
        assertEquals(GateKind.NO_ACCESS, gateKind(mapOf("foot" to "no")))
        assertEquals(GateKind.LOCKED, gateKind(mapOf("locked" to "yes", "foot" to "no")))
        assertNull(gateKind(mapOf<String, Any?>()))
        assertNull(gateKind(null))
    }
}
