package org.scottishtecharmy.soundscape

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import me.zhanghai.compose.preference.listPreference
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.audio.TourButton
import org.scottishtecharmy.soundscape.audio.availableTtsVoicesForCurrentLanguage
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.intents.resolveRouteByName
import org.scottishtecharmy.soundscape.navigation.SharedRoutes
import org.scottishtecharmy.soundscape.platform.readResourceText
import org.scottishtecharmy.soundscape.preferences.PreferenceDefaults
import org.scottishtecharmy.soundscape.preferences.PreferenceKeys
import org.scottishtecharmy.soundscape.preferences.PreferencesListener
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.settings_theme_auto
import org.scottishtecharmy.soundscape.resources.voice_voices
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.home.AdvancedMarkersAndRoutesSettingsViewModel
import org.scottishtecharmy.soundscape.screens.home.placesnearby.PlacesNearbyViewModel
import org.scottishtecharmy.soundscape.screens.home.settings.ClickableOption
import org.scottishtecharmy.soundscape.screens.home.settings.ListPreferenceItem
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen.AddAndEditRouteViewModel
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.markersscreen.MarkersViewModel
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.routesscreen.RoutesViewModel
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow

fun MainViewController() = ComposeUIViewController {
    installUnhandledExceptionLogger()
    val service = remember { IosSoundscapeService.getInstance() }
    val mgr = service.offlineMapManager
    val prefs = service.preferencesProvider
    val audioTour = service.audioTour
    val homeViewModel = service.homeViewModel

    val textColor = MaterialTheme.colorScheme.onBackground
    val expandedSectionModifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)

    val isFirstLaunch = remember {
        prefs.getBoolean(PreferenceKeys.FIRST_LAUNCH, PreferenceDefaults.FIRST_LAUNCH)
    }
    val startDestination = if (isFirstLaunch) SharedRoutes.ONBOARDING else SharedRoutes.HOME

    // First-launch users grant location permission from the onboarding permissions screen.
    // For returning users, ask now so the location provider can resume — iOS no-ops if the
    // status is already determined.
    if (!isFirstLaunch) {
        remember { service.iosLocationProvider.requestPermission() }
    }

    val audioTourRunning = remember { MutableStateFlow(false) }
    androidx.compose.runtime.LaunchedEffect(audioTour) {
        audioTour.currentInstruction.collect {
            audioTourRunning.value = audioTour.isRunning()
        }
    }

    val recordingEnabled = remember {
        MutableStateFlow(
            prefs.getBoolean(
                PreferenceKeys.RECORD_TRAVEL,
                PreferenceDefaults.RECORD_TRAVEL
            )
        )
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

    // TTS voice picker contents for the Audio section of the iOS settings
    // screen. Voices are enumerated once per launch — adding/removing voices
    // requires re-launching the app, which matches the legacy iOS behaviour.
    val ttsVoices = remember { availableTtsVoicesForCurrentLanguage() }
    val systemDefaultLabel = stringResource(Res.string.settings_theme_auto)
    val ttsVoiceValues = remember(ttsVoices) {
        listOf("") + ttsVoices.map { it.identifier }
    }
    val ttsVoiceDescriptions = remember(ttsVoices, systemDefaultLabel) {
        listOf(systemDefaultLabel) + ttsVoices.map { it.displayName }
    }
    val voiceSettingTitle = stringResource(Res.string.voice_voices)
    val settingsPlatformAudioContent: androidx.compose.foundation.lazy.LazyListScope.() -> Unit = {
        listPreference(
            key = PreferenceKeys.SELECTED_TTS_VOICE_ID,
            defaultValue = PreferenceDefaults.SELECTED_TTS_VOICE_ID,
            values = ttsVoiceValues,
            modifier = expandedSectionModifier,
            title = { Text(text = voiceSettingTitle, color = textColor) },
            item = { value, currentValue, onClick ->
                val idx = ttsVoiceValues.indexOf(value).coerceAtLeast(0)
                ListPreferenceItem(
                    description = ttsVoiceDescriptions[idx],
                    value = value,
                    currentValue = currentValue,
                    onClick = onClick,
                    index = idx,
                    listSize = ttsVoiceValues.size,
                )
            },
            summary = {
                val idx = ttsVoiceValues.indexOf(it).coerceAtLeast(0)
                ClickableOption(
                    text = ttsVoiceDescriptions[idx],
                    textColor = textColor,
                )
            },
        )
    }

    App(
        flows = AppFlows(
            locationFlow = service.locationFlow,
            directionFlow = service.orientationFlow,
            homeState = homeViewModel.state,
            offlineMapsNearbyExtractsState = mgr.nearbyExtractsState,
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
            onSearch = { query -> homeViewModel.onTriggerSearch(query) },
            onSaveMarker = { desc ->
                service.saveMarker(desc)
                audioTour.onMarkerCreateDone()
            },
            onDeleteMarker = { markerId -> service.deleteMarker(markerId) },
            onSaveRoute = { name, desc, waypoints -> service.saveRoute(name, desc, waypoints) },
            onDeleteRoute = { routeId -> service.deleteRoute(routeId) },
            onLoadRoute = { routeId -> service.loadRouteWaypoints(routeId) },
            createAddAndEditRouteViewModel = {
                AddAndEditRouteViewModel(service.routeDao, service)
            },
            createMarkersViewModel = {
                MarkersViewModel(service.routeDao, service.preferencesProvider, service)
            },
            createRoutesViewModel = {
                RoutesViewModel(service.routeDao, service.preferencesProvider, service)
            },
            createPlacesNearbyViewModel = {
                PlacesNearbyViewModel(service, audioTour)
            },
            createAdvancedMarkersAndRoutesSettingsViewModel = {
                AdvancedMarkersAndRoutesSettingsViewModel(
                    service.routeDao, service.markersAndRoutesIo,
                )
            },
            onMyLocation = {
                homeViewModel.myLocation()
                audioTour.onButtonPressed(TourButton.MY_LOCATION)
            },
            onWhatsAroundMe = {
                homeViewModel.whatsAroundMe()
                audioTour.onButtonPressed(TourButton.AROUND_ME)
            },
            onAheadOfMe = {
                homeViewModel.aheadOfMe()
                audioTour.onButtonPressed(TourButton.AHEAD_OF_ME)
            },
            onNearbyMarkers = {
                homeViewModel.nearbyMarkers()
                audioTour.onButtonPressed(TourButton.NEARBY_MARKERS)
            },
            // No-op: PlacesNearbyViewModel is now nav-scoped and handles these
            // events itself via createPlacesNearbyViewModel.
            onPlacesNearbyClickFolder = { _, _ -> },
            onPlacesNearbyClickBack = {},
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
            onStreetPreviewGo = { homeViewModel.streetPreviewGo() },
            onStreetPreviewExit = { homeViewModel.streetPreviewExit() },
            onEnableStreetPreview = { loc -> homeViewModel.enableStreetPreview(loc) },
            onShareRecording = {
                val fileUrl = service.writeRecordingFile()
                if (fileUrl != null) presentShareSheet(fileUrl)
            },
            onShareRoute = { routeId ->
                val fileUrl = service.writeRouteFile(routeId)
                if (fileUrl != null) presentShareSheet(fileUrl)
            },
            onShareLocation = { desc, message ->
                presentShareText(
                    org.scottishtecharmy.soundscape.utils.buildShareLocationText(
                        desc = desc,
                        messageTemplate = message,
                        mapsName = "Apple Maps",
                        mapsUrlBuilder = { lat, lon, encodedName ->
                            "https://maps.apple.com/?q=$encodedName&ll=$lat,$lon"
                        },
                    ),
                )
            },
            onRateApp = {
                val url =
                    NSURL.URLWithString("itms-apps://itunes.apple.com/app/idXXXXXXXX?action=write-review")
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
                val location = homeViewModel.state.value.location
                if (location != null) {
                    service.getLocationDescription(location)
                } else {
                    LocationDescription("", LngLatAlt())
                }
            },
            onSetApplicationLocale = { tag ->
                // iOS reads the per-app language override from
                // NSUserDefaults["AppleLanguages"] at launch. Writing it now
                // takes effect on the next launch — terminate the process so
                // the user re-launches into the chosen language.
                if (tag != null) {
                    platform.Foundation.NSUserDefaults.standardUserDefaults.setObject(
                        listOf(tag),
                        forKey = "AppleLanguages",
                    )
                } else {
                    platform.Foundation.NSUserDefaults.standardUserDefaults.removeObjectForKey(
                        "AppleLanguages",
                    )
                }
                platform.posix.exit(0)
            },
            onGetLanguageMismatch = {
                org.scottishtecharmy.soundscape.screens.onboarding.language.getLanguageMismatch()
            },
            getOpenSourceLicensesJson = { readResourceText("open_source_licenses.json") },
            // No onResetSettings hook — SharedNavGraph clears the
            // PreferencesProvider and navigates to the onboarding flow when
            // the platform leaves this null.
            onBeaconPreviewStart = { type -> service.startBeaconPreview(type) },
            onBeaconPreviewUpdate = { type -> service.updateBeaconPreviewType(type) },
            onBeaconPreviewStop = { commit, chosen ->
                service.stopBeaconPreview(commit, chosen)
            },
        ),
        startDestination = startDestination,
        audioEngine = service.audioEngine,
        audioTour = audioTour,
        preferencesProvider = prefs,
        settingsPlatformAudioContent = settingsPlatformAudioContent,
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

