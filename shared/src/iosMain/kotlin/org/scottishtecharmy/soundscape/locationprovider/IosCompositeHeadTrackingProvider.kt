package org.scottishtecharmy.soundscape.locationprovider

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.locationprovider.bleimu.BleImuHeadTrackingProvider

/**
 * Routes between two underlying [HeadTrackingProvider]s on iOS:
 *  - [airpods] — the AirPods/AirPods-Max H1 head tracker (preferred when present)
 *  - [bleImu]  — an external WitMotion BLE IMU, used only when AirPods are absent
 *
 * Both providers run continuously. The composite watches [airpods.statusFlow] and
 * forwards [headHeadingFlow] from whichever provider is currently authoritative.
 * The IMU is left running in the background even when AirPods are in use so the
 * swap is instant when the user removes them.
 */
class IosCompositeHeadTrackingProvider(
    directionProvider: DirectionProvider,
    locationProvider: LocationProvider,
) : HeadTrackingProvider() {

    private val airpods = IosHeadTrackingProvider(directionProvider, locationProvider)
    private val bleImu = BleImuHeadTrackingProvider(directionProvider, locationProvider)

    private var scope: CoroutineScope? = null

    override fun start() {
        if (scope != null) return
        val newScope = CoroutineScope(Dispatchers.Default + Job())
        scope = newScope

        airpods.start()
        bleImu.start()

        newScope.launch {
            combine(
                airpods.statusFlow,
                airpods.headHeadingFlow,
                bleImu.headHeadingFlow,
            ) { airpodsStatus, airpodsHeading, bleHeading ->
                val airpodsActive = airpodsStatus == HeadTrackingStatus.Connected ||
                    airpodsStatus == HeadTrackingStatus.Calibrated
                if (airpodsActive) airpodsHeading else bleHeading
            }.collect { mutableHeadHeadingFlow.value = it }
        }

        newScope.launch {
            combine(airpods.statusFlow, bleImu.statusFlow) { a, b ->
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
        bleImu.stop()
        mutableHeadHeadingFlow.value = null
        mutableStatusFlow.value = HeadTrackingStatus.Inactive
    }

    override fun destroy() {
        stop()
        airpods.destroy()
        bleImu.destroy()
    }
}
