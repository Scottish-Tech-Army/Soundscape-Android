package org.scottishtecharmy.soundscape.audio

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import platform.AVFAudio.AVAudio3DPoint
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

const val DEFAULT_RENDERING_DISTANCE = 2.0

/**
 * Converts a compass bearing (0=north, 90=east, 180=south, 270=west) and distance
 * to an AVAudio3DPoint in the coordinate system used by AVAudioEnvironmentNode.
 *
 * Matches the original iOS Soundscape formula:
 *   x = distance * cos(bearing_rad - π/2)
 *   y = 0
 *   z = distance * sin(bearing_rad - π/2)
 */
@OptIn(ExperimentalForeignApi::class)
fun bearingToPoint(bearingDegrees: Double, distance: Double = DEFAULT_RENDERING_DISTANCE): CValue<AVAudio3DPoint> {
    var radians = bearingDegrees * PI / 180.0 - PI / 2.0
    if (radians < 0) radians += 2.0 * PI
    return cValue {
        x = (distance * cos(radians)).toFloat()
        y = 0f
        z = (distance * sin(radians)).toFloat()
    }
}

/**
 * Returns the shortest angular difference between two angles in degrees.
 * Result is in range [-180, 180].
 */
fun angleDifference(a: Double, b: Double): Double {
    var diff = (a - b) % 360.0
    if (diff > 180.0) diff -= 360.0
    if (diff < -180.0) diff += 360.0
    return diff
}

/**
 * Calculates the bearing from one lat/lng to another in degrees [0, 360).
 */
fun bearing(fromLat: Double, fromLng: Double, toLat: Double, toLng: Double): Double {
    val lat1 = fromLat * PI / 180.0
    val lat2 = toLat * PI / 180.0
    val dLng = (toLng - fromLng) * PI / 180.0

    val y = sin(dLng) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLng)
    val bearing = kotlin.math.atan2(y, x) * 180.0 / PI
    return (bearing + 360.0) % 360.0
}
