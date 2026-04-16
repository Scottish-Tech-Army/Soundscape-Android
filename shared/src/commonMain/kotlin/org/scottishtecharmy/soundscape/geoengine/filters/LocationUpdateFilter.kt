package org.scottishtecharmy.soundscape.geoengine.filters

import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import kotlin.math.floor

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

    fun update(userGeometry: UserGeometry?)
    {
        if(userGeometry != null) {
            lastTime = userGeometry.timestampMilliseconds
            lastLocation = userGeometry
        }
    }

    private fun shouldUpdate(userGeometry: UserGeometry,
                             updateTimeInterval: Long,
                             updateDistanceInterval: Double,
                             destination : LngLatAlt? = null)
        : Boolean {

        if((lastLocation == null) || (lastTime == 0L)) {
            return true
        }

        lastLocation?.let { geometry ->
            val distance = userGeometry.ruler.distance(userGeometry.location, geometry.location)
            val timeDifference = userGeometry.timestampMilliseconds - lastTime

            // Adaptive distance addition
            if(destination != null) {
                val newDistance = userGeometry.ruler.distance(destination, userGeometry.location)
                val adaptiveDistance = when {
                    // Only announce the distance on large changes of value if we are far away
                    (newDistance > 50000.0) -> 10000.0
                    (newDistance > 5000.0) -> 5000.0
                    (newDistance > 1000.0) -> 1000.0
                    else -> 0.0
                }
                if (adaptiveDistance != 0.0) {
                    // See if we've crossed a threshold
                    val lastDistance = userGeometry.ruler.distance(destination, geometry.location)
                    return ((floor(lastDistance / adaptiveDistance)) !=
                            (floor(newDistance/adaptiveDistance)) &&
                            (timeDifference >= updateTimeInterval))
                }
            }

            if ((distance >= updateDistanceInterval) && (timeDifference >= updateTimeInterval)) {
                return true
            }
        }

        // Neither the time interval and/or the distance interval have been passed
        return false
    }

    fun shouldUpdate(userGeometry: UserGeometry, destination : LngLatAlt? = null) : Boolean {
        return shouldUpdate(userGeometry, minTimeMs, minDistance, destination)
    }

    private val inVehicleTimeIntervalMultiplier = 4
    fun shouldUpdateActivity(userGeometry: UserGeometry) : Boolean {
        if(userGeometry.inVehicle()) {
            // If travelling in a vehicle then the speed is used to determine how far has to be
            // travelled before updating and the time is increased by a multiplier.
            val timeInterval = minTimeMs * inVehicleTimeIntervalMultiplier
            var distanceInterval = minDistance
            if(userGeometry.speed > 0) {
                distanceInterval = userGeometry.speed * minTimeMs
            }

            return shouldUpdate(userGeometry, timeInterval, distanceInterval)

        }
        return shouldUpdate(userGeometry, minTimeMs, minDistance)
    }
}
