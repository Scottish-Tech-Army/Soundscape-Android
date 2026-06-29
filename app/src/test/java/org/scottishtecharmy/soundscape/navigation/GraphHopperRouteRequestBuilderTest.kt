package org.scottishtecharmy.soundscape.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class GraphHopperRouteRequestBuilderTest {
    @Test
    fun buildsWalkingRouteUrlForUnencodedInstructions() {
        val url = GraphHopperRouteRequestBuilder.buildRouteUrl(
            baseUrl = "http://127.0.0.1:8989/",
            request = GraphHopperRouteRequest(
                start = NavigationPoint(latitude = 43.7384, longitude = 7.4246),
                destination = NavigationPoint(latitude = 43.7339, longitude = 7.4213),
                locale = "en"
            )
        )

        assertEquals(
            "http://127.0.0.1:8989/route?profile=foot&point=43.7384,7.4246" +
                "&point=43.7339,7.4213&locale=en&instructions=true" +
                "&points_encoded=false&ch.disable=true",
            url
        )
    }

    @Test
    fun trimsBaseUrlBeforeAppendingRoutePath() {
        val url = GraphHopperRouteRequestBuilder.buildRouteUrl(
            baseUrl = "http://127.0.0.1:8989",
            request = GraphHopperRouteRequest(
                start = NavigationPoint(latitude = 55.0, longitude = -4.0),
                destination = NavigationPoint(latitude = 55.1, longitude = -4.1)
            )
        )

        assertEquals(
            "http://127.0.0.1:8989/route?profile=foot&point=55.0,-4.0" +
                "&point=55.1,-4.1&locale=en&instructions=true" +
                "&points_encoded=false&ch.disable=true",
            url
        )
    }
}
