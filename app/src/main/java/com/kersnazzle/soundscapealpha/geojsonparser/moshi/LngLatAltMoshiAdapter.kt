package com.kersnazzle.soundscapealpha.geojsonparser.moshi

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.LngLatAlt

/**
 * LngLatAlt de-serialization for Moshi
 */
open class LngLatAltMoshiAdapter : JsonAdapter<LngLatAlt>() {
    override fun fromJson(reader: JsonReader): LngLatAlt? {
        reader.beginArray()
        val node = LngLatAlt()

        node.longitude = reader.nextDouble()
        node.latitude = reader.nextDouble()
        node.altitude =
            if (reader.peek() != JsonReader.Token.NULL && reader.hasNext()) reader.nextDouble()
                ?: Double.NaN else null
        reader.endArray()

        return node
    }

    override fun toJson(writer: JsonWriter, value: LngLatAlt?) {
        writer.beginArray()
        writer.value(value?.longitude ?: 0.0)
        writer.value(value?.latitude ?: 0.0)

        if (value?.hasAltitude() == true) {
            writer.value(value.altitude ?: 0.0)
        }

        writer.endArray()
    }
}