package org.scottishtecharmy.soundscape

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Explore
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
import org.scottishtecharmy.soundscape.screens.home.placesnearby.PlacesNearbyScreen
import org.scottishtecharmy.soundscape.screens.home.placesnearby.PlacesNearbyUiState
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.MarkersAndRoutesUiState
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.markersscreen.MarkersScreen
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.routesscreen.RoutesScreen
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomAppBar
import org.scottishtecharmy.soundscape.screens.home.offlinemaps.SharedOfflineMapsScreen
import org.scottishtecharmy.soundscape.screens.onboarding.welcome.Welcome
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.network.DownloadStateCommon
import org.scottishtecharmy.soundscape.ui.theme.LocalAppButtonColors
import org.scottishtecharmy.soundscape.ui.theme.defaultAppButtonColors

data class AppCallbacks(
    val onStartBeacon: (Double, Double, String) -> Unit = { _, _, _ -> },
    val onStopBeacon: () -> Unit = {},
    val onSpeak: (String) -> Unit = {},
    val onStartRoute: (Long) -> Unit = {},
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
    val markersUiState: StateFlow<MarkersAndRoutesUiState>? = null,
    val routesUiState: StateFlow<MarkersAndRoutesUiState>? = null,
    val placesNearbyUiState: StateFlow<PlacesNearbyUiState>? = null,
    val offlineMapsNearbyExtracts: StateFlow<List<Feature>>? = null,
    val offlineMapsDownloaded: StateFlow<List<String>>? = null,
    val offlineMapsDownloadState: StateFlow<DownloadStateCommon>? = null,
)

private enum class Screen {
    WELCOME, HOME, PLACES_NEARBY, MARKERS_AND_ROUTES, LOCATION_DETAILS, OFFLINE_MAPS
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

                Screen.HOME -> HomeScreen(
                    flows = flows,
                    callbacks = callbacks,
                    onNavigateToPlacesNearby = { screen = Screen.PLACES_NEARBY },
                    onNavigateToMarkersAndRoutes = { screen = Screen.MARKERS_AND_ROUTES },
                    onNavigateToOfflineMaps = {
                        callbacks.onOfflineMapsRefresh()
                        screen = Screen.OFFLINE_MAPS
                    },
                )

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
                    )
                }

                Screen.LOCATION_DETAILS -> {
                    val location by flows.locationFlow?.collectAsState()
                        ?: remember { mutableStateOf(null) }
                    val desc = selectedLocation
                    if (desc != null) {
                        SharedLocationDetailsScreen(
                            locationDescription = desc,
                            userLocation = location?.let { LngLatAlt(it.longitude, it.latitude) },
                            onNavigateUp = { screen = previousScreen },
                            onStartBeacon = { loc, name ->
                                callbacks.onStartBeacon(loc.latitude, loc.longitude, name)
                                screen = Screen.HOME
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
            }
        }
    }
}

@Composable
private fun HomeScreen(
    flows: AppFlows,
    callbacks: AppCallbacks,
    onNavigateToPlacesNearby: () -> Unit,
    onNavigateToMarkersAndRoutes: () -> Unit,
    onNavigateToOfflineMaps: () -> Unit,
) {
    val location by flows.locationFlow?.collectAsState() ?: remember { mutableStateOf(null) }
    val direction by flows.directionFlow?.collectAsState() ?: remember { mutableStateOf(null) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Soundscape",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (location != null) {
                val loc = location!!
                val latStr = ((loc.latitude * 100000).toLong() / 100000.0).toString()
                val lonStr = ((loc.longitude * 100000).toLong() / 100000.0).toString()
                Text(
                    text = "$latStr, $lonStr",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (loc.hasAccuracy) {
                    Text(
                        text = "Accuracy: ${loc.accuracy.toInt()} m",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            } else {
                Text(
                    text = "Waiting for location...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            if (direction != null) {
                Text(
                    text = "Heading: ${direction!!.headingDegrees.toInt()}\u00B0",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = onNavigateToPlacesNearby) {
                        Icon(Icons.Rounded.Explore, contentDescription = "Places Nearby")
                    }
                    Text("Nearby", style = MaterialTheme.typography.labelSmall)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = onNavigateToMarkersAndRoutes) {
                        Icon(Icons.Rounded.Route, contentDescription = "Markers & Routes")
                    }
                    Text("Markers & Routes", style = MaterialTheme.typography.labelSmall)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = onNavigateToOfflineMaps) {
                        Icon(Icons.Rounded.Download, contentDescription = "Offline Maps")
                    }
                    Text("Offline Maps", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                Button(onClick = { callbacks.onSpeak("Hello from Soundscape") }) {
                    Text("Speak")
                }
                if (location != null) {
                    Button(onClick = {
                        val loc = location!!
                        callbacks.onStartBeacon(loc.latitude, loc.longitude, "Test Beacon")
                    }) {
                        Text("Beacon")
                    }
                }
                Button(onClick = { callbacks.onStopBeacon() }) {
                    Text("Stop")
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
) {
    var selectedTab by remember { mutableStateOf(0) }
    val location by flows.locationFlow?.collectAsState() ?: remember { mutableStateOf(null) }
    val userLocation = location?.let { LngLatAlt(it.longitude, it.latitude) }

    Scaffold(
        topBar = {
            Column {
                CustomAppBar(
                    title = "Markers & Routes",
                    onNavigateUp = onBack,
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
                        onSelectItem = { callbacks.onSpeak(it.name) },
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
                        onSelectItem = { callbacks.onSpeak(it.name) },
                        onStartPlayback = { callbacks.onStartRoute(it) },
                    )
                }
            }
        }
    }
}
