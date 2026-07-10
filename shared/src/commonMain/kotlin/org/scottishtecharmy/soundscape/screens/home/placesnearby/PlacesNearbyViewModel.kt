package org.scottishtecharmy.soundscape.screens.home.placesnearby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.scottishtecharmy.soundscape.audio.AudioTour
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.services.ServiceConnection

/**
 * Shared ViewModel backing the PlacesNearby screen on both Android and iOS.
 *
 * Owns nearby-places UI state and folder navigation. Listens for service binding
 * and starts/stops monitoring location + grid flows accordingly.
 */
open class PlacesNearbyViewModel(
    private val connection: ServiceConnection,
    audioTour: AudioTour? = null,
) : ViewModel() {

    val internalUiState = MutableStateFlow(PlacesNearbyUiState())
    val uiState: StateFlow<PlacesNearbyUiState> = internalUiState

    private var monitorJob: Job? = null

    init {
        audioTour?.onNavigatedToPlacesNearby()

        viewModelScope.launch {
            connection.serviceBoundState.collect { bound ->
                if (bound) startMonitoring() else stopMonitoring()
            }
        }
    }

    fun onClickBack() {
        internalUiState.value = internalUiState.value.copy(level = 0, filter = "", title = "")
    }

    fun onClickFolder(filter: String, title: String) {
        internalUiState.value =
            internalUiState.value.copy(level = 1, filter = filter, title = title)
    }

    fun startBeacon(location: LngLatAlt, name: String) {
        connection.service?.startBeacon(location, name)
    }

    /**
     * Manually cancel the viewModelScope. Use only when this holder is embedded
     * inside another ViewModel (e.g. AddAndEditRouteViewModel.logic) and so
     * the framework would never call onCleared() on it directly.
     */
    fun dispose() {
        viewModelScope.cancel()
    }

    private data class LocationAndGridState(val location: LngLatAlt?, val gridState: GridState?)

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun startMonitoring() {
        monitorJob?.cancel()
        val job = Job()
        monitorJob = job
        val service = connection.service ?: return
        viewModelScope.launch(job) {
            combine(service.gridStateFlow, service.locationFlow) { gridState, location ->
                LocationAndGridState(
                    location = location?.let { LngLatAlt(it.longitude, it.latitude) },
                    gridState = gridState,
                )
            }.collect { locationAndGrid ->
                if (locationAndGrid.location != null) {
                    internalUiState.update { it.copy(userLocation = locationAndGrid.location) }
                }
                if (locationAndGrid.gridState != null) {
                    val (pois, intersections) = withContext(locationAndGrid.gridState.treeContext) {
                        val p = runCatching {
                            locationAndGrid.gridState.getFeatureCollection(TreeId.POIS)
                        }.getOrElse {
                            println("PlacesNearbyViewModel: POIS lookup failed: ${it.message}")
                            FeatureCollection()
                        }
                        val i = runCatching {
                            locationAndGrid.gridState.getFeatureCollection(TreeId.INTERSECTIONS)
                        }.getOrElse {
                            println("PlacesNearbyViewModel: INTERSECTIONS lookup failed: ${it.message}")
                            FeatureCollection()
                        }
                        p to i
                    }
                    internalUiState.value = internalUiState.value.copy(
                        nearbyPlaces = pois,
                        nearbyIntersections = intersections,
                    )
                } else {
                    internalUiState.value = internalUiState.value.copy(
                        nearbyPlaces = FeatureCollection(),
                        nearbyIntersections = FeatureCollection(),
                    )
                }
            }
        }
    }

    private fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
    }
}
