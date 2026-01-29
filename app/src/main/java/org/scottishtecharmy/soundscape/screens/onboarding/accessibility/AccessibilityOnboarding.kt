package org.scottishtecharmy.soundscape.screens.onboarding.accessibility

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.switchPreference
import org.scottishtecharmy.soundscape.MainActivity
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.components.OnboardButton
import org.scottishtecharmy.soundscape.screens.onboarding.component.BoxWithGradientBackground
import org.scottishtecharmy.soundscape.ui.theme.smallPadding
import org.scottishtecharmy.soundscape.ui.theme.spacing

@Composable
fun AccessibilityOnboardingScreenVM(
    onNavigate: (() -> Unit)?,
    modifier: Modifier = Modifier,
    viewModel: AccessibilityOnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AccessibilityOnboardingScreen(
        onNavigate = onNavigate,
        uiState = uiState,
        onEnableGraphicalMap = { viewModel.enableGraphicalMaps(it)},
        modifier = modifier
    )
}

@Composable
fun AccessibilityOnboardingScreen(
    onNavigate: (() -> Unit)?,
    uiState: AccessibilityOnboardingUiState,
    onEnableGraphicalMap: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithGradientBackground(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface
    ){
        Column(
            modifier = Modifier
                .padding(horizontal = spacing.large)
                .padding(top = spacing.large)
                .fillMaxWidth()
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = stringResource(R.string.accessibility_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics {
                    heading()
                }
            )
            Spacer(modifier = Modifier.height(spacing.large))

            val text = if(uiState.talkbackEnabled) {
                stringResource(R.string.accessibility_screen_reader_enabled)
            } else {
                stringResource(R.string.accessibility_screen_reader_disabled)
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.smallPadding()
            )

            Spacer(modifier = Modifier.height(spacing.large))

            ProvidePreferenceLocals {
                LazyColumn(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
                    switchPreference(
                        key = MainActivity.SHOW_MAP_KEY,
                        defaultValue = MainActivity.SHOW_MAP_DEFAULT,
                        title = {
                            Text(
                                text = stringResource(R.string.settings_show_map),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        },
                    )
                }
            }

            if(onNavigate != null) {
                OnboardButton(
                    text = stringResource(R.string.ui_continue),
                    onClick = { onNavigate() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("accessibilityOnboardingScreenContinueButton"),
                )
            }
        }
    }
}

@Preview
@Composable
fun OfflineStorageOnboardingScreenPreview() {
    AccessibilityOnboardingScreen(
        onNavigate = null,
        AccessibilityOnboardingUiState(false),
        onEnableGraphicalMap = { _ -> },
    )
}