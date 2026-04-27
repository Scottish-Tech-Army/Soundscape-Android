package org.scottishtecharmy.soundscape.services.mediacontrol

import kotlinx.coroutines.flow.StateFlow
import org.scottishtecharmy.soundscape.audio.AudioType
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.locationprovider.SoundscapeLocation

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
}
