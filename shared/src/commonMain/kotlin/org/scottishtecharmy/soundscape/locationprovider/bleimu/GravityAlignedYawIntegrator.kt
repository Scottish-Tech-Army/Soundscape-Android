package org.scottishtecharmy.soundscape.locationprovider.bleimu

import kotlin.math.sqrt

/**
 * Integrates the body-frame gyroscope into yaw about the **world-up axis**,
 * making the output independent of how the IMU is physically mounted.
 *
 * Each update:
 *   1. Normalise the accelerometer to get `up_in_body` (gravity direction).
 *      A low-pass filter on this vector suppresses linear-acceleration spikes
 *      from head movements / walking bobs.
 *   2. Compute `yawRate = gyro · up_in_body` — the component of the body's
 *      angular velocity about the world-up axis.
 *   3. Integrate over `dt = now - previousSampleTime`.
 *
 * Sign: right-hand rule about world-up, so positive when rotating CCW viewed
 * from above. Callers that want compass-style CW-positive must negate.
 *
 * The integration drifts (no magnetic reference), but slow drift is corrected
 * by [HeadphoneCalibrationManager]'s correlation against compass + course.
 */
class GravityAlignedYawIntegrator(
    private val gravityLpfAlpha: Double = 0.05,
    private val maxDtMillis: Long = 200L,
) {
    private var filteredUpX: Double = 0.0
    private var filteredUpY: Double = 0.0
    private var filteredUpZ: Double = 0.0
    private var hasFilteredUp: Boolean = false

    private var integratedYawDegrees: Double = 0.0
    private var lastTimestampMillis: Long = 0L
    private var hasLastTimestamp: Boolean = false

    fun reset() {
        filteredUpX = 0.0; filteredUpY = 0.0; filteredUpZ = 0.0
        hasFilteredUp = false
        integratedYawDegrees = 0.0
        lastTimestampMillis = 0L
        hasLastTimestamp = false
    }

    /**
     * Add one IMU sample. Returns the integrated yaw in degrees (unbounded —
     * caller can normalize if needed) or null if this was the first sample
     * (no dt available yet).
     */
    fun update(accelG: Vec3, gyroDps: Vec3, timestampMillis: Long): Double? {
        val mag = sqrt(accelG.x * accelG.x + accelG.y * accelG.y + accelG.z * accelG.z)
        if (mag < MIN_ACCEL_MAG_G) {
            // Free-fall / bogus reading. Skip integration this frame, just bump
            // the timestamp so we don't get a huge dt next time.
            lastTimestampMillis = timestampMillis
            hasLastTimestamp = true
            return if (hasFilteredUp) integratedYawDegrees else null
        }
        val ux = accelG.x / mag
        val uy = accelG.y / mag
        val uz = accelG.z / mag

        if (!hasFilteredUp) {
            filteredUpX = ux; filteredUpY = uy; filteredUpZ = uz
            hasFilteredUp = true
        } else {
            val a = gravityLpfAlpha
            filteredUpX = filteredUpX * (1.0 - a) + ux * a
            filteredUpY = filteredUpY * (1.0 - a) + uy * a
            filteredUpZ = filteredUpZ * (1.0 - a) + uz * a
        }

        val prev = lastTimestampMillis
        val hadPrev = hasLastTimestamp
        lastTimestampMillis = timestampMillis
        hasLastTimestamp = true
        if (!hadPrev) return null
        val rawDt = timestampMillis - prev
        if (rawDt <= 0L) return integratedYawDegrees
        val dtSeconds = rawDt.coerceAtMost(maxDtMillis) / 1000.0

        val yawRateDps =
            gyroDps.x * filteredUpX + gyroDps.y * filteredUpY + gyroDps.z * filteredUpZ
        integratedYawDegrees += yawRateDps * dtSeconds
        return integratedYawDegrees
    }

    companion object {
        // Below this magnitude (g) the accelerometer reading is unusable for
        // estimating gravity direction (sensor essentially in free fall).
        private const val MIN_ACCEL_MAG_G = 0.1
    }
}
