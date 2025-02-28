package org.scottishtecharmy.soundscape

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import org.scottishtecharmy.soundscape.screens.home.PreviewDrawerContent
import org.scottishtecharmy.soundscape.screens.home.home.FaqHelpPreview
import org.scottishtecharmy.soundscape.screens.home.home.HomeHelpPreview
import org.scottishtecharmy.soundscape.screens.home.home.HomePreview
import org.scottishtecharmy.soundscape.screens.home.home.HomeRoutePreview
import org.scottishtecharmy.soundscape.screens.home.home.HomeSearchPreview
import org.scottishtecharmy.soundscape.screens.home.home.SleepScreenPreview
import org.scottishtecharmy.soundscape.screens.home.locationDetails.AddRouteScreenPreview
import org.scottishtecharmy.soundscape.screens.home.locationDetails.LocationDetailsPreview
import org.scottishtecharmy.soundscape.screens.home.placesnearby.PlacesNearbyPreview
import org.scottishtecharmy.soundscape.screens.home.settings.SettingsPreview
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen.AddWaypointsScreenPopulatedPreview
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

/**
 * This test is designed to spot theme issues where text or icons are set to the wrong color.In
 * that case, the previews will have visible text or icons. To enable that test, set testTheme to
 * true.
 */
const val testTheme = false
class ThemeTestClass {

    @CustomPreviews
    @Composable
    fun AudioBeaconsPreviewTest() {
        SoundscapeTheme(testTheme = testTheme) {
            AudioBeaconPreview()
        }
    }

    @CustomPreviews
    @Composable
    fun FinishPreviewTest() {
        SoundscapeTheme(testTheme = testTheme) {
            FinishScreen(
                onFinish = {}
            )
        }
    }

    @CustomPreviews
    @Composable
    fun HearingPreviewTest() {
        SoundscapeTheme(testTheme = testTheme) {
            HearingPreview()
        }
    }

    @CustomPreviews
    @Composable
    fun LanguagePreviewTest() {
        SoundscapeTheme(testTheme = testTheme) {
            LanguagePreview()
        }
    }

    @CustomPreviews
    @Composable
    fun ListeningPreviewTest() {
        SoundscapeTheme(testTheme = testTheme) {
            ListeningPreview()
        }
    }

    @CustomPreviews
    @Composable
    fun HomePreviewTest() {
        SoundscapeTheme(testTheme = testTheme) {
            HomePreview()
        }
    }

    @CustomPreviews
    @Composable
    fun HomeRoutePreviewTest() {
        SoundscapeTheme(testTheme = testTheme) {
            HomeRoutePreview()
        }
    }

    @CustomPreviews
    @Composable
    fun HomeSearchPreviewTest() {
        SoundscapeTheme(testTheme = testTheme) {
            HomeSearchPreview()
        }
    }

    @CustomPreviews
    @Composable
    fun SettingsPreviewTest() {
        SoundscapeTheme(testTheme = testTheme) {
            SettingsPreview()
        }
    }

    @CustomPreviews
    @Composable
    fun RoutesPreviewTest() {
        SoundscapeTheme(testTheme = testTheme) {
            RoutesScreenPreview()
        }
    }

    @CustomPreviews
    @Composable
    fun MarkersPreviewTest() {
        SoundscapeTheme(testTheme = testTheme) {
            MarkersScreenPopulatedPreview()
        }
    }

    @CustomPreviews
    @Composable
    fun NewRouteScreenPreviewTest() {
        SoundscapeTheme(testTheme = testTheme) {
            NewRouteScreenPreview()
        }
    }

    @CustomPreviews
    @Composable
    fun EditRouteScreenPreviewTest() {
        SoundscapeTheme(testTheme = testTheme) {
            EditRouteScreenPreview()
        }
    }

    @CustomPreviews
    @Composable
    fun RouteDetailsPreviewTest() {
        SoundscapeTheme(testTheme = testTheme) {
            RoutesDetailsPopulatedPreview()
        }
    }

    @CustomPreviews
    @Composable
    fun RoutesScreenPopulatedPreviewTest() {
        SoundscapeTheme(testTheme = testTheme) {
            RoutesScreenPopulatedPreview()
        }
    }

    @CustomPreviews
    @Composable
    fun HelpScreenPreviewTest() {
        SoundscapeTheme(testTheme = testTheme) {
            HomeHelpPreview()
        }
    }
    @CustomPreviews
    @Composable
    fun FaqHelpPreviewTest() {
        SoundscapeTheme(testTheme = testTheme) {
            FaqHelpPreview()
        }
    }

    @CustomPreviews
    @Composable
    fun SleepScreenPreviewTest() {
        SoundscapeTheme(testTheme = testTheme) {
            SleepScreenPreview()
        }
    }

    @CustomPreviews
    @Composable
    fun LocationDetailsPreviewTest() {
        SoundscapeTheme(testTheme = testTheme) {
            LocationDetailsPreview()
        }
    }

    @CustomPreviews
    @Composable
    fun AddRouteScreenPreviewTest() {
        SoundscapeTheme(testTheme = testTheme) {
            AddRouteScreenPreview()
        }
    }

    @CustomPreviews
    @Composable
    fun PlacesNearbyPreviewTest() {
        SoundscapeTheme(testTheme = testTheme) {
            PlacesNearbyPreview()
        }
    }

    @CustomPreviews
    @Composable
    fun PreviewDrawerContentTest() {
        SoundscapeTheme(testTheme = testTheme) {
            PreviewDrawerContent()
        }
    }

    @CustomPreviews
    @Composable
    fun AddWaypointsScreenPopulatedPreviewText() {
        SoundscapeTheme(testTheme = testTheme) {
            AddWaypointsScreenPopulatedPreview()
        }
    }
}