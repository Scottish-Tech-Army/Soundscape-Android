package org.scottishtecharmy.soundscape.locationprovider.bleimu

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.locationprovider.CalibratedHeadTrackingProvider
import org.scottishtecharmy.soundscape.locationprovider.DirectionProvider
import org.scottishtecharmy.soundscape.locationprovider.HeadTrackerInputLog
import org.scottishtecharmy.soundscape.locationprovider.HeadTrackingStatus
import org.scottishtecharmy.soundscape.locationprovider.LocationProvider
import org.scottishtecharmy.soundscape.platform.currentTimeMillis

/**
 * HeadTrackingProvider backed by an external BLE IMU (currently the WitMotion
 * WT9011DCL). The sensor's raw accelerometer and gyroscope give a yaw about the
 * vertical, whatever way up it's worn, which [CalibratedHeadTrackingProvider]
 * turns into a geographic heading.
 */
class BleImuHeadTrackingProvider(
    directionProvider: DirectionProvider,
    locationProvider: LocationProvider,
    private val client: BleImuClient = BleImuClient(),
    clock: () -> Long = ::currentTimeMillis,
) : CalibratedHeadTrackingProvider(
    directionProvider,
    locationProvider,
    inputId = INPUT_ID,
    // The WT streams at 100 Hz, so the device calibrator needs a ~10 s window
    // of samples to be as forgiving of brief look-aways as the AirPods
    // (which get ~8 s from their 200-sample window at ~25 Hz).
    deviceWindowSize = 1_000,
    clock = clock,
) {

    override val deviceName = "WitMotion"

    private val parser = WitMotionFrameParser(onRegisterResponse = ::onRegisterResponse)
    private val yawIntegrator = GravityAlignedYawIntegrator()

    private val _batteryVoltageFlow = MutableStateFlow<Float?>(null)
    /** Most recent battery voltage in volts, or null before the first reading. */
    val batteryVoltageFlow: StateFlow<Float?> = _batteryVoltageFlow

    private var scope: CoroutineScope? = null

    override val isRunning: Boolean get() = scope != null

    // Arrival time of the last notification that carried IMU frames, or null before the first
    private var lastFramesMillis: Long? = null

    override fun start() {
        if (scope != null) return
        val newScope = CoroutineScope(Dispatchers.Default + Job())
        scope = newScope
        calibrationManager.start()
        parser.reset()
        yawIntegrator.reset()
        mutableStatusFlow.value = HeadTrackingStatus.Disconnected

        observeReferences(newScope)
        newScope.launch { runBleLoop() }
    }

    override fun stop() {
        scope?.cancel()
        scope = null
        resetCalibration()
        parser.reset()
        yawIntegrator.reset()
        mutableStatusFlow.value = HeadTrackingStatus.Inactive
        _batteryVoltageFlow.value = null
        mutableBatteryPercentFlow.value = null
    }

    private fun onRegisterResponse(response: WitMotionRegisterResponse) {
        if (response.address == BleImuClient.REG_BATTERY_VOLTAGE) {
            // The first reply after connecting reads 0, before the sensor has measured its
            // battery. Taken at face value that announces a flat battery, so wait for the next.
            if (response.rawValue <= 0) return
            // Register units are 0.01 V (e.g. 410 -> 4.10 V).
            val volts = response.rawValue / 100.0f
            _batteryVoltageFlow.value = volts
            mutableBatteryPercentFlow.value = witMotionBatteryPercent(response.rawValue)
        }
    }

    override fun onSessionStarted() {
        parser.reset()
        yawIntegrator.reset()
        lastFramesMillis = null
    }

    override fun onSessionFinished() {
        _batteryVoltageFlow.value = null
        mutableBatteryPercentFlow.value = null
    }

    internal fun onBytes(bytes: ByteArray, nowMillis: Long) =
        input({ bytes(nowMillis, bytes) }) { processBytes(bytes, nowMillis) }

    private fun processBytes(bytes: ByteArray, nowMillis: Long) {
        val samples = parser.consume(bytes, nowMillis)
        if (samples.isEmpty()) return
        // The sensor packs its 100 Hz frames four to a notification, so they all arrive at once.
        // Share the time since the previous notification between them, rather than give the
        // first the whole interval and the rest none.
        val previous = lastFramesMillis
        lastFramesMillis = nowMillis
        val intervalMillis = if (previous == null) {
            NOMINAL_FRAME_MILLIS * samples.size
        } else {
            (nowMillis - previous).coerceIn(0L, MAX_NOTIFICATION_GAP_MILLIS)
        }
        val frameSeconds = intervalMillis / 1000.0 / samples.size
        for (sample in samples) {
            // Don't use sample.yawDegrees — it's rotation about the sensor's body
            // Z axis, which is only useful when the sensor happens to be mounted
            // Z-up. Instead derive yaw rate about the world-up axis from
            // (gyro · gravity_in_body) and integrate. Drift is corrected by the
            // calibration's correlation against compass and walking course.
            val integrated = yawIntegrator.update(sample.accelG, sample.gyroDps, frameSeconds)
                ?: continue
            // Integrator produces CCW-positive about world-up (right-hand rule).
            // Compass heading is CW-positive, so negate before the calibrator.
            processYaw(-integrated, nowMillis)
        }
    }

    override fun replayDeviceInput(fields: List<String>, t: Long): Boolean {
        if (fields[0] != "B") return false
        onBytes(HeadTrackerInputLog.parseBytes(fields[2]), t)
        return true
    }

    private suspend fun runBleLoop() {
        while (true) {
            try {
                client.runSession(
                    onConnected = { onSessionConnected(clock()) },
                    onBytes = { bytes -> onBytes(bytes, clock()) },
                )
            } catch (ce: kotlin.coroutines.cancellation.CancellationException) {
                throw ce
            } catch (_: Throwable) {
                // Scan failed / connection dropped / GATT error — wait and retry.
                onSessionEnded(clock())
                delay(RECONNECT_DELAY_MILLIS)
            }
        }
    }

    companion object {
        const val INPUT_ID = "witmotion"
        private const val RECONNECT_DELAY_MILLIS = 2_000L
        private const val NOMINAL_FRAME_MILLIS = 10L
        // Beyond this the connection stalled, and the frames don't span the whole gap
        private const val MAX_NOTIFICATION_GAP_MILLIS = 200L
    }
}
