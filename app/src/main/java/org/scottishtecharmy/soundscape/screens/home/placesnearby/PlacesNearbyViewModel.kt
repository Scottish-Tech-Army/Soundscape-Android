package org.scottishtecharmy.soundscape.screens.home.placesnearby

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
import org.scottishtecharmy.soundscape.geoengine.getTextForFeature
import org.scottishtecharmy.soundscape.geoengine.utils.getDistanceToFeature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import javax.inject.Inject

@HiltViewModel
class PlacesNearbyViewModel
    @Inject
    constructor(
        private val soundscapeServiceConnection: SoundscapeServiceConnection,
        @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PlacesNearbyUiState())
    val uiState: StateFlow<PlacesNearbyUiState> = _uiState

    init {
        // TODO: I'm not sure that we want this to update if the user is moving but using the
        //  screen. Perhaps adding to the list of locations rather than replacing the list
        //  would work, especially if the list remains sorted based on the location?

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
                    val osmSet = mutableSetOf<Any>()
                    val locations = nearbyPlaces.features.filter { feature ->

                        // TODO: This simple deduplication based on OSM id is a workaround. We
                        //  really want polygons to be merged and de-duplicated at the point that
                        //  the GridState is updated.
                        val osmId = feature.foreign?.get("osm_ids")
                        if (osmId != null) {
                            if (osmSet.contains(osmId)) {
                                false
                            } else {
                                osmSet.add(osmId)
                                true
                            }
                        } else {
                            true
                        }
                    }.map { feature ->
                        LocationDescription(
                            name = getTextForFeature(context, feature).text,
                            location = getDistanceToFeature(LngLatAlt(), feature).point
                        )
                    }.sortedBy { _uiState.value.userLocation?.distance(it.location) }

                    _uiState.value = uiState.value.copy(locations = locations)
                } else {
                    _uiState.value = uiState.value.copy(locations = emptyList())
                }
            }
        }
    }
}
