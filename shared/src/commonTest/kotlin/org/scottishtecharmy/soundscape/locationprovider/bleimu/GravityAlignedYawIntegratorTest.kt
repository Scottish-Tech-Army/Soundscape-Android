package org.scottishtecharmy.soundscape.locationprovider.bleimu

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GravityAlignedYawIntegratorTest {

    @Test
    fun firstSampleReturnsNull() {
        val it = GravityAlignedYawIntegrator()
        assertNull(it.update(Vec3(0.0, 0.0, 1.0), Vec3(0.0, 0.0, 0.0), 0L))
    }

    @Test
    fun integratesYawRateAboutWorldUpWhenSensorIsZUp() {
        // Sensor upright (accel Z = +1g => up_in_body = +Z).
        // Rotate 90 deg/s about body Z for 1 second => 90° integrated.
        val it = GravityAlignedYawIntegrator(gravityLpfAlpha = 1.0, maxDtMillis = 10_000L)
        assertNull(it.update(Vec3(0.0, 0.0, 1.0), Vec3(0.0, 0.0, 90.0), 0L))
        val result = it.update(Vec3(0.0, 0.0, 1.0), Vec3(0.0, 0.0, 90.0), 1_000L)
        assertNotNull(result)
        assertNear(90.0, result, tolerance = 0.5)
    }

    @Test
    fun ignoresRotationAboutHorizontalAxesWhenSensorIsZUp() {
        // Rotate about body X (not gravity-aligned) => no yaw accumulation.
        val it = GravityAlignedYawIntegrator(gravityLpfAlpha = 1.0, maxDtMillis = 10_000L)
        assertNull(it.update(Vec3(0.0, 0.0, 1.0), Vec3(120.0, 0.0, 0.0), 0L))
        val result = it.update(Vec3(0.0, 0.0, 1.0), Vec3(120.0, 0.0, 0.0), 1_000L)
        assertNotNull(result)
        assertNear(0.0, result, tolerance = 0.5)
    }

    @Test
    fun mountedOnSideStillCapturesYawCorrectly() {
        // Sensor lying on its side with body +X pointing up (accel X = +1g).
        // A head turn = world-up rotation. The user rotates 45 deg/s.
        // For a side-mounted sensor, that rotation appears entirely on gyro X.
        // Expected: 45° after 1 second.
        val it = GravityAlignedYawIntegrator(gravityLpfAlpha = 1.0, maxDtMillis = 10_000L)
        assertNull(it.update(Vec3(1.0, 0.0, 0.0), Vec3(45.0, 0.0, 0.0), 0L))
        val result = it.update(Vec3(1.0, 0.0, 0.0), Vec3(45.0, 0.0, 0.0), 1_000L)
        assertNotNull(result)
        assertNear(45.0, result, tolerance = 0.5)
    }

    @Test
    fun resetClearsAccumulation() {
        val it = GravityAlignedYawIntegrator(gravityLpfAlpha = 1.0, maxDtMillis = 10_000L)
        it.update(Vec3(0.0, 0.0, 1.0), Vec3(0.0, 0.0, 30.0), 0L)
        it.update(Vec3(0.0, 0.0, 1.0), Vec3(0.0, 0.0, 30.0), 1_000L)
        it.reset()
        assertNull(it.update(Vec3(0.0, 0.0, 1.0), Vec3(0.0, 0.0, 0.0), 0L))
        val result = it.update(Vec3(0.0, 0.0, 1.0), Vec3(0.0, 0.0, 0.0), 1_000L)
        assertEquals(0.0, result)
    }

    @Test
    fun freeFallReadingDoesNotCrash() {
        val it = GravityAlignedYawIntegrator()
        // Magnitude well below the gravity floor — should just bump timestamp.
        assertNull(it.update(Vec3(0.0, 0.0, 0.0), Vec3(50.0, 0.0, 0.0), 0L))
    }

    @Test
    fun clampsExcessiveDt() {
        val it = GravityAlignedYawIntegrator(gravityLpfAlpha = 1.0, maxDtMillis = 100L)
        assertNull(it.update(Vec3(0.0, 0.0, 1.0), Vec3(0.0, 0.0, 1_000.0), 0L))
        // 10 seconds elapsed but maxDtMillis = 100 -> integrate as if 0.1s.
        // 1000 dps * 0.1s = 100 degrees.
        val result = it.update(Vec3(0.0, 0.0, 1.0), Vec3(0.0, 0.0, 1_000.0), 10_000L)
        assertNotNull(result)
        assertNear(100.0, result, tolerance = 0.5)
    }

    private fun assertNear(expected: Double, actual: Double, tolerance: Double = 0.05) {
        assertTrue(abs(expected - actual) < tolerance, "expected $expected, got $actual")
    }
}
