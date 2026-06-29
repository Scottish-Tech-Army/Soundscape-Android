package org.scottishtecharmy.soundscape.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class GraphHopperRouteParserTest {
    @Test
    fun parsesUnencodedRouteGeometryAndInstructions() {
        val route = GraphHopperRouteParser.parse(
            """
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
                      "heading": 180.0,
                      "sign": 0,
                      "interval": [0, 1],
                      "text": "Continue",
                      "time": 10000,
                      "street_name": "Rue des Iris"
                    },
                    {
                      "distance": 30.0,
                      "sign": 2,
                      "interval": [1, 2],
                      "text": "Turn right onto Avenue Saint-Martin",
                      "time": 20000,
                      "street_name": "Avenue Saint-Martin"
                    }
                  ]
                }
              ]
            }
            """.trimIndent()
        )

        assertEquals(42.5, route.distanceMeters, 0.0)
        assertEquals(30000, route.durationMillis)
        assertEquals(3, route.geometry.size)
        assertEquals(43.73841, route.geometry.first().latitude, 0.0)
        assertEquals(7.42461, route.geometry.first().longitude, 0.0)
        assertEquals(2, route.instructions.size)
        assertEquals("Continue", route.instructions.first().text)
        assertEquals("Rue des Iris", route.instructions.first().streetName)
        assertEquals(0, route.instructions.first().geometryStartIndex)
        assertEquals(1, route.instructions.first().geometryEndIndex)
    }
}
