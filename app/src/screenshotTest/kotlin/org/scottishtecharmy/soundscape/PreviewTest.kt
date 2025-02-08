package org.scottishtecharmy.soundscape

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import org.scottishtecharmy.soundscape.screens.home.home.FaqHelpPreview
import org.scottishtecharmy.soundscape.screens.home.home.HomeHelpPreview
import org.scottishtecharmy.soundscape.screens.home.home.HomePreview
import org.scottishtecharmy.soundscape.screens.home.settings.SettingsPreview
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.MarkersAndRoutesPreview
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen.EditRouteScreenPreview
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen.NewRouteScreenPreview
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.markersscreen.MarkersScreenPopulatedPreview
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.routedetailsscreen.RoutesDetailsPopulatedPreview
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.routesscreen.RoutesScreenPopulatedPreview
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.routesscreen.RoutesScreenPreview
import org.scottishtecharmy.soundscape.screens.onboarding.audiobeacons.AudioBeaconPreview
import org.scottishtecharmy.soundscape.screens.onboarding.finish.FinishScreen
import org.scottishtecharmy.soundscape.screens.onboarding.hearing.HearingPreview
import org.scottishtecharmy.soundscape.screens.onboarding.language.LanguagePreview
import org.scottishtecharmy.soundscape.screens.onboarding.listening.ListeningPreview
import org.scottishtecharmy.soundscape.screens.onboarding.welcome.PreviewWelcome
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

@Preview(name = "MediumPhone", group = "Devices", device = "spec:width=411dp,height=891dp,dpi=420", showSystemUi = true)
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
            FinishScreen(
                onFinish = {}
            )
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

class MarkersAndRoutesPreviewTestClass {
    @CustomPreviews
    @Composable
    fun MarkersAndRoutesPreviewTest() {
        SoundscapeTheme {
            MarkersAndRoutesPreview()
        }
    }
}

class RoutesPreviewTestClass {
    @CustomPreviews
    @Composable
    fun RoutesPreviewTest() {
        SoundscapeTheme {
            RoutesScreenPreview()
        }
    }
}

class MarkersPreviewTestClass {
    @CustomPreviews
    @Composable
    fun MarkersPreviewTest() {
        SoundscapeTheme {
            MarkersScreenPopulatedPreview()
        }
    }
}

class NewRouteScreenPreviewTestClass {
    @CustomPreviews
    @Composable
    fun NewRouteScreenPreviewTest() {
        SoundscapeTheme {
            NewRouteScreenPreview()
        }
    }
}

class EditRouteScreenPreviewTestClass {
    @CustomPreviews
    @Composable
    fun EditRouteScreenPreviewTest() {
        SoundscapeTheme {
            EditRouteScreenPreview()
        }
    }
}

class RouteDetailsPreviewTestClass {
    @CustomPreviews
    @Composable
    fun RouteDetailsPreviewTest() {
        SoundscapeTheme {
            RoutesDetailsPopulatedPreview()
        }
    }
}

class RoutesScreenPopulatedPreviewTestClass {
    @CustomPreviews
    @Composable
    fun RoutesScreenPopulatedPreviewTest() {
        SoundscapeTheme {
            RoutesScreenPopulatedPreview()
        }
    }
}

class HelpScreenPreviewTestClass {
    @CustomPreviews
    @Composable
    fun HelpScreenPreviewTest() {
        SoundscapeTheme {
            HomeHelpPreview()
        }
    }
}

class FaqPreviewTestClass {
    @CustomPreviews
    @Composable
    fun FaqPreviewTest() {
        SoundscapeTheme {
            FaqHelpPreview()
        }
    }
}
