package org.scottishtecharmy.soundscape.viewmodels

import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine
import org.scottishtecharmy.soundscape.components.Language
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor(private val audioEngine : NativeAudioEngine): ViewModel() {

    data class LanguageUiState(
        // Data for the ViewMode that affects the UI
        var supportedLanguages : List<Language> = emptyList()
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

    val state: StateFlow<LanguageUiState> = flow {
        emit(LanguageUiState(supportedLanguages = getAllLanguages()))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LanguageUiState())

    fun updateSpeechLanguage(): Boolean {
        val languageCode = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        return audioEngine.setSpeechLanguage(languageCode)
    }
}