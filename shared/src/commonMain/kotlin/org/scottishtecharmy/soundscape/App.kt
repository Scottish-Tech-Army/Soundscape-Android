package org.scottishtecharmy.soundscape

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
import org.scottishtecharmy.soundscape.locationprovider.DeviceDirection
import org.scottishtecharmy.soundscape.locationprovider.SoundscapeLocation
import org.scottishtecharmy.soundscape.navigation.NavigationStateHolder
import org.scottishtecharmy.soundscape.navigation.SharedNavHost
import org.scottishtecharmy.soundscape.navigation.SharedRoutes
import org.scottishtecharmy.soundscape.network.DownloadStateCommon
import org.scottishtecharmy.soundscape.preferences.PreferencesProvider
import org.scottishtecharmy.soundscape.screens.home.HomeState
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.placesnearby.PlacesNearbyUiState
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.MarkersAndRoutesUiState
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen.AddAndEditRouteStateHolder
import org.scottishtecharmy.soundscape.screens.onboarding.language.Language
import org.scottishtecharmy.soundscape.ui.theme.LocalAppButtonColors
import org.scottishtecharmy.soundscape.ui.theme.defaultAppButtonColors

data class AppCallbacks(
    val onStartBeacon: (Double, Double, String) -> Unit = { _, _, _ -> },
    val onStopBeacon: () -> Unit = {},
    val onSpeak: (String) -> Unit = {},
    val onStartRoute: (Long) -> Unit = {},
    val onStartRouteInReverse: (Long) -> Unit = {},
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
    val createAddAndEditRouteStateHolder: (() -> AddAndEditRouteStateHolder)? = null,
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
    val onShareRecording: () -> Unit = {},
    val onShareRoute: (routeId: Long) -> Unit = {},
    val onShareLocation: (LocationDescription, message: String) -> Unit = { _, _ -> },
    val onRateApp: () -> Unit = {},
    val onContactSupport: () -> Unit = {},
    val onToggleAudioTour: () -> Unit = {},
    val onAudioTourInstructionAcknowledged: () -> Unit = {},
    val onMapLongClick: ((LngLatAlt) -> Boolean)? = null,
    val onGoToAppSettings: () -> Unit = {},
    val onGetCurrentLocationDescription: () -> LocationDescription = { LocationDescription("", LngLatAlt()) },
    val onSetApplicationLocale: (String?) -> Unit = {},
    val onGetLanguageMismatch: () -> Language? = { null },
    val getOpenSourceLicensesJson: (() -> String)? = null,
)

data class AppFlows(
    val locationFlow: StateFlow<SoundscapeLocation?>? = null,
    val directionFlow: StateFlow<DeviceDirection?>? = null,
    val homeState: StateFlow<HomeState>? = null,
    val markersUiState: StateFlow<MarkersAndRoutesUiState>? = null,
    val routesUiState: StateFlow<MarkersAndRoutesUiState>? = null,
    val placesNearbyUiState: StateFlow<PlacesNearbyUiState>? = null,
    val offlineMapsNearbyExtracts: StateFlow<List<Feature>>? = null,
    val offlineMapsDownloaded: StateFlow<List<String>>? = null,
    val offlineMapsDownloadedFc: StateFlow<org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection>? = null,
    val offlineMapsDownloadState: StateFlow<DownloadStateCommon>? = null,
    val beaconTypes: List<String> = emptyList(),
    // Home-screen extras
    val audioTourRunning: StateFlow<Boolean>? = null,
    val audioTourInstruction: StateFlow<AudioTourInstruction?>? = null,
    val recordingEnabled: StateFlow<Boolean>? = null,
    val permissionsRequired: StateFlow<Boolean>? = null,
    val voiceCommandListening: StateFlow<Boolean>? = null,
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
                platformNavBuilder = platformNavBuilder,
            )
        }
    }
}
