package org.scottishtecharmy.soundscape.screens.markers_routes.screens.addroutescreen

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.SoundscapeServiceConnection
import org.scottishtecharmy.soundscape.database.local.model.Location
import org.scottishtecharmy.soundscape.database.local.model.MarkerData
import org.scottishtecharmy.soundscape.database.local.model.RouteData
import org.scottishtecharmy.soundscape.database.repository.RoutesRepository
import org.scottishtecharmy.soundscape.geoengine.formatDistance
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import javax.inject.Inject

@HiltViewModel
class AddRouteViewModel @Inject constructor(
    private val routesRepository: RoutesRepository,
    private val soundscapeServiceConnection: SoundscapeServiceConnection,
    @ApplicationContext private val context: Context,
): ViewModel() {
    private val _uiState = MutableStateFlow(AddRouteUiState())
    val uiState: StateFlow<AddRouteUiState> = _uiState

    init {
        loadMarkers()
    }

    private fun loadMarkers() {
        viewModelScope.launch {
            try {
                val userLocation = soundscapeServiceConnection.getLocationFlow()?.firstOrNull()
                val markerVMs =
                    routesRepository.getMarkers().map {
                        val markerLngLat = LngLatAlt(it.location?.longitude ?: 0.0, it.location?.latitude ?: 0.0)
                        LocationDescription(
                            addressName = it.addressName,
                            fullAddress = it.fullAddress,
                            location = markerLngLat,
                            distance =
                            if(userLocation == null)
                                ""
                            else {
                                val userLngLat =
                                    LngLatAlt(userLocation.longitude, userLocation.latitude)
                                formatDistance(userLngLat.distance(markerLngLat), context)
                            },
                            marker = true
                        )
                    }
                _uiState.value = _uiState.value.copy(markers = markerVMs.toMutableList())
            } catch (e: Exception) {
                Log.e("MarkersViewModel", "Failed to load markers: ${e.message}")
            }
        }
    }

    fun onNameChange(newText: String) {
        val showDoneButton = newText.isNotBlank()
        _uiState.value = _uiState.value.copy(
            name = newText,
            showDoneButton = showDoneButton
        )
        _uiState.value = _uiState.value.copy(name = newText)
    }

    fun onDescriptionChange(newText: String) {
        _uiState.value = _uiState.value.copy(description = newText)
    }


    fun onDoneClicked() {
        viewModelScope.launch {
            val routeData = RouteData().apply {
                name = _uiState.value.name
                description = _uiState.value.description
            }
            _uiState.value.routeMembers.forEach() {
                routeData.waypoints.add(
                    MarkerData(
                        addressName = it.addressName ?: "",
                        location = Location(it.location),
                        fullAddress = it.fullAddress ?: ""
                    )
                )
            }

            try {
                routesRepository.insertRoute(routeData)
                Log.d("AddRouteViewModel", "Route saved successfully: ${routeData.name}")
                _uiState.value = _uiState.value.copy(doneActionCompleted = true)
            } catch (e: Exception) {
                Log.e("AddRouteViewModel", "Error saving route: ${e.message}")
                _uiState.value = _uiState.value.copy(errorMessage = "Error saving route: ${e.message}")
            }
        }
    }

    fun resetDoneActionState() {
        _uiState.value = _uiState.value.copy(doneActionCompleted = false)
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    // Reset navigation state after navigation is handled
    fun resetNavigationState() {
        _uiState.value = _uiState.value.copy(navigateToMarkersAndRoutes = false)
    }

    // Validation logic for name and description fields
    private fun isFieldValid(field: String): Boolean {
        return field.isNotBlank()
    }

    private fun validateFields(name: String, description: String): Pair<Boolean, Pair<Boolean, Boolean>> {
        val isNameValid = isFieldValid(name)
        val isDescriptionValid = isFieldValid(description)
        return Pair(isNameValid && isDescriptionValid, Pair(isNameValid, isDescriptionValid))
    }

}
