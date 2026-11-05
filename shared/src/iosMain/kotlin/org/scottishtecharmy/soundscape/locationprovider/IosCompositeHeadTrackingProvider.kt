package org.scottishtecharmy.soundscape.locationprovider

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.locationprovider.bleimu.BleImuHeadTrackingProvider
import org.scottishtecharmy.soundscape.locationprovider.bose.BoseFramesHeadTrackingProvider

/**
 * iOS head-tracker composite with a strict priority:
 *  - [airpods] — AirPods / AirPods Max H1 head tracker; always wins when connected
 *  - [externalBle] — fallback composite of any paired external BLE head trackers
 *    (WitMotion IMU, Bose Frames AR), used only when AirPods are absent
 *
 * To keep BLE scans off the air when AirPods are doing the job, [externalBle]
 * is only kept running while AirPods are *not* in Connected / Calibrated state.
 * When AirPods drop the connection, the external composite is restarted and
 * its inner arbitration picks the first BLE device that connects.
 */
class IosCompositeHeadTrackingProvider(
    directionProvider: DirectionProvider,
    locationProvider: LocationProvider,
) : HeadTrackingProvider() {

    private val airpods = IosHeadTrackingProvider(directionProvider, locationProvider)
    private val externalBle = CompositeHeadTrackingProvider(
        listOf(
            BleImuHeadTrackingProvider(directionProvider, locationProvider),
            BoseFramesHeadTrackingProvider(directionProvider, locationProvider),
        ),
    )

    private val mutableStateFlow = MutableStateFlow(INACTIVE)
    /**
     * The overall status, with the name and battery of whichever tracker is in use: the AirPods
     * while they are connected, otherwise the external composite's own state.
     */
    val stateFlow: StateFlow<HeadTrackerState> = mutableStateFlow

    private var scope: CoroutineScope? = null

    override fun start() {
        if (scope != null) return
        val newScope = CoroutineScope(Dispatchers.Default + Job())
        scope = newScope

        airpods.start()

        newScope.launch {
            // Gate the external-BLE composite on AirPods status. When AirPods
            // are connected we don't need BLE scanning at all; the moment they
            // disconnect, kick the external composite back on so WT/Bose can
            // take over.
            airpods.statusFlow.collect { airpodsStatus ->
                val airpodsActive = airpodsStatus == HeadTrackingStatus.Connected ||
                    airpodsStatus == HeadTrackingStatus.Calibrated
                if (airpodsActive) {
                    if (externalBle.statusFlow.value != HeadTrackingStatus.Inactive) {
                        externalBle.stop()
                    }
                } else if (externalBle.statusFlow.value == HeadTrackingStatus.Inactive) {
                    externalBle.start()
                }
            }
        }

        newScope.launch {
            combine(
                airpods.statusFlow,
                airpods.headHeadingFlow,
                externalBle.headHeadingFlow,
            ) { airpodsStatus, airpodsHeading, externalHeading ->
                val airpodsActive = airpodsStatus == HeadTrackingStatus.Connected ||
                    airpodsStatus == HeadTrackingStatus.Calibrated
                if (airpodsActive) airpodsHeading else externalHeading
            }.collect { mutableHeadHeadingFlow.value = it }
        }

        newScope.launch {
            combine(airpods.statusFlow, externalBle.statusFlow) { a, b ->
                // Surface the most-progressed of the two so the UI can show
                // "Calibrated" when either source is fully ready.
                listOf(a, b).maxBy { it.ordinal }
            }.collect { mutableStatusFlow.value = it }
        }

        newScope.launch {
            combine(airpods.statusFlow, externalBle.stateFlow) { airpodsStatus, external ->
                if (airpodsStatus.isActive()) {
                    HeadTrackerState(airpodsStatus, airpods.deviceName, null)
                } else {
                    // Same status as statusFlow, so a searching AirPods tracker still shows
                    external.copy(status = maxOf(airpodsStatus, external.status))
                }
            }.collect { mutableStateFlow.value = it }
        }
    }

    private fun HeadTrackingStatus.isActive(): Boolean =
        this == HeadTrackingStatus.Connected || this == HeadTrackingStatus.Calibrated

    override fun takeDebugSnapshot(includeInput: Boolean): HeadTrackingDebug? = mergeDebugSnapshots(
        listOfNotNull(airpods.takeDebugSnapshot(includeInput), externalBle.takeDebugSnapshot(includeInput))
    )

    override fun stop() {
        scope?.cancel()
        scope = null
        airpods.stop()
        externalBle.stop()
        mutableHeadHeadingFlow.value = null
        mutableStatusFlow.value = HeadTrackingStatus.Inactive
        mutableStateFlow.value = INACTIVE
    }

    override fun destroy() {
        stop()
        airpods.destroy()
        externalBle.destroy()
    }

    private companion object {
        val INACTIVE = HeadTrackerState(HeadTrackingStatus.Inactive, null, null)
    }
}
