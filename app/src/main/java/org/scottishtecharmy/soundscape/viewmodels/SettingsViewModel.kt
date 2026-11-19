package org.scottishtecharmy.soundscape.viewmodels

import android.content.Context
import android.speech.tts.Voice
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.scottishtecharmy.soundscape.locationprovider.HeadTrackerState
import org.scottishtecharmy.soundscape.MainActivity
import org.scottishtecharmy.soundscape.SoundscapeServiceConnection
import org.scottishtecharmy.soundscape.logging.AppLog
import org.scottishtecharmy.soundscape.screens.home.settings.TtsVoiceOption
import org.scottishtecharmy.soundscape.screens.onboarding.audiobeacons.getBeaconResourceId
import org.scottishtecharmy.soundscape.utils.StorageUtils
import org.scottishtecharmy.soundscape.utils.getCurrentLocale
import org.scottishtecharmy.soundscape.utils.getOfflineMapStorage
import java.util.Locale

class SettingsViewModel(
    private val soundscapeServiceConnection: SoundscapeServiceConnection,
    val appContext: Context
) : ViewModel() {
    data class SettingsUiState(
        // Data for the ViewMode that affects the UI
        var beaconDescriptions: List<org.jetbrains.compose.resources.StringResource> = emptyList(),
        var beaconValues: List<String> = emptyList(),
        var engineTypes: List<String> = emptyList(),
        var voiceOptions: List<TtsVoiceOption> = emptyList(),
        var storages: List<StorageUtils.StorageSpace> = emptyList(),
        var currentStoragePath: String = "",
        var selectedStorageIndex: Int = -1,
    )

    private val _state: MutableStateFlow<SettingsUiState> = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    /** The head tracker's status, device and battery, for the status line under its switch. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val headTrackerState: StateFlow<HeadTrackerState?> =
        soundscapeServiceConnection.serviceBoundState
            .flatMapLatest { bound ->
                val service = soundscapeServiceConnection.soundscapeService
                if (bound && service != null) service.headTrackerStateFlow else flowOf(null)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private var serviceBoundJob: Job? = null

    init {
        viewModelScope.launch {
            // Connect to the service and use its audio engine to get configuration and to
            // demonstrate settings changes.
            val storages = getOfflineMapStorage(appContext)
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext)
            val path = sharedPreferences.getString(
                MainActivity.SELECTED_STORAGE_KEY,
                MainActivity.SELECTED_STORAGE_DEFAULT
            )
            var currentPath = ""
            var currentIndex = 0
            if ((path != null) && (path.isNotEmpty())) {
                for ((index, storage) in storages.withIndex()) {
                    if (storage.path == path) {
                        currentIndex = index
                        currentPath = path
                        break
                    }
                }
            } else {
                if (storages.isNotEmpty()) {
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
                logger.d { "serviceBoundState $it" }
                val audioEngine = soundscapeServiceConnection.soundscapeService?.audioEngine
                if (it && audioEngine != null) {
                    serviceBoundJob = viewModelScope.launch {
                        audioEngine.ttsRunningStateChange.collectLatest { initialized ->
                            if (initialized) {
                                // Only once the TextToSpeech engine is initialized can we populate the
                                // members of these lists.
                                // getAvailableSpeechEngines/getAvailableSpeechVoices end up making a
                                // synchronous Binder call into the system TTS service
                                // (ITextToSpeechService.getVoices), which can block for a long time on
                                // some OEM builds. Run them off the main thread to avoid ANRs.
                                val (audioEngineTypes, audioEngineVoiceTypes) = withContext(Dispatchers.IO) {
                                    Pair(
                                        audioEngine.getAvailableSpeechEngines(),
                                        audioEngine.getAvailableSpeechVoices()
                                    )
                                }
                                val voiceOptions = voiceOptions(audioEngineVoiceTypes, getCurrentLocale())

                                val audioEngineBeaconTypes = audioEngine.getListOfBeaconTypes()
                                val beaconTypes =
                                    mutableListOf<org.jetbrains.compose.resources.StringResource>()
                                val beaconValues = mutableListOf<String>()
                                for (type in audioEngineBeaconTypes) {
                                    beaconTypes.add(getBeaconResourceId(type))
                                    beaconValues.add(type)
                                }
                                _state.value = _state.value.copy(
                                    beaconDescriptions = beaconTypes,
                                    beaconValues = beaconValues,
                                    voiceOptions = voiceOptions,
                                    engineTypes = audioEngineTypes.map { engine -> "${engine.label}:::${engine.name}" },
                                )
                            } else {
                                logger.d { "Engine has gone uninitialized" }
                            }
                        }
                    }
                } else {
                    serviceBoundJob?.cancel()
                }
            }
        }
    }

    fun selectStorage(path: String) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext)
        sharedPreferences.edit(commit = true) { putString(MainActivity.SELECTED_STORAGE_KEY, path) }

        var currentIndex = -1
        for ((index, storage) in _state.value.storages.withIndex()) {
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

    fun previewVoice(voiceName: String) {
        soundscapeServiceConnection.soundscapeService?.audioEngine?.previewSpeechVoice(voiceName)
    }

    fun restoreVoice() {
        soundscapeServiceConnection.soundscapeService?.audioEngine?.restoreSpeechVoice()
    }

    fun startBeaconPreview(beaconType: String) {
        soundscapeServiceConnection.startBeaconPreview(beaconType)
    }

    fun updateBeaconPreviewType(beaconType: String) {
        soundscapeServiceConnection.updateBeaconPreviewType(beaconType)
    }

    fun stopBeaconPreview(commit: Boolean, chosenBeaconType: String?) {
        soundscapeServiceConnection.stopBeaconPreview(commit, chosenBeaconType)
    }

    companion object {
        private const val TAG = "SettingsViewModel"
        private val logger = AppLog.tag(TAG)
    }
}

/**
 * The engine's voices as the voice picker lists them, grouped under their language: the app's own
 * locale first, then the rest of its language, then every other language by name. Voices in other
 * languages are offered because the app's language and the language of the street names around
 * the user are two different things, and because the app isn't translated into every language.
 *
 * Voices that need the network, or whose data isn't downloaded, are left out. There are no
 * provider sub-lists as on iOS: the engine is a setting of its own, and only its voices are here.
 */
internal fun voiceOptions(voices: Collection<Voice>, appLocale: Locale): List<TtsVoiceOption> {
    val appLanguage = appLocale.iso3LanguageOrNull()
    return voices
        .filter { !it.isNetworkConnectionRequired && !it.features.contains("notInstalled") }
        .map { voice ->
            // Some engines spell their locales with three-letter codes ("eng_GBR"), so compare
            // languages by their ISO 639-2 code rather than as written.
            val voiceLanguage = voice.locale.iso3LanguageOrNull()
            val inAppLanguage = appLanguage != null && voiceLanguage == appLanguage
            val inAppLocale = inAppLanguage &&
                voice.locale.iso3CountryOrNull() == appLocale.iso3CountryOrNull()
            Triple(voice, inAppLocale, inAppLanguage)
        }
        .map { (voice, inAppLocale, inAppLanguage) ->
            // getDisplayName follows the language's own capitalisation rules, which leave French
            // and Spanish names in lower case - fine mid-sentence, odd as a heading.
            val languageName = voice.locale.getDisplayName(appLocale)
                .replaceFirstChar { it.titlecase(appLocale) }
            inAppLocale to TtsVoiceOption(
                identifier = voice.name,
                // Android voices have no name beyond the engine's identifier for them.
                name = voice.name,
                languageDisplayName = languageName,
                inAppLanguage = inAppLanguage,
            )
        }
        .sortedWith(
            compareByDescending<Pair<Boolean, TtsVoiceOption>> { it.first }
                .thenByDescending { it.second.inAppLanguage }
                .thenBy { it.second.languageDisplayName }
                .thenBy { it.second.name },
        )
        .map { it.second }
}

private fun Locale.iso3LanguageOrNull(): String? = runCatching { isO3Language }.getOrNull()

private fun Locale.iso3CountryOrNull(): String? = runCatching { isO3Country }.getOrNull()
