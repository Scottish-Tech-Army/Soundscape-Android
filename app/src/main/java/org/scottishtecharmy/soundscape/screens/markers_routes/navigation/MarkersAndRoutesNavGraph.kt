package org.scottishtecharmy.soundscape.screens.markers_routes.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.scottishtecharmy.soundscape.screens.home.HomeRoutes
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.MarkersScreen
import org.scottishtecharmy.soundscape.screens.markers_routes.marker_route_screens.RoutesScreen
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.addroutescreen.AddRouteScreen

@Composable
fun MarkersAndRoutesNavGraph(
    navController: NavHostController,
    startDestination: String) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(ScreensForMarkersAndRoutes.Markers.route) {
            MarkersScreen(navController = navController)
        }
        composable(ScreensForMarkersAndRoutes.Routes.route) {
            RoutesScreen(navController = navController)
        }

        // AddRouteScreen, accessible within the MarkersAndRoutesScreen
        composable(HomeRoutes.AddRoute.route) {
            AddRouteScreen(navController = navController)
        }
    }
}


