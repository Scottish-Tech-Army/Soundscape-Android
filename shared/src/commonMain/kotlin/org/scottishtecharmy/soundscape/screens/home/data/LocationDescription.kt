package org.scottishtecharmy.soundscape.screens.home.data

import org.scottishtecharmy.soundscape.components.LocationSource
import org.scottishtecharmy.soundscape.geoengine.TextForFeature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

enum class LocationType {
    StreetNumber,
    Street,
    City,
    Country
}

data class LocationDescription(
    var name: String = "",
    var location: LngLatAlt,
    var opposite: Boolean = false,
    var locationType: LocationType = LocationType.Country,
    var description: String? = null,
    /**
     * The street a POI without an address of its own sits on, shown in lists in place of
     * [description] so that rows which would otherwise all read "Post Box" can be told apart.
     * Deliberately separate from [description]: that field seeds the annotation when saving a
     * marker, and a confected street name isn't something the user typed.
     */
    var street: String? = null,
    var typeDescription: TextForFeature? = null,
    var source: LocationSource = LocationSource.UnknownSource,
    var orderId: Long = 0L,
    var databaseId: Long = 0,

    // Deferred properties
    var feature: Feature? = null,
    var alternateLocation: LngLatAlt? = null,
    var featureName: TextForFeature? = null
)
