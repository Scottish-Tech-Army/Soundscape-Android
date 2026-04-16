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

/**
 * getTextForFeature returns text describing the feature for callouts. Usually it returns a name
 * or if it doesn't have one then a localized description of the type of feature it is e.g. bike
 * parking, or style. Some types of Feature have more info e.g. bus stops and railway stations
 * name from the OSM tag rather than an actual name.
 */
fun getTextForFeature(localized: LocalizedStrings?, feature: MvtFeature): TextForFeature {
    var generic = false
    val name = feature.name
    val entranceType = feature.properties?.get("entrance") as String?
    val featureValue = feature.featureValue
    val isMarker = feature.superCategory == SuperCategoryId.MARKER

    if (feature.superCategory == SuperCategoryId.HOUSENUMBER) {
        return TextForFeature(name ?: feature.housenumber ?: "", false)
    }

    if (isMarker) {
        val description = feature.properties?.get("description")
        var text = name
        if (description != null) {
            if (text != null)
                text += ", $description"
            else
                text = description as String
        }
        return if (text != null)
            TextForFeature(
                localized?.get(StringKey.MarkersMarkerWithName, text) ?: "Marker. $text",
                false
            )
        else
            TextForFeature(localized?.get(StringKey.MarkersGenericName) ?: "Marker", false)
    }

    var text = name

    val namedTransit = when (featureValue) {
        "bus_stop" -> Pair(StringKey.OsmBusStopNamed, StringKey.OsmBusStop)
        "station" -> Pair(StringKey.OsmTrainStationNamed, StringKey.OsmTrainStation)
        "tram_stop" -> Pair(StringKey.OsmTramStopNamed, StringKey.OsmTramStop)
        "subway" -> Pair(StringKey.OsmSubwayNamed, StringKey.OsmSubway)
        "ferry_terminal" -> Pair(StringKey.OsmFerryTerminalNamed, StringKey.OsmFerryTerminal)
        else -> null
    }
    if (namedTransit != null) {
        text = if (name != null)
            localized?.get(namedTransit.first, name) ?: "$name Transit Stop"
        else
            localized?.get(namedTransit.second) ?: "Transit"
    }

    if (entranceType != null) {
        val entranceName = feature.properties?.get("entrance_name") as String?
        val destinationName = text

        val entranceText =
            if (entranceType == "main")
                localized?.get(StringKey.OsmMainEntrance) ?: "Main entrance"
            else
                localized?.get(StringKey.OsmEntrance) ?: "Entrance"

        text = if (entranceName != null) {
            localized?.get(
                StringKey.OsmEntranceNamedWithDestination,
                destinationName,
                entranceText,
                entranceName,
            ) ?: "$destinationName $entranceText to $entranceName"
        } else {
            localized?.get(StringKey.OsmEntranceWithDestination, destinationName, entranceText)
                ?: "$destinationName $entranceText"
        }
    }

    if ((feature.featureClass == null) && (feature.featureSubClass == null)) {
        return if (text == null)
            TextForFeature("", true)
        else
            TextForFeature(text, false)
    }

    val osmText = if (localized != null) {
        feature.featureClass?.let { localized.resolveFeatureClass(it) }
            ?: feature.featureSubClass?.let { localized.resolveFeatureClass(it) }
    } else {
        "OSM Feature"
    }
    var additionalText: String? = null
    if (text == null) {
        text = osmText
        generic = true
    } else {
        additionalText = osmText
    }
    val capitalizedText = text?.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase() else it.toString()
    }
    if (capitalizedText == null)
        return TextForFeature("", generic, additionalText)

    return TextForFeature(capitalizedText, generic, additionalText)
}

fun formatDistanceAndDirection(
    distance: Double,
    heading: Double?,
    localized: LocalizedStrings?,
    userHeading: Double? = null,
    relativeTimeMode: String = "ClockFace",
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
        val formatted = formatDecimal(bigUnits.toDouble(), 2)
        distanceText = localized?.get(
            if (metric) StringKey.DistanceFormatKm else StringKey.DistanceFormatMiles,
            formatted
        ) ?: "$formatted km"
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
                        (localized?.get(StringKey.RelativeClockDirection, timeHeading.toString())
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

private fun formatDecimal(value: Double, decimals: Int): String {
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
    return if (decimals == 0) "$sign$whole"
    else "$sign$whole.${frac.toString().padStart(decimals, '0')}"
}

private fun travellingReverseGeocodeName(
    location: LngLatAlt,
    gridState: GridState,
    settlementGrid: GridState,
    localized: LocalizedStrings?,
): String? {
    if (!gridState.isLocationWithinGrid(location)) return null

    // Check if we're near a bus/tram/train stop.
    val busStopTree = gridState.getFeatureTree(TreeId.TRANSIT_STOPS)
    val nearestBusStop = busStopTree.getNearestFeature(location, gridState.ruler, 20.0)
    if (nearestBusStop != null) {
        val busStopText = getTextForFeature(localized, nearestBusStop as MvtFeature)
        if (!busStopText.generic) {
            return localized?.get(StringKey.DirectionsNearName, busStopText.text)
                ?: "Near ${busStopText.text}"
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

    val nearestRoad = gridState.getNearestFeature(
        TreeId.ROADS_AND_PATHS, gridState.ruler, location, 100.0
    ) as Way?
    if (nearestRoad != null) {
        val roadName = nearestRoad.getName(null, gridState, localized, true)
        if (roadName.isNotEmpty()) {
            return if (nearestSettlementName != null) {
                localized?.get(
                    StringKey.DirectionsNearRoadAndSettlement, roadName, nearestSettlementName
                ) ?: "Near $roadName and close to $nearestSettlementName"
            } else {
                localized?.get(StringKey.DirectionsNearName, roadName) ?: "Near $roadName"
            }
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
    val name = travellingReverseGeocodeName(userGeometry.location, gridState, settlementGrid, localized)
        ?: return null
    return PositionedString(
        text = name,
        location = userGeometry.location,
        type = AudioType.LOCALIZED,
    )
}
