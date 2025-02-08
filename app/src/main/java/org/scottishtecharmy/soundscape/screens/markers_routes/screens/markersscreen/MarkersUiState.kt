package org.scottishtecharmy.soundscape.screens.markers_routes.screens.markersscreen

import org.scottishtecharmy.soundscape.database.local.model.MarkerData
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription

data class MarkersUiState(
    val markers: List<LocationDescription> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSortByName: Boolean = false,
    val isSortAscending: Boolean = true,
)

data class MarkerVM(
    val markerData: MarkerData,
    val distance: String?,
)
