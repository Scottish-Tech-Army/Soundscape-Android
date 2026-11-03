package org.scottishtecharmy.soundscape.geoengine.headtracking

import org.scottishtecharmy.soundscape.geoengine.utils.normalizeDegrees
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Keeps the offset from a head tracker's relative yaw to a geographic heading up to date with
 * every GPS course reading, as a one-dimensional Kalman filter on the offset itself.
 *
 * The offset is what ought to stay constant - the course changes at every corner, and filtering
 * it would lag each turn and turn that lag into an offset error - so each course reading is a
 * measurement of `course - yaw`, weighted by the receiver's bearing accuracy. Between readings the
 * offset's variance grows by [driftVariancePerSecond], for gyro drift.
 *
 * Compared with waiting for a window of readings that all agree (see [HeadphoneCalibrator]), this
 * corrects a wrong start within a few tens of seconds of walking, rather than whenever a window
 * happens to pass. On a recorded WitMotion walk the window approach left the heading about 45° from
 * the walking direction on average; this left it about 20°.
 *
 * Readings it can't trust are skipped: a bearing accuracy worse than [maxCourseAccuracyDegrees],
 * and anything while turning - the course or the yaw having moved [maxTurnDegrees] or more since the
 * previous reading - because GPS course lags a turn, and the head usually leads it.
 */
class CourseOffsetTracker(
    private val driftVariancePerSecond: Double = 4.0,
    private val maxCourseAccuracyDegrees: Double = 60.0,
    private val maxTurnDegrees: Double = 20.0,
    /**
     * A calibration against the phone's compass agrees with itself very closely, but that says
     * nothing about whether the phone pointed the way the user faced. Its accuracy is taken as at
     * least this, so the walking course can soon correct it.
     */
    private val deviceAccuracyFloorDegrees: Double = 20.0,
) {
    var offsetDegrees: Double? = null
        private set

    /** The offset's current uncertainty, as a standard deviation in degrees. */
    val accuracyDegrees: Double?
        get() = if (offsetDegrees == null) null else sqrt(variance)

    private var variance = 0.0
    private var lastTimestampMillis = 0L
    private var previousCourse: Double? = null
    private var previousYaw = 0.0

    fun reset() {
        offsetDegrees = null
        variance = 0.0
        lastTimestampMillis = 0L
        previousCourse = null
    }

    /** Takes in a calibration against the phone's compass. */
    fun applyDeviceCalibration(calibration: HeadphoneCalibration) {
        update(
            calibration.offsetDegrees,
            max(calibration.accuracyDegrees, deviceAccuracyFloorDegrees),
            calibration.timestampMillis,
        )
    }

    /**
     * Takes in one course reading, or null when there isn't one. Returns the calibration it made,
     * or null if the reading was skipped.
     */
    fun processCourse(
        yawDegrees: Double,
        courseDegrees: Double?,
        courseAccuracyDegrees: Double?,
        timestampMillis: Long,
    ): HeadphoneCalibration? {
        val previous = previousCourse
        val previousYawDegrees = previousYaw
        previousCourse = courseDegrees
        previousYaw = yawDegrees
        if (courseDegrees == null || previous == null) return null
        val accuracy = courseAccuracyDegrees ?: UNKNOWN_COURSE_ACCURACY_DEGREES
        if (accuracy > maxCourseAccuracyDegrees) return null
        if (abs(angleBetween(courseDegrees, previous)) >= maxTurnDegrees) return null
        if (abs(yawDegrees - previousYawDegrees) >= maxTurnDegrees) return null

        val measured = (courseDegrees - yawDegrees).normalizeDegrees()
        val measurementAccuracy = max(accuracy, MIN_COURSE_ACCURACY_DEGREES)
        update(measured, measurementAccuracy, timestampMillis)
        return HeadphoneCalibration(
            offsetDegrees = measured,
            accuracyDegrees = measurementAccuracy,
            timestampMillis = timestampMillis,
            source = CalibrationSource.Course,
        )
    }

    private fun update(measuredDegrees: Double, accuracyDegrees: Double, timestampMillis: Long) {
        val measurementVariance = accuracyDegrees * accuracyDegrees
        val current = offsetDegrees
        if (current == null) {
            offsetDegrees = measuredDegrees.normalizeDegrees()
            variance = measurementVariance
            lastTimestampMillis = timestampMillis
            return
        }
        val elapsedSeconds = (timestampMillis - lastTimestampMillis).coerceAtLeast(0L) / 1000.0
        variance += driftVariancePerSecond * elapsedSeconds
        lastTimestampMillis = timestampMillis

        val gain = variance / (variance + measurementVariance)
        offsetDegrees = (current + gain * angleBetween(measuredDegrees, current)).normalizeDegrees()
        variance *= 1.0 - gain
    }

    /** [a] - [b], the short way round, in -180..180. */
    private fun angleBetween(a: Double, b: Double): Double = ((a - b) % 360.0 + 540.0) % 360.0 - 180.0

    private companion object {
        // What a reading with no reported accuracy is taken as: a middling GPS course
        const val UNKNOWN_COURSE_ACCURACY_DEGREES = 45.0
        // No reading is trusted more than this, however sure the receiver claims to be
        const val MIN_COURSE_ACCURACY_DEGREES = 5.0
    }
}
