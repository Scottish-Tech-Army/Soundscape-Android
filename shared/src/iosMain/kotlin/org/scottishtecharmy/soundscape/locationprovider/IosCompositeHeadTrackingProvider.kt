package org.scottishtecharmy.soundscape.locationprovider

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
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
 * All children run continuously so swapping between AirPods and a BLE head
 * tracker is instant when the user removes / replaces them.
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

    private var scope: CoroutineScope? = null

    override fun start() {
        if (scope != null) return
        val newScope = CoroutineScope(Dispatchers.Default + Job())
        scope = newScope

        airpods.start()
        externalBle.start()

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
    }

    override fun stop() {
        scope?.cancel()
        scope = null
        airpods.stop()
        externalBle.stop()
        mutableHeadHeadingFlow.value = null
        mutableStatusFlow.value = HeadTrackingStatus.Inactive
    }

    override fun destroy() {
        stop()
        airpods.destroy()
        externalBle.destroy()
    }
}
