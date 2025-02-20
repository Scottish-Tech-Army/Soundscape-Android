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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import org.scottishtecharmy.soundscape.database.local.model.RouteData
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
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen.parseSimpleRouteData
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
                    val ld = viewModel.getLocationDescription(location) ?: LocationDescription("", location)
                    navController.navigate(generateLocationDetailsRoute(ld))
                    true
                },
                getMyLocation = { viewModel.myLocation() },
                getWhatsAheadOfMe = { viewModel.aheadOfMe() },
                getWhatsAroundMe = { viewModel.whatsAroundMe() },
                getNearbyMarkers = { viewModel.nearbyMarkers() },
                getCurrentLocationDescription = {
                    if(state.value.location != null) {
                        val location = LngLatAlt(
                            state.value.location!!.longitude,
                            state.value.location!!.latitude
                        )
                        viewModel.getLocationDescription(location) ?: LocationDescription("", location)
                    } else {
                        LocationDescription("", LngLatAlt())
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
                streetPreviewExit = { viewModel.streetPreviewExit() }
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
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
                userLocation = state.value.location
            )
        }

        composable(HomeRoutes.AddAndEditRoute.route + "?command={command}&data={data}",
            arguments = listOf(
                navArgument("command") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("data") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val command = backStackEntry.arguments?.getString("command") ?: ""
            val data = backStackEntry.arguments?.getString("data") ?: ""

            var routeData : RouteData? = null
            when(command) {
                "import" -> {
                    try {
                        routeData = parseSimpleRouteData(data)
                    } catch(e: Exception) {
                        Log.e("RouteDetailsScreen", "Error parsing route data: $e")
                    }
                }
            }

            val addAndEditRouteViewModel: AddAndEditRouteViewModel = hiltViewModel()

            // Call the ViewModel's function to initialize the route data
            LaunchedEffect(data) {
                addAndEditRouteViewModel.loadMarkers()
                if(routeData != null) {
                    addAndEditRouteViewModel.initializeRoute(routeData)
                } else if(command == "edit") {
                    addAndEditRouteViewModel.initializeRouteFromDatabase(data)
                }
            }

            // Pass any route details to the EditRouteScreen composable
            val uiState by addAndEditRouteViewModel.uiState.collectAsStateWithLifecycle()
            AddAndEditRouteScreenVM(
                routeObjectId = uiState.routeObjectId,
                navController = navController,
                viewModel = addAndEditRouteViewModel,
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
                userLocation = state.value.location,
                editRoute = (command == "edit")
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

