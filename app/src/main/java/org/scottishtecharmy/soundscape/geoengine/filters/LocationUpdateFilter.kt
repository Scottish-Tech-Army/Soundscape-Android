package org.scottishtecharmy.soundscape.geoengine.filters

import org.scottishtecharmy.soundscape.geoengine.GeoEngine

/**
 * This class acts as a filter for throttling the frequency of computation which is initiated by
 * geolocation updates.
*/

open class LocationUpdateFilter(private val minTimeMs: Long, private val minDistance: Double) {

    private var lastLocation : GeoEngine.UserGeometry? = null
    private var lastTime = 0L

    fun update(userGeometry: GeoEngine.UserGeometry)
    {
        lastTime = System.currentTimeMillis()
        lastLocation = userGeometry
    }

    private fun shouldUpdate(userGeometry: GeoEngine.UserGeometry,
                             updateTimeInterval: Long,
                             updateDistanceInterval: Double)
        : Boolean {

        if((lastLocation == null) || (lastTime == 0L)) {
            return true
        }

        lastLocation?.let { geometry ->
            val distance = userGeometry.location.distance(geometry.location)
            val timeDifference = System.currentTimeMillis() - lastTime

            if ((distance > updateDistanceInterval) && (timeDifference > updateTimeInterval)) {
                return true
            }
        }

        // Neither the time interval and/or the distance interval have been passed
        return false
    }

    fun shouldUpdate(userGeometry: GeoEngine.UserGeometry) : Boolean {
        return shouldUpdate(userGeometry, minTimeMs, minDistance)
    }

    private val inVehicleTimeIntervalMultiplier = 4
    fun shouldUpdateActivity(userGeometry: GeoEngine.UserGeometry) : Boolean {
        if(userGeometry.inVehicle) {
            // If travelling in a vehicle then the speed is used to determine how far has to be
            // travelled before updating and the time is increased by a multiplier.
            val timeInterval = minTimeMs * inVehicleTimeIntervalMultiplier
            var distanceInterval = minDistance
            if(userGeometry.speed > 0) {
                distanceInterval = userGeometry.speed.toDouble() * minTimeMs
            }

            return shouldUpdate(userGeometry, timeInterval, distanceInterval)

        }
        return shouldUpdate(userGeometry, minTimeMs, minDistance)
    }
}
