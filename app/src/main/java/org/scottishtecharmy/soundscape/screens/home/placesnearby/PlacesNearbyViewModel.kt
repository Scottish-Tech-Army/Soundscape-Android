package org.scottishtecharmy.soundscape.screens.home.placesnearby

import androidx.lifecycle.ViewModel
import org.scottishtecharmy.soundscape.SoundscapeServiceConnection
import org.scottishtecharmy.soundscape.audio.AudioTour
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

class PlacesNearbyViewModel(
    soundscapeServiceConnection: SoundscapeServiceConnection,
    audioTour: AudioTour,
) : ViewModel() {

    val holder = PlacesNearbyStateHolder(soundscapeServiceConnection, audioTour)
    val uiState = holder.uiState

    fun onClickBack() = holder.onClickBack()
    fun onClickFolder(filter: String, title: String) = holder.onClickFolder(filter, title)
    fun startBeacon(location: LngLatAlt, name: String) = holder.startBeacon(location, name)

    override fun onCleared() {
        super.onCleared()
        holder.dispose()
    }
}
