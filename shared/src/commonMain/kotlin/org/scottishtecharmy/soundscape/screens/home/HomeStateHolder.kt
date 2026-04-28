package org.scottishtecharmy.soundscape.screens.home

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.audio.AudioTour
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.services.ServiceConnection
import org.scottishtecharmy.soundscape.services.mediacontrol.VoiceCommandState

/**
 * Shared state-holder backing the home screen on both Android and iOS.
 *
 * Combines service flows (location, heading, beacon, route, voice command) into
 * a single `HomeState` and offers action methods that delegate to the bound service.
 */
@OptIn(FlowPreview::class)
class HomeStateHolder(
    private val connection: ServiceConnection,
    private val audioTour: AudioTour? = null,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private var monitoringJob: Job? = null
    private var streetPreviewJob: Job? = null

    init {
        scope.launch {
            connection.serviceBoundState.collect { bound ->
                if (bound) {
                    startMonitoringLocation()
                    startMonitoringStreetPreview()
                } else {
                    stopMonitoringStreetPreview()
                    stopMonitoringLocation()
                }
            }
        }
    }

    fun dispose() {
        scope.cancel()
    }

    private fun startMonitoringLocation() {
        val service = connection.service ?: return
        monitoringJob?.cancel()
        val job = Job()
        monitoringJob = job

        scope.launch(job) {
            service.locationFlow.collectLatest { value ->
                if (value != null) {
                    _state.update {
                        it.copy(location = LngLatAlt(value.longitude, value.latitude))
                    }
                }
            }
        }
        scope.launch(job) {
            service.orientationFlow.collectLatest { value ->
                if (value != null) {
                    _state.update { it.copy(heading = value.headingDegrees) }
                }
            }
        }
        scope.launch(job) {
            service.beaconFlow.collectLatest { value ->
                _state.update { it.copy(beaconState = value) }
            }
        }
        scope.launch(job) {
            service.currentRouteFlow.collectLatest { value ->
                _state.update { it.copy(currentRouteData = value) }
            }
        }
        scope.launch(job) {
            service.voiceCommandStateFlow.collectLatest { voiceState ->
                _state.update {
                    it.copy(voiceCommandListening = voiceState is VoiceCommandState.Listening)
                }
            }
        }
    }

    private fun stopMonitoringLocation() {
        monitoringJob?.cancel()
        monitoringJob = null
    }

    private fun startMonitoringStreetPreview() {
        val service = connection.service ?: return
        streetPreviewJob?.cancel()
        val job = Job()
        streetPreviewJob = job

        scope.launch(job) {
            service.streetPreviewFlow.collect { value ->
                val previouslyEnabled = _state.value.streetPreviewState.enabled
                _state.update { it.copy(streetPreviewState = value) }

                if (previouslyEnabled != value.enabled) {
                    // Provider changed under us — restart the location-side jobs.
                    stopMonitoringLocation()
                    startMonitoringLocation()
                }
            }
        }
    }

    private fun stopMonitoringStreetPreview() {
        streetPreviewJob?.cancel()
        streetPreviewJob = null
    }

    // --- Actions ---

    fun myLocation() {
        scope.launch { connection.service?.myLocation() }
    }

    fun aheadOfMe() {
        scope.launch { connection.service?.aheadOfMe() }
    }

    fun whatsAroundMe() {
        scope.launch { connection.service?.whatsAroundMe() }
    }

    fun nearbyMarkers() {
        scope.launch { connection.service?.nearbyMarkers() }
    }

    fun streetPreviewGo() {
        scope.launch { connection.service?.streetPreviewGo() }
    }

    fun streetPreviewExit() {
        scope.launch { connection.service?.setStreetPreviewMode(false, null) }
    }

    fun routeSkipPrevious() {
        scope.launch { connection.service?.routeSkipPrevious() }
    }

    fun routeSkipNext() {
        scope.launch { connection.service?.routeSkipNext() }
    }

    fun routeMute() {
        scope.launch { connection.service?.routeMute() }
    }

    fun routeStop() {
        scope.launch {
            connection.service?.routeStop()
            audioTour?.onBeaconStopped()
        }
    }

    fun getLocationDescription(location: LngLatAlt): LocationDescription? {
        return connection.service?.getLocationDescription(location)
    }

    fun onTriggerSearch(text: String) {
        scope.launch {
            _state.update { it.copy(searchInProgress = true) }
            val result = connection.service?.searchResult(text)
            _state.update { it.copy(searchItems = result, searchInProgress = false) }
        }
    }

    fun setRoutesAndMarkersTab(pickRoutes: Boolean) {
        _state.update { it.copy(routesTabSelected = pickRoutes) }
    }
}
