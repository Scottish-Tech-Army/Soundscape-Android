package org.scottishtecharmy.soundscape

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.scottishtecharmy.soundscape.screens.Home
import org.scottishtecharmy.soundscape.screens.onboarding.Finish
import org.scottishtecharmy.soundscape.screens.onboarding.HearingPreview
import org.scottishtecharmy.soundscape.screens.onboarding.IntroductionAudioBeaconPreview
import org.scottishtecharmy.soundscape.screens.onboarding.LanguagePreview
import org.scottishtecharmy.soundscape.screens.onboarding.ListeningPreview
import org.scottishtecharmy.soundscape.screens.onboarding.PreviewWelcome
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme

class PreviewTest {
    @Preview(showBackground = true)
    @Composable
    fun AudioBeaconsPreviewTest() {
        SoundscapeTheme {
            IntroductionAudioBeaconPreview()
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun FinishPreviewTest() {
        SoundscapeTheme {
            Finish()
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun HearingPreviewTest() {
        SoundscapeTheme {
            HearingPreview()
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun LanguagePreviewTest() {
        SoundscapeTheme {
            LanguagePreview()
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun ListeningPreviewTest() {
        SoundscapeTheme {
            ListeningPreview()
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun WelcomePreviewTest() {
        SoundscapeTheme {
            PreviewWelcome()
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun HomePreviewTest() {
        SoundscapeTheme {
            Home()
        }
    }

}