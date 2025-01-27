// The code for Marker manipulation in maplibre has been moved into an annotations plugin.
// However, this doesn't appear to be supported in Kotlin yet. There's talk of un-deprecating those
// functions in the next release if support isn't added. In the meantime we use the deprecated
// functions and suppress the warnings here.
@file:Suppress("DEPRECATION")

package org.scottishtecharmy.soundscape.viewmodels.home

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.maplibre.android.annotations.Marker
import org.maplibre.android.geometry.LatLng
import org.scottishtecharmy.soundscape.SoundscapeServiceConnection
import org.scottishtecharmy.soundscape.utils.blankOrEmpty
import org.scottishtecharmy.soundscape.utils.toLocationDescriptions
import java.net.URLEncoder
import javax.inject.Inject

@HiltViewModel
@OptIn(FlowPreview::class)
class HomeViewModel
    @Inject
    constructor(
        private val soundscapeServiceConnection: SoundscapeServiceConnection,
    ) : ViewModel() {
        private val _state: MutableStateFlow<HomeState> = MutableStateFlow(HomeState())
        val state: StateFlow<HomeState> = _state.asStateFlow()
        private val _searchText: MutableStateFlow<String> = MutableStateFlow("")
        val searchText: StateFlow<String> = _searchText.asStateFlow()

        private var job = Job()
        private var spJob = Job()

        init {
            handleMonitoring()
            fetchSearchResult()
        }

        private fun handleMonitoring() {
            viewModelScope.launch {
                soundscapeServiceConnection.serviceBoundState.collect { serviceBoundState ->
                    Log.d(TAG, "serviceBoundState $serviceBoundState")
                    if (serviceBoundState) {
                        // The service has started, so start monitoring the location and heading
                        startMonitoringLocation()
                        // And start monitoring the street preview mode
                        startMonitoringStreetPreviewMode()
                    } else {
                        // The service has gone away so remove the current location marker
                        _state.update { it.copy(location = null) }
                        stopMonitoringStreetPreviewMode()
                        stopMonitoringLocation()
                    }
                }
            }
        }

        override fun onCleared() {
            super.onCleared()
            stopMonitoringStreetPreviewMode()
            stopMonitoringLocation()
        }

        /**
         * startMonitoringLocation launches monitoring of the location and orientation providers. These
         * can change e.g. when switching to and from StreetPreview mode, so they are launched in a job.
         * That job is cancelled when the StreetPreview mode changes and the monitoring restarted.
         */
        private fun startMonitoringLocation() {
            Log.d(TAG, "ViewModel startMonitoringLocation")
            job = Job()
            viewModelScope.launch(job) {
                // Observe location updates from the service
                soundscapeServiceConnection.getLocationFlow()?.collectLatest { value ->
                    if (value != null) {
                        Log.d(TAG, "Location $value")
                        _state.update { it.copy(location = value) }
                    }
                }
            }
            viewModelScope.launch(job) {
                // Observe orientation updates from the service
                soundscapeServiceConnection.getOrientationFlow()?.collectLatest { value ->
                    if (value != null) {
                        _state.update { it.copy(heading = value.headingDegrees) }
                    }
                }
            }
            viewModelScope.launch(job) {
                // Observe beacon location update from the service so we can show it on the map
                soundscapeServiceConnection.getBeaconFlow()?.collectLatest { value ->
                    Log.d(TAG, "beacon collected $value")
                    if (value != null) {
                        _state.update {
                            it.copy(
                                beaconLocation =
                                    LatLng(
                                        value.latitude,
                                        value.longitude,
                                    ),
                            )
                        }
                    } else {
                        _state.update { it.copy(beaconLocation = null) }
                    }
                }
            }
        }

        private fun stopMonitoringLocation() {
            Log.d(TAG, "stopMonitoringLocation")
            job.cancel()
        }

        /**
         * startMonitoringStreetPreviewMode launches a job to monitor the state of street preview mode.
         * When the mode from the service changes then the local flow for the UI is updated and the
         * location and orientation monitoring is turned off and on again so as to use the new providers.
         */
        private fun startMonitoringStreetPreviewMode() {
            Log.d(TAG, "startMonitoringStreetPreviewMode")
            spJob = Job()
            viewModelScope.launch(spJob) {
                // Observe street preview mode from the service so we can update state
                soundscapeServiceConnection.getStreetPreviewModeFlow()?.collect { value ->
                    Log.d(TAG, "Street Preview Mode: $value")
                    _state.update { it.copy(streetPreviewMode = value) }

                    // Restart location monitoring for new provider
                    stopMonitoringLocation()
                    startMonitoringLocation()
                }
            }
        }

        private fun stopMonitoringStreetPreviewMode() {
            Log.d(TAG, "stopMonitoringStreetPreviewMode")
            spJob.cancel()
        }

        fun onMarkerClick(marker: Marker): Boolean {
            Log.d(TAG, "marker click")

            if (marker.position == _state.value.beaconLocation) {
                soundscapeServiceConnection.soundscapeService?.destroyBeacon()
                _state.update { it.copy(beaconLocation = null) }

                return true
            }
            return false
        }

        fun myLocation() {
            // Log.d(TAG, "myLocation() triggered")
            viewModelScope.launch {
                soundscapeServiceConnection.soundscapeService?.myLocation()
            }
        }

        fun aheadOfMe() {
            // Log.d(TAG, "myLocation() triggered")
            viewModelScope.launch {
                soundscapeServiceConnection.soundscapeService?.aheadOfMe()
            }
        }

        fun whatsAroundMe() {
            // Log.d(TAG, "myLocation() triggered")
            viewModelScope.launch {
                soundscapeServiceConnection.soundscapeService?.whatsAroundMe()
            }
        }

        fun shareLocation(context: Context) {
            // Share the current location using standard Android sharing mechanism. It's shared as a
            //
            //  soundscape://latitude,longitude
            //
            // URI, with the , encoded. This shows up in Slack as a clickable link which is the main
            // usefulness for now
            val location = soundscapeServiceConnection.getLocationFlow()?.value
            if (location != null) {
                val sendIntent: Intent =
                    Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TITLE, "Problem location")
                        val latitude = location.latitude
                        val longitude = location.longitude
                        val uriData: String =
                            URLEncoder.encode("$latitude,$longitude", Charsets.UTF_8.name())
                        putExtra(Intent.EXTRA_TEXT, "soundscape://$uriData")
                        type = "text/plain"
                    }

                val shareIntent = Intent.createChooser(sendIntent, null)
                context.startActivity(shareIntent)
            }
        }

        fun onSearchTextChange(text: String) {
            _searchText.value = text
        }

        private fun fetchSearchResult() {
            viewModelScope.launch {
                _searchText
                    .debounce(500)
                    .distinctUntilChanged()
                    .collectLatest { searchText ->
                        if (searchText.blankOrEmpty()) {
                            _state.update { it.copy(searchItems = emptyList()) }
                        } else {
                            val result =
                                soundscapeServiceConnection.soundscapeService?.searchResult(searchText)

                            _state.update {
                                it.copy(
                                    searchItems =
                                        result?.toLocationDescriptions(
                                            currentLocationLatitude = state.value.location?.latitude ?: 0.0,
                                            currentLocationLongitude =
                                                state.value.location?.longitude
                                                    ?: 0.0,
                                        ),
                                )
                            }
                        }
                    }
            }
        }

        fun onToggleSearch() {
            _state.update { it.copy(isSearching = !it.isSearching) }

            if (!state.value.isSearching) {
                onSearchTextChange("")
            }
        }

        fun setRoutesAndMarkersTab(pickRoutes: Boolean) {
            _state.update { it.copy(routesTabSelected = pickRoutes) }
        }

    companion object {
            private const val TAG = "HomeViewModel"
        }
    }
