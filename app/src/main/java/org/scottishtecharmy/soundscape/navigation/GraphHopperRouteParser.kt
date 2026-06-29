package org.scottishtecharmy.soundscape.navigation

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

data class NavigationRoute(
    val distanceMeters: Double,
    val durationMillis: Long,
    val geometry: List<NavigationPoint>,
    val instructions: List<NavigationInstruction>
)

data class NavigationPoint(
    val latitude: Double,
    val longitude: Double
)

data class NavigationInstruction(
    val text: String,
    val streetName: String,
    val distanceMeters: Double,
    val durationMillis: Long,
    val sign: Int,
    val geometryStartIndex: Int,
    val geometryEndIndex: Int
)

object GraphHopperRouteParser {
    fun parse(routeJson: String): NavigationRoute {
        val root = Json.parseToJsonElement(routeJson).jsonObject
        val path = root.requiredArray("paths").firstOrNull()?.jsonObject
            ?: throw IllegalArgumentException("GraphHopper route response does not contain a path")

        return NavigationRoute(
            distanceMeters = path.requiredDouble("distance"),
            durationMillis = path.requiredLong("time"),
            geometry = parseGeometry(path.requiredObject("points")),
            instructions = path.requiredArray("instructions").map { instruction ->
                parseInstruction(instruction.jsonObject)
            }
        )
    }

    private fun parseGeometry(points: JsonObject): List<NavigationPoint> {
        return points.requiredArray("coordinates").map { coordinate ->
            val values = coordinate.jsonArray
            if (values.size < 2) {
                throw IllegalArgumentException("GraphHopper coordinate must include longitude and latitude")
            }
            NavigationPoint(
                longitude = values[0].jsonPrimitive.double,
                latitude = values[1].jsonPrimitive.double
            )
        }
    }

    private fun parseInstruction(instruction: JsonObject): NavigationInstruction {
        val interval = instruction.requiredArray("interval")
        if (interval.size < 2) {
            throw IllegalArgumentException("GraphHopper instruction interval must include start and end indexes")
        }

        return NavigationInstruction(
            text = instruction.requiredString("text"),
            streetName = instruction.optionalString("street_name"),
            distanceMeters = instruction.requiredDouble("distance"),
            durationMillis = instruction.requiredLong("time"),
            sign = instruction.requiredInt("sign"),
            geometryStartIndex = interval[0].jsonPrimitive.int,
            geometryEndIndex = interval[1].jsonPrimitive.int
        )
    }

    private fun JsonObject.requiredArray(name: String): JsonArray {
        return this[name]?.jsonArray
            ?: throw IllegalArgumentException("GraphHopper route response is missing '$name'")
    }

    private fun JsonObject.requiredObject(name: String): JsonObject {
        return this[name]?.jsonObject
            ?: throw IllegalArgumentException("GraphHopper route response is missing '$name'")
    }

    private fun JsonObject.requiredString(name: String): String {
        return this[name]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("GraphHopper route response is missing '$name'")
    }

    private fun JsonObject.optionalString(name: String): String {
        return this[name]?.jsonPrimitive?.content.orEmpty()
    }

    private fun JsonObject.requiredDouble(name: String): Double {
        return this[name]?.jsonPrimitive?.double
            ?: throw IllegalArgumentException("GraphHopper route response is missing '$name'")
    }

    private fun JsonObject.requiredLong(name: String): Long {
        return this[name]?.jsonPrimitive?.long
            ?: throw IllegalArgumentException("GraphHopper route response is missing '$name'")
    }

    private fun JsonObject.requiredInt(name: String): Int {
        return this[name]?.jsonPrimitive?.int
            ?: throw IllegalArgumentException("GraphHopper route response is missing '$name'")
    }
}
