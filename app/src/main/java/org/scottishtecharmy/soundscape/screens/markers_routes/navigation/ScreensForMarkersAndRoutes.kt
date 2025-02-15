package org.scottishtecharmy.soundscape.screens.markers_routes.navigation

import org.scottishtecharmy.soundscape.R

sealed class ScreensForMarkersAndRoutes(
    val route: String,
    val title: String,
    val iconResId: Int? = null,
) {
    data object Markers : ScreensForMarkersAndRoutes(
        route = "markers",
        title = "Markers",
        iconResId = R.drawable.ic_markers,
    )
    data object Routes : ScreensForMarkersAndRoutes(
        route = "routes",
        title = "Routes",
        iconResId = R.drawable.ic_routes,
    )
}