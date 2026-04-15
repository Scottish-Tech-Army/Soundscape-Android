package org.scottishtecharmy.soundscape.geojsonparser.geojson

open class LngLatAlt(
    var longitude: Double = 0.toDouble(),
    var latitude: Double = 0.toDouble(),
    var altitude: Double? = null
) {
    fun hasAltitude(): Boolean = altitude != null && altitude?.isNaN() == false

    fun clone() : LngLatAlt {
        return LngLatAlt(longitude, latitude, altitude)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LngLatAlt) return false
        if (this::class != other::class) return false
        return longitude == other.longitude && latitude == other.latitude &&
                (altitude == null && other.altitude == null ||
                        altitude != null && other.altitude != null && altitude == other.altitude)
    }

    override fun hashCode(): Int {
        var result = longitude.hashCode()
        result = 31 * result + latitude.hashCode()
        result = 31 * result + (altitude?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "$longitude,$latitude"
    }
}
