package org.scottishtecharmy.soundscape.screens.markers_routes.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.markersscreen.MarkersScreen
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.routesscreen.RoutesScreen

@Composable
fun MarkersAndRoutesNavGraph(
    navController: NavHostController,
    homeNavController: NavController,
    startDestination: String) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(ScreensForMarkersAndRoutes.Markers.route) {
            MarkersScreen(homeNavController = homeNavController)
        }
        composable(ScreensForMarkersAndRoutes.Routes.route) {
            RoutesScreen(homeNavController = homeNavController)
        }
    }
}


