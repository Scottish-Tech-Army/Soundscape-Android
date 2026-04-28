package org.scottishtecharmy.soundscape.screens.markers_routes.screens.markersscreen

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.database.local.dao.RouteDao
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.createCheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.preferences.PreferenceDefaults
import org.scottishtecharmy.soundscape.preferences.PreferenceKeys
import org.scottishtecharmy.soundscape.preferences.PreferencesProvider
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.MarkersAndRoutesUiState
import org.scottishtecharmy.soundscape.services.ServiceConnection

class MarkersStateHolder(
    private val routeDao: RouteDao,
    private val prefs: PreferencesProvider,
    private val connection: ServiceConnection,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _uiState = MutableStateFlow(MarkersAndRoutesUiState(markers = true))
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
            routeDao.getAllMarkersFlow().collect { markers ->
                val locations = markers.map {
                    LocationDescription(
                        name = it.name,
                        description = it.fullAddress,
                        location = LngLatAlt(it.longitude, it.latitude),
                        databaseId = it.markerId,
                    )
                }
                _uiState.value = _uiState.value.copy(
                    entries = sortMarkers(
                        locations,
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

    fun startBeacon(location: LngLatAlt, name: String) {
        connection.service?.startBeacon(location, name)
    }

    fun dispose() {
        scope.cancel()
    }
}

internal fun applyToggleSortByName(
    uiState: MarkersAndRoutesUiState,
    prefs: PreferencesProvider,
): MarkersAndRoutesUiState {
    val sortByName = !uiState.isSortByName
    prefs.putBoolean(PreferenceKeys.MARKERS_SORT_BY_NAME, sortByName)
    return uiState.copy(
        isSortByName = sortByName,
        entries = sortMarkers(
            uiState.entries,
            sortByName,
            uiState.isSortAscending,
            uiState.userLocation,
        ),
    )
}

internal fun applyToggleSortOrder(
    uiState: MarkersAndRoutesUiState,
    prefs: PreferencesProvider,
): MarkersAndRoutesUiState {
    val sortAscending = !uiState.isSortAscending
    prefs.putBoolean(PreferenceKeys.MARKERS_SORT_ASCENDING, sortAscending)
    return uiState.copy(
        isSortAscending = sortAscending,
        entries = sortMarkers(
            uiState.entries,
            uiState.isSortByName,
            sortAscending,
            uiState.userLocation,
        ),
    )
}

fun sortMarkers(
    markers: List<LocationDescription>,
    sortByName: Boolean,
    sortAscending: Boolean,
    userLocation: LngLatAlt?,
): List<LocationDescription> {
    val sortedMarkers = if (sortByName) {
        if (sortAscending) markers.sortedBy { it.name }
        else markers.sortedByDescending { it.name }
    } else {
        val ruler = userLocation?.createCheapRuler() ?: LngLatAlt().createCheapRuler()
        if (sortAscending) {
            markers.sortedBy {
                if (userLocation != null) ruler.distance(userLocation, it.location) else 0.0
            }
        } else {
            markers.sortedByDescending {
                if (userLocation != null) ruler.distance(userLocation, it.location) else 0.0
            }
        }
    }
    for ((index, marker) in sortedMarkers.withIndex()) {
        marker.orderId = index.toLong()
    }
    return sortedMarkers
}
