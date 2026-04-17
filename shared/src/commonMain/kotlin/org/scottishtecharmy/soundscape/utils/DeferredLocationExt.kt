package org.scottishtecharmy.soundscape.utils

import org.scottishtecharmy.soundscape.components.LocationSource
import org.scottishtecharmy.soundscape.geoengine.TextForFeature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription

fun Feature.deferredToLocationDescription(
    source: LocationSource,
    alternateLocation: LngLatAlt = LngLatAlt(),
    featureName: TextForFeature? = null,
): LocationDescription {
    val location = when (geometry.type) {
        "Point" -> (geometry as Point).coordinates
        else -> alternateLocation
    }
    return LocationDescription(
        source = source,
        location = location,
        feature = this,
        alternateLocation = alternateLocation,
        featureName = featureName,
    )
}
