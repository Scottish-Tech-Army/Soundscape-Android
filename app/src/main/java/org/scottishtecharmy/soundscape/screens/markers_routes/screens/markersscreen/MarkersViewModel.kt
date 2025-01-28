package org.scottishtecharmy.soundscape.screens.markers_routes.screens.markersscreen

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.SoundscapeServiceConnection
import org.scottishtecharmy.soundscape.database.repository.MarkersRepository
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.getSortOrderPreference
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.saveSortOrderPreference
import org.scottishtecharmy.soundscape.utils.calculateDistanceTo
import javax.inject.Inject

@HiltViewModel
class MarkersViewModel
    @Inject
    constructor(
        private val markersRepository: MarkersRepository,
        private val soundscapeServiceConnection: SoundscapeServiceConnection,
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(MarkersUiState())
        val uiState: StateFlow<MarkersUiState> = _uiState

        init {
            loadMarkers()
            // Load the saved sort order when initializing the ViewModel
            val isAscending = getSortOrderPreference(context)
            _uiState.value = _uiState.value.copy(isSortByName = isAscending)
            sortMarkers(isAscending)
        }

        fun toggleSortOrder() {
            val isAscending = !_uiState.value.isSortByName
            sortMarkers(isAscending)
            saveSortOrderPreference(context, isAscending)
        }

        private fun sortMarkers(isAscending: Boolean) {
            val sortedMarkers =
                if (isAscending) {
                    _uiState.value.markers.sortedBy { it.addressName }
                } else {
                    _uiState.value.markers
                }
            _uiState.value = _uiState.value.copy(markers = sortedMarkers, isSortByName = isAscending)
        }

        private fun loadMarkers() {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true)

                try {
                    val userLocation = soundscapeServiceConnection.getLocationFlow()?.firstOrNull()
                    val markerVMs =
                        markersRepository.getMarkers().map {
                            LocationDescription(
                                addressName = it.addressName,
                                fullAddress = it.fullAddress,
                                distance =
                                    if (userLocation != null && it.location != null) {
                                        userLocation.calculateDistanceTo(it.location!!.latitude, it.location!!.longitude)
                                    } else {
                                        null
                                    },
                                marker = true,
                            )
                        }
                    val isAscending = getSortOrderPreference(context)
                    val sortedMarkers =
                        if (isAscending) {
                            markerVMs.sortedBy { it.addressName }
                        } else {
                            markerVMs
                        }
                    _uiState.value = _uiState.value.copy(markers = sortedMarkers, isLoading = false, isSortByName = isAscending)
                } catch (e: Exception) {
                    Log.e("MarkersViewModel", "Failed to load markers: ${e.message}")
                    _uiState.value =
                        _uiState.value.copy(
                            errorMessage = "Failed to load markers: ${e.message}",
                            isLoading = false,
                        )
                }
            }
        }

        fun clearErrorMessage() {
            _uiState.value = _uiState.value.copy(errorMessage = null)
        }
    }
