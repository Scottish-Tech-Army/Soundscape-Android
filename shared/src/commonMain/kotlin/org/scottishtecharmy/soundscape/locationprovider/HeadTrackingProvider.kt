package org.scottishtecharmy.soundscape.locationprovider

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.scottishtecharmy.soundscape.geoengine.headtracking.HeadphoneCalibration

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

/**
 * What a head tracker was doing at one moment, recorded alongside each GPX track point so a walk
 * can be looked at afterwards. [yawDegrees] is the sensor's own relative yaw, clockwise and
 * unwrapped, and [headingDegrees] = [yawDegrees] + [offsetDegrees]. [courseDegrees] is the walking
 * course given to the calibrator, or null when there wasn't a fresh one. [calibrations] are those
 * made since the previous snapshot. [device] is the tracker the rest describes, by its
 * [CalibratedHeadTrackingProvider.inputId], and [inputs] holds everything each tracker that was
 * running was given since then, for replaying it (see [HeadTrackerInputLog]), by the same id.
 */
data class HeadTrackingDebug(
    val status: HeadTrackingStatus,
    val yawDegrees: Double?,
    val offsetDegrees: Double?,
    val headingDegrees: Double?,
    val phoneHeadingDegrees: Double?,
    val phoneFlat: Boolean,
    val courseDegrees: Double?,
    val calibrations: List<HeadphoneCalibration>,
    val device: String? = null,
    val inputs: Map<String, String> = emptyMap(),
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

    /**
     * The tracker's state for a GPX recording, or null if it isn't running or doesn't report one.
     * Each call takes the calibrations made since the last, so only the recorder should call it.
     * With [includeInput], the snapshot also carries the tracker's raw input for replaying it;
     * without, the tracker stops keeping it.
     */
    open fun takeDebugSnapshot(includeInput: Boolean): HeadTrackingDebug? = null

    abstract fun start()
    abstract fun stop()
    open fun destroy() {
        stop()
    }
}
