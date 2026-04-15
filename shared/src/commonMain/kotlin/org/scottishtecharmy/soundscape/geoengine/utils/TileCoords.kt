package org.scottishtecharmy.soundscape.geoengine.utils

import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import kotlin.math.PI
import kotlin.math.asinh
import kotlin.math.atan
import kotlin.math.floor
import kotlin.math.sinh
import kotlin.math.tan

fun getXYTile(
    location: LngLatAlt,
    zoom: Int
): Pair<Int, Int> {
    val latRad = toRadians(location.latitude)
    var xTile = floor((location.longitude + 180) / 360 * (1 shl zoom)).toInt()
    var yTile = floor((1.0 - asinh(tan(latRad)) / PI) / 2 * (1 shl zoom)).toInt()

    if (xTile < 0) xTile = 0
    if (xTile >= (1 shl zoom)) xTile = (1 shl zoom) - 1
    if (yTile < 0) yTile = 0
    if (yTile >= (1 shl zoom)) yTile = (1 shl zoom) - 1
    return Pair(xTile, yTile)
}

fun getLatLonTileWithOffset(
    xTile: Int,
    yTile: Int,
    zoom: Int,
    xOffset: Double,
    yOffset: Double,
): LngLatAlt {
    val x: Double = xTile.toDouble() + xOffset
    val y: Double = yTile.toDouble() + yOffset
    val lon: Double = ((x / (1 shl zoom)) * 360.0) - 180.0
    val latRad = atan(sinh(PI * (1 - 2 * y / (1 shl zoom))))

    return LngLatAlt(lon, fromRadians(latRad))
}
