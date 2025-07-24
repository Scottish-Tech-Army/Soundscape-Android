package org.scottishtecharmy.soundscape.viewmodels

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.SoundscapeServiceConnection
import org.scottishtecharmy.soundscape.audio.AudioType
import org.scottishtecharmy.soundscape.database.local.dao.RouteDao
import org.scottishtecharmy.soundscape.database.local.model.MarkerEntity
import org.scottishtecharmy.soundscape.geoengine.PositionedString
import org.scottishtecharmy.soundscape.geoengine.filters.TrackedCallout
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import java.net.URLEncoder
import javax.inject.Inject

@HiltViewModel
class LocationDetailsViewModel @Inject constructor(
    private val soundscapeServiceConnection : SoundscapeServiceConnection,
    private val routeDao: RouteDao
): ViewModel() {

    fun startBeacon(location: LngLatAlt, name: String) {
        soundscapeServiceConnection.soundscapeService?.startBeacon(location, name)
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
            routeDao = routeDao,
            viewModelScope = viewModelScope,
            onSuccess = {
                Log.d("LocationDetailsViewModel", successMessage)
                soundscapeServiceConnection.soundscapeService?.speakCallout(
                    TrackedCallout(
                        positionedStrings = listOf(
                            PositionedString(text = successMessage, type = AudioType.STANDARD)
                        ),
                        filter = false,
                    ),
                    false
                )
            },
            onFailure = {
                Log.e("LocationDetailsViewModel", failureMessage)
                soundscapeServiceConnection.soundscapeService?.speakCallout(
                    TrackedCallout(
                        positionedStrings = listOf(
                            PositionedString(text = failureMessage, type = AudioType.STANDARD)
                        ),
                        filter = false,
                    ),
                    false
                )
            }
        )
    }

    fun deleteMarker(objectId: Long) {
        viewModelScope.launch {
            try {
                routeDao.removeMarker(objectId)
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
    routeDao: RouteDao,
    viewModelScope: CoroutineScope,
    onSuccess: () -> Unit,
    onFailure: () -> Unit,
) {
    viewModelScope.launch {
        var name = locationDescription.name
        if (name.isEmpty()) {
            name = if(locationDescription.description == null)
                "Unknown"
            else
                locationDescription.description!!
        }

        var updated = false
        if(locationDescription.databaseId != 0L) {
            // We are updating an existing marker
            val markerData = MarkerEntity(
                markerId = locationDescription.databaseId,
                name = name,
                fullAddress = locationDescription.description
                    ?: "", // TODO Fanny is it possible to get no full address ?
                longitude = locationDescription.location.longitude,
                latitude = locationDescription.location.latitude
            )
            try {
                routeDao.updateMarker(markerData)
                onSuccess()
                updated = true
            } catch (e: Exception) {
                onFailure()
            }
        }
        if(!updated) {
            val marker =
                MarkerEntity(
                    name = name,
                    fullAddress = locationDescription.description
                        ?: "", // TODO Fanny is it possible to get no full address ?
                    longitude = locationDescription.location.longitude,
                    latitude = locationDescription.location.latitude
                )
            try {
                locationDescription.databaseId = routeDao.insertMarker(marker)
                onSuccess()
            } catch (e: Exception) {
                onFailure()
            }
        }
    }
}

