package org.scottishtecharmy.soundscape.geoengine.headtracking

import org.scottishtecharmy.soundscape.geoengine.utils.normalizeDegrees
import org.scottishtecharmy.soundscape.platform.currentTimeMillis

class HeadphoneCalibrationManager(
    /**
     * Window size for the device (phone-compass) calibrator. The compass
     * updates at the IMU sample rate so each sample is genuinely new
     * information. Sized in samples, so higher-rate sensors should pass
     * a larger value — AirPods @ ~25 Hz uses the 200 default (≈8 s),
     * WitMotion @ 100 Hz wants ≈1000 (≈10 s).
     */
    deviceWindowSize: Int = 200,
    /**
     * Window size for the course (GPS-bearing) calibrator. GPS updates at
     * ~1 Hz regardless of IMU rate, and only **new** course values are
     * accepted (see [pushCourseReference]), so this is in real GPS readings.
     * Default 30 ≈ 30 s of walking before the first course-based offset.
     */
    courseWindowSize: Int = 30,
    private val calibrator: CompositeHeadphoneCalibrator = CompositeHeadphoneCalibrator(
        deviceCalibrator = HeadphoneCalibrator(CalibrationSource.Device, windowSize = deviceWindowSize),
        courseCalibrator = HeadphoneCalibrator(CalibrationSource.Course, windowSize = courseWindowSize),
    ),
) {
    private var active = false

    private var lastDeviceHeadingDegrees: Double? = null
    private var lastCourseDegrees: Double? = null
    private var lastLogMillis: Long = 0L

    val isCalibrated: Boolean
        get() = calibrator.estimatedOffsetDegrees != null

    fun start() {
        active = true
    }

    fun stop() {
        active = false
        calibrator.reset()
        lastDeviceHeadingDegrees = null
        lastCourseDegrees = null
    }

    fun headingFor(yawDegrees: Double): Double? {
        if (!active) return null
        val offset = calibrator.estimatedOffsetDegrees
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
        calibrator.processDevice(yawDegrees, deviceHeadingDegrees, timestampMillis)
    }

    fun pushCourseReference(yawDegrees: Double, courseDegrees: Double?, timestampMillis: Long) {
        if (!active) return
        // GPS course updates at ~1 Hz but this is called at the IMU rate, so
        // the same reading would otherwise be pushed ~100 times before GPS
        // ticks again. Skip duplicates so each calibrator sample corresponds
        // to one genuinely new course observation; the window size is sized
        // in those units, not in IMU samples.
        if (courseDegrees == lastCourseDegrees) return
        lastCourseDegrees = courseDegrees
        calibrator.processCourse(yawDegrees, courseDegrees, timestampMillis)
    }

    private fun logOncePerSecond(headingDegrees: Double?) {
        val now = currentTimeMillis()
        if (now - lastLogMillis < 1_000L) return
        lastLogMillis = now
        println(
            "[HeadCal] reference=${fmt(lastDeviceHeadingDegrees)}°" +
                " course=${fmt(lastCourseDegrees)}°" +
                " head=${fmt(headingDegrees)}°"
        )
    }

    private fun fmt(v: Double?): String = v?.let { (it * 10).toInt() / 10.0 }?.toString() ?: "—"
}
