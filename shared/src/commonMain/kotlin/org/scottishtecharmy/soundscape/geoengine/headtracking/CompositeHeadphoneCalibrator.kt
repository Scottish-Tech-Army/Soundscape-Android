package org.scottishtecharmy.soundscape.geoengine.headtracking

import org.scottishtecharmy.soundscape.geoengine.filters.KalmanCircularFilter
import kotlin.concurrent.Volatile

class CompositeHeadphoneCalibrator(
    private val deviceCalibrator: HeadphoneCalibrator = HeadphoneCalibrator(CalibrationSource.Device),
    private val courseCalibrator: HeadphoneCalibrator = HeadphoneCalibrator(CalibrationSource.Course),
    private val filter: KalmanCircularFilter = KalmanCircularFilter(filterSigma = 1.0),
) {
    @Volatile
    private var lastEstimate: Double? = null

    val estimatedOffsetDegrees: Double?
        get() = lastEstimate

    fun reset() {
        deviceCalibrator.reset()
        courseCalibrator.reset()
        filter.reset()
        lastEstimate = null
    }

    fun processDevice(
        yawDegrees: Double,
        deviceHeadingDegrees: Double?,
        timestampMillis: Long,
    ): HeadphoneCalibration? {
        val cal = deviceCalibrator.process(yawDegrees, deviceHeadingDegrees, timestampMillis)
            ?: return null
        applyToFilter(cal)
        return cal
    }

    fun processCourse(
        yawDegrees: Double,
        courseDegrees: Double?,
        timestampMillis: Long,
    ): HeadphoneCalibration? {
        val cal =
            courseCalibrator.process(yawDegrees, courseDegrees, timestampMillis) ?: return null
        applyToFilter(cal)
        return cal
    }

    private fun applyToFilter(calibration: HeadphoneCalibration) {
        lastEstimate = filter.process(
            angleDegrees = calibration.offsetDegrees,
            timestamp = calibration.timestampMillis,
            accuracyDegrees = calibration.accuracyDegrees,
        )
    }
}
