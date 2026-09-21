package org.scottishtecharmy.soundscape.geoengine.utils

import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt

const val DEGREES_TO_RADIANS = 2.0 * PI / 360.0
const val RADIANS_TO_DEGREES = 1.0 / DEGREES_TO_RADIANS
const val EARTH_RADIUS_METERS =
    6378137.0 //  Original Soundscape uses 6378137.0 not 6371000.0
const val MIN_LATITUDE = -85.05112878
const val MAX_LATITUDE = 85.05112878
const val MIN_LONGITUDE: Double = -180.0002
const val MAX_LONGITUDE: Double = 180.0002

fun toRadians(degrees: Double): Double = degrees * DEGREES_TO_RADIANS

fun fromRadians(degrees: Double): Double = degrees * RADIANS_TO_DEGREES

fun Double.round(digitLength: Int): Double {
    val pow = 10.0.pow(digitLength)
    return (this * pow).roundToLong() / pow
}

fun distance(lat1: Double, long1: Double, lat2: Double, long2: Double): Double {
    val deltaLat = toRadians(lat2 - lat1)
    val deltaLon = toRadians(long2 - long1)
    val a =
        sin(deltaLat / 2) * sin(deltaLat / 2) + cos(toRadians(lat1)) * cos(toRadians(lat2)) * sin(
            deltaLon / 2
        ) * sin(
            deltaLon / 2
        )
    val c = 2 * asin(sqrt(a))
    return (EARTH_RADIUS_METERS * c)
}

fun bearingFromTwoPoints(
    loc1: LngLatAlt,
    loc2: LngLatAlt,
): Double {
    val latitude1 = toRadians(loc1.latitude)
    val latitude2 = toRadians(loc2.latitude)
    val longDiff = toRadians(loc2.longitude - loc1.longitude)
    val y = sin(longDiff) * cos(latitude2)
    val x = cos(latitude1) * sin(latitude2) - sin(latitude1) * cos(latitude2) * cos(longDiff)
    return ((fromRadians(atan2(y, x)) + 360) % 360).round(1)
}

fun getDestinationCoordinate(start: LngLatAlt, bearing: Double, distance: Double): LngLatAlt {
    val lat1 = toRadians(start.latitude)
    val lon1 = toRadians(start.longitude)

    val d = distance / EARTH_RADIUS_METERS

    val bearingRadians = toRadians(bearing)

    val lat2 = asin(
        sin(lat1) * cos(d) +
                cos(lat1) * sin(d) * cos(bearingRadians)
    )
    val lon2 = lon1 + atan2(
        sin(bearingRadians) * sin(d) * cos(lat1),
        cos(d) - sin(lat1) * sin(lat2)
    )

    return LngLatAlt(fromRadians(lon2), fromRadians(lat2))
}
