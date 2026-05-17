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
 * All children are started together. The composite emits the **most recently
 * sampled** non-null heading across them — so when only one external device
 * is paired only its samples appear, and when two arrive simultaneously the
 * one with the higher [HeadHeading.timestampMillis] wins. Status is the
 * highest enum ordinal across all children (e.g. Calibrated > Connected >
 * Disconnected > Inactive > Unavailable).
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
    }

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
