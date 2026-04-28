package org.scottishtecharmy.soundscape.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import org.scottishtecharmy.soundscape.network.DownloadStateCommon
import org.scottishtecharmy.soundscape.screens.home.HomeState
import org.scottishtecharmy.soundscape.screens.home.home.AudioTourInstructionDialog
import org.scottishtecharmy.soundscape.screens.home.home.SharedHelpScreen
import org.scottishtecharmy.soundscape.screens.home.home.SharedHomeScreen
import org.scottishtecharmy.soundscape.screens.home.home.SharedOpenSourceLicensesScreen
import org.scottishtecharmy.soundscape.screens.home.home.SharedSleepScreen
import org.scottishtecharmy.soundscape.viewmodels.OpenSourceLicensesStateHolder
import org.scottishtecharmy.soundscape.screens.home.locationDetails.SharedLocationDetailsScreen
import org.scottishtecharmy.soundscape.screens.home.locationDetails.SharedSaveAndEditMarkerScreen
import org.scottishtecharmy.soundscape.screens.home.offlinemaps.OfflineMapsUiState
import org.scottishtecharmy.soundscape.screens.home.offlinemaps.SharedOfflineMapsScreen
import org.scottishtecharmy.soundscape.screens.home.placesnearby.PlacesNearbyScreen
import org.scottishtecharmy.soundscape.screens.home.placesnearby.PlacesNearbyUiState
import org.scottishtecharmy.soundscape.screens.home.settings.SharedSettingsScreen
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.MarkersAndRoutesUiState
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen.SharedAddAndEditRouteScreen
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.markersscreen.MarkersScreen
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.routedetailsscreen.SharedRouteDetailsScreen
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.routesscreen.RoutesScreen
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomAppBar
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.preferences.PreferenceKeys
import org.scottishtecharmy.soundscape.preferences.PreferencesProvider
import org.scottishtecharmy.soundscape.screens.onboarding.SharedOnboardingNavHost
import org.scottishtecharmy.soundscape.screens.onboarding.welcome.Welcome
import org.scottishtecharmy.soundscape.resources.*

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
    platformNavBuilder: (NavGraphBuilder.() -> Unit)? = null,
) {
    Box(modifier = Modifier) {
    NavHost(navController = navController, startDestination = startDestination) {

        composable(SharedRoutes.WELCOME) {
            Welcome(onNavigate = { navController.navigate(SharedRoutes.HOME) {
                popUpTo(SharedRoutes.WELCOME) { inclusive = true }
            } })
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
                        navStateHolder.setSelectedLocation(desc)
                        navController.navigate(SharedRoutes.LOCATION_DETAILS)
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
            LaunchedEffect(Unit) { audioTour?.onNavigatedToPlacesNearby() }
            val uiState by flows.placesNearbyUiState?.collectAsState()
                ?: remember { mutableStateOf(PlacesNearbyUiState()) }
            PlacesNearbyScreen(
                uiState = uiState,
                onSelectItem = { desc ->
                    navStateHolder.setSelectedLocation(desc)
                    navController.navigate(SharedRoutes.LOCATION_DETAILS)
                },
                onClickFolder = { filter, title ->
                    callbacks.onPlacesNearbyClickFolder(filter, title)
                },
                onClickBack = {
                    if (uiState.level == 0) {
                        navController.popBackStack()
                    } else {
                        callbacks.onPlacesNearbyClickBack()
                    }
                },
                onStartBeacon = { desc ->
                    callbacks.onStartBeacon(desc.location.latitude, desc.location.longitude, desc.name)
                },
            )
        }

        composable(SharedRoutes.MARKERS_AND_ROUTES) {
            MarkersAndRoutesContainer(
                flows = flows,
                callbacks = callbacks,
                audioTour = audioTour,
                onBack = { navController.popBackStack() },
                onAddRoute = { navController.navigate(SharedRoutes.ADD_ROUTE) },
                onSelectMarker = { desc ->
                    navStateHolder.setSelectedLocation(desc)
                    navController.navigate(SharedRoutes.LOCATION_DETAILS)
                },
                onSelectRoute = { desc ->
                    navStateHolder.setSelectedLocation(desc)
                    navController.navigate(SharedRoutes.ROUTE_DETAILS)
                },
            )
        }

        composable(SharedRoutes.LOCATION_DETAILS) {
            LaunchedEffect(Unit) { audioTour?.onPlaceSelected() }
            val homeState by flows.homeState?.collectAsState()
                ?: remember { mutableStateOf(HomeState()) }
            val desc = navStateHolder.selectedLocation.collectAsState().value
            val shareMessage = stringResource(Res.string.universal_links_marker_share_message)
            if (desc != null) {
                SharedLocationDetailsScreen(
                    locationDescription = desc,
                    userLocation = homeState.location,
                    heading = homeState.heading,
                    onNavigateUp = { navController.popBackStack() },
                    onStartBeacon = { loc, name ->
                        callbacks.onStartBeacon(loc.latitude, loc.longitude, name)
                        navController.popBackStack(SharedRoutes.HOME, inclusive = false)
                    },
                    onSaveMarker = { updatedDesc ->
                        audioTour?.onMarkerCreateStarted()
                        navStateHolder.setSelectedLocation(updatedDesc)
                        navController.navigate(SharedRoutes.EDIT_MARKER)
                    },
                    onEditMarker = { updatedDesc ->
                        audioTour?.onMarkerCreateStarted()
                        navStateHolder.setSelectedLocation(updatedDesc)
                        navController.navigate(SharedRoutes.EDIT_MARKER)
                    },
                    onEnableStreetPreview = { loc ->
                        // TODO: wire street preview
                    },
                    onShareLocation = { sharedDesc ->
                        callbacks.onShareLocation(sharedDesc, shareMessage)
                    },
                    onOfflineMaps = { locationDesc ->
                        navStateHolder.setOfflineMapsTargetLocation(locationDesc.location)
                        callbacks.onOfflineMapsRefresh()
                        navController.navigate(SharedRoutes.OFFLINE_MAPS)
                    },
                )
            }
        }

        composable(SharedRoutes.OFFLINE_MAPS) {
            val location by flows.locationFlow?.collectAsState()
                ?: remember { mutableStateOf(null) }
            val targetLocation by navStateHolder.offlineMapsTargetLocation.collectAsState()
            val allExtracts by flows.offlineMapsNearbyExtracts?.collectAsState()
                ?: remember { mutableStateOf(emptyList()) }
            val downloadedFc by flows.offlineMapsDownloadedFc?.collectAsState()
                ?: remember {
                    mutableStateOf(org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection())
                }

            // Prefer the location the user navigated from (e.g. a place from
            // location details) so "Nearby offline maps" reflects that place
            // rather than the device's current location.
            val nearbyLngLat = targetLocation
                ?: location?.let { LngLatAlt(it.longitude, it.latitude) }
            val nearbyFc = remember(allExtracts, nearbyLngLat) {
                val list = if (nearbyLngLat != null) {
                    callbacks.onOfflineMapsGetExtracts(nearbyLngLat)
                } else {
                    allExtracts
                }
                org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection().apply {
                    list.forEach { addFeature(it) }
                }
            }

            DisposableEffect(Unit) {
                onDispose { navStateHolder.setOfflineMapsTargetLocation(null) }
            }

            val uiState = OfflineMapsUiState(
                nearbyExtracts = nearbyFc,
                downloadedExtracts = downloadedFc,
            )

            SharedOfflineMapsScreen(
                uiState = uiState,
                downloadState = flows.offlineMapsDownloadState
                    ?: kotlinx.coroutines.flow.MutableStateFlow(DownloadStateCommon.Idle),
                onBack = { navController.popBackStack() },
                onDownload = { name, feature -> callbacks.onOfflineMapsDownload(name, feature) },
                onDelete = { feature -> callbacks.onOfflineMapsDelete(feature) },
                onCancelDownload = { callbacks.onOfflineMapsCancelDownload() },
            )
        }

        composable(SharedRoutes.ADD_ROUTE) {
            val factory = callbacks.createAddAndEditRouteStateHolder
            if (factory != null) {
                val homeState by flows.homeState?.collectAsState()
                    ?: remember { mutableStateOf(HomeState()) }
                val holder = remember { factory() }
                DisposableEffect(holder) {
                    holder.loadMarkers()
                    onDispose { holder.dispose() }
                }
                SharedAddAndEditRouteScreen(
                    holder = holder,
                    isEditing = false,
                    userLocation = homeState.location,
                    heading = homeState.heading,
                    getCurrentLocationDescription = callbacks.onGetCurrentLocationDescription,
                    onNavigateUp = { navController.popBackStack() },
                    onSaveComplete = {
                        navController.popBackStack(SharedRoutes.MARKERS_AND_ROUTES, inclusive = false)
                    },
                    onDeleteComplete = {
                        navController.popBackStack(SharedRoutes.MARKERS_AND_ROUTES, inclusive = false)
                    },
                )
            }
        }

        composable(SharedRoutes.EDIT_ROUTE) {
            val factory = callbacks.createAddAndEditRouteStateHolder
            val routeDesc = navStateHolder.selectedLocation.collectAsState().value
            if (factory != null && routeDesc != null) {
                val homeState by flows.homeState?.collectAsState()
                    ?: remember { mutableStateOf(HomeState()) }
                val holder = remember(routeDesc.databaseId) { factory() }
                DisposableEffect(holder) {
                    holder.loadMarkers()
                    holder.initializeRouteFromDatabase(routeDesc.databaseId)
                    onDispose { holder.dispose() }
                }
                SharedAddAndEditRouteScreen(
                    holder = holder,
                    isEditing = true,
                    userLocation = homeState.location,
                    heading = homeState.heading,
                    getCurrentLocationDescription = callbacks.onGetCurrentLocationDescription,
                    onNavigateUp = { navController.popBackStack() },
                    onSaveComplete = {
                        navController.popBackStack(SharedRoutes.MARKERS_AND_ROUTES, inclusive = false)
                    },
                    onDeleteComplete = {
                        navController.popBackStack(SharedRoutes.MARKERS_AND_ROUTES, inclusive = false)
                    },
                )
            }
        }

        composable(SharedRoutes.ROUTE_DETAILS) {
            val homeState by flows.homeState?.collectAsState()
                ?: remember { mutableStateOf(HomeState()) }
            val routeDesc = navStateHolder.selectedLocation.collectAsState().value
            if (routeDesc != null) {
                val routeWaypoints = remember(routeDesc.databaseId) {
                    callbacks.onLoadRoute(routeDesc.databaseId) ?: emptyList()
                }
                val isRoutePlaying = homeState.currentRouteData.routeData?.route?.routeId == routeDesc.databaseId
                SharedRouteDetailsScreen(
                    routeName = routeDesc.name,
                    routeDescription = routeDesc.description ?: "",
                    waypoints = routeWaypoints,
                    isRoutePlaying = isRoutePlaying,
                    userLocation = homeState.location,
                    heading = homeState.heading,
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
                        navController.navigate(SharedRoutes.EDIT_ROUTE)
                    },
                )
            }
        }

        composable(SharedRoutes.EDIT_MARKER) {
            val homeState by flows.homeState?.collectAsState()
                ?: remember { mutableStateOf(HomeState()) }
            val desc = navStateHolder.selectedLocation.collectAsState().value
            if (desc != null) {
                SharedSaveAndEditMarkerScreen(
                    locationDescription = desc,
                    userLocation = homeState.location,
                    heading = homeState.heading,
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
                val stateHolder = remember { OpenSourceLicensesStateHolder(getJson()) }
                val uiState by stateHolder.uiState.collectAsState()
                SharedOpenSourceLicensesScreen(
                    licenses = uiState.licenses,
                    onNavigateUp = { navController.popBackStack() },
                    onLicenseClick = stateHolder::toggleLicense,
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
                )
            }
        }

        platformNavBuilder?.invoke(this)
    }

    val audioTourInstruction by flows.audioTourInstruction?.collectAsState()
        ?: remember { mutableStateOf<org.scottishtecharmy.soundscape.audio.AudioTourInstruction?>(null) }
    audioTourInstruction?.let { instruction ->
        AudioTourInstructionDialog(
            instruction = instruction,
            onContinue = { audioTour?.onInstructionAcknowledged() },
        )
    }
    }
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
    var selectedTab by remember { mutableStateOf(1) }
    LaunchedEffect(selectedTab) {
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
                        text = { Text("Markers") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Routes") }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> {
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
                1 -> {
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
