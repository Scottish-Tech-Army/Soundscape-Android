package org.scottishtecharmy.soundscape.screens.markers_routes.screens.addroute

data class AddRouteUiState(
    val name: String = "",
    val description: String = "",
    val nameError: Boolean = false,
    val descriptionError: Boolean = false,
    val navigateToMarkersAndRoutes: Boolean = false,
    val showDoneButton: Boolean = false,
    val doneActionCompleted: Boolean = false,
    val errorMessage: String? = null,
)
