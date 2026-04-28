package org.scottishtecharmy.soundscape

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.flow.MutableStateFlow
import org.scottishtecharmy.soundscape.audio.TourButton
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.navigation.SharedRoutes
import org.scottishtecharmy.soundscape.platform.readResourceText
import org.scottishtecharmy.soundscape.preferences.PreferenceDefaults
import org.scottishtecharmy.soundscape.preferences.PreferenceKeys
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen.AddAndEditRouteStateHolder
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

fun MainViewController() = ComposeUIViewController {
    val service = remember { IosSoundscapeService.getInstance() }
    val mgr = service.offlineMapManager
    val prefs = service.preferencesProvider
    val audioTour = service.audioTour
    val homeStateHolder = service.homeStateHolder
    val placesNearbyStateHolder = service.placesNearbyStateHolder

    val isFirstLaunch = remember {
        prefs.getBoolean(PreferenceKeys.FIRST_LAUNCH, PreferenceDefaults.FIRST_LAUNCH)
    }
    val startDestination = if (isFirstLaunch) SharedRoutes.ONBOARDING else SharedRoutes.HOME

    val audioTourRunning = remember { MutableStateFlow(false) }
    androidx.compose.runtime.LaunchedEffect(audioTour) {
        audioTour.currentInstruction.collect {
            audioTourRunning.value = audioTour.isRunning()
        }
    }

    // Stub flows for features iOS doesn't yet have its own state for.
    val recordingEnabled = remember { MutableStateFlow(false) }
    val permissionsRequired = remember { MutableStateFlow(false) }
    val voiceCommandListening = remember { MutableStateFlow(false) }

    App(
        flows = AppFlows(
            locationFlow = service.locationFlow,
            directionFlow = service.orientationFlow,
            homeState = homeStateHolder.state,
            markersUiState = service.markersStateHolder.uiState,
            routesUiState = service.routesStateHolder.uiState,
            placesNearbyUiState = placesNearbyStateHolder.uiState,
            offlineMapsNearbyExtracts = mgr.availableExtracts,
            offlineMapsDownloaded = mgr.downloadedExtracts,
            offlineMapsDownloadedFc = mgr.downloadedExtractsFc,
            offlineMapsDownloadState = mgr.downloadState,
            beaconTypes = service.audioEngine.getListOfBeaconTypes().toList(),
            audioTourRunning = audioTourRunning,
            audioTourInstruction = audioTour.currentInstruction,
            recordingEnabled = recordingEnabled,
            permissionsRequired = permissionsRequired,
            voiceCommandListening = voiceCommandListening,
        ),
        callbacks = AppCallbacks(
            onStartBeacon = { lat, lng, name ->
                service.startBeacon(LngLatAlt(lng, lat), name)
                audioTour.onBeaconStarted()
            },
            onStopBeacon = {
                service.destroyBeacon()
                audioTour.onBeaconStopped()
            },
            onSpeak = { text -> service.speakCallout(text) },
            onStartRoute = { routeId -> service.routeStartById(routeId) },
            onStartRouteInReverse = { routeId -> service.routeStartReverse(routeId) },
            onRouteStop = {
                service.routeStop()
                audioTour.onBeaconStopped()
            },
            onRouteSkipNext = { service.routeSkipNext() },
            onRouteSkipPrevious = { service.routeSkipPrevious() },
            onRouteMute = { service.routeMute() },
            onSearch = { query -> homeStateHolder.onTriggerSearch(query) },
            onSaveMarker = { desc ->
                service.saveMarker(desc)
                audioTour.onMarkerCreateDone()
            },
            onDeleteMarker = { markerId -> service.deleteMarker(markerId) },
            onSaveRoute = { name, desc, waypoints -> service.saveRoute(name, desc, waypoints) },
            onDeleteRoute = { routeId -> service.deleteRoute(routeId) },
            onLoadRoute = { routeId -> service.loadRouteWaypoints(routeId) },
            createAddAndEditRouteStateHolder = {
                AddAndEditRouteStateHolder(service.routeDao, service)
            },
            onMyLocation = {
                homeStateHolder.myLocation()
                audioTour.onButtonPressed(TourButton.MY_LOCATION)
            },
            onWhatsAroundMe = {
                homeStateHolder.whatsAroundMe()
                audioTour.onButtonPressed(TourButton.AROUND_ME)
            },
            onAheadOfMe = {
                homeStateHolder.aheadOfMe()
                audioTour.onButtonPressed(TourButton.AHEAD_OF_ME)
            },
            onNearbyMarkers = {
                homeStateHolder.nearbyMarkers()
                audioTour.onButtonPressed(TourButton.NEARBY_MARKERS)
            },
            onPlacesNearbyClickFolder = { filter, title ->
                placesNearbyStateHolder.onClickFolder(filter, title)
            },
            onPlacesNearbyClickBack = { placesNearbyStateHolder.onClickBack() },
            onOfflineMapsRefresh = { mgr.refresh() },
            onOfflineMapsGetExtracts = { location -> mgr.getExtractsContaining(location) },
            onOfflineMapsDownload = { _, feature ->
                val props = feature.properties ?: return@AppCallbacks
                val filename = props["filename"] as? String ?: return@AppCallbacks
                val extractSize = (props["extract-size"] as? Number)?.toDouble()
                    ?: (props["extract-size"] as? String)?.toDoubleOrNull()
                mgr.startDownload(filename, extractSize)
            },
            onOfflineMapsDelete = { feature -> mgr.deleteExtractByFeature(feature) },
            onOfflineMapsCancelDownload = { mgr.cancelDownload() },
            // iOS hooks for the previously Android-only home features. Stubs for now —
            // a future change can wire these to native iOS subsystems.
            onSleep = { /* TODO iOS sleep mode */ },
            onStreetPreviewGo = { homeStateHolder.streetPreviewGo() },
            onStreetPreviewExit = { homeStateHolder.streetPreviewExit() },
            onShareRecording = { /* TODO iOS UIActivityViewController */ },
            onRateApp = {
                val url = NSURL.URLWithString("itms-apps://itunes.apple.com/app/idXXXXXXXX?action=write-review")
                if (url != null) UIApplication.sharedApplication.openURL(url)
            },
            onContactSupport = {
                val url = NSURL.URLWithString("mailto:soundscape@scottishtecharmy.support")
                if (url != null) UIApplication.sharedApplication.openURL(url)
            },
            onToggleAudioTour = { audioTour.toggleState() },
            onAudioTourInstructionAcknowledged = { audioTour.onInstructionAcknowledged() },
            onMapLongClick = null,
            onGoToAppSettings = {
                val url = NSURL.URLWithString("app-settings:")
                if (url != null) UIApplication.sharedApplication.openURL(url)
            },
            onGetCurrentLocationDescription = {
                val location = homeStateHolder.state.value.location
                if (location != null) {
                    LocationDescription("", location)
                } else {
                    LocationDescription("", LngLatAlt())
                }
            },
            onSetApplicationLocale = { /* TODO iOS NSUserDefaults AppleLanguages */ },
            onGetLanguageMismatch = { null },
            getOpenSourceLicensesJson = { readResourceText("open_source_licenses.json") },
        ),
        startDestination = startDestination,
        audioEngine = service.audioEngine,
        preferencesProvider = prefs,
    )
}
