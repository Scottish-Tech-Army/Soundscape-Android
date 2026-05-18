package org.scottishtecharmy.soundscape.locationprovider.bleimu

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.geoengine.headtracking.HeadphoneCalibrationManager
import org.scottishtecharmy.soundscape.locationprovider.DirectionProvider
import org.scottishtecharmy.soundscape.locationprovider.HeadHeading
import org.scottishtecharmy.soundscape.locationprovider.HeadTrackingProvider
import org.scottishtecharmy.soundscape.locationprovider.HeadTrackingStatus
import org.scottishtecharmy.soundscape.locationprovider.LocationProvider
import org.scottishtecharmy.soundscape.platform.currentTimeMillis
import kotlin.concurrent.Volatile

/**
 * HeadTrackingProvider backed by an external BLE IMU (currently the WitMotion
 * WT9011DCL). Mirrors [org.scottishtecharmy.soundscape.locationprovider.IosHeadTrackingProvider]:
 * yaw from the IMU is fed into [HeadphoneCalibrationManager] alongside the
 * phone's compass + walking course, which estimates a constant offset that
 * converts the relative sensor yaw into a geographic heading.
 */
class BleImuHeadTrackingProvider(
    private val directionProvider: DirectionProvider,
    private val locationProvider: LocationProvider,
    private val client: BleImuClient = BleImuClient(),
) : HeadTrackingProvider() {

    private val parser = WitMotionFrameParser(onRegisterResponse = ::onRegisterResponse)
    // The WT streams at 100 Hz, so the device calibrator needs a ~10 s window
    // of samples to be as forgiving of brief look-aways as the iOS AirPods
    // path (which gets ~8 s from its 200-sample default at ~25 Hz).
    private val calibrationManager = HeadphoneCalibrationManager(deviceWindowSize = 1_000)
    private val yawIntegrator = GravityAlignedYawIntegrator()

    private val _batteryVoltageFlow = MutableStateFlow<Float?>(null)
    /** Most recent battery voltage in volts, or null before the first reading. */
    val batteryVoltageFlow: StateFlow<Float?> = _batteryVoltageFlow

    private var scope: CoroutineScope? = null

    @Volatile
    private var lastDeviceHeadingDegrees: Double? = null

    @Volatile
    private var lastCourseDegrees: Double? = null

    @Volatile
    private var lastCourseTimestampMillis: Long = 0L

    override fun start() {
        if (scope != null) return
        val newScope = CoroutineScope(Dispatchers.Default + Job())
        scope = newScope
        calibrationManager.start()
        parser.reset()
        yawIntegrator.reset()
        mutableStatusFlow.value = HeadTrackingStatus.Disconnected

        newScope.launch { observeDeviceHeading() }
        newScope.launch { observeCourse() }
        newScope.launch { runBleLoop() }
    }

    override fun stop() {
        scope?.cancel()
        scope = null
        calibrationManager.stop()
        parser.reset()
        yawIntegrator.reset()
        lastDeviceHeadingDegrees = null
        lastCourseDegrees = null
        lastCourseTimestampMillis = 0L
        mutableHeadHeadingFlow.value = null
        mutableStatusFlow.value = HeadTrackingStatus.Inactive
        _batteryVoltageFlow.value = null
    }

    private fun onRegisterResponse(response: WitMotionRegisterResponse) {
        if (response.address == BleImuClient.REG_BATTERY_VOLTAGE) {
            // Register units are 0.01 V (e.g. 410 -> 4.10 V).
            _batteryVoltageFlow.value = response.rawValue / 100.0f
        }
    }

    private suspend fun observeDeviceHeading() {
        directionProvider.orientationFlow.collect { dir ->
            lastDeviceHeadingDegrees =
                if (dir != null && dir.headingAccuracyDegrees >= 0f) dir.headingDegrees.toDouble()
                else null
        }
    }

    private suspend fun observeCourse() {
        locationProvider.filteredLocationFlow.collect { loc ->
            val usable = loc != null &&
                loc.hasBearing &&
                loc.hasSpeed &&
                loc.speed >= COURSE_MIN_SPEED_MPS
            if (usable) {
                lastCourseDegrees = loc!!.bearing.toDouble()
                lastCourseTimestampMillis = currentTimeMillis()
            } else {
                lastCourseDegrees = null
            }
        }
    }

    private suspend fun runBleLoop() {
        while (true) {
            try {
                client.runSession(
                    onConnected = {
                        mutableStatusFlow.value = HeadTrackingStatus.Connected
                        // Restart calibration so the offset doesn't carry over
                        // from a previous session with a different mounting.
                        calibrationManager.stop()
                        calibrationManager.start()
                        parser.reset()
                        yawIntegrator.reset()
                    },
                    onBytes = { bytes -> onBytes(bytes) },
                )
            } catch (ce: kotlin.coroutines.cancellation.CancellationException) {
                throw ce
            } catch (_: Throwable) {
                // Scan failed / connection dropped / GATT error — wait and retry.
                mutableHeadHeadingFlow.value = null
                mutableStatusFlow.value = HeadTrackingStatus.Disconnected
                delay(RECONNECT_DELAY_MILLIS)
            }
        }
    }

    private fun onBytes(bytes: ByteArray) {
        val now = currentTimeMillis()
        val samples = parser.consume(bytes, now)
        for (sample in samples) {
            processSample(sample, now)
        }
    }

    private fun processSample(sample: WitMotionSample, nowMillis: Long) {
        // Don't use sample.yawDegrees — it's rotation about the sensor's body
        // Z axis, which is only useful when the sensor happens to be mounted
        // Z-up. Instead derive yaw rate about the world-up axis from
        // (gyro · gravity_in_body) and integrate. Drift is corrected by the
        // calibration manager's correlation against compass and walking course.
        val integrated = yawIntegrator.update(sample.accelG, sample.gyroDps, nowMillis)
            ?: return
        // Integrator produces CCW-positive about world-up (right-hand rule).
        // Compass heading is CW-positive, so negate before the calibrator.
        val yawDegrees = -integrated

        calibrationManager.pushDeviceReference(
            yawDegrees,
            lastDeviceHeadingDegrees,
            nowMillis,
        )

        val course = lastCourseDegrees
        val courseFresh = course != null &&
            (nowMillis - lastCourseTimestampMillis) <= COURSE_STALENESS_MILLIS
        calibrationManager.pushCourseReference(
            yawDegrees,
            if (courseFresh) course else null,
            nowMillis,
        )

        val heading = calibrationManager.headingFor(yawDegrees) ?: return
        mutableHeadHeadingFlow.value = HeadHeading(
            degrees = heading,
            accuracyDegrees = REPORTED_ACCURACY_DEGREES,
            timestampMillis = nowMillis,
        )
        if (mutableStatusFlow.value == HeadTrackingStatus.Connected) {
            mutableStatusFlow.value = HeadTrackingStatus.Calibrated
        }
    }

    companion object {
        // Same gating values the iOS AirPods provider uses, for consistent UX.
        private const val COURSE_MIN_SPEED_MPS = 0.4f
        private const val COURSE_STALENESS_MILLIS = 3_000L

        private const val REPORTED_ACCURACY_DEGREES = 10.0
        private const val RECONNECT_DELAY_MILLIS = 2_000L
    }
}
