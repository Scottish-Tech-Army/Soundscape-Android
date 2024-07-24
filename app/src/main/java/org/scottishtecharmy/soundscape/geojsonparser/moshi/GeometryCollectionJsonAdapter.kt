package org.scottishtecharmy.soundscape.geojsonparser.moshi

import com.squareup.moshi.*
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeoJsonObject
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeometryCollection
import org.scottishtecharmy.soundscape.geojsonparser.moshi.GeoJsonObjectMoshiAdapter.Companion.OPTIONS

class GeometryCollectionJsonAdapter : JsonAdapter<GeometryCollection>() {
    private val defaultAdapter = GeoJsonObjectMoshiAdapter()

    @FromJson
    override fun fromJson(reader: JsonReader): GeometryCollection {
        val geometry = GeometryCollection()
        var type = ""

        reader.beginObject()
        while (reader.hasNext()) {
            when (val index = reader.selectName(JsonReader.Options.of(*OPTIONS, "geometries"))) {
                0 -> type = reader.nextString()
                1 -> {
                    reader.skipName()
                    reader.skipValue()
                }

                OPTIONS.size -> {
                    val geometryJson = reader.peekJson()
                    geometryJson.beginArray()
                    val geometries = arrayListOf<GeoJsonObject>()
                    while (geometryJson.hasNext()) {
                        defaultAdapter.fromJson(geometryJson)?.let {
                            geometries.add(it)
                        }
                    }
                    geometryJson.endArray()
                    reader.skipValue()

                    geometry.geometries = geometries
                }

                else -> {
                    defaultAdapter.readDefault(geometry, index, reader)
                }
            }
        }
        reader.endObject()

        if (type != "GeometryCollection") {
            throw JsonDataException("Required type is not a GeometryCollection at ${reader.path}")
        }

        return geometry
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: GeometryCollection?) {
        if (value == null) {
            throw NullPointerException("GeometryCollection was null! Wrap in .nullSafe() to write nullable values.")
        }

        writer.beginObject()
        writer.name("geometries")
        writer.beginArray()
        value.geometries.forEach { geometry ->
            defaultAdapter.toJson(writer, geometry)
        }
        writer.endArray()
        defaultAdapter.writeDefault(value, writer)
        writer.endObject()
    }
}