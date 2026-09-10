package org.scottishtecharmy.soundscape.utils

import org.scottishtecharmy.soundscape.components.LocationSource
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers the name LocationDescription.process() gives a house number, which is written the way
 * the country it's in writes addresses.
 */
class LocationDescriptionAddressTest {

    private fun houseNumber(location: LngLatAlt, number: String, road: String): LocationDescription =
        MvtFeature().apply {
            geometry = Point(location)
            properties = hashMapOf<String, Any?>("housenumber" to number, "street" to road)
        }.toLocationDescription(LocationSource.OfflineGeocoder)

    @Test
    fun addressIsWrittenTheWayTheCountryItIsInWritesThem() {
        // Whatever country the phone running the test is set to
        assertEquals(
            "48 Station Road",
            houseNumber(LngLatAlt(-4.2518, 55.8642), "48", "Station Road").name
        )
        assertEquals(
            "Avenida Corrientes 1155",
            houseNumber(LngLatAlt(-58.3832, -34.6035), "1155", "Avenida Corrientes").name
        )
    }

    @Test
    fun houseNumberOnALineOfItsOwnIsKeptInTheName() {
        // Iranian addresses put the house number on the line after the road
        assertEquals(
            "رشتچی ۱۴",
            houseNumber(LngLatAlt(51.3905, 35.6998), "۱۴", "رشتچی").name
        )
    }
}
