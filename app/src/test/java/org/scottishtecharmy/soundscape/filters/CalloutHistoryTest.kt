package org.scottishtecharmy.soundscape.filters

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.filters.CalloutHistory
import org.scottishtecharmy.soundscape.geoengine.filters.LocationUpdateFilter
import org.scottishtecharmy.soundscape.geoengine.filters.TrackedCallout
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

class CalloutHistoryTest {

    @Test
    fun testAddAndFind() {
        val history = CalloutHistory(10)
        val location = LngLatAlt(0.0, 0.0, 0.0) // Example location
        val callout = TrackedCallout(UserGeometry(), "Poi number one", location,
            isPoint = true,
            isGeneric = false
        )
        history.add(callout)
        assertTrue(history.find(callout))
        assertEquals(1, history.size())
    }

    @Test
    fun testTrimByTime() {
        val history = CalloutHistory(10)
        val location = LngLatAlt(0.0, 0.0, 0.0) // Example location
        val callout = TrackedCallout(
            UserGeometry(timestampMilliseconds = 0L),
            "Poi number two",
            location,
            isPoint = true,
            isGeneric = true
        )
        history.add(callout)
        history.trim(UserGeometry(
                location = location,
                timestampMilliseconds = 11L
            )
        )
        assertFalse(history.find(callout))
        assertEquals(0, history.size())
    }

    @Test
    fun testTrimByDistance() {
        val history = CalloutHistory(10)
        val location = LngLatAlt(0.0, 0.0, 0.0) // Example location
        val distantLocation = LngLatAlt(10.0, 10.0, 10.0)
        val callout = TrackedCallout(UserGeometry(), "Poi number three", distantLocation,
            isPoint = true,
            isGeneric = false
        )
        history.add(callout)
        history.trim(UserGeometry(location))
        assertFalse(history.find(callout))
        assertEquals(0, history.size())
    }

    @Test
    fun testTrimEmptyHistory() {
        val history = CalloutHistory(10)
        val location = LngLatAlt(0.0, 0.0, 0.0) // Example location
        history.trim(UserGeometry(location))
        assertEquals(0, history.size())
    }

    @Test
    fun testAdaptiveLocationFilter() {
        val filter = LocationUpdateFilter(0,100.0)

        var calloutCount = 0
        val destination = LngLatAlt()
        var distanceAway = 200000.0

        val ruler = CheapRuler(0.0)

        while (distanceAway >= 0.0) {
            distanceAway -= 10.0

            val remoteLocation =  ruler.destination(destination, distanceAway, 0.0)
            val userGeometry = UserGeometry(
                location = remoteLocation,
                timestampMilliseconds = 1,
                ruler = ruler
            )
            if(filter.shouldUpdate(userGeometry, destination)) {
                println("Updating on $distanceAway")
                calloutCount += 1
                filter.update(userGeometry)
            }
        }
        println("Callout count: $calloutCount")
        assert(calloutCount == 41)
    }
}