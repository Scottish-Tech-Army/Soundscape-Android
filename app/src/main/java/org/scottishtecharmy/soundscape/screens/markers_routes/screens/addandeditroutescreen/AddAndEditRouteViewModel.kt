package org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.mongodb.kbson.ObjectId
import org.scottishtecharmy.soundscape.SoundscapeServiceConnection
import org.scottishtecharmy.soundscape.database.local.model.Location
import org.scottishtecharmy.soundscape.database.local.model.MarkerData
import org.scottishtecharmy.soundscape.database.local.model.RouteData
import org.scottishtecharmy.soundscape.database.repository.RoutesRepository
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.placesnearby.PlacesNearbySharedLogic
import org.scottishtecharmy.soundscape.viewmodels.createMarker
import javax.inject.Inject

@HiltViewModel
class AddAndEditRouteViewModel @Inject constructor(
    private val routesRepository: RoutesRepository,
    soundscapeServiceConnection: SoundscapeServiceConnection
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddAndEditRouteUiState())
    val uiState: StateFlow<AddAndEditRouteUiState> = _uiState
    val logic = PlacesNearbySharedLogic(soundscapeServiceConnection, viewModelScope)
    var postInit = false
    fun loadMarkers() {
        // Monitor the markers in the database
        viewModelScope.launch {
            routesRepository.getMarkerFlow().collect { markers ->
                val markerVMs = markers.map {
                    val markerLngLat =
                        LngLatAlt(it.location?.longitude ?: 0.0, it.location?.latitude ?: 0.0)
                    LocationDescription(
                        name = it.addressName,
                        location = markerLngLat,
                        databaseId = it.objectId
                    )
                }

                // If the marker has appeared after the initial list of markers from the database,
                // then a new marker was just  created. In that case we need to find out which one
                // it was so that we can add it to the route as well as the list of markers.
                if(postInit) {
                    var newMarker: LocationDescription? = null
                    for(marker in markerVMs) {
                        if(!_uiState.value.markers.contains(marker)) {
                            // This marker wasn't in our old list of markers so it's new
                            newMarker = marker
                        }
                    }
                    if(newMarker != null) {
                        // Add the new marker to our route
                        val updatedList = uiState.value.routeMembers.toMutableList()
                        updatedList.add(newMarker)
                        _uiState.value = _uiState.value.copy(
                            markers = markerVMs.toMutableList(),
                            routeMembers = updatedList
                        )
                        return@collect
                    }
                }
                _uiState.value = _uiState.value.copy(markers = markerVMs.toMutableList())

                // Initialization complete
                postInit = true
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
                    databaseId = waypoint.objectId
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
    fun editComplete() {
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
                        objectId = it.databaseId!!
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

    fun onClickBack() {
        var newLevel = logic.uiState.value.level
        if(newLevel > 0) newLevel = newLevel - 1
        logic._uiState.value = logic.uiState.value.copy(level = newLevel)
    }
    fun onSelectLocation(location: LocationDescription) {
        logic._uiState.value = logic.uiState.value.copy(markerDescription = location)
    }

    fun onClickFolder(filter: String, title: String) {
        // Apply the filter
        val newLevel = logic.uiState.value.level + 1
        logic._uiState.value = logic.uiState.value.copy(level = newLevel, filter = filter, title = title)
    }

    fun createAndAddMarker(locationDescription: LocationDescription) {
        // Kick off adding the marker to the database
        createMarker(locationDescription, routesRepository, viewModelScope)

        // And ensure we're on the top level
        logic._uiState.value = logic.uiState.value.copy(
            markerDescription = null,
            level = 0
        )
    }
}
