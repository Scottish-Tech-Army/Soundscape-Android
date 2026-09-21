package org.scottishtecharmy.soundscape.screens.onboarding.battery

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
import org.scottishtecharmy.soundscape.resources.battery_optimization_message
import org.scottishtecharmy.soundscape.resources.battery_optimization_title
import org.scottishtecharmy.soundscape.resources.ui_continue
import org.scottishtecharmy.soundscape.resources.ui_grant_permission
import org.scottishtecharmy.soundscape.screens.onboarding.component.BoxWithGradientBackground
import org.scottishtecharmy.soundscape.ui.theme.spacing

@Composable
fun BatteryOptimization(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    onGrantPermission: (() -> Unit)? = null,
) {
    val focusRequester = remember { FocusRequester() }

    BoxWithGradientBackground(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = spacing.large, vertical = spacing.large)
                .fillMaxWidth()
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = stringResource(Res.string.battery_optimization_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .semantics { heading() }
                    .focusRequester(focusRequester)
                    .focusable(),
            )
            Spacer(modifier = Modifier.height(spacing.large))
            Text(
                text = stringResource(Res.string.battery_optimization_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.focusable()
            )
            Spacer(modifier = Modifier.height(spacing.extraLarge))

            Column(modifier = Modifier.padding(horizontal = spacing.medium)) {
                if (onGrantPermission != null) {
                    // Moving on to the next screen once the system permission dialog
                    // returns is the caller's responsibility (Android doesn't report
                    // whether the user tapped Allow or Deny), so there's no separate
                    // Continue button - this is the only control on screen.
                    OnboardButton(
                        text = stringResource(Res.string.ui_grant_permission),
                        onClick = onGrantPermission,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusable()
                            .testTag("batteryOptimizationGrantPermissionButton"),
                    )
                } else {
                    OnboardButton(
                        text = stringResource(Res.string.ui_continue),
                        onClick = onContinue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusable()
                            .testTag("batteryOptimizationContinueButton"),
                    )
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        focusRequester.requestFocus()
    }
}
