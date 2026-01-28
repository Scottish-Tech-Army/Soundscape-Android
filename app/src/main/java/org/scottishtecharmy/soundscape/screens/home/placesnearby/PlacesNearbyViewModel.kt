package org.scottishtecharmy.soundscape.screens.home.placesnearby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import org.scottishtecharmy.soundscape.SoundscapeServiceConnection
import org.scottishtecharmy.soundscape.audio.AudioTour
import javax.inject.Inject

@HiltViewModel
class PlacesNearbyViewModel
    @Inject
    constructor(
        soundscapeServiceConnection: SoundscapeServiceConnection,
        audioTour: AudioTour
) : ViewModel() {

    val logic = PlacesNearbySharedLogic(soundscapeServiceConnection, viewModelScope)

    init {
        // Notify audio tour that we've navigated to Places Nearby
        audioTour.onNavigatedToPlacesNearby()
    }

    fun onClickBack() {
        logic.internalUiState.value = logic.uiState.value.copy(level = 0)
    }

    fun onClickFolder(filter: String, title: String) {
        // Apply the filter
        logic.internalUiState.value = logic.uiState.value.copy(level = 1, filter = filter, title = title)
    }
}
