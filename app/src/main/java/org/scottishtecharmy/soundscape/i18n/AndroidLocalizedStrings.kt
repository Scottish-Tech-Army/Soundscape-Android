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
    }
}
