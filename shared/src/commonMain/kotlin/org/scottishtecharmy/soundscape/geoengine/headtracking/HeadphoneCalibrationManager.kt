package org.scottishtecharmy.soundscape.geoengine.headtracking

import org.scottishtecharmy.soundscape.geoengine.utils.normalizeDegrees
import org.scottishtecharmy.soundscape.logging.AppLog
import org.scottishtecharmy.soundscape.platform.currentTimeMillis

private val logger = AppLog.tag("HeadCal")

/**
 * Turns a head tracker's relative yaw into a geographic heading by keeping an offset between them.
 *
 * The offset starts from the phone's compass, when the phone is held flat and both have agreed for
 * a window of samples (see [HeadphoneCalibrator]), or from the walking course, and from then on
 * every course reading keeps it up to date (see [CourseOffsetTracker]).
 */
class HeadphoneCalibrationManager(
    /**
     * Window size for the device (phone-compass) calibrator. The compass
     * updates at the IMU sample rate so each sample is genuinely new
     * information. Sized in samples, so higher-rate sensors should pass
     * a larger value — AirPods @ ~45 Hz wants ≈360 (≈8 s),
     * WitMotion @ 100 Hz wants ≈1000 (≈10 s).
     */
    deviceWindowSize: Int = 200,
    private val deviceCalibrator: HeadphoneCalibrator =
        HeadphoneCalibrator(CalibrationSource.Device, windowSize = deviceWindowSize),
    private val offsetTracker: CourseOffsetTracker = CourseOffsetTracker(),
) {
    private var active = false

    private var lastDeviceHeadingDegrees: Double? = null
    private var lastCourseDegrees: Double? = null
    private var lastLogMillis: Long = 0L

    /** Whether the offset is established enough to give a heading - see [CourseOffsetTracker.isSettled]. */
    val isCalibrated: Boolean
        get() = offsetTracker.isSettled

    /** The current yaw-to-heading offset, or null before the first reading, even if not yet settled. */
    val offsetDegrees: Double?
        get() = offsetTracker.offsetDegrees

    /** Called with each calibration as it is made, from whichever thread pushed the reference. */
    var onCalibration: ((HeadphoneCalibration) -> Unit)? = null

    fun start() {
        active = true
    }

    fun stop() {
        active = false
        deviceCalibrator.reset()
        offsetTracker.reset()
        lastDeviceHeadingDegrees = null
        lastCourseDegrees = null
    }

    fun headingFor(yawDegrees: Double): Double? {
        if (!active) return null
        val offset = if (isCalibrated) offsetDegrees else null
        val heading = offset?.let { (yawDegrees + it).normalizeDegrees() }
        logOncePerSecond(heading)
        return heading
    }

    fun pushDeviceReference(
        yawDegrees: Double,
        deviceHeadingDegrees: Double?,
        timestampMillis: Long
    ) {
        if (!active) return
        lastDeviceHeadingDegrees = deviceHeadingDegrees
        deviceCalibrator.process(yawDegrees, deviceHeadingDegrees, timestampMillis)?.let {
            offsetTracker.applyDeviceCalibration(it)
            onCalibration?.invoke(it)
        }
    }

    /** [courseAccuracyDegrees] is the receiver's bearing accuracy, which weights the reading. */
    fun pushCourseReference(
        yawDegrees: Double,
        courseDegrees: Double?,
        timestampMillis: Long,
        courseAccuracyDegrees: Double? = null,
    ) {
        if (!active) return
        // GPS course updates at ~1 Hz but this is called at the IMU rate, so
        // the same reading would otherwise be pushed ~100 times before GPS
        // ticks again. Skip duplicates so that each reading is taken once.
        if (courseDegrees == lastCourseDegrees) return
        lastCourseDegrees = courseDegrees
        offsetTracker.processCourse(yawDegrees, courseDegrees, courseAccuracyDegrees, timestampMillis)
            ?.let { onCalibration?.invoke(it) }
    }

    private fun logOncePerSecond(headingDegrees: Double?) {
        val now = currentTimeMillis()
        if (now - lastLogMillis < 1_000L) return
        lastLogMillis = now
        logger.d {
            "reference=${fmt(lastDeviceHeadingDegrees)}°" +
                " course=${fmt(lastCourseDegrees)}°" +
                " head=${fmt(headingDegrees)}°"
        }
    }

    private fun fmt(v: Double?): String = v?.let { (it * 10).toInt() / 10.0 }?.toString() ?: "—"
}
