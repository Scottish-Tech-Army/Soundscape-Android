package com.kersnazzle.soundscapealpha.utils

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.asinh
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

const val DEGREES_TO_RADIANS = 2.0 * PI / 360.0
const val RADIANS_TO_DEGREES = 1.0 / DEGREES_TO_RADIANS
const val EARTH_RADIUS_METERS =
    6378137.0 //  Original Soundscape uses 6378137.0 not 6371000.0
const val MIN_LATITUDE = -85.05112878
const val MAX_LATITUDE = 85.05112878
const val MIN_LONGITUDE: Double = -180.0002
const val MAX_LONGITUDE: Double = 180.0002


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

/**
 * The size of the map in pixels for the given zoom level assuming the base is 256 pixels
 * @param zoom
 * A zoom level (between 0 and 23)
 * @return Side length of the map in pixels
 */
fun mapSize(zoom: Int): Int {
    val base = 256
    return base shl (zoom)
}

/**
 * Clips a value to the range [max, min]
 * @param value
 * Value to clip
 * @param minimum
 * Minimum value of range
 * @param maximum
 * Maximum value of range
 * @return The clipped value
 */
fun clip(value: Double, minimum: Double, maximum: Double): Double {
    return min(max(value, minimum), maximum)
}

/** Determines the ground resolution (in meters per pixel) at a specified
 * latitude and level of detail.
 * @param latitude
 * Latitude (in degrees) at which to measure the ground resolution.
 * @param zoom
 * Level of detail, from 1 (lowest detail) to 23 (highest detail).
 * @return The ground resolution, in meters per pixel.
 */
fun groundResolution(latitude: Double, zoom: Int): Double {
    val clippedLat = clip(latitude, MIN_LATITUDE, MAX_LATITUDE)

    return cos(clippedLat * PI / 180.0) * 2 * PI * EARTH_RADIUS_METERS / mapSize(zoom)
}

/** Calculates the coordinates of the Tile containing the provided pixel coordinates
 * @param pixelX
 * X coordinate of the pixel
 * @param pixelY
 * Y coordinate of the pixel
 * @return Tile coordinate as a Pair(x, y)
 */
fun getTileXY(pixelX: Int, pixelY: Int) = Pair(pixelX / 256, pixelY / 256)

/**
 * Generates a quad key string from the tile X, Y coordinates and zoom level provided
 * Here's the Microsoft info: https://learn.microsoft.com/en-us/bingmaps/articles/bing-maps-tile-system
 * @param tileX
 * X coordinate of a tile
 * @param tileY
 * Y coordinate of the tile
 * @param zoomLevel
 * Zoom level of the tile
 * @return A quad key String
 */
fun getQuadKey(tileX: Int, tileY: Int, zoomLevel: Int): String {

    var quadKey = ""

    for (level in zoomLevel downTo 1) {
        var digit = 0
        val mask = 1 shl (level - 1)
        if (tileX and mask != 0) {
            digit += 1
        }
        if (tileY and mask != 0) {
            digit += 2
        }
        quadKey += digit.toString()
    }
    return quadKey
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