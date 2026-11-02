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

/**
 * What the user is told about a head tracker: its status, the device it is, and its battery.
 * [deviceName] and [batteryPercent] are null unless a device is connected, and [batteryPercent]
 * also stays null for a device that doesn't report its battery.
 */
data class HeadTrackerState(
    val status: HeadTrackingStatus,
    val deviceName: String?,
    val batteryPercent: Int?,
)

abstract class HeadTrackingProvider {
    protected val mutableHeadHeadingFlow = MutableStateFlow<HeadHeading?>(null)
    val headHeadingFlow: StateFlow<HeadHeading?> = mutableHeadHeadingFlow

    protected val mutableStatusFlow = MutableStateFlow(HeadTrackingStatus.Inactive)
    val statusFlow: StateFlow<HeadTrackingStatus> = mutableStatusFlow

    protected val mutableBatteryPercentFlow = MutableStateFlow<Int?>(null)
    /** 0–100 battery of the connected device, or null when it doesn't report one. */
    val batteryPercentFlow: StateFlow<Int?> = mutableBatteryPercentFlow

    /** The device's brand name, used when telling the user it has connected. Not translated. */
    open val deviceName: String? = null

    abstract fun start()
    abstract fun stop()
    open fun destroy() {
        stop()
    }
}
