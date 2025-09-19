package org.scottishtecharmy.soundscape.screens.onboarding.listening

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.components.OnboardButton
import org.scottishtecharmy.soundscape.screens.onboarding.component.BoxWithGradientBackground
import org.scottishtecharmy.soundscape.ui.theme.spacing

@Composable
fun ListeningScreen(
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Listening(
        modifier = modifier,
        onNavigate = onNavigate
    )
}

@Composable
fun Listening(
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val landscape = (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE)
    val alignment = if(landscape)
        Alignment.CenterVertically
    else
        Alignment.Top

    BoxWithGradientBackground(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            verticalAlignment = alignment,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
        ) {
            if (landscape) {
                Column(modifier = Modifier.fillMaxHeight()) {
                    LocalImage()
                }
            }
            Column(
                modifier = Modifier
                    .padding(top = spacing.large),
                horizontalAlignment = Alignment.CenterHorizontally
            )
            {
                if (!landscape) {
                    LocalImage()
                    Spacer(modifier = Modifier.height(spacing.extraLarge))
                }

                Column(modifier = Modifier.padding(horizontal = spacing.large)) {
                    Text(
                        text = stringResource(R.string.first_launch_headphones_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics {
                            heading()
                        }
                    )
                    Spacer(modifier = Modifier.height(spacing.extraLarge))
                    Text(
                        text = stringResource(R.string.first_launch_headphones_message_1),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(spacing.small))
                    Text(
                        text = stringResource(R.string.first_launch_headphones_message_2),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(spacing.extraLarge))

                    OnboardButton(
                        text = stringResource(R.string.ui_continue),
                        onClick = { onNavigate() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("listeningScreenContinueButton"),
                    )
                }
            }
        }
    }
}

@Composable
private fun LocalImage() {
    Image(
        painter = painterResource(R.drawable.ic_listening),
        contentDescription = null,
    )
}


@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Preview
@Composable
fun ListeningPreview() {
    Listening(onNavigate = {})
}