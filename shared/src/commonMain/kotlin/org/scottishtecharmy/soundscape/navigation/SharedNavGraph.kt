package org.scottishtecharmy.soundscape.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.savedstate.read
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.AppCallbacks
import org.scottishtecharmy.soundscape.AppFlows
import org.scottishtecharmy.soundscape.audio.AudioEngine
import org.scottishtecharmy.soundscape.audio.AudioTour
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.intents.IncomingIntent
import org.scottishtecharmy.soundscape.network.DownloadStateCommon
import org.scottishtecharmy.soundscape.preferences.PreferenceKeys
import org.scottishtecharmy.soundscape.preferences.PreferencesProvider
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.search_view_markers
import org.scottishtecharmy.soundscape.resources.universal_links_marker_share_message
import org.scottishtecharmy.soundscape.screens.home.HomeState
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.home.AudioTourInstructionDialog
import org.scottishtecharmy.soundscape.screens.home.home.OpenSourceLicensesViewModel
import org.scottishtecharmy.soundscape.screens.home.home.SharedAdvancedMarkersAndRoutesSettingsScreen
import org.scottishtecharmy.soundscape.screens.home.home.SharedHelpScreen
import org.scottishtecharmy.soundscape.screens.home.home.SharedHomeScreen
import org.scottishtecharmy.soundscape.screens.home.home.SharedOpenSourceLicensesScreen
import org.scottishtecharmy.soundscape.screens.home.home.SharedSleepScreen
import org.scottishtecharmy.soundscape.screens.home.locationDetails.SharedLocationDetailsScreen
import org.scottishtecharmy.soundscape.screens.home.locationDetails.SharedSaveAndEditMarkerScreen
import org.scottishtecharmy.soundscape.screens.home.offlinemaps.NearbyExtractsState
import org.scottishtecharmy.soundscape.screens.home.offlinemaps.OfflineMapsUiState
import org.scottishtecharmy.soundscape.screens.home.offlinemaps.SharedOfflineMapsScreen
import org.scottishtecharmy.soundscape.screens.home.placesnearby.PlacesNearbyScreen
import org.scottishtecharmy.soundscape.screens.home.placesnearby.PlacesNearbyUiState
import org.scottishtecharmy.soundscape.screens.home.settings.SharedSettingsScreen
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomAppBar
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.MarkersAndRoutesUiState
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen.SharedAddAndEditRouteScreen
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.markersscreen.MarkersScreen
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.routedetailsscreen.SharedRouteDetailsScreen
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.routesscreen.RoutesScreen
import org.scottishtecharmy.soundscape.screens.onboarding.SharedOnboardingNavHost
import org.scottishtecharmy.soundscape.screens.onboarding.welcome.Welcome

@Composable
fun SharedNavHost(
    navController: NavHostController,
    navStateHolder: NavigationStateHolder,
    flows: AppFlows,
    callbacks: AppCallbacks,
    startDestination: String = SharedRoutes.WELCOME,
    audioEngine: AudioEngine? = null,
    audioTour: AudioTour? = null,
    preferencesProvider: PreferencesProvider? = null,
    homeContent: (@Composable (NavHostController, NavigationStateHolder) -> Unit)? = null,
    settingsContent: (@Composable (NavHostController) -> Unit)? = null,
    settingsPlatformAudioContent: (LazyListScope.() -> Unit)? = null,
    platformNavBuilder: (NavGraphBuilder.() -> Unit)? = null,
) {
    val pendingIntent by flows.pendingIntent?.collectAsState()
        ?: remember { mutableStateOf<IncomingIntent?>(null) }
    LaunchedEffect(pendingIntent) {
        val intent = pendingIntent ?: return@LaunchedEffect
        when (intent) {
            is IncomingIntent.OpenLocation -> {
                navStateHolder.navigateWithLocation(
                    navController, SharedRoutes.LOCATION_DETAILS, intent.locationDescription,
                )
            }

            is IncomingIntent.OpenLatLon -> {
                val displayName = intent.displayName ?: "${intent.latitude},${intent.longitude}"
                navStateHolder.navigateWithLocation(
                    navController,
                    SharedRoutes.LOCATION_DETAILS,
                    org.scottishtecharmy.soundscape.screens.home.data.LocationDescription(
                        name = displayName,
                        location = LngLatAlt(intent.longitude, intent.latitude),
                    ),
                )
            }

            is IncomingIntent.StartRoute -> callbacks.onStartRoute(intent.routeId)
            IncomingIntent.StopRoute -> callbacks.onRouteStop()
            is IncomingIntent.OpenFeature -> {
                MarkersAndRoutesTabMemory.selected = if (intent.tab == "markers") 0 else 1
                navController.navigate(SharedRoutes.MARKERS_AND_ROUTES)
            }

            is IncomingIntent.ImportRoute -> {
                navStateHolder.setPendingImportRoute(intent.route)
                navController.navigate(SharedRoutes.ADD_ROUTE)
            }

            is IncomingIntent.StartRouteByName -> callbacks.onStartRouteByName(intent.name)
        }
        flows.onPendingIntentHandled?.invoke()
    }

    // Prune navStateHolder's per-entry maps as entries leave the back stack so
    // it doesn't grow unbounded as the user navigates around the app.
    LaunchedEffect(navController) {
        navController.currentBackStack.collect { entries ->
            navStateHolder.prune(entries.map { it.id }.toSet())
        }
    }

    Box(modifier = Modifier) {
        NavHost(navController = navController, startDestination = startDestination) {

            composable(SharedRoutes.WELCOME) {
                Welcome(onNavigate = {
                    navController.navigate(SharedRoutes.HOME) {
                        popUpTo(SharedRoutes.WELCOME) { inclusive = true }
                    }
                })
            }

            composable(SharedRoutes.ONBOARDING) {
                if (audioEngine != null && preferencesProvider != null) {
                    SharedOnboardingNavHost(
                        audioEngine = audioEngine,
                        preferencesProvider = preferencesProvider,
                        beaconTypes = flows.beaconTypes,
                        onFinish = {
                            preferencesProvider.putBoolean(PreferenceKeys.FIRST_LAUNCH, false)
                            navController.navigate(SharedRoutes.HOME) {
                                popUpTo(SharedRoutes.ONBOARDING) { inclusive = true }
                            }
                        },
                        onSetApplicationLocale = callbacks.onSetApplicationLocale,
                    )
                }
            }

            composable(SharedRoutes.HOME) {
                if (homeContent != null) {
                    homeContent(navController, navStateHolder)
                } else {
                    val homeState by flows.homeState?.collectAsState()
                        ?: remember { mutableStateOf(HomeState()) }
                    val recordingEnabled by flows.recordingEnabled?.collectAsState()
                        ?: remember { mutableStateOf(false) }
                    val audioTourRunning by flows.audioTourRunning?.collectAsState()
                        ?: remember { mutableStateOf(false) }
                    val voiceCommandListening by flows.voiceCommandListening?.collectAsState()
                        ?: remember { mutableStateOf(false) }
                    val permissionsRequired by flows.permissionsRequired?.collectAsState()
                        ?: remember { mutableStateOf(false) }

                    SharedHomeScreen(
                        state = homeState,
                        onNavigate = { dest -> navController.navigate(dest) },
                        onSelectLocation = { desc ->
                            navStateHolder.navigateWithLocation(
                                navController, SharedRoutes.LOCATION_DETAILS, desc,
                            )
                        },
                        preferencesProvider = preferencesProvider,
                        onMapLongClick = callbacks.onMapLongClick,
                        bottomButtonFunctions = org.scottishtecharmy.soundscape.screens.home.home.BottomButtonFunctions(
                            myLocation = callbacks.onMyLocation,
                            aroundMe = callbacks.onWhatsAroundMe,
                            aheadOfMe = callbacks.onAheadOfMe,
                            nearbyMarkers = callbacks.onNearbyMarkers,
                        ),
                        routeFunctions = org.scottishtecharmy.soundscape.screens.home.home.RouteFunctions(
                            skipPrevious = callbacks.onRouteSkipPrevious,
                            skipNext = callbacks.onRouteSkipNext,
                            mute = callbacks.onRouteMute,
                            stop = callbacks.onRouteStop,
                        ),
                        streetPreviewFunctions = org.scottishtecharmy.soundscape.screens.home.home.StreetPreviewFunctions(
                            go = callbacks.onStreetPreviewGo,
                            exit = callbacks.onStreetPreviewExit,
                        ),
                        searchFunctions = org.scottishtecharmy.soundscape.screens.home.home.SearchFunctions(
                            onTriggerSearch = callbacks.onSearch,
                        ),
                        getCurrentLocationDescription = callbacks.onGetCurrentLocationDescription,
                        rateSoundscape = callbacks.onRateApp,
                        contactSupport = callbacks.onContactSupport,
                        shareRecording = callbacks.onShareRecording,
                        exitApp = callbacks.onExitApp,
                        toggleTutorial = callbacks.onToggleAudioTour,
                        tutorialRunning = audioTourRunning,
                        recordingEnabled = recordingEnabled,
                        voiceCommandListening = voiceCommandListening,
                        permissionsRequired = permissionsRequired,
                        goToAppSettings = callbacks.onGoToAppSettings,
                        onSleep = {
                            callbacks.onSleep()
                            navController.navigate(SharedRoutes.SLEEP)
                        },
                        onSetApplicationLocale = callbacks.onSetApplicationLocale,
                        getLanguageMismatch = callbacks.onGetLanguageMismatch,
                    )
                }
            }

            composable(SharedRoutes.PLACES_NEARBY) {
                val placesFactory = callbacks.createPlacesNearbyViewModel
                if (placesFactory != null) {
                    val holder = viewModel { placesFactory() }
                    val uiState by holder.uiState.collectAsState()
                    PlacesNearbyScreen(
                        uiState = uiState,
                        onSelectItem = { desc ->
                            navStateHolder.navigateWithLocation(
                                navController, SharedRoutes.LOCATION_DETAILS, desc,
                            )
                        },
                        onClickFolder = { filter, title -> holder.onClickFolder(filter, title) },
                        onClickBack = {
                            if (uiState.level == 0) navController.popBackStack()
                            else holder.onClickBack()
                        },
                        onStartBeacon = { desc ->
                            holder.startBeacon(desc.location, desc.name)
                        },
                    )
                } else {
                    // Fallback path: external state holder publishes via flows.
                    LaunchedEffect(Unit) { audioTour?.onNavigatedToPlacesNearby() }
                    val uiState by flows.placesNearbyUiState?.collectAsState()
                        ?: remember { mutableStateOf(PlacesNearbyUiState()) }
                    PlacesNearbyScreen(
                        uiState = uiState,
                        onSelectItem = { desc ->
                            navStateHolder.navigateWithLocation(
                                navController, SharedRoutes.LOCATION_DETAILS, desc,
                            )
                        },
                        onClickFolder = { filter, title ->
                            callbacks.onPlacesNearbyClickFolder(filter, title)
                        },
                        onClickBack = {
                            if (uiState.level == 0) navController.popBackStack()
                            else callbacks.onPlacesNearbyClickBack()
                        },
                        onStartBeacon = { desc ->
                            callbacks.onStartBeacon(
                                desc.location.latitude,
                                desc.location.longitude,
                                desc.name
                            )
                        },
                    )
                }
            }

            composable(SharedRoutes.MARKERS_AND_ROUTES) {
                MarkersAndRoutesContainer(
                    flows = flows,
                    callbacks = callbacks,
                    audioTour = audioTour,
                    onBack = { navController.popBackStack() },
                    onAddRoute = { navController.navigate(SharedRoutes.ADD_ROUTE) },
                    onSelectMarker = { desc ->
                        navStateHolder.navigateWithLocation(
                            navController, SharedRoutes.LOCATION_DETAILS, desc,
                        )
                    },
                    onSelectRoute = { desc ->
                        navStateHolder.navigateWithLocation(
                            navController, SharedRoutes.ROUTE_DETAILS, desc,
                        )
                    },
                )
            }

            composable(SharedRoutes.LOCATION_DETAILS) { entry ->
                LaunchedEffect(Unit) { audioTour?.onPlaceSelected() }
                val homeState by flows.homeState?.collectAsState()
                    ?: remember { mutableStateOf(HomeState()) }
                // Capture the seed once for this back-stack entry so the screen
                // keeps rendering during the pop animation even after pruning.
                val desc = remember(entry.id) { navStateHolder.selectedLocationFor(entry.id) }
                val shareMessage = stringResource(Res.string.universal_links_marker_share_message)
                if (desc != null) {
                    SharedLocationDetailsScreen(
                        locationDescription = desc,
                        userLocation = homeState.location,
                        heading = homeState.heading,
                        preferencesProvider = preferencesProvider,
                        onNavigateUp = { navController.popBackStack() },
                        onStartBeacon = { loc, name ->
                            callbacks.onStartBeacon(loc.latitude, loc.longitude, name)
                            navController.popBackStack(SharedRoutes.HOME, inclusive = false)
                        },
                        onSaveMarker = { updatedDesc ->
                            audioTour?.onMarkerCreateStarted()
                            navStateHolder.navigateWithLocation(
                                navController, SharedRoutes.EDIT_MARKER, updatedDesc,
                            )
                        },
                        onEditMarker = { updatedDesc ->
                            audioTour?.onMarkerCreateStarted()
                            navStateHolder.navigateWithLocation(
                                navController, SharedRoutes.EDIT_MARKER, updatedDesc,
                            )
                        },
                        onEnableStreetPreview = { loc ->
                            callbacks.onEnableStreetPreview(loc)
                            navController.popBackStack(SharedRoutes.HOME, inclusive = false)
                        },
                        onShareLocation = { sharedDesc ->
                            callbacks.onShareLocation(sharedDesc, shareMessage)
                        },
                        onOfflineMaps = { locationDesc ->
                            navStateHolder.navigateWithOfflineMapsTarget(
                                navController, SharedRoutes.OFFLINE_MAPS, locationDesc.location,
                            )
                        },
                    )
                }
            }

            composable(SharedRoutes.OFFLINE_MAPS) { entry ->
                val location by flows.locationFlow?.collectAsState()
                    ?: remember { mutableStateOf(null) }
                val homeState by flows.homeState?.collectAsState()
                    ?: remember { mutableStateOf(HomeState()) }
                val targetLocation = remember(entry.id) {
                    navStateHolder.offlineMapsTargetFor(entry.id)
                }
                val nearbyState by flows.offlineMapsNearbyExtractsState?.collectAsState()
                    ?: remember { mutableStateOf<NearbyExtractsState>(NearbyExtractsState.Loading) }
                val downloadedFc by flows.offlineMapsDownloadedFc?.collectAsState()
                    ?: remember {
                        mutableStateOf(org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection())
                    }

                // Refresh the manifest on every entry so the nearby list is
                // populated regardless of how the user reached this screen.
                LaunchedEffect(Unit) { callbacks.onOfflineMapsRefresh() }

                // Prefer the location the user navigated from (e.g. a place from
                // location details) so "Nearby offline maps" reflects that place
                // rather than the device's current location.
                val nearbyLngLat = targetLocation
                    ?: location?.let { LngLatAlt(it.longitude, it.latitude) }
                // Translate the manager's manifest state into the UI state. For
                // Loaded, optionally narrow the manifest to the target location.
                val uiNearbyState: NearbyExtractsState =
                    remember(nearbyState, nearbyLngLat) {
                        when (val s = nearbyState) {
                            is NearbyExtractsState.Loading -> NearbyExtractsState.Loading
                            is NearbyExtractsState.Error -> NearbyExtractsState.Error
                            is NearbyExtractsState.Loaded -> {
                                val filtered = if (nearbyLngLat != null) {
                                    callbacks.onOfflineMapsGetExtracts(nearbyLngLat)
                                } else {
                                    s.nearbyExtracts.features
                                }
                                val fc =
                                    org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection()
                                        .apply { filtered.forEach { addFeature(it) } }
                                NearbyExtractsState.Loaded(fc)
                            }
                        }
                    }

                val uiState = OfflineMapsUiState(
                    nearbyExtractsState = uiNearbyState,
                    downloadedExtracts = downloadedFc,
                    userLocation = location?.let { LngLatAlt(it.longitude, it.latitude) },
                    userHeading = homeState.heading,
                    markerLocation = targetLocation,
                )

                SharedOfflineMapsScreen(
                    uiState = uiState,
                    downloadState = flows.offlineMapsDownloadState
                        ?: kotlinx.coroutines.flow.MutableStateFlow(DownloadStateCommon.Idle),
                    onBack = { navController.popBackStack() },
                    onDownload = { name, feature ->
                        callbacks.onOfflineMapsDownload(
                            name,
                            feature
                        )
                    },
                    onDelete = { feature -> callbacks.onOfflineMapsDelete(feature) },
                    onCancelDownload = { callbacks.onOfflineMapsCancelDownload() },
                    preferencesProvider = preferencesProvider,
                )
            }

            composable(SharedRoutes.ADD_ROUTE) {
                val factory = callbacks.createAddAndEditRouteViewModel
                if (factory != null) {
                    val homeState by flows.homeState?.collectAsState()
                        ?: remember { mutableStateOf(HomeState()) }
                    val holder = viewModel { factory() }
                    LaunchedEffect(holder) {
                        holder.loadMarkers()
                        val pendingImport = navStateHolder.pendingImportRoute.value
                        if (pendingImport != null) {
                            holder.initializeFromImport(pendingImport)
                            navStateHolder.setPendingImportRoute(null)
                        }
                    }
                    SharedAddAndEditRouteScreen(
                        holder = holder,
                        isEditing = false,
                        userLocation = homeState.location,
                        heading = homeState.heading,
                        getCurrentLocationDescription = callbacks.onGetCurrentLocationDescription,
                        onNavigateUp = { navController.popBackStack() },
                        onSaveComplete = {
                            navController.popBackStack(
                                SharedRoutes.MARKERS_AND_ROUTES,
                                inclusive = false
                            )
                        },
                        onDeleteComplete = {
                            navController.popBackStack(
                                SharedRoutes.MARKERS_AND_ROUTES,
                                inclusive = false
                            )
                        },
                    )
                }
            }

            composable(SharedRoutes.EDIT_ROUTE) { entry ->
                val factory = callbacks.createAddAndEditRouteViewModel
                val routeDesc = remember(entry.id) { navStateHolder.selectedLocationFor(entry.id) }
                if (factory != null && routeDesc != null) {
                    val homeState by flows.homeState?.collectAsState()
                        ?: remember { mutableStateOf(HomeState()) }
                    val holder = viewModel(key = "edit-route-${routeDesc.databaseId}") { factory() }
                    LaunchedEffect(holder) {
                        holder.loadMarkers()
                        holder.initializeRouteFromDatabase(routeDesc.databaseId)
                    }
                    SharedAddAndEditRouteScreen(
                        holder = holder,
                        isEditing = true,
                        userLocation = homeState.location,
                        heading = homeState.heading,
                        getCurrentLocationDescription = callbacks.onGetCurrentLocationDescription,
                        onNavigateUp = { navController.popBackStack() },
                        onSaveComplete = {
                            navController.popBackStack(
                                SharedRoutes.MARKERS_AND_ROUTES,
                                inclusive = false
                            )
                        },
                        onDeleteComplete = {
                            navController.popBackStack(
                                SharedRoutes.MARKERS_AND_ROUTES,
                                inclusive = false
                            )
                        },
                    )
                }
            }

            composable(SharedRoutes.ROUTE_DETAILS) { entry ->
                val homeState by flows.homeState?.collectAsState()
                    ?: remember { mutableStateOf(HomeState()) }
                val routeDesc = remember(entry.id) { navStateHolder.selectedLocationFor(entry.id) }
                if (routeDesc != null) {
                    val routeWaypoints = remember(routeDesc.databaseId) {
                        callbacks.onLoadRoute(routeDesc.databaseId) ?: emptyList()
                    }
                    val isRoutePlaying =
                        homeState.currentRouteData.routeData?.route?.routeId == routeDesc.databaseId
                    SharedRouteDetailsScreen(
                        routeName = routeDesc.name,
                        routeDescription = routeDesc.description ?: "",
                        waypoints = routeWaypoints,
                        isRoutePlaying = isRoutePlaying,
                        userLocation = homeState.location,
                        heading = homeState.heading,
                        preferencesProvider = preferencesProvider,
                        onNavigateUp = { navController.popBackStack() },
                        onStartRoute = {
                            callbacks.onStartRoute(routeDesc.databaseId)
                            navController.popBackStack(SharedRoutes.HOME, inclusive = false)
                        },
                        onStartRouteInReverse = {
                            callbacks.onStartRouteInReverse(routeDesc.databaseId)
                            navController.popBackStack(SharedRoutes.HOME, inclusive = false)
                        },
                        onStopRoute = { callbacks.onRouteStop() },
                        onEditRoute = {
                            navStateHolder.navigateWithLocation(
                                navController, SharedRoutes.EDIT_ROUTE, routeDesc,
                            )
                        },
                        onShareRoute = {
                            callbacks.onShareRoute(routeDesc.databaseId)
                        },
                    )
                }
            }

            composable(SharedRoutes.EDIT_MARKER) { entry ->
                val homeState by flows.homeState?.collectAsState()
                    ?: remember { mutableStateOf(HomeState()) }
                val desc = remember(entry.id) { navStateHolder.selectedLocationFor(entry.id) }
                if (desc != null) {
                    SharedSaveAndEditMarkerScreen(
                        locationDescription = desc,
                        userLocation = homeState.location,
                        heading = homeState.heading,
                        preferencesProvider = preferencesProvider,
                        onCancel = { navController.popBackStack() },
                        onSave = { updated ->
                            callbacks.onSaveMarker(updated)
                            navController.popBackStack(SharedRoutes.HOME, inclusive = false)
                        },
                        onDelete = { markerId ->
                            callbacks.onDeleteMarker(markerId)
                            navController.popBackStack(SharedRoutes.HOME, inclusive = false)
                        },
                    )
                }
            }

            composable(SharedRoutes.HELP + "/{topic}") { backStackEntry ->
                val topic = backStackEntry.arguments?.read { getString("topic") } ?: ""
                SharedHelpScreen(
                    topic = topic,
                    onNavigate = { dest -> navController.navigate(dest) },
                    onNavigateUp = { navController.popBackStack() },
                    onOpenSourceLicenses = if (callbacks.getOpenSourceLicensesJson != null) {
                        { navController.navigate(SharedRoutes.OPEN_SOURCE_LICENSES) }
                    } else {
                        null
                    },
                )
            }

            composable(SharedRoutes.OPEN_SOURCE_LICENSES) {
                val getJson = callbacks.getOpenSourceLicensesJson
                if (getJson != null) {
                    // Nav-scoped: parsing happens once per visit, expand-state survives
                    // configuration changes, and the VM is cleared when the user pops back.
                    val vm = viewModel { OpenSourceLicensesViewModel(getJson()) }
                    val uiState by vm.uiState.collectAsState()
                    SharedOpenSourceLicensesScreen(
                        licenses = uiState.licenses,
                        onNavigateUp = { navController.popBackStack() },
                        onLicenseClick = vm::toggleLicense,
                    )
                }
            }

            composable(SharedRoutes.SLEEP) {
                SharedSleepScreen(
                    onWakeUp = callbacks.onWakeUp,
                    onExit = {
                        navController.popBackStack(SharedRoutes.HOME, inclusive = false)
                    },
                )
            }

            composable(SharedRoutes.SETTINGS) {
                if (settingsContent != null) {
                    settingsContent(navController)
                } else {
                    SharedSettingsScreen(
                        onNavigateUp = { navController.popBackStack() },
                        beaconTypes = flows.beaconTypes,
                        preferencesProvider = preferencesProvider,
                        platformAudioContent = settingsPlatformAudioContent,
                        onNavigateToAdvancedMarkersAndRoutes = if (callbacks.createAdvancedMarkersAndRoutesSettingsViewModel != null) {
                            { navController.navigate(SharedRoutes.ADVANCED_MARKERS_AND_ROUTES_SETTINGS) }
                        } else {
                            null
                        },
                        onSetApplicationLocale = callbacks.onSetApplicationLocale,
                        onResetSettings = if (preferencesProvider != null) {
                            {
                                preferencesProvider.clearAll()
                                // Hand off to the platform first. Android kills its
                                // own process here so this call never returns and
                                // the navigate-to-onboarding below is unreachable.
                                // iOS leaves it null and falls through to the
                                // in-app navigation, landing the user on the first
                                // onboarding screen of a freshly-defaulted app.
                                callbacks.onResetSettings?.invoke()
                                navController.navigate(SharedRoutes.ONBOARDING) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        } else null,
                        onBeaconPreviewStart = callbacks.onBeaconPreviewStart,
                        onBeaconPreviewUpdate = callbacks.onBeaconPreviewUpdate,
                        onBeaconPreviewStop = callbacks.onBeaconPreviewStop,
                    )
                }
            }

            composable(SharedRoutes.ADVANCED_MARKERS_AND_ROUTES_SETTINGS) {
                val factory = callbacks.createAdvancedMarkersAndRoutesSettingsViewModel
                if (factory != null) {
                    val holder = viewModel { factory() }
                    SharedAdvancedMarkersAndRoutesSettingsScreen(
                        holder = holder,
                        onNavigateUp = { navController.popBackStack() },
                    )
                }
            }

            platformNavBuilder?.invoke(this)
        }

        val audioTourInstruction by flows.audioTourInstruction?.collectAsState()
            ?: remember {
                mutableStateOf<org.scottishtecharmy.soundscape.audio.AudioTourInstruction?>(
                    null
                )
            }
        audioTourInstruction?.let { instruction ->
            AudioTourInstructionDialog(
                instruction = instruction,
                onContinue = { audioTour?.onInstructionAcknowledged() },
            )
        }
    }
}

internal object MarkersAndRoutesTabMemory {
    var selected: Int = 1
}

@Composable
private fun MarkersAndRoutesContainer(
    flows: AppFlows,
    callbacks: AppCallbacks,
    audioTour: AudioTour? = null,
    onBack: () -> Unit,
    onAddRoute: () -> Unit = {},
    onSelectMarker: (LocationDescription) -> Unit = {},
    onSelectRoute: (LocationDescription) -> Unit = {},
) {
    var selectedTab by remember { mutableStateOf(MarkersAndRoutesTabMemory.selected) }
    LaunchedEffect(selectedTab) {
        MarkersAndRoutesTabMemory.selected = selectedTab
        when (selectedTab) {
            0 -> audioTour?.onMarkers()
            1 -> audioTour?.onMarkerAndRoutes()
        }
    }
    val location by flows.locationFlow?.collectAsState() ?: remember { mutableStateOf(null) }
    val userLocation = location?.let { LngLatAlt(it.longitude, it.latitude) }

    Scaffold(
        topBar = {
            Column {
                CustomAppBar(
                    title = stringResource(Res.string.search_view_markers),
                    onNavigateUp = onBack,
                    rightButtonTitle = if (selectedTab == 1) "+" else "",
                    onRightButton = { if (selectedTab == 1) onAddRoute() },
                )
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Markers") },
                        modifier = Modifier.testTag("markersTab")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Routes") },
                        modifier = Modifier.testTag("routesTab")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> {
                    val markersFactory = callbacks.createMarkersViewModel
                    if (markersFactory != null) {
                        val holder = viewModel { markersFactory() }
                        val uiState by holder.uiState.collectAsState()
                        MarkersScreen(
                            uiState = uiState,
                            clearErrorMessage = { holder.clearErrorMessage() },
                            onToggleSortOrder = { holder.toggleSortOrder() },
                            onToggleSortByName = { holder.toggleSortByName() },
                            userLocation = userLocation,
                            onSelectItem = { onSelectMarker(it) },
                            onStartBeacon = { loc, name -> holder.startBeacon(loc, name) },
                        )
                    } else {
                        val uiState by flows.markersUiState?.collectAsState()
                            ?: remember { mutableStateOf(MarkersAndRoutesUiState()) }
                        MarkersScreen(
                            uiState = uiState,
                            clearErrorMessage = {},
                            onToggleSortOrder = {},
                            onToggleSortByName = {},
                            userLocation = userLocation,
                            onSelectItem = { onSelectMarker(it) },
                            onStartBeacon = { loc, name ->
                                callbacks.onStartBeacon(loc.latitude, loc.longitude, name)
                            },
                        )
                    }
                }

                1 -> {
                    val routesFactory = callbacks.createRoutesViewModel
                    if (routesFactory != null) {
                        val holder = viewModel { routesFactory() }
                        val uiState by holder.uiState.collectAsState()
                        RoutesScreen(
                            uiState = uiState,
                            userLocation = userLocation,
                            clearErrorMessage = { holder.clearErrorMessage() },
                            onToggleSortOrder = { holder.toggleSortOrder() },
                            onToggleSortByName = { holder.toggleSortByName() },
                            onSelectItem = { onSelectRoute(it) },
                            onStartPlayback = { holder.startRoute(it) },
                        )
                    } else {
                        val uiState by flows.routesUiState?.collectAsState()
                            ?: remember { mutableStateOf(MarkersAndRoutesUiState()) }
                        RoutesScreen(
                            uiState = uiState,
                            userLocation = userLocation,
                            clearErrorMessage = {},
                            onToggleSortOrder = {},
                            onToggleSortByName = {},
                            onSelectItem = { onSelectRoute(it) },
                            onStartPlayback = { callbacks.onStartRoute(it) },
                        )
                    }
                }
            }
        }
    }
}
