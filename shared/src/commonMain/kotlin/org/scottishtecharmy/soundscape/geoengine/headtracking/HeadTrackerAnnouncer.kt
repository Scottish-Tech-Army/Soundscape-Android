package org.scottishtecharmy.soundscape.geoengine.headtracking

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.getString
import org.scottishtecharmy.soundscape.locationprovider.HeadTrackerState
import org.scottishtecharmy.soundscape.locationprovider.HeadTrackingStatus
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.head_tracker_battery_low
import org.scottishtecharmy.soundscape.resources.head_tracker_calibrated
import org.scottishtecharmy.soundscape.resources.head_tracker_connected
import org.scottishtecharmy.soundscape.resources.head_tracker_connected_battery
import org.scottishtecharmy.soundscape.resources.head_tracker_disconnected

/** Something to tell the user about their head tracker. */
sealed interface HeadTrackerAnnouncement {
    data class Connected(val deviceName: String?, val batteryPercent: Int?) : HeadTrackerAnnouncement
    data object Calibrated : HeadTrackerAnnouncement
    data class Disconnected(val deviceName: String?) : HeadTrackerAnnouncement
    data class BatteryLow(val batteryPercent: Int) : HeadTrackerAnnouncement
}

suspend fun HeadTrackerAnnouncement.text(): String = when (this) {
    is HeadTrackerAnnouncement.Connected ->
        if (batteryPercent != null) {
            getString(Res.string.head_tracker_connected_battery, deviceName ?: "", batteryPercent)
        } else {
            getString(Res.string.head_tracker_connected, deviceName ?: "")
        }
    HeadTrackerAnnouncement.Calibrated -> getString(Res.string.head_tracker_calibrated)
    is HeadTrackerAnnouncement.Disconnected ->
        getString(Res.string.head_tracker_disconnected, deviceName ?: "")
    is HeadTrackerAnnouncement.BatteryLow ->
        getString(Res.string.head_tracker_battery_low, batteryPercent)
}

/**
 * Tells the user, through [announce], when a head tracker connects (with its battery level if it
 * reports one), when it has calibrated, when it disconnects, and when its battery runs low.
 *
 * A disconnect is only announced once the tracker has stayed disconnected for
 * [DISCONNECT_DEBOUNCE_MILLIS], and reconnecting within that time is not announced either, so a
 * brief drop-out says nothing at all. Head tracking being switched off (Inactive) is never
 * announced: the user did that themselves.
 *
 * Everything runs on [dispatcher], which must not run two coroutines at once: the collector and
 * the delayed announcements it launches share the state below without locking.
 */
class HeadTrackerAnnouncer(
    private val state: StateFlow<HeadTrackerState?>,
    private val announce: suspend (HeadTrackerAnnouncement) -> Unit,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default.limitedParallelism(1),
) {
    // The device whose connection has been announced, or null if there isn't one. Kept through
    // a pending disconnect so that a quick reconnect of the same device stays silent.
    private var announcedDevice: String? = null
    private var connectionAnnounced = false
    private val lowBatteryAnnounced = mutableSetOf<Int>()
    private var connectJob: Job? = null
    private var disconnectJob: Job? = null

    fun launchIn(scope: CoroutineScope): Job = scope.launch(dispatcher) {
        var previous = HeadTrackingStatus.Inactive
        state.collect { current ->
            val status = current?.status ?: HeadTrackingStatus.Inactive
            when {
                status == HeadTrackingStatus.Inactive -> reset()

                status.isActive() && !previous.isActive() -> onConnected(this, current)

                !status.isActive() && previous.isActive() -> onDisconnected(this)
            }
            if (status == HeadTrackingStatus.Calibrated && previous != HeadTrackingStatus.Calibrated) {
                announce(HeadTrackerAnnouncement.Calibrated)
            }
            if (status.isActive() && connectionAnnounced) {
                current?.batteryPercent?.let { checkBattery(it) }
            }
            previous = status
        }
    }

    private fun onConnected(scope: CoroutineScope, current: HeadTrackerState?) {
        val reconnected = disconnectJob?.isActive == true
        disconnectJob?.cancel()
        disconnectJob = null
        val deviceName = current?.deviceName
        if (reconnected && connectionAnnounced && deviceName == announcedDevice) return

        connectionAnnounced = false
        lowBatteryAnnounced.clear()
        connectJob?.cancel()
        connectJob = scope.launch {
            // The WitMotion answers its first battery request within about a second of
            // connecting, so wait a little for it rather than announce without it. A device that
            // doesn't report its battery is announced once the wait runs out.
            val battery = current?.batteryPercent
                ?: withTimeoutOrNull(BATTERY_WAIT_MILLIS) {
                    state.first { it?.batteryPercent != null }
                }?.batteryPercent
            announcedDevice = deviceName
            connectionAnnounced = true
            // The connection announcement gives the level, so don't follow it with a warning
            battery?.let { level -> lowBatteryAnnounced.addAll(LOW_BATTERY_THRESHOLDS.filter { level <= it }) }
            announce(HeadTrackerAnnouncement.Connected(deviceName, battery))
        }
    }

    private fun onDisconnected(scope: CoroutineScope) {
        connectJob?.cancel()
        connectJob = null
        if (!connectionAnnounced) return
        disconnectJob?.cancel()
        disconnectJob = scope.launch {
            delay(DISCONNECT_DEBOUNCE_MILLIS)
            val deviceName = announcedDevice
            announcedDevice = null
            connectionAnnounced = false
            lowBatteryAnnounced.clear()
            announce(HeadTrackerAnnouncement.Disconnected(deviceName))
        }
    }

    private suspend fun checkBattery(level: Int) {
        val crossed = LOW_BATTERY_THRESHOLDS.filter { level <= it && it !in lowBatteryAnnounced }
        if (crossed.isEmpty()) return
        lowBatteryAnnounced.addAll(crossed)
        announce(HeadTrackerAnnouncement.BatteryLow(level))
    }

    private fun reset() {
        connectJob?.cancel()
        connectJob = null
        disconnectJob?.cancel()
        disconnectJob = null
        announcedDevice = null
        connectionAnnounced = false
        lowBatteryAnnounced.clear()
    }

    private fun HeadTrackingStatus.isActive(): Boolean =
        this == HeadTrackingStatus.Connected || this == HeadTrackingStatus.Calibrated

    companion object {
        const val BATTERY_WAIT_MILLIS = 3_000L
        const val DISCONNECT_DEBOUNCE_MILLIS = 3_000L
        val LOW_BATTERY_THRESHOLDS = listOf(20, 10)
    }
}
