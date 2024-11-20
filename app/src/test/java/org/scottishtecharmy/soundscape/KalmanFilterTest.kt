package org.scottishtecharmy.soundscape

import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.kalman.KalmanFilter
import org.scottishtecharmy.soundscape.geoengine.utils.distance
import org.scottishtecharmy.soundscape.geoengine.utils.getDestinationCoordinate

class KalmanFilterTest {

    @Test
    fun testFilter() {
        val filter = KalmanFilter(3.0)
        filter.reset()

        val start = LngLatAlt(10.0, 20.0)
        // Start off with accurate location
        val timestamp = System.currentTimeMillis()
        var filteredValue = filter.process(start, timestamp, 10.0)
        println("First location: " + filteredValue.latitude + " " + filteredValue.longitude)
        var distanceToData =  distance(filteredValue.latitude, filteredValue.longitude, start.latitude, start.longitude)
        println("$distanceToData from start")
        assertEquals(start.latitude, filteredValue.latitude, 0.0)
        assertEquals(start.longitude, filteredValue.longitude, 0.0)
        assertEquals(distanceToData, 0.0, 0.0)

        // Try to move to this new point 1000m away at 30 degrees. Start as inaccurate location
        // and get more accurate
        val end = getDestinationCoordinate(start, 30.0, 1000.0)

        // 1 second later, inaccurate location - this shouldn't move us very close
        filteredValue = filter.process(end, timestamp + 1000, 1000.0)
        distanceToData =  distance(filteredValue.latitude, filteredValue.longitude, end.latitude, end.longitude)
        println("$distanceToData from end")
        assertEquals(distanceToData, 999.0, 1.0)

        // 1 second later, accurate location - this should move us much closer
        filteredValue = filter.process(end, timestamp + 1000, 10.0)
        distanceToData =  distance(filteredValue.latitude, filteredValue.longitude, end.latitude, end.longitude)
        println("$distanceToData from end")
        assertEquals(distanceToData, 478.0, 1.0)

        // 1 second later, accurate location - this should move us very close
        filteredValue = filter.process(end, timestamp + 1000, 1.0)
        distanceToData =  distance(filteredValue.latitude, filteredValue.longitude, end.latitude, end.longitude)
        println("$distanceToData from end")
        assertEquals(distanceToData, 9.0, 1.0)
    }

    @Test
    fun testFilter2() {
        val filter = KalmanFilter(3.0)
        filter.reset()

        val start = LngLatAlt(10.0, 20.0)
        // Start off with accurate location
        val timestamp = System.currentTimeMillis()
        var filteredValue = filter.process(start, timestamp, 10.0)
        println("First location: " + filteredValue.latitude + " " + filteredValue.longitude)
        var distanceToData =  distance(filteredValue.latitude, filteredValue.longitude, start.latitude, start.longitude)
        println("$distanceToData from start")
        assertEquals(start.latitude, filteredValue.latitude, 0.0)
        assertEquals(start.longitude, filteredValue.longitude, 0.0)
        assertEquals(distanceToData, 0.0, 0.0)

        // Try to move to this new point 1000m away at 30 degrees.
        // and get more accurate
        val end = getDestinationCoordinate(start, 30.0, 1000.0)

        // 1 second later, accurate location
        filteredValue = filter.process(end, timestamp + 1000, 1.0)
        distanceToData =  distance(filteredValue.latitude, filteredValue.longitude, end.latitude, end.longitude)
        println("$distanceToData from end")
        assertEquals(distanceToData, 0.0, 10.0)

        // 1 second later, accurate location
        filteredValue = filter.process(end, timestamp + 1000, 1.0)
        distanceToData =  distance(filteredValue.latitude, filteredValue.longitude, end.latitude, end.longitude)
        println("$distanceToData from end")
        assertEquals(distanceToData, 0.0, 5.0)
    }
}