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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.maplibre.android.annotations.Marker
import org.maplibre.android.geometry.LatLng
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
    private val _beaconLocation: MutableStateFlow<LatLng?> = MutableStateFlow(null) // Question, can we have more beacon ?
    val beaconLocation: StateFlow<LatLng?> = _beaconLocation.asStateFlow()
    private val _streetPreviewMode: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val streetPreviewMode: StateFlow<Boolean> = _streetPreviewMode.asStateFlow()

    private var job = Job()
    private var spJob = Job()

    init {
        serviceConnection = soundscapeServiceConnection
        viewModelScope.launch {
            soundscapeServiceConnection.serviceBoundState.collect {
                Log.d(TAG, "serviceBoundState $it")
                if (it) {
                    // The service has started, so start monitoring the location and heading
                    startMonitoringLocation()
                    // And start monitoring the street preview mode
                    startMonitoringStreetPreviewMode()
                } else {
                    // The service has gone away so remove the current location marker
                    _location.value = null

                    stopMonitoringStreetPreviewMode()
                    stopMonitoringLocation()
                }
            }
        }
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
            serviceConnection?.getLocationFlow()?.collectLatest { value ->
                if (value != null) {
                    Log.d(TAG, "Location $value")
                    _location.value = value
                }
            }
        }
        viewModelScope.launch(job) {
            // Observe orientation updates from the service
            serviceConnection?.getOrientationFlow()?.collectLatest { value ->
                if (value != null) {
                    _heading.value = value.headingDegrees
                }
            }
        }
        viewModelScope.launch(job) {
            // Observe beacon location update from the service so we can show it on the map
            serviceConnection?.getBeaconFlow()?.collectLatest { value ->
                Log.d(TAG, "beacon collected $value")
                if (value != null) {
                    _beaconLocation.value = LatLng(value.latitude, value.longitude)
                } else {
                    _beaconLocation.value = null
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
            serviceConnection?.getStreetPreviewModeFlow()?.collect { value ->
                Log.d(TAG, "Street Preview Mode: $value")
                _streetPreviewMode.value = value

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

    fun createBeacon(latitudeLongitude: LatLng) {
        Log.d(TAG, "create beacon")
        soundscapeServiceConnection.soundscapeService?.createBeacon(
            latitudeLongitude.latitude,
            latitudeLongitude.longitude,
        )
    }

    fun onMarkerClick(marker: Marker): Boolean {
        Log.d(TAG, "marker click")

        if (marker.position == beaconLocation.value) {
            soundscapeServiceConnection.soundscapeService?.destroyBeacon()
            _beaconLocation.value = null
            return true
        }
        return false
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
