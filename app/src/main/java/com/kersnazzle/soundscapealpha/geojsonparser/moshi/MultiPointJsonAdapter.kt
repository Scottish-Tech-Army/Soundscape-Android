package com.kersnazzle.soundscapealpha.geojsonparser.moshi

import com.squareup.moshi.*
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.LngLatAlt
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.MultiPoint
import com.kersnazzle.soundscapealpha.geojsonparser.moshi.GeoJsonObjectMoshiAdapter.Companion.OPTIONS


class MultiPointJsonAdapter : JsonAdapter<MultiPoint>() {
    private val defaultAdapter = GeoJsonObjectMoshiAdapter()
    private val positionJsonAdapter = LngLatAltMoshiAdapter()

    @FromJson
    override fun fromJson(reader: JsonReader): MultiPoint {
        val multiPoint = MultiPoint()
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
                    defaultAdapter.readDefault(multiPoint, index, reader)
                }
            }
        }
        reader.endObject()

        if (position == null || position.isEmpty()) {
            throw JsonDataException("Required positions are missing at ${reader.path}")
        }

        if (type != "MultiPoint") {
            throw JsonDataException("Required type is not a Point at ${reader.path}")
        }

        multiPoint.coordinates = position
        return multiPoint
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: MultiPoint?) {
        if (value == null) {
            throw NullPointerException("MultiPoint was null! Wrap in .nullSafe() to write nullable values.")
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