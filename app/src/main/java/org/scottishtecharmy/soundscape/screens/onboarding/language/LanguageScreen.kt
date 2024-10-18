package org.scottishtecharmy.soundscape.screens.onboarding.language

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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.components.OnboardButton
import org.scottishtecharmy.soundscape.screens.onboarding.component.BoxWithGradientBackground

@Composable
fun LanguageScreen(
    onNavigate: () -> Unit,
    modifier : Modifier = Modifier,
    viewModel: LanguageViewModel = hiltViewModel<LanguageViewModel>()
){
    val uiState: LanguageViewModel.LanguageUiState by viewModel.state.collectAsStateWithLifecycle()

    LanguageComposable(
        onNavigate = onNavigate,
        supportedLanguages = uiState.supportedLanguages,
        onLanguageSelected = { selectedLanguage ->
            viewModel.updateLanguage(selectedLanguage)
        },
        selectedLanguageIndex = uiState.selectedLanguageIndex,
        modifier = modifier
    )
}

@Composable
fun LanguageComposable(
    supportedLanguages: List<Language>,
    onNavigate: () -> Unit,
    onLanguageSelected: (Language) -> Unit,
    selectedLanguageIndex: Int,
    modifier : Modifier = Modifier
){

    /*TODO move to the test if(mockData != null) {
        supportedLanguages = MockLanguagePreviewData.languages
    }*/

    BoxWithGradientBackground(
        modifier = modifier
    ) {
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
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics {
                    heading()
                },
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.first_launch_soundscape_language_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            LanguageSelectionBox(
                allLanguages = supportedLanguages,
                onLanguageSelected = onLanguageSelected,
                selectedLanguageIndex = selectedLanguageIndex
                )

            Spacer(modifier = Modifier.height(40.dp))

            //val selectedLocale = AppCompatDelegate.getApplicationLocales()[0]
            //Log.d("Locales", "Locale is set to: $selectedLocale")

            val isContinueEnabled by remember(selectedLanguageIndex) {
                derivedStateOf {
                    selectedLanguageIndex != -1
                }
            }
            Column(modifier = Modifier.padding(horizontal = 50.dp)) {
                OnboardButton(
                    text = stringResource(R.string.ui_continue),
                    onClick = {
                        onNavigate()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isContinueEnabled
                )
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
fun LanguagePreview() {
    LanguageComposable(
        supportedLanguages = MockLanguagePreviewData.languages,
        onNavigate = {},
        onLanguageSelected = {},
        selectedLanguageIndex = -1,
    )
}