package org.scottishtecharmy.soundscape.screens.onboarding.audiobeacons

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
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import javax.inject.Inject
import androidx.core.content.edit

@HiltViewModel
class AudioBeaconsViewModel @Inject constructor(@param:ApplicationContext val context: Context, val audioEngine : NativeAudioEngine): ViewModel() {

    data class AudioBeaconsUiState(
        // Data for the ViewMode that affects the UI
        val beaconTypes : List<String> = emptyList(),
        val selectedBeacon: String? = null
    )
    private var beacon : Long = 0

    override fun onCleared() {
        super.onCleared()
        audioEngine.destroy()
    }

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
        beacon = audioEngine.createBeacon(LngLatAlt(1.0, 0.0), true)
        _state.value = state.value.copy(selectedBeacon = type)
        // Store the preference for future use
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        sharedPreferences.edit(commit = true) { putString(MainActivity.BEACON_TYPE_KEY, type) }
    }

    fun silenceBeacon() {
        if(beacon != 0L) {
            audioEngine.destroyBeacon(beacon)
            beacon = 0
        }
    }
}