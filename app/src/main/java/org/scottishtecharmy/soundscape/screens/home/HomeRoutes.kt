package org.scottishtecharmy.soundscape.screens.home

sealed class HomeRoutes(
    val route: String,
    val title: String,
) {
    data object Home : HomeRoutes(
        route = "home",
        title = "Home",
    )
    data object Settings : HomeRoutes(
        route = "settings",
        title = "Settings",
    )
    data object MarkersAndRoutes : HomeRoutes(
        route = "markers_and_routes_screen",
        title = "MarkersAndRoutesScreen",
    )
    data object LocationDetails : HomeRoutes(
        route = "location_details",
        title = "LocationDetails",
    )
    data object AddAndEditRoute : HomeRoutes(
        route = "add_and_edit_route_screen",
        title = "AddAndEditRouteScreen",
    )
    data object RouteDetails : HomeRoutes(
        route = "route_details_screen",
        title = "RouteDetailsScreen",
    )
    data object Help : HomeRoutes(
        route = "help_screen",
        title = "HelpScreen",
    )
    data object Sleep : HomeRoutes(
        route = "sleep_screen",
        title = "SleepScreen",
    )
}