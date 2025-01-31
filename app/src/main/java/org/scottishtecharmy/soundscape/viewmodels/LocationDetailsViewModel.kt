package org.scottishtecharmy.soundscape.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.SoundscapeServiceConnection
import org.scottishtecharmy.soundscape.database.local.model.Location
import org.scottishtecharmy.soundscape.database.local.model.RoutePoint
import org.scottishtecharmy.soundscape.database.repository.RoutesRepository
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import javax.inject.Inject

@HiltViewModel
class LocationDetailsViewModel @Inject constructor(
    private val soundscapeServiceConnection : SoundscapeServiceConnection,
    private val routesRepository: RoutesRepository
    ): ViewModel() {

    private var serviceConnection : SoundscapeServiceConnection? = null

    fun createBeacon(latitude: Double, longitude: Double) {
        soundscapeServiceConnection.soundscapeService?.createBeacon(latitude, longitude)
    }

    fun enableStreetPreview(latitude: Double, longitude: Double) {
        soundscapeServiceConnection.setStreetPreviewMode(true, latitude, longitude)

    }

    fun createMarker(locationDescription: LocationDescription) {
        viewModelScope.launch {
            var name = locationDescription.addressName
            if(name == null) name = locationDescription.streetNumberAndName
            name = name ?: "Unknown"
            val marker = RoutePoint(name,
                                    Location(locationDescription.latitude, locationDescription.longitude))
            try {
                routesRepository.insertWaypoint(marker)
                Log.d("LocationDetailsViewModel", "Marker saved successfully: ${marker.name}")
            } catch (e: Exception) {
                Log.e("LocationDetailsViewModel", "Error saving route: ${e.message}")
            }
        }
    }

    fun getLocationDescription(location: LngLatAlt) : LocationDescription? {
        return soundscapeServiceConnection.soundscapeService?.getLocationDescription(location)
    }

    init {
        serviceConnection = soundscapeServiceConnection
        viewModelScope.launch {
            soundscapeServiceConnection.serviceBoundState.collect {
                Log.d(TAG, "serviceBoundState $it")
            }
        }
    }

    companion object {
        private const val TAG = "LocationDetailsViewModel"
    }
}