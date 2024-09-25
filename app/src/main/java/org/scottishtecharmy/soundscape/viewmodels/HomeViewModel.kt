// The code for Marker manipulation in maplibre has been moved into an annotations plugin.
// However, this doesn't appear to be supported in Kotlin yet. There's talk of un-deprecating those
// functions in the next release if support isn't added. In the meantime we use the deprecated
// functions and suppress the warnings here.
@file:Suppress("DEPRECATION")

package org.scottishtecharmy.soundscape.viewmodels

import android.content.Context
import android.content.Intent
import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.layers.PropertyFactory
import org.scottishtecharmy.soundscape.SoundscapeServiceConnection
import java.net.URLEncoder
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val soundscapeServiceConnection: SoundscapeServiceConnection
) : ViewModel() {
    private var serviceConnection: SoundscapeServiceConnection? = null // TODO this should be improved to use soundscapeServiceConnection
    private val _heading: MutableStateFlow<Float> = MutableStateFlow(0.0f)
    val heading: StateFlow<Float> = _heading.asStateFlow()
    private val _location: MutableStateFlow<Location?> = MutableStateFlow(null)
    val location: StateFlow<Location?> = _location.asStateFlow()

    private var beaconLocation: LatLng? = null

    private var beaconLocationMarker: Marker? = null

    private var mapLibreMap: MapLibreMap? = null

    init {
        serviceConnection = soundscapeServiceConnection
        viewModelScope.launch {
            soundscapeServiceConnection.serviceBoundState.collect {
                Log.d(TAG, "serviceBoundState $it")
                if (it) {
                    // The service has started, so start monitoring the location and heading
                    startMonitoringLocation()
                } else {
                    // The service has gone away so remove the current location marker
                    _location.value = null
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
            && (newLocation.latitude != _location.value?.latitude || newLocation.longitude != _location.value?.longitude)
        ) {
            _location.value = newLocation
            Log.d(TAG, "lastLocation updated to $newLocation")
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

    fun myLocation(){
        //Log.d(TAG, "myLocation() triggered")
        viewModelScope.launch {
            serviceConnection?.soundscapeService?.myLocation()
        }
    }

    fun aheadOfMe(){
        //Log.d(TAG, "myLocation() triggered")
        viewModelScope.launch {
            serviceConnection?.soundscapeService?.aheadOfMe()
        }
    }

    fun whatsAroundMe(){
        //Log.d(TAG, "myLocation() triggered")
        viewModelScope.launch {
            serviceConnection?.soundscapeService?.whatsAroundMe()
        }
    }


    fun shareLocation(context: Context) {
        // Share the current location using standard Android sharing mechanism. It's shared as a
        //
        //  soundscape://latitude,longitude
        //
        // URI, with the , encoded. This shows up in Slack as a clickable link which is the main
        // usefulness for now
        val location = serviceConnection?.getLocationFlow()?.value
        if(location != null) {
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TITLE, "Problem location")
                val latitude = location.latitude
                val longitude = location.longitude
                val uriData: String = URLEncoder.encode("$latitude,$longitude", Charsets.UTF_8.name())
                putExtra(Intent.EXTRA_TEXT, "soundscape://$uriData")
                type = "text/plain"

            }

            val shareIntent = Intent.createChooser(sendIntent, null)
            context.startActivity(shareIntent)
        }
    }


    companion object {
        private const val TAG = "HomeViewModel"
    }
}
