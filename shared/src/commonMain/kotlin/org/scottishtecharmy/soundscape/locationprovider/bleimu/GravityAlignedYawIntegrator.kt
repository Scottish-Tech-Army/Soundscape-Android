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
 *   3. Integrate over the sample's interval, which the caller supplies: the
 *      WitMotion packs several samples into each notification, so their
 *      arrival times don't give it.
 *
 * Sign: right-hand rule about world-up, so positive when rotating CCW viewed
 * from above. Callers that want compass-style CW-positive must negate.
 *
 * The integration drifts (no magnetic reference), but slow drift is corrected
 * by [HeadphoneCalibrationManager]'s correlation against compass + course.
 */
class GravityAlignedYawIntegrator(
    private val gravityLpfAlpha: Double = 0.05,
) {
    private var filteredUpX: Double = 0.0
    private var filteredUpY: Double = 0.0
    private var filteredUpZ: Double = 0.0
    private var hasFilteredUp: Boolean = false

    private var integratedYawDegrees: Double = 0.0

    fun reset() {
        filteredUpX = 0.0; filteredUpY = 0.0; filteredUpZ = 0.0
        hasFilteredUp = false
        integratedYawDegrees = 0.0
    }

    /**
     * Add one IMU sample that covers [dtSeconds]. Returns the integrated yaw in degrees
     * (unbounded — caller can normalize if needed), or null before the first usable sample.
     */
    fun update(accelG: Vec3, gyroDps: Vec3, dtSeconds: Double): Double? {
        if (!updateGravity(accelG)) return if (hasFilteredUp) integratedYawDegrees else null
        val yawRateDps =
            gyroDps.x * filteredUpX + gyroDps.y * filteredUpY + gyroDps.z * filteredUpZ
        integratedYawDegrees += yawRateDps * dtSeconds
        return integratedYawDegrees
    }

    /** Low-pass filters the up direction with this sample. False if it's unusable. */
    private fun updateGravity(accelG: Vec3): Boolean {
        val mag = sqrt(accelG.x * accelG.x + accelG.y * accelG.y + accelG.z * accelG.z)
        if (mag < MIN_ACCEL_MAG_G) return false
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
        return true
    }

    companion object {
        // Below this magnitude (g) the accelerometer reading is unusable for
        // estimating gravity direction (sensor essentially in free fall).
        private const val MIN_ACCEL_MAG_G = 0.1
    }
}
