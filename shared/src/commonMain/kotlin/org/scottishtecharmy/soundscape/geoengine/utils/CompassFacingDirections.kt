package org.scottishtecharmy.soundscape.geoengine.utils

import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import org.scottishtecharmy.soundscape.i18n.StringKey

fun getCompassLabelFacingDirection(
    localized: LocalizedStrings,
    degrees: Int,
    inMotion: Boolean,
    inVehicle: Boolean
): String {
    val normalizedDegrees = normalizeHeading(degrees)
    if (!inMotion) {
        return when (normalizedDegrees) {
            in 338..360, in 0..22 -> localized.get(StringKey.DirectionsFacingN)
            in 23..67 -> localized.get(StringKey.DirectionsFacingNE)
            in 68..112 -> localized.get(StringKey.DirectionsFacingE)
            in 113..157 -> localized.get(StringKey.DirectionsFacingSE)
            in 158..202 -> localized.get(StringKey.DirectionsFacingS)
            in 203..247 -> localized.get(StringKey.DirectionsFacingSW)
            in 248..292 -> localized.get(StringKey.DirectionsFacingW)
            in 293..337 -> localized.get(StringKey.DirectionsFacingNW)
            else -> ""
        }
    } else if (inVehicle) {
        return when (normalizedDegrees) {
            in 338..360, in 0..22 -> localized.get(StringKey.DirectionsTravelingN)
            in 23..67 -> localized.get(StringKey.DirectionsTravelingNE)
            in 68..112 -> localized.get(StringKey.DirectionsTravelingE)
            in 113..157 -> localized.get(StringKey.DirectionsTravelingSE)
            in 158..202 -> localized.get(StringKey.DirectionsTravelingS)
            in 203..247 -> localized.get(StringKey.DirectionsTravelingSW)
            in 248..292 -> localized.get(StringKey.DirectionsTravelingW)
            in 293..337 -> localized.get(StringKey.DirectionsTravelingNW)
            else -> ""
        }
    } else {
        return when (normalizedDegrees) {
            in 338..360, in 0..22 -> localized.get(StringKey.DirectionsHeadingN)
            in 23..67 -> localized.get(StringKey.DirectionsHeadingNE)
            in 68..112 -> localized.get(StringKey.DirectionsHeadingE)
            in 113..157 -> localized.get(StringKey.DirectionsHeadingSE)
            in 158..202 -> localized.get(StringKey.DirectionsHeadingS)
            in 203..247 -> localized.get(StringKey.DirectionsHeadingSW)
            in 248..292 -> localized.get(StringKey.DirectionsHeadingW)
            in 293..337 -> localized.get(StringKey.DirectionsHeadingNW)
            else -> ""
        }
    }
}

fun getCompassLabelFacingDirectionAlong(
    localized: LocalizedStrings,
    degrees: Int,
    placeholder: String,
    inMotion: Boolean,
    inVehicle: Boolean
): String {
    val normalizedDegrees = normalizeHeading(degrees)
    if (!inMotion) {
        return when (normalizedDegrees) {
            in 338..360, in 0..22 -> localized.get(StringKey.DirectionsAlongFacingN, placeholder)
            in 23..67 -> localized.get(StringKey.DirectionsAlongFacingNE, placeholder)
            in 68..112 -> localized.get(StringKey.DirectionsAlongFacingE, placeholder)
            in 113..157 -> localized.get(StringKey.DirectionsAlongFacingSE, placeholder)
            in 158..202 -> localized.get(StringKey.DirectionsAlongFacingS, placeholder)
            in 203..247 -> localized.get(StringKey.DirectionsAlongFacingSW, placeholder)
            in 248..292 -> localized.get(StringKey.DirectionsAlongFacingW, placeholder)
            in 293..337 -> localized.get(StringKey.DirectionsAlongFacingNW, placeholder)
            else -> ""
        }
    } else if (inVehicle) {
        return when (normalizedDegrees) {
            in 338..360, in 0..22 -> localized.get(StringKey.DirectionsAlongTravelingN, placeholder)
            in 23..67 -> localized.get(StringKey.DirectionsAlongTravelingNE, placeholder)
            in 68..112 -> localized.get(StringKey.DirectionsAlongTravelingE, placeholder)
            in 113..157 -> localized.get(StringKey.DirectionsAlongTravelingSE, placeholder)
            in 158..202 -> localized.get(StringKey.DirectionsAlongTravelingS, placeholder)
            in 203..247 -> localized.get(StringKey.DirectionsAlongTravelingSW, placeholder)
            in 248..292 -> localized.get(StringKey.DirectionsAlongTravelingW, placeholder)
            in 293..337 -> localized.get(StringKey.DirectionsAlongTravelingNW, placeholder)
            else -> ""
        }
    } else {
        return when (normalizedDegrees) {
            in 338..360, in 0..22 -> localized.get(StringKey.DirectionsAlongHeadingN, placeholder)
            in 23..67 -> localized.get(StringKey.DirectionsAlongHeadingNE, placeholder)
            in 68..112 -> localized.get(StringKey.DirectionsAlongHeadingE, placeholder)
            in 113..157 -> localized.get(StringKey.DirectionsAlongHeadingSE, placeholder)
            in 158..202 -> localized.get(StringKey.DirectionsAlongHeadingS, placeholder)
            in 203..247 -> localized.get(StringKey.DirectionsAlongHeadingSW, placeholder)
            in 248..292 -> localized.get(StringKey.DirectionsAlongHeadingW, placeholder)
            in 293..337 -> localized.get(StringKey.DirectionsAlongHeadingNW, placeholder)
            else -> ""
        }
    }
}
