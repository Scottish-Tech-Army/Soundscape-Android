package org.scottishtecharmy.soundscape.screens.markers_routes.screens.routesscreen

import org.scottishtecharmy.soundscape.database.local.model.RouteData

data class RoutesUiState(
    val routes: List<RouteData> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
