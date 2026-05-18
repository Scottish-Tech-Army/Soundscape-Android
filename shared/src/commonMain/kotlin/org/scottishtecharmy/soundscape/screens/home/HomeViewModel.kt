package org.scottishtecharmy.soundscape.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.scottishtecharmy.soundscape.audio.AudioTour
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.services.ServiceConnection
import org.scottishtecharmy.soundscape.services.mediacontrol.VoiceCommandState

/**
 * Shared ViewModel backing the home screen on both Android and iOS.
 *
 * Combines service flows (location, heading, beacon, route, voice command) into
 * a single `HomeState` and offers action methods that delegate to the bound service.
 */
@OptIn(FlowPreview::class)
open class HomeViewModel(
    private val connection: ServiceConnection,
    private val audioTour: AudioTour? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    // Child scopes parented to viewModelScope: cancelling them stops just the
    // grouped collectors; viewModel onCleared() also cancels them transitively
    // because their root Job is a child of viewModelScope's Job. SupervisorJob
    // means a failure in one collector doesn't cancel the others in the group.
    private var monitoringScope: CoroutineScope? = null
    private var streetPreviewScope: CoroutineScope? = null

    init {
        viewModelScope.launch {
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

    private fun childScope(): CoroutineScope =
        CoroutineScope(viewModelScope.coroutineContext + SupervisorJob(viewModelScope.coroutineContext[Job]))

    private fun startMonitoringLocation() {
        val service = connection.service ?: return
        stopMonitoringLocation()
        val scope = childScope().also { monitoringScope = it }

        scope.launch {
            service.locationFlow.collectLatest { value ->
                if (value != null) {
                    _state.update {
                        it.copy(location = LngLatAlt(value.longitude, value.latitude))
                    }
                }
            }
        }
        scope.launch {
            // Prefer the external head-tracker heading when one is active;
            // fall back to the phone compass when no head tracker is paired.
            combine(
                service.orientationFlow,
                service.headHeadingFlow,
            ) { orientation, head ->
                head?.degrees?.toFloat() ?: orientation?.headingDegrees
            }.collectLatest { heading ->
                if (heading != null) {
                    _state.update { it.copy(heading = heading) }
                }
            }
        }
        scope.launch {
            service.beaconFlow.collectLatest { value ->
                _state.update { it.copy(beaconState = value) }
            }
        }
        scope.launch {
            service.currentRouteFlow.collectLatest { value ->
                _state.update { it.copy(currentRouteData = value) }
            }
        }
        scope.launch {
            service.voiceCommandStateFlow.collectLatest { voiceState ->
                _state.update {
                    it.copy(voiceCommandListening = voiceState is VoiceCommandState.Listening)
                }
            }
        }
    }

    private fun stopMonitoringLocation() {
        monitoringScope?.cancel()
        monitoringScope = null
    }

    private fun startMonitoringStreetPreview() {
        val service = connection.service ?: return
        stopMonitoringStreetPreview()
        val scope = childScope().also { streetPreviewScope = it }

        scope.launch {
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
        streetPreviewScope?.cancel()
        streetPreviewScope = null
    }

    // --- Actions ---
    //
    // viewModelScope defaults to Dispatchers.Main.immediate. The service action
    // methods (myLocation/aheadOfMe/whatsAroundMe/nearbyMarkers/search/etc.)
    // are synchronous and may walk the GeoEngine, so we launch them on
    // Dispatchers.Default to keep the UI thread responsive.

    fun myLocation() {
        viewModelScope.launch(Dispatchers.Default) { connection.service?.myLocation() }
    }

    fun aheadOfMe() {
        viewModelScope.launch(Dispatchers.Default) { connection.service?.aheadOfMe() }
    }

    fun whatsAroundMe() {
        viewModelScope.launch(Dispatchers.Default) { connection.service?.whatsAroundMe() }
    }

    fun nearbyMarkers() {
        viewModelScope.launch(Dispatchers.Default) { connection.service?.nearbyMarkers() }
    }

    fun streetPreviewGo() {
        viewModelScope.launch(Dispatchers.Default) { connection.service?.streetPreviewGo() }
    }

    fun streetPreviewExit() {
        viewModelScope.launch(Dispatchers.Default) {
            connection.service?.setStreetPreviewMode(false, null)
        }
    }

    fun enableStreetPreview(location: LngLatAlt) {
        viewModelScope.launch(Dispatchers.Default) {
            connection.service?.setStreetPreviewMode(true, location)
        }
    }

    fun routeSkipPrevious() {
        viewModelScope.launch(Dispatchers.Default) { connection.service?.routeSkipPrevious() }
    }

    fun routeSkipNext() {
        viewModelScope.launch(Dispatchers.Default) { connection.service?.routeSkipNext() }
    }

    fun routeMute() {
        viewModelScope.launch(Dispatchers.Default) { connection.service?.routeMute() }
    }

    fun routeStop() {
        viewModelScope.launch(Dispatchers.Default) {
            connection.service?.routeStop()
            audioTour?.onBeaconStopped()
        }
    }

    fun getLocationDescription(location: LngLatAlt): LocationDescription? {
        return connection.service?.getLocationDescription(location)
    }

    fun onTriggerSearch(text: String) {
        viewModelScope.launch {
            _state.update { it.copy(searchInProgress = true) }
            val result = withContext(Dispatchers.Default) {
                connection.service?.searchResult(text)
            }
            _state.update { it.copy(searchItems = result, searchInProgress = false) }
        }
    }

    fun setRoutesAndMarkersTab(pickRoutes: Boolean) {
        _state.update { it.copy(routesTabSelected = pickRoutes) }
    }
}
