// The code for Marker manipulation in maplibre has been moved into an annotations plugin.
// However, this doesn't appear to be supported in Kotlin yet. There's talk of un-deprecating those
// functions in the next release if support isn't added. In the meantime we use the deprecated
// functions and suppress the warnings here.
@file:Suppress("DEPRECATION")

package org.scottishtecharmy.soundscape.viewmodels

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.layers.PropertyFactory
import org.scottishtecharmy.soundscape.SoundscapeServiceConnection
import javax.inject.Inject

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        @ApplicationContext context: Context, // TODO should not be used at the VM layer
        private val soundscapeServiceConnection: SoundscapeServiceConnection,
    ) : ViewModel() {
        private var serviceConnection: SoundscapeServiceConnection? = null
        private var iconFactory: IconFactory
        private val _heading: MutableStateFlow<Float> = MutableStateFlow<Float>(0.0f)
        val heading: StateFlow<Float> = _heading.asStateFlow()
        private val _location: MutableStateFlow<Location?> = MutableStateFlow(null)
        val location: StateFlow<Location?> = _location.asStateFlow()

        private var mapCentered: Boolean = false // TODO see how it is used
        private var beaconLocation: LatLng? = null

        private var currentLocationMarker: Marker? = null
        private var beaconLocationMarker: Marker? = null

        private var mapLibreMap: MapLibreMap? = null

        init {
            serviceConnection = soundscapeServiceConnection
            iconFactory = IconFactory.getInstance(context)
            viewModelScope.launch {
                soundscapeServiceConnection.serviceBoundState.collect {
                    Log.d(TAG, "serviceBoundState $it")
                    if (it) {
                        // The service has started, so start monitoring the location and heading
                        startMonitoringLocation()
                    } else {
                        // The service has gone away so remove the current location marker
                        if (currentLocationMarker != null) {
                            mapLibreMap?.removeMarker(currentLocationMarker!!)
                            currentLocationMarker = null
                        }
                        // Reset map view variables so that the map re-centers when the service comes back
                        mapCentered = false
                    }
                }
            }
        }

        private fun updateBeaconLocation() {
            if (beaconLocationMarker != null) {
                beaconLocationMarker?.position = beaconLocation
            } else {
                val markerOptions =
                    MarkerOptions()
                        .position(beaconLocation)
                beaconLocationMarker = mapLibreMap?.addMarker(markerOptions)
            }
        }

        private fun startMonitoringLocation() {
            Log.d(TAG, "ViewModel startMonitoringLocation")
            viewModelScope.launch {
                // Observe location updates from the service
                serviceConnection?.getLocationFlow()?.collectLatest { value ->
                    if (value != null) {
                        Log.d(TAG, "Location $value")
                        updateLocationOnMap(value)
                    }
                }
            }

            viewModelScope.launch {
                // Observe orientation updates from the service
                serviceConnection?.getOrientationFlow()?.collectLatest { value ->
                    if (value != null) {
                        _heading.value = value.headingDegrees
                    }
                }
            }
            viewModelScope.launch {
                // Observe beacon location update from the service so we can show it on the map
                serviceConnection?.getBeaconFlow()?.collectLatest { value ->
                    if (value != null) {
                        // Use MarkerOptions and addMarker() to add a new marker in map
                        beaconLocation = LatLng(value.latitude, value.longitude)
                        updateBeaconLocation()
                    } else {
                        if (beaconLocationMarker != null) {
                            mapLibreMap?.removeMarker(beaconLocationMarker!!)
                            beaconLocationMarker = null
                        }
                    }
                }
            }
        }

        private fun updateLocationOnMap(newLocation: Location) {
            if (newLocation.hasAccuracy()
                && (newLocation.accuracy < 250.0)
                ) {
                _location.value = newLocation
                Log.d(TAG, "lastLocation updated to $newLocation")
            }
            if (!mapCentered && (mapLibreMap != null) && (_location.value != null)) {
                // If the map has already been created and it's not yet been centered, and we've received
                // a location with reasonable accuracy then center it
                mapCentered = true // TODO send this as a observable to force map to be centered
                _location.value = newLocation
            }
        }

        fun createBeacon(latitudeLongitude: LatLng) {
            soundscapeServiceConnection.soundscapeService?.createBeacon(
                latitudeLongitude.latitude,
                latitudeLongitude.longitude,
            )
        }

        fun onMarkerClick(marker: Marker): Boolean {
            if (marker == beaconLocationMarker) {
                soundscapeServiceConnection.soundscapeService?.destroyBeacon()
                return true
            }
            return false
        }

        // This is a demo function to show how to dynamically alter the map based on user input.
        // The result of this function is that Food POIs have their icons toggled between enlarged
        // and regular sized.
        private var highlightedPointsOfInterest: Boolean = false

        fun highlightPointsOfInterest() {
            highlightedPointsOfInterest = highlightedPointsOfInterest.xor(true)
            mapLibreMap?.getStyle {
                val foodLayer = it.getLayer("Food")
                var highlightSize = 1F
                if (highlightedPointsOfInterest) {
                    highlightSize = 2F
                }

                foodLayer?.setProperties(PropertyFactory.iconSize(highlightSize))
            }
        }

        companion object {
            private const val TAG = "HomeViewModel"
        }
    }
