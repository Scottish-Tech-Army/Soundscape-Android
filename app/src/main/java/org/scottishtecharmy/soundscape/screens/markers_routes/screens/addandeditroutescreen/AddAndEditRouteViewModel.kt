package org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.mongodb.kbson.ObjectId
import org.scottishtecharmy.soundscape.database.local.model.Location
import org.scottishtecharmy.soundscape.database.local.model.MarkerData
import org.scottishtecharmy.soundscape.database.local.model.RouteData
import org.scottishtecharmy.soundscape.database.repository.RoutesRepository
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import javax.inject.Inject

@HiltViewModel
class AddAndEditRouteViewModel @Inject constructor(
    private val routesRepository: RoutesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddAndEditRouteUiState())
    val uiState: StateFlow<AddAndEditRouteUiState> = _uiState

    fun loadMarkers() {
        viewModelScope.launch {
            try {
                val markerVMs =
                    routesRepository.getMarkers().map {
                        val markerLngLat = LngLatAlt(it.location?.longitude ?: 0.0, it.location?.latitude ?: 0.0)
                        LocationDescription(
                            name = it.addressName,
                            fullAddress = it.fullAddress,
                            location = markerLngLat,
                            markerObjectId = it.objectId
                        )
                    }
                _uiState.value = _uiState.value.copy(markers = markerVMs.toMutableList())
            } catch (e: Exception) {
                Log.e("MarkersViewModel", "Failed to load markers: ${e.message}")
            }
        }
    }

    // Function to initialize an importedRoute
    fun initializeRoute(routeData: RouteData) {
        val routeMembers = emptyList<LocationDescription>().toMutableList()
        for (waypoint in routeData.waypoints) {
            routeMembers.add(
                LocationDescription(
                    name = waypoint.addressName,
                    location = waypoint.location?.location() ?: LngLatAlt(),
                    fullAddress = waypoint.fullAddress,
                    markerObjectId = waypoint.objectId
                )
            )
        }
        _uiState.value = _uiState.value.copy(
            name = routeData.name,
            description = routeData.description,
            routeMembers = routeMembers,
            routeObjectId = routeData.objectId
        )
    }

    // Function to initialize the editing route
    fun initializeRouteFromDatabase(routeId: ObjectId) {
        viewModelScope.launch {
            try {
                val route = routesRepository.getRoute(routeId).firstOrNull()
                route?.let {
                    initializeRoute(route)
                }
            } catch (e: Exception) {
                Log.e("EditRouteViewModel", "Error loading route: ${e.message}")
                _uiState.value =
                    _uiState.value.copy(errorMessage = "Failed to load route: ${e.message}")
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

    // Function to handle deleting a route
    fun deleteRoute(objectId: ObjectId) {
        viewModelScope.launch {
            try {
                routesRepository.deleteRoute(objectId)
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

    // Add/Edit has been completed
    fun onDoneClicked() {
        viewModelScope.launch {
            val routeData = RouteData().apply {
                name = _uiState.value.name
                description = _uiState.value.description
                objectId = _uiState.value.routeObjectId ?: ObjectId()
            }
            _uiState.value.routeMembers.forEach {
                routeData.waypoints.add(
                    MarkerData(
                        addressName = it.name ?: "",
                        location = Location(it.location),
                        fullAddress = it.fullAddress ?: "",
                        objectId = it.markerObjectId!!
                    )
                )
            }

            try {
                routesRepository.insertRoute(routeData)
                Log.d("AddRouteViewModel", "Route saved successfully: ${routeData.name}")
                _uiState.value = _uiState.value.copy(
                    doneActionCompleted = true,
                    actionType = ActionType.UPDATE
                )
            } catch (e: Exception) {
                Log.e("AddRouteViewModel", "Error saving route: ${e.message}")
                _uiState.value = _uiState.value.copy(errorMessage = "Error saving route: ${e.message}")
            }
        }
    }
}
