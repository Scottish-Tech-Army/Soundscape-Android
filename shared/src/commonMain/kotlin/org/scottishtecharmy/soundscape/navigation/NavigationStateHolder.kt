package org.scottishtecharmy.soundscape.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription

class NavigationStateHolder {
    private val _selectedLocation = MutableStateFlow<LocationDescription?>(null)
    val selectedLocation: StateFlow<LocationDescription?> = _selectedLocation

    fun setSelectedLocation(location: LocationDescription?) {
        _selectedLocation.value = location
    }

    private val _offlineMapsTargetLocation = MutableStateFlow<LngLatAlt?>(null)
    val offlineMapsTargetLocation: StateFlow<LngLatAlt?> = _offlineMapsTargetLocation

    fun setOfflineMapsTargetLocation(location: LngLatAlt?) {
        _offlineMapsTargetLocation.value = location
    }
}
