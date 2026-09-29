package org.scottishtecharmy.soundscape.geoengine.utils.geocoders

import org.scottishtecharmy.soundscape.geoengine.utils.openlocationcode.OpenLocationCode
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoordinateSearchTest {

    private val glasgow = LngLatAlt(-4.2518, 55.8642)

    private fun parse(text: String, reference: LngLatAlt = glasgow) =
        parseCoordinateSearch(text, reference)

    private fun assertLocations(
        expected: List<Pair<Double, Double>>,
        actual: List<LngLatAlt>,
        tolerance: Double = 1e-6,
    ) {
        assertEquals(expected.size, actual.size, "Got $actual")
        for ((index, latLon) in expected.withIndex()) {
            val (latitude, longitude) = latLon
            assertTrue(
                abs(actual[index].latitude - latitude) < tolerance &&
                    abs(actual[index].longitude - longitude) < tolerance,
                "Expected $latitude,$longitude at $index, got $actual"
            )
        }
    }

    private fun assertSingle(latitude: Double, longitude: Double, text: String) =
        assertLocations(listOf(latitude to longitude), parse(text), tolerance = 1e-5)

    private fun assertNotCoordinate(text: String) =
        assertTrue(parse(text).isEmpty(), "\"$text\" was taken as ${parse(text)}")

    // Plain pairs of numbers

    @Test
    fun pairIsTriedBothWaysRoundNearestFirst() {
        val expected = listOf(55.9486 to -4.3148, -4.3148 to 55.9486)
        assertLocations(expected, parse("55.9486, -4.3148"))
        assertLocations(expected, parse("-4.3148 55.9486"))
        assertLocations(expected, parse("55.9486,-4.3148"))
        assertLocations(expected, parse("55.9486; -4.3148"))
        assertLocations(expected, parse("  +55.9486°, -4.3148°  "))
    }

    @Test
    fun nearestOfTheTwoComesFirst() {
        // Somewhere near 4.3N 55.9E, off the Horn of Africa
        val reference = LngLatAlt(55.0, 4.0)
        assertLocations(
            listOf(-4.3148 to 55.9486, 55.9486 to -4.3148),
            parse("55.9486, -4.3148", reference)
        )
    }

    @Test
    fun onlyValidOrderingIsReturned() {
        assertLocations(listOf(4.2 to 95.1), parse("95.1, 4.2"))
        assertNotCoordinate("95.1 120.0")
        assertNotCoordinate("200.5 10.5")
    }

    @Test
    fun equalNumbersGiveOneResult() {
        assertLocations(listOf(45.5 to 45.5), parse("45.5, 45.5"))
    }

    @Test
    fun commaDecimalSeparator() {
        val expected = listOf(55.9486 to -4.3148, -4.3148 to 55.9486)
        assertLocations(expected, parse("55,9486 -4,3148"))
        assertLocations(expected, parse("55,9486; -4,3148"))
        assertNotCoordinate("55,9,4,3")
    }

    @Test
    fun numbersWithoutDecimalsAreNotCoordinates() {
        assertNotCoordinate("12 34")
        assertNotCoordinate("55, 4")
        assertNotCoordinate("55.9 4")
        assertNotCoordinate("Main St 5.5")
        assertNotCoordinate("pharmacy")
        assertNotCoordinate("")
    }

    // Hemispheres

    @Test
    fun degreesMinutesSeconds() {
        assertSingle(50.0, 50.0, "50°00'00.0\"N 50°00'00.0\"E")
        assertSingle(55.948611, -4.314806, "55°56'55.0\"N 4°18'53.3\"W")
        assertSingle(-33.856944, 151.215278, "33°51'25\"S 151°12'55\"E")
    }

    @Test
    fun typographicSymbols() {
        assertSingle(55.948611, -4.314806, "55°56′55.0″N 4°18′53.3″W")
        assertSingle(55.948611, -4.314806, "55°56’55.0”N 4°18’53.3”W")
        assertSingle(55.948611, -4.314806, "55°56'55.0''N 4°18'53.3''W")
        assertSingle(55.948611, -4.314806, "55º56'55.0\"N 4º18'53.3\"W")
    }

    @Test
    fun longitudeFirst() {
        assertSingle(55.948611, -4.314806, "4°18'53.3\"W 55°56'55.0\"N")
    }

    @Test
    fun hemisphereBeforeTheNumbers() {
        assertSingle(55.948611, -4.314806, "N 55°56'55.0\" W 4°18'53.3\"")
        assertSingle(55.948611, -4.314806, "n55°56'55.0\", w4°18'53.3\"")
    }

    @Test
    fun decimalDegreesWithHemisphere() {
        assertSingle(55.9486, -4.3148, "55.9486° N, 4.3148° W")
        assertSingle(55.9486, -4.3148, "55.9486N 4.3148W")
        assertSingle(55.9486, -4.3148, "55,9486° N; 4,3148° W")
    }

    @Test
    fun degreesAndDecimalMinutes() {
        assertSingle(55.948617, -4.3148, "N 55° 56.917' W 004° 18.888'")
        assertSingle(55.948617, -4.3148, "N55 56.917 W4 18.888")
        assertSingle(55.948617, -4.3148, "55 56.917 N 4 18.888 W")
    }

    @Test
    fun invalidHemispheres() {
        assertNotCoordinate("50°N 60°N")
        assertNotCoordinate("50°E 60°W")
        assertNotCoordinate("50°61'N 4°W")
        assertNotCoordinate("50°10'60\"N 4°W")
        assertNotCoordinate("91°N 0°E")
        assertNotCoordinate("50.5°30'N 4°W")
        assertNotCoordinate("North Street")
    }

    // Links

    @Test
    fun geoLinks() {
        assertSingle(55.9486, -4.3148, "geo:55.9486,-4.3148")
        assertSingle(55.9486, -4.3148, "geo://55.9486,-4.3148")
        assertSingle(55.9486, -4.3148, "geo:55.9486,-4.3148?z=17")
        assertSingle(55.9486, -4.3148, "GEO:55.9486,-4.3148")
    }

    @Test
    fun googleLinks() {
        assertSingle(55.9486, -4.3148, "https://maps.google.com/?q=55.9486,-4.3148")
        assertSingle(55.9486, -4.3148, "https://www.google.com/maps?q=55.9486%2C-4.3148")
        assertSingle(55.9486, -4.3148, "https://www.google.com/maps/search/?api=1&query=55.9486,+-4.3148")
        assertSingle(55.9486, -4.3148, "https://www.google.com/maps/@55.9486,-4.3148,17z")
        assertSingle(55.9486, -4.3148, "https://www.google.com/maps/place/Somewhere/@55.9486,-4.3148,17z/data=!3m1")
    }

    @Test
    fun appleLinks() {
        assertSingle(55.9486, -4.3148, "https://maps.apple.com/?ll=55.9486,-4.3148&q=Pin")
        assertSingle(55.9486, -4.3148, "https://maps.apple.com/?q=55.9486,-4.3148")
    }

    @Test
    fun openStreetMapLinks() {
        assertSingle(55.9486, -4.3148, "https://www.openstreetmap.org/#map=17/55.9486/-4.3148")
        assertSingle(55.9486, -4.3148, "https://www.openstreetmap.org/?mlat=55.9486&mlon=-4.3148#map=17/55.9/-4.3")
    }

    @Test
    fun linkWithoutCoordinates() {
        assertNotCoordinate("https://www.example.com/")
        assertNotCoordinate("https://maps.google.com/?q=Glasgow+Central")
    }

    // Plus codes

    private val milngavie = LngLatAlt(-4.3148, 55.9486)

    @Test
    fun fullPlusCode() {
        val code = OpenLocationCode.encode(milngavie.latitude, milngavie.longitude)
        val area = OpenLocationCode.decode(code)
        assertSingle(area.centerLatitude, area.centerLongitude, code)
        assertSingle(area.centerLatitude, area.centerLongitude, code.lowercase())
        // Within the ~14m square of a 10 digit code
        assertLocations(listOf(milngavie.latitude to milngavie.longitude), parse(code), 1.5e-4)
    }

    @Test
    fun shortPlusCodeIsCompletedNearTheUser() {
        val code = OpenLocationCode.encode(milngavie.latitude, milngavie.longitude)
        val area = OpenLocationCode.decode(code)
        val short = code.substring(4)
        assertSingle(area.centerLatitude, area.centerLongitude, short)
        assertSingle(area.centerLatitude, area.centerLongitude, "$short Milngavie")
        assertSingle(area.centerLatitude, area.centerLongitude, "$short, Milngavie, UK")
    }

    @Test
    fun invalidPlusCodes() {
        assertNotCoordinate("22+")
        assertNotCoordinate("ABCD+EF")
        assertNotCoordinate("9C7QWMXQ+2V+3")
        assertNotCoordinate("C++")
    }

    @Test
    fun formattedCoordinate() {
        assertEquals("55.9486, -4.3148", formatCoordinate(LngLatAlt(-4.3148, 55.9486)))
        assertEquals("-0.000001, 0", formatCoordinate(LngLatAlt(0.0, -0.000001)))
        assertEquals("50, 50.123457", formatCoordinate(LngLatAlt(50.1234567, 50.0)))
    }
}
