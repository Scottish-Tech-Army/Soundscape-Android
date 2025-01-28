package org.scottishtecharmy.soundscape.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.SoundscapeServiceConnection
import org.scottishtecharmy.soundscape.database.local.model.Location
import org.scottishtecharmy.soundscape.database.local.model.MarkerData
import org.scottishtecharmy.soundscape.database.repository.MarkersRepository
import org.scottishtecharmy.soundscape.database.repository.RoutesRepository
import org.scottishtecharmy.soundscape.screens.home.Navigator
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import javax.inject.Inject

@HiltViewModel
class LocationDetailsViewModel
    @Inject
    constructor(
        private val soundscapeServiceConnection: SoundscapeServiceConnection,
        private val navigator: Navigator,
        private val routesRepository: RoutesRepository,
        private val markersRepository: MarkersRepository,
    ) : ViewModel() {
        private var serviceConnection: SoundscapeServiceConnection? = null

        fun createBeacon(
            latitude: Double,
            longitude: Double,
        ) {
            soundscapeServiceConnection.soundscapeService?.createBeacon(latitude, longitude)
        }

        fun enableStreetPreview(
            latitude: Double,
            longitude: Double,
        ) {
            soundscapeServiceConnection.setStreetPreviewMode(true, latitude, longitude)
        }

        fun createMarker(locationDescription: LocationDescription) {
            viewModelScope.launch {
                var name = locationDescription.addressName
                if (name == null) name = locationDescription.fullAddress
                name = name ?: "Unknown"
                val marker =
                    MarkerData(
                        addressName = name,
                        fullAddress = locationDescription.fullAddress ?: "", // TODO Fanny is it possible to get no full address ?
                        location = Location(latitude = locationDescription.latitude, longitude = locationDescription.longitude),
                    )
                try {
                    markersRepository.insertMarker(marker)

                    Log.d("LocationDetailsViewModel", "Marker saved successfully: ${marker.addressName}")
                } catch (e: Exception) {
                    Log.e("LocationDetailsViewModel", "Error saving route: ${e.message}")
                }
            }
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
