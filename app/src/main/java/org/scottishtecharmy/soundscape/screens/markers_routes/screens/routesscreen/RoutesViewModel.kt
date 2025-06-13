package org.scottishtecharmy.soundscape.screens.markers_routes.screens.routesscreen

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.database.local.dao.RouteDao
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.getSortFieldPreference
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.getSortOrderPreference
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.MarkersAndRoutesUiState
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.markersscreen.sortMarkers
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.markersscreen.toggleSortByName
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.markersscreen.toggleSortOrder
import javax.inject.Inject

@HiltViewModel
class RoutesViewModel @Inject constructor(
    private val routeDao: RouteDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(MarkersAndRoutesUiState(markers = false))
    val uiState: StateFlow<MarkersAndRoutesUiState> = _uiState

    init {
        // Load the saved sort orders
        _uiState.value = _uiState.value.copy(
            isSortByName = getSortFieldPreference(context),
            isSortAscending = getSortOrderPreference(context))

        // Collect the flow of routes from the repository so that we update when routes are added
        // and deleted. We turn the routes into LocationDescriptions so that they can be displayed
        // using common code with Markers.
        viewModelScope.launch {
            routeDao.getAllRoutesWithMarkersFlow().collect { routes ->
                _uiState.value =  uiState.value.copy(
                    entries = sortMarkers(
                        routes.map {
                            // Turn RouteData into LocationDescription
                            LocationDescription(
                                name = it.route.name,
                                location =
                                    if(it.markers.isNotEmpty())
                                        LngLatAlt(it.markers[0].longitude, it.markers[0].latitude)
                                    else
                                        LngLatAlt(),
                                description = it.route.description,
                                databaseId = it.route.routeId
                            )
                        },
                        _uiState.value.isSortByName,
                        _uiState.value.isSortAscending,
                        _uiState.value.userLocation
                    )
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
