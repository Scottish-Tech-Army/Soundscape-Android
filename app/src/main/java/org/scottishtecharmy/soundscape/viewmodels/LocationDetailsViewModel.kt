package org.scottishtecharmy.soundscape.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.mongodb.kbson.ObjectId
import org.scottishtecharmy.soundscape.SoundscapeServiceConnection
import org.scottishtecharmy.soundscape.database.local.model.Location
import org.scottishtecharmy.soundscape.database.local.model.MarkerData
import org.scottishtecharmy.soundscape.database.repository.RoutesRepository
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import javax.inject.Inject

@HiltViewModel
class LocationDetailsViewModel @Inject constructor(
    private val soundscapeServiceConnection : SoundscapeServiceConnection,
    private val routesRepository: RoutesRepository
): ViewModel() {

    fun createBeacon(location: LngLatAlt) {
        soundscapeServiceConnection.soundscapeService?.createBeacon(location)
    }

    fun enableStreetPreview(location: LngLatAlt) {
        soundscapeServiceConnection.setStreetPreviewMode(true, location)

    }

    fun createMarker(locationDescription: LocationDescription) {
        viewModelScope.launch {
            var name = locationDescription.name
            if (name == null) name = locationDescription.fullAddress
            name = name ?: "Unknown"

            val updated = locationDescription.markerObjectId?.let { objectId ->
                // We are updating an existing marker
                val markerData = MarkerData(
                    objectId = objectId,
                    addressName = name,
                    fullAddress = locationDescription.fullAddress
                        ?: "", // TODO Fanny is it possible to get no full address ?
                    location = Location(
                        latitude = locationDescription.location.latitude,
                        longitude = locationDescription.location.longitude
                    ),
                )
                try {
                    routesRepository.updateMarker(markerData)

                    Log.d(
                        "LocationDetailsViewModel",
                        "Marker saved successfully: ${markerData.addressName}"
                    )
                    true
                } catch (e: Exception) {
                    Log.e("LocationDetailsViewModel", "Error saving route: ${e.message}")
                    null
                }
            }
            if(updated == null) {
                val marker =
                    MarkerData(
                        addressName = name,
                        fullAddress = locationDescription.fullAddress
                            ?: "", // TODO Fanny is it possible to get no full address ?
                        location = Location(
                            latitude = locationDescription.location.latitude,
                            longitude = locationDescription.location.longitude
                        ),
                    )
                try {
                    routesRepository.insertMarker(marker)
                    locationDescription.markerObjectId = marker.objectId

                    Log.d(
                        "LocationDetailsViewModel",
                        "Marker saved successfully: ${marker.addressName}"
                    )
                } catch (e: Exception) {
                    Log.e("LocationDetailsViewModel", "Error saving route: ${e.message}")
                }
            }
        }
    }

    fun deleteMarker(objectId: ObjectId) {
        viewModelScope.launch {
            try {
                routesRepository.deleteMarker(objectId)
                Log.d("LocationDetailsViewModel","Delete $objectId")
            } catch (e: Exception) {
                Log.e("LocationDetailsViewModel", "Error deleting marker: ${e.message}")
            }
        }
    }

    init {
        viewModelScope.launch {
            soundscapeServiceConnection.serviceBoundState.collect {
                Log.d(TAG, "serviceBoundState $it")
            }
        }
    }

    fun getLocationDescription(location: LngLatAlt) : LocationDescription? {
        return soundscapeServiceConnection.soundscapeService?.getLocationDescription(location)
    }

    companion object {
        private const val TAG = "LocationDetailsViewModel"
    }
}
