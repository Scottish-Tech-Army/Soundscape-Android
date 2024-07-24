package org.scottishtecharmy.soundscape.geojsonparser.moshi

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.moshi.GeoJsonObjectMoshiAdapter.Companion.OPTIONS


class LineStringJsonAdapter : JsonAdapter<LineString>() {
    private val defaultAdapter = GeoJsonObjectMoshiAdapter()
    private val positionJsonAdapter = LngLatAltMoshiAdapter()

    @FromJson
    override fun fromJson(reader: JsonReader): LineString {
        val lineString = LineString()
        var type = ""
        var position: ArrayList<LngLatAlt>? = null

        reader.beginObject()
        while (reader.hasNext()) {
            when (val index = reader.selectName(JsonReader.Options.of(*OPTIONS))) {
                0 -> type = reader.nextString()
                1 -> {
                    position = arrayListOf()
                    reader.beginArray()
                    while (reader.hasNext()) {
                        position.add(positionJsonAdapter.fromJson(reader)!!)
                    }
                    reader.endArray()
                }

                else -> {
                    defaultAdapter.readDefault(lineString, index, reader)
                }
            }
        }
        reader.endObject()

        if (position == null || position.isEmpty()) {
            throw JsonDataException("Required positions are missing at ${reader.path}")
        }

        if (type != "LineString") {
            throw JsonDataException("Required type is not a LineString at ${reader.path}")
        }

        lineString.coordinates = position!!
        return lineString
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: LineString?) {
        if (value == null) {
            throw NullPointerException("LineString was null! Wrap in .nullSafe() to write nullable values.")
        }

        writer.beginObject()
        writer.name("coordinates")
        writer.beginArray()
        value.coordinates.forEach { coordinate ->
            positionJsonAdapter.toJson(writer, coordinate)
        }
        writer.endArray()
        defaultAdapter.writeDefault(value, writer)
        writer.endObject()
    }
}