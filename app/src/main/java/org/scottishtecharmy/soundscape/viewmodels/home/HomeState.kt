package org.scottishtecharmy.soundscape.viewmodels.home

import android.location.Location
import org.maplibre.android.geometry.LatLng
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription

data class HomeState(
    var heading: Float = 0.0f,
    var location: Location? = null,
    var beaconLocation: LatLng? = null,
    var streetPreviewMode: Boolean = false,
    var tileGridGeoJson: String = "",
    var isSearching: Boolean = false,
    var searchItems: List<LocationDescription>? = null,
)
