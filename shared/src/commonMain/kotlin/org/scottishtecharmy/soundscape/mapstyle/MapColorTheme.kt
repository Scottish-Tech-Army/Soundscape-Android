package org.scottishtecharmy.soundscape.mapstyle

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/**
 * Converts an ARGB integer color to a CSS rgba() string.
 */
fun argbToRgba(argb: Int): String {
    val a = (argb shr 24) and 0xFF
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    val alphaInt = (a * 100) / 255
    val alphaWhole = alphaInt / 100
    val alphaFrac = alphaInt % 100
    return "rgba($r, $g, $b, $alphaWhole.${alphaFrac.toString().padStart(2, '0')})"
}

fun colorString(color: String): JsonElement = JsonPrimitive(color)

/**
 * Runtime color overrides applied per-layer on top of the base theme.
 * Used for accessible mode where many layers get foreground/background/semantic colors.
 * Keys are layer IDs, values are maps of paint property name to CSS color string.
 */
data class LayerPaintOverrides(
    val backgroundColor: String? = null,
    val lineColor: Map<String, String> = emptyMap(),
    val fillColor: Map<String, String> = emptyMap(),
    val fillExtrusionColor: Map<String, String> = emptyMap(),
    val textColor: Map<String, String> = emptyMap(),
    val textHaloColor: Map<String, String> = emptyMap(),
)

/**
 * Creates the accessible mode layer overrides using theme-derived colors.
 * These override line-color, fill-color, text-color, and text-halo-color
 * for many layers, matching the behavior of the main branch's runtime style adjustments.
 *
 * @param foregroundColor CSS color string (e.g. from MaterialTheme.colorScheme.onBackground)
 * @param backgroundColor CSS color string (e.g. from MaterialTheme.colorScheme.background)
 * @param motorwayColor CSS color string, default "rgba(128, 128, 255, 1.00)"
 * @param trunkRoadColor CSS color string, default "rgba(255, 200, 30, 1.00)"
 * @param waterColor CSS color string, default "rgba(200, 200, 240, 1.00)"
 * @param greeneryColor CSS color string, default "rgba(240, 255, 240, 0.08)"
 */
fun accessibleLayerOverrides(
    foregroundColor: String,
    backgroundColor: String,
    motorwayColor: String = "rgba(128, 128, 255, 1.00)",
    trunkRoadColor: String = "rgba(255, 200, 30, 1.00)",
    waterColor: String = "rgba(200, 200, 240, 1.00)",
    greeneryColor: String = "rgba(240, 255, 240, 0.08)",
): LayerPaintOverrides {
    val lineOverrides = mapOf(
        "aeroway_runway" to foregroundColor,
        "aeroway_taxiway" to foregroundColor,
        "tunnel_motorway_link_casing" to foregroundColor,
        "tunnel_service_track_casing" to foregroundColor,
        "tunnel_link_casing" to foregroundColor,
        "tunnel_street_casing" to foregroundColor,
        "tunnel_secondary_tertiary_casing" to foregroundColor,
        "tunnel_trunk_primary_casing" to foregroundColor,
        "tunnel_motorway_casing" to foregroundColor,
        "tunnel_path_pedestrian" to foregroundColor,
        "tunnel_motorway_link" to motorwayColor,
        "tunnel_service_track" to backgroundColor,
        "tunnel_link" to backgroundColor,
        "tunnel_minor" to backgroundColor,
        "tunnel_secondary_tertiary" to trunkRoadColor,
        "tunnel_trunk_primary" to trunkRoadColor,
        "tunnel_motorway" to motorwayColor,
        "tunnel_major_rail" to foregroundColor,
        "tunnel_major_rail_hatching" to foregroundColor,
        "tunnel_transit_rail" to foregroundColor,
        "tunnel_transit_rail_hatching" to foregroundColor,
        "road_area_pattern" to foregroundColor,
        "road_motorway_link_casing" to foregroundColor,
        "road_service_track_casing" to foregroundColor,
        "road_link_casing" to foregroundColor,
        "road_minor_casing" to foregroundColor,
        "road_secondary_tertiary_casing" to foregroundColor,
        "road_trunk_primary_casing" to foregroundColor,
        "road_motorway_casing" to foregroundColor,
        "road_path_pedestrian" to foregroundColor,
        "road_motorway_link" to motorwayColor,
        "road_service_track" to backgroundColor,
        "road_link" to backgroundColor,
        "road_minor" to backgroundColor,
        "road_secondary_tertiary" to trunkRoadColor,
        "road_trunk_primary" to trunkRoadColor,
        "road_motorway" to motorwayColor,
        "road_major_rail" to foregroundColor,
        "road_major_rail_hatching" to foregroundColor,
        "road_transit_rail" to foregroundColor,
        "road_transit_rail_hatching" to foregroundColor,
        "bridge_motorway_link_casing" to foregroundColor,
        "bridge_service_track_casing" to foregroundColor,
        "bridge_link_casing" to foregroundColor,
        "bridge_street_casing" to foregroundColor,
        "bridge_path_pedestrian_casing" to foregroundColor,
        "bridge_secondary_tertiary_casing" to foregroundColor,
        "bridge_trunk_primary_casing" to foregroundColor,
        "bridge_motorway_casing" to foregroundColor,
        "bridge_path_pedestrian" to backgroundColor,
        "bridge_motorway_link" to motorwayColor,
        "bridge_service_track" to backgroundColor,
        "bridge_link" to backgroundColor,
        "bridge_street" to backgroundColor,
        "bridge_secondary_tertiary" to trunkRoadColor,
        "bridge_trunk_primary" to trunkRoadColor,
        "bridge_motorway" to motorwayColor,
        "bridge_major_rail" to foregroundColor,
        "bridge_major_rail_hatching" to foregroundColor,
        "bridge_transit_rail" to foregroundColor,
        "bridge_transit_rail_hatching" to foregroundColor,
        "waterway_tunnel" to waterColor,
        "waterway_river" to waterColor,
        "waterway_other" to waterColor,
        "park_outline" to foregroundColor,
        "boundary_3" to foregroundColor,
        "boundary_2_z0-4" to foregroundColor,
        "boundary_2_z5-" to foregroundColor,
    )

    val fillOverrides = mapOf(
        "park" to greeneryColor,
        "landcover_wood" to greeneryColor,
        "landcover_grass" to greeneryColor,
        "landuse_cemetery" to greeneryColor,
    )

    val fillExtrusionOverrides = mapOf(
        "building-3d" to foregroundColor,
    )

    // All symbol layers get foreground text and background halo
    val textColorOverrides = mutableMapOf<String, String>()
    val textHaloOverrides = mutableMapOf<String, String>()
    // These will be applied to all symbol layers in the builder

    return LayerPaintOverrides(
        backgroundColor = backgroundColor,
        lineColor = lineOverrides,
        fillColor = fillOverrides,
        fillExtrusionColor = fillExtrusionOverrides,
        textColor = textColorOverrides,
        textHaloColor = textHaloOverrides,
    )
}

data class MapColorTheme(
    val backgroundBackgroundColor: JsonElement, // background -> paint -> background-color
    val parkFillOutlineColor: JsonElement, // park -> paint -> fill-outline-color
    val parkFillColor: JsonElement, // park -> paint -> fill-color
    val landuseResidentialFillColor: JsonElement, // landuse_residential -> paint -> fill-color
    val landcoverWoodFillColor: JsonElement, // landcover_wood -> paint -> fill-color
    val landcoverGrassFillColor: JsonElement, // landcover_grass -> paint -> fill-color
    val landuseSchoolFillColor: JsonElement, // landuse_school -> paint -> fill-color
    val waterwayRiverLineColor: JsonElement, // waterway_river -> paint -> line-color
    val waterwayOtherLineColor: JsonElement, // waterway_other -> paint -> line-color
    val waterFillColor: JsonElement, // water -> paint -> fill-color
    val tunnelPathPedestrianLineColor: JsonElement, // tunnel_path_pedestrian -> paint -> line-color
    val roadServiceTrackCasingLineColor: JsonElement, // road_service_track_casing -> paint -> line-color
    val roadMinorCasingLineWidth: JsonElement, // road_minor_casing -> paint -> line-width
    val roadMinorCasingLineColor: JsonElement, // road_minor_casing -> paint -> line-color
    val roadTrunkPrimaryCasingLineWidth: JsonElement, // road_trunk_primary_casing -> paint -> line-width
    val roadTrunkPrimaryCasingLineColor: JsonElement, // road_trunk_primary_casing -> paint -> line-color
    val roadMotorwayCasingLineWidth: JsonElement, // road_motorway_casing -> paint -> line-width
    val roadMotorwayCasingLineColor: JsonElement, // road_motorway_casing -> paint -> line-color
    val roadPathPedestrianLineColor: JsonElement, // road_path_pedestrian -> paint -> line-color
    val roadServiceTrackLineColor: JsonElement, // road_service_track -> paint -> line-color
    val roadSecondaryTertiaryLineColor: JsonElement, // road_secondary_tertiary -> paint -> line-color
    val roadTrunkPrimaryLineWidth: JsonElement, // road_trunk_primary -> paint -> line-width
    val roadTrunkPrimaryLineColor: JsonElement, // road_trunk_primary -> paint -> line-color
    val roadMotorwayLineWidth: JsonElement, // road_motorway -> paint -> line-width
    val roadMotorwayLineColor: JsonElement, // road_motorway -> paint -> line-color
    val bridgePathPedestrianCasingLineColor: JsonElement, // bridge_path_pedestrian_casing -> paint -> line-color
    val bridgePathPedestrianLineColor: JsonElement, // bridge_path_pedestrian -> paint -> line-color
    val buildingFillOpacity: JsonElement, // building -> paint -> fill-opacity
    val buildingFillColor: JsonElement, // building -> paint -> fill-color
    val building3dFillExtrusionColor: JsonElement, // building-3d -> paint -> fill-extrusion-color
    val building3dFillExtrusionOpacity: JsonElement, // building-3d -> paint -> fill-extrusion-opacity
    val waterNameLineTextHaloWidth: JsonElement, // water_name_line -> paint -> text-halo-width
    val waterNameLineTextColor: JsonElement, // water_name_line -> paint -> text-color
    val waterNameLineTextFont: JsonElement, // water_name_line -> layout -> text-font
    val waterNameLineTextSize: JsonElement, // water_name_line -> layout -> text-size
    val waterNamePointTextColor: JsonElement, // water_name_point -> paint -> text-color
    val waterNamePointTextHaloBlur: JsonElement, // water_name_point -> paint -> text-halo-blur
    val waterNamePointTextFont: JsonElement, // water_name_point -> layout -> text-font
    val waterNamePointTextSize: JsonElement, // water_name_point -> layout -> text-size
    val poiZ16TextColor: JsonElement, // poi_z16 -> paint -> text-color
    val poiZ16TextFont: JsonElement, // poi_z16 -> layout -> text-font
    val poiZ16TextSize: JsonElement, // poi_z16 -> layout -> text-size
    val poiZ16IconSize: JsonElement, // poi_z16 -> layout -> icon-size
    val poiZ15TextColor: JsonElement, // poi_z15 -> paint -> text-color
    val poiZ15TextFont: JsonElement, // poi_z15 -> layout -> text-font
    val poiZ15TextSize: JsonElement, // poi_z15 -> layout -> text-size
    val poiZ15IconSize: JsonElement, // poi_z15 -> layout -> icon-size
    val poiZ14TextColor: JsonElement, // poi_z14 -> paint -> text-color
    val poiZ14TextFont: JsonElement, // poi_z14 -> layout -> text-font
    val poiZ14TextSize: JsonElement, // poi_z14 -> layout -> text-size
    val poiZ14IconSize: JsonElement, // poi_z14 -> layout -> icon-size
    val poiTransitTextHaloWidth: JsonElement, // poi_transit -> paint -> text-halo-width
    val poiTransitIconHaloWidth: JsonElement, // poi_transit -> paint -> icon-halo-width
    val poiTransitIconHaloColor: JsonElement, // poi_transit -> paint -> icon-halo-color
    val poiTransitTextHaloBlur: JsonElement, // poi_transit -> paint -> text-halo-blur
    val poiTransitTextColor: JsonElement, // poi_transit -> paint -> text-color
    val poiTransitTextFont: JsonElement, // poi_transit -> layout -> text-font
    val poiTransitTextSize: JsonElement, // poi_transit -> layout -> text-size
    val roadLabelTextHaloColor: JsonElement, // road_label -> paint -> text-halo-color
    val roadLabelTextHaloWidth: JsonElement, // road_label -> paint -> text-halo-width
    val roadLabelTextColor: JsonElement, // road_label -> paint -> text-color
    val roadLabelTextHaloBlur: JsonElement, // road_label -> paint -> text-halo-blur
    val roadLabelTextOffset: JsonElement, // road_label -> layout -> text-offset
    val roadLabelTextFont: JsonElement, // road_label -> layout -> text-font
    val roadLabelTextLineHeight: JsonElement, // road_label -> layout -> text-line-height
    val roadLabelTextSize: JsonElement, // road_label -> layout -> text-size
    val roadShieldTextFont: JsonElement, // road_shield -> layout -> text-font
    val roadShieldTextSize: JsonElement, // road_shield -> layout -> text-size
    val roadShieldIconSize: JsonElement, // road_shield -> layout -> icon-size
    val placeOtherTextColor: JsonElement, // place_other -> paint -> text-color
    val placeOtherTextFont: JsonElement, // place_other -> layout -> text-font
    val placeVillageTextFont: JsonElement, // place_village -> layout -> text-font
    val placeTownTextFont: JsonElement, // place_town -> layout -> text-font
    val placeTownTextSize: JsonElement, // place_town -> layout -> text-size
    val placeCityTextFont: JsonElement // place_city -> layout -> text-font
)

val AccessibleTheme = MapColorTheme(
    backgroundBackgroundColor = Json.parseToJsonElement(""""rgba(255, 255, 255, 1)""""),
    parkFillOutlineColor = Json.parseToJsonElement(""""rgba(19, 170, 19, 1)""""),
    parkFillColor = Json.parseToJsonElement(""""rgba(102, 247, 77, 1)""""),
    landuseResidentialFillColor = Json.parseToJsonElement("""{"base": 1, "stops": [[9, "rgba(242, 238, 238, 0.84)"], [12, "rgba(255, 4, 175, 1)"]]}"""),
    landcoverWoodFillColor = Json.parseToJsonElement(""""rgba(96, 255, 96, 0.7)""""),
    landcoverGrassFillColor = Json.parseToJsonElement(""""rgba(43, 255, 4, 1)""""),
    landuseSchoolFillColor = Json.parseToJsonElement(""""rgba(247, 247, 194, 1)""""),
    waterwayRiverLineColor = Json.parseToJsonElement(""""rgba(5, 127, 248, 1)""""),
    waterwayOtherLineColor = Json.parseToJsonElement(""""rgba(6, 128, 250, 1)""""),
    waterFillColor = Json.parseToJsonElement(""""rgba(116, 148, 244, 1)""""),
    tunnelPathPedestrianLineColor = Json.parseToJsonElement(""""rgba(0, 0, 0, 1)""""),
    roadServiceTrackCasingLineColor = Json.parseToJsonElement(""""rgba(0, 0, 0, 1)""""),
    roadMinorCasingLineWidth = Json.parseToJsonElement("""{"stops": [[12, 0.5], [13, 1], [14, 4], [20, 20]], "base": 0.8}"""),
    roadMinorCasingLineColor = Json.parseToJsonElement(""""rgba(0, 0, 0, 1)""""),
    roadTrunkPrimaryCasingLineWidth = Json.parseToJsonElement("""{"stops": [[5, 0.4], [6, 0.7], [7, 1.2], [20, 20]], "base": 1}"""),
    roadTrunkPrimaryCasingLineColor = Json.parseToJsonElement(""""rgba(0, 0, 0, 1)""""),
    roadMotorwayCasingLineWidth = Json.parseToJsonElement("""{"base": 1.2, "stops": [[5, 0.4], [6, 0.7], [7, 1.75], [20, 48]]}"""),
    roadMotorwayCasingLineColor = Json.parseToJsonElement(""""rgba(11, 11, 11, 1)""""),
    roadPathPedestrianLineColor = Json.parseToJsonElement(""""rgba(11, 11, 11, 1)""""),
    roadServiceTrackLineColor = Json.parseToJsonElement(""""rgba(0, 0, 0, 1)""""),
    roadSecondaryTertiaryLineColor = Json.parseToJsonElement(""""rgba(255, 213, 36, 1)""""),
    roadTrunkPrimaryLineWidth = Json.parseToJsonElement("""{"base": 1, "stops": [[5, 0], [7, 1], [20, 18]]}"""),
    roadTrunkPrimaryLineColor = Json.parseToJsonElement(""""rgba(250, 209, 42, 1)""""),
    roadMotorwayLineWidth = Json.parseToJsonElement("""{"base": 1.2, "stops": [[5, 0], [7, 1], [20, 40]]}"""),
    roadMotorwayLineColor = Json.parseToJsonElement("""{"base": 1, "stops": [[5, "rgba(242, 126, 38, 1)"], [6, "rgba(250, 166, 46, 1)"]]}"""),
    bridgePathPedestrianCasingLineColor = Json.parseToJsonElement(""""rgba(255, 255, 255, 1)""""),
    bridgePathPedestrianLineColor = Json.parseToJsonElement(""""rgba(5, 5, 5, 1)""""),
    buildingFillOpacity = Json.parseToJsonElement("""0.3"""),
    buildingFillColor = Json.parseToJsonElement(""""rgba(253, 139, 139,1)""""),
    building3dFillExtrusionColor = Json.parseToJsonElement(""""rgba(253, 139, 139, 1)""""),
    building3dFillExtrusionOpacity = Json.parseToJsonElement("""0.3"""),
    waterNameLineTextHaloWidth = Json.parseToJsonElement("""0"""),
    waterNameLineTextColor = Json.parseToJsonElement(""""rgba(249, 249, 249, 1)""""),
    waterNameLineTextFont = Json.parseToJsonElement("""["Roboto Bold"]"""),
    waterNameLineTextSize = Json.parseToJsonElement("""{"stops": [[13, 10], [20, 40]], "base": 1}"""),
    waterNamePointTextColor = Json.parseToJsonElement(""""rgba(253, 253, 255, 1)""""),
    waterNamePointTextHaloBlur = Json.parseToJsonElement("""0"""),
    waterNamePointTextFont = Json.parseToJsonElement("""["Roboto Bold"]"""),
    waterNamePointTextSize = Json.parseToJsonElement("""{"stops": [[13, 10], [20, 40]], "base": 1}"""),
    poiZ16TextColor = Json.parseToJsonElement(""""rgba(0, 0, 0, 1)""""),
    poiZ16TextFont = Json.parseToJsonElement("""["Roboto Condensed Bold Italic"]"""),
    poiZ16TextSize = Json.parseToJsonElement("""18"""),
    poiZ16IconSize = Json.parseToJsonElement("""1"""),
    poiZ15TextColor = Json.parseToJsonElement(""""rgba(0, 0, 0, 1)""""),
    poiZ15TextFont = Json.parseToJsonElement("""["Roboto Condensed Bold Italic"]"""),
    poiZ15TextSize = Json.parseToJsonElement("""18"""),
    poiZ15IconSize = Json.parseToJsonElement("""1.4"""),
    poiZ14TextColor = Json.parseToJsonElement(""""rgba(0, 0, 0, 1)""""),
    poiZ14TextFont = Json.parseToJsonElement("""["Roboto Condensed Bold Italic"]"""),
    poiZ14TextSize = Json.parseToJsonElement("""{"stops": [[14, 14], [18, 30]]}"""),
    poiZ14IconSize = Json.parseToJsonElement("""1"""),
    poiTransitTextHaloWidth = Json.parseToJsonElement("""2"""),
    poiTransitIconHaloWidth = Json.parseToJsonElement("""2"""),
    poiTransitIconHaloColor = Json.parseToJsonElement(""""rgba(158, 49, 49, 1)""""),
    poiTransitTextHaloBlur = Json.parseToJsonElement("""1"""),
    poiTransitTextColor = Json.parseToJsonElement(""""rgba(37, 129, 242, 1)""""),
    poiTransitTextFont = Json.parseToJsonElement("""["Roboto Condensed Bold Italic"]"""),
    poiTransitTextSize = Json.parseToJsonElement("""16"""),
    roadLabelTextHaloColor = Json.parseToJsonElement(""""rgba(255, 255, 255, 1)""""),
    roadLabelTextHaloWidth = Json.parseToJsonElement("""4"""),
    roadLabelTextColor = Json.parseToJsonElement(""""rgba(0, 0, 0, 1)""""),
    roadLabelTextHaloBlur = Json.parseToJsonElement("""2"""),
    roadLabelTextOffset = Json.parseToJsonElement("""[0, 0]"""),
    roadLabelTextFont = Json.parseToJsonElement("""["Roboto Bold"]"""),
    roadLabelTextLineHeight = Json.parseToJsonElement("""1"""),
    roadLabelTextSize = Json.parseToJsonElement("""{"stops": [[13, 10], [20, 40]], "base": 1}"""),
    roadShieldTextFont = Json.parseToJsonElement("""["Roboto Bold"]"""),
    roadShieldTextSize = Json.parseToJsonElement("""12"""),
    roadShieldIconSize = Json.parseToJsonElement("""1.0"""),
    placeOtherTextColor = Json.parseToJsonElement(""""rgba(0, 0, 0, 1)""""),
    placeOtherTextFont = Json.parseToJsonElement("""["Roboto Condensed Bold Italic"]"""),
    placeVillageTextFont = Json.parseToJsonElement("""["Roboto Bold"]"""),
    placeTownTextFont = Json.parseToJsonElement("""["Roboto Bold"]"""),
    placeTownTextSize = Json.parseToJsonElement("""{"base": 1.2, "stops": [[7, 12], [11, 25]]}"""),
    placeCityTextFont = Json.parseToJsonElement("""["Roboto Bold"]""")
)

val OriginalTheme = MapColorTheme(
    backgroundBackgroundColor = Json.parseToJsonElement(""""rgb(239,239,239)""""),
    parkFillOutlineColor = Json.parseToJsonElement(""""rgba(95, 208, 100, 1)""""),
    parkFillColor = Json.parseToJsonElement(""""#d8e8c8""""),
    landuseResidentialFillColor = Json.parseToJsonElement("""{"base": 1, "stops": [[9, "hsla(0, 3%, 85%, 0.84)"], [12, "hsla(35, 57%, 88%, 0.49)"]]}"""),
    landcoverWoodFillColor = Json.parseToJsonElement(""""hsla(98, 61%, 72%, 0.7)""""),
    landcoverGrassFillColor = Json.parseToJsonElement(""""rgba(176, 213, 154, 1)""""),
    landuseSchoolFillColor = Json.parseToJsonElement(""""rgb(236,238,204)""""),
    waterwayRiverLineColor = Json.parseToJsonElement(""""#a0c8f0""""),
    waterwayOtherLineColor = Json.parseToJsonElement(""""#a0c8f0""""),
    waterFillColor = Json.parseToJsonElement(""""rgb(158,189,255)""""),
    tunnelPathPedestrianLineColor = Json.parseToJsonElement(""""hsl(0, 0%, 100%)""""),
    roadServiceTrackCasingLineColor = Json.parseToJsonElement(""""#cfcdca""""),
    roadMinorCasingLineWidth = Json.parseToJsonElement("""{"base": 1.2, "stops": [[12, 0.5], [13, 1], [14, 4], [20, 20]]}"""),
    roadMinorCasingLineColor = Json.parseToJsonElement(""""#cfcdca""""),
    roadTrunkPrimaryCasingLineWidth = Json.parseToJsonElement("""{"base": 1.2, "stops": [[5, 0.4], [6, 0.7], [7, 1.75], [20, 22]]}"""),
    roadTrunkPrimaryCasingLineColor = Json.parseToJsonElement(""""#e9ac77""""),
    roadMotorwayCasingLineWidth = Json.parseToJsonElement("""{"base": 1.2, "stops": [[5, 0.4], [6, 0.7], [7, 1.75], [20, 22]]}"""),
    roadMotorwayCasingLineColor = Json.parseToJsonElement(""""#e9ac77""""),
    roadPathPedestrianLineColor = Json.parseToJsonElement(""""hsl(0, 0%, 100%)""""),
    roadServiceTrackLineColor = Json.parseToJsonElement(""""#fff""""),
    roadSecondaryTertiaryLineColor = Json.parseToJsonElement(""""#fea""""),
    roadTrunkPrimaryLineWidth = Json.parseToJsonElement("""{"base": 1.2, "stops": [[5, 0], [7, 1], [20, 18]]}"""),
    roadTrunkPrimaryLineColor = Json.parseToJsonElement(""""#fea""""),
    roadMotorwayLineWidth = Json.parseToJsonElement("""{"base": 1.2, "stops": [[5, 0], [7, 1], [20, 18]]}"""),
    roadMotorwayLineColor = Json.parseToJsonElement("""{"base": 1, "stops": [[5, "hsl(26, 87%, 62%)"], [6, "#fc8"]]}"""),
    bridgePathPedestrianCasingLineColor = Json.parseToJsonElement(""""hsl(35, 6%, 80%)""""),
    bridgePathPedestrianLineColor = Json.parseToJsonElement(""""hsl(0, 0%, 100%)""""),
    buildingFillOpacity = Json.parseToJsonElement("""null"""),
    buildingFillColor = Json.parseToJsonElement(""""hsl(35, 8%, 85%)""""),
    building3dFillExtrusionColor = Json.parseToJsonElement(""""hsl(35, 8%, 85%)""""),
    building3dFillExtrusionOpacity = Json.parseToJsonElement("""0.8"""),
    waterNameLineTextHaloWidth = Json.parseToJsonElement("""1"""),
    waterNameLineTextColor = Json.parseToJsonElement(""""#5d60be""""),
    waterNameLineTextFont = Json.parseToJsonElement("""["Roboto Regular"]"""),
    waterNameLineTextSize = Json.parseToJsonElement("""12"""),
    waterNamePointTextColor = Json.parseToJsonElement(""""#5d60be""""),
    waterNamePointTextHaloBlur = Json.parseToJsonElement("""null"""),
    waterNamePointTextFont = Json.parseToJsonElement("""["Roboto Regular"]"""),
    waterNamePointTextSize = Json.parseToJsonElement("""12"""),
    poiZ16TextColor = Json.parseToJsonElement(""""#666""""),
    poiZ16TextFont = Json.parseToJsonElement("""["Roboto Condensed Italic"]"""),
    poiZ16TextSize = Json.parseToJsonElement("""12"""),
    poiZ16IconSize = Json.parseToJsonElement("""null"""),
    poiZ15TextColor = Json.parseToJsonElement(""""#666""""),
    poiZ15TextFont = Json.parseToJsonElement("""["Roboto Condensed Italic"]"""),
    poiZ15TextSize = Json.parseToJsonElement("""12"""),
    poiZ15IconSize = Json.parseToJsonElement("""null"""),
    poiZ14TextColor = Json.parseToJsonElement(""""#666""""),
    poiZ14TextFont = Json.parseToJsonElement("""["Roboto Condensed Italic"]"""),
    poiZ14TextSize = Json.parseToJsonElement("""12"""),
    poiZ14IconSize = Json.parseToJsonElement("""null"""),
    poiTransitTextHaloWidth = Json.parseToJsonElement("""1"""),
    poiTransitIconHaloWidth = Json.parseToJsonElement("""null"""),
    poiTransitIconHaloColor = Json.parseToJsonElement("""null"""),
    poiTransitTextHaloBlur = Json.parseToJsonElement("""0.5"""),
    poiTransitTextColor = Json.parseToJsonElement(""""#4898ff""""),
    poiTransitTextFont = Json.parseToJsonElement("""["Roboto Condensed Italic"]"""),
    poiTransitTextSize = Json.parseToJsonElement("""12"""),
    roadLabelTextHaloColor = Json.parseToJsonElement("""null"""),
    roadLabelTextHaloWidth = Json.parseToJsonElement("""1"""),
    roadLabelTextColor = Json.parseToJsonElement(""""#765""""),
    roadLabelTextHaloBlur = Json.parseToJsonElement("""0.5"""),
    roadLabelTextOffset = Json.parseToJsonElement("""[0, 0.15]"""),
    roadLabelTextFont = Json.parseToJsonElement("""["Roboto Regular"]"""),
    roadLabelTextLineHeight = Json.parseToJsonElement("""null"""),
    roadLabelTextSize = Json.parseToJsonElement("""{"base": 1, "stops": [[13, 12], [14, 13]]}"""),
    roadShieldTextFont = Json.parseToJsonElement("""["Roboto Regular"]"""),
    roadShieldTextSize = Json.parseToJsonElement("""10"""),
    roadShieldIconSize = Json.parseToJsonElement("""0.8"""),
    placeOtherTextColor = Json.parseToJsonElement(""""#633""""),
    placeOtherTextFont = Json.parseToJsonElement("""["Roboto Condensed Italic"]"""),
    placeVillageTextFont = Json.parseToJsonElement("""["Roboto Regular"]"""),
    placeTownTextFont = Json.parseToJsonElement("""["Roboto Regular"]"""),
    placeTownTextSize = Json.parseToJsonElement("""{"base": 1.2, "stops": [[7, 12], [11, 16]]}"""),
    placeCityTextFont = Json.parseToJsonElement("""["Roboto Medium"]""")
)
