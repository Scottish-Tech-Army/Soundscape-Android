package org.scottishtecharmy.soundscape.geoengine.headtracking

import org.scottishtecharmy.soundscape.geoengine.utils.circularDifferenceDegrees
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HeadphoneCalibrationManagerTest {

    /** Small windows so tests converge in a handful of samples instead of 200/30. */
    private fun smallWindowManager(
        deviceWindow: Int = 3,
        courseWindow: Int = 3,
    ): HeadphoneCalibrationManager {
        val composite = CompositeHeadphoneCalibrator(
            deviceCalibrator = HeadphoneCalibrator(CalibrationSource.Device, windowSize = deviceWindow),
            courseCalibrator = HeadphoneCalibrator(CalibrationSource.Course, windowSize = courseWindow),
        )
        return HeadphoneCalibrationManager(calibrator = composite)
    }

    @Test
    fun notCalibratedBeforeAnyData() {
        val manager = smallWindowManager()
        assertFalse(manager.isCalibrated)
        assertNull(manager.headingFor(10.0))
    }

    @Test
    fun pushDeviceReferenceNoOpWhenNotStarted() {
        val manager = smallWindowManager(deviceWindow = 2)
        // Enough samples to converge if active, but start() was never called.
        for (i in 0..5) {
            val yaw = i * 10.0
            manager.pushDeviceReference(yaw, yaw + 30.0, i.toLong())
        }
        assertFalse(manager.isCalibrated)
        assertNull(manager.headingFor(10.0))
    }

    @Test
    fun startThenDeviceReferencesCalibrateAndHeadingForAppliesOffset() {
        val manager = smallWindowManager(deviceWindow = 3)
        manager.start()
        val offset = 47.0
        for (i in 0..5) {
            val yaw = i * 15.0
            manager.pushDeviceReference(yaw, yaw + offset, i.toLong())
        }
        assertTrue(manager.isCalibrated)

        val heading = manager.headingFor(10.0)
        assertNotNull(heading)
        val expected = 10.0 + offset
        assertTrue(
            abs(circularDifferenceDegrees(heading, expected)) < 0.5,
            "Expected heading ~$expected, got $heading",
        )
    }

    @Test
    fun stopResetsCalibrationAndDeactivatesFurtherPushes() {
        val manager = smallWindowManager(deviceWindow = 3)
        manager.start()
        val offset = 47.0
        for (i in 0..5) {
            val yaw = i * 15.0
            manager.pushDeviceReference(yaw, yaw + offset, i.toLong())
        }
        assertTrue(manager.isCalibrated)

        manager.stop()
        assertFalse(manager.isCalibrated)
        assertNull(manager.headingFor(10.0))

        // Pushes after stop() are no-ops (active == false) until start() again.
        manager.pushDeviceReference(0.0, offset, 100L)
        assertFalse(manager.isCalibrated)
    }

    @Test
    fun restartingAfterStopRequiresFreshConvergence() {
        val manager = smallWindowManager(deviceWindow = 3)
        manager.start()
        val offset = 47.0
        for (i in 0..5) {
            val yaw = i * 15.0
            manager.pushDeviceReference(yaw, yaw + offset, i.toLong())
        }
        assertTrue(manager.isCalibrated)
        manager.stop()

        manager.start()
        assertFalse(manager.isCalibrated, "A fresh start() should not carry over the previous calibration")
        assertNull(manager.headingFor(10.0))
    }

    @Test
    fun pushCourseReferenceIgnoresRepeatedIdenticalValues() {
        val manager = smallWindowManager(courseWindow = 2)
        manager.start()
        // Same course value pushed repeatedly (as would happen between ~1 Hz
        // GPS ticks while this is called at IMU rate) should count as a
        // single sample, so calibration never completes.
        repeat(20) { i ->
            manager.pushCourseReference(yawDegrees = 5.0, courseDegrees = 90.0, timestampMillis = i.toLong())
        }
        assertFalse(manager.isCalibrated)
    }

    @Test
    fun pushCourseReferenceCalibratesOnDistinctValues() {
        val manager = smallWindowManager(courseWindow = 2)
        manager.start()
        val offset = -20.0
        for (i in 0..5) {
            val yaw = i * 10.0
            manager.pushCourseReference(yaw, yaw + offset, i.toLong())
        }
        assertTrue(manager.isCalibrated)
    }

    @Test
    fun pushCourseReferenceNoOpWhenNotActive() {
        val manager = smallWindowManager(courseWindow = 2)
        for (i in 0..5) {
            manager.pushCourseReference(i * 10.0, i * 10.0 + 15.0, i.toLong())
        }
        assertFalse(manager.isCalibrated)
    }

    @Test
    fun nullReferenceClearsPendingWindow() {
        val manager = smallWindowManager(deviceWindow = 3)
        manager.start()
        val offset = 47.0
        // Fill most of the window.
        manager.pushDeviceReference(0.0, offset, 0L)
        manager.pushDeviceReference(10.0, 10.0 + offset, 1L)
        // Dropping the reference clears the buffer.
        manager.pushDeviceReference(20.0, null, 2L)
        assertFalse(manager.isCalibrated)

        // Needs a fresh full window (4 samples for windowSize=3) after the drop.
        manager.pushDeviceReference(0.0, offset, 3L)
        manager.pushDeviceReference(10.0, 10.0 + offset, 4L)
        manager.pushDeviceReference(20.0, 20.0 + offset, 5L)
        assertFalse(manager.isCalibrated)
        manager.pushDeviceReference(30.0, 30.0 + offset, 6L)
        assertTrue(manager.isCalibrated)
    }
}
