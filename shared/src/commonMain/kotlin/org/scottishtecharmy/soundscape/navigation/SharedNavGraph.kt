package org.scottishtecharmy.soundscape.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.network.DownloadStateCommon
import org.scottishtecharmy.soundscape.screens.home.HomeState
import org.scottishtecharmy.soundscape.screens.home.home.SharedHelpScreen
import org.scottishtecharmy.soundscape.screens.home.home.SharedHomeScreen
import org.scottishtecharmy.soundscape.screens.home.locationDetails.SharedLocationDetailsScreen
import org.scottishtecharmy.soundscape.screens.home.locationDetails.SharedSaveAndEditMarkerScreen
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
    preferencesProvider: PreferencesProvider? = null,
    homeContent: (@Composable (NavHostController, NavigationStateHolder) -> Unit)? = null,
    settingsContent: (@Composable (NavHostController) -> Unit)? = null,
    platformNavBuilder: (NavGraphBuilder.() -> Unit)? = null,
) {
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
                    onSleep = callbacks.onSleep,
                    onSetApplicationLocale = callbacks.onSetApplicationLocale,
                    getLanguageMismatch = callbacks.onGetLanguageMismatch,
                )
            }
        }

        composable(SharedRoutes.PLACES_NEARBY) {
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
                onBack = { navController.popBackStack() },
                onAddRoute = { navController.navigate(SharedRoutes.ADD_ROUTE) },
                onSelectMarker = { desc ->
                    navStateHolder.setSelectedLocation(desc)
                    navController.navigate(SharedRoutes.EDIT_MARKER)
                },
                onSelectRoute = { desc ->
                    navStateHolder.setSelectedLocation(desc)
                    navController.navigate(SharedRoutes.ROUTE_DETAILS)
                },
            )
        }

        composable(SharedRoutes.LOCATION_DETAILS) {
            val homeState by flows.homeState?.collectAsState()
                ?: remember { mutableStateOf(HomeState()) }
            val desc = navStateHolder.selectedLocation.collectAsState().value
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
                        navStateHolder.setSelectedLocation(updatedDesc)
                        navController.navigate(SharedRoutes.EDIT_MARKER)
                    },
                    onEditMarker = { updatedDesc ->
                        navStateHolder.setSelectedLocation(updatedDesc)
                        navController.navigate(SharedRoutes.EDIT_MARKER)
                    },
                    onEnableStreetPreview = { loc ->
                        // TODO: wire street preview
                    },
                    onOfflineMaps = { locationDesc ->
                        callbacks.onOfflineMapsRefresh()
                        navController.navigate(SharedRoutes.OFFLINE_MAPS)
                    },
                )
            }
        }

        composable(SharedRoutes.OFFLINE_MAPS) {
            val location by flows.locationFlow?.collectAsState()
                ?: remember { mutableStateOf(null) }
            val allExtracts by flows.offlineMapsNearbyExtracts?.collectAsState()
                ?: remember { mutableStateOf(emptyList()) }
            val downloaded by flows.offlineMapsDownloaded?.collectAsState()
                ?: remember { mutableStateOf(emptyList()) }

            val userLngLat = location?.let { LngLatAlt(it.longitude, it.latitude) }
            val containingExtracts = remember(allExtracts, userLngLat) {
                if (userLngLat != null) {
                    callbacks.onOfflineMapsGetExtracts(userLngLat)
                } else {
                    allExtracts
                }
            }

            SharedOfflineMapsScreen(
                nearbyExtracts = containingExtracts,
                downloadedPaths = downloaded,
                downloadState = flows.offlineMapsDownloadState
                    ?: kotlinx.coroutines.flow.MutableStateFlow(DownloadStateCommon.Idle),
                onBack = { navController.popBackStack() },
                onDownload = { callbacks.onOfflineMapsDownload(it) },
                onDelete = { callbacks.onOfflineMapsDelete(it) },
                onCancelDownload = { callbacks.onOfflineMapsCancelDownload() },
            )
        }

        composable(SharedRoutes.ADD_ROUTE) {
            val markersState by flows.markersUiState?.collectAsState()
                ?: remember { mutableStateOf(MarkersAndRoutesUiState()) }
            SharedAddAndEditRouteScreen(
                availableMarkers = markersState.entries,
                onNavigateUp = { navController.popBackStack() },
                onSave = { name, desc, waypoints ->
                    callbacks.onSaveRoute(name, desc, waypoints)
                    navController.popBackStack(SharedRoutes.MARKERS_AND_ROUTES, inclusive = false)
                },
            )
        }

        composable(SharedRoutes.EDIT_ROUTE) {
            val markersState by flows.markersUiState?.collectAsState()
                ?: remember { mutableStateOf(MarkersAndRoutesUiState()) }
            val routeDesc = navStateHolder.selectedLocation.collectAsState().value
            if (routeDesc != null) {
                val routeWaypoints = remember(routeDesc.databaseId) {
                    callbacks.onLoadRoute(routeDesc.databaseId) ?: emptyList()
                }
                SharedAddAndEditRouteScreen(
                    isEditing = true,
                    routeName = routeDesc.name,
                    routeDescription = routeDesc.description ?: "",
                    waypoints = routeWaypoints,
                    availableMarkers = markersState.entries,
                    onNavigateUp = { navController.popBackStack() },
                    onSave = { name, desc, waypoints ->
                        callbacks.onSaveRoute(name, desc, waypoints)
                        navController.popBackStack(SharedRoutes.MARKERS_AND_ROUTES, inclusive = false)
                    },
                    onDelete = {
                        callbacks.onDeleteRoute(routeDesc.databaseId)
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
                onOpenSourceLicenses = callbacks.onOpenSourceLicenses,
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
}

@Composable
private fun MarkersAndRoutesContainer(
    flows: AppFlows,
    callbacks: AppCallbacks,
    onBack: () -> Unit,
    onAddRoute: () -> Unit = {},
    onSelectMarker: (LocationDescription) -> Unit = {},
    onSelectRoute: (LocationDescription) -> Unit = {},
) {
    var selectedTab by remember { mutableStateOf(0) }
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
