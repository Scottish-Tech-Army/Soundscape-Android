package org.scottishtecharmy.soundscape.screens.onboarding.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.components.OnboardButton
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.first_launch_welcome_button
import org.scottishtecharmy.soundscape.resources.first_launch_welcome_description
import org.scottishtecharmy.soundscape.resources.first_launch_welcome_title
import org.scottishtecharmy.soundscape.resources.soundscape_alpha_1024
import org.scottishtecharmy.soundscape.screens.onboarding.component.BoxWithGradientBackground
import org.scottishtecharmy.soundscape.ui.theme.spacing

@Composable
fun Welcome(
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithGradientBackground(
        color = MaterialTheme.colorScheme.surface
    ) {
        BoxWithConstraints {
            val isLandscape = maxWidth > maxHeight
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
            ) {
                if (isLandscape) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        LocalImage()
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (!isLandscape) {
                        LocalImage()

                        Spacer(modifier = Modifier.height(spacing.large))
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = spacing.large)
                    ) {
                        Text(
                            text = stringResource(Res.string.first_launch_welcome_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(spacing.small))
                        Text(
                            text = stringResource(Res.string.first_launch_welcome_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(spacing.large))

                        Column(modifier = Modifier.padding(horizontal = spacing.large)) {
                            OnboardButton(
                                text = stringResource(Res.string.first_launch_welcome_button),
                                onClick = { onNavigate() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("welcomeScreenContinueButton")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalImage() {
    Image(
        painter = painterResource(Res.drawable.soundscape_alpha_1024),
        contentDescription = null,
        modifier = Modifier
            .clip(RoundedCornerShape(spacing.extraLarge))
    )
}
