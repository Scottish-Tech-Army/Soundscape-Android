package org.scottishtecharmy.soundscape

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import org.scottishtecharmy.soundscape.screens.home.HomePreview
import org.scottishtecharmy.soundscape.screens.home.SettingsPreview
import org.scottishtecharmy.soundscape.screens.onboarding.Finish
import org.scottishtecharmy.soundscape.screens.onboarding.HearingPreview
import org.scottishtecharmy.soundscape.screens.onboarding.AudioBeaconPreview
import org.scottishtecharmy.soundscape.screens.onboarding.LanguagePreview
import org.scottishtecharmy.soundscape.screens.onboarding.ListeningPreview
import org.scottishtecharmy.soundscape.screens.onboarding.PreviewWelcome
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme

@Preview(name = "Dansk", locale = "da", group = "Language", showBackground = true, device = "id:small_phone")
@Preview(name = "Deutsch", locale = "de", group = "Language", showBackground = true, device = "id:small_phone")
@Preview(name = "Ελληνικά", locale = "el", group = "Language", showBackground = true, device = "id:small_phone")
@Preview(name = "English", locale = "en", group = "Language", showBackground = true, device = "id:small_phone")
@Preview(name = "Español", locale = "es", group = "Language", showBackground = true, device = "id:small_phone")
@Preview(name = "Suomi", locale = "fi", group = "Language", showBackground = true, device = "id:small_phone")
@Preview(name = "Français", locale = "fr", group = "Language", showBackground = true, device = "id:small_phone")
@Preview(name = "Italiano", locale = "it", group = "Language", showBackground = true, device = "id:small_phone")
@Preview(name = "日本語", locale = "ja", group = "Language", showBackground = true, device = "id:small_phone")
@Preview(name = "Norsk", locale = "nb", group = "Language", showBackground = true, device = "id:small_phone")
@Preview(name = "Nederlands", locale = "nl", group = "Language", showBackground = true, device = "id:small_phone")
@Preview(name = "Português (Brasil)", locale = "pt", group = "Language", showBackground = true, device = "id:small_phone")
@Preview(name = "Svenska", locale = "sv", group = "Language", showBackground = true, device = "id:small_phone")
annotation class LocalePreviews

@Preview(name = "SmallFont", fontScale = 0.85f, group = "FontScale", device = "id:small_phone")
@Preview(name = "LargeFont", fontScale = 1.15f, group = "FontScale", device = "id:small_phone")
annotation class FontSizePreviews

@Preview(name = "MediumPhone", group = "Devices", device = "spec:id=reference_phone,shape=Normal,width=411,height=891,unit=dp,dpi=420", showSystemUi = true)
@Preview(name = "Tablet", group = "Devices", device = Devices.PIXEL_C, showSystemUi = true)
annotation class DevicePreviews

@LocalePreviews
@FontSizePreviews
@DevicePreviews
annotation class CustomPreviews

class AudioBeaconsPreviewTestClass {
    @CustomPreviews
    @Composable
    fun AudioBeaconsPreviewTest() {
        SoundscapeTheme {
            AudioBeaconPreview()
        }
    }
}

class FinishPreviewTestClass {
    @CustomPreviews
    @Composable
    fun FinishPreviewTest() {
        SoundscapeTheme {
            Finish()
        }
    }
}

class HearingPreviewTestClass {
    @CustomPreviews
    @Composable
    fun HearingPreviewTest() {
        SoundscapeTheme {
            HearingPreview()
        }
    }
}

class LanguagePreviewTestClass {
    @CustomPreviews
    @Composable
    fun LanguagePreviewTest() {
        SoundscapeTheme {
            LanguagePreview()
        }
    }
}

class ListeningPreviewTestClass {
    @CustomPreviews
    @Composable
    fun ListeningPreviewTest() {
        SoundscapeTheme {
            ListeningPreview()
        }
    }
}

class WelcomePreviewTestClass {
    @CustomPreviews
    @Composable
    fun WelcomePreviewTest() {
        SoundscapeTheme {
            PreviewWelcome()
        }
    }
}

class HomePreviewTestClass {
    @CustomPreviews
    @Composable
    fun HomePreviewTest() {
        SoundscapeTheme {
            HomePreview()
        }
    }
}

class SettingsPreviewTestClass {
    @CustomPreviews
    @Composable
    fun SettingsPreviewTest() {
        SoundscapeTheme {
            SettingsPreview()
        }
    }
}