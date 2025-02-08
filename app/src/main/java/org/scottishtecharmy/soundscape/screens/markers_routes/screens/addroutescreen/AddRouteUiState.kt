package org.scottishtecharmy.soundscape.screens.markers_routes.screens.addroutescreen

import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription

data class AddRouteUiState(
    val name: String = "",
    val description: String = "",
    val nameError: Boolean = false,
    val descriptionError: Boolean = false,
    val navigateToMarkersAndRoutes: Boolean = false,
    val showDoneButton: Boolean = false,
    val doneActionCompleted: Boolean = false,
    val errorMessage: String? = null,
    val routeMembers: MutableList<LocationDescription> = emptyList<LocationDescription>().toMutableList(),
    val markers: MutableList<LocationDescription> = emptyList<LocationDescription>().toMutableList()
)
