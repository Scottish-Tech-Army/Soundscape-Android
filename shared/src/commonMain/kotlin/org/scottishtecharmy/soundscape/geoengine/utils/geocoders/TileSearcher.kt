package org.scottishtecharmy.soundscape.geoengine.utils.geocoders

import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription

interface TileSearcher {
    fun search(
        location: LngLatAlt,
        searchString: String,
        localizedStrings: LocalizedStrings?,
        settlementNames: Set<String>
    ): List<LocationDescription>
}
