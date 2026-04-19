package org.scottishtecharmy.soundscape.screens.markers_routes.screens.routedetailsscreen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.koin.androidx.compose.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.rememberNavController
import androidx.preference.PreferenceManager
import org.scottishtecharmy.soundscape.MainActivity.Companion.SHOW_MAP_DEFAULT
import org.scottishtecharmy.soundscape.MainActivity.Companion.SHOW_MAP_KEY
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.components.LocationItem
import org.scottishtecharmy.soundscape.components.LocationItemDecoration
import org.scottishtecharmy.soundscape.database.local.model.MarkerEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.HomeRoutes
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.home.FullScreenMapFab
import org.scottishtecharmy.soundscape.screens.home.home.MapContainerLibre
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomAppBar
import org.scottishtecharmy.soundscape.screens.markers_routes.components.IconWithTextButton
import org.scottishtecharmy.soundscape.services.RoutePlayerState
import org.scottishtecharmy.soundscape.ui.theme.smallPadding
import org.scottishtecharmy.soundscape.ui.theme.spacing
import org.scottishtecharmy.soundscape.resources.*

@Composable
fun RouteDetailsScreenVM(
    navController: NavController,
    routeId: Long,
    viewModel: RouteDetailsViewModel = koinViewModel(),
    modifier: Modifier,
    userLocation: LngLatAlt?,
    heading: Float,
    routePlayerState: RoutePlayerState
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    RouteDetailsScreen(
        navController,
        routeId,
        modifier,
        uiState,
        routePlayerState,
        getRouteById = { viewModel.getRouteById(routeId) },
        startRoute = { viewModel.startRoute(routeId) },
        startRouteInReverse = { viewModel.startRouteInReverse(routeId) },
        stopRoute = { viewModel.stopRoute() },
        shareRoute = { viewModel.shareRoute(context, routeId) },
        clearErrorMessage = { viewModel.clearErrorMessage() },
        userLocation = userLocation,
        heading = heading
    )
}

@Composable
fun RouteDetailsScreen(
    navController: NavController,
    routeId: Long,
    modifier: Modifier,
    uiState: RouteDetailsUiState,
    routePlayerState: RoutePlayerState,
    getRouteById: () -> Unit,
    startRoute: () -> Unit,
    startRouteInReverse: () -> Unit,
    stopRoute: () -> Unit,
    shareRoute:(routeId: Long) -> Unit,
    clearErrorMessage: () -> Unit,
    userLocation: LngLatAlt?,
    heading: Float
) {
    // Observe the UI state from the ViewModel
    val context = LocalContext.current
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    val showMap = sharedPreferences.getBoolean(SHOW_MAP_KEY, SHOW_MAP_DEFAULT)

    val firstWaypoint = uiState.route?.markers?.firstOrNull()?.getLngLatAlt() ?: LngLatAlt()
    val thisRoutePlaying = (routePlayerState.routeData?.route?.routeId == routeId)
    val fullscreenMap = remember { mutableStateOf(false) }

    // Fetch the route details when the screen is launched
    LaunchedEffect(routeId) {
        getRouteById()
    }

    // Display error message if it exists
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            clearErrorMessage()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            CustomAppBar(
                title = stringResource(Res.string.behavior_experiences_route_nav_title),
                onNavigateUp = {
                    navController.popBackStack()
                }
            )
        },
        floatingActionButton = {
            if(showMap) FullScreenMapFab(fullscreenMap)
        }
    ) { innerPadding ->
        if(fullscreenMap.value) {
            MapContainerLibre(
                beaconLocation = null,
                routeData = uiState.route,
                allowScrolling = true,
                onMapLongClick = { _ -> false },
                mapCenter = firstWaypoint,
                userLocation = userLocation,
                userSymbolRotation = heading,
                modifier = modifier.fillMaxSize(),
                showMap = showMap
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    uiState.route != null -> {
                        val route = uiState.route!!
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface)
                                .smallPadding()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .smallPadding(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = route.route.name,
                                        style = MaterialTheme.typography.headlineMedium,
                                        modifier = Modifier.padding(bottom = spacing.extraSmall),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (route.route.description.isNotEmpty()) {
                                        Text(
                                            text = route.route.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                // Display additional route details if necessary
                            }
                            Column(modifier = Modifier.smallPadding()) {
                                if (thisRoutePlaying) {
                                    IconWithTextButton(
                                        modifier = Modifier.fillMaxWidth(),
                                        icon = Icons.Default.Stop,
                                        textModifier = Modifier.padding(horizontal = spacing.extraSmall),
                                        text = stringResource(Res.string.route_detail_action_stop_route),
                                        talkbackHint = stringResource(Res.string.route_detail_action_stop_route_hint),
                                        color = MaterialTheme.colorScheme.onSurface
                                    ) {
                                        stopRoute()
                                    }


                                } else {
                                    IconWithTextButton(
                                        modifier = Modifier.fillMaxWidth(),
                                        icon = Icons.Default.PlayArrow,
                                        textModifier = Modifier.padding(horizontal = spacing.extraSmall),
                                        talkbackHint = stringResource(Res.string.route_detail_action_start_route_hint),
                                        text = stringResource(Res.string.route_detail_action_start_route),
                                        color = MaterialTheme.colorScheme.onSurface
                                    ) {
                                        startRoute()
                                        // Pop up to the home screen
                                        navController.navigate(HomeRoutes.Home.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                inclusive = true
                                            }
                                            launchSingleTop = true
                                        }
                                    }
                                    IconWithTextButton(
                                        modifier = Modifier.fillMaxWidth(),
                                        icon = Icons.Default.SwapVert,
                                        textModifier = Modifier.padding(horizontal = spacing.extraSmall),
                                        talkbackHint = stringResource(Res.string.route_detail_action_start_route_reverse_hint),
                                        text = stringResource(Res.string.route_detail_action_start_route_reverse),
                                        color = MaterialTheme.colorScheme.onSurface
                                    ) {
                                        startRouteInReverse()
                                        navController.navigate(HomeRoutes.Home.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                inclusive = true
                                            }
                                            launchSingleTop = true
                                        }
                                    }
                                }
                                IconWithTextButton(
                                    modifier = Modifier.fillMaxWidth()
                                        .defaultMinSize(minHeight = spacing.targetSize),
                                    icon = Icons.Default.Edit,
                                    textModifier = Modifier.padding(horizontal = spacing.extraSmall),
                                    text = stringResource(Res.string.route_detail_action_edit),
                                    talkbackHint = stringResource(Res.string.route_detail_action_edit_hint),
                                    color = MaterialTheme.colorScheme.onSurface
                                ) {
                                    navController.navigate("${HomeRoutes.AddAndEditRoute.route}?command=edit&data=${route.route.routeId}")
                                }
                                IconWithTextButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    icon = Icons.Default.Share,
                                    textModifier = Modifier.padding(horizontal = spacing.extraSmall),
                                    text = stringResource(Res.string.share_title),
                                    talkbackHint = stringResource(Res.string.route_detail_action_share_hint),
                                    color = MaterialTheme.colorScheme.onSurface
                                ) {
                                    shareRoute(route.route.routeId)
                                }
                            }
                            // Small map showing route
                            MapContainerLibre(
                                beaconLocation = null,
                                routeData = route,
                                allowScrolling = true,
                                onMapLongClick = { _ -> false },
                                mapCenter = firstWaypoint,
                                userLocation = userLocation,
                                userSymbolRotation = heading,
                                modifier = modifier.fillMaxWidth().weight(1f).smallPadding(),
                                showMap = showMap
                            )
                            Spacer(modifier = Modifier.size(spacing.medium))

                            // List of all route points
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(spacing.tiny),
                                modifier = Modifier.weight(2f)
                            ) {
                                itemsIndexed(route.markers) { index, marker ->
                                    LocationItem(
                                        item = LocationDescription(
                                            name = marker.name,
                                            location = marker.getLngLatAlt(),
                                        ),
                                        decoration = LocationItemDecoration(
                                            location = false,
                                            index = index,
                                            indexDescription = stringResource(Res.string.waypoint_title),
                                        ),
                                        userLocation = userLocation
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(stringResource(Res.string.route_not_found))
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RoutesDetailsPopulatedPreview() {
    val routeData = RouteWithMarkers(
        RouteEntity(
            name = "Route 1",
            description = "Description 1"
        ),
        listOf(
            MarkerEntity(0, "Marker 1", 0.0, 0.0, "Description 1"),
            MarkerEntity(0, "Marker 2", 0.0, 0.0, "Description 2"),
            MarkerEntity(0, "Marker 3", 0.0, 0.0, "Description 3"),
            MarkerEntity(0, "Marker 4", 0.0, 0.0, "Description 4"),
            MarkerEntity(0, "Marker 5", 0.0, 0.0, "Description 5"),
            MarkerEntity(0, "Marker 6", 0.0, 0.0, "Description 6"),
            MarkerEntity(0, "Marker 7", 0.0, 0.0, "Description 7"),
            MarkerEntity(0, "Marker 8", 0.0, 0.0, "Description 8")
        )
    )

    RouteDetailsScreen(
        navController = rememberNavController(),
        routeId = 0L,
        modifier = Modifier,
        uiState = RouteDetailsUiState(
            route = routeData
        ),
        getRouteById = {},
        startRoute = {},
        startRouteInReverse = {},
        stopRoute = {},
        shareRoute = {},
        clearErrorMessage = {},
        userLocation = null,
        heading = 45.0F,
        routePlayerState = RoutePlayerState()
    )
}

@Preview(showBackground = true)
@Composable
fun RoutesDetailsLoadingPreview() {
    RouteDetailsScreen(
        navController = rememberNavController(),
        routeId = 0L,
        uiState = RouteDetailsUiState(isLoading = true),
        modifier = Modifier,
        getRouteById = {},
        startRoute = {},
        startRouteInReverse = {},
        stopRoute = {},
        shareRoute = {},
        clearErrorMessage = {},
        userLocation = null,
        heading = 45.0F,
        routePlayerState = RoutePlayerState()
    )
}

@Preview(showBackground = true)
@Composable
fun RoutesDetailsEmptyPreview() {
    RouteDetailsScreen(
        navController = rememberNavController(),
        routeId = 0L,
        modifier = Modifier,
        uiState = RouteDetailsUiState(),
        getRouteById = {},
        startRoute = {},
        startRouteInReverse = {},
        stopRoute = {},
        shareRoute = {},
        clearErrorMessage = {},
        userLocation = null,
        heading = 45.0F,
        routePlayerState = RoutePlayerState()
    )
}
