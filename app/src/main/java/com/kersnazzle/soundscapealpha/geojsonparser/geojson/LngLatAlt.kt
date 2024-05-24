package com.kersnazzle.soundscapealpha.geojsonparser.geojson

import com.squareup.moshi.JsonClass
import java.io.Serializable

@JsonClass(generateAdapter = true)
open class LngLatAlt(
    var longitude: Double = 0.toDouble(),
    var latitude: Double = 0.toDouble(),
    var altitude: Double? = null
) : Serializable {
    fun hasAltitude(): Boolean = altitude != null && altitude?.isNaN() == false

    // Problems with array of LngLatAlt comparisons. Attempting to fix here:
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (other is LngLatAlt) {
            val that = other as LngLatAlt
            return longitude == that.longitude && latitude == that.latitude &&
                    (altitude == null && that.altitude == null ||
                            altitude != null && that.altitude != null && altitude == that.altitude)
        }
        return false
    }

    override fun hashCode(): Int {
        var result = longitude.hashCode()
        result = 31 * result + latitude.hashCode()
        result = 31 * result + (altitude?.hashCode() ?: 0)
        return result
    }
}