package org.scottishtecharmy.soundscape.geojsonparser.moshi

import com.squareup.moshi.*
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.moshi.GeoJsonObjectMoshiAdapter.Companion.OPTIONS


class FeatureCollectionJsonAdapter : JsonAdapter<FeatureCollection>() {
    private val defaultAdapter = GeoJsonObjectMoshiAdapter()
    private val featureAdapter = FeatureJsonAdapter()

    @FromJson
    override fun fromJson(reader: JsonReader): FeatureCollection {
        val feature = FeatureCollection()
        var type = ""

        reader.beginObject()
        while (reader.hasNext()) {
            when (val index = reader.selectName(JsonReader.Options.of(*OPTIONS, "features"))) {
                0 -> type = reader.nextString()
                1 -> {
                    reader.skipName()
                    reader.skipValue()
                }

                OPTIONS.size -> {
                    val geometryJson = reader.peekJson()
                    geometryJson.beginArray()
                    val features = arrayListOf<Feature>()
                    while (geometryJson.hasNext()) {
                        try {
                            features.add(featureAdapter.fromJson(geometryJson))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    geometryJson.endArray()
                    reader.skipValue()

                    feature.features = features
                }

                else -> {
                    defaultAdapter.readDefault(feature, index, reader)
                }
            }
        }
        reader.endObject()

        if (type != "FeatureCollection") {
            throw JsonDataException("Required type is not a FeatureCollection at ${reader.path}")
        }

        return feature
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: FeatureCollection?) {
        if (value == null) {
            throw NullPointerException("FeatureCollection was null! Wrap in .nullSafe() to write nullable values.")
        }

        writer.beginObject()
        writer.name("features")
        writer.beginArray()
        value.features.forEach { feature ->
            defaultAdapter.toJson(writer, feature)
        }
        writer.endArray()
        defaultAdapter.writeDefault(value, writer)
        writer.endObject()
    }
}