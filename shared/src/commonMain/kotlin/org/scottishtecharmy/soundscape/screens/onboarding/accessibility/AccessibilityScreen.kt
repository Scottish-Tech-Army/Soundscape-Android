package org.scottishtecharmy.soundscape.screens.onboarding.accessibility

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.SwitchPreference
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.components.OnboardButton
import org.scottishtecharmy.soundscape.preferences.PreferenceDefaults
import org.scottishtecharmy.soundscape.preferences.PreferenceKeys
import org.scottishtecharmy.soundscape.preferences.PreferencesProvider
import org.scottishtecharmy.soundscape.preferences.rememberBooleanPreferenceState
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.accessibility_screen_reader_disabled
import org.scottishtecharmy.soundscape.resources.accessibility_screen_reader_enabled
import org.scottishtecharmy.soundscape.resources.accessibility_title
import org.scottishtecharmy.soundscape.resources.settings_show_map
import org.scottishtecharmy.soundscape.resources.ui_continue
import org.scottishtecharmy.soundscape.screens.home.settings.rememberSoundscapePreferenceFlow
import org.scottishtecharmy.soundscape.screens.onboarding.component.BoxWithGradientBackground
import org.scottishtecharmy.soundscape.ui.theme.smallPadding
import org.scottishtecharmy.soundscape.ui.theme.spacing

@Composable
fun AccessibilityOnboardingScreen(
    isScreenReaderActive: Boolean,
    preferencesProvider: PreferencesProvider,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    BoxWithGradientBackground(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface
    ) {
        val text = if (isScreenReaderActive) {
            stringResource(Res.string.accessibility_screen_reader_enabled)
        } else {
            stringResource(Res.string.accessibility_screen_reader_disabled)
        }

        // Bind the toggle to the app's PreferencesProvider so reads/writes share
        // a single storage path with everyone else (e.g. SharedHomeScreen).
        val showMap = rememberBooleanPreferenceState(
            preferencesProvider,
            PreferenceKeys.SHOW_MAP,
            PreferenceDefaults.SHOW_MAP,
        )
        // Default the preference based on screen-reader state when this screen
        // first appears, matching the legacy Android behaviour. The toggle
        // below lets the user override.
        LaunchedEffect(isScreenReaderActive) {
            showMap.value = !isScreenReaderActive
        }

        Column(
            modifier = Modifier
                .padding(horizontal = spacing.large)
                .padding(top = spacing.large)
                .fillMaxWidth()
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(Res.string.accessibility_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .semantics { heading() }
                    .focusRequester(focusRequester)
                    .focusable()
            )
            Spacer(modifier = Modifier.height(spacing.large))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .smallPadding()
                    .focusable()
            )
            Spacer(modifier = Modifier.height(spacing.large))

            ProvidePreferenceLocals(flow = rememberSoundscapePreferenceFlow()) {
                SwitchPreference(
                    state = showMap,
                    title = {
                        Text(
                            text = stringResource(Res.string.settings_show_map),
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusable()
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("accessibilityOnboardingScreenShowMapToggle"),
                )

                OnboardButton(
                    text = stringResource(Res.string.ui_continue),
                    onClick = { onNavigate() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusable()
                        .testTag("accessibilityOnboardingScreenContinueButton"),
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        focusRequester.requestFocus()
    }
}
