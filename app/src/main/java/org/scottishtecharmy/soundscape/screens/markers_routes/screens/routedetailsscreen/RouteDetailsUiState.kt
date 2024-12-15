package org.scottishtecharmy.soundscape.screens.markers_routes.screens.routedetailsscreen

import org.scottishtecharmy.soundscape.database.local.model.RouteData

data class RouteDetailsUiState(
    val route: List<RouteData> = emptyList(),
    val selectedRoute: RouteData? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
