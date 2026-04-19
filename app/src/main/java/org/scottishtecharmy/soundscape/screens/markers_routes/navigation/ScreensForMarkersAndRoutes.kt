package org.scottishtecharmy.soundscape.screens.markers_routes.navigation

import org.jetbrains.compose.resources.StringResource
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.markers_title
import org.scottishtecharmy.soundscape.resources.routes_title

sealed class ScreensForMarkersAndRoutes(
    val route: String,
    val title: StringResource,
    val iconResId: Int? = null,
) {
    data object Markers : ScreensForMarkersAndRoutes(
        route = "markers",
        title = Res.string.markers_title,
        iconResId = R.drawable.ic_markers,
    )
    data object Routes : ScreensForMarkersAndRoutes(
        route = "routes",
        title = Res.string.routes_title,
        iconResId = R.drawable.ic_routes,
    )
}