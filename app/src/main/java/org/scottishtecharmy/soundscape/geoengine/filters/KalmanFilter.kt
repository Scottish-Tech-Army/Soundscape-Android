package org.scottishtecharmy.soundscape.geoengine.filters

import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

class KalmanFilter(filterSigma : Double = 3.0) {

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
    private var estimate = LngLatAlt()

    /// Timestamp of last estimate
    private var timestamp: Long = 0L

    fun process(newVector: LngLatAlt,
                newTimestamp: Long,
                newAccuracy: Double) : LngLatAlt {

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
        val filtered = LngLatAlt()
        filtered.latitude = estimate.latitude + kalmanGain * ((newVector.latitude) - estimate.latitude)
        filtered.longitude = estimate.longitude + kalmanGain * ((newVector.longitude) - estimate.longitude)

        estimate = filtered
        timestamp = newTimestamp
        covariance *= (1 - kalmanGain)

        return filtered
    }

    fun reset() {
        covariance = 0.0
        estimate = LngLatAlt()
        timestamp = 0L
    }
}