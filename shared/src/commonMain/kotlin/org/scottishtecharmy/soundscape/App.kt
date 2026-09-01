package org.scottishtecharmy.soundscape

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.StateFlow
import org.scottishtecharmy.soundscape.audio.AudioEngine
import org.scottishtecharmy.soundscape.audio.AudioTour
import org.scottishtecharmy.soundscape.audio.AudioTourInstruction
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.intents.IncomingIntent
import org.scottishtecharmy.soundscape.locationprovider.DeviceDirection
import org.scottishtecharmy.soundscape.locationprovider.LocationProvider
import org.scottishtecharmy.soundscape.locationprovider.SoundscapeLocation
import org.scottishtecharmy.soundscape.navigation.NavigationStateHolder
import org.scottishtecharmy.soundscape.navigation.SharedNavHost
import org.scottishtecharmy.soundscape.navigation.SharedRoutes
import org.scottishtecharmy.soundscape.network.DownloadStateCommon
import org.scottishtecharmy.soundscape.preferences.PreferencesProvider
import org.scottishtecharmy.soundscape.screens.home.HomeState
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.home.AdvancedMarkersAndRoutesSettingsViewModel
import org.scottishtecharmy.soundscape.screens.home.offlinemaps.NearbyExtractsState
import org.scottishtecharmy.soundscape.screens.home.placesnearby.PlacesNearbyUiState
import org.scottishtecharmy.soundscape.screens.home.placesnearby.PlacesNearbyViewModel
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.MarkersAndRoutesUiState
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen.AddAndEditRouteViewModel
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.markersscreen.MarkersViewModel
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.routesscreen.RoutesViewModel
import org.scottishtecharmy.soundscape.screens.onboarding.language.Language
import org.scottishtecharmy.soundscape.ui.theme.LocalAppButtonColors
import org.scottishtecharmy.soundscape.ui.theme.defaultAppButtonColors

data class AppCallbacks(
    val onStartBeacon: (Double, Double, String) -> Unit = { _, _, _ -> },
    val onStopBeacon: () -> Unit = {},
    val onSpeak: (String) -> Unit = {},
    val onStartRoute: (Long) -> Unit = {},
    val onStartRouteInReverse: (Long) -> Unit = {},
    val onStartRouteByName: (String) -> Unit = {},
    val onMyLocation: () -> Unit = {},
    val onWhatsAroundMe: () -> Unit = {},
    val onAheadOfMe: () -> Unit = {},
    val onNearbyMarkers: () -> Unit = {},
    val onRouteSkipNext: () -> Unit = {},
    val onRouteSkipPrevious: () -> Unit = {},
    val onRouteMute: () -> Unit = {},
    val onRouteStop: () -> Unit = {},
    val onSearch: (String) -> Unit = {},
    val onSaveMarker: (LocationDescription) -> Unit = {},
    val onDeleteMarker: (Long) -> Unit = {},
    val onSaveRoute: (String, String, List<LocationDescription>) -> Unit = { _, _, _ -> },
    val onDeleteRoute: (Long) -> Unit = {},
    val onLoadRoute: (Long) -> List<LocationDescription>? = { null },
    val createAddAndEditRouteViewModel: (() -> AddAndEditRouteViewModel)? = null,
    /**
     * Factories for nav-scoped ViewModels. Used by the iOS-only shared routes
     * that need a fresh per-navigation-entry instance. Android does not reach
     * these shared routes (it overrides them in platformNavBuilder), so
     * factories are typically only set on iOS.
     */
    val createMarkersViewModel: (() -> MarkersViewModel)? = null,
    val createRoutesViewModel: (() -> RoutesViewModel)? = null,
    val createPlacesNearbyViewModel: (() -> PlacesNearbyViewModel)? = null,
    val createAdvancedMarkersAndRoutesSettingsViewModel: (() -> AdvancedMarkersAndRoutesSettingsViewModel)? = null,
    val onPlacesNearbyClickFolder: (String, String) -> Unit = { _, _ -> },
    val onPlacesNearbyClickBack: () -> Unit = {},
    val onOfflineMapsRefresh: () -> Unit = {},
    val onOfflineMapsGetExtracts: (LngLatAlt) -> List<Feature> = { emptyList() },
    val onOfflineMapsDownload: (String, Feature) -> Unit = { _, _ -> },
    val onOfflineMapsDelete: (Feature) -> Unit = {},
    val onOfflineMapsCancelDownload: () -> Unit = {},
    // Home-screen extras
    val onSleep: () -> Unit = {},
    val onWakeUp: () -> Unit = {},
    val onStreetPreviewGo: () -> Unit = {},
    val onStreetPreviewExit: () -> Unit = {},
    val onEnableStreetPreview: (LngLatAlt) -> Unit = {},
    val onShareRecording: () -> Unit = {},
    val onShareRoute: (routeId: Long) -> Unit = {},
    val onShareLocation: (LocationDescription, message: String) -> Unit = { _, _ -> },
    val onRateApp: () -> Unit = {},
    val onContactSupport: () -> Unit = {},
    val onToggleAudioTour: () -> Unit = {},
    val onAudioTourInstructionAcknowledged: () -> Unit = {},
    val onMapLongClick: ((LngLatAlt) -> Boolean)? = null,
    val onGoToAppSettings: () -> Unit = {},
    val onGetCurrentLocationDescription: () -> LocationDescription = {
        LocationDescription(
            "",
            LngLatAlt()
        )
    },
    /**
     * Offline-geocodes a point to a full address (house number included where the tile data
     * supports it). Used by the location details screen to fill in an address for a POI which
     * has none of its own. Null when the platform hasn't wired it up.
     */
    val onGetOfflineAddress: (suspend (LngLatAlt) -> LocationDescription?)? = null,
    val onGetLanguageMismatch: () -> Language? = { null },
    val provideLocationProvider: (() -> LocationProvider)? = null,
    val getOpenSourceLicensesJson: (() -> String)? = null,
    /**
     * Wipes preferences and immediately restarts the app. When non-null,
     * SharedSettingsScreen renders a "Reset settings to defaults" button in
     * the Debug section.
     */
    val onResetSettings: (() -> Unit)? = null,
    /**
     * Beacon style audio preview, plumbed through to [SharedSettingsScreen]
     * by [SharedNavGraph] so the iOS path picks them up. Android uses its
     * own settingsContent slot and wires [SettingsViewModel] directly, so
     * these are typically only set on iOS. When non-null, the beacon style
     * row in Settings opens a dialog that previews each style as the user
     * moves through the list. See [SharedSettingsScreen] for the full
     * contract.
     */
    val onBeaconPreviewStart: ((String) -> Unit)? = null,
    val onBeaconPreviewUpdate: ((String) -> Unit)? = null,
    val onBeaconPreviewStop: ((Boolean, String?) -> Unit)? = null,
    /**
     * Fully exits the app (stops the foreground service and finishes the
     * activity). Left null on iOS, where Apple's HIG forbids apps quitting
     * themselves; the drawer hides the "Exit Soundscape" item when null.
     */
    val onExitApp: (() -> Unit)? = null,
)

data class AppFlows(
    val locationFlow: StateFlow<SoundscapeLocation?>? = null,
    val directionFlow: StateFlow<DeviceDirection?>? = null,
    val homeState: StateFlow<HomeState>? = null,
    val markersUiState: StateFlow<MarkersAndRoutesUiState>? = null,
    val routesUiState: StateFlow<MarkersAndRoutesUiState>? = null,
    val placesNearbyUiState: StateFlow<PlacesNearbyUiState>? = null,
    val offlineMapsNearbyExtractsState: StateFlow<NearbyExtractsState>? = null,
    val offlineMapsDownloaded: StateFlow<List<String>>? = null,
    val offlineMapsDownloadedFc: StateFlow<org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection>? = null,
    val offlineMapsDownloadState: StateFlow<DownloadStateCommon>? = null,
    val beaconTypes: List<String> = emptyList(),
    // Home-screen extras
    val audioTourRunning: StateFlow<Boolean>? = null,
    val audioTourInstruction: StateFlow<AudioTourInstruction?>? = null,
    val recordingEnabled: StateFlow<Boolean>? = null,
    val permissionsRequired: StateFlow<Boolean>? = null,
    /**
     * Single-shot inbound intent (URL, file import, deep link) emitted by the
     * platform launch handler. SharedNavHost dispatches to navigation/callbacks
     * and then invokes [onPendingIntentHandled] so the publisher can clear it.
     */
    val pendingIntent: StateFlow<IncomingIntent?>? = null,
    val onPendingIntentHandled: (() -> Unit)? = null,
)

@Composable
fun App(
    flows: AppFlows = AppFlows(),
    callbacks: AppCallbacks = AppCallbacks(),
    startDestination: String? = null,
    audioEngine: AudioEngine? = null,
    audioTour: AudioTour? = null,
    preferencesProvider: PreferencesProvider? = null,
    homeContent: (@Composable (NavHostController, NavigationStateHolder) -> Unit)? = null,
    settingsContent: (@Composable (NavHostController) -> Unit)? = null,
    /**
     * Platform-specific items inserted into the Audio section of the default
     * settings screen. Only consumed when [settingsContent] is null — Android
     * supplies a full custom settings screen, iOS uses this slot to add the
     * voice picker.
     */
    settingsPlatformAudioContent: (LazyListScope.() -> Unit)? = null,
    platformNavBuilder: (NavGraphBuilder.() -> Unit)? = null,
) {
    MaterialTheme {
        val buttonColors = defaultAppButtonColors(MaterialTheme.colorScheme)
        CompositionLocalProvider(LocalAppButtonColors provides buttonColors) {
            val navController = rememberNavController()
            val navStateHolder = remember { NavigationStateHolder() }

            SharedNavHost(
                navController = navController,
                navStateHolder = navStateHolder,
                flows = flows,
                callbacks = callbacks,
                startDestination = startDestination ?: SharedRoutes.WELCOME,
                audioEngine = audioEngine,
                audioTour = audioTour,
                preferencesProvider = preferencesProvider,
                homeContent = homeContent,
                settingsContent = settingsContent,
                settingsPlatformAudioContent = settingsPlatformAudioContent,
                platformNavBuilder = platformNavBuilder,
            )
        }
    }
}
