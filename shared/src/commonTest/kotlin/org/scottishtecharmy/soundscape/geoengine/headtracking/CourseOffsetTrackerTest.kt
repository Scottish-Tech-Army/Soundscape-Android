package org.scottishtecharmy.soundscape.geoengine.headtracking

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CourseOffsetTrackerTest {

    private fun deviceCalibration(offset: Double, timestampMillis: Long = 0L) =
        HeadphoneCalibration(offset, 1.0, timestampMillis, CalibrationSource.Device)

    /** Feeds a straight walk: yaw steady, course giving [trueOffset], one reading a second. */
    private fun CourseOffsetTracker.walk(trueOffset: Double, seconds: Int, accuracy: Double? = 25.0, from: Long = 1_000L) {
        for (i in 0 until seconds) {
            val yaw = 40.0 + (i % 2) // a new course value every reading
            processCourse(yaw, (yaw + trueOffset) % 360.0, accuracy, from + i * 1_000L)
        }
    }

    private fun assertNear(expected: Double, actual: Double?, tolerance: Double) {
        assertNotNull(actual)
        val difference = abs(((actual - expected) % 360.0 + 540.0) % 360.0 - 180.0)
        assertTrue(difference <= tolerance, "expected $expected ± $tolerance, was $actual")
    }

    @Test
    fun aWrongCompassCalibrationIsCorrectedByWalking() {
        val tracker = CourseOffsetTracker()
        tracker.applyDeviceCalibration(deviceCalibration(320.0))
        assertEquals(320.0, tracker.offsetDegrees)

        tracker.walk(trueOffset = 270.0, seconds = 30)

        assertNear(270.0, tracker.offsetDegrees, 5.0)
    }

    @Test
    fun walkingAloneGivesAnOffsetFromTheSecondReadingButOnlySettlesAfterTen() {
        val tracker = CourseOffsetTracker()
        assertNull(tracker.processCourse(40.0, 310.0, 25.0, 0L))
        assertNull(tracker.offsetDegrees)
        assertNotNull(tracker.processCourse(41.0, 311.0, 25.0, 1_000L))
        assertNear(270.0, tracker.offsetDegrees, 0.1)
        assertFalse(tracker.isSettled, "one reading could be a glance to the side")

        for (i in 2..10) tracker.processCourse(40.0 + i % 2, 310.0 + i % 2, 25.0, i * 1_000L)
        assertTrue(tracker.isSettled)
    }

    @Test
    fun aCompassCalibrationSettlesAtOnce() {
        val tracker = CourseOffsetTracker()
        tracker.applyDeviceCalibration(deviceCalibration(320.0))
        assertTrue(tracker.isSettled)
    }

    @Test
    fun aYawThatWrapsIsNotATurn() {
        // Bose Frames report yaw in -180..180: walking straight with the head near the seam
        val tracker = CourseOffsetTracker()
        tracker.applyDeviceCalibration(deviceCalibration(0.0))
        for (i in 0 until 30) {
            val yaw = if (i % 2 == 0) 179.0 else -179.0
            val reading = tracker.processCourse(yaw, (yaw + 270.0 + 360.0) % 360.0, 25.0, i * 1_000L)
            // The first only has nothing to compare against
            if (i > 0) assertNotNull(reading, "reading $i was taken as a turn")
        }
        assertNear(270.0, tracker.offsetDegrees, 5.0)
    }

    @Test
    fun readingsWhileTurningAreSkipped() {
        val tracker = CourseOffsetTracker()
        tracker.applyDeviceCalibration(deviceCalibration(270.0))
        tracker.processCourse(40.0, 310.0, 25.0, 1_000L)
        // The course swings round a corner faster than the head tracker's yaw follows it
        assertNull(tracker.processCourse(40.0, 340.0, 25.0, 2_000L))
        assertNull(tracker.processCourse(80.0, 345.0, 25.0, 3_000L))
        assertEquals(270.0, tracker.offsetDegrees)
    }

    @Test
    fun inaccurateCoursesAreSkipped() {
        val tracker = CourseOffsetTracker()
        tracker.applyDeviceCalibration(deviceCalibration(270.0))
        tracker.walk(trueOffset = 180.0, seconds = 30, accuracy = 90.0)
        assertEquals(270.0, tracker.offsetDegrees)
    }

    @Test
    fun theOffsetWrapsTheShortWayRound() {
        val tracker = CourseOffsetTracker()
        tracker.applyDeviceCalibration(deviceCalibration(350.0))
        tracker.walk(trueOffset = 10.0, seconds = 30)
        assertNear(10.0, tracker.offsetDegrees, 5.0)
    }

    @Test
    fun resetForgetsTheOffset() {
        val tracker = CourseOffsetTracker()
        tracker.applyDeviceCalibration(deviceCalibration(320.0))
        tracker.reset()
        assertNull(tracker.offsetDegrees)
        assertNull(tracker.accuracyDegrees)
    }
}
