package org.scottishtecharmy.soundscape.geoengine.utils


import android.content.Context
import org.scottishtecharmy.soundscape.R

fun normalizeHeading(deg: Int) : Int {
    var tmp = deg
    while (tmp < 0) tmp += 360
    while (tmp > 360) tmp -= 360

    return tmp
}


fun getCompassLabelFacingDirection(localizedContext: Context,
                                   degrees: Int,
                                   inMotion: Boolean,
                                   inVehicle: Boolean
): String{
    val normalizedDegrees = normalizeHeading(degrees)
    if(!inMotion) {
        return when (normalizedDegrees) {
            in 338..360, in 0..22 -> localizedContext.getString(R.string.directions_facing_n)
            in 23..67 -> localizedContext.getString(R.string.directions_facing_ne)
            in 68..112 -> localizedContext.getString(R.string.directions_facing_e)
            in 113..157 -> localizedContext.getString(R.string.directions_facing_se)
            in 158..202 -> localizedContext.getString(R.string.directions_facing_s)
            in 203..247 -> localizedContext.getString(R.string.directions_facing_sw)
            in 248..292 -> localizedContext.getString(R.string.directions_facing_w)
            in 293..337 -> localizedContext.getString(R.string.directions_facing_nw)
            else -> ""
        }
    } else if(inVehicle) {
        return when (normalizedDegrees) {
            in 338..360, in 0..22 -> localizedContext.getString(R.string.directions_traveling_n)
            in 23..67 -> localizedContext.getString(R.string.directions_traveling_ne)
            in 68..112 -> localizedContext.getString(R.string.directions_traveling_e)
            in 113..157 -> localizedContext.getString(R.string.directions_traveling_se)
            in 158..202 -> localizedContext.getString(R.string.directions_traveling_s)
            in 203..247 -> localizedContext.getString(R.string.directions_traveling_sw)
            in 248..292 -> localizedContext.getString(R.string.directions_traveling_w)
            in 293..337 -> localizedContext.getString(R.string.directions_traveling_nw)
            else -> ""
        }
    } else {
        return when (normalizedDegrees) {
            in 338..360, in 0..22 -> localizedContext.getString(R.string.directions_heading_n)
            in 23..67 -> localizedContext.getString(R.string.directions_heading_ne)
            in 68..112 -> localizedContext.getString(R.string.directions_heading_e)
            in 113..157 -> localizedContext.getString(R.string.directions_heading_se)
            in 158..202 -> localizedContext.getString(R.string.directions_heading_s)
            in 203..247 -> localizedContext.getString(R.string.directions_heading_sw)
            in 248..292 -> localizedContext.getString(R.string.directions_heading_w)
            in 293..337 -> localizedContext.getString(R.string.directions_heading_nw)
            else -> ""
        }
    }
}

fun getCompassLabelFacingDirectionAlong(localizedContext: Context,
                                        degrees: Int,
                                        placeholder: String,
                                        inMotion: Boolean,
                                        inVehicle: Boolean
):String{
    val normalizedDegrees = normalizeHeading(degrees)
    if(!inMotion) {
        return when (normalizedDegrees) {
            in 338..360, in 0..22 -> localizedContext.getString(
                R.string.directions_along_facing_n,
                placeholder
            )
            in 23..67 -> localizedContext.getString(
                R.string.directions_along_facing_ne,
                placeholder
            )
            in 68..112 -> localizedContext.getString(
                R.string.directions_along_facing_e,
                placeholder
            )
            in 113..157 -> localizedContext.getString(
                R.string.directions_along_facing_se,
                placeholder
            )
            in 158..202 -> localizedContext.getString(
                R.string.directions_along_facing_s,
                placeholder
            )
            in 203..247 -> localizedContext.getString(
                R.string.directions_along_facing_sw,
                placeholder
            )
            in 248..292 -> localizedContext.getString(
                R.string.directions_along_facing_w,
                placeholder
            )
            in 293..337 -> localizedContext.getString(
                R.string.directions_along_facing_nw,
                placeholder
            )
            else -> ""
        }
    } else if(inVehicle) {
        return when (normalizedDegrees) {
            in 338..360, in 0..22 -> localizedContext.getString(
                R.string.directions_along_traveling_n,
                placeholder
            )
            in 23..67 -> localizedContext.getString(
                R.string.directions_along_traveling_ne,
                placeholder
            )
            in 68..112 -> localizedContext.getString(
                R.string.directions_along_traveling_e,
                placeholder
            )
            in 113..157 -> localizedContext.getString(
                R.string.directions_along_traveling_se,
                placeholder
            )
            in 158..202 -> localizedContext.getString(
                R.string.directions_along_traveling_s,
                placeholder
            )
            in 203..247 -> localizedContext.getString(
                R.string.directions_along_traveling_sw,
                placeholder
            )
            in 248..292 -> localizedContext.getString(
                R.string.directions_along_traveling_w,
                placeholder
            )
            in 293..337 -> localizedContext.getString(
                R.string.directions_along_traveling_nw,
                placeholder
            )
            else -> ""
        }
    } else {
        return when (normalizedDegrees) {
            in 338..360, in 0..22 -> localizedContext.getString(
                R.string.directions_along_heading_n,
                placeholder
            )
            in 23..67 -> localizedContext.getString(
                R.string.directions_along_heading_ne,
                placeholder
            )
            in 68..112 -> localizedContext.getString(
                R.string.directions_along_heading_e,
                placeholder
            )
            in 113..157 -> localizedContext.getString(
                R.string.directions_along_heading_se,
                placeholder
            )
            in 158..202 -> localizedContext.getString(
                R.string.directions_along_heading_s,
                placeholder
            )
            in 203..247 -> localizedContext.getString(
                R.string.directions_along_heading_sw,
                placeholder
            )
            in 248..292 -> localizedContext.getString(
                R.string.directions_along_heading_w,
                placeholder
            )
            in 293..337 -> localizedContext.getString(
                R.string.directions_along_heading_nw,
                placeholder
            )
            else -> ""
        }
    }
}

fun getCompassLabel(degrees: Int):Int {
    val normalizedDegrees = normalizeHeading(degrees)
    return when (normalizedDegrees) {
        in 338..360, in 0..22 -> R.string.directions_cardinal_north
        in 23..67 -> R.string.directions_cardinal_north_east
        in 68..112 -> R.string.directions_cardinal_east
        in 113..157 -> R.string.directions_cardinal_south_east
        in 158..202 -> R.string.directions_cardinal_south
        in 203..247 -> R.string.directions_cardinal_south_west
        in 248..292 -> R.string.directions_cardinal_west
        in 293..337 -> R.string.directions_cardinal_north_west
        else -> R.string.directions_cardinal_north
    }
}

fun getRelativeClockTime(degrees: Int, userDegrees: Int): Int {
    val relative = normalizeHeading(degrees - userDegrees)
    val hour = ((relative + 15) / 30) % 12
    return if (hour == 0) 12 else hour
}

fun getRelativeLeftRightLabel(relativeAngle: Int):Int {
    val normalizedAngle = normalizeHeading(relativeAngle)
    return when (normalizedAngle) {
        in 338..360, in 0..22 -> R.string.relative_left_right_direction_ahead
        in 23..67 -> R.string.relative_left_right_direction_ahead_right
        in 68..112 -> R.string.relative_left_right_direction_right
        in 113..157 -> R.string.relative_left_right_direction_behind_right
        in 158..202 -> R.string.relative_left_right_direction_behind
        in 203..247 -> R.string.relative_left_right_direction_behind_left
        in 248..292 -> R.string.relative_left_right_direction_left
        in 293..337 -> R.string.relative_left_right_direction_ahead_left
        else -> R.string.relative_left_right_direction_ahead
    }
}