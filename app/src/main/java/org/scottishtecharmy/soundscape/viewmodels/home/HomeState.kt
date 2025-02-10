package org.scottishtecharmy.soundscape.viewmodels.home

import org.maplibre.android.geometry.LatLng
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewEnabled
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewState
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription

data class HomeState(
    var heading: Float = 0.0f,
    var location: LngLatAlt? = null,
    var beaconLocation: LngLatAlt? = null,
    var streetPreviewState: StreetPreviewState = StreetPreviewState(StreetPreviewEnabled.OFF),
    var tileGridGeoJson: String = "",
    var isSearching: Boolean = false,
    var searchItems: List<LocationDescription>? = null,
    var routesTabSelected: Boolean = true,
)
