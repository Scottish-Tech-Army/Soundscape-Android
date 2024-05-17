package com.kersnazzle.soundscapealpha.utils

import kotlin.math.PI
import kotlin.math.asinh
import kotlin.math.floor
import kotlin.math.tan

const val DEGREES_TO_RADIANS = 2.0 * PI / 360.0
const val RADIANS_TO_DEGREES = 1.0 / DEGREES_TO_RADIANS

/**
 * Gets Slippy Map Tile Name from X and Y GPS coordinates and Zoom (fixed at 16 for Soundscape)
 * @param lat
 * latitude in decimal degrees
 * @param lon
 * longitude in decimal degrees
 * @param zoom
 * the zoom level.
 * @return a Pair(xtile, ytile)
 */
fun getXYTile(lat: Double, lon: Double, zoom: Int = 16): Pair<Int, Int> {
    val latRad = toRadians(lat)
    var xtile = floor((lon + 180) / 360 * (1 shl zoom)).toInt()
    var ytile = floor((1.0 - asinh(tan(latRad)) / PI) / 2 * (1 shl zoom)).toInt()

    if (xtile < 0) {
        xtile = 0
    }
    if (xtile >= (1 shl zoom)) {
        xtile = (1 shl zoom) - 1
    }
    if (ytile < 0) {
        ytile = 0
    }
    if (ytile >= (1 shl zoom)) {
        ytile = (1 shl zoom) - 1
    }
    return Pair(xtile, ytile)
}

fun toRadians(degrees: Double): Double {
    return degrees * DEGREES_TO_RADIANS
}

fun fromRadians(degrees: Double): Double {
    return degrees * RADIANS_TO_DEGREES
}