package org.scottishtecharmy.soundscape

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.scottishtecharmy.soundscape.MainActivity.Companion.MOBILITY_KEY
import org.scottishtecharmy.soundscape.MainActivity.Companion.PLACES_AND_LANDMARKS_KEY
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.MAX_ZOOM_LEVEL
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.callouts.AutoCallout
import org.scottishtecharmy.soundscape.geoengine.filters.MapMatchFilter
import org.scottishtecharmy.soundscape.geoengine.filters.RailMatchArbiter
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.AlongWayKind
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

private val enabledCategories = setOf(PLACES_AND_LANDMARKS_KEY, MOBILITY_KEY)

private val Way.brunnel get() = properties?.get("brunnel")
private val Way.service get() = properties?.get("service")
private fun Way.stopNames() = alongWayFeatures(AlongWayKind.RAILWAY_STOP).map { it.name }

private fun GridState.railways(filter: (Way) -> Boolean) =
    getFeatureTree(TreeId.TRANSIT).getAllCollection().features.filterIsInstance<Way>().filter(filter)

private fun GridState.roads(filter: (Way) -> Boolean) =
    getFeatureTree(TreeId.ROADS).getAllCollection().features.filterIsInstance<Way>().filter(filter)

/** The line [start] is part of, followed from it for [distance] metres through the Ways it's split into. */
private fun GridState.lineFrom(start: Way, distance: Double) =
    buildContinuousRoute(this, start, distance) { (it.name == start.name) && (it.service == null) }

/**
 * Travel mode on the railways of the cities outside the UK with a test extract: Paris, Tehran,
 * Buenos Aires and San Salvador.
 *
 * Metro lines are matched like any other railway, so a metro ride is picked up wherever its line
 * runs above ground - Paris Métro 6 on its viaduct, Tehran Line 1 in the south of the city - and
 * followed into the tunnels beyond. A ride that's underground from start to finish is never picked
 * up, because a tunnel can't start a train ride: GPS can't tell the line from the street above it,
 * and underground there's no usable fix to tell them apart with anyway.
 */
class WorldCitiesRailTest {

    /**
     * A journey through the geo engine the way the app makes one: a fix a second at a steady
     * [speed] along [route], road and rail map matching, RailMatchArbiter deciding whether it's a
     * train, and the callouts that come out of it.
     */
    private class Journey(centre: LngLatAlt, speed: Double, route: (GridState) -> List<LngLatAlt>) {
        /** The railway the user was reckoned to be on at each fix, or null where not on a train. */
        val onTrain = mutableListOf<Way?>()
        val callouts = mutableListOf<String>()
        var railMatched = false

        val wasATrain get() = onTrain.any { it != null }
        val trainNames get() = onTrain.filterNotNull().map { it.name }.toSet()

        init {
            val gridState = getGridStateForLocation(centre, MAX_ZOOM_LEVEL, 3)
            val settlementGrid = getGridStateForLocation(centre, 12, 3)
            val samples = resampleAtSpeed(route(gridState), gridState.ruler, speed, 1.0)

            val roadMatcher = MapMatchFilter()
            val railMatcher = MapMatchFilter(networkTree = TreeId.TRANSIT)
            val arbiter = RailMatchArbiter()
            val autoCallout = AutoCallout(null, null)
            for ((index, sample) in samples.withIndex()) {
                runBlocking {
                    gridState.locationUpdate(sample.location, enabledCategories, null)
                    settlementGrid.locationUpdate(sample.location, enabledCategories, null)
                }
                roadMatcher.filter(sample.location, gridState, FeatureCollection(), false, null, true)
                railMatcher.filter(sample.location, gridState, FeatureCollection(), false, null)
                if (railMatcher.isMatchConfident) railMatched = true

                val railway = arbiter.update(roadMatcher, railMatcher, speed)
                onTrain += railway
                autoCallout.updateLocation(
                    UserGeometry(
                        location = sample.location,
                        travelHeading = sample.bearing,
                        speed = speed,
                        mapMatchedWay = roadMatcher.matchedWay,
                        mapMatchedLocation = roadMatcher.matchedLocation,
                        mapMatchedRailway = railway,
                        timestampMilliseconds = 1_000_000L + index * 1000L
                    ),
                    gridState,
                    settlementGrid
                )?.let { callout -> callouts += callout.positionedStrings.map { it.text } }
            }
        }

        fun assertCalledOut(expected: String) =
            assertTrue("Expected \"$expected\" in $callouts", callouts.any { it.contains(expected) })

        /** Checked alongside a journey that's never a train, so that it can't pass by matching nothing. */
        fun assertRailMatched() =
            assertTrue("The rail matcher never locked on, so this proves nothing", railMatched)
    }

    // ------------------------------------------------------------------------------------ Paris

    private val cambronne = LngLatAlt(2.3029, 48.8476)

    @Test
    fun parisMetroOnAViaductIsATrain() {
        // Line 6 crosses the 15th arrondissement on a viaduct, and the Seine on the Pont de
        // Bir-Hakeim, before going underground at Passy
        val journey = Journey(cambronne, 12.0) { grid ->
            val start = grid.railways {
                (it.name == "Métro 6") && (it.brunnel == "bridge") && (it.stopNames().firstOrNull() == "Dupleix")
            }.maxBy { it.length }
            grid.lineFrom(start, 3000.0)
        }

        assertTrue(journey.wasATrain)
        assertEquals(setOf("Métro 6"), journey.trainNames)
        journey.assertCalledOut("Approaching Bir-Hakeim")
        journey.assertCalledOut("At Bir-Hakeim")
        journey.assertCalledOut("At Passy")
        // Not a station below the viaduct, which isn't on the line
        assertTrue(journey.callouts.toString(), journey.callouts.none { it.contains("Tour Eiffel - Quai Branly") })
    }

    @Test
    fun parisDrivingUnderTheMetroViaductIsNotATrain() {
        // Boulevard Garibaldi and Boulevard de Grenelle run beneath the Line 6 viaduct from
        // Cambronne to Dupleix, and the rail matcher locks onto the viaduct from the road below
        val boulevards = setOf("Boulevard Garibaldi", "Boulevard de Grenelle")
        val journey = Journey(cambronne, 12.0) { grid ->
            val start = grid.railways {
                (it.name == "Métro 6") && (it.brunnel == "bridge") && (it.stopNames().firstOrNull() == "Cambronne")
            }.maxBy { it.length }
            // The viaduct's route, moved onto the boulevard beneath it
            resampleAtSpeed(grid.lineFrom(start, 2500.0), grid.ruler, 12.0, 1.0).mapNotNull { sample ->
                grid.getFeatureTree(TreeId.ROADS)
                    .getNearestCollection(sample.location, 30.0, 10, grid.ruler)
                    .features.filterIsInstance<Way>()
                    .firstOrNull { it.name in boulevards }
                    ?.let { grid.ruler.distanceToLineString(sample.location, it.geometry as LineString).point }
            }
        }

        journey.assertRailMatched()
        assertFalse(journey.onTrain.toString(), journey.wasATrain)
    }

    @Test
    fun parisMetroInATunnelIsNeverATrain() {
        // Line 1 runs under the Rue de Rivoli from Châtelet to Concorde. Even with a perfect fix
        // on the line throughout, a tunnel can't start a train ride.
        val journey = Journey(parisTestLocation, 15.0) { grid ->
            val start = grid.railways {
                (it.name == "Métro 1") && (it.brunnel == "tunnel") && (it.service == null)
            }.maxBy { it.length }
            grid.lineFrom(start, 3000.0)
        }

        journey.assertRailMatched()
        assertFalse(journey.wasATrain)
    }

    // ----------------------------------------------------------------------------------- Tehran

    @Test
    fun tehranMetroAboveGroundCallsItsStations() {
        // Line 1 runs on the surface through Shahr-e Rey, beside the Tehran-Mashhad railway, and
        // then goes underground
        val journey = Journey(LngLatAlt(51.418, 35.612), 15.0) { grid ->
            val start = grid.railways { way ->
                val line = way.geometry as LineString
                (way.name == "خط ۱") && ("شهرری" in way.stopNames()) &&
                    (line.coordinates.first().latitude > line.coordinates.last().latitude)
            }.single()
            grid.lineFrom(start, 4000.0)
        }

        assertEquals(setOf("خط ۱"), journey.trainNames)
        journey.assertCalledOut("Approaching شهرری")
        journey.assertCalledOut("At شهرری")
        // ...and still followed once the line is in tunnel
        assertTrue(journey.onTrain.any { it?.brunnel == "tunnel" })
        journey.assertCalledOut("Entering a tunnel")
    }

    @Test
    fun tehranMetroUnderTheCityIsNeverATrain() {
        // Line 2 runs under Azadi Street
        val journey = Journey(tehranTestLocation, 15.0) { grid ->
            val start = grid.railways { (it.name == "خط ۲ مترو") && (it.brunnel == "tunnel") }.maxBy { it.length }
            grid.lineFrom(start, 2000.0)
        }

        journey.assertRailMatched()
        assertFalse(journey.wasATrain)
    }

    // ----------------------------------------------------------------------------- Buenos Aires

    @Test
    fun buenosAiresSuburbanTrainIsATrain() {
        // The Mitre line on the surface through Recoleta towards Retiro
        val journey = Journey(LngLatAlt(-58.42, -34.575), 20.0) { grid ->
            val start = grid.railways {
                (it.name == "FC Mitre") && (it.brunnel == null) && (it.service == null)
            }.maxBy { it.length }
            grid.lineFrom(start, 4000.0)
        }

        assertEquals(setOf("FC Mitre"), journey.trainNames)
        journey.assertCalledOut("On FC Mitre")
    }

    @Test
    fun buenosAiresSubteIsNeverATrain() {
        // Line B runs under the Avenida Corrientes, and the Subte is underground throughout
        val journey = Journey(buenosAiresTestLocation, 15.0) { grid ->
            val start = grid.railways {
                (it.name == "Línea B") && (it.brunnel == "tunnel") && (it.service == null)
            }.maxBy { it.length }
            grid.lineFrom(start, 2500.0)
        }

        journey.assertRailMatched()
        assertFalse(journey.wasATrain)
    }

    // ----------------------------------------------------------------------------- San Salvador

    @Test
    fun sanSalvadorBusRapidTransitIsNotATrain() {
        // The SITRAMSS bus lane passes the old FENADESAL station, but there's no railway to match
        val journey = Journey(sanSalvadorTestLocation, 15.0) { grid ->
            buildContinuousRoute(grid, grid.roads { it.name == "Carril SITRAMSS" }.maxBy { it.length }, 3000.0)
        }

        assertFalse(journey.railMatched)
        assertFalse(journey.wasATrain)
    }
}
