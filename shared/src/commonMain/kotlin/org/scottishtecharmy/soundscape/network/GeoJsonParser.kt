package org.scottishtecharmy.soundscape.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeoJsonObject
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiLineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiPoint
import org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiPolygon
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon

/**
 * KMP-compatible GeoJSON parser using kotlinx.serialization.json.
 * Replaces the JVM-only Moshi-based GeoMoshi parser for shared code.
 */
object GeoJsonParser {

    private val json = Json { ignoreUnknownKeys = true }

    fun parseFeatureCollection(jsonString: String): FeatureCollection? {
        return try {
            val root = json.parseToJsonElement(jsonString).jsonObject
            parseFeatureCollectionObj(root)
        } catch (e: Exception) {
            println("GeoJsonParser: Failed to parse: ${e.message}")
            null
        }
    }

    /**
     * Parse a single GeoJSON `Feature` - the shape written by [toJson], used for the metadata
     * sidecar stored alongside a downloaded extract.
     */
    fun parseFeature(jsonString: String): Feature? {
        return try {
            parseFeature(json.parseToJsonElement(jsonString).jsonObject)
        } catch (e: Exception) {
            println("GeoJsonParser: Failed to parse feature: ${e.message}")
            null
        }
    }

    /**
     * Serialize a [Feature] back to GeoJSON. Round-trips through [parseFeature], so the
     * properties it emits are the JSON primitives, arrays and objects that [parseFeature]
     * produces; anything else is written as its string form.
     */
    fun toJson(feature: Feature): String =
        json.encodeToString(JsonObject.serializer(), featureToJsonObject(feature))

    private fun featureToJsonObject(feature: Feature): JsonObject =
        buildJsonObject {
            put("type", "Feature")
            feature.id?.let { put("id", it) }
            put("geometry", geometryToJson(feature.geometry) ?: JsonNull)
            put("properties", propertiesToJson(feature.properties))
        }

    private fun propertiesToJson(properties: Map<String, Any?>?): JsonObject =
        buildJsonObject {
            for ((key, value) in properties.orEmpty()) {
                put(key, valueToJson(value))
            }
        }

    private fun valueToJson(value: Any?): JsonElement = when (value) {
        null -> JsonNull
        is JsonElement -> value
        is Boolean -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value)
        is String -> JsonPrimitive(value)
        is List<*> -> JsonArray(value.map { valueToJson(it) })
        is Map<*, *> -> buildJsonObject {
            for ((k, v) in value) put(k.toString(), valueToJson(v))
        }

        else -> JsonPrimitive(value.toString())
    }

    private fun geometryToJson(geometry: GeoJsonObject): JsonObject? = when (geometry) {
        is Point -> geometryObject("Point", positionToJson(geometry.coordinates))
        is LineString -> geometryObject("LineString", positionsToJson(geometry.coordinates))
        is MultiPoint -> geometryObject("MultiPoint", positionsToJson(geometry.coordinates))
        is Polygon -> geometryObject("Polygon", ringsToJson(geometry.coordinates))
        is MultiLineString -> geometryObject("MultiLineString", ringsToJson(geometry.coordinates))
        is MultiPolygon -> geometryObject(
            "MultiPolygon",
            JsonArray(geometry.coordinates.map { ringsToJson(it) }),
        )

        else -> null
    }

    private fun geometryObject(type: String, coordinates: JsonElement): JsonObject =
        buildJsonObject {
            put("type", type)
            put("coordinates", coordinates)
        }

    /**
     * Altitude is only emitted when it is actually carrying a value: [parseCoordinate] fills in
     * 0.0 for the two-element positions that the extract manifest uses, and writing that back out
     * would inflate every ring of every polygon by a third of its size for nothing.
     */
    private fun positionToJson(position: LngLatAlt): JsonArray {
        val altitude = position.altitude
        val values = if (altitude != null && altitude != 0.0 && !altitude.isNaN()) {
            listOf(position.longitude, position.latitude, altitude)
        } else {
            listOf(position.longitude, position.latitude)
        }
        return JsonArray(values.map { JsonPrimitive(it) })
    }

    private fun positionsToJson(positions: List<LngLatAlt>): JsonArray =
        JsonArray(positions.map { positionToJson(it) })

    private fun ringsToJson(rings: List<List<LngLatAlt>>): JsonArray =
        JsonArray(rings.map { positionsToJson(it) })

    private fun parseFeatureCollectionObj(obj: JsonObject): FeatureCollection {
        val fc = FeatureCollection()
        val featuresArray = obj["features"]?.jsonArray ?: return fc
        for (element in featuresArray) {
            val featureObj = element.jsonObject
            val feature = parseFeature(featureObj)
            if (feature != null) {
                fc.addFeature(feature)
            }
        }
        return fc
    }

    private fun parseFeature(obj: JsonObject): Feature? {
        val feature = Feature()

        // Parse geometry
        val geometryObj = obj["geometry"]?.jsonObject ?: return null
        val geometry = parseGeometry(geometryObj) ?: return null
        feature.geometry = geometry

        // Parse properties
        val propsObj = obj["properties"]?.jsonObject
        if (propsObj != null) {
            val props = HashMap<String, Any?>()
            for ((key, value) in propsObj) {
                props[key] = propertyValue(value)
            }
            feature.properties = props
        }

        // Parse id
        obj["id"]?.let { idElement ->
            try {
                feature.id = idElement.jsonPrimitive.content
            } catch (_: Exception) {
            }
        }

        return feature
    }

    /**
     * Property values are kept as the strings the rest of the app expects for primitives, but
     * arrays are unpacked into a List. The manifest uses arrays for the city lists of a
     * city_cluster extract, and ExtractDetails only renders those when it is handed a List -
     * flattening them to their JSON text meant the cities were silently dropped.
     */
    private fun propertyValue(value: JsonElement): Any? = when (value) {
        is JsonArray -> value.map { propertyValue(it) }
        is JsonObject -> value.mapValues { (_, v) -> propertyValue(v) }
        else -> try {
            value.jsonPrimitive.content
        } catch (_: Exception) {
            value.toString()
        }
    }

    private fun parseGeometry(obj: JsonObject): GeoJsonObject? {
        val type = obj["type"]?.jsonPrimitive?.content ?: return null
        return when (type) {
            "Point" -> parsePoint(obj)
            "LineString" -> parseLineString(obj)
            "Polygon" -> parsePolygon(obj)
            "MultiPoint" -> parseMultiPoint(obj)
            "MultiLineString" -> parseMultiLineString(obj)
            "MultiPolygon" -> parseMultiPolygon(obj)
            else -> null
        }
    }

    private fun parseCoordinate(arr: JsonArray): LngLatAlt {
        val lng = arr[0].jsonPrimitive.double
        val lat = arr[1].jsonPrimitive.double
        val alt = if (arr.size > 2) arr[2].jsonPrimitive.double else 0.0
        return LngLatAlt(lng, lat, alt)
    }

    private fun parsePoint(obj: JsonObject): Point? {
        val coords = obj["coordinates"]?.jsonArray ?: return null
        val lngLatAlt = parseCoordinate(coords)
        return Point(lngLatAlt.longitude, lngLatAlt.latitude, lngLatAlt.altitude)
    }

    private fun parseLineString(obj: JsonObject): LineString? {
        val coords = obj["coordinates"]?.jsonArray ?: return null
        val lineString = LineString()
        for (coord in coords) {
            lineString.coordinates.add(parseCoordinate(coord.jsonArray))
        }
        return lineString
    }

    private fun parsePolygon(obj: JsonObject): Polygon? {
        val coords = obj["coordinates"]?.jsonArray ?: return null
        val polygon = Polygon()
        for (ring in coords) {
            val ringCoords = arrayListOf<LngLatAlt>()
            for (coord in ring.jsonArray) {
                ringCoords.add(parseCoordinate(coord.jsonArray))
            }
            polygon.coordinates.add(ringCoords)
        }
        return polygon
    }

    private fun parseMultiPoint(obj: JsonObject): MultiPoint? {
        val coords = obj["coordinates"]?.jsonArray ?: return null
        val multiPoint = MultiPoint()
        for (coord in coords) {
            multiPoint.coordinates.add(parseCoordinate(coord.jsonArray))
        }
        return multiPoint
    }

    private fun parseMultiLineString(obj: JsonObject): MultiLineString? {
        val coords = obj["coordinates"]?.jsonArray ?: return null
        val multiLineString = MultiLineString()
        for (line in coords) {
            val lineCoords = arrayListOf<LngLatAlt>()
            for (coord in line.jsonArray) {
                lineCoords.add(parseCoordinate(coord.jsonArray))
            }
            multiLineString.coordinates.add(lineCoords)
        }
        return multiLineString
    }

    private fun parseMultiPolygon(obj: JsonObject): MultiPolygon? {
        val coords = obj["coordinates"]?.jsonArray ?: return null
        val multiPolygon = MultiPolygon()
        for (polygon in coords) {
            val rings = arrayListOf<ArrayList<LngLatAlt>>()
            for (ring in polygon.jsonArray) {
                val ringCoords = arrayListOf<LngLatAlt>()
                for (coord in ring.jsonArray) {
                    ringCoords.add(parseCoordinate(coord.jsonArray))
                }
                rings.add(ringCoords)
            }
            multiPolygon.coordinates.add(rings)
        }
        return multiPolygon
    }
}
