package org.scottishtecharmy.soundscape.screens.markers_routes.screens.routesscreen

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.markersscreen.applyToggleSortByName
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.markersscreen.applyToggleSortOrder
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.markersscreen.sortMarkers
import org.scottishtecharmy.soundscape.services.ServiceConnection

class RoutesStateHolder(
    private val routeDao: RouteDao,
    private val prefs: PreferencesProvider,
    private val connection: ServiceConnection,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _uiState = MutableStateFlow(MarkersAndRoutesUiState(markers = false))
    val uiState: StateFlow<MarkersAndRoutesUiState> = _uiState

    init {
        _uiState.value = _uiState.value.copy(
            isSortByName = prefs.getBoolean(
                PreferenceKeys.MARKERS_SORT_BY_NAME,
                PreferenceDefaults.MARKERS_SORT_BY_NAME,
            ),
            isSortAscending = prefs.getBoolean(
                PreferenceKeys.MARKERS_SORT_ASCENDING,
                PreferenceDefaults.MARKERS_SORT_ASCENDING,
            ),
        )

        scope.launch {
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

    fun toggleSortByName() {
        _uiState.value = applyToggleSortByName(_uiState.value, prefs)
    }

    fun toggleSortOrder() {
        _uiState.value = applyToggleSortOrder(_uiState.value, prefs)
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun startRoute(routeId: Long) {
        connection.service?.routeStartById(routeId)
    }

    fun dispose() {
        scope.cancel()
    }
}
