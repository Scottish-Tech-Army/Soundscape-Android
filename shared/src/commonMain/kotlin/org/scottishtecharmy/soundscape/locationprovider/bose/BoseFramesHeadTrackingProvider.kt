package org.scottishtecharmy.soundscape.locationprovider.bose

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.locationprovider.CalibratedHeadTrackingProvider
import org.scottishtecharmy.soundscape.locationprovider.DirectionProvider
import org.scottishtecharmy.soundscape.locationprovider.HeadTrackerInputLog
import org.scottishtecharmy.soundscape.locationprovider.HeadTrackingStatus
import org.scottishtecharmy.soundscape.locationprovider.LocationProvider
import org.scottishtecharmy.soundscape.platform.currentTimeMillis

/**
 * HeadTrackingProvider backed by Bose Frames AR glasses.
 *
 * Bose's quaternion gives a yaw (degrees, sensor frame, drifting from boot), which
 * [CalibratedHeadTrackingProvider] turns into a geographic heading. The earlier
 * proof-of-concept required the glasses to be powered on facing north before app
 * launch — calibration replaces that.
 */
class BoseFramesHeadTrackingProvider(
    directionProvider: DirectionProvider,
    locationProvider: LocationProvider,
    private val client: BoseFramesClient = BoseFramesClient(),
    clock: () -> Long = ::currentTimeMillis,
) : CalibratedHeadTrackingProvider(
    directionProvider,
    locationProvider,
    inputId = INPUT_ID,
    deviceWindowSize = 200,
    clock = clock,
) {

    override val deviceName = "Bose Frames"

    private var scope: CoroutineScope? = null

    override val isRunning: Boolean get() = scope != null

    override fun start() {
        if (scope != null) return
        val newScope = CoroutineScope(Dispatchers.Default + Job())
        scope = newScope
        calibrationManager.start()
        mutableStatusFlow.value = HeadTrackingStatus.Disconnected

        observeReferences(newScope)
        newScope.launch { runBleLoop() }
    }

    override fun stop() {
        scope?.cancel()
        scope = null
        resetCalibration()
        mutableStatusFlow.value = HeadTrackingStatus.Inactive
    }

    private suspend fun runBleLoop() {
        while (true) {
            try {
                client.runSession(
                    // Calibration restarts with each connection — the glasses orient yaw=0
                    // wherever they happen to be when powered on.
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

    internal fun onBytes(bytes: ByteArray, nowMillis: Long) = input({ bytes(nowMillis, bytes) }) {
        val sample = BoseFramesDataParser.parse(bytes, nowMillis)
        // Bose Frames quaternion → yaw uses the right-hand rule (CCW positive
        // looking down from above). Compass heading increases clockwise, so
        // negate before handing off — matches the WitMotion provider's choice.
        if (sample != null) processYaw(-sample.yawDegrees, nowMillis)
    }

    override fun replayDeviceInput(fields: List<String>, t: Long): Boolean {
        if (fields[0] != "B") return false
        onBytes(HeadTrackerInputLog.parseBytes(fields[2]), t)
        return true
    }

    companion object {
        const val INPUT_ID = "bose"
        private const val RECONNECT_DELAY_MILLIS = 2_000L
    }
}
