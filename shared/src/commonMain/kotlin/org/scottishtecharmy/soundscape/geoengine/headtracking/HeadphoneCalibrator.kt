package org.scottishtecharmy.soundscape.geoengine.headtracking

import org.scottishtecharmy.soundscape.geoengine.utils.circularDifferenceDegrees
import org.scottishtecharmy.soundscape.geoengine.utils.circularMeanDegrees
import org.scottishtecharmy.soundscape.geoengine.utils.circularStdDevDegrees
import org.scottishtecharmy.soundscape.geoengine.utils.normalizeDegrees

enum class CalibrationSource { Device, Course }

data class HeadphoneCalibration(
    val offsetDegrees: Double,
    val accuracyDegrees: Double,
    val timestampMillis: Long,
    val source: CalibrationSource,
)

class HeadphoneCalibrator(
    val source: CalibrationSource,
    private val windowSize: Int = 200,
    private val stdDevGateDegrees: Double = 10.0,
) {
    private data class Sample(val referenceDegrees: Double, val yawDegrees: Double)

    private val samples = ArrayDeque<Sample>(windowSize + 1)

    fun reset() {
        samples.clear()
    }

    fun process(
        yawDegrees: Double,
        referenceDegrees: Double?,
        timestampMillis: Long,
    ): HeadphoneCalibration? {
        if (referenceDegrees == null) {
            samples.clear()
            return null
        }

        samples.addLast(
            Sample(referenceDegrees.normalizeDegrees(), yawDegrees.normalizeDegrees())
        )
        if (samples.size <= windowSize) {
            return null
        }
        samples.removeFirst()

        val differences =
            samples.map { circularDifferenceDegrees(it.referenceDegrees, it.yawDegrees) }
        val stdev = differences.circularStdDevDegrees() ?: return null
        if (stdev >= stdDevGateDegrees) return null
        val mean = differences.circularMeanDegrees() ?: return null

        samples.clear()
        return HeadphoneCalibration(
            offsetDegrees = mean,
            accuracyDegrees = stdev,
            timestampMillis = timestampMillis,
            source = source,
        )
    }
}
