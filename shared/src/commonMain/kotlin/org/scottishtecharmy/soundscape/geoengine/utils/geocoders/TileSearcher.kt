package org.scottishtecharmy.soundscape.geoengine.utils.geocoders

import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription

interface TileSearcher {
    fun search(
        location: LngLatAlt,
        searchString: String,
        localizedStrings: LocalizedStrings?,
        settlementNames: Set<String>
    ): List<LocationDescription>

    /**
     * The places with a class or subclass in [values] nearest to [location], nearest first. When
     * [name] isn't null, only those which it matches the name of.
     */
    fun searchByCategory(
        location: LngLatAlt,
        values: Set<String>,
        name: String?,
        localizedStrings: LocalizedStrings?,
        limit: Int
    ): List<LocationDescription> = emptyList()
}
