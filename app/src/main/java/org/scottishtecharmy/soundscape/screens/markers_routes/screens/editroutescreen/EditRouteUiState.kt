package org.scottishtecharmy.soundscape.screens.markers_routes.screens.editroutescreen

import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription

enum class ActionType {
    UPDATE, DELETE, NONE
}

data class EditRouteUiState(
    val name: String = "",
    val description: String = "",
    val markers: MutableList<LocationDescription> = emptyList<LocationDescription>().toMutableList(),
    val nameError: Boolean = false,
    val descriptionError: Boolean = false,
    val navigateToMarkersAndRoutes: Boolean = false,
    val doneActionCompleted: Boolean = false,
    val errorMessage: String? = null,
    val showDoneButton: Boolean = false,
    val actionType: ActionType = ActionType.NONE
)
