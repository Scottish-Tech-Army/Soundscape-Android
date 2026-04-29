package org.scottishtecharmy.soundscape.screens.onboarding.accessibility

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.components.OnboardButton
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.accessibility_screen_reader_disabled
import org.scottishtecharmy.soundscape.resources.accessibility_screen_reader_enabled
import org.scottishtecharmy.soundscape.resources.accessibility_title
import org.scottishtecharmy.soundscape.resources.ui_continue
import org.scottishtecharmy.soundscape.screens.onboarding.component.BoxWithGradientBackground
import org.scottishtecharmy.soundscape.ui.theme.smallPadding
import org.scottishtecharmy.soundscape.ui.theme.spacing

@Composable
fun AccessibilityOnboardingScreen(
    isScreenReaderActive: Boolean,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithGradientBackground(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface
    ) {
        val text = if (isScreenReaderActive) {
            stringResource(Res.string.accessibility_screen_reader_enabled)
        } else {
            stringResource(Res.string.accessibility_screen_reader_disabled)
        }

        LazyColumn(
            modifier = Modifier
                .padding(horizontal = spacing.large)
                .padding(top = spacing.large)
                .fillMaxWidth()
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Text(
                    text = stringResource(Res.string.accessibility_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() }
                )
                Spacer(modifier = Modifier.height(spacing.large))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.smallPadding()
                )
                Spacer(modifier = Modifier.height(spacing.large))
            }

            item {
                OnboardButton(
                    text = stringResource(Res.string.ui_continue),
                    onClick = { onNavigate() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("accessibilityOnboardingScreenContinueButton"),
                )
            }
        }
    }
}
