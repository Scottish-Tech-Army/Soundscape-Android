package org.scottishtecharmy.soundscape.geojsonparser.geojson

import com.squareup.moshi.JsonClass
import org.maplibre.android.geometry.LatLng
import org.scottishtecharmy.soundscape.geoengine.utils.distance
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
            return longitude == other.longitude && latitude == other.latitude &&
                    (altitude == null && other.altitude == null ||
                            altitude != null && other.altitude != null && altitude == other.altitude)
        }
        return false
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

    fun toLatLng(): LatLng {
        return LatLng(latitude, longitude)
    }

    fun distance(other: LngLatAlt): Double {
        return distance(latitude, longitude, other.latitude, other.longitude)
    }

    fun distanceToLine(l1: LngLatAlt,
                       l2: LngLatAlt,
                       nearestPoint: LngLatAlt? = null): Double {
        return distance(l1.latitude, l1.longitude,
            l2.latitude, l2.longitude,
            latitude, longitude,
            nearestPoint)
    }

    /**
     * Distance to a LineString from current location.
     * @param lineStringCoordinates
     * LineString that we are working out the distance from
     * @param nearestPoint
     * Point in the line nearest that had the shortest distance
     * @return The distance of the point to the LineString
     */
    fun distanceToLineString(
        lineStringCoordinates: LineString,
        nearestPoint: LngLatAlt? = null
    ): Double {

        var shortestDistance = Double.MAX_VALUE
        var bestNearestPoint = LngLatAlt()
        for(i in 1 until lineStringCoordinates.coordinates.size) {
            val nearestPointOnSegment = LngLatAlt()
            val distance = distanceToLine(
                lineStringCoordinates.coordinates[i-1],
                lineStringCoordinates.coordinates[i],
                nearestPointOnSegment)

            if(distance < shortestDistance) {
                shortestDistance = distance
                bestNearestPoint = nearestPointOnSegment
            }
        }
        if(nearestPoint != null) {
            nearestPoint.longitude = bestNearestPoint.longitude
            nearestPoint.latitude = bestNearestPoint.latitude
        }
        return shortestDistance
    }
}

fun fromLatLng(loc:LatLng): LngLatAlt {
    return LngLatAlt(loc.longitude, loc.latitude)
}
