package org.scottishtecharmy.soundscape.viewmodels

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.preference.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.scottishtecharmy.soundscape.MainActivity
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine
import javax.inject.Inject

@HiltViewModel
class AudioBeaconsViewModel @Inject constructor(@ApplicationContext val context: Context, val audioEngine : NativeAudioEngine): ViewModel() {

    data class AudioBeaconsUiState(
        // Data for the ViewMode that affects the UI
        val beaconTypes : List<String> = emptyList(),
        val selectedBeacon: String? = null
    )
    private var beacon : Long = 0

    private val _state: MutableStateFlow<AudioBeaconsUiState> = MutableStateFlow(
        AudioBeaconsUiState(
            beaconTypes = audioEngine.getListOfBeaconTypes().toList(),
            selectedBeacon = null
        )
    )

    val state: StateFlow<AudioBeaconsUiState> = _state.asStateFlow()
    @SuppressLint("ApplySharedPref")
    fun setAudioBeaconType(type: String) {
        audioEngine.setBeaconType(type)
        if(beacon != 0L)
            audioEngine.destroyBeacon(beacon)
        beacon = audioEngine.createBeacon(1.0, 0.0)
        _state.value = state.value.copy(selectedBeacon = type)
        // Store the preference for future use
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        sharedPreferences.edit().putString(MainActivity.BEACON_TYPE_KEY, type).commit()
    }

    fun silenceBeacon() {
        if(beacon != 0L) {
            audioEngine.destroyBeacon(beacon)
            beacon = 0
        }
    }
}