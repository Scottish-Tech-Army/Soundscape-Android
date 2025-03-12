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
        private val soundscapeServiceConnection: SoundscapeServiceConnection
) : ViewModel() {

    val logic = PlacesNearbySharedLogic(soundscapeServiceConnection, viewModelScope)
    fun onClickBack() {
        logic._uiState.value = logic.uiState.value.copy(topLevel = true)
    }

    fun onClickFolder(filter: String, title: String) {
        // Apply the filter
        logic._uiState.value = logic.uiState.value.copy(topLevel = false, filter = filter, title = title)
    }
}
