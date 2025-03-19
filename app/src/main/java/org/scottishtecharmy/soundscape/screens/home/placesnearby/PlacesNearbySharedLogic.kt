package org.scottishtecharmy.soundscape.screens.home.placesnearby

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.scottishtecharmy.soundscape.SoundscapeServiceConnection
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

class PlacesNearbySharedLogic(
    private val soundscapeServiceConnection: SoundscapeServiceConnection,
    private val viewModelScope: CoroutineScope
) {
    internal val _uiState = MutableStateFlow(PlacesNearbyUiState())
    val uiState: StateFlow<PlacesNearbyUiState> = _uiState

    init {
        // Collect the flow for the service connection and use it to start and stop collecting the
        // Location and GridState flows
        viewModelScope.launch {
            soundscapeServiceConnection.serviceBoundState.collect { serviceBoundState ->
                if(serviceBoundState) {
                    startMonitoringFlows()
                } else {
                    stopMonitoringFlows()
                }
            }
        }
    }
    private var monitorJob = Job()
    private fun stopMonitoringFlows() {
        monitorJob.cancel()
    }

    data class LocationAndGridState(val location: LngLatAlt?, val gridState: GridState?)
    private fun startMonitoringFlows() {
        monitorJob = Job()
        viewModelScope.launch(monitorJob) {

            val gridFlow = soundscapeServiceConnection.getGridStateFlow()!!
            val locationFlow = soundscapeServiceConnection.getLocationFlow()!!
            combine(gridFlow, locationFlow) { gridState, location ->
                if(location != null)
                    LocationAndGridState( LngLatAlt(location.longitude, location.latitude), gridState)
                else
                    LocationAndGridState( null, gridState)

            }.collect { locationAndGrid ->
                if (locationAndGrid.location != null) {
                    _uiState.update {
                        it.copy(
                            userLocation = locationAndGrid.location
                        )
                    }
                }
                if (locationAndGrid.gridState != null) {
                    val nearbyPlaces = runBlocking {
                        withContext(locationAndGrid.gridState.treeContext) {
                            locationAndGrid.gridState.getFeatureCollection(
                                TreeId.POIS
                            )
                        }
                    }
                    val nearbyIntersections = runBlocking {
                        withContext(locationAndGrid.gridState.treeContext) {
                            locationAndGrid.gridState.getFeatureCollection(
                                TreeId.INTERSECTIONS
                            )
                        }
                    }

                    _uiState.value = uiState.value.copy(
                        nearbyPlaces = nearbyPlaces,
                        nearbyIntersections = nearbyIntersections
                    )
                } else {
                    _uiState.value = uiState.value.copy(
                        nearbyPlaces = FeatureCollection(),
                        nearbyIntersections = FeatureCollection()
                    )
                }
            }
        }
    }
}
