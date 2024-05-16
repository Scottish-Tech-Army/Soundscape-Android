package com.kersnazzle.soundscapealpha.geojsonparser.moshi

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.Feature
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.FeatureCollection
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.GeoJsonObject
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.GeometryCollection
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.LineString
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.MultiLineString
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.MultiPoint
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.MultiPolygon
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.Point
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.Polygon
import java.util.HashMap

open class GeoJsonObjectMoshiAdapter() : JsonAdapter<GeoJsonObject>() {
    companion object {
        private val types = mapOf<String, JsonAdapter<out GeoJsonObject>>(
            "Feature" to FeatureJsonAdapter(),
            "FeatureCollection" to FeatureCollectionJsonAdapter(),
            "GeometryCollection" to GeometryCollectionJsonAdapter(),
            "LineString" to LineStringJsonAdapter(),
            "MultiLineString" to MultiLineStringJsonAdapter(),
            "Point" to PointJsonAdapter(),
            "MultiPoint" to MultiPointJsonAdapter(),
            "Polygon" to PolygonJsonAdapter(),
            "MultiPolygon" to MultiPolygonJsonAdapter()
        )

        val OPTIONS = arrayOf("type", "coordinates", "bbox", "properties")
    }

    override fun fromJson(reader: JsonReader): GeoJsonObject? {
        var type = ""
        val dataReader = reader.peekJson()

        dataReader.beginObject()
        while (dataReader.hasNext()) {
            when (dataReader.selectName(JsonReader.Options.of("type"))) {
                0 -> type = dataReader.nextString()
                -1 -> {
                    dataReader.skipName()
                    dataReader.skipValue()
                }
            }
        }
        dataReader.endObject()

        return types[type]?.fromJson(reader) as GeoJsonObject?
    }

    override fun toJson(writer: JsonWriter, value: GeoJsonObject?) {
        if (value == null) {
            throw NullPointerException("GeoJsonObject was null! Wrap in .nullSafe() to write nullable values.")
        }

        when (value.type) {
            "Point" -> (types["Point"] as PointJsonAdapter).toJson(writer, value as Point)
            "Feature" -> (types["Feature"] as FeatureJsonAdapter).toJson(writer, value as Feature)
            "FeatureCollection" -> (types["FeatureCollection"] as FeatureCollectionJsonAdapter).toJson(
                writer,
                value as FeatureCollection
            )

            "GeometryCollection" -> (types["GeometryCollection"] as GeometryCollectionJsonAdapter).toJson(
                writer,
                value as GeometryCollection
            )

            "LineString" -> (types["LineString"] as LineStringJsonAdapter).toJson(
                writer,
                value as LineString
            )

            "MultiLineString" -> (types["MultiLineString"] as MultiLineStringJsonAdapter).toJson(
                writer,
                value as MultiLineString
            )

            "MultiPoint" -> (types["MultiPoint"] as MultiPointJsonAdapter).toJson(
                writer,
                value as MultiPoint
            )

            "Polygon" -> (types["Polygon"] as PolygonJsonAdapter).toJson(writer, value as Polygon)
            "MultiPolygon" -> (types["MultiPolygon"] as MultiPolygonJsonAdapter).toJson(
                writer,
                value as MultiPolygon
            )
        }
    }

    public fun readDefault(outObj: GeoJsonObject, paramIndex: Int, reader: JsonReader) {
        when (paramIndex) {
            2 -> outObj.bbox = reader.readJsonValue() as List<Double>?
            3 -> {
                (reader.readJsonValue() as? Map<String, Any?>)?.let {
                    outObj.properties = HashMap(it)
                }
            }

            -1 -> {
                outObj.foreign = outObj.foreign ?: HashMap<String, Any?>()

                if (reader.peek() == JsonReader.Token.NAME) {
                    val name = reader.nextName()
                    val value = reader.readJsonValue()
                    outObj.foreign!![name] = value
                } else {
                    reader.skipValue()
                }
            }
        }
    }

    public fun writeDefault(inObj: GeoJsonObject, writer: JsonWriter) {
        inObj.bbox?.let {
            writer.name("bbox")
            writer.beginArray()
            it.forEach { coord -> writer.value(coord as Double) }
            writer.endArray()
        }

        inObj.foreign?.forEach { k, v ->
            writer.name(k)
            writeUnknown(v, writer)
        }

        if (inObj.properties != null) {
            writer.name("properties")
            writer.beginObject()
            inObj.properties?.forEach { k, v ->
                writer.name(k)
                writeUnknown(v, writer)
            }
            writer.endObject()
        }

        writer.name("type")
        writer.value(inObj.type)
    }

    public fun writeUnknown(value: Any?, writer: JsonWriter) {
        when (value) {
            is String -> writer.value(value as String)
            is Double -> writer.value(value as Double)
            is Int -> writer.value(value as Int)
            is Boolean -> writer.value(value as Boolean)
            is Map<*, *> -> writeMap(value as Map<*, *>, writer)
            is Collection<*> -> {
                writer.beginArray()
                value.forEach { writeUnknown(it, writer) }
                writer.endArray()
            }

            else -> writer.value(null as String?)
        }
    }

    public fun writeMap(map: Map<*, *>, writer: JsonWriter) {
        writer.beginObject()
        map.forEach { k, v ->
            writer.name(k as String)
            writeUnknown(v, writer)
        }
        writer.endObject()
    }
}