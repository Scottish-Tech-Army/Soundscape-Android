package org.scottishtecharmy.soundscape.viewmodels.home

import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription

data class HomeState(
    var heading: Float = 0.0f,
    var location: LngLatAlt? = null,
    var beaconLocation: LngLatAlt? = null,
    var streetPreviewMode: Boolean = false,
    var tileGridGeoJson: String = "",
    var isSearching: Boolean = false,
    var searchItems: List<LocationDescription>? = null,
    var routesTabSelected: Boolean = true,
)
