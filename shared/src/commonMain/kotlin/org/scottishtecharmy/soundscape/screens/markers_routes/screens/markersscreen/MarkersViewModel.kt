package org.scottishtecharmy.soundscape.screens.markers_routes.screens.markersscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

open class MarkersViewModel(
    private val routeDao: RouteDao,
    private val prefs: PreferencesProvider,
    private val connection: ServiceConnection,
) : ViewModel() {

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

        viewModelScope.launch {
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

    fun updateUserLocation(location: LngLatAlt?) {
        _uiState.value = applyUserLocation(_uiState.value, location)
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun startBeacon(location: LngLatAlt, name: String) {
        connection.service?.startBeacon(location, name)
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

/**
 * Record the user's location so that sorting by distance has something to measure from. The list
 * is only re-sorted when the first location arrives - re-sorting on every location update would
 * shuffle the list under the user's finger (or TalkBack focus) as they walk. Later sorts (toggles,
 * database changes) pick up the latest location.
 */
internal fun applyUserLocation(
    uiState: MarkersAndRoutesUiState,
    location: LngLatAlt?,
): MarkersAndRoutesUiState {
    if (location == null || location == uiState.userLocation) return uiState
    val firstLocation = uiState.userLocation == null
    return uiState.copy(
        userLocation = location,
        entries = if (firstLocation && !uiState.isSortByName) {
            sortMarkers(uiState.entries, false, uiState.isSortAscending, location)
        } else {
            uiState.entries
        },
    )
}

fun sortMarkers(
    markers: List<LocationDescription>,
    sortByName: Boolean,
    sortAscending: Boolean,
    userLocation: LngLatAlt?,
): List<LocationDescription> {
    val sortedMarkers = if (sortByName) {
        val byName = compareBy(String.CASE_INSENSITIVE_ORDER) { marker: LocationDescription -> marker.name }
        if (sortAscending) markers.sortedWith(byName)
        else markers.sortedWith(byName.reversed())
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
