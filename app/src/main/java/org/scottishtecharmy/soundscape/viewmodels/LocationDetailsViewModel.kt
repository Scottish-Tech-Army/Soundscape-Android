package org.scottishtecharmy.soundscape.viewmodels

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import org.scottishtecharmy.soundscape.SoundscapeServiceConnection
import org.scottishtecharmy.soundscape.audio.AudioTour
import org.scottishtecharmy.soundscape.database.local.dao.RouteDao
import org.scottishtecharmy.soundscape.database.local.model.MarkerEntity
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import java.net.URLEncoder

class LocationDetailsViewModel(
    soundscapeServiceConnection: SoundscapeServiceConnection,
    routeDao: RouteDao,
    audioTour: AudioTour,
) : ViewModel() {

    val holder = LocationDetailsStateHolder(soundscapeServiceConnection, routeDao, audioTour)

    fun startBeacon(location: LngLatAlt, name: String) = holder.startBeacon(location, name)
    fun enableStreetPreview(location: LngLatAlt) = holder.enableStreetPreview(location)
    fun createMarker(
        locationDescription: LocationDescription,
        successMessage: String,
        failureMessage: String,
        duplicateMessage: String,
    ) = holder.createMarker(locationDescription, successMessage, failureMessage, duplicateMessage)
    fun deleteMarker(objectId: Long) = holder.deleteMarker(objectId)
    fun getLocationDescription(location: LngLatAlt): LocationDescription? =
        holder.getLocationDescription(location)
    suspend fun getMarkerAtLocation(location: LngLatAlt): MarkerEntity? =
        holder.getMarkerAtLocation(location)
    fun showDialog() = holder.showDialog()

    fun shareLocation(context: Context, message: String, locationDescription: LocationDescription) {
        val location = locationDescription.location
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TITLE, locationDescription.name)
            val latitude = "%.5f".format(location.latitude)
            val longitude = "%.5f".format(location.longitude)
            val soundscapeUrl =
                "https://links.soundscape.scottishtecharmy.org/v1/sharemarker?" +
                    "lat=$latitude&lon=$longitude&name=" +
                    URLEncoder.encode(locationDescription.name, Charsets.UTF_8.name())
            val googleMapsUrl = "https://www.google.com/maps/?q=$latitude,$longitude"
            putExtra(
                Intent.EXTRA_TEXT,
                message.format(locationDescription.name, soundscapeUrl, googleMapsUrl)
            )
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, null))
    }

    override fun onCleared() {
        super.onCleared()
        holder.dispose()
    }
}
