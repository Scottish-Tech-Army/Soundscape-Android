package org.scottishtecharmy.soundscape.screens.markers_routes.screens.routedetailsscreen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.rememberNavController
import org.mongodb.kbson.ObjectId
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.components.LocationItem
import org.scottishtecharmy.soundscape.components.LocationItemDecoration
import org.scottishtecharmy.soundscape.database.local.model.MarkerData
import org.scottishtecharmy.soundscape.database.local.model.RouteData
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.HomeRoutes
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.home.MapContainerLibre
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomAppBar
import org.scottishtecharmy.soundscape.screens.markers_routes.components.IconWithTextButton
import org.scottishtecharmy.soundscape.services.RoutePlayerState
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme

@Composable
fun RouteDetailsScreenVM(
    navController: NavController,
    routeId: ObjectId,
    viewModel: RouteDetailsViewModel = hiltViewModel(),
    modifier: Modifier,
    userLocation: LngLatAlt?,
    heading: Float,
    routePlayerState: RoutePlayerState
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RouteDetailsScreen(
        navController,
        routeId,
        modifier,
        uiState,
        routePlayerState,
        getRouteById = { viewModel.getRouteById(routeId) },
        startRoute = { viewModel.startRoute(routeId) },
        stopRoute = { viewModel.stopRoute() },
        clearErrorMessage = { viewModel.clearErrorMessage() },
        userLocation = userLocation,
        heading = heading
    )
}

@Composable
fun RouteDetailsScreen(
    navController: NavController,
    routeId: ObjectId,
    modifier: Modifier,
    uiState: RouteDetailsUiState,
    routePlayerState: RoutePlayerState,
    getRouteById: (routeId: ObjectId) -> Unit,
    startRoute: (routeId: ObjectId) -> Unit,
    stopRoute: () -> Unit,
    clearErrorMessage: () -> Unit,
    userLocation: LngLatAlt?,
    heading: Float
) {
    // Observe the UI state from the ViewModel
    val context = LocalContext.current
    val location = uiState.route?.waypoints?.firstOrNull()?.location?.location() ?: LngLatAlt()
    val thisRoutePlaying = (routePlayerState.routeData?.objectId == routeId)

    // Fetch the route details when the screen is launched
    LaunchedEffect(routeId) {
        getRouteById(routeId)
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
                title = stringResource(R.string.behavior_experiences_route_nav_title),
                onNavigateUp = {
                    navController.popBackStack()
                }
            )
        }
    ) { innerPadding ->
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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = uiState.route.name,
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                if(uiState.route.description.isNotEmpty()) {
                                    Text(
                                        text = uiState.route.description,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                            // Display additional route details if necessary
                        }
                        Column(modifier = Modifier.height(150.dp)) {
                            if(thisRoutePlaying) {
                                IconWithTextButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    icon = Icons.Default.Stop,
                                    iconModifier = Modifier.size(40.dp),
                                    textModifier = Modifier.padding(horizontal = 4.dp),
                                    iconText = stringResource(R.string.route_detail_action_stop_route),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    onClick = {
                                        stopRoute()
                                    }
                                )

                            } else {
                                IconWithTextButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    icon = Icons.Default.PlayArrow,
                                    iconModifier = Modifier.size(40.dp),
                                    textModifier = Modifier.padding(horizontal = 4.dp),
                                    iconText = stringResource(R.string.route_detail_action_start_route),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    onClick = {
                                        startRoute(uiState.route.objectId)
                                        // Pop up to the home screen
                                        navController.navigate(HomeRoutes.Home.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                inclusive = true
                                            }
                                            launchSingleTop = true
                                        }
                                    })
                            }
                            IconWithTextButton(
                                modifier = Modifier.fillMaxWidth(),
                                icon = Icons.Default.Edit,
                                iconModifier = Modifier.size(40.dp),
                                textModifier = Modifier.padding(horizontal = 4.dp),
                                iconText = stringResource(R.string.route_detail_action_edit),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                onClick = { navController.navigate("${HomeRoutes.AddAndEditRoute.route}?command=edit&data=${uiState.route.objectId.toHexString()}") })
                            IconWithTextButton(
                                modifier = Modifier.fillMaxWidth(),
                                icon = Icons.Default.Share,
                                iconModifier = Modifier.size(40.dp),
                                textModifier = Modifier.padding(horizontal = 4.dp),
                                iconText = stringResource(R.string.share_title),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                onClick = { /*TODO*/ })
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .weight(1f),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Small map showing first route point
                            MapContainerLibre(
                                beaconLocation = null,
                                routeData = uiState.route,
                                allowScrolling = true,
                                onMapLongClick = { _ -> false },
                                // Center on the beacon
                                mapCenter = location,
                                userLocation = userLocation,
                                mapViewRotation = 0.0F,
                                userSymbolRotation = heading,
                                modifier = modifier.fillMaxWidth()
                            )
                        }
                        // List of all route points
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.weight(2f)
                        ) {
                            itemsIndexed(uiState.route.waypoints) { index, marker ->
                                LocationItem(
                                    item = LocationDescription(
                                        name = marker.addressName,
                                        location = marker.location?.location() ?: LngLatAlt(),
                                    ),
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .background(MaterialTheme.colorScheme.primary),
                                    decoration = LocationItemDecoration(
                                        location = false,
                                        index = index,
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
                        Text("Route not found")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RoutesDetailsPopulatedPreview() {
    val routeData = RouteData(
        name = "Route 1",
        description = "Description 1"
    )
    routeData.waypoints.add(MarkerData("Marker 1", null, "Description 1"))
    routeData.waypoints.add(MarkerData("Marker 2", null, "Description 2"))
    routeData.waypoints.add(MarkerData("Marker 3", null, "Description 3"))
    routeData.waypoints.add(MarkerData("Marker 4", null, "Description 4"))
    routeData.waypoints.add(MarkerData("Marker 5", null, "Description 5"))
    routeData.waypoints.add(MarkerData("Marker 6", null, "Description 6"))
    routeData.waypoints.add(MarkerData("Marker 7", null, "Description 7"))
    routeData.waypoints.add(MarkerData("Marker 8", null, "Description 8"))

    SoundscapeTheme {
        RouteDetailsScreen(
            navController = rememberNavController(),
            routeId = ObjectId(),
            modifier = Modifier,
            uiState = RouteDetailsUiState(
                route = routeData
            ),
            getRouteById = {},
            startRoute = {},
            stopRoute = {},
            clearErrorMessage = {},
            userLocation = null,
            heading = 0.0F,
            routePlayerState = RoutePlayerState()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RoutesDetailsLoadingPreview() {
    SoundscapeTheme {
        RouteDetailsScreen(
            navController = rememberNavController(),
            routeId = ObjectId(),
            uiState = RouteDetailsUiState(isLoading = true),
            modifier = Modifier,
            getRouteById = {},
            startRoute = {},
            stopRoute = {},
            clearErrorMessage = {},
            userLocation = null,
            heading = 0.0F,
            routePlayerState = RoutePlayerState()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RoutesDetailsEmptyPreview() {
    SoundscapeTheme {
        RouteDetailsScreen(
            navController = rememberNavController(),
            routeId = ObjectId(),
            modifier = Modifier,
            uiState = RouteDetailsUiState(),
            getRouteById = {},
            startRoute = {},
            stopRoute = {},
            clearErrorMessage = {},
            userLocation = null,
            heading = 0.0F,
            routePlayerState = RoutePlayerState()
        )
    }
}
