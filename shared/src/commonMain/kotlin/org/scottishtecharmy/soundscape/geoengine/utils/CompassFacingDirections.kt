package org.scottishtecharmy.soundscape.geoengine.utils

import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import org.scottishtecharmy.soundscape.i18n.StringKey

/**
 * The plain English compass word for [degrees], used only as a fallback when [localized] is null
 * (e.g. in tests that want English output without pulling in the real string-resource bundle) -
 * matches the wording baked into the directions_(along_)traveling/facing/heading_* resources, so
 * a null-localized caller sees the same text a real English-locale run would produce.
 */
private fun compassWordFallback(degrees: Int): String {
    return when (normalizeHeading(degrees)) {
        in 338..360, in 0..22 -> "north"
        in 23..67 -> "northeast"
        in 68..112 -> "east"
        in 113..157 -> "southeast"
        in 158..202 -> "south"
        in 203..247 -> "southwest"
        in 248..292 -> "west"
        in 293..337 -> "northwest"
        else -> ""
    }
}

fun getCompassLabelFacingDirection(
    localized: LocalizedStrings?,
    degrees: Int,
    inMotion: Boolean,
    inVehicle: Boolean
): String {
    val normalizedDegrees = normalizeHeading(degrees)
    val word = compassWordFallback(degrees)
    if (!inMotion) {
        return when (normalizedDegrees) {
            in 338..360, in 0..22 -> localized?.get(StringKey.DirectionsFacingN) ?: "Facing $word"
            in 23..67 -> localized?.get(StringKey.DirectionsFacingNE) ?: "Facing $word"
            in 68..112 -> localized?.get(StringKey.DirectionsFacingE) ?: "Facing $word"
            in 113..157 -> localized?.get(StringKey.DirectionsFacingSE) ?: "Facing $word"
            in 158..202 -> localized?.get(StringKey.DirectionsFacingS) ?: "Facing $word"
            in 203..247 -> localized?.get(StringKey.DirectionsFacingSW) ?: "Facing $word"
            in 248..292 -> localized?.get(StringKey.DirectionsFacingW) ?: "Facing $word"
            in 293..337 -> localized?.get(StringKey.DirectionsFacingNW) ?: "Facing $word"
            else -> ""
        }
    } else if (inVehicle) {
        return when (normalizedDegrees) {
            in 338..360, in 0..22 -> localized?.get(StringKey.DirectionsTravelingN) ?: "Traveling $word"
            in 23..67 -> localized?.get(StringKey.DirectionsTravelingNE) ?: "Traveling $word"
            in 68..112 -> localized?.get(StringKey.DirectionsTravelingE) ?: "Traveling $word"
            in 113..157 -> localized?.get(StringKey.DirectionsTravelingSE) ?: "Traveling $word"
            in 158..202 -> localized?.get(StringKey.DirectionsTravelingS) ?: "Traveling $word"
            in 203..247 -> localized?.get(StringKey.DirectionsTravelingSW) ?: "Traveling $word"
            in 248..292 -> localized?.get(StringKey.DirectionsTravelingW) ?: "Traveling $word"
            in 293..337 -> localized?.get(StringKey.DirectionsTravelingNW) ?: "Traveling $word"
            else -> ""
        }
    } else {
        return when (normalizedDegrees) {
            in 338..360, in 0..22 -> localized?.get(StringKey.DirectionsHeadingN) ?: "Heading $word"
            in 23..67 -> localized?.get(StringKey.DirectionsHeadingNE) ?: "Heading $word"
            in 68..112 -> localized?.get(StringKey.DirectionsHeadingE) ?: "Heading $word"
            in 113..157 -> localized?.get(StringKey.DirectionsHeadingSE) ?: "Heading $word"
            in 158..202 -> localized?.get(StringKey.DirectionsHeadingS) ?: "Heading $word"
            in 203..247 -> localized?.get(StringKey.DirectionsHeadingSW) ?: "Heading $word"
            in 248..292 -> localized?.get(StringKey.DirectionsHeadingW) ?: "Heading $word"
            in 293..337 -> localized?.get(StringKey.DirectionsHeadingNW) ?: "Heading $word"
            else -> ""
        }
    }
}

fun getCompassLabelFacingDirectionAlong(
    localized: LocalizedStrings?,
    degrees: Int,
    placeholder: String,
    inMotion: Boolean,
    inVehicle: Boolean
): String {
    val normalizedDegrees = normalizeHeading(degrees)
    val word = compassWordFallback(degrees)
    if (!inMotion) {
        return when (normalizedDegrees) {
            in 338..360, in 0..22 ->
                localized?.get(StringKey.DirectionsAlongFacingN, placeholder) ?: "Facing $word along $placeholder"
            in 23..67 ->
                localized?.get(StringKey.DirectionsAlongFacingNE, placeholder) ?: "Facing $word along $placeholder"
            in 68..112 ->
                localized?.get(StringKey.DirectionsAlongFacingE, placeholder) ?: "Facing $word along $placeholder"
            in 113..157 ->
                localized?.get(StringKey.DirectionsAlongFacingSE, placeholder) ?: "Facing $word along $placeholder"
            in 158..202 ->
                localized?.get(StringKey.DirectionsAlongFacingS, placeholder) ?: "Facing $word along $placeholder"
            in 203..247 ->
                localized?.get(StringKey.DirectionsAlongFacingSW, placeholder) ?: "Facing $word along $placeholder"
            in 248..292 ->
                localized?.get(StringKey.DirectionsAlongFacingW, placeholder) ?: "Facing $word along $placeholder"
            in 293..337 ->
                localized?.get(StringKey.DirectionsAlongFacingNW, placeholder) ?: "Facing $word along $placeholder"
            else -> ""
        }
    } else if (inVehicle) {
        return when (normalizedDegrees) {
            in 338..360, in 0..22 ->
                localized?.get(StringKey.DirectionsAlongTravelingN, placeholder) ?: "Traveling $word along $placeholder"
            in 23..67 ->
                localized?.get(StringKey.DirectionsAlongTravelingNE, placeholder) ?: "Traveling $word along $placeholder"
            in 68..112 ->
                localized?.get(StringKey.DirectionsAlongTravelingE, placeholder) ?: "Traveling $word along $placeholder"
            in 113..157 ->
                localized?.get(StringKey.DirectionsAlongTravelingSE, placeholder) ?: "Traveling $word along $placeholder"
            in 158..202 ->
                localized?.get(StringKey.DirectionsAlongTravelingS, placeholder) ?: "Traveling $word along $placeholder"
            in 203..247 ->
                localized?.get(StringKey.DirectionsAlongTravelingSW, placeholder) ?: "Traveling $word along $placeholder"
            in 248..292 ->
                localized?.get(StringKey.DirectionsAlongTravelingW, placeholder) ?: "Traveling $word along $placeholder"
            in 293..337 ->
                localized?.get(StringKey.DirectionsAlongTravelingNW, placeholder) ?: "Traveling $word along $placeholder"
            else -> ""
        }
    } else {
        return when (normalizedDegrees) {
            in 338..360, in 0..22 ->
                localized?.get(StringKey.DirectionsAlongHeadingN, placeholder) ?: "Heading $word along $placeholder"
            in 23..67 ->
                localized?.get(StringKey.DirectionsAlongHeadingNE, placeholder) ?: "Heading $word along $placeholder"
            in 68..112 ->
                localized?.get(StringKey.DirectionsAlongHeadingE, placeholder) ?: "Heading $word along $placeholder"
            in 113..157 ->
                localized?.get(StringKey.DirectionsAlongHeadingSE, placeholder) ?: "Heading $word along $placeholder"
            in 158..202 ->
                localized?.get(StringKey.DirectionsAlongHeadingS, placeholder) ?: "Heading $word along $placeholder"
            in 203..247 ->
                localized?.get(StringKey.DirectionsAlongHeadingSW, placeholder) ?: "Heading $word along $placeholder"
            in 248..292 ->
                localized?.get(StringKey.DirectionsAlongHeadingW, placeholder) ?: "Heading $word along $placeholder"
            in 293..337 ->
                localized?.get(StringKey.DirectionsAlongHeadingNW, placeholder) ?: "Heading $word along $placeholder"
            else -> ""
        }
    }
}
