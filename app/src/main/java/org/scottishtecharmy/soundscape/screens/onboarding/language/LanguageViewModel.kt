package org.scottishtecharmy.soundscape.screens.onboarding.language

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor(private val audioEngine : NativeAudioEngine): ViewModel() {

    val _state : MutableStateFlow<LanguageUiState> = MutableStateFlow(LanguageUiState())
    val state: StateFlow<LanguageUiState> = _state.asStateFlow()
    init {
        _state.value = LanguageUiState(supportedLanguages = getAllLanguages(), selectedLanguageIndex = -1)
    }
    data class LanguageUiState(
        // Data for the ViewMode that affects the UI
        var supportedLanguages : List<Language> = emptyList(),
        var selectedLanguageIndex: Int = -1
    )

    private fun addIfSpeechSupports(allLanguages: MutableList<Language>, language: Language) {
        // TODO: The idea here is to add the language only if it's supported by the text to speech
        //  engine. However, the audioEngine appears to be struggling to initialize the TextToSpeech.
        //  Not sure why - needs investigation.
//        val locales = audioEngine.getAvailableSpeechLanguages()
//        for (locale in locales) {
//            if (locale.language == language.code) {
        allLanguages.add(language)
//                return
//            }
//        }
    }

    private fun getAllLanguages(): List<Language> {
        val allLanguages = mutableListOf<Language>()

        addIfSpeechSupports(allLanguages, Language("Dansk", "da"))
        addIfSpeechSupports(allLanguages, Language("Deutsch", "de"))
        addIfSpeechSupports(allLanguages, Language("Ελληνικά", "el"))
        addIfSpeechSupports(allLanguages, Language("English", "en"))
        addIfSpeechSupports(allLanguages, Language("Español", "es"))
        addIfSpeechSupports(allLanguages, Language("Suomi", "fi"))
        addIfSpeechSupports(allLanguages, Language("Français", "fr"))
        addIfSpeechSupports(allLanguages, Language("Italiano", "it"))
        addIfSpeechSupports(allLanguages, Language("日本語", "ja"))
        addIfSpeechSupports(allLanguages, Language("Norsk", "nb"))
        addIfSpeechSupports(allLanguages, Language("Nederlands", "nl"))
        addIfSpeechSupports(allLanguages, Language("Português (Brasil)", "pt"))
        addIfSpeechSupports(allLanguages, Language("Svenska", "sv"))

        return allLanguages
    }

    fun updateSpeechLanguage(selectedLanguage: Language): Boolean {
        val indexOfSelectedLanguage = _state.value.supportedLanguages.indexOf(selectedLanguage)
        _state.value = _state.value.copy(selectedLanguageIndex = indexOfSelectedLanguage)
        return audioEngine.setSpeechLanguage(selectedLanguage.code)
    }
}