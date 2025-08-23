package org.scottishtecharmy.soundscape.screens.markers_routes.navigation

import androidx.compose.ui.res.stringResource
import org.scottishtecharmy.soundscape.R

sealed class ScreensForMarkersAndRoutes(
    val route: String,
    val title: Int,
    val iconResId: Int? = null,
) {
    data object Markers : ScreensForMarkersAndRoutes(
        route = "markers",
        title = R.string.markers_title,
        iconResId = R.drawable.ic_markers,
    )
    data object Routes : ScreensForMarkersAndRoutes(
        route = "routes",
        title = R.string.routes_title,
        iconResId = R.drawable.ic_routes,
    )
}