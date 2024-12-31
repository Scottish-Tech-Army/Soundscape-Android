package org.scottishtecharmy.soundscape.screens.home

import android.util.Log
import androidx.compose.runtime.Composable
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
import org.scottishtecharmy.soundscape.screens.home.settings.Settings
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.MarkersAndRoutesScreen
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.addroute.AddRouteScreen
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
                    viewModel.createBeacon(latLong)
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
                heading = state.value.heading,
            )
        }

        // MarkersAndRoutesScreen with tab selection
        navigation(
            startDestination = "${HomeRoutes.MarkersAndRoutes.route}/{tab}",
            route = HomeRoutes.MarkersAndRoutes.route,
        ) {
            composable("${HomeRoutes.MarkersAndRoutes.route}/{tab}") { backStackEntry ->
                val selectedTab = backStackEntry.arguments?.getString("tab")
                MarkersAndRoutesScreen(mainNavController = navController, selectedTab = selectedTab)
            }
        }

        // AddRouteScreen, accessible within the MarkersAndRoutesScreen
        composable(HomeRoutes.AddRoute.route) {
            AddRouteScreen(navController = navController)
        }
    }
}
