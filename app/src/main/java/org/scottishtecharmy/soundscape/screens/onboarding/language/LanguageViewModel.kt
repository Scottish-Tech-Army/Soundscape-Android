package org.scottishtecharmy.soundscape.screens.onboarding.language

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.scottishtecharmy.soundscape.utils.supportedLanguages
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor(): ViewModel() {

    private val _state : MutableStateFlow<LanguageUiState> = MutableStateFlow(LanguageUiState())
    val state: StateFlow<LanguageUiState> = _state.asStateFlow()
    init {
        val allLanguages = supportedLanguages
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