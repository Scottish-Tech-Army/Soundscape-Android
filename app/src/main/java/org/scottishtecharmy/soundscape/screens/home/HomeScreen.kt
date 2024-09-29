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
import org.scottishtecharmy.soundscape.screens.home.home.Home
import org.scottishtecharmy.soundscape.screens.home.locationDetails.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.locationDetails.LocationDetailsScreen
import org.scottishtecharmy.soundscape.screens.home.settings.Settings
import org.scottishtecharmy.soundscape.screens.markers_routes.navigation.MarkersAndRoutesNavGraph
import org.scottishtecharmy.soundscape.screens.markers_routes.navigation.ScreensForMarkersAndRoutes
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.AddRouteScreen
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.MarkersAndRoutesScreen
import org.scottishtecharmy.soundscape.viewmodels.HomeViewModel

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
    val location = viewModel.location.collectAsStateWithLifecycle()
    val heading = viewModel.heading.collectAsStateWithLifecycle()
    val beaconLocation = viewModel.beaconLocation.collectAsStateWithLifecycle()
    val streetPreviewMode = viewModel.streetPreviewMode.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = HomeRoutes.Home.route,
    ) {
        // Main navigation
        composable(HomeRoutes.Home.route) {
            val context = LocalContext.current
            Home(
                latitude = location.value?.latitude,
                longitude = location.value?.longitude,
                beaconLocation = beaconLocation.value,
                heading = heading.value,
                onNavigate = {
                    dest -> navController.navigate(dest)
                },
                onMapLongClick = { latLong ->
                    viewModel.createBeacon(latLong)
                },
                onMarkerClick = { marker ->
                    viewModel.onMarkerClick(marker)
                },
                getMyLocation = { viewModel.myLocation() },
                getWhatsAheadOfMe = { viewModel.aheadOfMe() },
                getWhatsAroundMe = { viewModel.whatsAroundMe() },
                shareLocation = { viewModel.shareLocation(context) },
                rateSoundscape = rateSoundscape,
                streetPreviewEnabled = streetPreviewMode.value
            )
        }

        // Settings screen
        composable(HomeRoutes.Settings.route) {
            // Always just pop back out of settings, don't add to the queue
            Settings(
                onNavigateUp = { navController.navigateUp() },
                null
            )
        }

        // Location details screen
        composable(HomeRoutes.LocationDetails.route + "/{json}") { navBackStackEntry ->

            // Parse the LocationDescription ot of the json provided by the caller
            val gson = GsonBuilder().create()
            val json = navBackStackEntry.arguments?.getString("json")
            val ld = gson.fromJson(json, LocationDescription::class.java)

            LocationDetailsScreen(
                locationDescription = ld,
                onNavigateUp = {
                    navController.navigate(HomeRoutes.Home.route) {
                        popUpTo(HomeRoutes.Home.route) {
                            inclusive = false  // Ensures Home screen is not popped from the stack
                        }
                        launchSingleTop = true  // Prevents multiple instances of Home
                    }
                },
                latitude = location.value?.latitude,
                longitude = location.value?.longitude,
                heading = heading.value,
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

            // Nested graph: Routes and Markers
            composable(ScreensForMarkersAndRoutes.Markers.route) {
                MarkersAndRoutesNavGraph(
                    navController = navController,
                    startDestination = "Markers",
                )
            }
        }

        // AddRouteScreen, accessible within the MarkersAndRoutesScreen
        // TODO should not be at this level
        composable(HomeRoutes.AddRoute.route) {
            AddRouteScreen(navController = navController)
        }
    }
}
