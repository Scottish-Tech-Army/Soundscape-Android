package org.scottishtecharmy.soundscape.screens.home

sealed class MainScreens(
    val route: String,
    val title: String,
) {
    data object Home : MainScreens(
        route = "home",
        title = "Home",
    )
    data object Settings : MainScreens(
        route = "settings",
        title = "Settings",
    )
    data object MarkersAndRoutes : MainScreens(
        route = "markers_and_routes_screen",
        title = "MarkersAndRoutesScreen",
    )

    data object AddRoute : MainScreens(
        route = "add_route_screen",
        title = "AddRouteScreen",
    )
}