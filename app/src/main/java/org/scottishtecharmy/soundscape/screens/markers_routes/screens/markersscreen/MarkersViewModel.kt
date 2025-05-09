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
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.MarkersAndRoutesUiState
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
        private val _uiState = MutableStateFlow(MarkersAndRoutesUiState(markers = true))
        val uiState: StateFlow<MarkersAndRoutesUiState> = _uiState

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
                            description = it.fullAddress,
                            location = markerLngLat,
                            databaseId = it.objectId
                        )
                    }
                    _uiState.value =  uiState.value.copy(
                        entries = sortMarkers(
                            locations,
                            _uiState.value.isSortByName,
                            _uiState.value.isSortAscending,
                            _uiState.value.userLocation)
                    )
                }
            }
        }
        fun toggleSortByName() {
            _uiState.value = toggleSortByName(_uiState.value, context)
        }

        fun toggleSortOrder() {
            _uiState.value = toggleSortOrder(_uiState.value, context)
        }

        fun clearErrorMessage() {
            _uiState.value = _uiState.value.copy(errorMessage = null)
        }
    }

fun toggleSortByName(uiState: MarkersAndRoutesUiState, context: Context) : MarkersAndRoutesUiState {
    val sortByName = !uiState.isSortByName
    saveSortFieldPreference(context, sortByName)
    return uiState.copy(
        isSortByName = sortByName,
        entries = sortMarkers(
            uiState.entries,
            sortByName,
            uiState.isSortAscending,
            uiState.userLocation)
    )
}

fun toggleSortOrder(uiState: MarkersAndRoutesUiState, context: Context) : MarkersAndRoutesUiState  {
    val sortAscending = !uiState.isSortAscending
    saveSortOrderPreference(context, sortAscending)
    return uiState.copy(
        isSortAscending = sortAscending,
        entries = sortMarkers(
            uiState.entries,
            uiState.isSortByName,
            sortAscending,
            uiState.userLocation)
    )
}

fun sortMarkers(
    markers: List<LocationDescription>,
    sortByName: Boolean,
    sortAscending: Boolean,
    userLocation: LngLatAlt?) : List<LocationDescription> {
    val sortedMarkers = if(sortByName) {
        if(sortAscending)
            markers.sortedBy { it.name }
        else
            markers.sortedByDescending { it.name }
    } else {
        val ruler = userLocation?.createCheapRuler() ?: LngLatAlt().createCheapRuler()
        if(sortAscending)
            markers.sortedBy {
                if(userLocation != null)
                    ruler.distance(userLocation, it.location)
                else
                    0.0
            }
        else
            markers.sortedByDescending {
                if (userLocation != null)
                    ruler.distance(userLocation, it.location)
                else
                    0.0
            }
    }
    return sortedMarkers
}
