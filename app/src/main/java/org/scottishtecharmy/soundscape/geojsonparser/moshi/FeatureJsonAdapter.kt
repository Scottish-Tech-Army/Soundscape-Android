package org.scottishtecharmy.soundscape.geojsonparser.moshi

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.utils.SuperCategoryId
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

        if(value is MvtFeature) {
            // We're going to populate the properties with the values that we have stored.
            (value.properties ?: HashMap()).also { properties ->
                properties["name"] = value.name
                properties["osm_id"] = value.osmId
                properties["class"] = value.featureClass
                properties["subclass"] = value.featureSubClass
                properties["feature_type"] = value.featureType
                properties["category"] = when(value.superCategory) {
                    SuperCategoryId.UNCATEGORIZED -> ""
                    SuperCategoryId.SETTLEMENT_CITY -> "city"
                    SuperCategoryId.SETTLEMENT_TOWN -> "town"
                    SuperCategoryId.SETTLEMENT_VILLAGE -> "village"
                    SuperCategoryId.SETTLEMENT_HAMLET -> "hamlet"
                    SuperCategoryId.OBJECT -> "object"
                    SuperCategoryId.PLACE -> "place"
                    SuperCategoryId.INFORMATION -> "information"
                    SuperCategoryId.MOBILITY -> "mobility"
                    SuperCategoryId.SAFETY -> "safety"
                    SuperCategoryId.LANDMARK -> "landmark"
                    SuperCategoryId.MARKER -> "marker"
                    SuperCategoryId.HOUSENUMBER -> "housenumber"
                }
                properties["feature_value"] = value.featureValue

                value.properties = properties
            }
        }

        writer.beginObject()
        writer.name("geometry")
        defaultAdapter.toJson(writer, value.geometry)
        defaultAdapter.writeDefault(value, writer)
        writer.endObject()
    }
}