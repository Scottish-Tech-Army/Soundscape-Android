package org.scottishtecharmy.soundscape.viewmodels

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.mongodb.kbson.ObjectId
import org.scottishtecharmy.soundscape.SoundscapeServiceConnection
import org.scottishtecharmy.soundscape.audio.AudioType
import org.scottishtecharmy.soundscape.database.local.model.Location
import org.scottishtecharmy.soundscape.database.local.model.MarkerData
import org.scottishtecharmy.soundscape.database.repository.RoutesRepository
import org.scottishtecharmy.soundscape.geoengine.PositionedString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import java.net.URLEncoder
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

    fun createMarker(
        locationDescription: LocationDescription,
        successMessage: String,
        failureMessage: String
    ) {
        createMarker(
            locationDescription = locationDescription,
            routesRepository = routesRepository,
            viewModelScope = viewModelScope,
            onSuccess = {
                Log.d("LocationDetailsViewModel", successMessage)
                soundscapeServiceConnection.soundscapeService?.speakCallout(
                    listOf(PositionedString(text = successMessage, type = AudioType.STANDARD)),
                    false
                )
            },
            onFailure = {
                Log.e("LocationDetailsViewModel", failureMessage)
                soundscapeServiceConnection.soundscapeService?.speakCallout(
                    listOf(PositionedString(text = failureMessage, type = AudioType.STANDARD)),
                    false
                )
            }
        )
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

    fun shareLocation(context: Context, message: String, locationDescription: LocationDescription) {
        // Share the current location using standard Android sharing mechanism. It's shared as a
        //
        //  geo://latitude,longitude
        //
        // URI, with the , encoded. This shows up in Slack as a clickable link which is the main
        // usefulness for now
        val location = locationDescription.location
        val sendIntent: Intent =
            Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TITLE, locationDescription.name)
                val latitude = location.latitude
                val longitude = location.longitude
                val uriData: String =
                    URLEncoder.encode("$latitude,$longitude", Charsets.UTF_8.name())
                putExtra(Intent.EXTRA_TEXT, "${message.format(locationDescription.name)}:\n geo://$uriData")
                type = "text/plain"
            }

        val shareIntent = Intent.createChooser(sendIntent, null)
        context.startActivity(shareIntent)
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

fun createMarker(
    locationDescription: LocationDescription,
    routesRepository: RoutesRepository,
    viewModelScope: CoroutineScope,
    onSuccess: () -> Unit,
    onFailure: () -> Unit,
) {
    viewModelScope.launch {
        var name = locationDescription.name
        if (name == null) name = locationDescription.description
        name = name ?: "Unknown"

        val updated = locationDescription.databaseId?.let { objectId ->
            // We are updating an existing marker
            val markerData = MarkerData(
                objectId = objectId,
                addressName = name,
                fullAddress = locationDescription.description
                    ?: "", // TODO Fanny is it possible to get no full address ?
                location = Location(
                    latitude = locationDescription.location.latitude,
                    longitude = locationDescription.location.longitude
                ),
            )
            try {
                routesRepository.updateMarker(markerData)
                onSuccess()
                true
            } catch (e: Exception) {
                onFailure()
                null
            }
        }
        if(updated == null) {
            val marker =
                MarkerData(
                    addressName = name,
                    fullAddress = locationDescription.description
                        ?: "", // TODO Fanny is it possible to get no full address ?
                    location = Location(
                        latitude = locationDescription.location.latitude,
                        longitude = locationDescription.location.longitude
                    ),
                )
            try {
                routesRepository.insertMarker(marker)
                locationDescription.databaseId = marker.objectId
                onSuccess()
            } catch (e: Exception) {
                onFailure()
            }
        }
    }
}

