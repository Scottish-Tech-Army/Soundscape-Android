package org.scottishtecharmy.soundscape.locationprovider

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.platform.PlatformLock
import org.scottishtecharmy.soundscape.platform.withLock

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

    // Held by stop() and by the collectors whenever they act. Cancelling the scope doesn't
    // interrupt a collector already running on another thread, so without this one part way
    // through when stop() is called could write a stale status over stop()'s Inactive, or
    // restart a child that stop() has just stopped and leave it scanning.
    private val lock = PlatformLock()

    /** Runs [block] under [lock], unless [session] has been stopped. */
    private inline fun ifStillRunning(session: CoroutineScope, block: () -> Unit) =
        lock.withLock { if (scope === session) block() }

    override fun start() {
        val newScope = lock.withLock {
            if (scope != null) return
            CoroutineScope(Dispatchers.Default + Job()).also { scope = it }
        }

        children.forEach { it.start() }

        newScope.launch {
            combine(children.map { it.headHeadingFlow }) { headings ->
                headings.filterNotNull().maxByOrNull { it.timestampMillis }
            }.collect { ifStillRunning(newScope) { mutableHeadHeadingFlow.value = it } }
        }
        newScope.launch {
            combine(children.map { it.statusFlow }) { statuses ->
                statuses.maxByOrNull { it.ordinal } ?: HeadTrackingStatus.Inactive
            }.collect { ifStillRunning(newScope) { mutableStatusFlow.value = it } }
        }
        newScope.launch {
            // Scan-coordination: at most one child is allowed to be active at
            // a time. Inactive children sit idle (no BLE scan, no power draw)
            // until the active one drops below Connected.
            combine(children.map { it.statusFlow }) { statuses -> statuses.toList() }
                .collect { statuses -> ifStillRunning(newScope) { arbitrate(statuses) } }
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
        // Once this returns no collector can act any more: any part way through has finished,
        // and the rest will find their session gone
        lock.withLock {
            scope?.cancel()
            scope = null
        }
        children.forEach { it.stop() }
        mutableHeadHeadingFlow.value = null
        mutableStatusFlow.value = HeadTrackingStatus.Inactive
    }

    override fun destroy() {
        stop()
        children.forEach { it.destroy() }
    }
}
