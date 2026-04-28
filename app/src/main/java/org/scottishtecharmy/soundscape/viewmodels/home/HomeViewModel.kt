package org.scottishtecharmy.soundscape.viewmodels.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.ViewModel
import org.scottishtecharmy.soundscape.SoundscapeServiceConnection
import org.scottishtecharmy.soundscape.audio.AudioTour
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.HomeStateHolder
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription

class HomeViewModel(
    soundscapeServiceConnection: SoundscapeServiceConnection,
    val audioTour: AudioTour,
) : ViewModel() {

    val holder = HomeStateHolder(soundscapeServiceConnection, audioTour)
    val state = holder.state

    fun myLocation() = holder.myLocation()
    fun aheadOfMe() = holder.aheadOfMe()
    fun whatsAroundMe() = holder.whatsAroundMe()
    fun nearbyMarkers() = holder.nearbyMarkers()
    fun streetPreviewGo() = holder.streetPreviewGo()
    fun streetPreviewExit() = holder.streetPreviewExit()
    fun routeSkipPrevious() = holder.routeSkipPrevious()
    fun routeSkipNext() = holder.routeSkipNext()
    fun routeMute() = holder.routeMute()
    fun routeStop() = holder.routeStop()
    fun getLocationDescription(location: LngLatAlt): LocationDescription? =
        holder.getLocationDescription(location)
    fun onTriggerSearch(text: String) = holder.onTriggerSearch(text)
    fun setRoutesAndMarkersTab(pickRoutes: Boolean) = holder.setRoutesAndMarkersTab(pickRoutes)

    fun goToAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.fromParts("package", "org.scottishtecharmy.soundscape", null)
        context.startActivity(intent)
    }

    override fun onCleared() {
        super.onCleared()
        holder.dispose()
    }
}
