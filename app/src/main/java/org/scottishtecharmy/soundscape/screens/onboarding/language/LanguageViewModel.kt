package org.scottishtecharmy.soundscape.screens.onboarding.language

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor(): ViewModel() {

    private val _state : MutableStateFlow<LanguageUiState> = MutableStateFlow(LanguageUiState())
    val state: StateFlow<LanguageUiState> = _state.asStateFlow()
    init {
        val allLanguages = getAllLanguages()
        _state.value = LanguageUiState(
            supportedLanguages = allLanguages,
            selectedLanguageIndex = allLanguages.indexOfLanguageMatchingDeviceLanguage()
        )
    }
    data class LanguageUiState(
        // Data for the ViewMode that affects the UI
        var supportedLanguages : List<Language> = emptyList(),
        var selectedLanguageIndex: Int = -1
    )

    private fun addIfSpeechSupports(allLanguages: MutableList<Language>, language: Language) {
        allLanguages.add(language)
    }

    private fun getAllLanguages(): List<Language> {
        val allLanguages = mutableListOf<Language>()

        addIfSpeechSupports(allLanguages, Language("العربية المصرية", "arz", "EG"))
        addIfSpeechSupports(allLanguages, Language("Dansk", "da", "DK"))
        addIfSpeechSupports(allLanguages, Language("Deutsch", "de", "DE"))
        addIfSpeechSupports(allLanguages, Language("Ελληνικά", "el", "GR"))
        addIfSpeechSupports(allLanguages, Language("English", "en", "US"))
        addIfSpeechSupports(allLanguages, Language("English (UK)", "en", "GB"))
        addIfSpeechSupports(allLanguages, Language("Español", "es", "ES"))
        addIfSpeechSupports(allLanguages, Language("فارسی", "fa", "IR"))
        addIfSpeechSupports(allLanguages, Language("Suomi", "fi", "FI"))
        addIfSpeechSupports(allLanguages, Language("Français (France)", "fr", "FR"))
        addIfSpeechSupports(allLanguages, Language("Français (Canada)", "fr", "CA"))
        addIfSpeechSupports(allLanguages, Language("Íslenska", "is", "IS"))
        addIfSpeechSupports(allLanguages, Language("Italiano", "it", "IT"))
        addIfSpeechSupports(allLanguages, Language("日本語", "ja", "JP"))
        addIfSpeechSupports(allLanguages, Language("Norsk", "nb", "NO"))
        addIfSpeechSupports(allLanguages, Language("Nederlands", "nl", "NL"))
        addIfSpeechSupports(allLanguages, Language("Polski", "pl", "PL"))
        addIfSpeechSupports(allLanguages, Language("Português (Portugal)", "pt", "PT"))
        addIfSpeechSupports(allLanguages, Language("Português (Brasil)", "pt", "BR"))
        addIfSpeechSupports(allLanguages, Language("Русский", "ru", "RU"))
        addIfSpeechSupports(allLanguages, Language("Svenska", "sv", "SE"))
        addIfSpeechSupports(allLanguages, Language("українська", "uk", "UK"))

        return allLanguages
    }

    private fun List<Language>.indexOfLanguageMatchingDeviceLanguage(): Int {
        val phoneLocale = Locale.getDefault()
        val deviceLanguage = phoneLocale.language
        val deviceRegion = phoneLocale.country
        var bestIndex = -1
        for (language in this) {
            if (language.code == deviceLanguage && language.region == deviceRegion) {
                return this.indexOf(language)
            }
            else if (language.code == deviceLanguage) {
                if(bestIndex == -1) {
                    bestIndex = this.indexOf(language)
                }
            }
        }
        return bestIndex
    }

    fun updateLanguage(selectedLanguage: Language) {
        val indexOfSelectedLanguage = _state.value.supportedLanguages.indexOf(selectedLanguage)
        _state.value = _state.value.copy(selectedLanguageIndex = indexOfSelectedLanguage)


        val list = LocaleListCompat.forLanguageTags("${selectedLanguage.code}-${selectedLanguage.region}")
        AppCompatDelegate.setApplicationLocales(list)
    }
}