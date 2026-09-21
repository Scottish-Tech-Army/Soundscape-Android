package org.scottishtecharmy.soundscape.screens.home.placesnearby

import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription

data class PlacesNearbyUiState(
    var userLocation: LngLatAlt? = null,
    /**
     * The country whose address conventions every place in the list is written in, looked up once
     * from [userLocation]. Everything nearby is by definition in the same country as the user, so
     * the rows share the one answer instead of each asking the country boundaries themselves.
     */
    var countryCode: String? = null,
    var level: Int = 0,
    var nearbyPlaces: FeatureCollection = FeatureCollection(),
    var nearbyIntersections: FeatureCollection = FeatureCollection(),
    var filter: String = "",
    var title: String = "",
    var markerDescription: LocationDescription? = null
)
