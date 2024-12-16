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
import androidx.navigation.navigation
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.home.Home
import org.scottishtecharmy.soundscape.screens.home.locationDetails.LocationDetailsScreen
import org.scottishtecharmy.soundscape.screens.home.locationDetails.generateLocationDetailsRoute
import org.scottishtecharmy.soundscape.screens.home.settings.Settings
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.MarkersAndRoutesScreen
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.addroutescreen.AddRouteScreen
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.editroutescreen.EditRouteScreen
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.editroutescreen.EditRouteViewModel
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.routedetailsscreen.RouteDetailsScreen
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
                latitude = state.value.location?.latitude,
                longitude = state.value.location?.longitude,
                beaconLocation = state.value.beaconLocation,
                heading = state.value.heading,
                onNavigate = { dest -> navController.navigate(dest) },
                onMapLongClick = { latLong ->
                    val ld =
                        LocationDescription(
                            adressName = "Current location",
                            latitude =latLong.latitude,
                            longitude = latLong.longitude,
                        )
                    navController.navigate(generateLocationDetailsRoute(ld))
                    true
                },
                onMarkerClick = { marker ->
                    viewModel.onMarkerClick(marker)
                },
                getMyLocation = { viewModel.myLocation() },
                getWhatsAheadOfMe = { viewModel.aheadOfMe() },
                getWhatsAroundMe = { viewModel.whatsAroundMe() },
                searchText = searchText.value,
                isSearching = state.value.isSearching,
                onToogleSearch = viewModel::onToogleSearch,
                onSearchTextChange = viewModel::onSearchTextChange,
                searchItems = state.value.searchItems.orEmpty(),
                shareLocation = { viewModel.shareLocation(context) },
                rateSoundscape = rateSoundscape,
                streetPreviewEnabled = state.value.streetPreviewMode,
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

            // Parse the LocationDescription ot of the json provided by the caller
            val gson = GsonBuilder().create()
            val json = navBackStackEntry.arguments?.getString("json")
            val locationDescription = gson.fromJson(json, LocationDescription::class.java)

            LocationDetailsScreen(
                locationDescription = locationDescription,
                onNavigateUp = {
                    navController.navigate(HomeRoutes.Home.route) {
                        popUpTo(HomeRoutes.Home.route) {
                            inclusive = false // Ensures Home screen is not popped from the stack
                        }
                        launchSingleTop = true // Prevents multiple instances of Home
                    }
                },
                latitude = state.value.location?.latitude,
                longitude = state.value.location?.longitude,
                navController = navController,
                heading = state.value.heading,
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
            )
        }

        // MarkersAndRoutesScreen with tab selection
        navigation(
            startDestination = "${HomeRoutes.MarkersAndRoutes.route}/{tab}",
            route = HomeRoutes.MarkersAndRoutes.route,
        ) {
            composable("${HomeRoutes.MarkersAndRoutes.route}/{tab}") { backStackEntry ->
                val selectedTab = backStackEntry.arguments?.getString("tab")
                MarkersAndRoutesScreen(mainNavController = navController,
                    selectedTab = selectedTab,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing))
            }
        }

        // AddRouteScreen, accessible within the MarkersAndRoutesScreen
        composable(HomeRoutes.AddRoute.route) {
            AddRouteScreen(navController = navController)
        }

        composable(HomeRoutes.RouteDetails.route + "/{routeName}") { backStackEntry ->
            val routeName = backStackEntry.arguments?.getString("routeName") ?: ""
            RouteDetailsScreen(
                routeName = routeName,
                navController = navController)
        }

        // Edit route screen
        composable("edit_route/{routeName}") { backStackEntry ->
            val routeName = backStackEntry.arguments?.getString("routeName") ?: ""
            val editRouteViewModel: EditRouteViewModel = hiltViewModel()

            // Call the ViewModel's function to initialize the route data
            LaunchedEffect(routeName) {
                editRouteViewModel.initializeRoute(routeName)
            }

            // Pass the route details to the EditRouteScreen composable
            val uiState by editRouteViewModel.uiState.collectAsStateWithLifecycle()
            EditRouteScreen(
                routeName = uiState.name,
                routeDescription = uiState.description,
                navController = navController,
                viewModel = editRouteViewModel
            )
        }
    }
}
