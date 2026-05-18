package org.scottishtecharmy.soundscape.locationprovider

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * HeadTrackingProvider that aggregates several underlying providers.
 *
 * All children are started together so any of them can pick up a device first.
 * Once one reaches [HeadTrackingStatus.Connected] or [HeadTrackingStatus.Calibrated],
 * the others are stopped to suppress wasteful concurrent BLE scans / GATT
 * connections. If the winning child drops back below `Connected` (device
 * disconnect, error) the others are restarted so any of them can take over.
 *
 * The composite emits the **most recently sampled** non-null heading across
 * children — so when only one external device is paired only its samples
 * appear. Status is the highest enum ordinal across all children
 * (Calibrated > Connected > Disconnected > Inactive > Unavailable).
 *
 * Suits the Android case where the user might have a WitMotion IMU or Bose
 * Frames, but typically only one at a time.
 */
class CompositeHeadTrackingProvider(
    private val children: List<HeadTrackingProvider>,
) : HeadTrackingProvider() {

    private var scope: CoroutineScope? = null

    override fun start() {
        if (scope != null) return
        val newScope = CoroutineScope(Dispatchers.Default + Job())
        scope = newScope

        children.forEach { it.start() }

        newScope.launch {
            combine(children.map { it.headHeadingFlow }) { headings ->
                headings.filterNotNull().maxByOrNull { it.timestampMillis }
            }.collect { mutableHeadHeadingFlow.value = it }
        }
        newScope.launch {
            combine(children.map { it.statusFlow }) { statuses ->
                statuses.maxByOrNull { it.ordinal } ?: HeadTrackingStatus.Inactive
            }.collect { mutableStatusFlow.value = it }
        }
        newScope.launch {
            // Scan-coordination: at most one child is allowed to be active at
            // a time. Inactive children sit idle (no BLE scan, no power draw)
            // until the active one drops below Connected.
            combine(children.map { it.statusFlow }) { statuses -> statuses.toList() }
                .collect { statuses -> arbitrate(statuses) }
        }
    }

    private fun arbitrate(statuses: List<HeadTrackingStatus>) {
        val activeIndex = statuses.indexOfFirst { it.isActive() }
        if (activeIndex >= 0) {
            children.forEachIndexed { i, child ->
                if (i != activeIndex && child.statusFlow.value != HeadTrackingStatus.Inactive) {
                    child.stop()
                }
            }
        } else {
            children.forEach {
                if (it.statusFlow.value == HeadTrackingStatus.Inactive) it.start()
            }
        }
    }

    private fun HeadTrackingStatus.isActive(): Boolean =
        this == HeadTrackingStatus.Connected || this == HeadTrackingStatus.Calibrated

    override fun stop() {
        scope?.cancel()
        scope = null
        children.forEach { it.stop() }
        mutableHeadHeadingFlow.value = null
        mutableStatusFlow.value = HeadTrackingStatus.Inactive
    }

    override fun destroy() {
        stop()
        children.forEach { it.destroy() }
    }
}
