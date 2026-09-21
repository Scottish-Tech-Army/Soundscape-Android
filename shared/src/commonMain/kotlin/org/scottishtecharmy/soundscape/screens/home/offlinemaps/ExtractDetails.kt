package org.scottishtecharmy.soundscape.screens.home.offlinemaps

import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature

/**
 * Parses display fields out of an extract manifest [Feature].
 *
 * - Local name/cities come from the user's locale-specific properties (`name_local`,
 *   `city_local_names`).
 * - Alternate name/cities are the canonical English ones, shown alongside the local
 *   versions so the user can match them up.
 */
class ExtractDetails(extract: Feature) {
    var localName: String = ""
    var alternateName: String = ""

    var localCities: String = ""
    var alternateCities: String = ""

    init {
        val namePropLocal = extract.properties?.get("name_local")
        val nameProp = extract.properties?.get("name")
        if (namePropLocal != null) {
            localName = namePropLocal.toString()
            alternateName = nameProp?.toString() ?: ""
        } else {
            localName = nameProp?.toString() ?: ""
        }

        localCities = joinCityList(extract.properties?.get("city_local_names"))
        alternateCities = joinCityList(extract.properties?.get("city_names"))
    }

    private fun joinCityList(value: Any?): String {
        if (value !is List<*>) return ""
        return value.joinToString(separator = ", ") { it.toString() }
    }
}
