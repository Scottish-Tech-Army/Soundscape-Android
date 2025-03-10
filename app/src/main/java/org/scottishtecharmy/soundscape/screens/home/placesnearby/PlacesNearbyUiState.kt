package org.scottishtecharmy.soundscape.screens.home.placesnearby

import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

data class PlacesNearbyUiState(
    var userLocation: LngLatAlt? = null,
    var topLevel: Boolean = true,
    var nearbyPlaces: FeatureCollection = FeatureCollection(),
    var filter: String = "",
    var title: String = ""
)
