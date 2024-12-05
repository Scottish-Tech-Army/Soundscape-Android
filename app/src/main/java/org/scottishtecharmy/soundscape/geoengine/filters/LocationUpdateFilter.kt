package org.scottishtecharmy.soundscape.geoengine.filters

import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

/**
 * This class acts as a filter for throttling the frequency of computation which is initiated by
 * geolocation updates.
*/

open class LocationUpdateFilter(private val minTimeMs: Long, private val minDistance: Double) {

    private var lastLocation : LngLatAlt? = null
    private var lastTime = 0L

    fun update(location: LngLatAlt)
    {
        lastTime = System.currentTimeMillis()
        lastLocation = location
    }

    private fun shouldUpdate(location: LngLatAlt,
                             updateTimeInterval: Long,
                             updateDistanceInterval: Double)
        : Boolean {

        if((lastLocation == null) || (lastTime == 0L)) {
            return true
        }

        val distance = location.distance(lastLocation!!)
        val timeDifference = System.currentTimeMillis() - lastTime

        if((distance > updateDistanceInterval) && (timeDifference > updateTimeInterval)) {
            return true
        }

        // Neither the time interval and/or the distance interval have been passed
        return false
    }

    fun shouldUpdate(location: LngLatAlt) : Boolean {
        return shouldUpdate(location, minTimeMs, minDistance)
    }

    private val inVehicleTimeIntervalMultiplier = 4
    fun shouldUpdateActivity(location: LngLatAlt, speed: Float, inVehicle: Boolean) : Boolean {
        if(inVehicle) {
            // If travelling in a vehicle then the speed is used to determine how far has to be
            // travelled before updating and the time is increased by a multiplier.
            val timeInterval = minTimeMs * inVehicleTimeIntervalMultiplier
            var distanceInterval = minDistance
            if(speed > 0) {
                distanceInterval = speed.toDouble() * minTimeMs
            }

            return shouldUpdate(location, timeInterval, distanceInterval)

        }
        return shouldUpdate(location, minTimeMs, minDistance)
    }
}
