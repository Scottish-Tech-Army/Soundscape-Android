package org.scottishtecharmy.soundscape.i18n

import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.scottishtecharmy.soundscape.geoengine.utils.ResourceMapper
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.callouts_audio_beacon
import org.scottishtecharmy.soundscape.resources.callouts_no_nearby_markers
import org.scottishtecharmy.soundscape.resources.callouts_nothing_to_call_out_now
import org.scottishtecharmy.soundscape.resources.confect_name_dead_end
import org.scottishtecharmy.soundscape.resources.confect_name_joins
import org.scottishtecharmy.soundscape.resources.confect_name_pavement
import org.scottishtecharmy.soundscape.resources.confect_name_pavement_next_to
import org.scottishtecharmy.soundscape.resources.confect_name_to
import org.scottishtecharmy.soundscape.resources.confect_name_to_via
import org.scottishtecharmy.soundscape.resources.confect_name_via
import org.scottishtecharmy.soundscape.resources.directions_along_facing_e
import org.scottishtecharmy.soundscape.resources.directions_along_facing_n
import org.scottishtecharmy.soundscape.resources.directions_along_facing_ne
import org.scottishtecharmy.soundscape.resources.directions_along_facing_nw
import org.scottishtecharmy.soundscape.resources.directions_along_facing_s
import org.scottishtecharmy.soundscape.resources.directions_along_facing_se
import org.scottishtecharmy.soundscape.resources.directions_along_facing_sw
import org.scottishtecharmy.soundscape.resources.directions_along_facing_w
import org.scottishtecharmy.soundscape.resources.directions_along_heading_e
import org.scottishtecharmy.soundscape.resources.directions_along_heading_n
import org.scottishtecharmy.soundscape.resources.directions_along_heading_ne
import org.scottishtecharmy.soundscape.resources.directions_along_heading_nw
import org.scottishtecharmy.soundscape.resources.directions_along_heading_s
import org.scottishtecharmy.soundscape.resources.directions_along_heading_se
import org.scottishtecharmy.soundscape.resources.directions_along_heading_sw
import org.scottishtecharmy.soundscape.resources.directions_along_heading_w
import org.scottishtecharmy.soundscape.resources.directions_along_traveling_e
import org.scottishtecharmy.soundscape.resources.directions_along_traveling_n
import org.scottishtecharmy.soundscape.resources.directions_along_traveling_ne
import org.scottishtecharmy.soundscape.resources.directions_along_traveling_nw
import org.scottishtecharmy.soundscape.resources.directions_along_traveling_s
import org.scottishtecharmy.soundscape.resources.directions_along_traveling_se
import org.scottishtecharmy.soundscape.resources.directions_along_traveling_sw
import org.scottishtecharmy.soundscape.resources.directions_along_traveling_w
import org.scottishtecharmy.soundscape.resources.directions_at_poi
import org.scottishtecharmy.soundscape.resources.directions_cardinal_east
import org.scottishtecharmy.soundscape.resources.directions_cardinal_north
import org.scottishtecharmy.soundscape.resources.directions_cardinal_north_east
import org.scottishtecharmy.soundscape.resources.directions_cardinal_north_west
import org.scottishtecharmy.soundscape.resources.directions_cardinal_south
import org.scottishtecharmy.soundscape.resources.directions_cardinal_south_east
import org.scottishtecharmy.soundscape.resources.directions_cardinal_south_west
import org.scottishtecharmy.soundscape.resources.directions_cardinal_west
import org.scottishtecharmy.soundscape.resources.directions_direction_ahead
import org.scottishtecharmy.soundscape.resources.directions_facing_e
import org.scottishtecharmy.soundscape.resources.directions_facing_n
import org.scottishtecharmy.soundscape.resources.directions_facing_ne
import org.scottishtecharmy.soundscape.resources.directions_facing_nw
import org.scottishtecharmy.soundscape.resources.directions_facing_s
import org.scottishtecharmy.soundscape.resources.directions_facing_se
import org.scottishtecharmy.soundscape.resources.directions_facing_sw
import org.scottishtecharmy.soundscape.resources.directions_facing_w
import org.scottishtecharmy.soundscape.resources.directions_heading_e
import org.scottishtecharmy.soundscape.resources.directions_heading_n
import org.scottishtecharmy.soundscape.resources.directions_heading_ne
import org.scottishtecharmy.soundscape.resources.directions_heading_nw
import org.scottishtecharmy.soundscape.resources.directions_heading_s
import org.scottishtecharmy.soundscape.resources.directions_heading_se
import org.scottishtecharmy.soundscape.resources.directions_heading_sw
import org.scottishtecharmy.soundscape.resources.directions_heading_w
import org.scottishtecharmy.soundscape.resources.directions_name_continues_ahead
import org.scottishtecharmy.soundscape.resources.directions_name_goes_left
import org.scottishtecharmy.soundscape.resources.directions_name_goes_right
import org.scottishtecharmy.soundscape.resources.directions_near_name
import org.scottishtecharmy.soundscape.resources.directions_near_road_and_settlement
import org.scottishtecharmy.soundscape.resources.directions_traveling_e
import org.scottishtecharmy.soundscape.resources.directions_traveling_n
import org.scottishtecharmy.soundscape.resources.directions_traveling_ne
import org.scottishtecharmy.soundscape.resources.directions_traveling_nw
import org.scottishtecharmy.soundscape.resources.directions_traveling_s
import org.scottishtecharmy.soundscape.resources.directions_traveling_se
import org.scottishtecharmy.soundscape.resources.directions_traveling_sw
import org.scottishtecharmy.soundscape.resources.directions_traveling_w
import org.scottishtecharmy.soundscape.resources.distance_format_feet
import org.scottishtecharmy.soundscape.resources.distance_format_km
import org.scottishtecharmy.soundscape.resources.distance_format_meters
import org.scottishtecharmy.soundscape.resources.distance_format_miles
import org.scottishtecharmy.soundscape.resources.general_error_location_services_find_location_error
import org.scottishtecharmy.soundscape.resources.intersection_approaching_intersection
import org.scottishtecharmy.soundscape.resources.markers_generic_name
import org.scottishtecharmy.soundscape.resources.markers_marker_with_name
import org.scottishtecharmy.soundscape.resources.osm_bus_stop
import org.scottishtecharmy.soundscape.resources.osm_bus_stop_named
import org.scottishtecharmy.soundscape.resources.osm_entrance
import org.scottishtecharmy.soundscape.resources.osm_entrance_named_with_destination
import org.scottishtecharmy.soundscape.resources.osm_entrance_with_destination
import org.scottishtecharmy.soundscape.resources.osm_ferry_terminal
import org.scottishtecharmy.soundscape.resources.osm_ferry_terminal_named
import org.scottishtecharmy.soundscape.resources.osm_main_entrance
import org.scottishtecharmy.soundscape.resources.osm_subway
import org.scottishtecharmy.soundscape.resources.osm_subway_named
import org.scottishtecharmy.soundscape.resources.osm_train_station
import org.scottishtecharmy.soundscape.resources.osm_train_station_named
import org.scottishtecharmy.soundscape.resources.osm_tram_stop
import org.scottishtecharmy.soundscape.resources.osm_tram_stop_named
import org.scottishtecharmy.soundscape.resources.relative_clock_direction
import org.scottishtecharmy.soundscape.resources.relative_degrees_direction
import org.scottishtecharmy.soundscape.resources.relative_left_right_direction_ahead
import org.scottishtecharmy.soundscape.resources.relative_left_right_direction_ahead_left
import org.scottishtecharmy.soundscape.resources.relative_left_right_direction_ahead_right
import org.scottishtecharmy.soundscape.resources.relative_left_right_direction_behind
import org.scottishtecharmy.soundscape.resources.relative_left_right_direction_behind_left
import org.scottishtecharmy.soundscape.resources.relative_left_right_direction_behind_right
import org.scottishtecharmy.soundscape.resources.relative_left_right_direction_left
import org.scottishtecharmy.soundscape.resources.relative_left_right_direction_right
import org.scottishtecharmy.soundscape.resources.stationary_on_way
import org.scottishtecharmy.soundscape.resources.street_description_between
import org.scottishtecharmy.soundscape.resources.street_description_intersection
import org.scottishtecharmy.soundscape.resources.street_description_relative_after
import org.scottishtecharmy.soundscape.resources.street_description_relative_before
import org.scottishtecharmy.soundscape.resources.street_description_since
import org.scottishtecharmy.soundscape.resources.street_description_until

class ComposeLocalizedStrings : LocalizedStrings {
    override fun get(key: StringKey, vararg args: Any?): String =
        runBlocking { getString(resId(key), *args.map { it ?: "" }.toTypedArray()) }

    override fun getOrNull(key: StringKey, vararg args: Any?): String? =
        runCatching { get(key, *args) }.getOrNull()

    override fun resolveFeatureClass(key: String): String? =
        ResourceMapper.getStringResource(key)?.let {
            runBlocking { getString(it) }
        }

    private fun resId(key: StringKey): StringResource = when (key) {
        StringKey.ConfectNameTo             -> Res.string.confect_name_to
        StringKey.ConfectNameToVia          -> Res.string.confect_name_to_via
        StringKey.ConfectNameVia            -> Res.string.confect_name_via
        StringKey.ConfectNameJoins          -> Res.string.confect_name_joins
        StringKey.ConfectNameDeadEnd        -> Res.string.confect_name_dead_end
        StringKey.ConfectNamePavementNextTo -> Res.string.confect_name_pavement_next_to
        StringKey.ConfectNamePavement       -> Res.string.confect_name_pavement
        StringKey.CalloutsAudioBeacon       -> Res.string.callouts_audio_beacon
        StringKey.DirectionsAtPoi           -> Res.string.directions_at_poi
        StringKey.DirectionsDirectionAhead  -> Res.string.directions_direction_ahead
        StringKey.IntersectionApproachingIntersection -> Res.string.intersection_approaching_intersection
        StringKey.DirectionsNameGoesLeft    -> Res.string.directions_name_goes_left
        StringKey.DirectionsNameGoesRight   -> Res.string.directions_name_goes_right
        StringKey.DirectionsNameContinuesAhead -> Res.string.directions_name_continues_ahead
        StringKey.DistanceFormatMeters      -> Res.string.distance_format_meters
        StringKey.DistanceFormatFeet        -> Res.string.distance_format_feet
        StringKey.DistanceFormatKm          -> Res.string.distance_format_km
        StringKey.DistanceFormatMiles       -> Res.string.distance_format_miles
        StringKey.RelativeClockDirection    -> Res.string.relative_clock_direction
        StringKey.RelativeDegreesDirection  -> Res.string.relative_degrees_direction
        StringKey.RelativeLeftRightDirectionAhead       -> Res.string.relative_left_right_direction_ahead
        StringKey.RelativeLeftRightDirectionAheadRight  -> Res.string.relative_left_right_direction_ahead_right
        StringKey.RelativeLeftRightDirectionRight       -> Res.string.relative_left_right_direction_right
        StringKey.RelativeLeftRightDirectionBehindRight -> Res.string.relative_left_right_direction_behind_right
        StringKey.RelativeLeftRightDirectionBehind      -> Res.string.relative_left_right_direction_behind
        StringKey.RelativeLeftRightDirectionBehindLeft  -> Res.string.relative_left_right_direction_behind_left
        StringKey.RelativeLeftRightDirectionLeft        -> Res.string.relative_left_right_direction_left
        StringKey.RelativeLeftRightDirectionAheadLeft   -> Res.string.relative_left_right_direction_ahead_left
        StringKey.DirectionsCardinalNorth      -> Res.string.directions_cardinal_north
        StringKey.DirectionsCardinalNorthEast  -> Res.string.directions_cardinal_north_east
        StringKey.DirectionsCardinalEast       -> Res.string.directions_cardinal_east
        StringKey.DirectionsCardinalSouthEast  -> Res.string.directions_cardinal_south_east
        StringKey.DirectionsCardinalSouth      -> Res.string.directions_cardinal_south
        StringKey.DirectionsCardinalSouthWest  -> Res.string.directions_cardinal_south_west
        StringKey.DirectionsCardinalWest       -> Res.string.directions_cardinal_west
        StringKey.DirectionsCardinalNorthWest  -> Res.string.directions_cardinal_north_west
        StringKey.MarkersMarkerWithName     -> Res.string.markers_marker_with_name
        StringKey.MarkersGenericName        -> Res.string.markers_generic_name
        StringKey.OsmBusStopNamed           -> Res.string.osm_bus_stop_named
        StringKey.OsmBusStop                -> Res.string.osm_bus_stop
        StringKey.OsmTrainStationNamed      -> Res.string.osm_train_station_named
        StringKey.OsmTrainStation           -> Res.string.osm_train_station
        StringKey.OsmTramStopNamed          -> Res.string.osm_tram_stop_named
        StringKey.OsmTramStop               -> Res.string.osm_tram_stop
        StringKey.OsmSubwayNamed            -> Res.string.osm_subway_named
        StringKey.OsmSubway                 -> Res.string.osm_subway
        StringKey.OsmFerryTerminalNamed     -> Res.string.osm_ferry_terminal_named
        StringKey.OsmFerryTerminal          -> Res.string.osm_ferry_terminal
        StringKey.OsmMainEntrance           -> Res.string.osm_main_entrance
        StringKey.OsmEntrance               -> Res.string.osm_entrance
        StringKey.OsmEntranceNamedWithDestination -> Res.string.osm_entrance_named_with_destination
        StringKey.OsmEntranceWithDestination      -> Res.string.osm_entrance_with_destination
        StringKey.DirectionsNearName               -> Res.string.directions_near_name
        StringKey.DirectionsNearRoadAndSettlement  -> Res.string.directions_near_road_and_settlement
        StringKey.StreetDescriptionIntersection    -> Res.string.street_description_intersection
        StringKey.StreetDescriptionRelativeBefore  -> Res.string.street_description_relative_before
        StringKey.StreetDescriptionRelativeAfter   -> Res.string.street_description_relative_after
        StringKey.StreetDescriptionBetween         -> Res.string.street_description_between
        StringKey.StreetDescriptionUntil           -> Res.string.street_description_until
        StringKey.StreetDescriptionSince           -> Res.string.street_description_since

        StringKey.DirectionsFacingN        -> Res.string.directions_facing_n
        StringKey.DirectionsFacingNE       -> Res.string.directions_facing_ne
        StringKey.DirectionsFacingE        -> Res.string.directions_facing_e
        StringKey.DirectionsFacingSE       -> Res.string.directions_facing_se
        StringKey.DirectionsFacingS        -> Res.string.directions_facing_s
        StringKey.DirectionsFacingSW       -> Res.string.directions_facing_sw
        StringKey.DirectionsFacingW        -> Res.string.directions_facing_w
        StringKey.DirectionsFacingNW       -> Res.string.directions_facing_nw

        StringKey.DirectionsHeadingN       -> Res.string.directions_heading_n
        StringKey.DirectionsHeadingNE      -> Res.string.directions_heading_ne
        StringKey.DirectionsHeadingE       -> Res.string.directions_heading_e
        StringKey.DirectionsHeadingSE      -> Res.string.directions_heading_se
        StringKey.DirectionsHeadingS       -> Res.string.directions_heading_s
        StringKey.DirectionsHeadingSW      -> Res.string.directions_heading_sw
        StringKey.DirectionsHeadingW       -> Res.string.directions_heading_w
        StringKey.DirectionsHeadingNW      -> Res.string.directions_heading_nw

        StringKey.DirectionsTravelingN     -> Res.string.directions_traveling_n
        StringKey.DirectionsTravelingNE    -> Res.string.directions_traveling_ne
        StringKey.DirectionsTravelingE     -> Res.string.directions_traveling_e
        StringKey.DirectionsTravelingSE    -> Res.string.directions_traveling_se
        StringKey.DirectionsTravelingS     -> Res.string.directions_traveling_s
        StringKey.DirectionsTravelingSW    -> Res.string.directions_traveling_sw
        StringKey.DirectionsTravelingW     -> Res.string.directions_traveling_w
        StringKey.DirectionsTravelingNW    -> Res.string.directions_traveling_nw

        StringKey.DirectionsAlongFacingN   -> Res.string.directions_along_facing_n
        StringKey.DirectionsAlongFacingNE  -> Res.string.directions_along_facing_ne
        StringKey.DirectionsAlongFacingE   -> Res.string.directions_along_facing_e
        StringKey.DirectionsAlongFacingSE  -> Res.string.directions_along_facing_se
        StringKey.DirectionsAlongFacingS   -> Res.string.directions_along_facing_s
        StringKey.DirectionsAlongFacingSW  -> Res.string.directions_along_facing_sw
        StringKey.DirectionsAlongFacingW   -> Res.string.directions_along_facing_w
        StringKey.DirectionsAlongFacingNW  -> Res.string.directions_along_facing_nw

        StringKey.DirectionsAlongHeadingN  -> Res.string.directions_along_heading_n
        StringKey.DirectionsAlongHeadingNE -> Res.string.directions_along_heading_ne
        StringKey.DirectionsAlongHeadingE  -> Res.string.directions_along_heading_e
        StringKey.DirectionsAlongHeadingSE -> Res.string.directions_along_heading_se
        StringKey.DirectionsAlongHeadingS  -> Res.string.directions_along_heading_s
        StringKey.DirectionsAlongHeadingSW -> Res.string.directions_along_heading_sw
        StringKey.DirectionsAlongHeadingW  -> Res.string.directions_along_heading_w
        StringKey.DirectionsAlongHeadingNW -> Res.string.directions_along_heading_nw

        StringKey.DirectionsAlongTravelingN  -> Res.string.directions_along_traveling_n
        StringKey.DirectionsAlongTravelingNE -> Res.string.directions_along_traveling_ne
        StringKey.DirectionsAlongTravelingE  -> Res.string.directions_along_traveling_e
        StringKey.DirectionsAlongTravelingSE -> Res.string.directions_along_traveling_se
        StringKey.DirectionsAlongTravelingS  -> Res.string.directions_along_traveling_s
        StringKey.DirectionsAlongTravelingSW -> Res.string.directions_along_traveling_sw
        StringKey.DirectionsAlongTravelingW  -> Res.string.directions_along_traveling_w
        StringKey.DirectionsAlongTravelingNW -> Res.string.directions_along_traveling_nw

        StringKey.GeneralErrorFindLocationError -> Res.string.general_error_location_services_find_location_error
        StringKey.StationaryOnWay                -> Res.string.stationary_on_way
        StringKey.CalloutsNothingToCallOutNow    -> Res.string.callouts_nothing_to_call_out_now
        StringKey.CalloutsNoNearbyMarkers        -> Res.string.callouts_no_nearby_markers
    }
}
