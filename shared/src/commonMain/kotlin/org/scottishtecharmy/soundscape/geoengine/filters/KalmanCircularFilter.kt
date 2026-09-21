package org.scottishtecharmy.soundscape.geoengine.filters

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private const val DEG_TO_RAD = PI / 180.0
private const val RAD_TO_DEG = 180.0 / PI

class KalmanCircularFilter(filterSigma: Double = 1.0) {
    private val inner = KalmanFilter(filterSigma = filterSigma, dimensions = 2)

    fun process(angleDegrees: Double, timestamp: Long, accuracyDegrees: Double): Double {
        val rad = angleDegrees * DEG_TO_RAD
        val vec = doubleArrayOf(sin(rad), cos(rad))
        val filtered = inner.process(vec, timestamp, accuracyDegrees)
        val out = atan2(filtered[0], filtered[1]) * RAD_TO_DEG
        val mod = out % 360.0
        return if (mod < 0.0) mod + 360.0 else mod
    }

    fun reset() {
        inner.reset()
    }
}
