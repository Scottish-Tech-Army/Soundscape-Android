package com.kersnazzle.soundscapealpha.geojsonparser.moshi

import com.squareup.moshi.*
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.LngLatAlt
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.MultiLineString
import com.kersnazzle.soundscapealpha.geojsonparser.moshi.GeoJsonObjectMoshiAdapter.Companion.OPTIONS


class MultiLineStringJsonAdapter : JsonAdapter<MultiLineString>() {
    private val defaultAdapter = GeoJsonObjectMoshiAdapter()
    private val positionJsonAdapter = LngLatAltMoshiAdapter()

    @FromJson
    override fun fromJson(reader: JsonReader): MultiLineString {
        val lineString = MultiLineString()
        var type = ""
        var position: ArrayList<ArrayList<LngLatAlt>>? = arrayListOf()

        reader.beginObject()
        while (reader.hasNext()) {
            when (val index = reader.selectName(JsonReader.Options.of(*OPTIONS))) {
                0 -> type = reader.nextString()
                1 -> {
                    position = arrayListOf()
                    reader.beginArray()
                    while (reader.hasNext()) {
                        reader.beginArray()
                        val line = arrayListOf<LngLatAlt>()
                        while (reader.hasNext()) {
                            line.add(positionJsonAdapter.fromJson(reader)!!)
                        }
                        position.add(line)
                        reader.endArray()
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

        if (type != "MultiLineString") {
            throw JsonDataException("Required type is not a MultiLineString at ${reader.path}")
        }

        lineString.coordinates = position
        return lineString
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: MultiLineString?) {
        if (value == null) {
            throw NullPointerException("MultiLineString was null! Wrap in .nullSafe() to write nullable values.")
        }

        writer.beginObject()
        writer.name("coordinates")
        writer.beginArray()
        value.coordinates.forEach { coordinateList ->
            writer.beginArray()
            coordinateList.forEach { coordinate ->
                positionJsonAdapter.toJson(writer, coordinate)
            }
            writer.endArray()
        }
        writer.endArray()
        defaultAdapter.writeDefault(value, writer)
        writer.endObject()
    }
}