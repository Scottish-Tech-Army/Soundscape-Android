package org.scottishtecharmy.soundscape.geoengine.utils

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CircularStatisticsTest {

    @Test
    fun normalizeDegreesWraps() {
        assertEquals(0.0, 0.0.normalizeDegrees())
        assertEquals(0.0, 360.0.normalizeDegrees())
        assertEquals(10.0, 370.0.normalizeDegrees())
        assertEquals(350.0, (-10.0).normalizeDegrees())
        assertEquals(180.0, (-180.0).normalizeDegrees())
    }

    @Test
    fun circularDifferenceHandlesWraparound() {
        assertNear(2.0, circularDifferenceDegrees(1.0, 359.0))
        assertNear(-2.0, circularDifferenceDegrees(359.0, 1.0))
        assertNear(0.0, circularDifferenceDegrees(180.0, 180.0))
        assertNear(180.0, abs(circularDifferenceDegrees(0.0, 180.0)))
    }

    @Test
    fun circularMeanAcrossZeroBoundary() {
        val mean = listOf(359.0, 1.0).circularMeanDegrees()
        assertNotNull(mean)
        // Mean of 359 and 1 is 0, not 180
        val diff = abs(circularDifferenceDegrees(mean, 0.0))
        assertTrue(diff < 0.01, "Expected ~0°, got $mean (diff=$diff)")
    }

    @Test
    fun circularMeanOfConstantStream() {
        val samples = List(50) { 47.0 }
        val mean = samples.circularMeanDegrees()
        assertNotNull(mean)
        assertNear(47.0, mean)
    }

    @Test
    fun circularStdDevOfConstantStreamIsZero() {
        val samples = List(50) { 47.0 }
        val stddev = samples.circularStdDevDegrees()
        assertNotNull(stddev)
        assertTrue(stddev < 0.01, "Expected ~0, got $stddev")
    }

    @Test
    fun circularStdDevAcrossZeroBoundary() {
        // Tight cluster straddling 0/360
        val samples = listOf(359.0, 0.0, 1.0)
        val stddev = samples.circularStdDevDegrees()
        assertNotNull(stddev)
        assertTrue(stddev < 5.0, "Expected small stddev, got $stddev")
    }

    private fun assertNear(expected: Double, actual: Double, tol: Double = 0.001) {
        assertTrue(abs(expected - actual) < tol, "Expected $expected, got $actual")
    }
}
