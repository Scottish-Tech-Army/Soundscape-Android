package org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen

import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription

enum class ActionType {
    UPDATE, DELETE, NONE
}

data class AddAndEditRouteUiState(
    val routeObjectId: Long ?= null,
    val name: String = "",
    val description: String = "",
    val nameError: Boolean = false,
    val descriptionError: Boolean = false,
    val navigateToMarkersAndRoutes: Boolean = false,
    val doneActionCompleted: Boolean = false,
    val errorMessage: String? = null,
    val showDoneButton: Boolean = false,
    val actionType: ActionType = ActionType.NONE,
    val routeMembers: List<LocationDescription> = emptyList(),
    val markers: List<LocationDescription> = emptyList(),
    val toggledMembers: List<LocationDescription> = emptyList()
)
