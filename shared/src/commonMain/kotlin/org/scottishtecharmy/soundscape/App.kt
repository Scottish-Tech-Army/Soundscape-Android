package org.scottishtecharmy.soundscape

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Route
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.locationprovider.DeviceDirection
import org.scottishtecharmy.soundscape.locationprovider.SoundscapeLocation
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.locationDetails.SharedLocationDetailsScreen
import org.scottishtecharmy.soundscape.screens.home.locationDetails.SharedSaveAndEditMarkerScreen
import org.scottishtecharmy.soundscape.screens.home.placesnearby.PlacesNearbyScreen
import org.scottishtecharmy.soundscape.screens.home.placesnearby.PlacesNearbyUiState
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.MarkersAndRoutesUiState
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.markersscreen.MarkersScreen
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.routesscreen.RoutesScreen
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomAppBar
import org.scottishtecharmy.soundscape.screens.home.HomeState
import org.scottishtecharmy.soundscape.screens.home.home.SharedHomeScreen
import org.scottishtecharmy.soundscape.screens.home.offlinemaps.SharedOfflineMapsScreen
import org.scottishtecharmy.soundscape.screens.home.settings.SharedSettingsScreen
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen.SharedAddAndEditRouteScreen
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.routedetailsscreen.SharedRouteDetailsScreen
import org.scottishtecharmy.soundscape.screens.onboarding.welcome.Welcome
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.resources.*
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.network.DownloadStateCommon
import org.scottishtecharmy.soundscape.screens.home.home.PlatformMapContainer
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
    val onPlacesNearbyClickFolder: (String, String) -> Unit = { _, _ -> },
    val onPlacesNearbyClickBack: () -> Unit = {},
    val onOfflineMapsRefresh: () -> Unit = {},
    val onOfflineMapsGetExtracts: (LngLatAlt) -> List<Feature> = { emptyList() },
    val onOfflineMapsDownload: (Feature) -> Unit = {},
    val onOfflineMapsDelete: (String) -> Unit = {},
    val onOfflineMapsCancelDownload: () -> Unit = {},
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
    val offlineMapsDownloadState: StateFlow<DownloadStateCommon>? = null,
    val beaconTypes: List<String> = emptyList(),
)

private enum class Screen {
    WELCOME, HOME, PLACES_NEARBY, MARKERS_AND_ROUTES, LOCATION_DETAILS, OFFLINE_MAPS, ADD_ROUTE, EDIT_ROUTE, EDIT_MARKER, SETTINGS, ROUTE_DETAILS
}

@Composable
fun App(
    flows: AppFlows = AppFlows(),
    callbacks: AppCallbacks = AppCallbacks(),
) {
    MaterialTheme {
        val buttonColors = defaultAppButtonColors(MaterialTheme.colorScheme)
        CompositionLocalProvider(LocalAppButtonColors provides buttonColors) {
            var screen by remember { mutableStateOf(Screen.WELCOME) }
            var selectedLocation by remember { mutableStateOf<LocationDescription?>(null) }
            var previousScreen by remember { mutableStateOf(Screen.HOME) }

            when (screen) {
                Screen.WELCOME -> Welcome(onNavigate = { screen = Screen.HOME })

                Screen.HOME -> {
                    val homeState by flows.homeState?.collectAsState()
                        ?: remember { mutableStateOf(HomeState()) }
                    SharedHomeScreen(
                        homeState = homeState,
                        onMyLocation = callbacks.onMyLocation,
                        onAroundMe = callbacks.onWhatsAroundMe,
                        onAheadOfMe = callbacks.onAheadOfMe,
                        onNearbyMarkers = callbacks.onNearbyMarkers,
                        onNavigateToPlacesNearby = { screen = Screen.PLACES_NEARBY },
                        onNavigateToMarkersAndRoutes = { screen = Screen.MARKERS_AND_ROUTES },
                        onNavigateToOfflineMaps = {
                            callbacks.onOfflineMapsRefresh()
                            screen = Screen.OFFLINE_MAPS
                        },
                        onNavigateToSettings = { screen = Screen.SETTINGS },
                        onRouteSkipPrevious = callbacks.onRouteSkipPrevious,
                        onRouteSkipNext = callbacks.onRouteSkipNext,
                        onRouteMute = callbacks.onRouteMute,
                        onRouteStop = callbacks.onRouteStop,
                        onSearch = callbacks.onSearch,
                        onSearchItemClick = { desc ->
                            selectedLocation = desc
                            previousScreen = Screen.HOME
                            screen = Screen.LOCATION_DETAILS
                        },
                    )
                }

                Screen.PLACES_NEARBY -> {
                    val uiState by flows.placesNearbyUiState?.collectAsState()
                        ?: remember { mutableStateOf(PlacesNearbyUiState()) }
                    PlacesNearbyScreen(
                        uiState = uiState,
                        onSelectItem = { desc ->
                            selectedLocation = desc
                            previousScreen = Screen.PLACES_NEARBY
                            screen = Screen.LOCATION_DETAILS
                        },
                        onClickFolder = { filter, title ->
                            callbacks.onPlacesNearbyClickFolder(filter, title)
                        },
                        onClickBack = {
                            if (uiState.level == 0) {
                                screen = Screen.HOME
                            } else {
                                callbacks.onPlacesNearbyClickBack()
                            }
                        },
                        onStartBeacon = { desc ->
                            callbacks.onStartBeacon(desc.location.latitude, desc.location.longitude, desc.name)
                        },
                    )
                }

                Screen.MARKERS_AND_ROUTES -> {
                    MarkersAndRoutesContainer(
                        flows = flows,
                        callbacks = callbacks,
                        onBack = { screen = Screen.HOME },
                        onAddRoute = { screen = Screen.ADD_ROUTE },
                        onSelectMarker = { desc ->
                            selectedLocation = desc
                            previousScreen = Screen.MARKERS_AND_ROUTES
                            screen = Screen.EDIT_MARKER
                        },
                        onSelectRoute = { desc ->
                            selectedLocation = desc
                            screen = Screen.ROUTE_DETAILS
                        },
                    )
                }

                Screen.LOCATION_DETAILS -> {
                    val homeState by flows.homeState?.collectAsState()
                        ?: remember { mutableStateOf(HomeState()) }
                    val desc = selectedLocation
                    if (desc != null) {
                        SharedLocationDetailsScreen(
                            locationDescription = desc,
                            userLocation = homeState.location,
                            heading = homeState.heading,
                            onNavigateUp = { screen = previousScreen },
                            onStartBeacon = { loc, name ->
                                callbacks.onStartBeacon(loc.latitude, loc.longitude, name)
                                screen = Screen.HOME
                            },
                            onSaveMarker = { desc ->
                                selectedLocation = desc
                                previousScreen = Screen.LOCATION_DETAILS
                                screen = Screen.EDIT_MARKER
                            },
                            onEditMarker = { desc ->
                                selectedLocation = desc
                                previousScreen = Screen.LOCATION_DETAILS
                                screen = Screen.EDIT_MARKER
                            },
                            onEnableStreetPreview = { loc ->
                                // TODO: wire street preview
                            },
                            onOfflineMaps = { locationDesc ->
                                callbacks.onOfflineMapsRefresh()
                                screen = Screen.OFFLINE_MAPS
                            },
                        )
                    }
                }

                Screen.OFFLINE_MAPS -> {
                    val location by flows.locationFlow?.collectAsState()
                        ?: remember { mutableStateOf(null) }
                    val allExtracts by flows.offlineMapsNearbyExtracts?.collectAsState()
                        ?: remember { mutableStateOf(emptyList()) }
                    val downloaded by flows.offlineMapsDownloaded?.collectAsState()
                        ?: remember { mutableStateOf(emptyList()) }

                    // Filter extracts to those containing the current location
                    val userLngLat = location?.let { LngLatAlt(it.longitude, it.latitude) }
                    val containingExtracts = remember(allExtracts, userLngLat) {
                        if (userLngLat != null) {
                            callbacks.onOfflineMapsGetExtracts(userLngLat)
                        } else {
                            allExtracts // show all if no location yet
                        }
                    }

                    SharedOfflineMapsScreen(
                        nearbyExtracts = containingExtracts,
                        downloadedPaths = downloaded,
                        downloadState = flows.offlineMapsDownloadState
                            ?: kotlinx.coroutines.flow.MutableStateFlow(DownloadStateCommon.Idle),
                        onBack = { screen = Screen.HOME },
                        onDownload = { callbacks.onOfflineMapsDownload(it) },
                        onDelete = { callbacks.onOfflineMapsDelete(it) },
                        onCancelDownload = { callbacks.onOfflineMapsCancelDownload() },
                    )
                }

                Screen.ADD_ROUTE -> {
                    val markersState by flows.markersUiState?.collectAsState()
                        ?: remember { mutableStateOf(MarkersAndRoutesUiState()) }
                    SharedAddAndEditRouteScreen(
                        availableMarkers = markersState.entries,
                        onNavigateUp = { screen = Screen.MARKERS_AND_ROUTES },
                        onSave = { name, desc, waypoints ->
                            callbacks.onSaveRoute(name, desc, waypoints)
                            screen = Screen.MARKERS_AND_ROUTES
                        },
                    )
                }

                Screen.EDIT_ROUTE -> {
                    val markersState by flows.markersUiState?.collectAsState()
                        ?: remember { mutableStateOf(MarkersAndRoutesUiState()) }
                    val routeDesc = selectedLocation
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
                            onNavigateUp = { screen = Screen.MARKERS_AND_ROUTES },
                            onSave = { name, desc, waypoints ->
                                callbacks.onSaveRoute(name, desc, waypoints)
                                screen = Screen.MARKERS_AND_ROUTES
                            },
                            onDelete = {
                                callbacks.onDeleteRoute(routeDesc.databaseId)
                                screen = Screen.MARKERS_AND_ROUTES
                            },
                        )
                    }
                }

                Screen.ROUTE_DETAILS -> {
                    val homeState by flows.homeState?.collectAsState()
                        ?: remember { mutableStateOf(HomeState()) }
                    val routeDesc = selectedLocation
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
                            onNavigateUp = { screen = Screen.MARKERS_AND_ROUTES },
                            onStartRoute = {
                                callbacks.onStartRoute(routeDesc.databaseId)
                                screen = Screen.HOME
                            },
                            onStartRouteInReverse = {
                                callbacks.onStartRouteInReverse(routeDesc.databaseId)
                                screen = Screen.HOME
                            },
                            onStopRoute = { callbacks.onRouteStop() },
                            onEditRoute = {
                                screen = Screen.EDIT_ROUTE
                            },
                        )
                    }
                }

                Screen.EDIT_MARKER -> {
                    val homeState by flows.homeState?.collectAsState()
                        ?: remember { mutableStateOf(HomeState()) }
                    val desc = selectedLocation
                    if (desc != null) {
                        SharedSaveAndEditMarkerScreen(
                            locationDescription = desc,
                            userLocation = homeState.location,
                            heading = homeState.heading,
                            onCancel = { screen = previousScreen },
                            onSave = { updated ->
                                callbacks.onSaveMarker(updated)
                                screen = Screen.HOME
                            },
                            onDelete = { markerId ->
                                callbacks.onDeleteMarker(markerId)
                                screen = Screen.HOME
                            },
                        )
                    }
                }

                Screen.SETTINGS -> {
                    SharedSettingsScreen(
                        onNavigateUp = { screen = Screen.HOME },
                        beaconTypes = flows.beaconTypes,
                    )
                }
            }
        }
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
