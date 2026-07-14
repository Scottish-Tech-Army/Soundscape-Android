package org.scottishtecharmy.soundscape.geoengine

import org.scottishtecharmy.soundscape.audio.AudioType
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.utils.SuperCategoryId
import org.scottishtecharmy.soundscape.geoengine.utils.getCompassLabel
import org.scottishtecharmy.soundscape.geoengine.utils.getRelativeClockTime
import org.scottishtecharmy.soundscape.geoengine.utils.getRelativeLeftRightLabel
import org.scottishtecharmy.soundscape.geoengine.utils.normalizeHeading
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import org.scottishtecharmy.soundscape.i18n.StringKey
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt

/**
 * We're going to round metric as documented for iOS:
 *  For metric units, we round all distances less than 1000 meters to the nearest 5 meters and all
 *  distances over 1000 meters to the nearest 50 meters.
 *
 * The iOS imperial docs are wrong, and in fact distances are all in feet and we can round in the
 * same way as metric.
 */
var metric = true

fun formatDistanceAndDirection(
    distance: Double,
    heading: Double?,
    localized: LocalizedStrings?,
    userHeading: Double? = null,
    relativeTimeMode: String = "ClockFace",
    forAccessibility: Boolean = false,
): String {
    var units = distance
    var bigUnitDivisor = 100
    if (!metric) {
        units = (distance * 1.09361 * 3)
        bigUnitDivisor = (176 * 3)
    }

    val roundToNearest = if (units < 1000) 5.0 else 50.0
    val roundedDistance =
        ((units + (roundToNearest / 2)) / roundToNearest).toInt() * roundToNearest

    val distanceText: String
    if (roundedDistance < 1000) {
        val wholeUnits = roundedDistance.toInt()
        distanceText = localized?.get(
            if (metric) StringKey.DistanceFormatMeters else StringKey.DistanceFormatFeet,
            wholeUnits.toString()
        ) ?: "$wholeUnits metres"
    } else {
        val bigUnits = (roundedDistance.toInt() / 10).toFloat() / bigUnitDivisor
        val separator = decimalSeparator(localized, forAccessibility)
        val formatted = formatDecimal(
            bigUnits.toDouble(),
            decimals = 2,
            separator = separator,
            spaceFractionalDigits = forAccessibility,
        )
        val bigUnitKey = if (metric) {
            if (forAccessibility) StringKey.DistanceFormatKmA11y else StringKey.DistanceFormatKm
        } else {
            StringKey.DistanceFormatMiles
        }
        distanceText = localized?.get(bigUnitKey, formatted) ?: "$formatted km"
    }

    var headingText = ""
    if (heading != null) {
        if (userHeading == null) {
            if (localized != null)
                headingText = ", " + localized.get(getCompassLabel(heading.toInt()))
        } else {
            when (relativeTimeMode) {
                "ClockFace" -> {
                    val timeHeading = getRelativeClockTime(heading.toInt(), userHeading.toInt())
                    headingText = ", " +
                            (localized?.get(
                                StringKey.RelativeClockDirection,
                                timeHeading.toString()
                            )
                                ?: "at $timeHeading o'clock")
                }

                "Degrees" -> {
                    val relativeHeading = (heading - userHeading)
                    val degrees = normalizeHeading(((relativeHeading / 5.0).roundToInt() * 5))
                    headingText = ", " +
                            (localized?.get(StringKey.RelativeDegreesDirection, degrees.toString())
                                ?: "at $degrees degrees")
                }

                "LeftRight" -> {
                    val labelKey = getRelativeLeftRightLabel((heading - userHeading).toInt())
                    headingText = ", " + (localized?.get(labelKey) ?: when (labelKey) {
                        StringKey.RelativeLeftRightDirectionAhead -> "Ahead"
                        StringKey.RelativeLeftRightDirectionAheadRight -> "Ahead right"
                        StringKey.RelativeLeftRightDirectionRight -> "Right"
                        StringKey.RelativeLeftRightDirectionBehindRight -> "Behind right"
                        StringKey.RelativeLeftRightDirectionBehind -> "Behind"
                        StringKey.RelativeLeftRightDirectionBehindLeft -> "Behind left"
                        StringKey.RelativeLeftRightDirectionLeft -> "Left"
                        StringKey.RelativeLeftRightDirectionAheadLeft -> "Ahead left"
                        else -> "Unknown"
                    })
                }
            }
        }
    }
    return "$distanceText$headingText"
}

internal fun decimalSeparator(localized: LocalizedStrings?, forAccessibility: Boolean): String {
    val key = if (forAccessibility) StringKey.NumberDecimalSeparatorA11y
    else StringKey.NumberDecimalSeparator
    return localized?.get(key) ?: if (forAccessibility) " point " else "."
}

internal fun formatDecimal(
    value: Double,
    decimals: Int,
    separator: String = ".",
    spaceFractionalDigits: Boolean = false,
): String {
    val factor = when (decimals) {
        0 -> 1L
        1 -> 10L
        2 -> 100L
        3 -> 1000L
        else -> 100L
    }
    val rounded = round(value * factor).toLong()
    val sign = if (rounded < 0) "-" else ""
    val absVal = abs(rounded)
    val whole = absVal / factor
    val frac = absVal % factor
    if (decimals == 0) return "$sign$whole"
    val fracStr = frac.toString().padStart(decimals, '0')
    val fracOut = if (spaceFractionalDigits) fracStr.toCharArray().joinToString(" ") else fracStr
    return "$sign$whole$separator$fracOut"
}

private fun travellingReverseGeocodeName(
    userGeometry: UserGeometry,
    gridState: GridState,
    settlementGrid: GridState,
    localized: LocalizedStrings?,
): String? {
    val location = userGeometry.location
    if (!gridState.isLocationWithinGrid(location)) return null

    // Check if we're near a bus/tram/train stop.
    val busStopTree = gridState.getFeatureTree(TreeId.TRANSIT_STOPS)
    val nearestBusStop = busStopTree.getNearestFeature(location, gridState.ruler, 20.0)
    if (nearestBusStop != null) {
        val busStopText = (nearestBusStop as MvtFeature).getText(localized)
        if (!busStopText.generic) {
            return localized?.get(StringKey.DirectionsNearName, busStopText.text)
                ?: "Near ${busStopText.text}"
        }
    }

    val probablyOnTrain = userGeometry.probablyOnTrain()

    // Prefer the map-matched way (the road/railway we're actually confirmed to be on) over an
    // independent nearest-feature search, which can pick the wrong road at junctions or parallel
    // carriageways. Since we're confirmed to be on it (rather than merely near it), phrase it as
    // "On X" rather than "Near X". A train is matched against the separate railway network -
    // there's no independent-search fallback for it, since a lower-confidence guess at a railway
    // line is much less useful than one for a road (you can't be "near" a railway in the way you
    // can be near a road, e.g. on a parallel street - either the matcher has locked onto the line
    // you're travelling on, or it hasn't).
    val nearestRoad = if (probablyOnTrain) {
        userGeometry.mapMatchedRailway
    } else {
        userGeometry.mapMatchedWay ?: gridState.getNearestFeature(
            TreeId.ROADS_AND_PATHS, gridState.ruler, location, 100.0
        ) as Way?
    }
    val roadName = nearestRoad?.getName(null, gridState, localized, true)?.takeIf { it.isNotEmpty() }

    // Check if we're near a highway junction (motorway exit, interchange etc.) - not relevant
    // when travelling by train.
    if (!probablyOnTrain) {
        val junctionTree = gridState.getFeatureTree(TreeId.HIGHWAY_JUNCTIONS)
        val nearestJunction = junctionTree.getNearestFeature(location, gridState.ruler, 500.0)
        if (nearestJunction != null) {
            val junction = nearestJunction as MvtFeature
            val ref = junction.properties?.get("ref") as? String
            val name = junction.name
            val junctionText = if (ref != null) {
                if (name != null) {
                    localized?.get(StringKey.DirectionsJunctionWithRefAndName, ref, name)
                        ?: "Junction $ref, $name"
                } else {
                    localized?.get(StringKey.DirectionsJunctionWithRef, ref) ?: "Junction $ref"
                }
            } else {
                name
            }
            if (junctionText != null) {
                return if (roadName != null) {
                    localized?.get(StringKey.DirectionsOnRoadAtJunction, roadName, junctionText)
                        ?: "On $roadName at $junctionText"
                } else {
                    localized?.get(StringKey.DirectionsNearName, junctionText)
                        ?: "Near $junctionText"
                }
            }
        }
    }

    // Check if we're inside a POI
    val gridPoiTree = gridState.getFeatureTree(TreeId.POIS)
    val insidePois = gridPoiTree.getContainingPolygons(location)
    for (poi in insidePois) {
        val mvtPoi = poi as MvtFeature
        val poiName = mvtPoi.name
        if (poiName != null) {
            return localized?.get(StringKey.DirectionsAtPoi, poiName) ?: "At $poiName"
        }
    }

    // Nearest settlements with Nominatim-style proximities.
    var nearestSettlement = settlementGrid.getFeatureTree(TreeId.SETTLEMENT_HAMLET)
        .getNearestFeature(location, settlementGrid.ruler, 1000.0) as MvtFeature?
    var nearestSettlementName = nearestSettlement?.name
    if (nearestSettlementName == null) {
        nearestSettlement = settlementGrid.getFeatureTree(TreeId.SETTLEMENT_VILLAGE)
            .getNearestFeature(location, settlementGrid.ruler, 2000.0) as MvtFeature?
        nearestSettlementName = nearestSettlement?.name
        if (nearestSettlementName == null) {
            nearestSettlement = settlementGrid.getFeatureTree(TreeId.SETTLEMENT_TOWN)
                .getNearestFeature(location, settlementGrid.ruler, 4000.0) as MvtFeature?
            nearestSettlementName = nearestSettlement?.name
            if (nearestSettlementName == null) {
                nearestSettlement = settlementGrid.getFeatureTree(TreeId.SETTLEMENT_CITY)
                    .getNearestFeature(location, settlementGrid.ruler, 15000.0) as MvtFeature?
                nearestSettlementName = nearestSettlement?.name
            }
        }
    }

    if (roadName != null) {
        return if (nearestSettlementName != null) {
            localized?.get(
                StringKey.DirectionsOnRoadAndSettlement, roadName, nearestSettlementName
            ) ?: "On $roadName and close to $nearestSettlementName"
        } else {
            localized?.get(StringKey.DirectionsOnRoad, roadName) ?: "On $roadName"
        }
    }

    if (nearestSettlementName != null) {
        return localized?.get(StringKey.DirectionsNearName, nearestSettlementName)
            ?: "Near $nearestSettlementName"
    }

    return null
}

/** Reverse geocodes a location into 1 of 4 possible states
 * - within a POI
 * - alongside a road
 * - general location
 * - unknown location.
 */
fun describeReverseGeocode(
    userGeometry: UserGeometry,
    gridState: GridState,
    settlementGrid: GridState,
    localized: LocalizedStrings?,
): PositionedString? {
    val name =
        travellingReverseGeocodeName(userGeometry, gridState, settlementGrid, localized)
            ?: return null
    return PositionedString(
        text = name,
        location = userGeometry.location,
        type = AudioType.LOCALIZED,
    )
}
