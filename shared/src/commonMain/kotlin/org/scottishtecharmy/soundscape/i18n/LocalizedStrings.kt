package org.scottishtecharmy.soundscape.i18n

interface LocalizedStrings {
    fun get(key: StringKey, vararg args: Any?): String
    fun getOrNull(key: StringKey, vararg args: Any?): String?

    /**
     * Resolves a string whose wording depends on how many of something it describes, e.g. "1
     * mile" against "5 miles".
     *
     * [quantity] only selects the wording and is separate from [args] - the number the user
     * actually sees is passed in as an argument like any other, because it isn't always a whole
     * number. For the ones that aren't, ask [fractionalPluralQuantity] what to select on.
     */
    fun getPlural(key: PluralKey, quantity: Int, vararg args: Any?): String

    /**
     * The quantity to pass to [getPlural] for an amount that isn't a whole number, e.g. the "1.4"
     * of "1.4 km".
     *
     * Plural resources can only select on a whole number, so the fraction never reaches CLDR's
     * `v` operand and we have to name an integer that lands in the category the real value would.
     * 2 is right for most languages, but not for the ones whose `one` covers an integer part of 0
     * or 1: French and Portuguese take the singular, "1,4 kilomètre" and "1,4 quilómetro".
     */
    val fractionalPluralQuantity: Int get() = 2

    fun resolveFeatureClass(key: String): String?
}

/**
 * Keys for strings that decline with a quantity, kept apart from [StringKey] because they resolve
 * to a different kind of resource (`<plurals>` rather than `<string>`) and need the extra
 * quantity argument to resolve at all.
 */
enum class PluralKey {
    DistanceMeters,
    DistanceFeet,
    DistanceKm,
    DistanceMiles,
    DistanceKmA11y,
}

enum class StringKey {
    ConfectNameTo,
    ConfectNameToVia,
    ConfectNameVia,
    ConfectNameJoins,
    ConfectNameDeadEnd,
    ConfectNamePavementNextTo,
    ConfectNamePavement,
    ConfectNameNextTo,
    CalloutsAudioBeacon,
    CalloutsAudioBeaconDistance,
    DirectionsAtPoi,
    DirectionsDirectionAhead,
    IntersectionApproachingIntersection,
    DirectionsNameGoesLeft,
    DirectionsNameGoesRight,
    DirectionsNameContinuesAhead,
    BytesFormatB,
    BytesFormatBA11y,
    BytesFormatKb,
    BytesFormatKbA11y,
    BytesFormatMb,
    BytesFormatMbA11y,
    BytesFormatGb,
    BytesFormatGbA11y,
    BytesFormatTb,
    BytesFormatTbA11y,
    NumberDecimalSeparator,
    NumberDecimalSeparatorA11y,
    RelativeClockDirection,
    RelativeDegreesDirection,
    RelativeLeftRightDirectionAhead,
    RelativeLeftRightDirectionAheadRight,
    RelativeLeftRightDirectionRight,
    RelativeLeftRightDirectionBehindRight,
    RelativeLeftRightDirectionBehind,
    RelativeLeftRightDirectionBehindLeft,
    RelativeLeftRightDirectionLeft,
    RelativeLeftRightDirectionAheadLeft,
    DirectionsCardinalNorth,
    DirectionsCardinalNorthEast,
    DirectionsCardinalEast,
    DirectionsCardinalSouthEast,
    DirectionsCardinalSouth,
    DirectionsCardinalSouthWest,
    DirectionsCardinalWest,
    DirectionsCardinalNorthWest,
    DirectionsCardinalNorthBound,
    DirectionsCardinalNorthEastBound,
    DirectionsCardinalEastBound,
    DirectionsCardinalSouthEastBound,
    DirectionsCardinalSouthBound,
    DirectionsCardinalSouthWestBound,
    DirectionsCardinalWestBound,
    DirectionsCardinalNorthWestBound,
    MarkersMarkerWithName,
    MarkersGenericName,
    OsmBusStopNamed,
    OsmBusStop,
    OsmTrainStationNamed,
    OsmTrainStation,
    OsmTramStopNamed,
    OsmTramStop,
    OsmSubwayNamed,
    OsmSubway,
    OsmFerryTerminalNamed,
    OsmFerryTerminal,
    OsmMainEntrance,
    OsmEntrance,
    OsmEntranceNamedWithDestination,
    OsmEntranceWithDestination,
    DirectionsNearName,
    DirectionsStreetSettlement,
    DirectionsNearRoadAndSettlement,
    DirectionsTransitStopNearSettlement,
    DirectionsTransitStopNearPoi,
    DirectionsTransitStopStreetLocality,
    DirectionsTransitStopStreetLocalityBound,
    DirectionsTransitStopCommonNameBound,
    DirectionsTransitStopWithLandmark,
    DirectionsJunctionWithRef,
    DirectionsJunctionWithRefAndName,
    DirectionsRoadWithRefAndName,
    DirectionsOnRoad,
    DirectionsOnRoadAndSettlement,
    DirectionsOnRoadAtJunction,
    DirectionsAtJunctionInline,
    DirectionsOnRoadAndSettlementSince,
    DirectionsTowardsSettlement,
    DirectionsAwayFromSettlement,
    DirectionsNearSettlementInline,
    DirectionsCloseToSettlementInline,
    DirectionsGenericTrain,
    DirectionsCrossingWaterway,
    DirectionsCrossingRailwayGeneric,
    DirectionsGoingUnderRailway,
    DirectionsGoingUnderRailwayGeneric,
    DirectionsEnteringTunnel,
    DirectionsEnteringTunnelNamed,
    StreetDescriptionIntersection,
    StreetDescriptionRelativeBefore,
    StreetDescriptionRelativeAfter,
    StreetDescriptionRelativeNear,
    StreetDescriptionBetween,
    StreetDescriptionUntil,
    StreetDescriptionSince,

    // Compass facing directions (stationary)
    DirectionsFacingN,
    DirectionsFacingNE,
    DirectionsFacingE,
    DirectionsFacingSE,
    DirectionsFacingS,
    DirectionsFacingSW,
    DirectionsFacingW,
    DirectionsFacingNW,

    // Compass heading directions (walking)
    DirectionsHeadingN,
    DirectionsHeadingNE,
    DirectionsHeadingE,
    DirectionsHeadingSE,
    DirectionsHeadingS,
    DirectionsHeadingSW,
    DirectionsHeadingW,
    DirectionsHeadingNW,

    // Compass traveling directions (vehicle)
    DirectionsTravelingN,
    DirectionsTravelingNE,
    DirectionsTravelingE,
    DirectionsTravelingSE,
    DirectionsTravelingS,
    DirectionsTravelingSW,
    DirectionsTravelingW,
    DirectionsTravelingNW,

    // Compass facing along road (stationary)
    DirectionsAlongFacingN,
    DirectionsAlongFacingNE,
    DirectionsAlongFacingE,
    DirectionsAlongFacingSE,
    DirectionsAlongFacingS,
    DirectionsAlongFacingSW,
    DirectionsAlongFacingW,
    DirectionsAlongFacingNW,

    // Compass heading along road (walking)
    DirectionsAlongHeadingN,
    DirectionsAlongHeadingNE,
    DirectionsAlongHeadingE,
    DirectionsAlongHeadingSE,
    DirectionsAlongHeadingS,
    DirectionsAlongHeadingSW,
    DirectionsAlongHeadingW,
    DirectionsAlongHeadingNW,

    // Compass traveling along road (vehicle)
    DirectionsAlongTravelingN,
    DirectionsAlongTravelingNE,
    DirectionsAlongTravelingE,
    DirectionsAlongTravelingSE,
    DirectionsAlongTravelingS,
    DirectionsAlongTravelingSW,
    DirectionsAlongTravelingW,
    DirectionsAlongTravelingNW,

    // Callout UI strings
    GeneralErrorFindLocationError,
    StationaryOnWay,
    CalloutsNothingToCallOutNow,
    CalloutsNoNearbyMarkers,

    // Assistant (Siri / voice shortcut) confirmations and errors, spoken by the
    // assistant rather than by Soundscape's own audio engine.
    ActionRouteStarted,
    ActionRouteStopped,
    ActionBeaconStarted,
    ActionBeaconStopped,
    ActionNoSuchRoute,
    ActionNoSuchMarker,
    ActionItemNotFound,
    ActionNoMarkersSaved,
    ActionNoLocation,
    ActionNoMapData,
    ActionNoRouteActive,
    ActionAtRouteStart,
    ActionAtRouteEnd,
    ActionNoOtherWaypoints,
    ActionServiceNotRunning,
    MenuNoRoutes,

    // Left behind by the voice-control removal in 75c0bc59 and still translated, so the
    // list actions reuse them rather than adding new copy for the same sentences.
    VoiceCmdRoutesList,
    VoiceCmdMarkersList,
}
