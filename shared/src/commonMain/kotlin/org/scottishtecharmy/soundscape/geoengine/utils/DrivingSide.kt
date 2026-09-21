package org.scottishtecharmy.soundscape.geoengine.utils

import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.network.GeoJsonParser
import org.scottishtecharmy.soundscape.platform.readResourceText

enum class DrivingSide {
    LEFT,
    RIGHT
}

// ISO 3166-1 alpha-2 codes of countries/territories that drive on the left. This list changes
// extremely rarely (the last switch was Samoa in 2009) so a static set is fine.
private val leftHandTrafficCountries = setOf(
    // Africa
    "BW", "KE", "LS", "MW", "MU", "MZ", "NA", "ZA", "SZ", "TZ", "UG", "ZM", "ZW", "SC",
    // Asia
    "BD", "BT", "BN", "TL", "HK", "IN", "ID", "JP", "MO", "MY", "NP", "PK", "SG", "LK", "TH",
    // Europe
    "CY", "IE", "MT", "GB", "IM", "GG", "JE",
    // Oceania
    "AU", "CK", "FJ", "KI", "NR", "NZ", "NU", "PG", "WS", "SB", "TO", "TV",
    // Caribbean/Americas
    "AI", "AG", "BS", "BB", "BM", "VG", "KY", "DM", "FK", "GD", "GY", "JM", "MS", "KN", "LC",
    "VC", "SR", "TT", "TC", "VI",
)

/**
 * Looks up which side of the road a country drives on, from its ISO_A2 code (as found in the
 * bundled country-boundaries GeoJSON - see [CountryBoundaries]). Defaults to RIGHT for any code
 * not in [leftHandTrafficCountries], since right-hand traffic is the global majority.
 */
fun drivingSideForCountry(isoA2: String): DrivingSide {
    return if (isoA2.uppercase() in leftHandTrafficCountries) DrivingSide.LEFT else DrivingSide.RIGHT
}

/**
 * Loads a bundled, simplified (Natural Earth 1:110m) set of country boundaries once and uses it
 * to look up which country - and so which side of the road traffic drives on - a location falls
 * within. This is a coarse boundary (accurate to within a few hundred metres at best), which is
 * fine for a "which side of the road" heuristic but not suitable for anything requiring precise
 * border accuracy.
 */
object CountryBoundaries {
    private val featureTree: FeatureTree by lazy {
        val geoJson = readResourceText("geography/countries.geojson")
        val featureCollection = GeoJsonParser.parseFeatureCollection(geoJson)
        FeatureTree(featureCollection)
    }

    /** Returns the ISO_A2 country code for the country containing [location], or null if none. */
    fun countryCode(location: LngLatAlt): String? {
        val containing = featureTree.getContainingPolygons(location)
        return containing.features.firstOrNull()?.properties?.get("ISO_A2") as? String
    }

    /** Returns the driving side for [location]'s country, or null if the country can't be determined. */
    fun drivingSide(location: LngLatAlt): DrivingSide? {
        return countryCode(location)?.let { drivingSideForCountry(it) }
    }
}
