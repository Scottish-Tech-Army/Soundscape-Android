package org.scottishtecharmy.soundscape.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.SoundscapeServiceConnection
import org.scottishtecharmy.soundscape.screens.home.Navigator
import org.scottishtecharmy.soundscape.screens.home.locationDetails.generateLocationDetailsRoute
import javax.inject.Inject

@HiltViewModel
class LocationDetailsViewModel @Inject constructor(
    private val soundscapeServiceConnection : SoundscapeServiceConnection,
    private val navigator : Navigator): ViewModel() {

    private var serviceConnection : SoundscapeServiceConnection? = null

    fun createBeacon(latitude: Double, longitude: Double) {
        soundscapeServiceConnection.soundscapeService?.createBeacon(latitude, longitude)
    }

    fun enableStreetPreview(latitude: Double, longitude: Double) {
        soundscapeServiceConnection.setStreetPreviewMode(true, latitude, longitude)

    }

    init {
        navigator.navigate("")
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