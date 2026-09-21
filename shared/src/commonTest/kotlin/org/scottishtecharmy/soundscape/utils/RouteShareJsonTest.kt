package org.scottishtecharmy.soundscape.utils

import org.scottishtecharmy.soundscape.database.local.model.MarkerEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RouteShareJsonTest {

    private fun marker(
        markerId: Long,
        name: String,
        longitude: Double,
        latitude: Double,
        fullAddress: String = "",
    ) = MarkerEntity(
        markerId = markerId,
        name = name,
        longitude = longitude,
        latitude = latitude,
        fullAddress = fullAddress,
    )

    // ----- multiple markers -----

    @Test
    fun routeToShareJson_withMultipleMarkers_producesCorrectFieldsAndOrder() {
        val route = RouteWithMarkers(
            route = RouteEntity(routeId = 42, name = "Test Route", description = "A nice route"),
            markers = listOf(
                marker(markerId = 1, name = "Start", longitude = -4.25, latitude = 55.86, fullAddress = "123 Main St"),
                marker(markerId = 2, name = "End", longitude = -4.3, latitude = 55.9, fullAddress = "456 Oak Ave"),
            ),
        )

        val json = routeToShareJson(route)
        val root = Json.parseToJsonElement(json).jsonObject

        assertEquals("Test Route", root["name"]!!.jsonPrimitive.content)
        assertEquals("42", root["id"]!!.jsonPrimitive.content)
        assertEquals("A nice route", root["routeDescription"]!!.jsonPrimitive.content)

        val waypoints = root["waypoints"]!!.jsonArray
        assertEquals(2, waypoints.size)

        // First waypoint corresponds to the first marker, in list order.
        val waypoint0 = waypoints[0].jsonObject
        assertEquals(0, waypoint0["index"]!!.jsonPrimitive.int)
        assertEquals("1", waypoint0["markerId"]!!.jsonPrimitive.content)
        val marker0 = waypoint0["marker"]!!.jsonObject
        assertEquals("Start", marker0["nickname"]!!.jsonPrimitive.content)
        assertEquals("1", marker0["id"]!!.jsonPrimitive.content)
        assertEquals("123 Main St", marker0["estimatedAddress"]!!.jsonPrimitive.content)
        val location0 = marker0["location"]!!.jsonObject
        assertEquals("Start", location0["name"]!!.jsonPrimitive.content)
        val coordinate0 = location0["coordinate"]!!.jsonObject
        assertEquals(55.86, coordinate0["latitude"]!!.jsonPrimitive.double)
        assertEquals(-4.25, coordinate0["longitude"]!!.jsonPrimitive.double)

        // Second waypoint corresponds to the second marker.
        val waypoint1 = waypoints[1].jsonObject
        assertEquals(1, waypoint1["index"]!!.jsonPrimitive.int)
        assertEquals("2", waypoint1["markerId"]!!.jsonPrimitive.content)
        val marker1 = waypoint1["marker"]!!.jsonObject
        assertEquals("End", marker1["nickname"]!!.jsonPrimitive.content)
        assertEquals("2", marker1["id"]!!.jsonPrimitive.content)
        assertEquals("456 Oak Ave", marker1["estimatedAddress"]!!.jsonPrimitive.content)
        val location1 = marker1["location"]!!.jsonObject
        assertEquals("End", location1["name"]!!.jsonPrimitive.content)
        val coordinate1 = location1["coordinate"]!!.jsonObject
        assertEquals(55.9, coordinate1["latitude"]!!.jsonPrimitive.double)
        assertEquals(-4.3, coordinate1["longitude"]!!.jsonPrimitive.double)
    }

    // ----- zero markers -----

    @Test
    fun routeToShareJson_withZeroMarkers_producesEmptyWaypointsArray() {
        val route = RouteWithMarkers(
            route = RouteEntity(routeId = 7, name = "Empty Route", description = "No stops yet"),
            markers = emptyList(),
        )

        val json = routeToShareJson(route)
        val root = Json.parseToJsonElement(json).jsonObject

        assertEquals("Empty Route", root["name"]!!.jsonPrimitive.content)
        assertEquals("7", root["id"]!!.jsonPrimitive.content)
        assertTrue(root["waypoints"]!!.jsonArray.isEmpty())
    }

    // ----- characters that used to break naive string interpolation -----
    //
    // routeToShareJson now builds JSON via kotlinx.serialization's JSON DSL,
    // which escapes free-text fields (route/marker name, description,
    // address) automatically. These tests pin that a `"` or `\` in
    // user-entered text round-trips correctly instead of corrupting the
    // emitted JSON.

    @Test
    fun routeToShareJson_withDoubleQuoteInRouteName_producesValidJsonWithContentPreserved() {
        val route = RouteWithMarkers(
            route = RouteEntity(
                routeId = 1,
                name = "Alice's \"Cool\" Route",
                description = "fine",
            ),
            markers = emptyList(),
        )

        val json = routeToShareJson(route)
        val root = Json.parseToJsonElement(json).jsonObject

        assertEquals("Alice's \"Cool\" Route", root["name"]!!.jsonPrimitive.content)
    }

    @Test
    fun routeToShareJson_withBackslashInDescription_producesValidJsonWithContentPreserved() {
        val route = RouteWithMarkers(
            route = RouteEntity(
                routeId = 1,
                name = "fine",
                description = "C:\\Users\\test",
            ),
            markers = emptyList(),
        )

        val json = routeToShareJson(route)
        val root = Json.parseToJsonElement(json).jsonObject

        assertEquals("C:\\Users\\test", root["routeDescription"]!!.jsonPrimitive.content)
    }
}
