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
class AudioBeaconsViewModel @Inject constructor(private val audioEngine : NativeAudioEngine): ViewModel() {

    data class AudioBeaconsUiState(
        // Data for the ViewMode that affects the UI
        var beaconTypes : List<String> = emptyList()
    )
    private var beacon : Long = 0

    val state: StateFlow<AudioBeaconsUiState> = flow {
        val audioEngineBeaconTypes = audioEngine.getListOfBeaconTypes()
        val beaconTypes = mutableListOf<String>()
        for (type in audioEngineBeaconTypes) {
            beaconTypes.add(type)
        }
        emit(AudioBeaconsUiState(beaconTypes = beaconTypes))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AudioBeaconsUiState())

    fun setAudioBeaconType(type: String) {
        audioEngine.setBeaconType(type)
        if(beacon != 0L)
            audioEngine.destroyBeacon(beacon)
        beacon = audioEngine.createBeacon(0.0, 0.0)
    }

    fun silenceBeacon() {
        if(beacon != 0L) {
            audioEngine.destroyBeacon(beacon)
            beacon = 0
        }
    }
}