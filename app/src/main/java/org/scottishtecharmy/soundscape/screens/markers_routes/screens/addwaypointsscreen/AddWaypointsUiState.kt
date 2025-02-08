package org.scottishtecharmy.soundscape.screens.markers_routes.screens.addwaypointsscreen

import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription

data class AddWaypointsUiState(
    val markers: MutableList<LocationDescription> = emptyList<LocationDescription>().toMutableList(),
    val route: MutableList<LocationDescription> = emptyList<LocationDescription>().toMutableList()
)