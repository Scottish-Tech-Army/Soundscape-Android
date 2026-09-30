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

/**
 * The abbreviated cardinal for [degrees] ("NW"), as used where there isn't room for the word -
 * the beacon card on the home screen. Speech uses [getCompassLabel] instead, because a screen
 * reader saying "NW" is worse than one saying "north west".
 */
fun getCompassLabelAbbreviated(degrees: Int): StringKey =
    when (getCompassLabel(degrees)) {
        StringKey.DirectionsCardinalNorthEast -> StringKey.DirectionsCardinalNorthEastAbb
        StringKey.DirectionsCardinalEast -> StringKey.DirectionsCardinalEastAbb
        StringKey.DirectionsCardinalSouthEast -> StringKey.DirectionsCardinalSouthEastAbb
        StringKey.DirectionsCardinalSouth -> StringKey.DirectionsCardinalSouthAbb
        StringKey.DirectionsCardinalSouthWest -> StringKey.DirectionsCardinalSouthWestAbb
        StringKey.DirectionsCardinalWest -> StringKey.DirectionsCardinalWestAbb
        StringKey.DirectionsCardinalNorthWest -> StringKey.DirectionsCardinalNorthWestAbb
        else -> StringKey.DirectionsCardinalNorthAbb
    }

fun getRelativeClockTime(degrees: Int, userDegrees: Int): Int {
    val relative = normalizeHeading(degrees - userDegrees)
    val hour = ((relative + 15) / 30) % 12
    return if (hour == 0) 12 else hour
}

/**
 * The clock position [hour] (1-12) as a word for relative_clock_direction. A bare digit leaves the
 * speech synthesiser to guess its form, and where the position is an ordinal in a grammatical case
 * (Polish "na godzinie dziewiątej") it guesses wrong. Languages that don't need a word keep the
 * digit.
 */
fun getRelativeClockHourLabel(hour: Int): StringKey =
    when (hour) {
        1 -> StringKey.RelativeClockHour1
        2 -> StringKey.RelativeClockHour2
        3 -> StringKey.RelativeClockHour3
        4 -> StringKey.RelativeClockHour4
        5 -> StringKey.RelativeClockHour5
        6 -> StringKey.RelativeClockHour6
        7 -> StringKey.RelativeClockHour7
        8 -> StringKey.RelativeClockHour8
        9 -> StringKey.RelativeClockHour9
        10 -> StringKey.RelativeClockHour10
        11 -> StringKey.RelativeClockHour11
        else -> StringKey.RelativeClockHour12
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
