package org.scottishtecharmy.soundscape.screens.markers_routes.screens.markersscreen

import org.scottishtecharmy.soundscape.database.local.model.RoutePoint

data class MarkersUiState(
    val markers: List<RoutePoint> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSortByName: Boolean = false,
)
