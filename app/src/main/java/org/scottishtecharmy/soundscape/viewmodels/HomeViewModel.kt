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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.layers.PropertyFactory
import org.scottishtecharmy.soundscape.BuildConfig
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.SoundscapeServiceConnection
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(@ApplicationContext context: Context, private val soundscapeServiceConnection : SoundscapeServiceConnection): ViewModel(),
    MapLibreMap.OnMarkerClickListener {

    private var serviceConnection : SoundscapeServiceConnection? = null
    private var iconFactory : IconFactory
    var latitude : Double = 0.0
    var longitude : Double = 0.0
    var heading : Float = 0.0F
    private var initialLocation : Location? = null
    private var mapCentered : Boolean = false
    private var beaconLocation : LatLng? = null

    private var currentLocationMarker : Marker? = null
    private var beaconLocationMarker : Marker? = null

    private var mapLibreMap : MapLibreMap? = null

    private fun updateBeaconLocation() {
        if (beaconLocationMarker != null) {
            beaconLocationMarker?.position = beaconLocation
        } else {
            val markerOptions = MarkerOptions()
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
                    heading = value.headingDegrees

                    mapLibreMap?.cameraPosition = CameraPosition.Builder()
                        .bearing(heading.toDouble())
                        .build()
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
                }
                else {
                    if(beaconLocationMarker != null) {
                        mapLibreMap?.removeMarker(beaconLocationMarker!!)
                        beaconLocationMarker = null
                    }
                }
            }
        }
    }

    private fun updateLocationOnMap(location : Location) {
        if( (initialLocation == null) &&
            location.hasAccuracy() && (location.accuracy < 250.0)) {
            initialLocation = location
            Log.d(TAG, "lastLocation updated to $location")
        }
        if(!mapCentered && (mapLibreMap != null) && (initialLocation != null)) {
            // If the map has already been created and it's not yet been centered, and we've received
            // a location with reasonable accuracy then center it
            mapCentered = true
            mapLibreMap?.cameraPosition = CameraPosition.Builder()
                .target(LatLng(location.latitude, location.longitude))
                .zoom(15.0)
                .bearing(heading.toDouble())
                .build()
        }

        latitude = location.latitude
        longitude = location.longitude

        // Use MarkerOptions and addMarker() to add a new marker in map
        val latLng = LatLng(latitude, longitude)
        if(currentLocationMarker != null) {
            currentLocationMarker?.position = latLng
        }
        else {
            val icon = iconFactory.fromResource(R.drawable.icons8_navigation_24)
            val markerOptions = MarkerOptions()
                .position(latLng)
                .icon(icon)
            currentLocationMarker = mapLibreMap?.addMarker(markerOptions)
        }
    }

    fun setMap(map: MapLibreMap) {
        // Set the style after mapView was loaded
        mapLibreMap = map
        val apiKey = BuildConfig.TILE_PROVIDER_API_KEY
        val styleUrl = "https://api.maptiler.com/maps/streets-v2/style.json?key=$apiKey"
        mapLibreMap?.setStyle(styleUrl) {

            ////////////////////////////////////////////////////////////////////////////////////////
            // Prove that these are vector maps by listing the layers and then changing the colour
            // of water...
//            for (singleLayer in it.layers) {
//                Log.d(TAG, "onMapReady: layer id = " + singleLayer.id)
//            }
//            val waterLayer = it.getLayer("Water")
//            waterLayer?.setProperties(PropertyFactory.fillColor(Color.parseColor("#900090")))
            //
            ////////////////////////////////////////////////////////////////////////////////////////

            mapLibreMap?.uiSettings?.setAttributionMargins(15, 0, 0, 15)

            // Set the map view center if we already have an initial location
            initialLocation?.let { location -> updateLocationOnMap(location) }
            // Update the map with the beacon location if we already have one
            beaconLocation?.let { updateBeaconLocation() }

            mapLibreMap?.addOnMapLongClickListener { latitudeLongitude ->
                soundscapeServiceConnection.soundscapeService?.createBeacon(
                    latitudeLongitude.latitude,
                    latitudeLongitude.longitude
                )
                false
            }
            mapLibreMap?.setOnMarkerClickListener(this)
        }

    }

    override fun onMarkerClick(marker: Marker): Boolean {
        if(marker == beaconLocationMarker) {
            soundscapeServiceConnection.soundscapeService?.destroyBeacon()
            return true
        }
        return false
    }

    // This is a demo function to show how to dynamically alter the map based on user input.
    // The result of this function is that Food POIs have their icons toggled between enlarged
    // and regular sized.
    private var highlightedPointsOfInterest : Boolean = false
    fun highlightPointsOfInterest() {
        highlightedPointsOfInterest = highlightedPointsOfInterest.xor(true)
        mapLibreMap?.getStyle {
            val foodLayer = it.getLayer("Food")
            var highlightSize = 1F
            if(highlightedPointsOfInterest)
                highlightSize = 2F

            foodLayer?.setProperties(PropertyFactory.iconSize(highlightSize))
        }
    }

    init {
        serviceConnection = soundscapeServiceConnection
        iconFactory = IconFactory.getInstance(context)
        viewModelScope.launch {
            soundscapeServiceConnection.serviceBoundState.collect {
                Log.d(TAG, "serviceBoundState $it")
                if(it) {
                    startMonitoringLocation()
                }
            }
        }
    }

    companion object {
        private const val TAG = "HomeViewModel"
    }
}
