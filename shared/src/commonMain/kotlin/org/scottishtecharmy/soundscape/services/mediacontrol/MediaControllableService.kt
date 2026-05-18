package org.scottishtecharmy.soundscape.services.mediacontrol

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.scottishtecharmy.soundscape.audio.AudioType
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewEnabled
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewState
import org.scottishtecharmy.soundscape.geoengine.filters.TrackedCallout
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.locationprovider.DeviceDirection
import org.scottishtecharmy.soundscape.locationprovider.HeadHeading
import org.scottishtecharmy.soundscape.locationprovider.SoundscapeLocation
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.services.BeaconState
import org.scottishtecharmy.soundscape.services.RoutePlayerState

private val DEFAULT_STREET_PREVIEW_FLOW: StateFlow<StreetPreviewState> =
    MutableStateFlow(StreetPreviewState(StreetPreviewEnabled.OFF)).asStateFlow()

private val DEFAULT_VOICE_COMMAND_FLOW: StateFlow<VoiceCommandState> =
    MutableStateFlow<VoiceCommandState>(VoiceCommandState.Idle).asStateFlow()

private val DEFAULT_HEAD_HEADING_FLOW: StateFlow<HeadHeading?> =
    MutableStateFlow<HeadHeading?>(null).asStateFlow()

private val DEFAULT_HEADSET_BATTERY_FLOW: StateFlow<Int?> =
    MutableStateFlow<Int?>(null).asStateFlow()

interface MediaControllableService {
    // Media control target methods
    fun routeMute(): Boolean
    fun routeSkipNext(): Boolean
    fun routeSkipPrevious(): Boolean
    fun myLocation()
    fun whatsAroundMe()

    // Route player support
    val filteredLocationFlow: StateFlow<SoundscapeLocation?>
    fun speakText(
        text: String,
        type: AudioType,
        latitude: Double = Double.NaN,
        longitude: Double = Double.NaN,
        heading: Double = Double.NaN,
    )

    fun clearTextToSpeechQueue()
    fun createBeacon(location: LngLatAlt?, headingOnly: Boolean)
    fun destroyBeacon()

    // Audio menu support
    var menuActive: Boolean
    fun speak2dText(text: String, clearQueue: Boolean = false, earcon: String? = null)
    fun callbackHoldOff()
    fun requestAudioFocus(): Boolean
    fun aheadOfMe()
    fun nearbyMarkers()
    fun routeStop()
    fun routeStartById(routeId: Long)
    fun startBeacon(location: LngLatAlt, name: String)

    // Flow surface used by shared state-holders. Each implementation forwards to its
    // location/direction provider, route player, beacon state, etc.
    val locationFlow: StateFlow<SoundscapeLocation?>
    val orientationFlow: StateFlow<DeviceDirection?>
    /** Calibrated head heading from an external head tracker, or null when no
     *  external head tracker is active. The home screen uses this in
     *  preference to [orientationFlow] when present. */
    val headHeadingFlow: StateFlow<HeadHeading?>
        get() = DEFAULT_HEAD_HEADING_FLOW
    /** 0–100 battery for the most-recently-active Bluetooth audio device, or
     *  null when the platform hasn't reported one (no headphones paired, or
     *  the device doesn't expose battery — e.g. AirPods on Android). */
    val headsetBatteryPercentFlow: StateFlow<Int?>
        get() = DEFAULT_HEADSET_BATTERY_FLOW
    val beaconFlow: StateFlow<BeaconState>
    val currentRouteFlow: StateFlow<RoutePlayerState>
    val gridStateFlow: StateFlow<GridState?>

    // Default no-op implementations for iOS until it grows these features.
    val streetPreviewFlow: StateFlow<StreetPreviewState>
        get() = DEFAULT_STREET_PREVIEW_FLOW
    val voiceCommandStateFlow: StateFlow<VoiceCommandState>
        get() = DEFAULT_VOICE_COMMAND_FLOW

    fun routeStartReverse(routeId: Long)
    fun setStreetPreviewMode(on: Boolean, location: LngLatAlt? = null) {}
    fun streetPreviewGo() {}
    fun getLocationDescription(location: LngLatAlt): LocationDescription
    suspend fun searchResult(query: String): List<LocationDescription>?
    fun isAudioEngineBusy(): Boolean
    fun speakCallout(callout: TrackedCallout?, addModeEarcon: Boolean): Long
}
