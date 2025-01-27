package org.scottishtecharmy.soundscape.screens.markers_routes.screens.markersscreen

import org.scottishtecharmy.soundscape.database.local.model.RouteData

data class MarkersUiState(
    val routes: List<RouteData> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSortByName: Boolean = false,
)
