package org.scottishtecharmy.soundscape.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
                props[key] = try {
                    value.jsonPrimitive.content
                } catch (_: Exception) {
                    value.toString()
                }
            }
            feature.properties = props
        }

        // Parse id
        obj["id"]?.let { idElement ->
            try {
                feature.id = idElement.jsonPrimitive.content
            } catch (_: Exception) {}
        }

        return feature
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

    private fun parsePoint(obj: JsonObject): Point {
        val coords = obj["coordinates"]!!.jsonArray
        val lngLatAlt = parseCoordinate(coords)
        return Point(lngLatAlt.longitude, lngLatAlt.latitude, lngLatAlt.altitude)
    }

    private fun parseLineString(obj: JsonObject): LineString {
        val coords = obj["coordinates"]!!.jsonArray
        val lineString = LineString()
        for (coord in coords) {
            lineString.coordinates.add(parseCoordinate(coord.jsonArray))
        }
        return lineString
    }

    private fun parsePolygon(obj: JsonObject): Polygon {
        val coords = obj["coordinates"]!!.jsonArray
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

    private fun parseMultiPoint(obj: JsonObject): MultiPoint {
        val coords = obj["coordinates"]!!.jsonArray
        val multiPoint = MultiPoint()
        for (coord in coords) {
            multiPoint.coordinates.add(parseCoordinate(coord.jsonArray))
        }
        return multiPoint
    }

    private fun parseMultiLineString(obj: JsonObject): MultiLineString {
        val coords = obj["coordinates"]!!.jsonArray
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

    private fun parseMultiPolygon(obj: JsonObject): MultiPolygon {
        val coords = obj["coordinates"]!!.jsonArray
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
