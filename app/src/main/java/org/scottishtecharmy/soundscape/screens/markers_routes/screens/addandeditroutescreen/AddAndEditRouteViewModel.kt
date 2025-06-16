package org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.SoundscapeServiceConnection
import org.scottishtecharmy.soundscape.audio.AudioType
import org.scottishtecharmy.soundscape.database.local.dao.RouteDao
import org.scottishtecharmy.soundscape.database.local.model.MarkerEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers
import org.scottishtecharmy.soundscape.geoengine.PositionedString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.placesnearby.PlacesNearbySharedLogic
import org.scottishtecharmy.soundscape.viewmodels.createMarker
import javax.inject.Inject

@HiltViewModel
class AddAndEditRouteViewModel @Inject constructor(
    private val routeDao: RouteDao,
    private val soundscapeServiceConnection: SoundscapeServiceConnection
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddAndEditRouteUiState())
    val uiState: StateFlow<AddAndEditRouteUiState> = _uiState
    val logic = PlacesNearbySharedLogic(soundscapeServiceConnection, viewModelScope)
    var postInit = false
    fun loadMarkers() {
        // Monitor the markers in the database
        viewModelScope.launch {
            routeDao.getAllMarkersFlow().collect() { markers ->
                val markerVMs = markers.map {
                    val markerLngLat =
                        LngLatAlt(it.longitude, it.latitude)
                    LocationDescription(
                        name = it.name,
                        location = markerLngLat,
                        databaseId = it.markerId
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
    fun initializeRoute(routeData: RouteWithMarkers) {
        val routeMembers = emptyList<LocationDescription>().toMutableList()
        for ((index, waypoint) in routeData.markers.withIndex()) {
            routeMembers.add(
                LocationDescription(
                    name = waypoint.name,
                    location = LngLatAlt(waypoint.longitude, waypoint.latitude),
                    orderId = index.toLong(),
                    databaseId = waypoint.markerId
                )
            )
        }
        _uiState.value = _uiState.value.copy(
            name = routeData.route.name,
            description = routeData.route.description,
            routeMembers = routeMembers,
            routeObjectId = routeData.route.routeId
        )
    }

    // Function to initialize the editing route
    fun initializeRouteFromData(routeData: RouteWithMarkers) {
        viewModelScope.launch {
            try {
                initializeRoute(routeData)
            } catch (e: Exception) {
                Log.e("EditRouteViewModel", "Error loading route: ${e.message}")
                _uiState.value =
                    _uiState.value.copy(errorMessage = "Failed to load route: ${e.message}")
            }
        }
    }

    fun initializeRouteFromDatabase(routeId: Long) {
        viewModelScope.launch {
            try {
                // Read the route from the database
                val route = routeDao.getRouteWithMarkers(routeId)
                if(route == null) throw Exception("Route not found")
                initializeRoute(route)
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
    fun deleteRoute(objectId: Long) {
        viewModelScope.launch {
            try {
                routeDao.removeRoute(objectId)
                Log.d("EditRouteViewModel", "Route deleted successfully: \$routeName")
                _uiState.value = _uiState.value.copy(
                    doneActionCompleted = true,
                    actionType = ActionType.DELETE
                )
            } catch (e: Exception) {
                Log.e("EditRouteViewModel", "Error deleting route: ${e.message}")
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

            // Until it's been put in the database, the routeObjectId will be null
            val routeData = RouteEntity(
                routeId = _uiState.value.routeObjectId ?: 0L,
                name = _uiState.value.name,
                description = _uiState.value.description,
            )
            if(_uiState.value.actionType == ActionType.DELETE) {
                routeDao.removeRoute(_uiState.value.routeObjectId ?: 0L)
                return@launch
            }
            val markers = mutableListOf<MarkerEntity>()
            var newMarkers = false
            _uiState.value.routeMembers.forEach {
                markers.add(
                    MarkerEntity(
                        name = it.name,
                        longitude = it.location.longitude,
                        latitude = it.location.latitude,
                        markerId = it.databaseId
                    )
                )
                if(it.databaseId == 0L)
                    newMarkers = true
            }
            try {
                if(newMarkers)
                    routeDao.insertRouteWithNewMarkers(routeData, markers)
                else
                    routeDao.insertRouteWithExistingMarkers(routeData, markers)
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
        logic.internalUiState.value = logic.uiState.value.copy(level = newLevel)
    }
    fun onSelectLocation(location: LocationDescription) {
        logic.internalUiState.value = logic.uiState.value.copy(markerDescription = location)
    }

    fun onClickFolder(filter: String, title: String) {
        // Apply the filter
        val newLevel = logic.uiState.value.level + 1
        logic.internalUiState.value = logic.uiState.value.copy(level = newLevel, filter = filter, title = title)
    }

    fun createAndAddMarker(
        locationDescription: LocationDescription,
        successMessage: String,
        failureMessage: String
    ) {
        // Kick off adding the marker to the database
        createMarker(locationDescription, routeDao, viewModelScope,
            onSuccess = {
                soundscapeServiceConnection.soundscapeService?.speakCallout(
                    listOf(PositionedString(text = successMessage, type = AudioType.STANDARD)),
                    false
                )
            },
            onFailure = {
                soundscapeServiceConnection.soundscapeService?.speakCallout(
                    listOf(PositionedString(text = failureMessage, type = AudioType.STANDARD)),
                    false
                )
            }
        )

        // And ensure we're on the top level
        logic.internalUiState.value = logic.uiState.value.copy(
            markerDescription = null,
            level = 0
        )
    }
}
