package org.scottishtecharmy.soundscape

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.flow.MutableStateFlow
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.navigation.SharedRoutes
import org.scottishtecharmy.soundscape.preferences.PreferenceDefaults
import org.scottishtecharmy.soundscape.preferences.PreferenceKeys
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

fun MainViewController() = ComposeUIViewController {
    val service = remember { IosSoundscapeService.getInstance() }
    val mgr = service.offlineMapManager
    val prefs = service.preferencesProvider

    val isFirstLaunch = remember {
        prefs.getBoolean(PreferenceKeys.FIRST_LAUNCH, PreferenceDefaults.FIRST_LAUNCH)
    }
    val startDestination = if (isFirstLaunch) SharedRoutes.ONBOARDING else SharedRoutes.HOME

    // Stub flows for features iOS doesn't yet have its own state for. They're @Composable-safe
    // because they're MutableStateFlow.
    val audioTourRunning = remember { MutableStateFlow(false) }
    val recordingEnabled = remember { MutableStateFlow(false) }
    val permissionsRequired = remember { MutableStateFlow(false) }
    val voiceCommandListening = remember { MutableStateFlow(false) }

    App(
        flows = AppFlows(
            locationFlow = service.getLocationFlow(),
            directionFlow = service.getOrientationFlow(),
            homeState = service.homeState,
            markersUiState = service.markersUiState,
            routesUiState = service.routesUiState,
            placesNearbyUiState = service.placesNearbyUiState,
            offlineMapsNearbyExtracts = mgr.availableExtracts,
            offlineMapsDownloaded = mgr.downloadedExtracts,
            offlineMapsDownloadState = mgr.downloadState,
            beaconTypes = service.audioEngine.getListOfBeaconTypes().toList(),
            audioTourRunning = audioTourRunning,
            recordingEnabled = recordingEnabled,
            permissionsRequired = permissionsRequired,
            voiceCommandListening = voiceCommandListening,
        ),
        callbacks = AppCallbacks(
            onStartBeacon = { lat, lng, name ->
                service.startBeacon(LngLatAlt(lng, lat), name)
            },
            onStopBeacon = { service.destroyBeacon() },
            onSpeak = { text -> service.speakCallout(text) },
            onStartRoute = { routeId -> service.routeStartById(routeId) },
            onStartRouteInReverse = { routeId -> service.routePlayer.startRoute(routeId, reverse = true) },
            onRouteStop = { service.routeStop() },
            onRouteSkipNext = { service.routeSkipNext() },
            onRouteSkipPrevious = { service.routeSkipPrevious() },
            onRouteMute = { service.routeMute() },
            onSearch = { query -> service.search(query) },
            onSaveMarker = { desc -> service.saveMarker(desc) },
            onDeleteMarker = { markerId -> service.deleteMarker(markerId) },
            onSaveRoute = { name, desc, waypoints -> service.saveRoute(name, desc, waypoints) },
            onDeleteRoute = { routeId -> service.deleteRoute(routeId) },
            onLoadRoute = { routeId -> service.loadRouteWaypoints(routeId) },
            onMyLocation = { service.myLocation() },
            onWhatsAroundMe = { service.whatsAroundMe() },
            onAheadOfMe = { service.aheadOfMe() },
            onNearbyMarkers = { service.nearbyMarkers() },
            onPlacesNearbyClickFolder = { filter, title ->
                service.placesNearbyClickFolder(filter, title)
            },
            onPlacesNearbyClickBack = { service.placesNearbyClickBack() },
            onOfflineMapsRefresh = { mgr.refresh() },
            onOfflineMapsGetExtracts = { location -> mgr.getExtractsContaining(location) },
            onOfflineMapsDownload = { feature ->
                val props = feature.properties ?: return@AppCallbacks
                val filename = props["filename"] as? String ?: return@AppCallbacks
                val extractSize = (props["extract-size"] as? String)?.toDoubleOrNull()
                mgr.startDownload(filename, extractSize)
            },
            onOfflineMapsDelete = { path -> mgr.deleteExtract(path) },
            onOfflineMapsCancelDownload = { mgr.cancelDownload() },
            // iOS hooks for the previously Android-only home features. Stubs for now —
            // a future change can wire these to native iOS subsystems.
            onSleep = { /* TODO iOS sleep mode */ },
            onStreetPreviewGo = { /* TODO iOS street preview */ },
            onStreetPreviewExit = { /* TODO iOS street preview */ },
            onShareRecording = { /* TODO iOS UIActivityViewController */ },
            onRateApp = {
                val url = NSURL.URLWithString("itms-apps://itunes.apple.com/app/idXXXXXXXX?action=write-review")
                if (url != null) UIApplication.sharedApplication.openURL(url)
            },
            onContactSupport = {
                val url = NSURL.URLWithString("mailto:soundscape@scottishtecharmy.support")
                if (url != null) UIApplication.sharedApplication.openURL(url)
            },
            onToggleAudioTour = { /* TODO iOS audio tour */ },
            onMapLongClick = null,
            onGoToAppSettings = {
                val url = NSURL.URLWithString("app-settings:")
                if (url != null) UIApplication.sharedApplication.openURL(url)
            },
            onGetCurrentLocationDescription = {
                val location = service.homeState.value.location
                if (location != null) {
                    LocationDescription("", location)
                } else {
                    LocationDescription("", LngLatAlt())
                }
            },
            onSetApplicationLocale = { /* TODO iOS NSUserDefaults AppleLanguages */ },
            onGetLanguageMismatch = { null },
        ),
        startDestination = startDestination,
        audioEngine = service.audioEngine,
        preferencesProvider = prefs,
    )
}
