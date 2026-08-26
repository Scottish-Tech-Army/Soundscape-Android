package org.scottishtecharmy.soundscape.geoengine.headtracking

import org.scottishtecharmy.soundscape.geoengine.utils.circularDifferenceDegrees
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CompositeHeadphoneCalibratorTest {

    private fun composite(deviceWindow: Int = 3, courseWindow: Int = 3) = CompositeHeadphoneCalibrator(
        deviceCalibrator = HeadphoneCalibrator(CalibrationSource.Device, windowSize = deviceWindow),
        courseCalibrator = HeadphoneCalibrator(CalibrationSource.Course, windowSize = courseWindow),
    )

    @Test
    fun estimatedOffsetNullInitially() {
        assertNull(composite().estimatedOffsetDegrees)
    }

    @Test
    fun processDeviceReturnsNullUntilWindowFillsThenSetsSharedEstimate() {
        val c = composite(deviceWindow = 3)
        val offset = 30.0
        var produced: HeadphoneCalibration? = null
        for (i in 0..5) {
            val yaw = i * 12.0
            val result = c.processDevice(yaw, yaw + offset, i.toLong())
            if (result != null) {
                produced = result
                break
            }
            assertNull(c.estimatedOffsetDegrees, "No estimate should exist before the first calibration lands")
        }
        assertNotNull(produced)
        assertEquals(CalibrationSource.Device, produced.source)

        val estimate = c.estimatedOffsetDegrees
        assertNotNull(estimate)
        assertTrue(abs(circularDifferenceDegrees(estimate, offset)) < 0.5, "Expected ~$offset, got $estimate")
    }

    @Test
    fun processCourseReturnsNullUntilWindowFillsThenSetsSharedEstimate() {
        val c = composite(courseWindow = 3)
        val offset = -25.0
        var produced: HeadphoneCalibration? = null
        for (i in 0..5) {
            val yaw = i * 12.0
            val result = c.processCourse(yaw, yaw + offset, i.toLong())
            if (result != null) {
                produced = result
                break
            }
        }
        assertNotNull(produced)
        assertEquals(CalibrationSource.Course, produced.source)

        val estimate = c.estimatedOffsetDegrees
        assertNotNull(estimate)
        assertTrue(abs(circularDifferenceDegrees(estimate, offset)) < 0.5, "Expected ~$offset, got $estimate")
    }

    @Test
    fun processReturnsNullAndLeavesEstimateUntouchedWhenReferenceIsNull() {
        val c = composite(deviceWindow = 2)
        assertNull(c.processDevice(10.0, null, 0L))
        assertNull(c.estimatedOffsetDegrees)
    }

    @Test
    fun deviceAndCourseBothFeedTheSameSharedFilter() {
        val c = composite(deviceWindow = 2, courseWindow = 2)

        // Converge the device calibrator on offset A.
        val offsetA = 0.0
        for (i in 0..4) {
            val yaw = i * 10.0
            if (c.processDevice(yaw, yaw + offsetA, i.toLong()) != null) break
        }
        val afterDevice = c.estimatedOffsetDegrees
        assertNotNull(afterDevice)
        assertTrue(abs(circularDifferenceDegrees(afterDevice, offsetA)) < 0.5)

        // Now converge the course calibrator on a different offset B. Because
        // both calibrators feed the same Kalman filter, the shared estimate
        // should move from A towards B rather than resetting or ignoring it.
        val offsetB = 20.0
        for (i in 0..4) {
            val yaw = i * 10.0
            if (c.processCourse(yaw, yaw + offsetB, 1_000L + i) != null) break
        }
        val afterCourse = c.estimatedOffsetDegrees
        assertNotNull(afterCourse)
        assertNotEquals(afterDevice, afterCourse, "Course calibration should have moved the shared estimate")

        val distToBBefore = abs(circularDifferenceDegrees(afterDevice, offsetB))
        val distToBAfter = abs(circularDifferenceDegrees(afterCourse, offsetB))
        assertTrue(
            distToBAfter < distToBBefore,
            "Expected estimate to move closer to course offset $offsetB: " +
                "before=$afterDevice ($distToBBefore deg away), after=$afterCourse ($distToBAfter deg away)",
        )
    }

    @Test
    fun resetClearsEstimateAndBothCalibratorWindows() {
        val c = composite(deviceWindow = 2, courseWindow = 2)
        for (i in 0..4) {
            val yaw = i * 10.0
            if (c.processDevice(yaw, yaw + 15.0, i.toLong()) != null) break
        }
        assertNotNull(c.estimatedOffsetDegrees)

        c.reset()
        assertNull(c.estimatedOffsetDegrees)

        // A full new window (3 samples for windowSize=2) is required again after reset.
        assertNull(c.processDevice(0.0, 15.0, 100L))
        assertNull(c.processDevice(10.0, 25.0, 101L))
        assertNull(c.estimatedOffsetDegrees)
        assertNotNull(c.processDevice(20.0, 35.0, 102L))
        assertNotNull(c.estimatedOffsetDegrees)
    }
}
