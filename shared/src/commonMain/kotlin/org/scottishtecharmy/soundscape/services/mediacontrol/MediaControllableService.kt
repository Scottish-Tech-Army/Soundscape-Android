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
}
