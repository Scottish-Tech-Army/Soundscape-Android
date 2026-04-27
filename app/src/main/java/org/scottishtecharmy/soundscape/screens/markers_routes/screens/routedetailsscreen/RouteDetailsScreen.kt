package org.scottishtecharmy.soundscape.screens.markers_routes.screens.routedetailsscreen

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import org.jetbrains.compose.resources.stringResource
import org.koin.androidx.compose.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.preference.PreferenceManager
import org.scottishtecharmy.soundscape.MainActivity.Companion.SHOW_MAP_DEFAULT
import org.scottishtecharmy.soundscape.MainActivity.Companion.SHOW_MAP_KEY
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.HomeRoutes
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.services.RoutePlayerState
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
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    val showMap = sharedPreferences.getBoolean(SHOW_MAP_KEY, SHOW_MAP_DEFAULT)

    // Fetch the route details when the screen is launched
    LaunchedEffect(routeId) {
        viewModel.getRouteById(routeId)
    }

    // Display error message if it exists
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.clearErrorMessage()
        }
    }

    when {
        uiState.isLoading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        uiState.route != null -> {
            val route = uiState.route!!
            val waypoints = route.markers.map { marker ->
                LocationDescription(
                    name = marker.name,
                    location = marker.getLngLatAlt(),
                )
            }
            val thisRoutePlaying = (routePlayerState.routeData?.route?.routeId == routeId)

            SharedRouteDetailsScreen(
                routeName = route.route.name,
                routeDescription = route.route.description,
                waypoints = waypoints,
                isRoutePlaying = thisRoutePlaying,
                userLocation = userLocation,
                heading = heading,
                showMap = showMap,
                onNavigateUp = { navController.popBackStack() },
                onStartRoute = {
                    viewModel.startRoute(routeId)
                    navController.navigate(HomeRoutes.Home.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
                onStartRouteInReverse = {
                    viewModel.startRouteInReverse(routeId)
                    navController.navigate(HomeRoutes.Home.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
                onStopRoute = { viewModel.stopRoute() },
                onEditRoute = {
                    navController.navigate("${HomeRoutes.AddAndEditRoute.route}?command=edit&data=${route.route.routeId}")
                },
                onShareRoute = { viewModel.shareRoute(context, routeId) },
            )
        }

        else -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(Res.string.route_not_found))
            }
        }
    }
}
