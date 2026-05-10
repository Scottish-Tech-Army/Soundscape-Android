package org.scottishtecharmy.soundscape.screens.home.placesnearby

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.scottishtecharmy.soundscape.SoundscapeServiceConnection
import org.scottishtecharmy.soundscape.components.LocationSource
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.getTextForFeature
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.utils.featureHasEntrances
import org.scottishtecharmy.soundscape.geoengine.utils.featureIsInFilterGroup
import org.scottishtecharmy.soundscape.geoengine.utils.getDistanceToFeature
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.utils.deferredToLocationDescription

class PlacesNearbySharedLogic(
    private val soundscapeServiceConnection: SoundscapeServiceConnection,
    private val viewModelScope: CoroutineScope
) {
    private val internalUiState = MutableStateFlow(PlacesNearbyUiState())
    val uiState: StateFlow<PlacesNearbyUiState> = internalUiState

    fun startBeacon(location: LngLatAlt, name: String) {
        soundscapeServiceConnection.startBeacon(location, name)
    }

    init {
        // Collect the flow for the service connection and use it to start and stop collecting the
        // Location and GridState flows
        viewModelScope.launch {
            soundscapeServiceConnection.serviceBoundState.collect { serviceBoundState ->
                if (serviceBoundState) {
                    startMonitoringFlows()
                } else {
                    stopMonitoringFlows()
                }
            }
        }
    }

    private var monitorJob: Job? = null
    private fun stopMonitoringFlows() {
        monitorJob?.cancel()
        monitorJob = null
    }

    data class LocationAndGridState(val location: LngLatAlt?, val gridState: GridState?)

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun startMonitoringFlows() {
        monitorJob = Job()
        viewModelScope.launch(monitorJob!!) {

            val gridFlow = soundscapeServiceConnection.getGridStateFlow()!!
            val locationFlow = soundscapeServiceConnection.getLocationFlow()!!
            combine(gridFlow, locationFlow) { gridState, location ->
                if (location != null)
                    LocationAndGridState(
                        LngLatAlt(location.longitude, location.latitude),
                        gridState
                    )
                else
                    LocationAndGridState(null, gridState)

            }.collect { locationAndGrid ->
                if (locationAndGrid.location != null) {
                    internalUiState.update {
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

                    internalUiState.value = internalUiState.value.copy(
                        nearbyPlaces = nearbyPlaces,
                        nearbyIntersections = nearbyIntersections
                    )
                } else {
                    internalUiState.value = internalUiState.value.copy(
                        nearbyPlaces = FeatureCollection(),
                        nearbyIntersections = FeatureCollection()
                    )
                }
            }
        }
    }

    fun navigateToLevel(level: Int) {
        internalUiState.value = internalUiState.value.copy(level = level)
    }

    fun resetLevel() {
        internalUiState.value = internalUiState.value.copy(level = 0)
    }

    fun applyFilter(level: Int, filter: String, title: String) {
        internalUiState.value =
            internalUiState.value.copy(level = level, filter = filter, title = title)
    }

    fun locationSelected(markerDescription: LocationDescription) {
        internalUiState.value = internalUiState.value.copy(
            markerDescription = markerDescription
        )
    }

    fun resetMarkerDescription() {
        internalUiState.value = internalUiState.value.copy(
            markerDescription = null
        )
    }
}

fun filterLocations(
    userLocation: LngLatAlt?,
    filter: String,
    nearbyIntersections: FeatureCollection,
    nearbyPlaces: FeatureCollection,
    context: Context
): List<LocationDescription> {
    val location = userLocation ?: LngLatAlt()
    val ruler = CheapRuler(location.latitude)
    return if (filter == "intersections") {
        nearbyIntersections.features.filter { feature ->
            // Filter out un-named intersections
            (feature as MvtFeature).name.toString().isNotEmpty()
        }.map { feature ->
            LocationDescription(
                name = (feature as MvtFeature).name.toString(),
                location = getDistanceToFeature(location, feature, ruler).point
            )
        }.sortedBy {
            userLocation?.let { location ->
                ruler.distance(location, it.location)
            } ?: 0.0
        }
    } else {
        nearbyPlaces.features.filter { feature ->
            // Filter based on any folder selected and filter out POIs with entrances
            !featureHasEntrances(feature) &&
                    featureIsInFilterGroup(feature, filter) &&
                    getTextForFeature(context, feature as MvtFeature).text.isNotEmpty()
        }.map { feature ->
            feature.deferredToLocationDescription(
                LocationSource.OfflineGeocoder,
                getDistanceToFeature(location, feature, ruler).point,
                getTextForFeature(context, feature as MvtFeature)
            )
        }.sortedBy {
            userLocation?.let { location ->
                ruler.distance(location, it.location)
            } ?: 0.0
        }
    }
}