package org.scottishtecharmy.soundscape.screens.onboarding.finish

import androidx.compose.foundation.Image
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
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import org.scottishtecharmy.soundscape.components.OnboardButton
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.first_launch_prompt_button
import org.scottishtecharmy.soundscape.resources.first_launch_prompt_message
import org.scottishtecharmy.soundscape.resources.first_launch_prompt_title
import org.scottishtecharmy.soundscape.resources.ic_finish
import org.scottishtecharmy.soundscape.screens.onboarding.component.BoxWithGradientBackground
import org.scottishtecharmy.soundscape.ui.theme.mediumPadding
import org.scottishtecharmy.soundscape.ui.theme.spacing

@Composable
fun FinishScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithGradientBackground(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface
    ) {
        BoxWithConstraints {
            val landscape = maxWidth > maxHeight
            val alignment = if (landscape) Alignment.CenterVertically else Alignment.Top

            Row(
                verticalAlignment = alignment,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
            ) {
                if (landscape) {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                    ) {
                        LocalImage()
                    }
                }
                Column(
                    modifier = Modifier.fillMaxHeight(),
                ) {
                    if (!landscape) {
                        LocalImage()
                        Spacer(modifier = Modifier.height(spacing.extraLarge))
                    }

                    Column(
                        modifier = Modifier.padding(horizontal = spacing.large)
                    ) {
                        Text(
                            text = stringResource(Res.string.first_launch_prompt_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().semantics { heading() }
                        )

                        Spacer(modifier = Modifier.height(spacing.large))

                        Text(
                            text = stringResource(Res.string.first_launch_prompt_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(spacing.extraLarge))

                        OnboardButton(
                            text = stringResource(Res.string.first_launch_prompt_button),
                            onClick = { onFinish() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("finishScreenContinueButton")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalImage() {
    Image(
        painter = painterResource(Res.drawable.ic_finish),
        contentDescription = null,
        modifier = Modifier
            .mediumPadding()
            .clip(RoundedCornerShape(spacing.extraLarge))
    )
}
