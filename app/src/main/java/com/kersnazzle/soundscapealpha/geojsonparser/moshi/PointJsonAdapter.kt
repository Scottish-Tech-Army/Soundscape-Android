package com.kersnazzle.soundscapealpha.geojsonparser.moshi

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.LngLatAlt
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.Point
import com.kersnazzle.soundscapealpha.geojsonparser.moshi.GeoJsonObjectMoshiAdapter.Companion.OPTIONS


class PointJsonAdapter : JsonAdapter<Point>() {
    private val defaultAdapter = GeoJsonObjectMoshiAdapter()
    private val positionJsonAdapter = LngLatAltMoshiAdapter()

    @FromJson
    override fun fromJson(reader: JsonReader): Point {
        val point = Point()
        var type = ""
        var position: LngLatAlt? = null

        reader.beginObject()
        while (reader.hasNext()) {
            when (val index = reader.selectName(JsonReader.Options.of(*OPTIONS))) {
                0 -> type = reader.nextString()
                1 -> position = positionJsonAdapter.fromJson(reader)
                else -> {
                    defaultAdapter.readDefault(point, index, reader)
                }
            }
        }
        reader.endObject()

        if (position == null) {
            throw JsonDataException("Required positions are missing at ${reader.path}")
        }

        if (type != "Point") {
            throw JsonDataException("Required type is not a Point at ${reader.path}")
        }

        point.coordinates = position
        return point
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: Point?) {
        if (value == null) {
            throw NullPointerException("Point was null! Wrap in .nullSafe() to write nullable values.")
        }

        writer.beginObject()
        writer.name("coordinates")
        positionJsonAdapter.toJson(writer, value.coordinates)
        defaultAdapter.writeDefault(value, writer)
        writer.endObject()
    }
}