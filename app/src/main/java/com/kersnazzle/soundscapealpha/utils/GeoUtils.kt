package com.kersnazzle.soundscapealpha.utils

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.asinh
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

const val DEGREES_TO_RADIANS = 2.0 * PI / 360.0
const val RADIANS_TO_DEGREES = 1.0 / DEGREES_TO_RADIANS
const val EARTH_RADIUS_METERS =
    6378137.0 //  Original Soundscape uses 6378137.0 not 6371000.0


/**
 * Compute the Haversine distance between the two coordinates.
 * @param lat1
 * the start latitude in decimal degrees
 * @param long1
 * the start longitude in decimal degrees
 * @param lat2
 * the finish latitude in decimal degrees
 * @param long2
 * the finish longitude in decimal degrees
 * @return the distance in meters
 */
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

    return (EARTH_RADIUS_METERS * c).round(2)
}

fun toRadians(degrees: Double): Double {
    return degrees * DEGREES_TO_RADIANS
}

fun fromRadians(degrees: Double): Double {
    return degrees * RADIANS_TO_DEGREES
}

fun Double.round(digitLength: Int): Double {
    val pow = 10.0.pow(digitLength)
    return (this * pow).roundToLong() / pow
}