package org.scottishtecharmy.soundscape.screens.markers_routes.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.scottishtecharmy.soundscape.screens.home.HomeRoutes
import org.scottishtecharmy.soundscape.screens.markers_routes.components.MarkersAndRoutesAppBar
import org.scottishtecharmy.soundscape.screens.markers_routes.components.MarkersAndRoutesTabs
import org.scottishtecharmy.soundscape.screens.markers_routes.marker_route_screens.RoutesScreen
import org.scottishtecharmy.soundscape.screens.markers_routes.navigation.ScreensForMarkersAndRoutes
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme

@Composable
fun MarkersAndRoutesScreen(
    mainNavController: NavController,
    selectedTab: String? = null // This could be used to determine initial tab state if needed.
) {
// Map tab names to indices.
    val tabMapping = mapOf(
        "markers" to 0,
        "routes" to 1
    )
    val initialTabIndex = tabMapping[selectedTab] ?: 0

    val selectedTabIndex = remember { mutableIntStateOf(initialTabIndex) }

    val showAddIcon = selectedTabIndex.intValue == 1

    // Top bar and tabs
    Scaffold(
        topBar = {
            Column {
                MarkersAndRoutesAppBar(
                    onNavigateUp = { mainNavController.navigateUp()},
                    onNavigateToDestination = {
                        mainNavController.navigate(HomeRoutes.AddRoute.route)
                    },
                )
                MarkersAndRoutesTabs(
                        selectedTabIndex = selectedTabIndex.intValue,
                onTabSelected = { index -> selectedTabIndex.intValue = index }
                )
            }

        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTabIndex.intValue) {
                0 -> MarkersScreen(navController = mainNavController)
                1 -> RoutesScreen(
                    navController = mainNavController,
                    onNavigateToAddRoute = { mainNavController.navigate(HomeRoutes.AddRoute.route) },
                )
            }
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