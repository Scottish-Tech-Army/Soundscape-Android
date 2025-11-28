package org.scottishtecharmy.soundscape.viewmodels

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.MainActivity
import org.scottishtecharmy.soundscape.SoundscapeServiceConnection
import org.scottishtecharmy.soundscape.screens.onboarding.audiobeacons.getBeaconResourceId
import org.scottishtecharmy.soundscape.utils.StorageUtils
import org.scottishtecharmy.soundscape.utils.getCurrentLocale
import org.scottishtecharmy.soundscape.utils.getOfflineMapStorage
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val soundscapeServiceConnection: SoundscapeServiceConnection,
    @param:ApplicationContext val appContext: Context
) : ViewModel() {
    data class SettingsUiState(
        // Data for the ViewMode that affects the UI
        var beaconDescriptions : List<Int> = emptyList(),
        var beaconValues : List<String> = emptyList(),
        var engineTypes : List<String> = emptyList(),
        var voiceTypes : List<String> = emptyList(),
        var storages : List<StorageUtils.StorageSpace> = emptyList(),
        var currentStoragePath: String = "",
        var selectedStorageIndex: Int = -1
    )

    private val _restartAppEvent = MutableSharedFlow<Unit>()
    val restartAppEvent = _restartAppEvent.asSharedFlow()

    private val _state: MutableStateFlow<SettingsUiState> = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()
    private val coroutineScope = CoroutineScope(Job())
    private var serviceBoundJob: Job? = null

    init {
        viewModelScope.launch {
            // Connect to the service and use its audio engine to get configuration and to
            // demonstrate settings changes.
            val storages = getOfflineMapStorage(appContext)
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext)
            val path = sharedPreferences.getString(MainActivity.SELECTED_STORAGE_KEY, MainActivity.SELECTED_STORAGE_DEFAULT)
            var currentPath = ""
            var currentIndex = 0
            if((path != null) && (path.isNotEmpty())) {
                for((index, storage) in storages.withIndex()) {
                    if (storage.path == path) {
                        currentIndex = index
                        currentPath = path
                        break
                    }
                }
            } else {
                if(storages.isNotEmpty()) {
                    currentPath = storages[0].path
                    currentIndex = 0
                }
            }
            _state.value = SettingsUiState(
                storages = storages,
                currentStoragePath = currentPath,
                selectedStorageIndex = currentIndex
            )
            soundscapeServiceConnection.serviceBoundState.collect {
                Log.d(TAG, "serviceBoundState $it")
                if (it) {
                    val audioEngine = soundscapeServiceConnection.soundscapeService?.audioEngine!!
                    serviceBoundJob = Job()
                    viewModelScope.launch(serviceBoundJob!!) {
                        audioEngine.ttsRunningStateChange.collectLatest { initialized ->
                            if (initialized) {
                                // Only once the TextToSpeech engine is initialized can we populate the
                                // members of these lists.
                                val audioEngineTypes = audioEngine.getAvailableSpeechEngines()

                                val audioEngineVoiceTypes = audioEngine.getAvailableSpeechVoices()
                                val voiceTypes = mutableListOf<String>()

                                // The list of voices will start of with those in the current locale
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
                                // And then we add all the others. Because we don't support all
                                // languages it's useful to be able to select a voice from a different
                                // locale than the one that the app is currently using.
                                for (type in audioEngineVoiceTypes) {
                                    if (!type.isNetworkConnectionRequired &&
                                        !type.features.contains("notInstalled") &&
                                        type.locale.language != locale.language
                                    ) {
                                        // The Voice don't contain any description, just a text string
                                        voiceTypes.add(type.name)
                                    }
                                }

                                val audioEngineBeaconTypes = audioEngine.getListOfBeaconTypes()
                                val beaconTypes = mutableListOf<Int>()
                                val beaconValues = mutableListOf<String>()
                                for (type in audioEngineBeaconTypes) {
                                    beaconTypes.add(getBeaconResourceId(type))
                                    beaconValues.add(type)
                                }
                                _state.value = _state.value.copy(
                                    beaconDescriptions = beaconTypes,
                                    beaconValues = beaconValues,
                                    voiceTypes = voiceTypes,
                                    engineTypes = audioEngineTypes.map { engine -> "${engine.label}:::${engine.name}" },
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
                    serviceBoundJob?.cancel()
                }
            }
        }
    }
    fun updateLanguage(localContext: MainActivity) {
        coroutineScope.launch {
            localContext.setServiceState(false)
            Thread.sleep(1000)
            localContext.setServiceState(true)
        }
    }

    fun selectStorage(path: String) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext)
        sharedPreferences.edit(commit = true) { putString(MainActivity.SELECTED_STORAGE_KEY, path) }

        var currentIndex = -1
        for((index, storage) in _state.value.storages.withIndex()) {
            if (storage.path == path) {
                currentIndex = index
                break
            }
        }
        _state.value = _state.value.copy(
            currentStoragePath = path,
            selectedStorageIndex = currentIndex
        )
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext)
            sharedPreferences.edit(commit = true) {
                clear()
            }
            _restartAppEvent.emit(Unit)
        }
    }

    companion object {
        private const val TAG = "SettingsViewModel"
    }
}