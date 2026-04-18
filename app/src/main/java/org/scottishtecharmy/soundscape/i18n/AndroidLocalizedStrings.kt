package org.scottishtecharmy.soundscape.i18n

import android.content.Context
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.geoengine.utils.ResourceMapper

class AndroidLocalizedStrings(private val context: Context) : LocalizedStrings {
    override fun get(key: StringKey, vararg args: Any?): String =
        context.getString(resId(key), *args)

    override fun getOrNull(key: StringKey, vararg args: Any?): String? =
        runCatching { get(key, *args) }.getOrNull()

    override fun resolveFeatureClass(key: String): String? =
        ResourceMapper.getResourceId(key)?.let { context.getString(it) }

    private fun resId(key: StringKey): Int = when (key) {
        StringKey.ConfectNameTo             -> R.string.confect_name_to
        StringKey.ConfectNameToVia          -> R.string.confect_name_to_via
        StringKey.ConfectNameVia            -> R.string.confect_name_via
        StringKey.ConfectNameJoins          -> R.string.confect_name_joins
        StringKey.ConfectNameDeadEnd        -> R.string.confect_name_dead_end
        StringKey.ConfectNamePavementNextTo -> R.string.confect_name_pavement_next_to
        StringKey.ConfectNamePavement       -> R.string.confect_name_pavement
        StringKey.CalloutsAudioBeacon       -> R.string.callouts_audio_beacon
        StringKey.DirectionsAtPoi           -> R.string.directions_at_poi
        StringKey.DirectionsDirectionAhead  -> R.string.directions_direction_ahead
        StringKey.IntersectionApproachingIntersection -> R.string.intersection_approaching_intersection
        StringKey.DirectionsNameGoesLeft    -> R.string.directions_name_goes_left
        StringKey.DirectionsNameGoesRight   -> R.string.directions_name_goes_right
        StringKey.DirectionsNameContinuesAhead -> R.string.directions_name_continues_ahead
        StringKey.DistanceFormatMeters      -> R.string.distance_format_meters
        StringKey.DistanceFormatFeet        -> R.string.distance_format_feet
        StringKey.DistanceFormatKm          -> R.string.distance_format_km
        StringKey.DistanceFormatMiles       -> R.string.distance_format_miles
        StringKey.RelativeClockDirection    -> R.string.relative_clock_direction
        StringKey.RelativeDegreesDirection  -> R.string.relative_degrees_direction
        StringKey.RelativeLeftRightDirectionAhead       -> R.string.relative_left_right_direction_ahead
        StringKey.RelativeLeftRightDirectionAheadRight  -> R.string.relative_left_right_direction_ahead_right
        StringKey.RelativeLeftRightDirectionRight       -> R.string.relative_left_right_direction_right
        StringKey.RelativeLeftRightDirectionBehindRight -> R.string.relative_left_right_direction_behind_right
        StringKey.RelativeLeftRightDirectionBehind      -> R.string.relative_left_right_direction_behind
        StringKey.RelativeLeftRightDirectionBehindLeft  -> R.string.relative_left_right_direction_behind_left
        StringKey.RelativeLeftRightDirectionLeft        -> R.string.relative_left_right_direction_left
        StringKey.RelativeLeftRightDirectionAheadLeft   -> R.string.relative_left_right_direction_ahead_left
        StringKey.DirectionsCardinalNorth      -> R.string.directions_cardinal_north
        StringKey.DirectionsCardinalNorthEast  -> R.string.directions_cardinal_north_east
        StringKey.DirectionsCardinalEast       -> R.string.directions_cardinal_east
        StringKey.DirectionsCardinalSouthEast  -> R.string.directions_cardinal_south_east
        StringKey.DirectionsCardinalSouth      -> R.string.directions_cardinal_south
        StringKey.DirectionsCardinalSouthWest  -> R.string.directions_cardinal_south_west
        StringKey.DirectionsCardinalWest       -> R.string.directions_cardinal_west
        StringKey.DirectionsCardinalNorthWest  -> R.string.directions_cardinal_north_west
        StringKey.MarkersMarkerWithName     -> R.string.markers_marker_with_name
        StringKey.MarkersGenericName        -> R.string.markers_generic_name
        StringKey.OsmBusStopNamed           -> R.string.osm_bus_stop_named
        StringKey.OsmBusStop                -> R.string.osm_bus_stop
        StringKey.OsmTrainStationNamed      -> R.string.osm_train_station_named
        StringKey.OsmTrainStation           -> R.string.osm_train_station
        StringKey.OsmTramStopNamed          -> R.string.osm_tram_stop_named
        StringKey.OsmTramStop               -> R.string.osm_tram_stop
        StringKey.OsmSubwayNamed            -> R.string.osm_subway_named
        StringKey.OsmSubway                 -> R.string.osm_subway
        StringKey.OsmFerryTerminalNamed     -> R.string.osm_ferry_terminal_named
        StringKey.OsmFerryTerminal          -> R.string.osm_ferry_terminal
        StringKey.OsmMainEntrance           -> R.string.osm_main_entrance
        StringKey.OsmEntrance               -> R.string.osm_entrance
        StringKey.OsmEntranceNamedWithDestination -> R.string.osm_entrance_named_with_destination
        StringKey.OsmEntranceWithDestination      -> R.string.osm_entrance_with_destination
        StringKey.DirectionsNearName               -> R.string.directions_near_name
        StringKey.DirectionsNearRoadAndSettlement  -> R.string.directions_near_road_and_settlement
        StringKey.StreetDescriptionIntersection    -> R.string.street_description_intersection
        StringKey.StreetDescriptionRelativeBefore  -> R.string.street_description_relative_before
        StringKey.StreetDescriptionRelativeAfter   -> R.string.street_description_relative_after
        StringKey.StreetDescriptionBetween         -> R.string.street_description_between
        StringKey.StreetDescriptionUntil           -> R.string.street_description_until
        StringKey.StreetDescriptionSince           -> R.string.street_description_since

        StringKey.DirectionsFacingN        -> R.string.directions_facing_n
        StringKey.DirectionsFacingNE       -> R.string.directions_facing_ne
        StringKey.DirectionsFacingE        -> R.string.directions_facing_e
        StringKey.DirectionsFacingSE       -> R.string.directions_facing_se
        StringKey.DirectionsFacingS        -> R.string.directions_facing_s
        StringKey.DirectionsFacingSW       -> R.string.directions_facing_sw
        StringKey.DirectionsFacingW        -> R.string.directions_facing_w
        StringKey.DirectionsFacingNW       -> R.string.directions_facing_nw

        StringKey.DirectionsHeadingN       -> R.string.directions_heading_n
        StringKey.DirectionsHeadingNE      -> R.string.directions_heading_ne
        StringKey.DirectionsHeadingE       -> R.string.directions_heading_e
        StringKey.DirectionsHeadingSE      -> R.string.directions_heading_se
        StringKey.DirectionsHeadingS       -> R.string.directions_heading_s
        StringKey.DirectionsHeadingSW      -> R.string.directions_heading_sw
        StringKey.DirectionsHeadingW       -> R.string.directions_heading_w
        StringKey.DirectionsHeadingNW      -> R.string.directions_heading_nw

        StringKey.DirectionsTravelingN     -> R.string.directions_traveling_n
        StringKey.DirectionsTravelingNE    -> R.string.directions_traveling_ne
        StringKey.DirectionsTravelingE     -> R.string.directions_traveling_e
        StringKey.DirectionsTravelingSE    -> R.string.directions_traveling_se
        StringKey.DirectionsTravelingS     -> R.string.directions_traveling_s
        StringKey.DirectionsTravelingSW    -> R.string.directions_traveling_sw
        StringKey.DirectionsTravelingW     -> R.string.directions_traveling_w
        StringKey.DirectionsTravelingNW    -> R.string.directions_traveling_nw

        StringKey.DirectionsAlongFacingN   -> R.string.directions_along_facing_n
        StringKey.DirectionsAlongFacingNE  -> R.string.directions_along_facing_ne
        StringKey.DirectionsAlongFacingE   -> R.string.directions_along_facing_e
        StringKey.DirectionsAlongFacingSE  -> R.string.directions_along_facing_se
        StringKey.DirectionsAlongFacingS   -> R.string.directions_along_facing_s
        StringKey.DirectionsAlongFacingSW  -> R.string.directions_along_facing_sw
        StringKey.DirectionsAlongFacingW   -> R.string.directions_along_facing_w
        StringKey.DirectionsAlongFacingNW  -> R.string.directions_along_facing_nw

        StringKey.DirectionsAlongHeadingN  -> R.string.directions_along_heading_n
        StringKey.DirectionsAlongHeadingNE -> R.string.directions_along_heading_ne
        StringKey.DirectionsAlongHeadingE  -> R.string.directions_along_heading_e
        StringKey.DirectionsAlongHeadingSE -> R.string.directions_along_heading_se
        StringKey.DirectionsAlongHeadingS  -> R.string.directions_along_heading_s
        StringKey.DirectionsAlongHeadingSW -> R.string.directions_along_heading_sw
        StringKey.DirectionsAlongHeadingW  -> R.string.directions_along_heading_w
        StringKey.DirectionsAlongHeadingNW -> R.string.directions_along_heading_nw

        StringKey.DirectionsAlongTravelingN  -> R.string.directions_along_traveling_n
        StringKey.DirectionsAlongTravelingNE -> R.string.directions_along_traveling_ne
        StringKey.DirectionsAlongTravelingE  -> R.string.directions_along_traveling_e
        StringKey.DirectionsAlongTravelingSE -> R.string.directions_along_traveling_se
        StringKey.DirectionsAlongTravelingS  -> R.string.directions_along_traveling_s
        StringKey.DirectionsAlongTravelingSW -> R.string.directions_along_traveling_sw
        StringKey.DirectionsAlongTravelingW  -> R.string.directions_along_traveling_w
        StringKey.DirectionsAlongTravelingNW -> R.string.directions_along_traveling_nw

        StringKey.GeneralErrorFindLocationError -> R.string.general_error_location_services_find_location_error
        StringKey.StationaryOnWay                -> R.string.stationary_on_way
        StringKey.CalloutsNothingToCallOutNow    -> R.string.callouts_nothing_to_call_out_now
        StringKey.CalloutsNoNearbyMarkers        -> R.string.callouts_no_nearby_markers
    }
}
