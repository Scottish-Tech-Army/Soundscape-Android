package org.scottishtecharmy.soundscape.geojsonparser.moshi

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiPolygon
import org.scottishtecharmy.soundscape.geojsonparser.moshi.GeoJsonObjectMoshiAdapter.Companion.OPTIONS


class MultiPolygonJsonAdapter : JsonAdapter<MultiPolygon>() {
    private val defaultAdapter = GeoJsonObjectMoshiAdapter()
    private val positionJsonAdapter = LngLatAltMoshiAdapter()

    @FromJson
    override fun fromJson(reader: JsonReader): MultiPolygon {
        val point = MultiPolygon()
        var type = ""
        var position: ArrayList<ArrayList<ArrayList<LngLatAlt>>>? = arrayListOf()

        reader.beginObject()
        while (reader.hasNext()) {
            when (val index = reader.selectName(JsonReader.Options.of(*OPTIONS))) {
                0 -> type = reader.nextString()
                1 -> {
                    position = arrayListOf()
                    reader.beginArray()
                    while (reader.hasNext()) {
                        val polygon = arrayListOf<ArrayList<LngLatAlt>>()
                        reader.beginArray()

                        while (reader.hasNext()) {
                            reader.beginArray()
                            val polyLine = arrayListOf<LngLatAlt>()
                            while (reader.hasNext()) {
                                polyLine.add(positionJsonAdapter.fromJson(reader)!!)
                            }

                            polygon.add(polyLine)
                            reader.endArray()
                        }

                        position.add(polygon)
                        reader.endArray()
                    }
                    reader.endArray()
                }

                else -> {
                    defaultAdapter.readDefault(point, index, reader)
                }
            }
        }
        reader.endObject()

        if (position == null || position.isEmpty()) {
            throw JsonDataException("Required positions are missing at ${reader.path}")
        }

        if (type != "MultiPolygon") {
            throw JsonDataException("Required type is not a MultiPolygon at ${reader.path}")
        }

        point.coordinates = position
        return point
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: MultiPolygon?) {
        if (value == null) {
            throw NullPointerException("MultiPolygon was null! Wrap in .nullSafe() to write nullable values.")
        }

        writer.beginObject()
        writer.name("coordinates")
        writer.beginArray()
        value.coordinates.forEach { polygonList ->
            writer.beginArray()
            polygonList.forEach { coordinateList ->
                writer.beginArray()
                coordinateList.forEach { coordinate ->
                    positionJsonAdapter.toJson(writer, coordinate)
                }
                writer.endArray()
            }
            writer.endArray()
        }
        writer.endArray()
        defaultAdapter.writeDefault(value, writer)
        writer.endObject()
    }
}