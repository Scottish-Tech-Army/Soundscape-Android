package org.scottishtecharmy.soundscape.screens.onboarding.language

import androidx.compose.foundation.focusable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.components.OnboardButton
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.first_launch_soundscape_language
import org.scottishtecharmy.soundscape.resources.first_launch_soundscape_language_text
import org.scottishtecharmy.soundscape.resources.ui_continue
import org.scottishtecharmy.soundscape.screens.onboarding.component.BoxWithGradientBackground
import org.scottishtecharmy.soundscape.ui.theme.spacing

/**
 * Picker UI shown both in onboarding and from Settings → Language. Pure
 * presentation: the caller owns selection state and triggers the locale
 * change, since locale persistence is platform-specific.
 */
@Composable
fun SharedLanguageScreen(
    supportedLanguages: List<Language>,
    selectedLanguageIndex: Int,
    onLanguageSelected: (Language) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    BoxWithGradientBackground(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = spacing.medium, vertical = spacing.large)
                .fillMaxWidth()
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Text(
                text = stringResource(Res.string.first_launch_soundscape_language),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
                    .focusRequester(focusRequester).focusable(),
            )
            Spacer(modifier = Modifier.height(spacing.small))
            Text(
                text = stringResource(Res.string.first_launch_soundscape_language_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.focusable()
            )
            Spacer(modifier = Modifier.height(spacing.large))

            LanguageDropDownMenu(
                allLanguages = supportedLanguages,
                onLanguageSelected = onLanguageSelected,
                selectedLanguageIndex = selectedLanguageIndex,
            )

            Spacer(modifier = Modifier.height(spacing.large))

            val isContinueEnabled by remember(selectedLanguageIndex) {
                derivedStateOf { selectedLanguageIndex != -1 }
            }

            Column(modifier = Modifier.padding(horizontal = spacing.large)) {
                OnboardButton(
                    text = stringResource(Res.string.ui_continue),
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusable()
                        .testTag("languageScreenContinueButton"),
                    enabled = isContinueEnabled,
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        focusRequester.requestFocus()
    }
}

/**
 * Returns the index of the [Language] in [supportedLanguages] that best
 * matches the given locale snapshot, or -1 if there's no language-only match.
 * Prefers an exact language+region match.
 */
fun indexOfBestLanguageMatch(locale: LocaleSnapshot): Int {
    var bestIndex = -1
    for ((index, language) in supportedLanguages.withIndex()) {
        if (language.code == locale.language && language.region == locale.region) {
            return index
        }
        if (language.code == locale.language && bestIndex == -1) {
            bestIndex = index
        }
    }
    return bestIndex
}
