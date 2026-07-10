package org.scottishtecharmy.soundscape.screens.onboarding.hearing

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.first_launch_callouts_example_1
import org.scottishtecharmy.soundscape.resources.first_launch_callouts_example_3
import org.scottishtecharmy.soundscape.resources.first_launch_callouts_example_4
import org.scottishtecharmy.soundscape.screens.onboarding.AudioOnboardingViewModel

// Hearing composable is now in shared module

@Composable
fun HearingScreen(
    onBack: () -> Unit,
    onNavigate: () -> Unit,
    viewModel: AudioOnboardingViewModel
) {
    val speechText = buildString {
        append(stringResource(Res.string.first_launch_callouts_example_1))
        append(". ")
        append(stringResource(Res.string.first_launch_callouts_example_3))
        append(". ")
        append(stringResource(Res.string.first_launch_callouts_example_4))
    }
    BackHandler(enabled = true) {
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
        }
    )
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Preview
@Composable
fun HearingPreview() {
    Hearing(onContinue = {}, onPlaySpeech = {})
}
