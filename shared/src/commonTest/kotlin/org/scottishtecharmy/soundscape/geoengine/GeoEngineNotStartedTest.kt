package org.scottishtecharmy.soundscape.geoengine

import kotlinx.coroutines.runBlocking
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

/**
 * A GeoEngine which was never started, as the Android service has while it snoozes for wake on
 * leave: the UI can still be bound to the service then and ask it things, and the service can be
 * stopped. Each of these used to throw UninitializedPropertyAccessException on the engine's
 * lateinit state - the test farm's monkey hit stop() from the service's onDestroy, and
 * getLocationDescription() and getOfflineAddress() from the Home screen.
 */
class GeoEngineNotStartedTest {

    private val location = LngLatAlt(-4.2518, 55.8642)

    @Test
    fun stop() {
        val engine = GeoEngine()
        engine.stop()
        // and again, as a stopped engine is in the same state as one never started
        engine.stop()
    }

    @Test
    fun refreshOfflineMaps() {
        GeoEngine().refreshOfflineMaps()
    }

    @Test
    fun lookupsAnswerWithNothing() = runBlocking {
        val engine = GeoEngine()

        val description = engine.getLocationDescription(location)
        assertEquals("", description.name)
        assertEquals(location, description.location)

        assertNull(engine.getOfflineAddress(location))
        assertNull(engine.searchResult("cafe"))
        assertFalse(engine.searchIsOfflineWithoutMaps())
    }

    @Test
    fun calloutsAnswerWithNothing() = runBlocking {
        val engine = GeoEngine()

        assertNull(engine.myLocation())
        assertNull(engine.whatsAroundMe())
        assertNull(engine.aheadOfMe())
        assertNull(engine.nearbyMarkers())
        assertNull(engine.calloutBeacon())
        assertNull(engine.beaconMoreInfo("Beacon"))
    }
}
