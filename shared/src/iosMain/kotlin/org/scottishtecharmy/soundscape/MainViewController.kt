package org.scottishtecharmy.soundscape

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.flow.MutableStateFlow
import org.scottishtecharmy.soundscape.audio.TourButton
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.intents.IncomingIntent
import org.scottishtecharmy.soundscape.intents.resolveRouteByName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.navigation.SharedRoutes
import org.scottishtecharmy.soundscape.platform.readResourceText
import org.scottishtecharmy.soundscape.preferences.PreferenceDefaults
import org.scottishtecharmy.soundscape.preferences.PreferenceKeys
import org.scottishtecharmy.soundscape.preferences.PreferencesListener
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen.AddAndEditRouteStateHolder
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow

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

    // First-launch users grant location permission from the onboarding permissions screen.
    // For returning users, ask now so the location provider can resume — iOS no-ops if the
    // status is already determined.
    if (!isFirstLaunch) {
        remember { service.locationProvider.requestPermission() }
    }

    val audioTourRunning = remember { MutableStateFlow(false) }
    androidx.compose.runtime.LaunchedEffect(audioTour) {
        audioTour.currentInstruction.collect {
            audioTourRunning.value = audioTour.isRunning()
        }
    }

    val recordingEnabled = remember {
        MutableStateFlow(prefs.getBoolean(PreferenceKeys.RECORD_TRAVEL, PreferenceDefaults.RECORD_TRAVEL))
    }
    DisposableEffect(prefs) {
        val listener = PreferencesListener { key ->
            if (key == PreferenceKeys.RECORD_TRAVEL) {
                recordingEnabled.value = prefs.getBoolean(
                    PreferenceKeys.RECORD_TRAVEL,
                    PreferenceDefaults.RECORD_TRAVEL,
                )
            }
        }
        prefs.addListener(listener)
        onDispose { prefs.removeListener(listener) }
    }

    // Stub flows for features iOS doesn't yet have its own state for.
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
            pendingIntent = service.pendingIntent,
            onPendingIntentHandled = { service.pendingIntentHandled() },
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
            onStartRouteByName = { name ->
                CoroutineScope(Dispatchers.Default).launch {
                    val id = resolveRouteByName(service.routeDao, name)
                    if (id != null) service.routeStartById(id)
                }
            },
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
            onSleep = { service.setSleeping(true) },
            onWakeUp = { service.setSleeping(false) },
            onStreetPreviewGo = { homeStateHolder.streetPreviewGo() },
            onStreetPreviewExit = { homeStateHolder.streetPreviewExit() },
            onShareRecording = {
                val fileUrl = service.writeRecordingFile()
                if (fileUrl != null) presentShareSheet(fileUrl)
            },
            onShareRoute = { routeId ->
                val fileUrl = service.writeRouteFile(routeId)
                if (fileUrl != null) presentShareSheet(fileUrl)
            },
            onShareLocation = { desc, message ->
                presentShareText(buildShareLocationText(desc, message))
            },
            onRateApp = {
                val url = NSURL.URLWithString("itms-apps://itunes.apple.com/app/idXXXXXXXX?action=write-review")
                if (url != null) openExternalUrl(url)
            },
            onContactSupport = { presentContactSupport(service) },
            onToggleAudioTour = { audioTour.toggleState() },
            onAudioTourInstructionAcknowledged = { audioTour.onInstructionAcknowledged() },
            onMapLongClick = null,
            onGoToAppSettings = {
                val url = NSURL.URLWithString("app-settings:")
                if (url != null) openExternalUrl(url)
            },
            onGetCurrentLocationDescription = {
                val location = homeStateHolder.state.value.location
                if (location != null) {
                    service.getLocationDescription(location)
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
        audioTour = audioTour,
        preferencesProvider = prefs,
    )
}

private fun presentShareSheet(fileUrl: NSURL) {
    presentActivityViewController(listOf(fileUrl))
}

private fun presentShareText(text: String) {
    presentActivityViewController(listOf(text))
}

private fun presentActivityViewController(items: List<Any>) {
    presentTopViewController(
        UIActivityViewController(
            activityItems = items,
            applicationActivities = null,
        ),
    )
}

internal fun presentTopViewController(viewController: UIViewController) {
    val keyWindow = UIApplication.sharedApplication.windows
        .mapNotNull { it as? UIWindow }
        .firstOrNull { it.isKeyWindow() }
        ?: UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow
        ?: return
    var top: UIViewController? = keyWindow.rootViewController
    while (top?.presentedViewController != null) top = top.presentedViewController
    top?.presentViewController(viewController, animated = true, completion = null)
}

internal fun openExternalUrl(url: NSURL) {
    UIApplication.sharedApplication.openURL(
        url,
        options = emptyMap<Any?, Any?>(),
        completionHandler = null,
    )
}

private fun buildShareLocationText(
    desc: LocationDescription,
    messageTemplate: String,
): String {
    val latitude = formatCoordinate(desc.location.latitude)
    val longitude = formatCoordinate(desc.location.longitude)
    val encodedName = urlEncode(desc.name)
    val soundscapeUrl =
        "https://links.soundscape.scottishtecharmy.org/v1/sharemarker?" +
            "lat=$latitude&lon=$longitude&name=$encodedName"
    val appleMapsUrl =
        "https://maps.apple.com/?q=$encodedName&ll=$latitude,$longitude"
    return messageTemplate
        .replace("Google Maps", "Apple Maps")
        .replace("%1\$s", desc.name)
        .replace("%2\$s", soundscapeUrl)
        .replace("%3\$s", appleMapsUrl)
}

private fun formatCoordinate(value: Double): String {
    val scaled = kotlin.math.round(value * 100000.0) / 100000.0
    val asString = scaled.toString()
    val dot = asString.indexOf('.')
    return when {
        dot < 0 -> "$asString.00000"
        asString.length - dot - 1 >= 5 -> asString.substring(0, dot + 6)
        else -> asString + "0".repeat(5 - (asString.length - dot - 1))
    }
}

private fun urlEncode(value: String): String {
    val bytes = value.encodeToByteArray()
    val builder = StringBuilder(bytes.size)
    for (b in bytes) {
        val c = b.toInt() and 0xFF
        val isUnreserved = (c in 0x30..0x39) || // 0-9
            (c in 0x41..0x5A) || // A-Z
            (c in 0x61..0x7A) || // a-z
            c == '-'.code || c == '_'.code || c == '.'.code || c == '~'.code
        if (isUnreserved) {
            builder.append(c.toChar())
        } else {
            builder.append('%')
            builder.append(c.toString(16).uppercase().padStart(2, '0'))
        }
    }
    return builder.toString()
}
