package org.scottishtecharmy.soundscape.screens.onboarding

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine
import org.scottishtecharmy.soundscape.components.Language
import org.scottishtecharmy.soundscape.components.LanguageSelectionBox
import org.scottishtecharmy.soundscape.components.OnboardButton
import org.scottishtecharmy.soundscape.screens.onboarding.AudioBeaconsViewModel.AudioBeaconsUiState
import org.scottishtecharmy.soundscape.ui.theme.IntroTypography
import org.scottishtecharmy.soundscape.ui.theme.IntroductionTheme
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

@Composable
fun Language(onNavigate: (String) -> Unit, mockData : MockLanguagePreviewData?){

    var viewModel : LanguageViewModel? = null
    val supportedLanguages : List<Language>
    if(mockData != null) {
        supportedLanguages = mockData.languages
    }
    else {
        viewModel = hiltViewModel<LanguageViewModel>()
        val uiState: LanguageViewModel.LanguageUiState by viewModel.state.collectAsStateWithLifecycle()
        supportedLanguages = uiState.supportedLanguages
    }

    IntroductionTheme {
        MaterialTheme(typography = IntroTypography){
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 50.dp)
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = stringResource(R.string.first_launch_soundscape_language),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.first_launch_soundscape_language_text),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(40.dp))

                LanguageSelectionBox(supportedLanguages)

                Spacer(modifier = Modifier.height(40.dp))

                //val selectedLocale = AppCompatDelegate.getApplicationLocales()[0]
                //Log.d("Locales", "Locale is set to: $selectedLocale")

                Column(modifier = Modifier.padding(horizontal = 50.dp)) {
                    OnboardButton(
                        text = stringResource(R.string.ui_continue),
                        onClick = {
                            viewModel?.updateSpeechLanguage()
                            onNavigate(Screens.Navigating.route)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

            }
        }
    }
}

// Data used by preview
data object MockLanguagePreviewData {
    val languages = listOf(
        Language("Dansk", "da"),
        Language("Deutsch", "de"),
        Language("Ελληνικά", "el"),
        Language("English", "en"),
        Language("Español", "es"),
        Language("Suomi", "fi"),
        Language("Français", "fr"),
        Language("Italiano", "it"),
        Language("日本語", "ja"),
        Language("Norsk", "nb"),
        Language("Nederlands", "nl"),
        Language("Português (Brasil)", "pt"),
        Language("Svenska", "sv")
    )
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Preview
@Composable
fun LanguagePreview(){
    Language(onNavigate = {}, MockLanguagePreviewData)
}