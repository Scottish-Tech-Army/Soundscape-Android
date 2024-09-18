package org.scottishtecharmy.soundscape.utils


import android.content.Context
import android.content.res.Configuration
import org.scottishtecharmy.soundscape.R

fun getCompassLabelFacingDirection(context: Context, degrees: Int, locale: java.util.Locale): String{
    val configuration = Configuration(context.resources.configuration)
    configuration.setLocale(locale)
    val localizedContext = context.createConfigurationContext(configuration)

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
}

fun getCompassLabelFacingDirectionAlong(context: Context, degrees: Int, placeholder: String, locale: java.util.Locale):String{
    val configuration = Configuration(context.resources.configuration)
    configuration.setLocale(locale)
    val localizedContext = context.createConfigurationContext(configuration)

    return when (degrees) {
        in 338 .. 360, in 0 .. 22 -> context.getString(R.string.directions_along_facing_n, placeholder)
        in 23 .. 67 -> localizedContext.getString(R.string.directions_along_facing_ne, placeholder)
        in 68 .. 112 -> localizedContext.getString(R.string.directions_along_facing_e, placeholder)
        in 113 .. 157 -> localizedContext.getString(R.string.directions_along_facing_se, placeholder)
        in 158 .. 202 -> localizedContext.getString(R.string.directions_along_facing_s, placeholder)
        in 203 .. 247 -> localizedContext.getString(R.string.directions_along_facing_sw, placeholder)
        in 248 .. 292 -> localizedContext.getString(R.string.directions_along_facing_w, placeholder)
        in 293 .. 337 -> localizedContext.getString(R.string.directions_along_facing_nw, placeholder)
        else -> ""
    }
}

fun getRelativeDirectionLabel(context: Context, relativeDirection: Int, locale: java.util.Locale): String{
    val configuration = Configuration(context.resources.configuration)
    configuration.setLocale(locale)
    val localizedContext = context.createConfigurationContext(configuration)

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

