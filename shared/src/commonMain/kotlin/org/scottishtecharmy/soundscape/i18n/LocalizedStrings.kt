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
     * number ("1.4 miles" is quantity 2, since no language treats a fraction as singular).
     */
    fun getPlural(key: PluralKey, quantity: Int, vararg args: Any?): String

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
}
