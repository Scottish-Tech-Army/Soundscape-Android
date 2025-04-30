package org.scottishtecharmy.soundscape.geojsonparser.geojson

import com.squareup.moshi.JsonClass
import org.maplibre.android.geometry.LatLng
import org.scottishtecharmy.soundscape.geoengine.utils.EARTH_RADIUS_METERS
import org.scottishtecharmy.soundscape.geoengine.utils.PointAndDistanceAndHeading
import org.scottishtecharmy.soundscape.geoengine.utils.bearingFromTwoPoints
import org.scottishtecharmy.soundscape.geoengine.utils.distance
import org.scottishtecharmy.soundscape.geoengine.utils.toRadians
import java.io.Serializable
import java.lang.Math.toDegrees
import kotlin.math.cos
import kotlin.math.min

@JsonClass(generateAdapter = true)
open class LngLatAlt(
    var longitude: Double = 0.toDouble(),
    var latitude: Double = 0.toDouble(),
    var altitude: Double? = null
) : Serializable {
    fun hasAltitude(): Boolean = altitude != null && altitude?.isNaN() == false

    fun clone() : LngLatAlt {
        return LngLatAlt(longitude, latitude, altitude)
    }

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

    fun project(location: LngLatAlt, reference: LngLatAlt): LngLatAlt {
        val dLat = toRadians(location.latitude - reference.latitude)
        val dLon = toRadians(location.longitude - reference.longitude)

        val x = EARTH_RADIUS_METERS * dLon * cos(toRadians(reference.latitude))
        val y = EARTH_RADIUS_METERS * dLat

        return LngLatAlt(x, y)
    }

    fun unproject(projected: LngLatAlt, reference: LngLatAlt): LngLatAlt {
        val dLat = projected.latitude / EARTH_RADIUS_METERS
        val dLon = projected.longitude / (EARTH_RADIUS_METERS * cos(toRadians(reference.latitude)))

        val lat = reference.latitude + toDegrees(dLat)
        val lon = reference.longitude + toDegrees(dLon)

        return LngLatAlt(lon, lat)
    }
    fun distanceToLine(
        l1: LngLatAlt,
        l2: LngLatAlt,
        nearestPoint: LngLatAlt? = null
    ): Double {

        // Use l1 as our reference point
        val l1Projected = project(l1, l1)
        val l2Projected = project(l2, l1)
        val thisProjected = project(this, l1)
        val nearestPointProjected = LngLatAlt()
        val result = distance(
            l1Projected.latitude, l1Projected.longitude,
            l2Projected.latitude, l2Projected.longitude,
            thisProjected.latitude, thisProjected.longitude,
            nearestPointProjected
        )

        if(nearestPoint != null) {
            val np = unproject(nearestPointProjected, l1)

            nearestPoint.latitude = np.latitude
            nearestPoint.longitude = np.longitude
        }
        return result
    }

    /**
     * Distance to a LineString from current location.
     * @param lineStringCoordinates LineString that we are working out the distance from
     * @return The distance of the point to the LineString, the nearest point on the line and the
     * heading of the line at that point.
     */
    fun distanceToLineString(
        lineStringCoordinates: LineString
    ): PointAndDistanceAndHeading {

        val result = PointAndDistanceAndHeading()
        var last = lineStringCoordinates.coordinates[0]
        for (i in 1 until lineStringCoordinates.coordinates.size) {
            val current = lineStringCoordinates.coordinates[i]
            val pointOnLine = LngLatAlt()
            val distance = distanceToLine(last, current, pointOnLine)
            if (distance < result.distance) {
                result.distance = min(result.distance, distance)
                result.point = pointOnLine
                result.heading = bearingFromTwoPoints(last, current)
            }
            last = current
        }
        return result
    }
}

fun fromLatLng(loc:LatLng): LngLatAlt {
    return LngLatAlt(loc.longitude, loc.latitude)
}
