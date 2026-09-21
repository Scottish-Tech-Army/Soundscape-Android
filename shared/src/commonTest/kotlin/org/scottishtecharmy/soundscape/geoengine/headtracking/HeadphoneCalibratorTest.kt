package org.scottishtecharmy.soundscape.geoengine.headtracking

import org.scottishtecharmy.soundscape.geoengine.utils.circularDifferenceDegrees
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HeadphoneCalibratorTest {

    @Test
    fun returnsNullBeforeWindowFills() {
        val cal = HeadphoneCalibrator(CalibrationSource.Device, windowSize = 200)
        for (i in 0 until 200) {
            val result = cal.process(
                yawDegrees = 10.0,
                referenceDegrees = 57.0,
                timestampMillis = i.toLong()
            )
            assertNull(result, "Should be null at sample $i")
        }
    }

    @Test
    fun convergesOnConstantOffset() {
        val cal = HeadphoneCalibrator(CalibrationSource.Device, windowSize = 200)
        val offset = 47.0
        var produced: HeadphoneCalibration? = null
        for (i in 0..220) {
            val yaw = (i * 0.5) // sweeping yaw
            val ref = (yaw + offset)
            val result = cal.process(yaw, ref, i.toLong())
            if (result != null) {
                produced = result; break
            }
        }
        assertNotNull(produced)
        val diff = abs(circularDifferenceDegrees(produced.offsetDegrees, offset))
        assertTrue(
            diff < 0.5,
            "Expected offset ~$offset, got ${produced.offsetDegrees} (diff=$diff)"
        )
    }

    @Test
    fun rejectsNoisyWindow() {
        val cal = HeadphoneCalibrator(
            CalibrationSource.Device,
            windowSize = 200,
            stdDevGateDegrees = 10.0
        )
        // User spinning their head: yaw varies wildly, reference fixed
        for (i in 0..220) {
            val yaw = (i * 17.0) % 360.0
            val ref = 90.0
            val result = cal.process(yaw, ref, i.toLong())
            assertNull(result, "Should reject high-stddev window at sample $i")
        }
    }

    @Test
    fun resetsBufferOnNullReference() {
        val cal = HeadphoneCalibrator(CalibrationSource.Device, windowSize = 200)
        // Fill almost the whole window
        for (i in 0 until 199) {
            cal.process(10.0, 57.0, i.toLong())
        }
        // Drop the reference — buffer should clear
        cal.process(10.0, null, 200)
        // After reset we need a full new window before any output. 200 samples produce no output yet.
        for (i in 201..400) {
            val result = cal.process(10.0, 57.0, i.toLong())
            assertNull(result, "Still filling new window at $i")
        }
        // Sample 401 closes the window and should produce a calibration
        val result = cal.process(10.0, 57.0, 401L)
        assertNotNull(result)
    }
}
