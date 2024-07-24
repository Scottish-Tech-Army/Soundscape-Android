package org.scottishtecharmy.soundscape.geojsonparser.moshi

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.moshi.GeoJsonObjectMoshiAdapter.Companion.OPTIONS


class FeatureJsonAdapter : JsonAdapter<Feature>() {
    private val defaultAdapter = GeoJsonObjectMoshiAdapter()

    @FromJson
    override fun fromJson(reader: JsonReader): Feature {
        val feature = Feature()
        var type = ""

        reader.beginObject()
        while (reader.hasNext()) {
            when (val index =
                reader.selectName(JsonReader.Options.of(*OPTIONS, "geometry", "id"))) {
                0 -> type = reader.nextString()
                1 -> {
                    reader.skipName()
                    reader.skipValue()
                }

                OPTIONS.size -> {
                    val geometryJson = reader.peekJson()
                    defaultAdapter.fromJson(geometryJson)?.let {
                        feature.geometry = it
                    }
                }

                OPTIONS.size + 1 -> {
                    feature.id = reader.nextString()
                }

                else -> {
                    defaultAdapter.readDefault(feature, index, reader)
                }
            }
        }
        reader.endObject()

        if (type != "Feature") {
            throw JsonDataException("Required type is not a Feature at ${reader.path}")
        }

        return feature
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: Feature?) {
        if (value == null) {
            throw NullPointerException("Feature was null! Wrap in .nullSafe() to write nullable values.")
        }

        writer.beginObject()
        writer.name("geometry")
        defaultAdapter.toJson(writer, value.geometry)
        defaultAdapter.writeDefault(value, writer)
        writer.endObject()
    }
}