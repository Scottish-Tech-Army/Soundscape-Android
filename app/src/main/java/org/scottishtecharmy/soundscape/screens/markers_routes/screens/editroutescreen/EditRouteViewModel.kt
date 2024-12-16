package org.scottishtecharmy.soundscape.screens.markers_routes.screens.editroutescreen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.database.local.model.RouteData
import org.scottishtecharmy.soundscape.database.repository.RoutesRepository
import javax.inject.Inject

@HiltViewModel
class EditRouteViewModel @Inject constructor(
    private val routesRepository: RoutesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditRouteUiState())
    val uiState: StateFlow<EditRouteUiState> = _uiState

    // Function to initialize the editing route
    fun initializeRoute(routeName: String) {
        viewModelScope.launch {
            try {
                val route = routesRepository.getRoute(routeName).firstOrNull()
                route?.let {
                    _uiState.value = _uiState.value.copy(
                        name = it.name,
                        description = it.description
                    )
                }
            } catch (e: Exception) {
                Log.e("EditRouteViewModel", "Error loading route: ${e.message}")
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to load route: ${e.message}")
            }
        }
    }

    // Function to handle updates to the route name
    fun onNameChange(newText: String) {
        val showDoneButton = newText.isNotBlank() && _uiState.value.description.isNotBlank()
        _uiState.value = _uiState.value.copy(
            name = newText,
            showDoneButton = showDoneButton
        )
    }

    // Function to handle updates to the route description
    fun onDescriptionChange(newText: String) {
        val showDoneButton = _uiState.value.name.isNotBlank() && newText.isNotBlank()
        _uiState.value = _uiState.value.copy(
            description = newText,
            showDoneButton = showDoneButton
        )
    }

    // Function to handle the save action
    fun updateRoute() {
        viewModelScope.launch {
            val route = RouteData().apply {
                name = _uiState.value.name
                description = _uiState.value.description
            }
            try {
                routesRepository.updateRoute(route)
                Log.d("EditRouteViewModel", "Route updated successfully: ${route.name}")
                _uiState.value = _uiState.value.copy(
                    doneActionCompleted = true,
                    actionType = ActionType.UPDATE
                )
            } catch (e: Exception) {
                Log.e("EditRouteViewModel", "Error updating route: ${e.message}")
                _uiState.value = _uiState.value.copy(errorMessage = "Error updating route: ${e.message}")
            }
        }
    }

    // Function to handle deleting a route
    fun deleteRoute(routeName: String) {
        viewModelScope.launch {
            try {
                routesRepository.deleteRoute(routeName)
                Log.d("EditRouteViewModel", "Route deleted successfully: \$routeName")
                _uiState.value = _uiState.value.copy(
                    doneActionCompleted = true,
                    actionType = ActionType.DELETE
                )
            } catch (e: Exception) {
                Log.e("EditRouteViewModel", "Error deleting route: \${e.message}")
                _uiState.value = _uiState.value.copy(errorMessage = "Error deleting route: \${e.message}")
            }
        }
    }

    // Reset the state for done action
    fun resetDoneActionState() {
        _uiState.value = _uiState.value.copy(doneActionCompleted = false,  actionType = ActionType.NONE)
    }

    // Clear any error messages
    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
