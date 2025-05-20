package org.scottishtecharmy.soundscape.screens.home.placesnearby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import org.scottishtecharmy.soundscape.SoundscapeServiceConnection
import javax.inject.Inject

@HiltViewModel
class PlacesNearbyViewModel
    @Inject
    constructor(
        soundscapeServiceConnection: SoundscapeServiceConnection
) : ViewModel() {

    val logic = PlacesNearbySharedLogic(soundscapeServiceConnection, viewModelScope)
    fun onClickBack() {
        logic.internalUiState.value = logic.uiState.value.copy(level = 0)
    }

    fun onClickFolder(filter: String, title: String) {
        // Apply the filter
        logic.internalUiState.value = logic.uiState.value.copy(level = 1, filter = filter, title = title)
    }
}
