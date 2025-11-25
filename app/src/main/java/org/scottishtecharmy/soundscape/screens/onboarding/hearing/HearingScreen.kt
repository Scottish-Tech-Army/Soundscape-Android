package org.scottishtecharmy.soundscape.screens.onboarding.hearing

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import org.scottishtecharmy.soundscape.screens.onboarding.AudioOnboardingViewModel
import org.scottishtecharmy.soundscape.screens.onboarding.component.BoxWithGradientBackground
import org.scottishtecharmy.soundscape.ui.theme.spacing


@Composable
fun HearingScreen(
    onBack: () -> Unit,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel : AudioOnboardingViewModel
){
    val speechText = buildString {
        append(stringResource(R.string.first_launch_callouts_example_1))
        append(". ")
        append(stringResource(R.string.first_launch_callouts_example_3))
        append(". ")
        append(stringResource(R.string.first_launch_callouts_example_4))
    }
    BackHandler(enabled = true) {
        // If the user presses the back button silence the speech
        viewModel.silenceSpeech()
        onBack()
    }
    Hearing(
        onContinue = {
            viewModel.silenceSpeech()
            onNavigate()
        },
        onPlaySpeech = {
            viewModel.playSpeech(speechText)
        },
        modifier = modifier,
    )
}

@Composable
fun Hearing(
    onContinue: () -> Unit,
    onPlaySpeech: () -> Unit,
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
                Column(
                    modifier = Modifier
                        .fillMaxHeight(),
                ) {
                    LocalImage()
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            )
            {
                if (!landscape) {
                    LocalImage()
                    Spacer(modifier = Modifier.height(spacing.extraLarge))
                }

                Column(
                    modifier = Modifier.padding(horizontal = spacing.large)
                ) {
                    Text(
                        text = stringResource(R.string.first_launch_callouts_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().semantics {
                            heading()
                        },
                    )
                    Spacer(modifier = Modifier.height(spacing.extraLarge))
                    Text(
                        text = stringResource(R.string.first_launch_callouts_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(spacing.large))
                    Text(
                        text = stringResource(R.string.first_launch_callouts_listen),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(spacing.small))

                    // <string name="first_launch_callouts_example_1">Cafe</string>
                    // <string name="first_launch_callouts_example_3">Main Street goes left</string>
                    // <string name="first_launch_callouts_example_4">Main Street goes right</string>

                    //val speechText = stringResource(R.string.first_launch_callouts_example_4)

                    // This is a bodged version to introduce pauses between translation strings
                    // Not sure if this will work with some of the other languages, for example, Japanese

                    Button(
                        onClick = {
                            onPlaySpeech()
                        },
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(spacing.tiny),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        )
                    )
                    {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(spacing.small))
                            Text(
                                text = stringResource(R.string.first_launch_callouts_listen_accessibility_label),
                                color = MaterialTheme.colorScheme.onSurface,
                                )
                        }
                    }
                    Spacer(modifier = Modifier.height(spacing.medium))

                    OnboardButton(
                        text = stringResource(R.string.ui_continue),
                        onClick = {
                            onContinue()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hearingScreenContinueButton")
                    )
                }
            }
        }
    }
}

@Composable
private fun LocalImage() {
    Image(
        painter = painterResource(R.drawable.ic_surroundings),
        contentDescription = null
    )
}


@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Preview
@Composable
fun HearingPreview() {
    Hearing(onContinue = {},
        onPlaySpeech = {}
    )
}