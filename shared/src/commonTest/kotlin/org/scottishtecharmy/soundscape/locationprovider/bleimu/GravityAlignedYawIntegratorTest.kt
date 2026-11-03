package org.scottishtecharmy.soundscape.locationprovider.bleimu

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GravityAlignedYawIntegratorTest {

    @Test
    fun integratesYawRateAboutWorldUpWhenSensorIsZUp() {
        // Sensor upright (accel Z = +1g => up_in_body = +Z).
        // Rotate 90 deg/s about body Z for 1 second => 90° integrated.
        val it = GravityAlignedYawIntegrator(gravityLpfAlpha = 1.0)
        val result = it.update(Vec3(0.0, 0.0, 1.0), Vec3(0.0, 0.0, 90.0), 1.0)
        assertNotNull(result)
        assertNear(90.0, result)
    }

    @Test
    fun sumsShortSamples() {
        // Four 10 ms samples at 100 deg/s, as the WitMotion packs into one notification
        val integrator = GravityAlignedYawIntegrator(gravityLpfAlpha = 1.0)
        var result: Double? = null
        repeat(4) { result = integrator.update(Vec3(0.0, 0.0, 1.0), Vec3(0.0, 0.0, 100.0), 0.01) }
        assertNear(4.0, result!!)
    }

    @Test
    fun ignoresRotationAboutHorizontalAxesWhenSensorIsZUp() {
        // Rotate about body X (not gravity-aligned) => no yaw accumulation.
        val it = GravityAlignedYawIntegrator(gravityLpfAlpha = 1.0)
        val result = it.update(Vec3(0.0, 0.0, 1.0), Vec3(120.0, 0.0, 0.0), 1.0)
        assertNotNull(result)
        assertNear(0.0, result)
    }

    @Test
    fun mountedOnSideStillCapturesYawCorrectly() {
        // Sensor lying on its side with body +X pointing up (accel X = +1g).
        // A head turn = world-up rotation. The user rotates 45 deg/s.
        // For a side-mounted sensor, that rotation appears entirely on gyro X.
        // Expected: 45° after 1 second.
        val it = GravityAlignedYawIntegrator(gravityLpfAlpha = 1.0)
        val result = it.update(Vec3(1.0, 0.0, 0.0), Vec3(45.0, 0.0, 0.0), 1.0)
        assertNotNull(result)
        assertNear(45.0, result)
    }

    @Test
    fun resetClearsAccumulation() {
        val it = GravityAlignedYawIntegrator(gravityLpfAlpha = 1.0)
        it.update(Vec3(0.0, 0.0, 1.0), Vec3(0.0, 0.0, 30.0), 1.0)
        it.reset()
        assertEquals(0.0, it.update(Vec3(0.0, 0.0, 1.0), Vec3(0.0, 0.0, 0.0), 1.0))
    }

    @Test
    fun freeFallReadingIsSkipped() {
        val it = GravityAlignedYawIntegrator(gravityLpfAlpha = 1.0)
        // Magnitude well below the gravity floor, before any usable sample: nothing yet
        assertNull(it.update(Vec3(0.0, 0.0, 0.0), Vec3(50.0, 0.0, 0.0), 1.0))
        it.update(Vec3(0.0, 0.0, 1.0), Vec3(0.0, 0.0, 10.0), 1.0)
        // ...and after one, the yaw so far without adding the free-fall sample's rotation
        assertEquals(10.0, it.update(Vec3(0.0, 0.0, 0.0), Vec3(0.0, 0.0, 50.0), 1.0))
    }

    private fun assertNear(expected: Double, actual: Double, tolerance: Double = 0.05) {
        assertTrue(abs(expected - actual) < tolerance, "expected $expected, got $actual")
    }
}
