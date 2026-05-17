package org.scottishtecharmy.soundscape.locationprovider.bleimu

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
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

    private val parser = WitMotionFrameParser()
    private val calibrationManager = HeadphoneCalibrationManager()

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
        lastDeviceHeadingDegrees = null
        lastCourseDegrees = null
        lastCourseTimestampMillis = 0L
        mutableHeadHeadingFlow.value = null
        mutableStatusFlow.value = HeadTrackingStatus.Inactive
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
        // WitMotion follows the right-hand rule with Z up, so its yaw increases
        // counter-clockwise looking down. Compass heading increases clockwise,
        // so negate before handing off — without this the spatial audio rotates
        // the opposite way to head movement.
        val yawDegrees = -sample.yawDegrees

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
