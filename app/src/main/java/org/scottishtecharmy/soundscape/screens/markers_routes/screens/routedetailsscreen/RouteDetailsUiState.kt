package org.scottishtecharmy.soundscape.screens.markers_routes.screens.routedetailsscreen

import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers

data class RouteDetailsUiState(
    val route: RouteWithMarkers? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
