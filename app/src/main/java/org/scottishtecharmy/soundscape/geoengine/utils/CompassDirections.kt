package org.scottishtecharmy.soundscape.geoengine.utils


import android.content.Context
import org.scottishtecharmy.soundscape.R

fun getCompassLabelFacingDirection(localizedContext: Context,
                                   degrees: Int,
                                   inMotion: Boolean,
                                   inVehicle: Boolean
): String{
    if(!inMotion) {
        return when (degrees) {
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
        return when (degrees) {
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
        return when (degrees) {
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
    if(!inMotion) {
        return when (degrees) {
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
        return when (degrees) {
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
        return when (degrees) {
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

fun getRelativeDirectionLabel(localizedContext: Context,
                              relativeDirection: Int): String{
    return when (relativeDirection) {
        0 -> localizedContext.getString(R.string.directions_direction_behind)
        1 -> localizedContext.getString(R.string.directions_direction_behind_to_the_left)
        2 -> localizedContext.getString(R.string.directions_direction_to_the_left)
        3 -> localizedContext.getString(R.string.directions_direction_ahead_to_the_left)
        4 -> localizedContext.getString(R.string.directions_direction_ahead)
        5 -> localizedContext.getString(R.string.directions_direction_ahead_to_the_right)
        6 -> localizedContext.getString(R.string.directions_direction_to_the_right)
        7 -> localizedContext.getString(R.string.directions_direction_behind_to_the_right)
        else -> "Unknown"
    }
}

