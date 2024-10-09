package org.scottishtecharmy.soundscape.screens.markers_routes.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.scottishtecharmy.soundscape.screens.home.HomeRoutes
import org.scottishtecharmy.soundscape.screens.markers_routes.components.BottomNavigationBar
import org.scottishtecharmy.soundscape.screens.markers_routes.components.MarkersAndRoutesAppBar
import org.scottishtecharmy.soundscape.screens.markers_routes.navigation.MarkersAndRoutesNavGraph
import org.scottishtecharmy.soundscape.screens.markers_routes.navigation.ScreensForMarkersAndRoutes
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme

@Composable
fun MarkersAndRoutesScreen(mainNavController: NavController, selectedTab: String?) {
    // Nested navController for the tab navigation inside MarkersAndRoutes
    val nestedNavController = rememberNavController()

    // Determine if the add icon should be shown based on the current route
    val currentRoute = nestedNavController.currentBackStackEntryAsState().value?.destination?.route
    val showAddIcon = currentRoute == ScreensForMarkersAndRoutes.Routes.route

    Scaffold(
        topBar = {
            MarkersAndRoutesAppBar(
                showAddIcon = showAddIcon,
                onNavigateUp = {
                    mainNavController.navigateUp()
                },
                onNavigateToDestination = {
                    mainNavController.navigate(HomeRoutes.AddRoute.route)
                },
            )
        },
        bottomBar = {
            BottomNavigationBar(navController = nestedNavController)
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            // MarkersAndRoutesNavGraph is now responsible for handling navigation
            MarkersAndRoutesNavGraph(
                navController = nestedNavController,
                startDestination = selectedTab ?: ScreensForMarkersAndRoutes.Markers.route
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun MarkersAndRoutesPreview() {
    SoundscapeTheme {
        MarkersAndRoutesScreen(
            mainNavController = rememberNavController(),
            selectedTab = ScreensForMarkersAndRoutes.Markers.route)
    }
}
