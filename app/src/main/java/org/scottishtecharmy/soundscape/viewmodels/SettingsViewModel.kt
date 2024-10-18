package org.scottishtecharmy.soundscape.viewmodels

import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.SoundscapeServiceConnection
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val soundscapeServiceConnection: SoundscapeServiceConnection
) : ViewModel() {
    data class SettingsUiState(
        // Data for the ViewMode that affects the UI
        var beaconTypes : List<String> = emptyList(),
        var voiceTypes : List<String> = emptyList()
    )

    private val _state: MutableStateFlow<SettingsUiState> = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // Connect to the service and use its audio engine to get configuration and to
            // demonstrate settings changes.
            soundscapeServiceConnection.serviceBoundState.collect {
                Log.d(TAG, "serviceBoundState $it")
                val job = Job()
                if (it) {
                    val audioEngine = soundscapeServiceConnection.soundscapeService?.audioEngine!!
                    viewModelScope.launch(job) {
                        audioEngine.textToSpeechRunning.collectLatest { initialized ->
                            if (initialized) {
                                // Only once the TextToSpeech engine is initialized can we populate the
                                // members of these lists.
                                val audioEngineVoiceTypes = audioEngine.getAvailableSpeechVoices()
                                val voiceTypes = mutableListOf<String>()

                                val locale = AppCompatDelegate.getApplicationLocales()[0]
                                for (type in audioEngineVoiceTypes) {
                                    if (!type.isNetworkConnectionRequired &&
                                        !type.features.contains("notInstalled") &&
                                        type.locale.language == locale!!.language
                                    ) {
                                        // The Voice don't contain any description, just a text string
                                        voiceTypes.add(type.name)
                                    }
                                }

                                val audioEngineBeaconTypes = audioEngine.getListOfBeaconTypes()
                                val beaconTypes = mutableListOf<String>()
                                for (type in audioEngineBeaconTypes) {
                                    beaconTypes.add(type)
                                }
                                _state.value = SettingsUiState(
                                    beaconTypes = beaconTypes,
                                    voiceTypes = voiceTypes
                                )
                            }
                        }
                    }
                }
                else
                {
                    job.cancel()
                }
            }
        }
    }
    companion object {
        private const val TAG = "SettingsViewModel"
    }
}