package org.scottishtecharmy.soundscape.screens.markers_routes.screens

import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription

data class MarkersAndRoutesUiState(
    val entries: List<LocationDescription> = emptyList(),
    val markers: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSortByName: Boolean = false,
    val isSortAscending: Boolean = true,
    var userLocation: LngLatAlt? = null
)
