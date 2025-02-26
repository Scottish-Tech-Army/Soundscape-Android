package org.scottishtecharmy.soundscape.screens.markers_routes.screens.markersscreen

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.database.repository.RoutesRepository
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.getSortFieldPreference
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.getSortOrderPreference
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.saveSortFieldPreference
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.saveSortOrderPreference
import javax.inject.Inject

@HiltViewModel
class MarkersViewModel
    @Inject
    constructor(
        private val routesRepository: RoutesRepository,
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(MarkersUiState())
        val uiState: StateFlow<MarkersUiState> = _uiState

        init {
            // Load the saved sort orders
            _uiState.value = _uiState.value.copy(
                isSortByName = getSortFieldPreference(context),
                isSortAscending = getSortOrderPreference(context)
            )

            // Collect the flow of routes from the repository so that we update when routes are added
            // and deleted
            viewModelScope.launch {
                routesRepository.getMarkerFlow().collect { markers ->
                    val locations = markers.map {
                        val markerLngLat =
                            LngLatAlt(it.location?.longitude ?: 0.0, it.location?.latitude ?: 0.0)
                        LocationDescription(
                            name = it.addressName,
                            fullAddress = it.fullAddress,
                            location = markerLngLat,
                            markerObjectId = it.objectId
                        )
                    }
                    _uiState.value =  uiState.value.copy(
                        markers = sortMarkers(locations, _uiState.value.isSortByName, _uiState.value.isSortAscending))
                }
            }
        }

        fun toggleSortByName() {
            val sortByName = !_uiState.value.isSortByName
            saveSortFieldPreference(context, sortByName)
            _uiState.value =  uiState.value.copy(
                isSortByName = sortByName,
                markers = sortMarkers(uiState.value.markers,sortByName, _uiState.value.isSortAscending))
        }

        fun toggleSortOrder() {
            val sortAscending = !_uiState.value.isSortAscending
            saveSortOrderPreference(context, sortAscending)
            _uiState.value =  uiState.value.copy(
                isSortAscending = sortAscending,
                markers = sortMarkers(uiState.value.markers, _uiState.value.isSortByName, sortAscending))
        }

        private fun sortMarkers(
            markers: List<LocationDescription>,
            sortByName: Boolean,
            sortAscending: Boolean) : List<LocationDescription> {
            return if(sortByName) {
                if(sortAscending)
                    markers.sortedBy { it.name }
                else
                    markers.sortedByDescending { it.name }
            } else {
                if(sortAscending)
                    markers.sortedBy { _uiState.value.userLocation?.distance(it.location) }
                else
                    markers.sortedByDescending { _uiState.value.userLocation?.distance(it.location) }
            }
        }

        fun clearErrorMessage() {
            _uiState.value = _uiState.value.copy(errorMessage = null)
        }
    }
