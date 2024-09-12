package org.scottishtecharmy.soundscape.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(private val audioEngine : NativeAudioEngine): ViewModel() {

    data class SettingsUiState(
        // Data for the ViewMode that affects the UI
        var beaconTypes : List<String> = emptyList()
    )
    val state: StateFlow<SettingsUiState> = flow {
        val audioEngineBeaconTypes = audioEngine.getListOfBeaconTypes()
        val beaconTypes = mutableListOf<String>()
        for (type in audioEngineBeaconTypes) {
            beaconTypes.add(type)
        }
        emit(SettingsUiState(beaconTypes = beaconTypes))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())
}