package org.scottishtecharmy.soundscape.geoengine

import org.scottishtecharmy.soundscape.audio.AudioType
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.utils.SuperCategoryId
import org.scottishtecharmy.soundscape.geoengine.utils.calculateHeadingOffset
import org.scottishtecharmy.soundscape.geoengine.utils.getCompassLabel
import org.scottishtecharmy.soundscape.geoengine.utils.getCompassLabelFacingDirectionAlong
import org.scottishtecharmy.soundscape.geoengine.utils.getRelativeClockTime
import org.scottishtecharmy.soundscape.geoengine.utils.getRelativeLeftRightLabel
import org.scottishtecharmy.soundscape.geoengine.utils.normalizeHeading
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
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

/**
 * Tracks the last railway station passed while travelling by train, so travel-mode reverse
 * geocoding can describe progress along the line as "distance since {station}" rather than just
 * naming the line - see [UserGeometry.probablyOnTrain]. A single reverse-geocode call has no
 * memory of previous ones, so this is held by the caller (AutoCallout) and passed in each time.
 */
class LastStationTracker {
    var name: String? = null
    var location: LngLatAlt? = null

    fun updateStation(newName: String, newLocation: LngLatAlt?) {
        name = newName
        location = newLocation
    }
}

/**
 * Tracks how recently something notable (a major road junction, or a passed large POI) was last
 * announced while travelling by car/bus, so a quiet stretch with nothing major nearby can still
 * fall back to mentioning a minor road junction instead of staying silent indefinitely. A single
 * reverse-geocode call has no memory of previous ones, so this is held by the caller (AutoCallout)
 * and passed in/updated each time - see the junction selection in [travellingReverseGeocodeName]
 * and AutoCallout.buildCalloutForVehicleLandmark.
 */
class NotableVehicleEventTracker {
    private var lastEventTimestampMs: Long? = null

    fun recordEvent(timestampMilliseconds: Long) {
        lastEventTimestampMs = timestampMilliseconds
    }

    fun quietFor(timestampMilliseconds: Long, thresholdMilliseconds: Long): Boolean {
        val last = lastEventTimestampMs ?: return true
        return (timestampMilliseconds - last) > thresholdMilliseconds
    }
}

// How long nothing notable (major junction/large POI) needs to have been announced before a minor
// road junction becomes eligible for a callout too - see NotableVehicleEventTracker.
private const val MINOR_JUNCTION_QUIET_THRESHOLD_MS = 90_000L

// Highway junction "class" tiers (from the junction feature's "class" property - see
// extractHighwayJunctions), used to prefer major junctions and only fall back to minor ones after
// a quiet spell. Deliberately excludes paths/tracks/service roads and anything with no known class
// - a junction with an unrecognised or missing class is never called out.
private val majorHighwayJunctionClasses = setOf("motorway", "trunk", "primary")
private val minorHighwayJunctionClasses =
    setOf("secondary", "tertiary", "residential", "unclassified", "living_street")

/**
 * @param text the text to actually speak.
 * @param dedupText the text to use for callout-history comparison - defaults to [text], but for
 * callouts that embed an ever-changing value (e.g. a live "distance since X") this should be the
 * same text with that value left out, so the callout can still dedup against an earlier one that
 * differs only in that value. See [PositionedString.dedupText].
 */
private data class ReverseGeocodeText(val text: String, val dedupText: String = text)

private fun travellingReverseGeocodeName(
    userGeometry: UserGeometry,
    gridState: GridState,
    settlementGrid: GridState,
    localized: LocalizedStrings?,
    lastStationTracker: LastStationTracker? = null,
    notableEventTracker: NotableVehicleEventTracker? = null,
): ReverseGeocodeText? {
    val location = userGeometry.location
    if (!gridState.isLocationWithinGrid(location)) return null

    // Passing a bus/tram/train stop is announced separately - see
    // AutoCallout.buildCalloutForVehicleTransitStop - since it needs to sweep the path travelled
    // since the last location update (a point-radius check here would miss most stops, as this
    // function is only checked periodically and a stop's detection radius is easily crossed
    // between checks at driving speed).

    val probablyOnTrain = userGeometry.probablyOnTrain()

    // Note the most recent railway station we've passed close to, so progress along the line can
    // be described as "distance since {station}" further down. A station is commonly two
    // separate features in this tile schema: a bare railway=station point (often just named after
    // the settlement, e.g. "Milngavie") and a building=train_station footprint with the fuller
    // name commuters would recognise (e.g. "Milngavie Station") - we want either.
    if (probablyOnTrain && (lastStationTracker != null)) {
        val nearestStation = gridState.getFeatureTree(TreeId.TRANSIT_STOPS)
            .getNearestFeature(location, gridState.ruler, 50.0) as? MvtFeature
        val isStation = (nearestStation?.featureValue == "station") ||
            (nearestStation?.featureValue == "train_station")
        if (isStation && (nearestStation.name != null) &&
            (nearestStation.name != lastStationTracker.name)
        ) {
            lastStationTracker.updateStation(
                nearestStation.name!!, (nearestStation.geometry as? Point)?.coordinates
            )
        }
    }

    // Prefer the map-matched way (the road/railway we're actually confirmed to be on) over an
    // independent nearest-feature search, which can pick the wrong road at junctions or parallel
    // carriageways. Since we're confirmed to be on it (rather than merely near it), phrase it as
    // "On X" rather than "Near X". A train is matched against the separate railway network -
    // there's no independent-search fallback for it, since a lower-confidence guess at a railway
    // line is much less useful than one for a road (you can't be "near" a railway in the way you
    // can be near a road, e.g. on a parallel street - either the matcher has locked onto the line
    // you're travelling on, or it hasn't).
    // This whole function only ever runs while in a vehicle (see AutoCallout.buildCalloutForRoadSense),
    // so the fallback search is restricted to TreeId.ROADS - a car/bus can't be on a footway/cycleway,
    // and TreeId.ROADS_AND_PATHS would otherwise let one get picked as the nearest "road".
    val nearestRoad = if (probablyOnTrain) {
        userGeometry.mapMatchedRailway
    } else {
        userGeometry.mapMatchedWay ?: gridState.getNearestFeature(
            TreeId.ROADS, gridState.ruler, location, 100.0
        ) as Way?
    }
    val roadName = nearestRoad?.getName(null, gridState, localized, true)?.takeIf { it.isNotEmpty() }

    // The direction of travel along a road (e.g. "Traveling north along M8") - using the
    // map-matched heading, which snaps to the road's own tangent (see UserGeometry.snappedHeading)
    // - is only meaningful for an actual road, not a railway line. Every road callout below goes
    // through roadPhrase() so they read consistently, rather than some saying "On M8" and others
    // "Traveling north along M8"; it only falls back to the bare "On M8" form when there's no
    // heading to work with (getCompassLabelFacingDirectionAlong has its own English fallback text
    // for a null localized, e.g. in tests, so this doesn't need to check for that separately).
    val travelHeadingDegrees = if (!probablyOnTrain) userGeometry.snappedHeading()?.toInt() else null
    fun roadPhrase(name: String): String =
        if (travelHeadingDegrees != null) {
            getCompassLabelFacingDirectionAlong(localized, travelHeadingDegrees, name, true, true)
        } else {
            localized?.get(StringKey.DirectionsOnRoad, name) ?: "On $name"
        }

    // Check if we're near a highway junction (motorway exit, interchange etc.) - not relevant
    // when travelling by train. Major junctions (motorway/trunk/primary) are always eligible;
    // minor ones only become eligible once nothing notable has been announced for a while, so a
    // quiet residential junction doesn't compete with a nearby motorway interchange. Junctions
    // with an unrecognised/missing class (this also covers paths/tracks/service roads, which
    // should never be called out) are never eligible.
    if (!probablyOnTrain) {
        val junctionTree = gridState.getFeatureTree(TreeId.HIGHWAY_JUNCTIONS)
        val nearbyJunctions = junctionTree.getNearestCollection(location, 500.0, 5, gridState.ruler)
        val allowMinorJunctions = notableEventTracker?.quietFor(
            userGeometry.timestampMilliseconds, MINOR_JUNCTION_QUIET_THRESHOLD_MS
        ) ?: true
        val nearestJunction = nearbyJunctions.features.firstOrNull { feature ->
            when ((feature as MvtFeature).properties?.get("class") as? String) {
                in majorHighwayJunctionClasses -> true
                in minorHighwayJunctionClasses -> allowMinorJunctions
                else -> false
            }
        } as MvtFeature?
        if (nearestJunction != null) {
            val ref = nearestJunction.ref
            val name = nearestJunction.name
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
                notableEventTracker?.recordEvent(userGeometry.timestampMilliseconds)
                // dedupText excludes the direction of travel (unlike the spoken text) - on a
                // winding road, the compass direction can shift tick to tick while the road and
                // junction stay the same, and that shouldn't be treated as a new thing to
                // announce (see the equivalent reasoning below for the plain "on road" callouts).
                if (roadName != null) {
                    return ReverseGeocodeText(
                        text = if (travelHeadingDegrees != null) {
                            "${roadPhrase(roadName)} " + (
                                localized?.get(StringKey.DirectionsAtJunctionInline, junctionText)
                                    ?: "at $junctionText"
                                )
                        } else {
                            localized?.get(StringKey.DirectionsOnRoadAtJunction, roadName, junctionText)
                                ?: "On $roadName at $junctionText"
                        },
                        dedupText = "On $roadName at $junctionText"
                    )
                }
                return ReverseGeocodeText(
                    localized?.get(StringKey.DirectionsNearName, junctionText) ?: "Near $junctionText"
                )
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
            return ReverseGeocodeText(
                localized?.get(StringKey.DirectionsAtPoi, poiName) ?: "At $poiName"
            )
        }
    }

    // Nearest settlements with Nominatim-style proximities. Hamlet/village/town are discrete
    // points a road passes near or through, so they're eligible for directional "towards X"/
    // "away from X"/"near X" phrasing below (see nearestSettlementIsCity). A city is a large,
    // often-merged conurbation where that phrasing would be misleading - you can't be "towards
    // Glasgow" while already inside its urban area - so it keeps the vaguer "close to" instead.
    var nearestSettlement = settlementGrid.getFeatureTree(TreeId.SETTLEMENT_HAMLET)
        .getNearestFeature(location, settlementGrid.ruler, 1000.0) as MvtFeature?
    var nearestSettlementName = nearestSettlement?.name
    var nearestSettlementIsCity = false
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
                nearestSettlementIsCity = nearestSettlementName != null
            }
        }
    }

    if (roadName != null) {
        val phrase = roadPhrase(roadName)

        // Distance since the last station is only worth mentioning alongside something else worth
        // describing (a nearby settlement) - otherwise it ends up as a standalone callout that
        // fires on every location update as the distance keeps climbing, which is far too
        // frequent on its own (see real train-1/train-2.gpx replays).
        val sinceStationName = lastStationTracker?.name
        val sinceStationLocation = lastStationTracker?.location
        if (probablyOnTrain && (nearestSettlementName != null) &&
            (sinceStationName != null) && (sinceStationLocation != null)
        ) {
            // The distance climbs on every call, so it's never suppressed as a duplicate if it
            // were included in the dedup comparison - dedupText leaves it out, so this only
            // re-announces when the road/settlement/station combination actually changes.
            val distanceText = formatDistanceAndDirection(
                gridState.ruler.distance(location, sinceStationLocation), null, localized
            )
            return ReverseGeocodeText(
                text = localized?.get(
                    StringKey.DirectionsOnRoadAndSettlementSince,
                    roadName, nearestSettlementName, distanceText, sinceStationName
                ) ?: "On $roadName and close to $nearestSettlementName, $distanceText since $sinceStationName",
                // Keep the station in the dedup key (unlike the distance, which is never
                // included) - a genuinely new "since {station}" is worth a fresh announcement,
                // only the ever-climbing distance number itself shouldn't defeat deduping. This
                // key is never spoken, so it doesn't need localizing.
                dedupText = "On $roadName and close to $nearestSettlementName since $sinceStationName"
            )
        }

        // Trains don't get the directional towards/away from/near settlement phrasing below
        // (travelHeadingDegrees is always null for a train - see roadPhrase above), just a plain
        // settlement mention.
        if (probablyOnTrain) {
            return ReverseGeocodeText(
                if (nearestSettlementName != null) {
                    localized?.get(
                        StringKey.DirectionsOnRoadAndSettlement, roadName, nearestSettlementName
                    ) ?: "On $roadName and close to $nearestSettlementName"
                } else {
                    phrase
                }
            )
        }

        // A discrete settlement (hamlet/village/town) the road runs towards, away from, or past
        // gets phrased relative to the direction of travel; a city keeps the vaguer "close to"
        // (see nearestSettlementIsCity above). Both need a heading to work out the relative
        // bearing, so without one this falls through to the plain road/settlement mention below.
        val settlementLocation = (nearestSettlement?.geometry as? Point)?.coordinates
        if ((nearestSettlementName != null) && (travelHeadingDegrees != null) &&
            (settlementLocation != null)
        ) {
            if (nearestSettlementIsCity) {
                return ReverseGeocodeText(
                    text = "$phrase " + (
                        localized?.get(StringKey.DirectionsCloseToSettlementInline, nearestSettlementName)
                            ?: "close to $nearestSettlementName"
                        ),
                    // Excludes the direction of travel - see the dedupText comment further below.
                    dedupText = "On $roadName close to $nearestSettlementName"
                )
            }

            val headingOffset = calculateHeadingOffset(
                travelHeadingDegrees.toDouble(), gridState.ruler.bearing(location, settlementLocation)
            )
            val (settlementPhrase, dedupSuffix) = when {
                headingOffset <= 45.0 -> {
                    val distanceText = formatDistanceAndDirection(
                        gridState.ruler.distance(location, settlementLocation), null, localized
                    )
                    Pair(
                        localized?.get(
                            StringKey.DirectionsTowardsSettlement, nearestSettlementName, distanceText
                        ) ?: "towards $nearestSettlementName, $distanceText away",
                        "towards $nearestSettlementName"
                    )
                }
                headingOffset >= 135.0 -> {
                    val distanceText = formatDistanceAndDirection(
                        gridState.ruler.distance(location, settlementLocation), null, localized
                    )
                    Pair(
                        localized?.get(
                            StringKey.DirectionsAwayFromSettlement, nearestSettlementName, distanceText
                        ) ?: "away from $nearestSettlementName, $distanceText away",
                        "away from $nearestSettlementName"
                    )
                }
                else -> Pair(
                    localized?.get(StringKey.DirectionsNearSettlementInline, nearestSettlementName)
                        ?: "near $nearestSettlementName",
                    "near $nearestSettlementName"
                )
            }
            return ReverseGeocodeText(
                text = "$phrase $settlementPhrase",
                // Excludes both the ever-changing distance (see the "since station" case above)
                // and the direction of travel - on a winding road the compass direction can shift
                // tick to tick while the road and the towards/away/near relationship stay the
                // same, and that alone shouldn't trigger a fresh announcement.
                dedupText = "On $roadName $dedupSuffix"
            )
        }

        return ReverseGeocodeText(
            text = if (nearestSettlementName != null) {
                localized?.get(
                    StringKey.DirectionsOnRoadAndSettlement, roadName, nearestSettlementName
                ) ?: "On $roadName and close to $nearestSettlementName"
            } else {
                phrase
            },
            // Excludes the direction of travel (see the equivalent dedupText comments above) - on
            // a winding road, phrase's compass direction can shift tick to tick purely from the
            // road's own bends, well before the road or settlement actually changes.
            dedupText = if (nearestSettlementName != null) {
                "On $roadName and close to $nearestSettlementName"
            } else {
                roadName
            }
        )
    }

    if (nearestSettlementName != null) {
        return ReverseGeocodeText(
            localized?.get(StringKey.DirectionsNearName, nearestSettlementName)
                ?: "Near $nearestSettlementName"
        )
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
    lastStationTracker: LastStationTracker? = null,
    notableEventTracker: NotableVehicleEventTracker? = null,
): PositionedString? {
    val description =
        travellingReverseGeocodeName(
            userGeometry, gridState, settlementGrid, localized, lastStationTracker,
            notableEventTracker
        ) ?: return null
    return PositionedString(
        text = description.text,
        dedupText = description.dedupText,
        location = userGeometry.location,
        type = AudioType.LOCALIZED,
    )
}
