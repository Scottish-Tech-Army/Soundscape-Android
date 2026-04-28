package org.scottishtecharmy.soundscape.intents

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IntentParserTest {

    @Test
    fun geoUrlReturnsOpenLatLon() {
        val result = IntentParser.parseUrl("geo:55.9533,-3.1883")
        assertTrue(result is IncomingIntent.OpenLatLon)
        assertEquals(55.9533, result.latitude)
        assertEquals(-3.1883, result.longitude)
    }

    @Test
    fun geoUrlWithLeadingSlashes() {
        val result = IntentParser.parseUrl("geo://55.9533,-3.1883")
        assertTrue(result is IncomingIntent.OpenLatLon)
        assertEquals(55.9533, result.latitude)
        assertEquals(-3.1883, result.longitude)
    }

    @Test
    fun soundscapeCoordUrlReturnsOpenLatLon() {
        val result = IntentParser.parseUrl("soundscape:55.9533,-3.1883")
        assertTrue(result is IncomingIntent.OpenLatLon)
        assertEquals(55.9533, result.latitude)
        assertEquals(-3.1883, result.longitude)
    }

    @Test
    fun soundscapeCoordUrlPercentEncodedComma() {
        // Share Extension generates URLs with URLComponents which percent-encodes the comma.
        val result = IntentParser.parseUrl("soundscape://55.939774610297796%2C-4.315596520900726")
        assertTrue(result is IncomingIntent.OpenLatLon)
        assertEquals(55.939774610297796, result.latitude)
        assertEquals(-4.315596520900726, result.longitude)
    }

    @Test
    fun soundscapeLocationHostUrl() {
        val result = IntentParser.parseUrl(
            "soundscape://location?lat=-12.045448&lon=-77.030864&name=Lima"
        )
        assertTrue(result is IncomingIntent.OpenLatLon)
        assertEquals(-12.045448, result.latitude)
        assertEquals(-77.030864, result.longitude)
        assertEquals("Lima", result.displayName)
    }

    @Test
    fun soundscapeLocationHostMissingCoords() {
        assertNull(IntentParser.parseUrl("soundscape://location?name=NoCoords"))
    }

    @Test
    fun soundscapeCoordUrlWithNameQuery() {
        val result = IntentParser.parseUrl("soundscape://55.9533%2C-3.1883?name=Edinburgh%20Castle")
        assertTrue(result is IncomingIntent.OpenLatLon)
        assertEquals(55.9533, result.latitude)
        assertEquals(-3.1883, result.longitude)
        assertEquals("Edinburgh Castle", result.displayName)
    }

    @Test
    fun soundscapeFeatureRoutes() {
        val result = IntentParser.parseUrl("soundscape://feature/routes")
        assertTrue(result is IncomingIntent.OpenFeature)
        assertEquals("routes", result.tab)
    }

    @Test
    fun soundscapeFeatureMarkers() {
        val result = IntentParser.parseUrl("soundscape://feature/markers")
        assertTrue(result is IncomingIntent.OpenFeature)
        assertEquals("markers", result.tab)
    }

    @Test
    fun soundscapeFeatureUnknownReturnsNull() {
        assertNull(IntentParser.parseUrl("soundscape://feature/bogus"))
    }

    @Test
    fun soundscapeRouteStop() {
        assertEquals(IncomingIntent.StopRoute, IntentParser.parseUrl("soundscape://route/stop"))
    }

    @Test
    fun soundscapeRouteByName() {
        val result = IntentParser.parseUrl("soundscape://route/Westerton")
        assertTrue(result is IncomingIntent.StartRouteByName)
        assertEquals("Westerton", result.name)
    }

    @Test
    fun soundscapeRouteByNamePercentDecoded() {
        val result = IntentParser.parseUrl("soundscape://route/My%20Walk")
        assertTrue(result is IncomingIntent.StartRouteByName)
        assertEquals("My Walk", result.name)
    }

    @Test
    fun shareMarkerUniversalLink() {
        val result = IntentParser.parseUrl(
            "https://links.soundscape.scottishtecharmy.org/v1/sharemarker?lat=55.9533&lon=-3.1883&name=Edinburgh"
        )
        assertTrue(result is IncomingIntent.OpenLocation)
        assertEquals("Edinburgh", result.locationDescription.name)
        assertEquals(55.9533, result.locationDescription.location.latitude)
        assertEquals(-3.1883, result.locationDescription.location.longitude)
    }

    @Test
    fun shareMarkerPrefersNicknameOverName() {
        val result = IntentParser.parseUrl(
            "https://links.soundscape.scottishtecharmy.org/v1/sharemarker?lat=55.9533&lon=-3.1883&nickname=Home&name=Edinburgh"
        )
        assertTrue(result is IncomingIntent.OpenLocation)
        assertEquals("Home", result.locationDescription.name)
    }

    @Test
    fun shareMarkerFallsBackToCoordsAsName() {
        val result = IntentParser.parseUrl(
            "https://links.soundscape.scottishtecharmy.org/v1/sharemarker?lat=55.9533&lon=-3.1883"
        )
        assertTrue(result is IncomingIntent.OpenLocation)
        assertEquals("55.9533,-3.1883", result.locationDescription.name)
    }

    @Test
    fun shareMarkerWrongHostReturnsNull() {
        assertNull(IntentParser.parseUrl("https://example.com/v1/sharemarker?lat=1&lon=2"))
    }

    @Test
    fun shareMarkerMissingCoordsReturnsNull() {
        assertNull(IntentParser.parseUrl("https://links.soundscape.scottishtecharmy.org/v1/sharemarker?name=Edinburgh"))
    }

    @Test
    fun unknownSchemeReturnsNull() {
        assertNull(IntentParser.parseUrl("ftp://example.com/foo"))
    }

    @Test
    fun parseRouteJsonSoundscapeIosShape() {
        val json = """
            {
              "name": "Test Walk",
              "id": "0",
              "routeDescription": "",
              "waypoints": [
                {
                  "marker": {
                    "nickname": "Cafe",
                    "location": {
                      "name": "Cafe",
                      "coordinate": { "latitude": 55.9533, "longitude": -3.1883 }
                    },
                    "estimatedAddress": "1 Princes Street",
                    "id": "1"
                  },
                  "index": 0,
                  "markerId": "1"
                },
                {
                  "marker": {
                    "nickname": "Park",
                    "location": {
                      "name": "Park",
                      "coordinate": { "latitude": 55.9540, "longitude": -3.1900 }
                    }
                  },
                  "index": 1
                }
              ]
            }
        """.trimIndent()
        val result = IntentParser.parseRouteJson(json)
        assertNotNull(result)
        assertEquals("Test Walk", result.route.route.name)
        assertEquals(2, result.route.markers.size)
        assertEquals("Cafe", result.route.markers[0].name)
        assertEquals(55.9533, result.route.markers[0].latitude)
        assertEquals(-3.1883, result.route.markers[0].longitude)
        assertEquals("1 Princes Street", result.route.markers[0].fullAddress)
        // The second waypoint omits estimatedAddress — should default to empty string.
        assertEquals("", result.route.markers[1].fullAddress)
    }

    @Test
    fun parseRouteJsonRejectsOversize() {
        val payload = "x".repeat(1_000_001)
        assertNull(IntentParser.parseRouteJson(payload))
    }

    @Test
    fun parseRouteJsonRejectsMalformed() {
        assertNull(IntentParser.parseRouteJson("not json"))
    }

    @Test
    fun parseRouteJsonRejectsMissingName() {
        assertNull(IntentParser.parseRouteJson("""{"waypoints":[]}"""))
    }
}
