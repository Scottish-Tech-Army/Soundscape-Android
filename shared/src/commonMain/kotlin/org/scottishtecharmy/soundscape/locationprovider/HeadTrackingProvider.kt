package org.scottishtecharmy.soundscape.locationprovider

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class HeadHeading(
    val degrees: Double,
    val accuracyDegrees: Double,
    val timestampMillis: Long,
)

enum class HeadTrackingStatus {
    Unavailable,
    Inactive,
    Disconnected,
    Connected,
    Calibrated,
}

abstract class HeadTrackingProvider {
    protected val mutableHeadHeadingFlow = MutableStateFlow<HeadHeading?>(null)
    val headHeadingFlow: StateFlow<HeadHeading?> = mutableHeadHeadingFlow

    protected val mutableStatusFlow = MutableStateFlow(HeadTrackingStatus.Inactive)
    val statusFlow: StateFlow<HeadTrackingStatus> = mutableStatusFlow

    abstract fun start()
    abstract fun stop()
    open fun destroy() {
        stop()
    }
}
