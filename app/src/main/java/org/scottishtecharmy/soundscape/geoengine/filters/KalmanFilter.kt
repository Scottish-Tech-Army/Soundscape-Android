package org.scottishtecharmy.soundscape.geoengine.filters

import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

open class KalmanFilter(filterSigma : Double = 9.0, private val dimensions : Int = 2) {

    /// The minimum allowable accuracy measurement. Input accuracy values are clamped to this
    /// minimum in order to prevent a division by zero in the `process(:)` method.
    private val minimumAccuracy: Double = 0.1

    /// Parameter describing how quickly the accuracy of the current filtered location
    /// degrades in the absence of any additional location updates.
    private var sigma: Double = 0.0

    init {
        sigma = filterSigma
    }

    /// Current covariance of the filter. This value updates each time a new location update is
    /// passed to the filter.
    private var covariance = 0.0

    /// Last estimate  computed by the `process(:)` method
    private var estimate = DoubleArray(dimensions)

    /// Timestamp of last estimate
    private var timestamp: Long = 0L

    fun process(newVector: DoubleArray,
                newTimestamp: Long,
                newAccuracy: Double) : DoubleArray {

        assert(newVector.size == dimensions)

        // Ensure `accuracy >= minimumAccuracy`
        val accuracy = maxOf(newAccuracy, minimumAccuracy)

        // Calculate variance
        val measurementVariance = accuracy * accuracy

        // Check if the filter is initialized, and initialize it if it isn't
        if(timestamp == 0L) {
            estimate = newVector
            timestamp = newTimestamp
            covariance = measurementVariance
            return newVector
        }

        // Increase the covariance linearly with time (to represent the decay in the accuracy of the previous measurement)
        val interval = (newTimestamp - timestamp) / 1000.0
        if(interval > 0 ) {
            covariance += interval * sigma * sigma
        }

        // Smooth the input location to estimate the current location
        val kalmanGain = covariance / (covariance + measurementVariance)
        val filtered = DoubleArray(dimensions)
        for(entry in filtered.withIndex()) {
            filtered[entry.index] =
                estimate[entry.index] +
                kalmanGain * ((newVector[entry.index]) - estimate[entry.index])
        }
        estimate = filtered
        timestamp = newTimestamp
        covariance *= (1 - kalmanGain)

        return filtered
    }

    fun reset() {
        covariance = 0.0
        estimate = DoubleArray(2)
        timestamp = 0L
    }
}

fun arrayToLngLatAlt(array: DoubleArray) : LngLatAlt {
    return LngLatAlt(array[0], array[1])
}
fun lngLatAltToArray(location: LngLatAlt) : DoubleArray {
    return doubleArrayOf(location.longitude, location.latitude)
}
class KalmanLocationFilter(filterSigma : Double = 6.0) : KalmanFilter(filterSigma, 2) {
    fun process(newVector: LngLatAlt,
                newTimestamp: Long,
                newAccuracy: Double) : LngLatAlt {
        return arrayToLngLatAlt(super.process(lngLatAltToArray(newVector), newTimestamp, newAccuracy))
    }
}

class KalmanHeadingFilter(filterSigma : Double = 9.0) : KalmanFilter(filterSigma, 1) {
    fun process(newVector: Double,
                newTimestamp: Long,
                newAccuracy: Double) : Double {
        return super.process(doubleArrayOf(newVector), newTimestamp, newAccuracy)[0]
    }
}
