package org.scottishtecharmy.soundscape.screens.onboarding.accessibility

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.scottishtecharmy.soundscape.preferences.PreferencesProvider

@Composable
fun AccessibilityOnboardingScreenVM(
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccessibilityOnboardingViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val preferencesProvider: PreferencesProvider = koinInject()
    AccessibilityOnboardingScreen(
        isScreenReaderActive = uiState.talkbackEnabled,
        preferencesProvider = preferencesProvider,
        onNavigate = onNavigate,
        modifier = modifier,
    )
}

@Preview(device = "spec:width=320dp,height=480dp,dpi=160")
@Composable
fun AccessibilityOnboardingScreenPreview() {
    // Preview requires a PreferencesProvider; the runtime composable is exercised
    // via AccessibilityOnboardingScreenVM in the live app.
}
