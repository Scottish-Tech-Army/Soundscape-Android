package org.scottishtecharmy.soundscape.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class GraphHopperRouteProviderTest {
    @Test
    fun fetchesRouteJsonAndParsesNavigationRoute() {
        val fetcher = CapturingRouteResponseFetcher(sampleRouteJson())
        val provider = GraphHopperRouteProvider(
            baseUrl = "http://127.0.0.1:8989",
            responseFetcher = fetcher
        )

        val route = provider.route(
            GraphHopperRouteRequest(
                start = NavigationPoint(latitude = 43.7384, longitude = 7.4246),
                destination = NavigationPoint(latitude = 43.7339, longitude = 7.4213)
            )
        )

        assertEquals(
            "http://127.0.0.1:8989/route?profile=foot&point=43.7384,7.4246" +
                "&point=43.7339,7.4213&locale=en&instructions=true" +
                "&points_encoded=false&ch.disable=true",
            fetcher.requestedUrl
        )
        assertEquals(42.5, route.distanceMeters, 0.0)
        assertEquals(2, route.instructions.size)
        assertEquals("Turn right", route.instructions[1].text)
    }

    private class CapturingRouteResponseFetcher(
        private val response: String
    ) : GraphHopperRouteResponseFetcher {
        var requestedUrl: String? = null

        override fun fetch(url: String): String {
            requestedUrl = url
            return response
        }
    }

    private fun sampleRouteJson(): String {
        return """
            {
              "paths": [
                {
                  "distance": 42.5,
                  "time": 30000,
                  "points": {
                    "type": "LineString",
                    "coordinates": [
                      [7.42461, 43.73841],
                      [7.42480, 43.73810],
                      [7.42500, 43.73790]
                    ]
                  },
                  "instructions": [
                    {
                      "distance": 12.5,
                      "sign": 0,
                      "interval": [0, 1],
                      "text": "Continue",
                      "time": 10000,
                      "street_name": ""
                    },
                    {
                      "distance": 30.0,
                      "sign": 2,
                      "interval": [1, 2],
                      "text": "Turn right",
                      "time": 20000,
                      "street_name": "Avenue Saint-Martin"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()
    }
}
