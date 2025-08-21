package org.scottishtecharmy.soundscape.viewmodels

import android.util.Log
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
import org.scottishtecharmy.soundscape.screens.onboarding.audiobeacons.getBeaconResourceId
import org.scottishtecharmy.soundscape.utils.getCurrentLocale
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val soundscapeServiceConnection: SoundscapeServiceConnection
) : ViewModel() {
    data class SettingsUiState(
        // Data for the ViewMode that affects the UI
        var beaconTypes : List<Int> = emptyList(),
        var engineTypes : List<String> = emptyList(),
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
                        audioEngine.ttsRunningStateChange.collectLatest { initialized ->
                            if (initialized) {
                                // Only once the TextToSpeech engine is initialized can we populate the
                                // members of these lists.
                                val audioEngineTypes = audioEngine.getAvailableSpeechEngines()

                                val audioEngineVoiceTypes = audioEngine.getAvailableSpeechVoices()
                                val voiceTypes = mutableListOf<String>()

                                val locale = getCurrentLocale()
                                for (type in audioEngineVoiceTypes) {
                                    if (!type.isNetworkConnectionRequired &&
                                        !type.features.contains("notInstalled") &&
                                        type.locale.language == locale.language
                                    ) {
                                        // The Voice don't contain any description, just a text string
                                        voiceTypes.add(type.name)
                                    }
                                }
                                if(voiceTypes.isEmpty()) {
                                    // I found that with some alternate Text to Speech engines
                                    // installed (e.g. Vocalizer TTS) the locale language doesn't
                                    // always match. If we found no voices, try again without checking
                                    // the language.
                                    for (type in audioEngineVoiceTypes) {
                                        if (!type.isNetworkConnectionRequired &&
                                            !type.features.contains("notInstalled")
                                        ) {
                                            // The Voice don't contain any description, just a text string
                                            voiceTypes.add(type.name)
                                        }
                                    }
                                }

                                val audioEngineBeaconTypes = audioEngine.getListOfBeaconTypes()
                                val beaconTypes = mutableListOf<Int>()
                                for (type in audioEngineBeaconTypes) {
                                    beaconTypes.add(getBeaconResourceId(type))
                                }
                                _state.value = SettingsUiState(
                                    beaconTypes = beaconTypes,
                                    voiceTypes = voiceTypes,
                                    engineTypes = audioEngineTypes.map { engine -> "${engine.label}:::${engine.name}" }
                                )
                            }
                            else {
                                Log.d(TAG, "Engine has gone uninitialized")
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