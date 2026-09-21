package org.scottishtecharmy.soundscape.geoengine.filters

import org.scottishtecharmy.soundscape.geoengine.utils.circularDifferenceDegrees
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class KalmanCircularFilterTest {

    @Test
    fun convergesOnConstantInput() {
        val filter = KalmanCircularFilter(filterSigma = 1.0)
        val target = 47.0
        var last = Double.NaN
        for (i in 0..200) {
            last =
                filter.process(angleDegrees = target, timestamp = i * 100L, accuracyDegrees = 1.0)
        }
        val diff = abs(circularDifferenceDegrees(last, target))
        assertTrue(diff < 0.5, "Expected ~$target, got $last (diff=$diff)")
    }

    @Test
    fun handlesAcrossZeroBoundary() {
        val filter = KalmanCircularFilter(filterSigma = 1.0)
        val samples = listOf(359.0, 0.0, 1.0, 359.0, 0.0, 1.0)
        var last = Double.NaN
        samples.forEachIndexed { i, v ->
            last = filter.process(v, (i * 100).toLong(), 1.0)
        }
        // Should produce something near 0 (not 180)
        val diff = abs(circularDifferenceDegrees(last, 0.0))
        assertTrue(diff < 5.0, "Expected near 0°, got $last (diff=$diff)")
    }
}
