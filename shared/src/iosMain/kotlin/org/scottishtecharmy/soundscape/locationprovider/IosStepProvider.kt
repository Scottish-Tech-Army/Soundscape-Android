package org.scottishtecharmy.soundscape.locationprovider

import kotlin.concurrent.AtomicReference
import platform.CoreMotion.CMAuthorizationStatusAuthorized
import platform.CoreMotion.CMAuthorizationStatusDenied
import platform.CoreMotion.CMAuthorizationStatusRestricted
import platform.CoreMotion.CMPedometer
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

/**
 * CMPedometer's running step count and cadence. iOS has no per-step event, so only the counter
 * fields of [StepSnapshot] are filled in, and they arrive in batches every few seconds. Starting
 * it is what shows the Motion & Fitness prompt, which is why it waits for the first recorded fix.
 */
class IosStepProvider : StepProvider {

    private val pedometer = CMPedometer()
    private val state = AtomicReference<StepSnapshot?>(null)
    private var started = false

    override fun snapshot(): StepSnapshot? {
        if (!started) start()
        return state.value
    }

    /** Called from onboarding to show the Motion & Fitness prompt up front. */
    fun requestPermission() {
        if (!started) start()
    }

    fun isPermissionGranted(): Boolean =
        CMPedometer.authorizationStatus() == CMAuthorizationStatusAuthorized

    /** True once the user has said no - requestPermission() won't show the system prompt again. */
    fun isPermissionDenied(): Boolean {
        val status = CMPedometer.authorizationStatus()
        return status == CMAuthorizationStatusDenied || status == CMAuthorizationStatusRestricted
    }

    fun isAvailable(): Boolean = CMPedometer.isStepCountingAvailable()

    private fun start() {
        if (!CMPedometer.isStepCountingAvailable()) return
        val status = CMPedometer.authorizationStatus()
        if (status == CMAuthorizationStatusDenied || status == CMAuthorizationStatusRestricted) return
        started = true
        pedometer.startPedometerUpdatesFromDate(NSDate()) { data, error ->
            if (data == null || error != null) return@startPedometerUpdatesFromDate
            state.value = StepSnapshot(
                counterSteps = data.numberOfSteps.longValue,
                counterMillis = (data.endDate.timeIntervalSince1970 * 1000.0).toLong(),
                cadence = data.currentCadence?.doubleValue,
            )
        }
    }

    override fun stop() {
        if (started) pedometer.stopPedometerUpdates()
        started = false
    }
}
