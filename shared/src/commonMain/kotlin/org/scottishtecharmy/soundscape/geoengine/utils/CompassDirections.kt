package org.scottishtecharmy.soundscape.geoengine.utils

import org.scottishtecharmy.soundscape.i18n.StringKey

fun normalizeHeading(deg: Int): Int {
    var tmp = deg
    while (tmp < 0) tmp += 360
    while (tmp > 360) tmp -= 360
    return tmp
}

fun getCompassLabel(degrees: Int): StringKey {
    val normalizedDegrees = normalizeHeading(degrees)
    return when (normalizedDegrees) {
        in 338..360, in 0..22 -> StringKey.DirectionsCardinalNorth
        in 23..67 -> StringKey.DirectionsCardinalNorthEast
        in 68..112 -> StringKey.DirectionsCardinalEast
        in 113..157 -> StringKey.DirectionsCardinalSouthEast
        in 158..202 -> StringKey.DirectionsCardinalSouth
        in 203..247 -> StringKey.DirectionsCardinalSouthWest
        in 248..292 -> StringKey.DirectionsCardinalWest
        in 293..337 -> StringKey.DirectionsCardinalNorthWest
        else -> StringKey.DirectionsCardinalNorth
    }
}

fun getRelativeClockTime(degrees: Int, userDegrees: Int): Int {
    val relative = normalizeHeading(degrees - userDegrees)
    val hour = ((relative + 15) / 30) % 12
    return if (hour == 0) 12 else hour
}

fun getRelativeLeftRightLabel(relativeAngle: Int): StringKey {
    val normalizedAngle = normalizeHeading(relativeAngle)
    return when (normalizedAngle) {
        in 338..360, in 0..22 -> StringKey.RelativeLeftRightDirectionAhead
        in 23..67 -> StringKey.RelativeLeftRightDirectionAheadRight
        in 68..112 -> StringKey.RelativeLeftRightDirectionRight
        in 113..157 -> StringKey.RelativeLeftRightDirectionBehindRight
        in 158..202 -> StringKey.RelativeLeftRightDirectionBehind
        in 203..247 -> StringKey.RelativeLeftRightDirectionBehindLeft
        in 248..292 -> StringKey.RelativeLeftRightDirectionLeft
        in 293..337 -> StringKey.RelativeLeftRightDirectionAheadLeft
        else -> StringKey.RelativeLeftRightDirectionAhead
    }
}
