package org.scottishtecharmy.soundscape

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.audio.AudioTourInstruction
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.network.DownloadStateCommon
import org.scottishtecharmy.soundscape.preferences.PreferencesListener
import org.scottishtecharmy.soundscape.preferences.PreferencesProvider
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.tour_my_location
import org.scottishtecharmy.soundscape.screens.home.HomeState
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.data.LocationType
import org.scottishtecharmy.soundscape.screens.home.home.AudioTourInstructionDialog
import org.scottishtecharmy.soundscape.screens.home.home.BottomButtonFunctions
import org.scottishtecharmy.soundscape.screens.home.home.LicenseInfo
import org.scottishtecharmy.soundscape.screens.home.home.RouteFunctions
import org.scottishtecharmy.soundscape.screens.home.home.SearchFunctions
import org.scottishtecharmy.soundscape.screens.home.home.SharedAdvancedMarkersAndRoutesSettingsScreen
import org.scottishtecharmy.soundscape.screens.home.home.SharedDrawerContent
import org.scottishtecharmy.soundscape.screens.home.home.SharedHelpScreen
import org.scottishtecharmy.soundscape.screens.home.home.SharedHomeScreen
import org.scottishtecharmy.soundscape.screens.home.home.SharedLanguageMismatchDialog
import org.scottishtecharmy.soundscape.screens.home.home.SharedNewReleaseDialog
import org.scottishtecharmy.soundscape.screens.home.home.SharedOpenSourceLicensesScreen
import org.scottishtecharmy.soundscape.screens.home.home.SharedSleepScreen
import org.scottishtecharmy.soundscape.screens.home.home.SleepScreenState
import org.scottishtecharmy.soundscape.screens.home.home.StreetPreviewFunctions
import org.scottishtecharmy.soundscape.screens.home.locationDetails.SharedLocationDetailsScreen
import org.scottishtecharmy.soundscape.screens.home.locationDetails.SharedSaveAndEditMarkerScreen
import org.scottishtecharmy.soundscape.screens.home.offlinemaps.NearbyExtractsState
import org.scottishtecharmy.soundscape.screens.home.offlinemaps.OfflineMapsUiState
import org.scottishtecharmy.soundscape.screens.home.offlinemaps.SharedOfflineMapsScreen
import org.scottishtecharmy.soundscape.screens.home.placesnearby.PlacesNearbyScreen
import org.scottishtecharmy.soundscape.screens.home.placesnearby.PlacesNearbyUiState
import org.scottishtecharmy.soundscape.screens.home.settings.SharedSettingsScreen
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.MarkersAndRoutesUiState
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.markersscreen.MarkersScreen
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.routedetailsscreen.SharedRouteDetailsScreen
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.routesscreen.RoutesScreen
import org.scottishtecharmy.soundscape.screens.onboarding.accessibility.AccessibilityOnboardingScreen
import org.scottishtecharmy.soundscape.screens.onboarding.audiobeacons.AudioBeacons
import org.scottishtecharmy.soundscape.screens.onboarding.battery.BatteryOptimization
import org.scottishtecharmy.soundscape.screens.onboarding.finish.FinishScreen
import org.scottishtecharmy.soundscape.screens.onboarding.hearing.Hearing
import org.scottishtecharmy.soundscape.screens.onboarding.language.SharedLanguageScreen
import org.scottishtecharmy.soundscape.screens.onboarding.language.supportedLanguages
import org.scottishtecharmy.soundscape.screens.onboarding.listening.Listening
import org.scottishtecharmy.soundscape.screens.onboarding.permissions.PermissionsScreen
import org.scottishtecharmy.soundscape.screens.onboarding.terms.TermsScreen
import org.scottishtecharmy.soundscape.screens.onboarding.welcome.Welcome
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme

@Preview(
    name = "Arabic",
    locale = "arz",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "Danish",
    locale = "da",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "German",
    locale = "de",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "Greek",
    locale = "el",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "English",
    locale = "en",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "Spanish",
    locale = "es",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "Persian",
    locale = "fa",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "Finnish",
    locale = "fi",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "French",
    locale = "fr",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "Hindi",
    locale = "hi",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "Icelandic",
    locale = "is",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "Italian",
    locale = "it",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "Japanese",
    locale = "ja",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "Norwegian",
    locale = "nb",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "Netherlands",
    locale = "nl",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "Polish",
    locale = "pl",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "Portuguese (Brasil)",
    locale = "pt",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "Romanian",
    locale = "ro",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "Russian",
    locale = "ru",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "Swedish",
    locale = "sv",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "Turkish",
    locale = "tr",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "Ukrainian",
    locale = "uk",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "Chinese",
    locale = "zh",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
annotation class LocalePreviews

@Preview(name = "SmallFont", fontScale = 0.85f, group = "FontScale", device = "id:small_phone")
@Preview(name = "LargeFont", fontScale = 1.15f, group = "FontScale", device = "id:small_phone")
annotation class FontSizePreviews

@LocalePreviews
@FontSizePreviews
annotation class CustomPreviews

/**
 * This test is designed to spot theme issues where text or icons are set to the wrong color.
 * In that case, the previews will have visible text or icons. To enable that test, set testTheme to
 * true.
 */
const val testTheme = false

// ---------------------------------------------------------------------------
// Helpers used by the screen previews below.
// ---------------------------------------------------------------------------

/**
 * No-op preferences source used by previews. Returns the supplied defaults so
 * preference-driven UI renders in its baseline state.
 */
private object PreviewPreferencesProvider : PreferencesProvider {
    override fun getBoolean(key: String, default: Boolean): Boolean = default
    override fun getString(key: String, default: String): String = default
    override fun getFloat(key: String, default: Float): Float = default
    override fun putBoolean(key: String, value: Boolean) {}
    override fun putString(key: String, value: String) {}
    override fun clearAll() {}
    override fun addListener(listener: PreferencesListener) {}
    override fun removeListener(listener: PreferencesListener) {}
}

private fun previewLngLatAlt() = LngLatAlt(-4.2518, 55.8642)

private fun previewLocation(name: String): LocationDescription =
    LocationDescription(
        name = name,
        location = previewLngLatAlt(),
        locationType = LocationType.Street
    )

private fun previewMarkersList(): List<LocationDescription> = listOf(
    previewLocation("Home"),
    previewLocation("Work"),
    previewLocation("Coffee shop"),
    previewLocation("Library"),
)

private val previewBeaconTypes = listOf("Original", "Current", "Tactile", "Ping")

// ---------------------------------------------------------------------------
// Onboarding screens
// ---------------------------------------------------------------------------

@Preview(showBackground = true)
@Composable
fun PreviewWelcome() {
    Welcome(onNavigate = {})
}

@Preview(showBackground = true)
@Composable
fun TermsPreview() {
    TermsScreen(onNavigate = {})
}

@Preview(showBackground = true)
@Composable
fun LanguagePreview() {
    SharedLanguageScreen(
        supportedLanguages = supportedLanguages,
        selectedLanguageIndex = 0,
        onLanguageSelected = {},
        onContinue = {},
    )
}

@Preview(showBackground = true)
@Composable
fun ListeningPreview() {
    Listening(onNavigate = {})
}

@Preview(showBackground = true)
@Composable
fun HearingPreview() {
    Hearing(onContinue = {}, onPlaySpeech = {})
}

@Preview(showBackground = true)
@Composable
fun AudioBeaconPreview() {
    AudioBeacons(
        beacons = previewBeaconTypes,
        onBeaconSelected = {},
        selectedBeacon = previewBeaconTypes.first(),
        onContinue = {},
    )
}

@Preview(showBackground = true)
@Composable
fun AccessibilityOnboardingScreenPreview() {
    AccessibilityOnboardingScreen(
        isScreenReaderActive = false,
        preferencesProvider = PreviewPreferencesProvider,
        onNavigate = {},
    )
}

@Preview(showBackground = true)
@Composable
fun FinishPreview() {
    FinishScreen(onFinish = {})
}

@Preview(showBackground = true)
@Composable
fun PermissionsScreenPreview() {
    PermissionsScreen(onContinue = {})
}

@Preview(showBackground = true)
@Composable
fun BatteryOptimizationPreview() {
    BatteryOptimization(onContinue = {})
}

// ---------------------------------------------------------------------------
// Home + map experience
// ---------------------------------------------------------------------------

@Composable
private fun BaseHomePreview(state: HomeState) {
    SharedHomeScreen(
        state = state,
        onNavigate = {},
        onSelectLocation = {},
        preferencesProvider = PreviewPreferencesProvider,
        onMapLongClick = null,
        bottomButtonFunctions = BottomButtonFunctions(),
        routeFunctions = RouteFunctions(),
        streetPreviewFunctions = StreetPreviewFunctions(),
        searchFunctions = SearchFunctions(),
        getCurrentLocationDescription = { previewLocation("Current location") },
        rateSoundscape = {},
        contactSupport = {},
        shareRecording = {},
        toggleTutorial = {},
        tutorialRunning = false,
        recordingEnabled = false,
        voiceCommandListening = false,
        permissionsRequired = false,
        goToAppSettings = {},
        onSleep = {},
    )
}

@Preview(showBackground = true)
@Composable
fun HomePreview() {
    BaseHomePreview(HomeState(location = previewLngLatAlt()))
}

@Preview(showBackground = true)
@Composable
fun HomeSearchPreview() {
    BaseHomePreview(
        HomeState(
            location = previewLngLatAlt(),
            isSearching = true,
            searchItems = previewMarkersList(),
        ),
    )
}

@Preview(showBackground = true)
@Composable
fun HomeRoutePreview() {
    BaseHomePreview(
        HomeState(
            location = previewLngLatAlt(),
            routesTabSelected = true,
        ),
    )
}

@Preview(showBackground = true)
@Composable
fun SleepScreenPreview() {
    SharedSleepScreen(
        modifier = Modifier,
        onWakeUpNowClicked = {},
        onWakeOnLeaveClicked = {},
        state = SleepScreenState.Sleeping
    )
}

@Preview(showBackground = true)
@Composable
fun AdvancedMarkersAndRoutesSettingsPreview() {
    SharedAdvancedMarkersAndRoutesSettingsScreen(
        userFeedback = "",
        onUserFeedbackShown = {},
        onExport = {},
        onImport = {},
        onClearAll = {},
        onNavigateUp = {},
    )
}

@Preview(showBackground = true)
@Composable
fun OpenSourceLicensesPreview() {
    SharedOpenSourceLicensesScreen(
        licenses = listOf(
            LicenseInfo(
                project = "Sample Library",
                description = "A library used for testing the screenshot tooling.",
                version = "1.2.3",
                developers = listOf("Jane Doe", "Alex Smith"),
                url = "https://example.com",
                licenses = listOf("Apache-2.0" to "https://www.apache.org/licenses/LICENSE-2.0"),
            ),
            LicenseInfo(
                project = "Another Library",
                description = "Another sample.",
                version = "0.9.0",
                developers = emptyList(),
                url = null,
                licenses = listOf("MIT" to "https://opensource.org/licenses/MIT"),
            ),
        ),
        onNavigateUp = {},
        onLicenseClick = {},
    )
}

// ---------------------------------------------------------------------------
// Help screens
// ---------------------------------------------------------------------------

@Preview(showBackground = true)
@Composable
fun HelpScreenMenuPreview() {
    // No matching topic → renders the help index.
    SharedHelpScreen(topic = "", onNavigate = {}, onNavigateUp = {})
}

@Preview(showBackground = true)
@Composable
fun BeaconHelpPreview() {
    SharedHelpScreen(topic = "pagebeacon_audio_beacon", onNavigate = {}, onNavigateUp = {})
}

@Preview(showBackground = true)
@Composable
fun VoicesHelpPreview() {
    SharedHelpScreen(topic = "pagevoice_voices", onNavigate = {}, onNavigateUp = {})
}

// ---------------------------------------------------------------------------
// Drawer + dialogs (modal/overlay UI reused on top of Home).
// ---------------------------------------------------------------------------

@Preview(showBackground = true)
@Composable
fun PreviewDrawerContent() {
    SharedDrawerContent(
        onClose = {},
        onNavigate = {},
        rateSoundscape = {},
        contactSupport = {},
        shareRecording = {},
        offlineMaps = {},
        toggleTutorial = {},
        tutorialRunning = false,
        recordingEnabled = false,
        newReleaseDialog = null,
    )
}

@Preview
@Composable
fun AudioTourDialogTest() {
    AudioTourInstructionDialog(
        instruction = AudioTourInstruction(stringResource(Res.string.tour_my_location)),
        onContinue = {},
    )
}

@Preview
@Composable
fun NewReleaseDialogPreview() {
    SharedNewReleaseDialog(
        innerPadding = PaddingValues(),
        preferencesProvider = PreviewPreferencesProvider,
        newReleaseDialog = remember { mutableStateOf(true) },
    )
}

// ---------------------------------------------------------------------------
// Settings
// ---------------------------------------------------------------------------

@Preview(showBackground = true)
@Composable
fun SettingsPreview() {
    SharedSettingsScreen(
        onNavigateUp = {},
        beaconTypes = previewBeaconTypes,
        preferencesProvider = PreviewPreferencesProvider,
        onNavigateToAdvancedMarkersAndRoutes = {},
        onResetSettings = {},
    )
}

// ---------------------------------------------------------------------------
// Location details + markers
// ---------------------------------------------------------------------------

@Preview(showBackground = true)
@Composable
fun LocationDetailsPreview() {
    SharedLocationDetailsScreen(
        locationDescription = previewLocation("21 Buchanan Street, Glasgow"),
        userLocation = previewLngLatAlt(),
        heading = 0f,
        preferencesProvider = PreviewPreferencesProvider,
        onNavigateUp = {},
        onStartBeacon = { _, _ -> },
        onSaveMarker = {},
        onEditMarker = {},
        onDeleteMarker = {},
        onEnableStreetPreview = {},
        onShareLocation = {},
        onOfflineMaps = {},
    )
}

@Preview(showBackground = true)
@Composable
fun SaveAndEditMarkerPreview() {
    SharedSaveAndEditMarkerScreen(
        locationDescription = previewLocation("Favourite cafe"),
        userLocation = previewLngLatAlt(),
        heading = 0f,
        preferencesProvider = PreviewPreferencesProvider,
        onCancel = {},
        onSave = {},
        onDelete = {},
    )
}

// ---------------------------------------------------------------------------
// Offline maps
// ---------------------------------------------------------------------------

@Preview(showBackground = true)
@Composable
fun OfflineMapsScreenPreview() {
    SharedOfflineMapsScreen(
        uiState = OfflineMapsUiState(
            nearbyExtractsState = NearbyExtractsState.Loaded(FeatureCollection()),
            downloadedExtracts = FeatureCollection(),
        ),
        downloadState = MutableStateFlow(DownloadStateCommon.Idle),
        onBack = {},
        onDownload = { _, _ -> },
        onDelete = {},
        onCancelDownload = {},
        preferencesProvider = PreviewPreferencesProvider,
    )
}

@Preview(showBackground = true)
@Composable
fun OfflineMapsScreenDownloadingPreview() {
    SharedOfflineMapsScreen(
        uiState = OfflineMapsUiState(
            downloadingExtractName = "Glasgow",
            nearbyExtractsState = NearbyExtractsState.Loaded(FeatureCollection()),
            downloadedExtracts = FeatureCollection(),
        ),
        downloadState = MutableStateFlow(DownloadStateCommon.Downloading(progress = 420)),
        onBack = {},
        onDownload = { _, _ -> },
        onDelete = {},
        onCancelDownload = {},
        preferencesProvider = PreviewPreferencesProvider,
    )
}

// ---------------------------------------------------------------------------
// Places nearby + markers/routes
// ---------------------------------------------------------------------------

@Preview(showBackground = true)
@Composable
fun PlacesNearbyPreview() {
    PlacesNearbyScreen(
        uiState = PlacesNearbyUiState(
            userLocation = previewLngLatAlt(),
            title = "Places nearby",
        ),
        onSelectItem = {},
    )
}

@Preview(showBackground = true)
@Composable
fun MarkersScreenPopulatedPreview() {
    MarkersScreen(
        uiState = MarkersAndRoutesUiState(
            entries = previewMarkersList(),
            markers = true,
            userLocation = previewLngLatAlt(),
        ),
        clearErrorMessage = {},
        onToggleSortOrder = {},
        onToggleSortByName = {},
        userLocation = previewLngLatAlt(),
        onSelectItem = {},
    )
}

@Preview(showBackground = true)
@Composable
fun RoutesScreenPreview() {
    RoutesScreen(
        uiState = MarkersAndRoutesUiState(),
        userLocation = previewLngLatAlt(),
        clearErrorMessage = {},
        onToggleSortOrder = {},
        onToggleSortByName = {},
        onSelectItem = {},
    )
}

@Preview(showBackground = true)
@Composable
fun RoutesScreenPopulatedPreview() {
    RoutesScreen(
        uiState = MarkersAndRoutesUiState(
            entries = previewMarkersList(),
            userLocation = previewLngLatAlt(),
        ),
        userLocation = previewLngLatAlt(),
        clearErrorMessage = {},
        onToggleSortOrder = {},
        onToggleSortByName = {},
        onSelectItem = {},
    )
}

@Preview(showBackground = true)
@Composable
fun RoutesDetailsPopulatedPreview() {
    SharedRouteDetailsScreen(
        routeName = "Morning loop",
        routeDescription = "A short walk through the park and back via the cafe.",
        waypoints = previewMarkersList(),
        isRoutePlaying = false,
        userLocation = previewLngLatAlt(),
        heading = 0f,
        preferencesProvider = PreviewPreferencesProvider,
        onNavigateUp = {},
        onStartRoute = {},
        onStartRouteInReverse = {},
        onStopRoute = {},
        onEditRoute = {},
        onShareRoute = {},
    )
}

// ---------------------------------------------------------------------------
// Screenshot test wrappers — the screenshot plugin renders these (multiplied
// by @CustomPreviews). Each one applies the SoundscapeTheme so the captured
// screenshot matches in-app rendering.
// ---------------------------------------------------------------------------

class ThemeTestClass {

    @CustomPreviews
    @Composable
    @PreviewTest
    fun PreviewWelcomeTest() {
        SoundscapeTheme(testTheme = testTheme) { PreviewWelcome() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun TermsPreviewTest() {
        SoundscapeTheme(testTheme = testTheme) { TermsPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun LanguagePreviewTest() {
        SoundscapeTheme(testTheme = testTheme) { LanguagePreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun ListeningPreviewTest() {
        SoundscapeTheme(testTheme = testTheme) { ListeningPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun HearingPreviewTest() {
        SoundscapeTheme(testTheme = testTheme) { HearingPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun AudioBeaconsPreviewTest() {
        SoundscapeTheme(testTheme = testTheme) { AudioBeaconPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun AccessibilityPreviewTest() {
        SoundscapeTheme(testTheme = testTheme) { AccessibilityOnboardingScreenPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun FinishPreviewTest() {
        SoundscapeTheme(testTheme = testTheme) { FinishPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun PermissionsPreviewTest() {
        SoundscapeTheme(testTheme = testTheme) { PermissionsScreenPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun BatteryOptimizationPreviewTest() {
        SoundscapeTheme(testTheme = testTheme) { BatteryOptimizationPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun HomePreviewTest() {
        SoundscapeTheme(testTheme = testTheme) { HomePreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun HomeRoutePreviewTest() {
        SoundscapeTheme(testTheme = testTheme) { HomeRoutePreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun HomeSearchPreviewTest() {
        SoundscapeTheme(testTheme = testTheme) { HomeSearchPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun SleepScreenPreviewTest() {
        SoundscapeTheme(testTheme = testTheme) { SleepScreenPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun AdvancedMarkersAndRoutesSettingsPreviewTest() {
        SoundscapeTheme(testTheme = testTheme) { AdvancedMarkersAndRoutesSettingsPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun OpenSourceLicensesPreviewTest() {
        SoundscapeTheme(testTheme = testTheme) { OpenSourceLicensesPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun HelpScreenMenuPreviewTest() {
        SoundscapeTheme(testTheme = testTheme) { HelpScreenMenuPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun BeaconHelpPreviewTest() {
        SoundscapeTheme(testTheme = testTheme) { BeaconHelpPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun VoicesHelpPreviewTest() {
        SoundscapeTheme(testTheme = testTheme) { VoicesHelpPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun PreviewDrawerContentTest() {
        SoundscapeTheme(testTheme = testTheme) { PreviewDrawerContent() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun AudioTourDialogTestTest() {
        SoundscapeTheme(testTheme = testTheme) { AudioTourDialogTest() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun NewReleaseDialogPreviewTest() {
        SoundscapeTheme(testTheme = testTheme) { NewReleaseDialogPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun SettingsPreviewTest() {
        SoundscapeTheme(testTheme = testTheme) { SettingsPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun LocationDetailsPreviewTest() {
        SoundscapeTheme(testTheme = testTheme) { LocationDetailsPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun SaveAndEditMarkerPreviewTest() {
        SoundscapeTheme(testTheme = testTheme) { SaveAndEditMarkerPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun OfflineMapsScreenPreviewTest() {
        SoundscapeTheme(testTheme = testTheme) { OfflineMapsScreenPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun OfflineMapsScreenDownloadingPreviewTest() {
        SoundscapeTheme(testTheme = testTheme) { OfflineMapsScreenDownloadingPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun PlacesNearbyPreviewTest() {
        SoundscapeTheme(testTheme = testTheme) { PlacesNearbyPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun MarkersScreenPopulatedPreviewTest() {
        SoundscapeTheme(testTheme = testTheme) { MarkersScreenPopulatedPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun RoutesScreenPreviewTest() {
        SoundscapeTheme(testTheme = testTheme) { RoutesScreenPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun RoutesScreenPopulatedPreviewTest() {
        SoundscapeTheme(testTheme = testTheme) { RoutesScreenPopulatedPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun RoutesDetailsPopulatedPreviewTest() {
        SoundscapeTheme(testTheme = testTheme) { RoutesDetailsPopulatedPreview() }
    }
}
