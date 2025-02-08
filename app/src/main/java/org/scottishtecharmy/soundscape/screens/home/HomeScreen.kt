package org.scottishtecharmy.soundscape.screens.home

import android.util.Log
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.home.HelpScreen
import org.scottishtecharmy.soundscape.screens.home.home.Home
import org.scottishtecharmy.soundscape.screens.home.home.SleepScreen
import org.scottishtecharmy.soundscape.screens.home.locationDetails.LocationDetailsScreen
import org.scottishtecharmy.soundscape.screens.home.locationDetails.generateLocationDetailsRoute
import org.scottishtecharmy.soundscape.screens.home.settings.Settings
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.MarkersAndRoutesScreen
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen.AddAndEditRouteScreenVM
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen.AddAndEditRouteViewModel
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen.newRouteName
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.routedetailsscreen.RouteDetailsScreenVM
import org.scottishtecharmy.soundscape.viewmodels.SettingsViewModel
import org.scottishtecharmy.soundscape.viewmodels.home.HomeViewModel

class Navigator {
    var destination = MutableStateFlow(HomeRoutes.Home.route)

    fun navigate(newDestination: String) {
        Log.d("NavigationRoot", "Navigate to $newDestination")
        this.destination.value = newDestination
    }
}

@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: HomeViewModel = hiltViewModel(),
    rateSoundscape: () -> Unit,
) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    val searchText = viewModel.searchText.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = HomeRoutes.Home.route,
    ) {
        // Main navigation
        composable(HomeRoutes.Home.route) {
            val context = LocalContext.current
            Home(
                location = state.value.location,
                beaconLocation = state.value.beaconLocation,
                heading = state.value.heading,
                onNavigate = { dest -> navController.navigate(dest) },
                onMapLongClick = { latLong ->
                    val location = LngLatAlt(latLong.longitude, latLong.latitude)
                    val ld = viewModel.getLocationDescription(location) ?: LocationDescription()
                    navController.navigate(generateLocationDetailsRoute(ld))
                    true
                },
                onMarkerClick = { marker ->
                    viewModel.onMarkerClick(marker)
                },
                getMyLocation = { viewModel.myLocation() },
                getWhatsAheadOfMe = { viewModel.aheadOfMe() },
                getWhatsAroundMe = { viewModel.whatsAroundMe() },
                getCurrentLocationDescription = {
                    if(state.value.location != null) {
                        val location = LngLatAlt(
                            state.value.location!!.longitude,
                            state.value.location!!.latitude
                        )
                        viewModel.getLocationDescription(location) ?: LocationDescription()
                    } else {
                        LocationDescription()
                    }
                },
                searchText = searchText.value,
                isSearching = state.value.isSearching,
                onToggleSearch = viewModel::onToggleSearch,
                onSearchTextChange = viewModel::onSearchTextChange,
                searchItems = state.value.searchItems.orEmpty(),
                shareLocation = { viewModel.shareLocation(context) },
                rateSoundscape = rateSoundscape,
                streetPreviewState = state.value.streetPreviewState,
                streetPreviewGo = { viewModel.streetPreviewGo() },
                streetPreviewExit = { viewModel.streetPreviewExit() },
                tileGridGeoJson = state.value.tileGridGeoJson,
            )
        }

        // Settings screen
        composable(HomeRoutes.Settings.route) {
            // Always just pop back out of settings, don't add to the queue
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val uiState = settingsViewModel.state.collectAsStateWithLifecycle()
            Settings(
                onNavigateUp = { navController.navigateUp() },
                uiState = uiState.value,
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
            )
        }

        // Location details screen
        composable(HomeRoutes.LocationDetails.route + "/{json}") { navBackStackEntry ->

            // Parse the LocationDescription out of the json provided by the caller
            val gson = GsonBuilder().create()
            val json = navBackStackEntry.arguments?.getString("json")
            val locationDescription = gson.fromJson(json, LocationDescription::class.java)

            LocationDetailsScreen(
                locationDescription = locationDescription,
                onNavigateUp = {
                    // If the location is a marker, then we're in the routes menu, so just pop back
                    // up. Otherwise, pop up to Home.
                    if(locationDescription.markerObjectId != null) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(HomeRoutes.Home.route) {
                            popUpTo(HomeRoutes.Home.route) {
                                inclusive =
                                    false // Ensures Home screen is not popped from the stack
                            }
                            launchSingleTop = true // Prevents multiple instances of Home
                        }
                    }
                },
                location = state.value.location,
                navController = navController,
                heading = state.value.heading,
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
            )
        }

        // MarkersAndRoutesScreen with tab selection
        composable(HomeRoutes.MarkersAndRoutes.route) {
            MarkersAndRoutesScreen(
                navController = navController,
                viewModel = viewModel,
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing))
        }

        composable(HomeRoutes.RouteDetails.route + "/{routeName}") { backStackEntry ->
            val routeName = backStackEntry.arguments?.getString("routeName") ?: ""
            RouteDetailsScreenVM(
                routeName = routeName,
                navController = navController,
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
            )
        }

        composable(HomeRoutes.AddAndEditRoute.route + "/{routeName}") { backStackEntry ->
            val routeName = backStackEntry.arguments?.getString("routeName") ?: newRouteName
            val addAndEditRouteViewModel: AddAndEditRouteViewModel = hiltViewModel()

            // Call the ViewModel's function to initialize the route data
            LaunchedEffect(routeName) {
                addAndEditRouteViewModel.loadMarkers()
                addAndEditRouteViewModel.initializeRoute(routeName)
            }

            // Pass any route details to the EditRouteScreen composable
            val uiState by addAndEditRouteViewModel.uiState.collectAsStateWithLifecycle()
            AddAndEditRouteScreenVM(
                routeObjectId = uiState.routeObjectId,
                routeName = routeName,
                routeDescription = uiState.description,
                navController = navController,
                viewModel = addAndEditRouteViewModel,
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
            )
        }

        composable(HomeRoutes.Help.route + "/{topic}") { backStackEntry ->
            val topic = backStackEntry.arguments?.getString("topic") ?: ""
            HelpScreen(
                topic = topic,
                navController = navController,
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
            )
        }
        composable(HomeRoutes.Sleep.route) {
            SleepScreen(
                navController = navController,
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
            )
        }
    }
}

