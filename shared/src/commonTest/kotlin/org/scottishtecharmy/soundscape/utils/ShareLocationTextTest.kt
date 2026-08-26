package org.scottishtecharmy.soundscape.utils

import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShareLocationTextTest {

    private fun desc(name: String, longitude: Double, latitude: Double) =
        LocationDescription(name = name, location = LngLatAlt(longitude, latitude))

    private val googleMapsBuilder: (String, String, String) -> String =
        { lat, lon, encodedName -> "https://maps.google.com/?q=$lat,$lon($encodedName)" }

    @Test
    fun buildsFullMessageWithAllPlaceholdersAndCoordinateFormatting() {
        val result = buildShareLocationText(
            desc = desc("Café & Bar", -4.25, 55.5),
            messageTemplate = "See %1\$s here: %2\$s (or Google Maps: %3\$s)",
            mapsName = "Bing Maps",
            mapsUrlBuilder = { lat, lon, encodedName ->
                "https://bing.com/maps?lat=$lat&lon=$lon&q=$encodedName"
            },
        )

        val soundscapeUrl =
            "https://links.soundscape.scottishtecharmy.org/v1/sharemarker?" +
                "lat=55.50000&lon=-4.25000&name=Caf%C3%A9%20%26%20Bar"
        val mapsUrl = "https://bing.com/maps?lat=55.50000&lon=-4.25000&q=Caf%C3%A9%20%26%20Bar"

        assertEquals(
            "See Café & Bar here: $soundscapeUrl (or Bing Maps: $mapsUrl)",
            result,
        )
    }

    @Test
    fun replacesLiteralGoogleMapsTextWithMapsName() {
        val result = buildShareLocationText(
            desc = desc("Home", 0.0, 0.0),
            messageTemplate = "Open in Google Maps",
            mapsName = "Apple Maps",
            mapsUrlBuilder = googleMapsBuilder,
        )
        assertEquals("Open in Apple Maps", result)
    }

    @Test
    fun templateWithoutGoogleMapsText_isUnaffected() {
        val result = buildShareLocationText(
            desc = desc("Home", 0.0, 0.0),
            messageTemplate = "%1\$s",
            mapsName = "Apple Maps",
            mapsUrlBuilder = googleMapsBuilder,
        )
        assertEquals("Home", result)
    }

    @Test
    fun emptyName_producesEmptyNamePlaceholder() {
        val result = buildShareLocationText(
            desc = desc("", 1.0, 2.0),
            messageTemplate = "%1\$s|%2\$s",
            mapsName = "Maps",
            mapsUrlBuilder = googleMapsBuilder,
        )
        assertTrue(result.startsWith("|"))
    }

    @Test
    fun zeroCoordinates_formatToFiveDecimalPlaces() {
        val result = buildShareLocationText(
            desc = desc("Origin", 0.0, 0.0),
            messageTemplate = "%2\$s",
            mapsName = "Maps",
            mapsUrlBuilder = googleMapsBuilder,
        )
        assertTrue(result.contains("lat=0.00000&lon=0.00000"))
    }

    @Test
    fun negativeCoordinates_formatToFiveDecimalPlaces() {
        val result = buildShareLocationText(
            desc = desc("Somewhere", -0.5, -12.0),
            messageTemplate = "%2\$s",
            mapsName = "Maps",
            mapsUrlBuilder = googleMapsBuilder,
        )
        assertTrue(result.contains("lat=-12.00000&lon=-0.50000"))
    }

    @Test
    fun coordinatesWithMoreThanFiveDecimals_areRounded() {
        val result = buildShareLocationText(
            desc = desc("Somewhere", 1.999999, 55.123456),
            messageTemplate = "%2\$s",
            mapsName = "Maps",
            mapsUrlBuilder = googleMapsBuilder,
        )
        // 1.999999 rounds to 2.00000, 55.123456 rounds to 55.12346 (round-half-up at the 5th place).
        assertTrue(result.contains("lat=55.12346&lon=2.00000"))
    }

    @Test
    fun coordinatesWithFewerThanFiveDecimals_arePaddedWithZeros() {
        val result = buildShareLocationText(
            desc = desc("Somewhere", -4.25, 55.5),
            messageTemplate = "%2\$s",
            mapsName = "Maps",
            mapsUrlBuilder = googleMapsBuilder,
        )
        assertTrue(result.contains("lat=55.50000&lon=-4.25000"))
    }

    @Test
    fun nameWithSpacesAndUnicode_isPercentEncodedInUrls() {
        val result = buildShareLocationText(
            desc = desc("Café & Bar", 0.0, 0.0),
            messageTemplate = "%2\$s|%3\$s",
            mapsName = "Maps",
            mapsUrlBuilder = { _, _, encodedName -> encodedName },
        )
        // Both the soundscape URL and the maps-builder-provided encoded name use the same
        // percent-encoded form: space -> %20, '&' -> %26, 'é' (UTF-8 0xC3 0xA9) -> %C3%A9.
        assertTrue(result.contains("name=Caf%C3%A9%20%26%20Bar"))
        assertTrue(result.endsWith("|Caf%C3%A9%20%26%20Bar"))
    }

    @Test
    fun unreservedCharactersAreNotEncoded() {
        val result = buildShareLocationText(
            desc = desc("abc-XYZ_123.~", 0.0, 0.0),
            messageTemplate = "%2\$s",
            mapsName = "Maps",
            mapsUrlBuilder = googleMapsBuilder,
        )
        assertTrue(result.contains("name=abc-XYZ_123.~"))
    }

    @Test
    fun mapsUrlBuilderReceivesFormattedLatLonAndEncodedName() {
        var receivedLat: String? = null
        var receivedLon: String? = null
        var receivedName: String? = null
        buildShareLocationText(
            desc = desc("A B", -1.5, 2.5),
            messageTemplate = "%3\$s",
            mapsName = "Maps",
            mapsUrlBuilder = { lat, lon, encodedName ->
                receivedLat = lat
                receivedLon = lon
                receivedName = encodedName
                "unused"
            },
        )
        assertEquals("2.50000", receivedLat)
        assertEquals("-1.50000", receivedLon)
        assertEquals("A%20B", receivedName)
    }

    @Test
    fun nameContainingPlaceholderSyntax_isNotSweptUpByLaterSubstitution() {
        // A single regex pass matches placeholders against the original template only, so a
        // location name that happens to contain literal placeholder-like text (e.g. "%2$s")
        // is inserted as-is and never re-matched by a later substitution.
        val soundscapeUrlPrefix = "https://links.soundscape.scottishtecharmy.org/v1/sharemarker?"
        val result = buildShareLocationText(
            desc = desc("%2\$s", 0.0, 0.0),
            messageTemplate = "%1\$s - %2\$s",
            mapsName = "Maps",
            mapsUrlBuilder = googleMapsBuilder,
        )
        val actualUrl = result.substringAfter(" - ")
        assertTrue(actualUrl.startsWith(soundscapeUrlPrefix))
        // The literal name text stays untouched; only the real template placeholder resolves.
        assertEquals("%2\$s - $actualUrl", result)
    }
}
