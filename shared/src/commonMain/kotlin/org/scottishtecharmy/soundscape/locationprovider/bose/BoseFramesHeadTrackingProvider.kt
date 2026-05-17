package org.scottishtecharmy.soundscape.locationprovider.bose

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
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
 * HeadTrackingProvider backed by Bose Frames AR glasses.
 *
 * Mirrors the WitMotion provider:
 *   1. Bose's quaternion → yaw (degrees, sensor frame, drifts from boot)
 *   2. [HeadphoneCalibrationManager] correlates the yaw with the phone's
 *      compass + walking course to estimate a constant offset
 *   3. Calibrated geographic heading is published on [headHeadingFlow]
 *
 * The earlier proof-of-concept required the glasses to be powered on facing
 * north before app launch — the calibration manager replaces that with the
 * same auto-calibration the iOS AirPods provider already uses.
 */
class BoseFramesHeadTrackingProvider(
    private val directionProvider: DirectionProvider,
    private val locationProvider: LocationProvider,
    private val client: BoseFramesClient = BoseFramesClient(),
) : HeadTrackingProvider() {

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
        mutableStatusFlow.value = HeadTrackingStatus.Disconnected

        newScope.launch { observeDeviceHeading() }
        newScope.launch { observeCourse() }
        newScope.launch { runBleLoop() }
    }

    override fun stop() {
        scope?.cancel()
        scope = null
        calibrationManager.stop()
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
                        // Restart calibration so the offset is re-learned for
                        // this session — the glasses orient yaw=0 wherever they
                        // happen to be when powered on.
                        calibrationManager.stop()
                        calibrationManager.start()
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
        val sample = BoseFramesDataParser.parse(bytes, now) ?: return
        processSample(sample.yawDegrees, now)
    }

    private fun processSample(rawYawDegrees: Double, nowMillis: Long) {
        // Bose Frames quaternion → yaw uses the right-hand rule (CCW positive
        // looking down from above). Compass heading increases clockwise, so
        // negate before handing off — matches the WitMotion provider's choice.
        val yawDegrees = -rawYawDegrees

        calibrationManager.pushDeviceReference(yawDegrees, lastDeviceHeadingDegrees, nowMillis)

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
        private const val COURSE_MIN_SPEED_MPS = 0.4f
        private const val COURSE_STALENESS_MILLIS = 3_000L

        private const val REPORTED_ACCURACY_DEGREES = 10.0
        private const val RECONNECT_DELAY_MILLIS = 2_000L
    }
}
