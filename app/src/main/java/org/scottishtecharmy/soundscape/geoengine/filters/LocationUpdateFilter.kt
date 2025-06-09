package org.scottishtecharmy.soundscape.geoengine.filters

import org.scottishtecharmy.soundscape.geoengine.UserGeometry

/**
 * This class acts as a filter for throttling the frequency of computation which is initiated by
 * geolocation updates.
*/

open class LocationUpdateFilter(minTimeMilliseconds: Long, private val minDistance: Double) {

    private var lastLocation : UserGeometry? = null
    private var lastTime = 0L
    private var minTimeMs : Long = 0
    init {
        minTimeMs = minTimeMilliseconds
    }

    fun update(userGeometry: UserGeometry)
    {
        lastTime = userGeometry.timestampMilliseconds
        lastLocation = userGeometry
    }

    private fun shouldUpdate(userGeometry: UserGeometry,
                             updateTimeInterval: Long,
                             updateDistanceInterval: Double)
        : Boolean {

        if((lastLocation == null) || (lastTime == 0L)) {
            return true
        }

        lastLocation?.let { geometry ->
            val distance = userGeometry.ruler.distance(userGeometry.location, geometry.location)
            val timeDifference = userGeometry.timestampMilliseconds - lastTime

            if ((distance >= updateDistanceInterval) && (timeDifference >= updateTimeInterval)) {
                return true
            }
        }

        // Neither the time interval and/or the distance interval have been passed
        return false
    }

    fun shouldUpdate(userGeometry: UserGeometry) : Boolean {
        return shouldUpdate(userGeometry, minTimeMs, minDistance)
    }

    private val inVehicleTimeIntervalMultiplier = 4
    fun shouldUpdateActivity(userGeometry: UserGeometry) : Boolean {
        if(userGeometry.inVehicle()) {
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
