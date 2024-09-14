package org.scottishtecharmy.soundscape.screens.markers_routes.navigation

import org.scottishtecharmy.soundscape.R

sealed class ScreensForMarkersAndRoutes(
    val route: String,
    val title: String,
    val selectedIconResId: Int? = null,  // Optional with default value
    val unselectedIconResId: Int? = null // Optional with default value
) {
    object Home : ScreensForMarkersAndRoutes("home", "Home")
    object Markers : ScreensForMarkersAndRoutes(
        route = "markers",
        title = "Markers",
        selectedIconResId = R.drawable.marker_selected,
        unselectedIconResId = R.drawable.marker_unselected)
    object Routes : ScreensForMarkersAndRoutes(
        route = "routes",
        title = "Routes",
        selectedIconResId = R.drawable.routes_selected,
        unselectedIconResId = R.drawable.routes_unselected)

}