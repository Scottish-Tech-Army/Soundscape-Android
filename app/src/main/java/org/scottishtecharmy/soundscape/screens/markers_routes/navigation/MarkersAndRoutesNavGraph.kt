package org.scottishtecharmy.soundscape.screens.markers_routes.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.scottishtecharmy.soundscape.screens.home.HomeRoutes
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.markersscreen.MarkersScreen
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.addroutescreen.AddRouteScreen
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.editroutescreen.EditRouteScreen
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.editroutescreen.EditRouteViewModel
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.routedetailsscreen.RouteDetailsScreen
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.routesscreen.RoutesScreen

@Composable
fun MarkersAndRoutesNavGraph(
    navController: NavHostController,
    onNavigateToAddRoute: () -> Unit,
    startDestination: String) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(ScreensForMarkersAndRoutes.Markers.route) {
            MarkersScreen(navController = navController)
        }
        composable(ScreensForMarkersAndRoutes.Routes.route) {
            RoutesScreen(
                navController = navController,
                onNavigateToAddRoute = onNavigateToAddRoute
            )
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
        }    }
}


