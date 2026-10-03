package org.scottishtecharmy.soundscape

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.audio.AudioTourInstruction
import org.scottishtecharmy.soundscape.database.local.dao.RouteDao
import org.scottishtecharmy.soundscape.database.local.model.MarkerEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewChoice
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewEnabled
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewState
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.network.DownloadStateCommon
import org.scottishtecharmy.soundscape.platform.appVersionMinorTrimmed
import org.scottishtecharmy.soundscape.preferences.PreferenceKeys
import org.scottishtecharmy.soundscape.preferences.PreferencesListener
import org.scottishtecharmy.soundscape.preferences.PreferencesProvider
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.offline_map_details_title
import org.scottishtecharmy.soundscape.resources.tour_my_location
import org.scottishtecharmy.soundscape.resources.ui_back_button_title
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
import org.scottishtecharmy.soundscape.screens.home.home.SharedNewReleaseDialog
import org.scottishtecharmy.soundscape.screens.home.home.SharedOpenSourceLicensesScreen
import org.scottishtecharmy.soundscape.screens.home.home.SharedSleepScreen
import org.scottishtecharmy.soundscape.screens.home.home.SleepScreenState
import org.scottishtecharmy.soundscape.screens.home.home.StreetPreviewFunctions
import org.scottishtecharmy.soundscape.screens.home.locationDetails.SharedLocationDetailsScreen
import org.scottishtecharmy.soundscape.screens.home.locationDetails.SharedSaveAndEditMarkerScreen
import org.scottishtecharmy.soundscape.screens.home.offlinemaps.NearbyExtractsState
import org.scottishtecharmy.soundscape.screens.home.offlinemaps.OfflineMapsUiState
import org.scottishtecharmy.soundscape.screens.home.offlinemaps.SharedOfflineMapExtractDetails
import org.scottishtecharmy.soundscape.screens.home.offlinemaps.SharedOfflineMapsScreen
import org.scottishtecharmy.soundscape.screens.home.placesnearby.PlacesNearbyScreen
import org.scottishtecharmy.soundscape.screens.home.placesnearby.PlacesNearbyUiState
import org.scottishtecharmy.soundscape.screens.home.settings.SharedSettingsScreen
import org.scottishtecharmy.soundscape.screens.markers_routes.components.FlexibleAppBar
import org.scottishtecharmy.soundscape.screens.markers_routes.components.IconWithTextButton
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.MarkersAndRoutesUiState
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen.AddAndEditRouteUiState
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen.AddAndEditRouteViewModel
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen.AddWaypointsDialog
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen.SharedAddAndEditRouteScreen
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.markersscreen.MarkersScreen
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.markersscreen.SharedAddMarkerScreen
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.routedetailsscreen.SharedRouteDetailsScreen
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.routesscreen.RoutesScreen
import org.scottishtecharmy.soundscape.screens.migration.LegacyMigrationScreenContent
import org.scottishtecharmy.soundscape.screens.migration.LegacyMigrationUiState
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
import org.scottishtecharmy.soundscape.services.BeaconState
import org.scottishtecharmy.soundscape.services.RoutePlayerState
import org.scottishtecharmy.soundscape.services.ServiceConnection
import org.scottishtecharmy.soundscape.services.mediacontrol.MediaControllableService
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme

// Each preview is named with its language's Weblate code, the one the translation
// questionnaires use (docs/translation-questions/questions-<code>.md). The images are sorted
// into a folder per name, and screenshots.yaml publishes each as screenshots-<code>.zip.
@Preview(
    name = "ar",
    locale = "ar",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "bg",
    locale = "bg",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "bn",
    locale = "bn",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "ca",
    locale = "ca",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "cs",
    locale = "cs",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "da",
    locale = "da",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "de",
    locale = "de",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "el",
    locale = "el",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "en",
    locale = "en",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "en_GB",
    locale = "en-rGB",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "es",
    locale = "es",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "et",
    locale = "et",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "fa",
    locale = "fa",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "fi",
    locale = "fi",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "fr",
    locale = "fr",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "fr_CA",
    locale = "fr-rCA",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "ha",
    locale = "ha",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "hi",
    locale = "hi",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "hr",
    locale = "hr",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "hu",
    locale = "hu",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "id",
    locale = "id",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "is",
    locale = "is",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "it",
    locale = "it",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "ja",
    locale = "ja",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "ko",
    locale = "ko",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "mr",
    locale = "mr",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "nb_NO",
    locale = "nb",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "nl",
    locale = "nl",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "pa",
    locale = "pa",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "pl",
    locale = "pl",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "pt",
    locale = "pt",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "pt_BR",
    locale = "pt-rBR",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "ro",
    locale = "ro",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "ru",
    locale = "ru",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "sk",
    locale = "sk",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "sl",
    locale = "sl",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "sr",
    locale = "sr",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "sv",
    locale = "sv",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "sw",
    locale = "sw",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "ta",
    locale = "ta",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "te",
    locale = "te",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "th",
    locale = "th",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "tr",
    locale = "tr",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "uk",
    locale = "uk",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "ur",
    locale = "ur",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "vi",
    locale = "vi",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
@Preview(
    name = "zh_Hans",
    locale = "zh",
    group = "Language",
    showBackground = true,
    device = "id:small_phone"
)
annotation class LocalePreviews

/**
 * A phone-width screen tall enough to show the whole of a long scrolling page, so that a native
 * speaker can read all of its text in one image.
 */
const val TALL_DEVICE = "spec:width=360dp,height=2000dp,dpi=320"

@Preview(
    name = "ar",
    locale = "ar",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
@Preview(
    name = "bg",
    locale = "bg",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
@Preview(
    name = "bn",
    locale = "bn",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
@Preview(
    name = "ca",
    locale = "ca",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
@Preview(
    name = "cs",
    locale = "cs",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
@Preview(
    name = "da",
    locale = "da",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
@Preview(
    name = "de",
    locale = "de",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
@Preview(
    name = "el",
    locale = "el",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
@Preview(
    name = "en",
    locale = "en",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
@Preview(
    name = "en_GB",
    locale = "en-rGB",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
@Preview(
    name = "es",
    locale = "es",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
@Preview(
    name = "et",
    locale = "et",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
@Preview(
    name = "fa",
    locale = "fa",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
@Preview(
    name = "fi",
    locale = "fi",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
@Preview(
    name = "fr",
    locale = "fr",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
@Preview(
    name = "fr_CA",
    locale = "fr-rCA",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
@Preview(
    name = "ha",
    locale = "ha",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
@Preview(
    name = "hi",
    locale = "hi",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
@Preview(
    name = "hr",
    locale = "hr",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
@Preview(
    name = "hu",
    locale = "hu",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
@Preview(
    name = "id",
    locale = "id",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
@Preview(
    name = "is",
    locale = "is",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
@Preview(
    name = "it",
    locale = "it",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
@Preview(
    name = "ja",
    locale = "ja",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
@Preview(
    name = "ko",
    locale = "ko",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
@Preview(
    name = "mr",
    locale = "mr",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
@Preview(
    name = "nb_NO",
    locale = "nb",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
@Preview(
    name = "nl",
    locale = "nl",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
@Preview(
    name = "pa",
    locale = "pa",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
@Preview(
    name = "pl",
    locale = "pl",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
@Preview(
    name = "pt",
    locale = "pt",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
@Preview(
    name = "pt_BR",
    locale = "pt-rBR",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
@Preview(
    name = "ro",
    locale = "ro",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
@Preview(
    name = "ru",
    locale = "ru",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
@Preview(
    name = "sk",
    locale = "sk",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
@Preview(
    name = "sl",
    locale = "sl",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
@Preview(
    name = "sr",
    locale = "sr",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
@Preview(
    name = "sv",
    locale = "sv",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
@Preview(
    name = "sw",
    locale = "sw",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
@Preview(
    name = "ta",
    locale = "ta",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
@Preview(
    name = "te",
    locale = "te",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
@Preview(
    name = "th",
    locale = "th",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
@Preview(
    name = "tr",
    locale = "tr",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
@Preview(
    name = "uk",
    locale = "uk",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
@Preview(
    name = "ur",
    locale = "ur",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
@Preview(
    name = "vi",
    locale = "vi",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
@Preview(
    name = "zh_Hans",
    locale = "zh",
    group = "Language",
    showBackground = true,
    device = TALL_DEVICE
)
annotation class TallLocalePreviews

@Preview(name = "SmallFont", fontScale = 0.85f, group = "FontScale", device = "id:small_phone")
@Preview(name = "LargeFont", fontScale = 1.15f, group = "FontScale", device = "id:small_phone")
annotation class FontSizePreviews

@LocalePreviews
@FontSizePreviews
annotation class CustomPreviews

@Preview(name = "SmallFont", fontScale = 0.85f, group = "FontScale", device = TALL_DEVICE)
@Preview(name = "LargeFont", fontScale = 1.15f, group = "FontScale", device = TALL_DEVICE)
annotation class TallFontSizePreviews

/** For long scrolling pages: [CustomPreviews] on a [TALL_DEVICE]. */
@TallLocalePreviews
@TallFontSizePreviews
annotation class TallCustomPreviews

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
    // Report this release's dialog as already seen, otherwise it covers every Home preview.
    // It has a preview of its own, NewReleaseDialogPreview.
    override fun getString(key: String, default: String): String =
        if (key == PreferenceKeys.LAST_NEW_RELEASE) appVersionMinorTrimmed() else default
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

private fun previewRoute(): RouteWithMarkers = RouteWithMarkers(
    RouteEntity(name = "Morning loop", description = "A short walk through the park and back via the cafe."),
    listOf(
        MarkerEntity(name = "Home", longitude = -4.2518, latitude = 55.8642),
        MarkerEntity(name = "Park gate", longitude = -4.2530, latitude = 55.8650),
        MarkerEntity(name = "Coffee shop", longitude = -4.2540, latitude = 55.8660),
    ),
)

/** The route editor only touches the database when saving, so nothing here is ever called. */
private val previewRouteDao = java.lang.reflect.Proxy.newProxyInstance(
    RouteDao::class.java.classLoader,
    arrayOf(RouteDao::class.java),
) { _, _, _ -> null } as RouteDao

private object PreviewServiceConnection : ServiceConnection {
    override val serviceBoundState: StateFlow<Boolean> = MutableStateFlow(false)
    override val service: MediaControllableService? = null
}

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
fun MarkersScreenPreview() {
    MarkersScreen(
        uiState = MarkersAndRoutesUiState(markers = true),
        clearErrorMessage = {},
        onCycleSort = {},
        userLocation = previewLngLatAlt(),
        onSelectItem = {},
    )
}

@Preview(showBackground = true)
@Composable
fun AddMarkerScreenPreview() {
    SharedAddMarkerScreen(
        placesNearbyUiState = PlacesNearbyUiState(userLocation = previewLngLatAlt()),
        userLocation = previewLngLatAlt(),
        heading = 0f,
        preferencesProvider = null,
        getCurrentLocationDescription = { previewLocation("Current location") },
        onClickFolder = { _, _ -> },
        onClickBack = {},
        onCancel = {},
        onSave = {},
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
        onCycleSort = {},
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
        onCycleSort = {},
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
        onCycleSort = {},
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
// Home states with their own text: beacon set, route playing, street preview.
// ---------------------------------------------------------------------------

@Preview(showBackground = true)
@Composable
fun HomeBeaconPreview() {
    BaseHomePreview(
        HomeState(
            location = previewLngLatAlt(),
            beaconState = BeaconState(location = previewLngLatAlt(), name = "Coffee shop"),
        ),
    )
}

@Preview(showBackground = true)
@Composable
fun HomeRoutePlayingPreview() {
    BaseHomePreview(
        HomeState(
            location = previewLngLatAlt(),
            beaconState = BeaconState(location = previewLngLatAlt(), name = "Park gate"),
            currentRouteData = RoutePlayerState(routeData = previewRoute(), currentWaypoint = 1),
        ),
    )
}

@Preview(showBackground = true)
@Composable
fun HomeStreetPreviewPreview() {
    BaseHomePreview(
        HomeState(
            location = previewLngLatAlt(),
            streetPreviewState = StreetPreviewState(
                enabled = StreetPreviewEnabled.ON,
                choices = listOf(
                    StreetPreviewChoice(0.0, "Buchanan Street", Way()),
                    StreetPreviewChoice(90.0, "Gordon Street", Way()),
                    StreetPreviewChoice(180.0, "Buchanan Street", Way()),
                ),
            ),
        ),
    )
}

// ---------------------------------------------------------------------------
// Settings, one preview per expandable section.
// ---------------------------------------------------------------------------

@Composable
private fun SettingsSectionPreview(section: String) {
    SharedSettingsScreen(
        onNavigateUp = {},
        beaconTypes = previewBeaconTypes,
        preferencesProvider = PreviewPreferencesProvider,
        onNavigateToAdvancedMarkersAndRoutes = {},
        onResetSettings = {},
        initialExpandedSection = section,
    )
}

// ---------------------------------------------------------------------------
// Route editing
// ---------------------------------------------------------------------------

@Preview(showBackground = true)
@Composable
fun AddAndEditRoutePreview() {
    val holder = remember {
        AddAndEditRouteViewModel(previewRouteDao, PreviewServiceConnection).apply {
            initializeFromImport(previewRoute())
        }
    }
    SharedAddAndEditRouteScreen(
        holder = holder,
        isEditing = true,
        userLocation = previewLngLatAlt(),
        heading = 0f,
        getCurrentLocationDescription = { previewLocation("Current location") },
        onNavigateUp = {},
        onSaveComplete = {},
        onDeleteComplete = {},
    )
}

@Preview(showBackground = true)
@Composable
fun AddWaypointsDialogPreview() {
    AddWaypointsDialog(
        uiState = AddAndEditRouteUiState(
            markers = previewMarkersList(),
            toggledMembers = previewMarkersList().take(2),
        ),
        placesNearbyUiState = PlacesNearbyUiState(userLocation = previewLngLatAlt()),
        modifier = Modifier,
        onAddWaypointComplete = {},
        onClickFolder = { _, _ -> },
        onClickBack = {},
        onSelectLocation = {},
        onToggleMember = {},
        createAndAddMarker = { _, _, _, _ -> },
        userLocation = previewLngLatAlt(),
        getCurrentLocationDescription = { previewLocation("Current location") },
        heading = 0f,
    )
}

// ---------------------------------------------------------------------------
// Offline map extract details + legacy iOS migration
// ---------------------------------------------------------------------------

@Preview(showBackground = true)
@Composable
fun OfflineMapExtractDetailsPreview() {
    val extract = remember {
        Feature().apply {
            properties = hashMapOf<String, Any?>(
                "name" to "Glasgow",
                "city_names" to listOf("Glasgow", "Paisley", "East Kilbride"),
                "extract-size-string" to "120 MB",
            )
        }
    }
    // The details draw no background or title bar of their own: in the app they sit inside
    // SharedOfflineMapsScreen's Scaffold, so this reproduces that.
    Scaffold(
        topBar = {
            FlexibleAppBar(
                title = stringResource(Res.string.offline_map_details_title),
                leftSide = {
                    IconWithTextButton(
                        text = stringResource(Res.string.ui_back_button_title),
                        color = MaterialTheme.colorScheme.onSurface,
                    ) {}
                },
            )
        },
    ) { padding ->
        SharedOfflineMapExtractDetails(
            extract = extract,
            downloadExtract = { _, _ -> },
            deleteExtract = {},
            local = false,
            userLocation = previewLngLatAlt(),
            preferencesProvider = PreviewPreferencesProvider,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface),
        )
    }
}

@Composable
private fun LegacyMigrationPreview(state: LegacyMigrationUiState) {
    LegacyMigrationScreenContent(state = state, onRetry = {}, onContinue = {})
}

// ---------------------------------------------------------------------------
// Screenshot test wrappers — the screenshot plugin renders these (multiplied
// by @CustomPreviews). Each one applies the SoundscapeTheme so the captured
// screenshot matches in-app rendering.
// ---------------------------------------------------------------------------

/**
 * [SoundscapeTheme] for the screenshot wrappers below. Strings fetched outside composition, such
 * as the distance and direction under each marker, are resolved against Locale.getDefault(),
 * which the preview's locale leaves alone, so without this they would always be in English. In
 * the app the two locales always agree.
 */
@Composable
private fun PreviewTheme(content: @Composable () -> Unit) {
    Locale.setDefault(LocalConfiguration.current.locales[0])
    SoundscapeTheme(testTheme = testTheme, content = content)
}

class ThemeTestClass {

    @CustomPreviews
    @Composable
    @PreviewTest
    fun PreviewWelcomeTest() {
        PreviewTheme { PreviewWelcome() }
    }

    @TallCustomPreviews
    @Composable
    @PreviewTest
    fun TermsPreviewTest() {
        PreviewTheme { TermsPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun LanguagePreviewTest() {
        PreviewTheme { LanguagePreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun ListeningPreviewTest() {
        PreviewTheme { ListeningPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun HearingPreviewTest() {
        PreviewTheme { HearingPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun AudioBeaconsPreviewTest() {
        PreviewTheme { AudioBeaconPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun AccessibilityPreviewTest() {
        PreviewTheme { AccessibilityOnboardingScreenPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun FinishPreviewTest() {
        PreviewTheme { FinishPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun PermissionsPreviewTest() {
        PreviewTheme { PermissionsScreenPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun BatteryOptimizationPreviewTest() {
        PreviewTheme { BatteryOptimizationPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun HomePreviewTest() {
        PreviewTheme { HomePreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun HomeRoutePreviewTest() {
        PreviewTheme { HomeRoutePreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun HomeSearchPreviewTest() {
        PreviewTheme { HomeSearchPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun SleepScreenPreviewTest() {
        PreviewTheme { SleepScreenPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun AdvancedMarkersAndRoutesSettingsPreviewTest() {
        PreviewTheme { AdvancedMarkersAndRoutesSettingsPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun OpenSourceLicensesPreviewTest() {
        PreviewTheme { OpenSourceLicensesPreview() }
    }

    @TallCustomPreviews
    @Composable
    @PreviewTest
    fun HelpScreenMenuPreviewTest() {
        PreviewTheme { HelpScreenMenuPreview() }
    }

    @TallCustomPreviews
    @Composable
    @PreviewTest
    fun BeaconHelpPreviewTest() {
        PreviewTheme { BeaconHelpPreview() }
    }

    @TallCustomPreviews
    @Composable
    @PreviewTest
    fun VoicesHelpPreviewTest() {
        PreviewTheme { VoicesHelpPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun PreviewDrawerContentTest() {
        PreviewTheme { PreviewDrawerContent() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun AudioTourDialogTestTest() {
        PreviewTheme { AudioTourDialogTest() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun NewReleaseDialogPreviewTest() {
        PreviewTheme { NewReleaseDialogPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun SettingsPreviewTest() {
        PreviewTheme { SettingsPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun LocationDetailsPreviewTest() {
        PreviewTheme { LocationDetailsPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun SaveAndEditMarkerPreviewTest() {
        PreviewTheme { SaveAndEditMarkerPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun OfflineMapsScreenPreviewTest() {
        PreviewTheme { OfflineMapsScreenPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun OfflineMapsScreenDownloadingPreviewTest() {
        PreviewTheme { OfflineMapsScreenDownloadingPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun PlacesNearbyPreviewTest() {
        PreviewTheme { PlacesNearbyPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun MarkersScreenPreviewTest() {
        PreviewTheme { MarkersScreenPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun AddMarkerScreenPreviewTest() {
        PreviewTheme { AddMarkerScreenPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun MarkersScreenPopulatedPreviewTest() {
        PreviewTheme { MarkersScreenPopulatedPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun RoutesScreenPreviewTest() {
        PreviewTheme { RoutesScreenPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun RoutesScreenPopulatedPreviewTest() {
        PreviewTheme { RoutesScreenPopulatedPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun RoutesDetailsPopulatedPreviewTest() {
        PreviewTheme { RoutesDetailsPopulatedPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun HomeBeaconPreviewTest() {
        PreviewTheme { HomeBeaconPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun HomeRoutePlayingPreviewTest() {
        PreviewTheme { HomeRoutePlayingPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun HomeStreetPreviewPreviewTest() {
        PreviewTheme { HomeStreetPreviewPreview() }
    }

    @TallCustomPreviews
    @Composable
    @PreviewTest
    fun SettingsCalloutsPreviewTest() {
        PreviewTheme { SettingsSectionPreview("callouts") }
    }

    @TallCustomPreviews
    @Composable
    @PreviewTest
    fun SettingsAudioPreviewTest() {
        PreviewTheme { SettingsSectionPreview("audio") }
    }

    @TallCustomPreviews
    @Composable
    @PreviewTest
    fun SettingsSearchPreviewTest() {
        PreviewTheme { SettingsSectionPreview("search") }
    }

    @TallCustomPreviews
    @Composable
    @PreviewTest
    fun SettingsAccessibilityPreviewTest() {
        PreviewTheme { SettingsSectionPreview("accessibility") }
    }

    @TallCustomPreviews
    @Composable
    @PreviewTest
    fun SettingsLanguagePreviewTest() {
        PreviewTheme { SettingsSectionPreview("language") }
    }

    @TallCustomPreviews
    @Composable
    @PreviewTest
    fun SettingsStoragePreviewTest() {
        PreviewTheme { SettingsSectionPreview("storage") }
    }

    @TallCustomPreviews
    @Composable
    @PreviewTest
    fun SettingsDebugPreviewTest() {
        PreviewTheme { SettingsSectionPreview("debug") }
    }

    @TallCustomPreviews
    @Composable
    @PreviewTest
    fun FaqHelpPreviewTest() {
        PreviewTheme { SharedHelpScreen(topic = "pagefaq_title", onNavigate = {}, onNavigateUp = {}) }
    }

    @TallCustomPreviews
    @Composable
    @PreviewTest
    fun TipsHelpPreviewTest() {
        PreviewTheme { SharedHelpScreen(topic = "pagefaq_tips_title", onNavigate = {}, onNavigateUp = {}) }
    }

    @TallCustomPreviews
    @Composable
    @PreviewTest
    fun OfflineHelpPreviewTest() {
        PreviewTheme { SharedHelpScreen(topic = "pagehelp_offline_page_title", onNavigate = {}, onNavigateUp = {}) }
    }

    @TallCustomPreviews
    @Composable
    @PreviewTest
    fun GpsAccuracyHelpPreviewTest() {
        PreviewTheme { SharedHelpScreen(topic = "pagehelp_gps_accuracy_page_title", onNavigate = {}, onNavigateUp = {}) }
    }

    @TallCustomPreviews
    @Composable
    @PreviewTest
    fun FaqAnswerHelpPreviewTest() {
        PreviewTheme { SharedHelpScreen(topic = "faqfaq_what_is_osm_question.faq_what_is_osm_answer", onNavigate = {}, onNavigateUp = {}) }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun AddAndEditRoutePreviewTest() {
        PreviewTheme { AddAndEditRoutePreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun AddWaypointsDialogPreviewTest() {
        PreviewTheme { AddWaypointsDialogPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun OfflineMapExtractDetailsPreviewTest() {
        PreviewTheme { OfflineMapExtractDetailsPreview() }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun LegacyMigrationRunningPreviewTest() {
        PreviewTheme { LegacyMigrationPreview(LegacyMigrationUiState.Running(12, 40)) }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun LegacyMigrationFinishedPreviewTest() {
        PreviewTheme { LegacyMigrationPreview(LegacyMigrationUiState.Finished(40)) }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun LegacyMigrationNeedsMapDataPreviewTest() {
        PreviewTheme { LegacyMigrationPreview(LegacyMigrationUiState.NeedsMapData) }
    }

    @CustomPreviews
    @Composable
    @PreviewTest
    fun LegacyMigrationFailedPreviewTest() {
        PreviewTheme { LegacyMigrationPreview(LegacyMigrationUiState.Failed) }
    }
}
