package org.scottishtecharmy.soundscape.screens.markers_routes.screens.routesscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.database.local.dao.RouteDao
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.preferences.PreferenceDefaults
import org.scottishtecharmy.soundscape.preferences.PreferenceKeys
import org.scottishtecharmy.soundscape.preferences.PreferencesProvider
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.MarkersAndRoutesUiState
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.markersscreen.applyCycleSort
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.markersscreen.applyUserLocation
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.markersscreen.sortMarkers
import org.scottishtecharmy.soundscape.services.ServiceConnection

open class RoutesViewModel(
    private val routeDao: RouteDao,
    private val prefs: PreferencesProvider,
    private val connection: ServiceConnection,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MarkersAndRoutesUiState(markers = false))
    val uiState: StateFlow<MarkersAndRoutesUiState> = _uiState

    init {
        // Routes used to share the markers sort order, so until the user sorts routes for
        // themselves, start from whatever order the markers list is in.
        _uiState.value = _uiState.value.copy(
            isSortByName = prefs.getBoolean(
                PreferenceKeys.ROUTES_SORT_BY_NAME,
                prefs.getBoolean(
                    PreferenceKeys.MARKERS_SORT_BY_NAME,
                    PreferenceDefaults.MARKERS_SORT_BY_NAME,
                ),
            ),
            isSortAscending = prefs.getBoolean(
                PreferenceKeys.ROUTES_SORT_ASCENDING,
                prefs.getBoolean(
                    PreferenceKeys.MARKERS_SORT_ASCENDING,
                    PreferenceDefaults.MARKERS_SORT_ASCENDING,
                ),
            ),
        )

        viewModelScope.launch {
            routeDao.getAllRoutesWithMarkersFlow().collect { routes ->
                _uiState.value = _uiState.value.copy(
                    entries = sortMarkers(
                        routes.map {
                            LocationDescription(
                                name = it.route.name,
                                location = if (it.markers.isNotEmpty()) {
                                    LngLatAlt(it.markers[0].longitude, it.markers[0].latitude)
                                } else {
                                    LngLatAlt()
                                },
                                description = it.route.description,
                                databaseId = it.route.routeId,
                            )
                        },
                        _uiState.value.isSortByName,
                        _uiState.value.isSortAscending,
                        _uiState.value.userLocation,
                    )
                )
            }
        }
    }

    fun cycleSort() {
        _uiState.value = applyCycleSort(
            _uiState.value,
            prefs,
            PreferenceKeys.ROUTES_SORT_BY_NAME,
            PreferenceKeys.ROUTES_SORT_ASCENDING,
        )
    }

    fun updateUserLocation(location: LngLatAlt?) {
        _uiState.value = applyUserLocation(_uiState.value, location)
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun startRoute(routeId: Long) {
        connection.service?.routeStartById(routeId)
    }
}
