package org.scottishtecharmy.soundscape.mapstyle

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonArray
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.maplibre.compose.style.BaseStyle

/**
 * Put a JsonElement value, skipping if it's JsonNull (so MapLibre uses its default).
 */
private fun JsonObjectBuilder.putNonNull(key: String, value: JsonElement) {
    if (value !is JsonNull) {
        put(key, value)
    }
}

private fun applyOverrides(
    style: JsonObject,
    overrides: LayerPaintOverrides,
    symbolForegroundColor: String?,
    symbolBackgroundColor: String?,
): JsonObject {
    if (overrides.backgroundColor == null && overrides.lineColor.isEmpty() &&
        overrides.fillColor.isEmpty() && overrides.fillExtrusionColor.isEmpty() &&
        symbolForegroundColor == null && symbolBackgroundColor == null) {
        return style
    }

    val layers = style["layers"]?.jsonArray ?: return style
    val newLayers = JsonArray(layers.map { layerElement ->
        val layer = layerElement.jsonObject
        val id = (layer["id"] as? JsonPrimitive)?.content ?: return@map layerElement
        val type = (layer["type"] as? JsonPrimitive)?.content

        val paintOverrides = mutableMapOf<String, JsonElement>()

        // Background layer override
        if (type == "background" && overrides.backgroundColor != null) {
            paintOverrides["background-color"] = JsonPrimitive(overrides.backgroundColor)
        }
        // Line color overrides
        if (type == "line" && id in overrides.lineColor) {
            paintOverrides["line-color"] = JsonPrimitive(overrides.lineColor[id]!!)
        }
        // Fill color overrides
        if (type == "fill" && id in overrides.fillColor) {
            paintOverrides["fill-color"] = JsonPrimitive(overrides.fillColor[id]!!)
        }
        // Fill-extrusion color overrides
        if (type == "fill-extrusion" && id in overrides.fillExtrusionColor) {
            paintOverrides["fill-extrusion-color"] = JsonPrimitive(overrides.fillExtrusionColor[id]!!)
        }
        // Symbol layers: apply foreground/background text colors
        if (type == "symbol") {
            if (symbolForegroundColor != null) {
                paintOverrides["text-color"] = JsonPrimitive(symbolForegroundColor)
            }
            if (symbolBackgroundColor != null) {
                paintOverrides["text-halo-color"] = JsonPrimitive(symbolBackgroundColor)
            }
        }
        // Per-layer text color overrides
        if (id in overrides.textColor) {
            paintOverrides["text-color"] = JsonPrimitive(overrides.textColor[id]!!)
        }
        if (id in overrides.textHaloColor) {
            paintOverrides["text-halo-color"] = JsonPrimitive(overrides.textHaloColor[id]!!)
        }

        if (paintOverrides.isEmpty()) {
            layerElement
        } else {
            val existingPaint = layer["paint"]?.jsonObject ?: JsonObject(emptyMap())
            val mergedPaint = JsonObject(existingPaint + paintOverrides)
            JsonObject(layer + ("paint" to mergedPaint))
        }
    })

    return JsonObject(style + ("layers" to newLayers))
}

fun buildMapStyle(
    theme: MapColorTheme,
    spritePath: String,
    glyphsPath: String,
    tileSourceUrl: String,
    overrides: LayerPaintOverrides = LayerPaintOverrides(),
    symbolForegroundColor: String? = null,
    symbolBackgroundColor: String? = null,
): BaseStyle.Json {
    val baseStyle = buildJsonObject {
    put("version", 8)
    put("name", "OSM Liberty")
    put("id", "osm-liberty")
    putJsonObject("metadata") {
        put("maputnik:license", "https://github.com/maputnik/osm-liberty/blob/gh-pages/LICENSE.md")
        put("maputnik:renderer", "mbgljs")
        put("openmaptiles:version", "3.x")
    }
    put("sprite", spritePath)
    put("glyphs", glyphsPath)
    putJsonObject("sources") {
        putJsonObject("openmaptiles") {
            put("type", "vector")
            put("url", tileSourceUrl)
        }
    }
    putJsonArray("layers") {
    // Layer: background
    addJsonObject {
        put("id", "background")
        put("type", "background")
        putJsonObject("paint") {
            putNonNull("background-color", theme.backgroundBackgroundColor)
        }
    }
    // Layer: park
    addJsonObject {
        put("id", "park")
        put("type", "fill")
        put("source", "openmaptiles")
        put("source-layer", "park")
        putJsonObject("paint") {
            putNonNull("fill-color", theme.parkFillColor)
            put("fill-opacity", 0.7)
            putNonNull("fill-outline-color", theme.parkFillOutlineColor)
        }
    }
    // Layer: park_outline
    addJsonObject {
        put("id", "park_outline")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "park")
        putJsonObject("paint") {
            putJsonArray("line-dasharray") {
                add(1)
                add(1.5)
            }
            put("line-color", "rgba(228, 241, 215, 1)")
        }
    }
    // Layer: landuse_residential
    addJsonObject {
        put("id", "landuse_residential")
        put("type", "fill")
        put("source", "openmaptiles")
        put("source-layer", "landuse")
        put("maxzoom", 8)
        putJsonArray("filter") {
            add("==")
            add("class")
            add("residential")
        }
        putJsonObject("paint") {
            putNonNull("fill-color", theme.landuseResidentialFillColor)
        }
    }
    // Layer: landcover_wood
    addJsonObject {
        put("id", "landcover_wood")
        put("type", "fill")
        put("source", "openmaptiles")
        put("source-layer", "landcover")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("class")
                add("wood")
            }
        }
        putJsonObject("paint") {
            put("fill-antialias", false)
            putNonNull("fill-color", theme.landcoverWoodFillColor)
            put("fill-opacity", 0.4)
        }
    }
    // Layer: landcover_grass
    addJsonObject {
        put("id", "landcover_grass")
        put("type", "fill")
        put("source", "openmaptiles")
        put("source-layer", "landcover")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("class")
                add("grass")
            }
        }
        putJsonObject("paint") {
            put("fill-antialias", false)
            putNonNull("fill-color", theme.landcoverGrassFillColor)
            put("fill-opacity", 0.3)
        }
    }
    // Layer: landcover_ice
    addJsonObject {
        put("id", "landcover_ice")
        put("type", "fill")
        put("source", "openmaptiles")
        put("source-layer", "landcover")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("class")
                add("ice")
            }
        }
        putJsonObject("paint") {
            put("fill-antialias", false)
            put("fill-color", "rgba(224, 236, 236, 1)")
            put("fill-opacity", 0.8)
        }
    }
    // Layer: landcover_wetland
    addJsonObject {
        put("id", "landcover_wetland")
        put("type", "fill")
        put("source", "openmaptiles")
        put("source-layer", "landcover")
        put("minzoom", 12)
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("class")
                add("wetland")
            }
        }
        putJsonObject("paint") {
            put("fill-antialias", true)
            put("fill-opacity", 0.8)
            put("fill-pattern", "wetland_bg_11")
            put("fill-translate-anchor", "map")
        }
    }
    // Layer: landuse_pitch
    addJsonObject {
        put("id", "landuse_pitch")
        put("type", "fill")
        put("source", "openmaptiles")
        put("source-layer", "landuse")
        putJsonArray("filter") {
            add("==")
            add("class")
            add("pitch")
        }
        putJsonObject("paint") {
            put("fill-color", "#DEE3CD")
        }
    }
    // Layer: landuse_track
    addJsonObject {
        put("id", "landuse_track")
        put("type", "fill")
        put("source", "openmaptiles")
        put("source-layer", "landuse")
        putJsonArray("filter") {
            add("==")
            add("class")
            add("track")
        }
        putJsonObject("paint") {
            put("fill-color", "#DEE3CD")
        }
    }
    // Layer: landuse_cemetery
    addJsonObject {
        put("id", "landuse_cemetery")
        put("type", "fill")
        put("source", "openmaptiles")
        put("source-layer", "landuse")
        putJsonArray("filter") {
            add("==")
            add("class")
            add("cemetery")
        }
        putJsonObject("paint") {
            put("fill-color", "hsl(75, 37%, 81%)")
        }
    }
    // Layer: landuse_hospital
    addJsonObject {
        put("id", "landuse_hospital")
        put("type", "fill")
        put("source", "openmaptiles")
        put("source-layer", "landuse")
        putJsonArray("filter") {
            add("==")
            add("class")
            add("hospital")
        }
        putJsonObject("paint") {
            put("fill-color", "#fde")
        }
    }
    // Layer: landuse_school
    addJsonObject {
        put("id", "landuse_school")
        put("type", "fill")
        put("source", "openmaptiles")
        put("source-layer", "landuse")
        putJsonArray("filter") {
            add("==")
            add("class")
            add("school")
        }
        putJsonObject("paint") {
            putNonNull("fill-color", theme.landuseSchoolFillColor)
        }
    }
    // Layer: waterway_tunnel
    addJsonObject {
        put("id", "waterway_tunnel")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "waterway")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("brunnel")
                add("tunnel")
            }
        }
        putJsonObject("paint") {
            put("line-color", "#a0c8f0")
            putJsonArray("line-dasharray") {
                add(3)
                add(3)
            }
            putJsonObject("line-gap-width") {
                putJsonArray("stops") {
                    addJsonArray {
                        add(12)
                        add(0)
                    }
                    addJsonArray {
                        add(20)
                        add(6)
                    }
                }
            }
            put("line-opacity", 1)
            putJsonObject("line-width") {
                put("base", 1.4)
                putJsonArray("stops") {
                    addJsonArray {
                        add(8)
                        add(1)
                    }
                    addJsonArray {
                        add(20)
                        add(2)
                    }
                }
            }
        }
    }
    // Layer: waterway_river
    addJsonObject {
        put("id", "waterway_river")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "waterway")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("class")
                add("river")
            }
            addJsonArray {
                add("!=")
                add("brunnel")
                add("tunnel")
            }
        }
        putJsonObject("paint") {
            putNonNull("line-color", theme.waterwayRiverLineColor)
            putJsonObject("line-width") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(11)
                        add(0.5)
                    }
                    addJsonArray {
                        add(20)
                        add(6)
                    }
                }
            }
        }
        putJsonObject("layout") {
            put("line-cap", "round")
        }
    }
    // Layer: waterway_other
    addJsonObject {
        put("id", "waterway_other")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "waterway")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("!=")
                add("class")
                add("river")
            }
            addJsonArray {
                add("!=")
                add("brunnel")
                add("tunnel")
            }
        }
        putJsonObject("paint") {
            putNonNull("line-color", theme.waterwayOtherLineColor)
            putJsonObject("line-width") {
                put("base", 1.3)
                putJsonArray("stops") {
                    addJsonArray {
                        add(13)
                        add(0.5)
                    }
                    addJsonArray {
                        add(20)
                        add(6)
                    }
                }
            }
        }
        putJsonObject("layout") {
            put("line-cap", "round")
        }
    }
    // Layer: water
    addJsonObject {
        put("id", "water")
        put("type", "fill")
        put("source", "openmaptiles")
        put("source-layer", "water")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("!=")
                add("brunnel")
                add("tunnel")
            }
        }
        putJsonObject("paint") {
            putNonNull("fill-color", theme.waterFillColor)
        }
    }
    // Layer: landcover_sand
    addJsonObject {
        put("id", "landcover_sand")
        put("type", "fill")
        put("source", "openmaptiles")
        put("source-layer", "landcover")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("class")
                add("sand")
            }
        }
        putJsonObject("paint") {
            put("fill-color", "rgba(247, 239, 195, 1)")
        }
    }
    // Layer: aeroway_fill
    addJsonObject {
        put("id", "aeroway_fill")
        put("type", "fill")
        put("source", "openmaptiles")
        put("source-layer", "aeroway")
        put("minzoom", 11)
        putJsonArray("filter") {
            add("==")
            add("\$type")
            add("Polygon")
        }
        putJsonObject("paint") {
            put("fill-color", "rgba(229, 228, 224, 1)")
            put("fill-opacity", 0.7)
        }
    }
    // Layer: aeroway_runway
    addJsonObject {
        put("id", "aeroway_runway")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "aeroway")
        put("minzoom", 11)
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("\$type")
                add("LineString")
            }
            addJsonArray {
                add("==")
                add("class")
                add("runway")
            }
        }
        putJsonObject("paint") {
            put("line-color", "#f0ede9")
            putJsonObject("line-width") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(11)
                        add(3)
                    }
                    addJsonArray {
                        add(20)
                        add(16)
                    }
                }
            }
        }
    }
    // Layer: aeroway_taxiway
    addJsonObject {
        put("id", "aeroway_taxiway")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "aeroway")
        put("minzoom", 11)
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("\$type")
                add("LineString")
            }
            addJsonArray {
                add("==")
                add("class")
                add("taxiway")
            }
        }
        putJsonObject("paint") {
            put("line-color", "#f0ede9")
            putJsonObject("line-width") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(11)
                        add(0.5)
                    }
                    addJsonArray {
                        add(20)
                        add(6)
                    }
                }
            }
        }
    }
    // Layer: tunnel_motorway_link_casing
    addJsonObject {
        put("id", "tunnel_motorway_link_casing")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("class")
                add("motorway")
            }
            addJsonArray {
                add("==")
                add("ramp")
                add(1)
            }
            addJsonArray {
                add("==")
                add("brunnel")
                add("tunnel")
            }
        }
        putJsonObject("paint") {
            put("line-color", "#e9ac77")
            putJsonArray("line-dasharray") {
                add(0.5)
                add(0.25)
            }
            putJsonObject("line-width") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(12)
                        add(1)
                    }
                    addJsonArray {
                        add(13)
                        add(3)
                    }
                    addJsonArray {
                        add(14)
                        add(4)
                    }
                    addJsonArray {
                        add(20)
                        add(15)
                    }
                }
            }
        }
        putJsonObject("layout") {
            put("line-join", "round")
        }
    }
    // Layer: tunnel_service_track_casing
    addJsonObject {
        put("id", "tunnel_service_track_casing")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("brunnel")
                add("tunnel")
            }
            addJsonArray {
                add("in")
                add("class")
                add("service")
                add("track")
            }
        }
        putJsonObject("paint") {
            put("line-color", "#cfcdca")
            putJsonArray("line-dasharray") {
                add(0.5)
                add(0.25)
            }
            putJsonObject("line-width") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(15)
                        add(1)
                    }
                    addJsonArray {
                        add(16)
                        add(4)
                    }
                    addJsonArray {
                        add(20)
                        add(11)
                    }
                }
            }
        }
        putJsonObject("layout") {
            put("line-join", "round")
        }
    }
    // Layer: tunnel_link_casing
    addJsonObject {
        put("id", "tunnel_link_casing")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("ramp")
                add(1)
            }
            addJsonArray {
                add("==")
                add("brunnel")
                add("tunnel")
            }
        }
        putJsonObject("paint") {
            put("line-color", "#e9ac77")
            putJsonObject("line-width") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(12)
                        add(1)
                    }
                    addJsonArray {
                        add(13)
                        add(3)
                    }
                    addJsonArray {
                        add(14)
                        add(4)
                    }
                    addJsonArray {
                        add(20)
                        add(15)
                    }
                }
            }
        }
        putJsonObject("layout") {
            put("line-join", "round")
        }
    }
    // Layer: tunnel_street_casing
    addJsonObject {
        put("id", "tunnel_street_casing")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("brunnel")
                add("tunnel")
            }
            addJsonArray {
                add("in")
                add("class")
                add("street")
                add("street_limited")
            }
        }
        putJsonObject("paint") {
            put("line-color", "#cfcdca")
            putJsonObject("line-opacity") {
                putJsonArray("stops") {
                    addJsonArray {
                        add(12)
                        add(0)
                    }
                    addJsonArray {
                        add(12.5)
                        add(1)
                    }
                }
            }
            putJsonObject("line-width") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(12)
                        add(0.5)
                    }
                    addJsonArray {
                        add(13)
                        add(1)
                    }
                    addJsonArray {
                        add(14)
                        add(4)
                    }
                    addJsonArray {
                        add(20)
                        add(15)
                    }
                }
            }
        }
        putJsonObject("layout") {
            put("line-join", "round")
        }
    }
    // Layer: tunnel_secondary_tertiary_casing
    addJsonObject {
        put("id", "tunnel_secondary_tertiary_casing")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("brunnel")
                add("tunnel")
            }
            addJsonArray {
                add("in")
                add("class")
                add("secondary")
                add("tertiary")
                add("busway")
                add("bus_guideway")
            }
        }
        putJsonObject("paint") {
            put("line-color", "#e9ac77")
            putJsonObject("line-width") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(8)
                        add(1.5)
                    }
                    addJsonArray {
                        add(20)
                        add(17)
                    }
                }
            }
        }
        putJsonObject("layout") {
            put("line-join", "round")
        }
    }
    // Layer: tunnel_trunk_primary_casing
    addJsonObject {
        put("id", "tunnel_trunk_primary_casing")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("brunnel")
                add("tunnel")
            }
            addJsonArray {
                add("in")
                add("class")
                add("primary")
                add("trunk")
            }
        }
        putJsonObject("paint") {
            put("line-color", "#e9ac77")
            putJsonObject("line-width") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(5)
                        add(0.4)
                    }
                    addJsonArray {
                        add(6)
                        add(0.7)
                    }
                    addJsonArray {
                        add(7)
                        add(1.75)
                    }
                    addJsonArray {
                        add(20)
                        add(22)
                    }
                }
            }
        }
        putJsonObject("layout") {
            put("line-join", "round")
        }
    }
    // Layer: tunnel_motorway_casing
    addJsonObject {
        put("id", "tunnel_motorway_casing")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("class")
                add("motorway")
            }
            addJsonArray {
                add("!=")
                add("ramp")
                add(1)
            }
            addJsonArray {
                add("==")
                add("brunnel")
                add("tunnel")
            }
        }
        putJsonObject("paint") {
            put("line-color", "#e9ac77")
            putJsonArray("line-dasharray") {
                add(0.5)
                add(0.25)
            }
            putJsonObject("line-width") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(5)
                        add(0.4)
                    }
                    addJsonArray {
                        add(6)
                        add(0.7)
                    }
                    addJsonArray {
                        add(7)
                        add(1.75)
                    }
                    addJsonArray {
                        add(20)
                        add(22)
                    }
                }
            }
        }
        putJsonObject("layout") {
            put("line-join", "round")
        }
    }
    // Layer: tunnel_path_pedestrian
    addJsonObject {
        put("id", "tunnel_path_pedestrian")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("\$type")
                add("LineString")
            }
            addJsonArray {
                add("==")
                add("brunnel")
                add("tunnel")
            }
            addJsonArray {
                add("in")
                add("class")
                add("path")
                add("pedestrian")
            }
        }
        putJsonObject("paint") {
            putNonNull("line-color", theme.tunnelPathPedestrianLineColor)
            putJsonArray("line-dasharray") {
                add(1)
                add(0.75)
            }
            putJsonObject("line-width") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(14)
                        add(0.5)
                    }
                    addJsonArray {
                        add(20)
                        add(10)
                    }
                }
            }
        }
    }
    // Layer: tunnel_motorway_link
    addJsonObject {
        put("id", "tunnel_motorway_link")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("class")
                add("motorway")
            }
            addJsonArray {
                add("==")
                add("ramp")
                add(1)
            }
            addJsonArray {
                add("==")
                add("brunnel")
                add("tunnel")
            }
        }
        putJsonObject("paint") {
            put("line-color", "#fc8")
            putJsonObject("line-width") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(12.5)
                        add(0)
                    }
                    addJsonArray {
                        add(13)
                        add(1.5)
                    }
                    addJsonArray {
                        add(14)
                        add(2.5)
                    }
                    addJsonArray {
                        add(20)
                        add(11.5)
                    }
                }
            }
        }
        putJsonObject("layout") {
            put("line-join", "round")
        }
    }
    // Layer: tunnel_service_track
    addJsonObject {
        put("id", "tunnel_service_track")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("brunnel")
                add("tunnel")
            }
            addJsonArray {
                add("in")
                add("class")
                add("service")
                add("track")
            }
        }
        putJsonObject("paint") {
            put("line-color", "#fff")
            putJsonObject("line-width") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(15.5)
                        add(0)
                    }
                    addJsonArray {
                        add(16)
                        add(2)
                    }
                    addJsonArray {
                        add(20)
                        add(7.5)
                    }
                }
            }
        }
        putJsonObject("layout") {
            put("line-join", "round")
        }
    }
    // Layer: tunnel_link
    addJsonObject {
        put("id", "tunnel_link")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("ramp")
                add(1)
            }
            addJsonArray {
                add("==")
                add("brunnel")
                add("tunnel")
            }
        }
        putJsonObject("paint") {
            put("line-color", "#fff4c6")
            putJsonObject("line-width") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(12.5)
                        add(0)
                    }
                    addJsonArray {
                        add(13)
                        add(1.5)
                    }
                    addJsonArray {
                        add(14)
                        add(2.5)
                    }
                    addJsonArray {
                        add(20)
                        add(11.5)
                    }
                }
            }
        }
        putJsonObject("layout") {
            put("line-join", "round")
        }
    }
    // Layer: tunnel_minor
    addJsonObject {
        put("id", "tunnel_minor")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("brunnel")
                add("tunnel")
            }
            addJsonArray {
                add("in")
                add("class")
                add("minor")
            }
        }
        putJsonObject("paint") {
            put("line-color", "#fff")
            putJsonObject("line-width") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(13.5)
                        add(0)
                    }
                    addJsonArray {
                        add(14)
                        add(2.5)
                    }
                    addJsonArray {
                        add(20)
                        add(11.5)
                    }
                }
            }
        }
        putJsonObject("layout") {
            put("line-join", "round")
        }
    }
    // Layer: tunnel_secondary_tertiary
    addJsonObject {
        put("id", "tunnel_secondary_tertiary")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("brunnel")
                add("tunnel")
            }
            addJsonArray {
                add("in")
                add("class")
                add("secondary")
                add("tertiary")
                add("busway")
                add("bus_guideway")
            }
        }
        putJsonObject("paint") {
            put("line-color", "#fff4c6")
            putJsonObject("line-width") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(6.5)
                        add(0)
                    }
                    addJsonArray {
                        add(7)
                        add(0.5)
                    }
                    addJsonArray {
                        add(20)
                        add(10)
                    }
                }
            }
        }
        putJsonObject("layout") {
            put("line-join", "round")
        }
    }
    // Layer: tunnel_trunk_primary
    addJsonObject {
        put("id", "tunnel_trunk_primary")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("brunnel")
                add("tunnel")
            }
            addJsonArray {
                add("in")
                add("class")
                add("primary")
                add("trunk")
            }
        }
        putJsonObject("paint") {
            put("line-color", "#fff4c6")
            putJsonObject("line-width") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(5)
                        add(0)
                    }
                    addJsonArray {
                        add(7)
                        add(1)
                    }
                    addJsonArray {
                        add(20)
                        add(18)
                    }
                }
            }
        }
        putJsonObject("layout") {
            put("line-join", "round")
        }
    }
    // Layer: tunnel_motorway
    addJsonObject {
        put("id", "tunnel_motorway")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("class")
                add("motorway")
            }
            addJsonArray {
                add("!=")
                add("ramp")
                add(1)
            }
            addJsonArray {
                add("==")
                add("brunnel")
                add("tunnel")
            }
        }
        putJsonObject("paint") {
            put("line-color", "#ffdaa6")
            putJsonObject("line-width") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(5)
                        add(0)
                    }
                    addJsonArray {
                        add(7)
                        add(1)
                    }
                    addJsonArray {
                        add(20)
                        add(18)
                    }
                }
            }
        }
        putJsonObject("layout") {
            put("line-join", "round")
        }
    }
    // Layer: tunnel_major_rail
    addJsonObject {
        put("id", "tunnel_major_rail")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("brunnel")
                add("tunnel")
            }
            addJsonArray {
                add("in")
                add("class")
                add("rail")
            }
        }
        putJsonObject("paint") {
            put("line-color", "#bbb")
            putJsonObject("line-width") {
                put("base", 1.4)
                putJsonArray("stops") {
                    addJsonArray {
                        add(14)
                        add(0.4)
                    }
                    addJsonArray {
                        add(15)
                        add(0.75)
                    }
                    addJsonArray {
                        add(20)
                        add(2)
                    }
                }
            }
        }
    }
    // Layer: tunnel_major_rail_hatching
    addJsonObject {
        put("id", "tunnel_major_rail_hatching")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("brunnel")
                add("tunnel")
            }
            addJsonArray {
                add("==")
                add("class")
                add("rail")
            }
        }
        putJsonObject("paint") {
            put("line-color", "#bbb")
            putJsonArray("line-dasharray") {
                add(0.2)
                add(8)
            }
            putJsonObject("line-width") {
                put("base", 1.4)
                putJsonArray("stops") {
                    addJsonArray {
                        add(14.5)
                        add(0)
                    }
                    addJsonArray {
                        add(15)
                        add(3)
                    }
                    addJsonArray {
                        add(20)
                        add(8)
                    }
                }
            }
        }
    }
    // Layer: tunnel_transit_rail
    addJsonObject {
        put("id", "tunnel_transit_rail")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("brunnel")
                add("tunnel")
            }
            addJsonArray {
                add("in")
                add("class")
                add("transit")
            }
        }
        putJsonObject("paint") {
            put("line-color", "#bbb")
            putJsonObject("line-width") {
                put("base", 1.4)
                putJsonArray("stops") {
                    addJsonArray {
                        add(14)
                        add(0.4)
                    }
                    addJsonArray {
                        add(15)
                        add(0.75)
                    }
                    addJsonArray {
                        add(20)
                        add(2)
                    }
                }
            }
        }
    }
    // Layer: tunnel_transit_rail_hatching
    addJsonObject {
        put("id", "tunnel_transit_rail_hatching")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("brunnel")
                add("tunnel")
            }
            addJsonArray {
                add("==")
                add("class")
                add("transit")
            }
        }
        putJsonObject("paint") {
            put("line-color", "#bbb")
            putJsonArray("line-dasharray") {
                add(0.2)
                add(8)
            }
            putJsonObject("line-width") {
                put("base", 1.4)
                putJsonArray("stops") {
                    addJsonArray {
                        add(14.5)
                        add(0)
                    }
                    addJsonArray {
                        add(15)
                        add(3)
                    }
                    addJsonArray {
                        add(20)
                        add(8)
                    }
                }
            }
        }
    }
    // Layer: road_area_pattern
    addJsonObject {
        put("id", "road_area_pattern")
        put("type", "fill")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("\$type")
                add("Polygon")
            }
        }
        putJsonObject("paint") {
            put("fill-pattern", "pedestrian_polygon")
        }
    }
    // Layer: road_motorway_link_casing
    addJsonObject {
        put("id", "road_motorway_link_casing")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        put("minzoom", 12)
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("!in")
                add("brunnel")
                add("bridge")
                add("tunnel")
            }
            addJsonArray {
                add("==")
                add("class")
                add("motorway")
            }
            addJsonArray {
                add("==")
                add("ramp")
                add(1)
            }
        }
        putJsonObject("paint") {
            put("line-color", "#e9ac77")
            putJsonObject("line-width") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(12)
                        add(1)
                    }
                    addJsonArray {
                        add(13)
                        add(3)
                    }
                    addJsonArray {
                        add(14)
                        add(4)
                    }
                    addJsonArray {
                        add(20)
                        add(15)
                    }
                }
            }
        }
        putJsonObject("layout") {
            put("line-cap", "round")
            put("line-join", "round")
        }
    }
    // Layer: road_service_track_casing
    addJsonObject {
        put("id", "road_service_track_casing")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("!in")
                add("brunnel")
                add("bridge")
                add("tunnel")
            }
            addJsonArray {
                add("in")
                add("class")
                add("service")
                add("track")
            }
        }
        putJsonObject("paint") {
            putNonNull("line-color", theme.roadServiceTrackCasingLineColor)
            putJsonObject("line-width") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(15)
                        add(1)
                    }
                    addJsonArray {
                        add(16)
                        add(4)
                    }
                    addJsonArray {
                        add(20)
                        add(11)
                    }
                }
            }
        }
        putJsonObject("layout") {
            put("line-cap", "round")
            put("line-join", "round")
        }
    }
    // Layer: road_link_casing
    addJsonObject {
        put("id", "road_link_casing")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        put("minzoom", 13)
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("!in")
                add("brunnel")
                add("bridge")
                add("tunnel")
            }
            addJsonArray {
                add("!in")
                add("class")
                add("pedestrian")
                add("path")
                add("track")
                add("service")
                add("motorway")
            }
            addJsonArray {
                add("==")
                add("ramp")
                add(1)
            }
        }
        putJsonObject("paint") {
            put("line-color", "#e9ac77")
            putJsonObject("line-width") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(12)
                        add(1)
                    }
                    addJsonArray {
                        add(13)
                        add(3)
                    }
                    addJsonArray {
                        add(14)
                        add(4)
                    }
                    addJsonArray {
                        add(20)
                        add(15)
                    }
                }
            }
        }
        putJsonObject("layout") {
            put("line-cap", "round")
            put("line-join", "round")
        }
    }
    // Layer: road_minor_casing
    addJsonObject {
        put("id", "road_minor_casing")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("\$type")
                add("LineString")
            }
            addJsonArray {
                add("!in")
                add("brunnel")
                add("bridge")
                add("tunnel")
            }
            addJsonArray {
                add("in")
                add("class")
                add("minor")
            }
            addJsonArray {
                add("!=")
                add("ramp")
                add(1)
            }
        }
        putJsonObject("paint") {
            putNonNull("line-color", theme.roadMinorCasingLineColor)
            putJsonObject("line-opacity") {
                putJsonArray("stops") {
                    addJsonArray {
                        add(12)
                        add(0)
                    }
                    addJsonArray {
                        add(12.5)
                        add(1)
                    }
                }
            }
            putNonNull("line-width", theme.roadMinorCasingLineWidth)
        }
        putJsonObject("layout") {
            put("line-cap", "round")
            put("line-join", "round")
        }
    }
    // Layer: road_secondary_tertiary_casing
    addJsonObject {
        put("id", "road_secondary_tertiary_casing")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("!in")
                add("brunnel")
                add("bridge")
                add("tunnel")
            }
            addJsonArray {
                add("in")
                add("class")
                add("secondary")
                add("tertiary")
                add("busway")
                add("bus_guideway")
            }
            addJsonArray {
                add("!=")
                add("ramp")
                add(1)
            }
        }
        putJsonObject("paint") {
            put("line-color", "#e9ac77")
            putJsonObject("line-width") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(8)
                        add(1.5)
                    }
                    addJsonArray {
                        add(20)
                        add(17)
                    }
                }
            }
        }
        putJsonObject("layout") {
            put("line-cap", "round")
            put("line-join", "round")
        }
    }
    // Layer: road_trunk_primary_casing
    addJsonObject {
        put("id", "road_trunk_primary_casing")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("!in")
                add("brunnel")
                add("bridge")
                add("tunnel")
            }
            addJsonArray {
                add("in")
                add("class")
                add("primary")
                add("trunk")
            }
        }
        putJsonObject("paint") {
            putNonNull("line-color", theme.roadTrunkPrimaryCasingLineColor)
            putNonNull("line-width", theme.roadTrunkPrimaryCasingLineWidth)
        }
        putJsonObject("layout") {
            put("line-join", "round")
        }
    }
    // Layer: road_motorway_casing
    addJsonObject {
        put("id", "road_motorway_casing")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        put("minzoom", 5)
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("!in")
                add("brunnel")
                add("bridge")
                add("tunnel")
            }
            addJsonArray {
                add("==")
                add("class")
                add("motorway")
            }
            addJsonArray {
                add("!=")
                add("ramp")
                add(1)
            }
        }
        putJsonObject("paint") {
            putNonNull("line-color", theme.roadMotorwayCasingLineColor)
            putNonNull("line-width", theme.roadMotorwayCasingLineWidth)
        }
        putJsonObject("layout") {
            put("line-cap", "round")
            put("line-join", "round")
        }
    }
    // Layer: road_path_pedestrian
    addJsonObject {
        put("id", "road_path_pedestrian")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        put("minzoom", 14)
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("\$type")
                add("LineString")
            }
            addJsonArray {
                add("!in")
                add("brunnel")
                add("bridge")
                add("tunnel")
            }
            addJsonArray {
                add("in")
                add("class")
                add("path")
                add("pedestrian")
            }
        }
        putJsonObject("paint") {
            putNonNull("line-color", theme.roadPathPedestrianLineColor)
            putJsonArray("line-dasharray") {
                add(1)
                add(0.7)
            }
            putJsonObject("line-width") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(14)
                        add(1)
                    }
                    addJsonArray {
                        add(20)
                        add(10)
                    }
                }
            }
        }
        putJsonObject("layout") {
            put("line-join", "round")
        }
    }
    // Layer: road_motorway_link
    addJsonObject {
        put("id", "road_motorway_link")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        put("minzoom", 12)
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("!in")
                add("brunnel")
                add("bridge")
                add("tunnel")
            }
            addJsonArray {
                add("==")
                add("class")
                add("motorway")
            }
            addJsonArray {
                add("==")
                add("ramp")
                add(1)
            }
        }
        putJsonObject("paint") {
            put("line-color", "#fc8")
            putJsonObject("line-width") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(12.5)
                        add(0)
                    }
                    addJsonArray {
                        add(13)
                        add(1.5)
                    }
                    addJsonArray {
                        add(14)
                        add(2.5)
                    }
                    addJsonArray {
                        add(20)
                        add(11.5)
                    }
                }
            }
        }
        putJsonObject("layout") {
            put("line-cap", "round")
            put("line-join", "round")
        }
    }
    // Layer: road_service_track
    addJsonObject {
        put("id", "road_service_track")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("!in")
                add("brunnel")
                add("bridge")
                add("tunnel")
            }
            addJsonArray {
                add("in")
                add("class")
                add("service")
                add("track")
            }
        }
        putJsonObject("paint") {
            putNonNull("line-color", theme.roadServiceTrackLineColor)
            putJsonObject("line-width") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(15.5)
                        add(0)
                    }
                    addJsonArray {
                        add(16)
                        add(2)
                    }
                    addJsonArray {
                        add(20)
                        add(7.5)
                    }
                }
            }
        }
        putJsonObject("layout") {
            put("line-cap", "round")
            put("line-join", "round")
        }
    }
    // Layer: road_link
    addJsonObject {
        put("id", "road_link")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        put("minzoom", 13)
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("!in")
                add("brunnel")
                add("bridge")
                add("tunnel")
            }
            addJsonArray {
                add("==")
                add("ramp")
                add(1)
            }
            addJsonArray {
                add("!in")
                add("class")
                add("pedestrian")
                add("path")
                add("track")
                add("service")
                add("motorway")
            }
        }
        putJsonObject("paint") {
            put("line-color", "#fea")
            putJsonObject("line-width") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(12.5)
                        add(0)
                    }
                    addJsonArray {
                        add(13)
                        add(1.5)
                    }
                    addJsonArray {
                        add(14)
                        add(2.5)
                    }
                    addJsonArray {
                        add(20)
                        add(11.5)
                    }
                }
            }
        }
        putJsonObject("layout") {
            put("line-cap", "round")
            put("line-join", "round")
        }
    }
    // Layer: road_minor
    addJsonObject {
        put("id", "road_minor")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("\$type")
                add("LineString")
            }
            addJsonArray {
                add("!in")
                add("brunnel")
                add("bridge")
                add("tunnel")
            }
            addJsonArray {
                add("in")
                add("class")
                add("minor")
            }
        }
        putJsonObject("paint") {
            put("line-color", "#fff")
            putJsonObject("line-width") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(13.5)
                        add(0)
                    }
                    addJsonArray {
                        add(14)
                        add(2.5)
                    }
                    addJsonArray {
                        add(20)
                        add(18)
                    }
                }
            }
        }
        putJsonObject("layout") {
            put("line-cap", "round")
            put("line-join", "round")
        }
    }
    // Layer: road_secondary_tertiary
    addJsonObject {
        put("id", "road_secondary_tertiary")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("!in")
                add("brunnel")
                add("bridge")
                add("tunnel")
            }
            addJsonArray {
                add("in")
                add("class")
                add("secondary")
                add("tertiary")
                add("busway")
                add("bus_guideway")
            }
        }
        putJsonObject("paint") {
            putNonNull("line-color", theme.roadSecondaryTertiaryLineColor)
            putJsonObject("line-width") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(6.5)
                        add(0)
                    }
                    addJsonArray {
                        add(8)
                        add(0.5)
                    }
                    addJsonArray {
                        add(20)
                        add(13)
                    }
                }
            }
        }
        putJsonObject("layout") {
            put("line-cap", "round")
            put("line-join", "round")
        }
    }
    // Layer: road_trunk_primary
    addJsonObject {
        put("id", "road_trunk_primary")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("!in")
                add("brunnel")
                add("bridge")
                add("tunnel")
            }
            addJsonArray {
                add("in")
                add("class")
                add("primary")
                add("trunk")
            }
        }
        putJsonObject("paint") {
            putNonNull("line-color", theme.roadTrunkPrimaryLineColor)
            putNonNull("line-width", theme.roadTrunkPrimaryLineWidth)
        }
        putJsonObject("layout") {
            put("line-join", "round")
        }
    }
    // Layer: road_motorway
    addJsonObject {
        put("id", "road_motorway")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        put("minzoom", 5)
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("!in")
                add("brunnel")
                add("bridge")
                add("tunnel")
            }
            addJsonArray {
                add("==")
                add("class")
                add("motorway")
            }
            addJsonArray {
                add("!=")
                add("ramp")
                add(1)
            }
        }
        putJsonObject("paint") {
            putNonNull("line-color", theme.roadMotorwayLineColor)
            putNonNull("line-width", theme.roadMotorwayLineWidth)
        }
        putJsonObject("layout") {
            put("line-cap", "round")
            put("line-join", "round")
        }
    }
    // Layer: road_major_rail
    addJsonObject {
        put("id", "road_major_rail")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("!in")
                add("brunnel")
                add("bridge")
                add("tunnel")
            }
            addJsonArray {
                add("==")
                add("class")
                add("rail")
            }
        }
        putJsonObject("paint") {
            put("line-color", "#bbb")
            putJsonObject("line-width") {
                put("base", 1.4)
                putJsonArray("stops") {
                    addJsonArray {
                        add(14)
                        add(0.4)
                    }
                    addJsonArray {
                        add(15)
                        add(0.75)
                    }
                    addJsonArray {
                        add(20)
                        add(2)
                    }
                }
            }
        }
    }
    // Layer: road_major_rail_hatching
    addJsonObject {
        put("id", "road_major_rail_hatching")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("!in")
                add("brunnel")
                add("bridge")
                add("tunnel")
            }
            addJsonArray {
                add("==")
                add("class")
                add("rail")
            }
        }
        putJsonObject("paint") {
            put("line-color", "#bbb")
            putJsonArray("line-dasharray") {
                add(0.2)
                add(8)
            }
            putJsonObject("line-width") {
                put("base", 1.4)
                putJsonArray("stops") {
                    addJsonArray {
                        add(14.5)
                        add(0)
                    }
                    addJsonArray {
                        add(15)
                        add(3)
                    }
                    addJsonArray {
                        add(20)
                        add(8)
                    }
                }
            }
        }
    }
    // Layer: road_transit_rail
    addJsonObject {
        put("id", "road_transit_rail")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("!in")
                add("brunnel")
                add("bridge")
                add("tunnel")
            }
            addJsonArray {
                add("==")
                add("class")
                add("transit")
            }
        }
        putJsonObject("paint") {
            put("line-color", "#bbb")
            putJsonObject("line-width") {
                put("base", 1.4)
                putJsonArray("stops") {
                    addJsonArray {
                        add(14)
                        add(0.4)
                    }
                    addJsonArray {
                        add(15)
                        add(0.75)
                    }
                    addJsonArray {
                        add(20)
                        add(2)
                    }
                }
            }
        }
    }
    // Layer: road_transit_rail_hatching
    addJsonObject {
        put("id", "road_transit_rail_hatching")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("!in")
                add("brunnel")
                add("bridge")
                add("tunnel")
            }
            addJsonArray {
                add("==")
                add("class")
                add("transit")
            }
        }
        putJsonObject("paint") {
            put("line-color", "#bbb")
            putJsonArray("line-dasharray") {
                add(0.2)
                add(8)
            }
            putJsonObject("line-width") {
                put("base", 1.4)
                putJsonArray("stops") {
                    addJsonArray {
                        add(14.5)
                        add(0)
                    }
                    addJsonArray {
                        add(15)
                        add(3)
                    }
                    addJsonArray {
                        add(20)
                        add(8)
                    }
                }
            }
        }
    }
    // Layer: road_one_way_arrow
    addJsonObject {
        put("id", "road_one_way_arrow")
        put("type", "symbol")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        put("minzoom", 16)
        putJsonArray("filter") {
            add("==")
            add("oneway")
            add(1)
        }
        putJsonObject("layout") {
            put("icon-image", "arrow")
            put("symbol-placement", "line")
        }
    }
    // Layer: road_one_way_arrow_opposite
    addJsonObject {
        put("id", "road_one_way_arrow_opposite")
        put("type", "symbol")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        put("minzoom", 16)
        putJsonArray("filter") {
            add("==")
            add("oneway")
            add(-1)
        }
        putJsonObject("layout") {
            put("icon-image", "arrow")
            put("symbol-placement", "line")
            put("icon-rotate", 180)
        }
    }
    // Layer: bridge_motorway_link_casing
    addJsonObject {
        put("id", "bridge_motorway_link_casing")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("class")
                add("motorway")
            }
            addJsonArray {
                add("==")
                add("ramp")
                add(1)
            }
            addJsonArray {
                add("==")
                add("brunnel")
                add("bridge")
            }
        }
        putJsonObject("paint") {
            put("line-color", "#e9ac77")
            putJsonObject("line-width") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(12)
                        add(1)
                    }
                    addJsonArray {
                        add(13)
                        add(3)
                    }
                    addJsonArray {
                        add(14)
                        add(4)
                    }
                    addJsonArray {
                        add(20)
                        add(15)
                    }
                }
            }
        }
        putJsonObject("layout") {
            put("line-join", "round")
        }
    }
    // Layer: bridge_service_track_casing
    addJsonObject {
        put("id", "bridge_service_track_casing")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("brunnel")
                add("bridge")
            }
            addJsonArray {
                add("in")
                add("class")
                add("service")
                add("track")
            }
        }
        putJsonObject("paint") {
            put("line-color", "#cfcdca")
            putJsonObject("line-width") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(15)
                        add(1)
                    }
                    addJsonArray {
                        add(16)
                        add(4)
                    }
                    addJsonArray {
                        add(20)
                        add(11)
                    }
                }
            }
        }
        putJsonObject("layout") {
            put("line-join", "round")
        }
    }
    // Layer: bridge_link_casing
    addJsonObject {
        put("id", "bridge_link_casing")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("class")
                add("link")
            }
            addJsonArray {
                add("==")
                add("brunnel")
                add("bridge")
            }
        }
        putJsonObject("paint") {
            put("line-color", "#e9ac77")
            putJsonObject("line-width") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(12)
                        add(1)
                    }
                    addJsonArray {
                        add(13)
                        add(3)
                    }
                    addJsonArray {
                        add(14)
                        add(4)
                    }
                    addJsonArray {
                        add(20)
                        add(15)
                    }
                }
            }
        }
        putJsonObject("layout") {
            put("line-join", "round")
        }
    }
    // Layer: bridge_street_casing
    addJsonObject {
        put("id", "bridge_street_casing")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("brunnel")
                add("bridge")
            }
            addJsonArray {
                add("in")
                add("class")
                add("street")
                add("street_limited")
            }
        }
        putJsonObject("paint") {
            put("line-color", "hsl(36, 6%, 74%)")
            putJsonObject("line-opacity") {
                putJsonArray("stops") {
                    addJsonArray {
                        add(12)
                        add(0)
                    }
                    addJsonArray {
                        add(12.5)
                        add(1)
                    }
                }
            }
            putJsonObject("line-width") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(12)
                        add(0.5)
                    }
                    addJsonArray {
                        add(13)
                        add(1)
                    }
                    addJsonArray {
                        add(14)
                        add(4)
                    }
                    addJsonArray {
                        add(20)
                        add(25)
                    }
                }
            }
        }
        putJsonObject("layout") {
            put("line-join", "round")
        }
    }
    // Layer: bridge_path_pedestrian_casing
    addJsonObject {
        put("id", "bridge_path_pedestrian_casing")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("\$type")
                add("LineString")
            }
            addJsonArray {
                add("==")
                add("brunnel")
                add("bridge")
            }
            addJsonArray {
                add("in")
                add("class")
                add("path")
                add("pedestrian")
            }
        }
        putJsonObject("paint") {
            putNonNull("line-color", theme.bridgePathPedestrianCasingLineColor)
            putJsonArray("line-dasharray") {
                add(1)
                add(0)
            }
            putJsonObject("line-width") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(14)
                        add(1.5)
                    }
                    addJsonArray {
                        add(20)
                        add(18)
                    }
                }
            }
        }
    }
    // Layer: bridge_secondary_tertiary_casing
    addJsonObject {
        put("id", "bridge_secondary_tertiary_casing")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("brunnel")
                add("bridge")
            }
            addJsonArray {
                add("in")
                add("class")
                add("secondary")
                add("tertiary")
                add("busway")
                add("bus_guideway")
            }
        }
        putJsonObject("paint") {
            put("line-color", "#e9ac77")
            putJsonObject("line-width") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(8)
                        add(1.5)
                    }
                    addJsonArray {
                        add(20)
                        add(17)
                    }
                }
            }
        }
        putJsonObject("layout") {
            put("line-join", "round")
        }
    }
    // Layer: bridge_trunk_primary_casing
    addJsonObject {
        put("id", "bridge_trunk_primary_casing")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("brunnel")
                add("bridge")
            }
            addJsonArray {
                add("in")
                add("class")
                add("primary")
                add("trunk")
            }
        }
        putJsonObject("paint") {
            put("line-color", "#e9ac77")
            putJsonObject("line-width") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(5)
                        add(0.4)
                    }
                    addJsonArray {
                        add(6)
                        add(0.7)
                    }
                    addJsonArray {
                        add(7)
                        add(1.75)
                    }
                    addJsonArray {
                        add(20)
                        add(22)
                    }
                }
            }
        }
        putJsonObject("layout") {
            put("line-join", "round")
        }
    }
    // Layer: bridge_motorway_casing
    addJsonObject {
        put("id", "bridge_motorway_casing")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("class")
                add("motorway")
            }
            addJsonArray {
                add("!=")
                add("ramp")
                add(1)
            }
            addJsonArray {
                add("==")
                add("brunnel")
                add("bridge")
            }
        }
        putJsonObject("paint") {
            put("line-color", "#e9ac77")
            putJsonObject("line-width") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(5)
                        add(0.4)
                    }
                    addJsonArray {
                        add(6)
                        add(0.7)
                    }
                    addJsonArray {
                        add(7)
                        add(1.75)
                    }
                    addJsonArray {
                        add(20)
                        add(22)
                    }
                }
            }
        }
        putJsonObject("layout") {
            put("line-join", "round")
        }
    }
    // Layer: bridge_path_pedestrian
    addJsonObject {
        put("id", "bridge_path_pedestrian")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("\$type")
                add("LineString")
            }
            addJsonArray {
                add("==")
                add("brunnel")
                add("bridge")
            }
            addJsonArray {
                add("in")
                add("class")
                add("path")
                add("pedestrian")
            }
        }
        putJsonObject("paint") {
            putNonNull("line-color", theme.bridgePathPedestrianLineColor)
            putJsonArray("line-dasharray") {
                add(1)
                add(0.3)
            }
            putJsonObject("line-width") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(14)
                        add(0.5)
                    }
                    addJsonArray {
                        add(20)
                        add(10)
                    }
                }
            }
        }
    }
    // Layer: bridge_motorway_link
    addJsonObject {
        put("id", "bridge_motorway_link")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("class")
                add("motorway")
            }
            addJsonArray {
                add("==")
                add("ramp")
                add(1)
            }
            addJsonArray {
                add("==")
                add("brunnel")
                add("bridge")
            }
        }
        putJsonObject("paint") {
            put("line-color", "#fc8")
            putJsonObject("line-width") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(12.5)
                        add(0)
                    }
                    addJsonArray {
                        add(13)
                        add(1.5)
                    }
                    addJsonArray {
                        add(14)
                        add(2.5)
                    }
                    addJsonArray {
                        add(20)
                        add(11.5)
                    }
                }
            }
        }
        putJsonObject("layout") {
            put("line-join", "round")
        }
    }
    // Layer: bridge_service_track
    addJsonObject {
        put("id", "bridge_service_track")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("brunnel")
                add("bridge")
            }
            addJsonArray {
                add("in")
                add("class")
                add("service")
                add("track")
            }
        }
        putJsonObject("paint") {
            put("line-color", "#fff")
            putJsonObject("line-width") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(15.5)
                        add(0)
                    }
                    addJsonArray {
                        add(16)
                        add(2)
                    }
                    addJsonArray {
                        add(20)
                        add(7.5)
                    }
                }
            }
        }
        putJsonObject("layout") {
            put("line-join", "round")
        }
    }
    // Layer: bridge_link
    addJsonObject {
        put("id", "bridge_link")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("class")
                add("link")
            }
            addJsonArray {
                add("==")
                add("brunnel")
                add("bridge")
            }
        }
        putJsonObject("paint") {
            put("line-color", "#fea")
            putJsonObject("line-width") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(12.5)
                        add(0)
                    }
                    addJsonArray {
                        add(13)
                        add(1.5)
                    }
                    addJsonArray {
                        add(14)
                        add(2.5)
                    }
                    addJsonArray {
                        add(20)
                        add(11.5)
                    }
                }
            }
        }
        putJsonObject("layout") {
            put("line-join", "round")
        }
    }
    // Layer: bridge_street
    addJsonObject {
        put("id", "bridge_street")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("brunnel")
                add("bridge")
            }
            addJsonArray {
                add("in")
                add("class")
                add("minor")
            }
        }
        putJsonObject("paint") {
            put("line-color", "#fff")
            putJsonObject("line-width") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(13.5)
                        add(0)
                    }
                    addJsonArray {
                        add(14)
                        add(2.5)
                    }
                    addJsonArray {
                        add(20)
                        add(18)
                    }
                }
            }
        }
        putJsonObject("layout") {
            put("line-join", "round")
        }
    }
    // Layer: bridge_secondary_tertiary
    addJsonObject {
        put("id", "bridge_secondary_tertiary")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("brunnel")
                add("bridge")
            }
            addJsonArray {
                add("in")
                add("class")
                add("secondary")
                add("tertiary")
                add("busway")
                add("bus_guideway")
            }
        }
        putJsonObject("paint") {
            put("line-color", "#fea")
            putJsonObject("line-width") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(6.5)
                        add(0)
                    }
                    addJsonArray {
                        add(7)
                        add(0.5)
                    }
                    addJsonArray {
                        add(20)
                        add(10)
                    }
                }
            }
        }
        putJsonObject("layout") {
            put("line-join", "round")
        }
    }
    // Layer: bridge_trunk_primary
    addJsonObject {
        put("id", "bridge_trunk_primary")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("brunnel")
                add("bridge")
            }
            addJsonArray {
                add("in")
                add("class")
                add("primary")
                add("trunk")
            }
        }
        putJsonObject("paint") {
            put("line-color", "#fea")
            putJsonObject("line-width") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(5)
                        add(0)
                    }
                    addJsonArray {
                        add(7)
                        add(1)
                    }
                    addJsonArray {
                        add(20)
                        add(18)
                    }
                }
            }
        }
        putJsonObject("layout") {
            put("line-join", "round")
        }
    }
    // Layer: bridge_motorway
    addJsonObject {
        put("id", "bridge_motorway")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("class")
                add("motorway")
            }
            addJsonArray {
                add("!=")
                add("ramp")
                add(1)
            }
            addJsonArray {
                add("==")
                add("brunnel")
                add("bridge")
            }
        }
        putJsonObject("paint") {
            put("line-color", "#fc8")
            putJsonObject("line-width") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(5)
                        add(0)
                    }
                    addJsonArray {
                        add(7)
                        add(1)
                    }
                    addJsonArray {
                        add(20)
                        add(18)
                    }
                }
            }
        }
        putJsonObject("layout") {
            put("line-join", "round")
        }
    }
    // Layer: bridge_major_rail
    addJsonObject {
        put("id", "bridge_major_rail")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("class")
                add("rail")
            }
            addJsonArray {
                add("==")
                add("brunnel")
                add("bridge")
            }
        }
        putJsonObject("paint") {
            put("line-color", "#bbb")
            putJsonObject("line-width") {
                put("base", 1.4)
                putJsonArray("stops") {
                    addJsonArray {
                        add(14)
                        add(0.4)
                    }
                    addJsonArray {
                        add(15)
                        add(0.75)
                    }
                    addJsonArray {
                        add(20)
                        add(2)
                    }
                }
            }
        }
    }
    // Layer: bridge_major_rail_hatching
    addJsonObject {
        put("id", "bridge_major_rail_hatching")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("class")
                add("rail")
            }
            addJsonArray {
                add("==")
                add("brunnel")
                add("bridge")
            }
        }
        putJsonObject("paint") {
            put("line-color", "#bbb")
            putJsonArray("line-dasharray") {
                add(0.2)
                add(8)
            }
            putJsonObject("line-width") {
                put("base", 1.4)
                putJsonArray("stops") {
                    addJsonArray {
                        add(14.5)
                        add(0)
                    }
                    addJsonArray {
                        add(15)
                        add(3)
                    }
                    addJsonArray {
                        add(20)
                        add(8)
                    }
                }
            }
        }
    }
    // Layer: bridge_transit_rail
    addJsonObject {
        put("id", "bridge_transit_rail")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("class")
                add("transit")
            }
            addJsonArray {
                add("==")
                add("brunnel")
                add("bridge")
            }
        }
        putJsonObject("paint") {
            put("line-color", "#bbb")
            putJsonObject("line-width") {
                put("base", 1.4)
                putJsonArray("stops") {
                    addJsonArray {
                        add(14)
                        add(0.4)
                    }
                    addJsonArray {
                        add(15)
                        add(0.75)
                    }
                    addJsonArray {
                        add(20)
                        add(2)
                    }
                }
            }
        }
    }
    // Layer: bridge_transit_rail_hatching
    addJsonObject {
        put("id", "bridge_transit_rail_hatching")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "transportation")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("class")
                add("transit")
            }
            addJsonArray {
                add("==")
                add("brunnel")
                add("bridge")
            }
        }
        putJsonObject("paint") {
            put("line-color", "#bbb")
            putJsonArray("line-dasharray") {
                add(0.2)
                add(8)
            }
            putJsonObject("line-width") {
                put("base", 1.4)
                putJsonArray("stops") {
                    addJsonArray {
                        add(14.5)
                        add(0)
                    }
                    addJsonArray {
                        add(15)
                        add(3)
                    }
                    addJsonArray {
                        add(20)
                        add(8)
                    }
                }
            }
        }
    }
    // Layer: building
    addJsonObject {
        put("id", "building")
        put("type", "fill")
        put("source", "openmaptiles")
        put("source-layer", "building")
        put("minzoom", 13)
        put("maxzoom", 14)
        putJsonObject("paint") {
            putNonNull("fill-color", theme.buildingFillColor)
            putJsonObject("fill-outline-color") {
                put("base", 1)
                putJsonArray("stops") {
                    addJsonArray {
                        add(13)
                        add("hsla(35, 6%, 79%, 0.32)")
                    }
                    addJsonArray {
                        add(14)
                        add("hsl(35, 6%, 79%)")
                    }
                }
            }
            putNonNull("fill-opacity", theme.buildingFillOpacity)
        }
    }
    // Layer: building-3d
    addJsonObject {
        put("id", "building-3d")
        put("type", "fill-extrusion")
        put("source", "openmaptiles")
        put("source-layer", "building")
        put("minzoom", 14)
        putJsonObject("paint") {
            putNonNull("fill-extrusion-color", theme.building3dFillExtrusionColor)
            putJsonObject("fill-extrusion-height") {
                put("property", "render_height")
                put("type", "identity")
            }
            putJsonObject("fill-extrusion-base") {
                put("property", "render_min_height")
                put("type", "identity")
            }
            putNonNull("fill-extrusion-opacity", theme.building3dFillExtrusionOpacity)
        }
    }
    // Layer: boundary_3
    addJsonObject {
        put("id", "boundary_3")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "boundary")
        put("minzoom", 8)
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("in")
                add("admin_level")
                add(3)
                add(4)
            }
        }
        putJsonObject("paint") {
            put("line-color", "#9e9cab")
            putJsonArray("line-dasharray") {
                add(5)
                add(1)
            }
            putJsonObject("line-width") {
                put("base", 1)
                putJsonArray("stops") {
                    addJsonArray {
                        add(4)
                        add(0.4)
                    }
                    addJsonArray {
                        add(5)
                        add(1)
                    }
                    addJsonArray {
                        add(12)
                        add(1.8)
                    }
                }
            }
        }
        putJsonObject("layout") {
            put("line-join", "round")
        }
    }
    // Layer: boundary_2_z0-4
    addJsonObject {
        put("id", "boundary_2_z0-4")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "boundary")
        put("maxzoom", 5)
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("admin_level")
                add(2)
            }
            addJsonArray {
                add("!has")
                add("claimed_by")
            }
        }
        putJsonObject("paint") {
            put("line-color", "hsl(248, 1%, 41%)")
            putJsonObject("line-opacity") {
                put("base", 1)
                putJsonArray("stops") {
                    addJsonArray {
                        add(0)
                        add(0.4)
                    }
                    addJsonArray {
                        add(4)
                        add(1)
                    }
                }
            }
            putJsonObject("line-width") {
                put("base", 1)
                putJsonArray("stops") {
                    addJsonArray {
                        add(3)
                        add(1)
                    }
                    addJsonArray {
                        add(5)
                        add(1.2)
                    }
                    addJsonArray {
                        add(12)
                        add(3)
                    }
                }
            }
        }
        putJsonObject("layout") {
            put("line-cap", "round")
            put("line-join", "round")
        }
    }
    // Layer: boundary_2_z5-
    addJsonObject {
        put("id", "boundary_2_z5-")
        put("type", "line")
        put("source", "openmaptiles")
        put("source-layer", "boundary")
        put("minzoom", 5)
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("admin_level")
                add(2)
            }
        }
        putJsonObject("paint") {
            put("line-color", "hsl(248, 1%, 41%)")
            putJsonObject("line-opacity") {
                put("base", 1)
                putJsonArray("stops") {
                    addJsonArray {
                        add(0)
                        add(0.4)
                    }
                    addJsonArray {
                        add(4)
                        add(1)
                    }
                }
            }
            putJsonObject("line-width") {
                put("base", 1)
                putJsonArray("stops") {
                    addJsonArray {
                        add(3)
                        add(1)
                    }
                    addJsonArray {
                        add(5)
                        add(1.2)
                    }
                    addJsonArray {
                        add(12)
                        add(3)
                    }
                }
            }
        }
        putJsonObject("layout") {
            put("line-cap", "round")
            put("line-join", "round")
        }
    }
    // Layer: water_name_line
    addJsonObject {
        put("id", "water_name_line")
        put("type", "symbol")
        put("source", "openmaptiles")
        put("source-layer", "waterway")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("\$type")
                add("LineString")
            }
        }
        putJsonObject("paint") {
            putNonNull("text-color", theme.waterNameLineTextColor)
            put("text-halo-color", "rgba(255,255,255,0.7)")
            putNonNull("text-halo-width", theme.waterNameLineTextHaloWidth)
        }
        putJsonObject("layout") {
            put("text-field", "{name}")
            putNonNull("text-font", theme.waterNameLineTextFont)
            put("text-max-width", 5)
            putNonNull("text-size", theme.waterNameLineTextSize)
            put("symbol-placement", "line")
        }
    }
    // Layer: water_name_point
    addJsonObject {
        put("id", "water_name_point")
        put("type", "symbol")
        put("source", "openmaptiles")
        put("source-layer", "water_name")
        putJsonArray("filter") {
            add("==")
            add("\$type")
            add("Point")
        }
        putJsonObject("paint") {
            putNonNull("text-color", theme.waterNamePointTextColor)
            put("text-halo-color", "rgba(255,255,255,0.7)")
            putNonNull("text-halo-blur", theme.waterNamePointTextHaloBlur)
            put("text-halo-width", 1)
        }
        putJsonObject("layout") {
            put("text-field", "{name}")
            putNonNull("text-font", theme.waterNamePointTextFont)
            put("text-max-width", 5)
            putNonNull("text-size", theme.waterNamePointTextSize)
        }
    }
    // Layer: poi_z16
    addJsonObject {
        put("id", "poi_z16")
        put("type", "symbol")
        put("source", "openmaptiles")
        put("source-layer", "poi")
        put("minzoom", 16)
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("\$type")
                add("Point")
            }
            addJsonArray {
                add(">=")
                add("rank")
                add(20)
            }
        }
        putJsonObject("paint") {
            putNonNull("text-color", theme.poiZ16TextColor)
            put("text-halo-blur", 0.5)
            put("text-halo-color", "#ffffff")
            put("text-halo-width", 1)
        }
        putJsonObject("layout") {
            putJsonArray("icon-image") {
                add("match")
                addJsonArray {
                    add("get")
                    add("subclass")
                }
                addJsonArray {
                    add("florist")
                    add("furniture")
                    add("soccer")
                    add("tennis")
                }
                addJsonArray {
                    add("get")
                    add("subclass")
                }
                addJsonArray {
                    add("get")
                    add("class")
                }
            }
            put("text-anchor", "top")
            put("text-field", "{name}")
            putNonNull("text-font", theme.poiZ16TextFont)
            put("text-max-width", 9)
            putJsonArray("text-offset") {
                add(0)
                add(0.6)
            }
            putNonNull("text-size", theme.poiZ16TextSize)
            putNonNull("icon-size", theme.poiZ16IconSize)
        }
    }
    // Layer: poi_z15
    addJsonObject {
        put("id", "poi_z15")
        put("type", "symbol")
        put("source", "openmaptiles")
        put("source-layer", "poi")
        put("minzoom", 15)
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("\$type")
                add("Point")
            }
            addJsonArray {
                add(">=")
                add("rank")
                add(7)
            }
            addJsonArray {
                add("<")
                add("rank")
                add(20)
            }
        }
        putJsonObject("paint") {
            putNonNull("text-color", theme.poiZ15TextColor)
            put("text-halo-blur", 0.5)
            put("text-halo-color", "#ffffff")
            put("text-halo-width", 1)
        }
        putJsonObject("layout") {
            putJsonArray("icon-image") {
                add("match")
                addJsonArray {
                    add("get")
                    add("subclass")
                }
                addJsonArray {
                    add("florist")
                    add("furniture")
                    add("soccer")
                    add("tennis")
                }
                addJsonArray {
                    add("get")
                    add("subclass")
                }
                addJsonArray {
                    add("get")
                    add("class")
                }
            }
            put("text-anchor", "top")
            put("text-field", "{name}")
            putNonNull("text-font", theme.poiZ15TextFont)
            put("text-max-width", 9)
            putJsonArray("text-offset") {
                add(0)
                add(0.6)
            }
            putNonNull("text-size", theme.poiZ15TextSize)
            putNonNull("icon-size", theme.poiZ15IconSize)
        }
    }
    // Layer: poi_z14
    addJsonObject {
        put("id", "poi_z14")
        put("type", "symbol")
        put("source", "openmaptiles")
        put("source-layer", "poi")
        put("minzoom", 14)
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("\$type")
                add("Point")
            }
            addJsonArray {
                add(">=")
                add("rank")
                add(1)
            }
            addJsonArray {
                add("<")
                add("rank")
                add(7)
            }
        }
        putJsonObject("paint") {
            putNonNull("text-color", theme.poiZ14TextColor)
            put("text-halo-blur", 0.5)
            put("text-halo-color", "#ffffff")
            put("text-halo-width", 1)
        }
        putJsonObject("layout") {
            putJsonArray("icon-image") {
                add("match")
                addJsonArray {
                    add("get")
                    add("subclass")
                }
                addJsonArray {
                    add("florist")
                    add("furniture")
                    add("soccer")
                    add("tennis")
                }
                addJsonArray {
                    add("get")
                    add("subclass")
                }
                addJsonArray {
                    add("get")
                    add("class")
                }
            }
            put("text-anchor", "top")
            put("text-field", "{name}")
            putNonNull("text-font", theme.poiZ14TextFont)
            put("text-max-width", 9)
            putJsonArray("text-offset") {
                add(0)
                add(0.6)
            }
            putNonNull("text-size", theme.poiZ14TextSize)
            putNonNull("icon-size", theme.poiZ14IconSize)
        }
    }
    // Layer: poi_transit
    addJsonObject {
        put("id", "poi_transit")
        put("type", "symbol")
        put("source", "openmaptiles")
        put("source-layer", "poi")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("in")
                add("class")
                add("bus")
                add("rail")
                add("airport")
            }
        }
        putJsonObject("paint") {
            putNonNull("text-color", theme.poiTransitTextColor)
            put("text-halo-color", "#ffffff")
            putNonNull("text-halo-width", theme.poiTransitTextHaloWidth)
            putNonNull("icon-halo-width", theme.poiTransitIconHaloWidth)
            putNonNull("icon-halo-color", theme.poiTransitIconHaloColor)
            putNonNull("text-halo-blur", theme.poiTransitTextHaloBlur)
        }
        putJsonObject("layout") {
            put("icon-image", "{class}")
            put("text-anchor", "left")
            put("text-field", "{name_en}")
            putNonNull("text-font", theme.poiTransitTextFont)
            put("text-max-width", 9)
            putJsonArray("text-offset") {
                add(0.9)
                add(0)
            }
            putNonNull("text-size", theme.poiTransitTextSize)
        }
    }
    // Layer: road_label
    addJsonObject {
        put("id", "road_label")
        put("type", "symbol")
        put("source", "openmaptiles")
        put("source-layer", "transportation_name")
        putJsonArray("filter") {
            add("all")
        }
        putJsonObject("paint") {
            putNonNull("text-color", theme.roadLabelTextColor)
            putNonNull("text-halo-width", theme.roadLabelTextHaloWidth)
            putNonNull("text-halo-color", theme.roadLabelTextHaloColor)
            putNonNull("text-halo-blur", theme.roadLabelTextHaloBlur)
        }
        putJsonObject("layout") {
            put("symbol-placement", "line")
            put("text-anchor", "center")
            put("text-field", "{name}")
            putNonNull("text-font", theme.roadLabelTextFont)
            putNonNull("text-size", theme.roadLabelTextSize)
            putNonNull("text-line-height", theme.roadLabelTextLineHeight)
            putNonNull("text-offset", theme.roadLabelTextOffset)
        }
    }
    // Layer: road_shield
    addJsonObject {
        put("id", "road_shield")
        put("type", "symbol")
        put("source", "openmaptiles")
        put("source-layer", "transportation_name")
        put("minzoom", 7)
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("<=")
                add("ref_length")
                add(6)
            }
        }
        putJsonObject("layout") {
            put("icon-image", "default_{ref_length}")
            put("icon-rotation-alignment", "viewport")
            putJsonObject("symbol-placement") {
                put("base", 1)
                putJsonArray("stops") {
                    addJsonArray {
                        add(10)
                        add("point")
                    }
                    addJsonArray {
                        add(11)
                        add("line")
                    }
                }
            }
            put("symbol-spacing", 500)
            put("text-field", "{ref}")
            putNonNull("text-font", theme.roadShieldTextFont)
            putJsonArray("text-offset") {
                add(0)
                add(0.1)
            }
            put("text-rotation-alignment", "viewport")
            putNonNull("text-size", theme.roadShieldTextSize)
            putNonNull("icon-size", theme.roadShieldIconSize)
        }
    }
    // Layer: place_other
    addJsonObject {
        put("id", "place_other")
        put("type", "symbol")
        put("source", "openmaptiles")
        put("source-layer", "place")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("in")
                add("class")
                add("hamlet")
                add("island")
                add("islet")
                add("neighbourhood")
                add("suburb")
                add("quarter")
            }
        }
        putJsonObject("paint") {
            putNonNull("text-color", theme.placeOtherTextColor)
            put("text-halo-color", "rgba(255,255,255,0.8)")
            put("text-halo-width", 1.2)
        }
        putJsonObject("layout") {
            put("text-field", "{name_en}")
            putNonNull("text-font", theme.placeOtherTextFont)
            put("text-letter-spacing", 0.1)
            put("text-max-width", 9)
            putJsonObject("text-size") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(12)
                        add(10)
                    }
                    addJsonArray {
                        add(15)
                        add(14)
                    }
                }
            }
            put("text-transform", "uppercase")
        }
    }
    // Layer: place_village
    addJsonObject {
        put("id", "place_village")
        put("type", "symbol")
        put("source", "openmaptiles")
        put("source-layer", "place")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("class")
                add("village")
            }
        }
        putJsonObject("paint") {
            put("text-color", "#333")
            put("text-halo-color", "rgba(255,255,255,0.8)")
            put("text-halo-width", 1.2)
        }
        putJsonObject("layout") {
            put("text-field", "{name_en}")
            putNonNull("text-font", theme.placeVillageTextFont)
            put("text-max-width", 8)
            putJsonObject("text-size") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(10)
                        add(12)
                    }
                    addJsonArray {
                        add(15)
                        add(22)
                    }
                }
            }
        }
    }
    // Layer: place_town
    addJsonObject {
        put("id", "place_town")
        put("type", "symbol")
        put("source", "openmaptiles")
        put("source-layer", "place")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("class")
                add("town")
            }
        }
        putJsonObject("paint") {
            put("text-color", "#333")
            put("text-halo-color", "rgba(255,255,255,0.8)")
            put("text-halo-width", 1.2)
        }
        putJsonObject("layout") {
            putJsonObject("icon-image") {
                put("base", 1)
                putJsonArray("stops") {
                    addJsonArray {
                        add(0)
                        add("dot_9")
                    }
                    addJsonArray {
                        add(8)
                        add("")
                    }
                }
            }
            put("text-anchor", "bottom")
            put("text-field", "{name_en}")
            putNonNull("text-font", theme.placeTownTextFont)
            put("text-max-width", 8)
            putJsonArray("text-offset") {
                add(0)
                add(0)
            }
            putNonNull("text-size", theme.placeTownTextSize)
        }
    }
    // Layer: place_city
    addJsonObject {
        put("id", "place_city")
        put("type", "symbol")
        put("source", "openmaptiles")
        put("source-layer", "place")
        put("minzoom", 5)
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("class")
                add("city")
            }
        }
        putJsonObject("paint") {
            put("text-color", "#333")
            put("text-halo-color", "rgba(255,255,255,0.8)")
            put("text-halo-width", 1.2)
        }
        putJsonObject("layout") {
            putJsonObject("icon-image") {
                put("base", 1)
                putJsonArray("stops") {
                    addJsonArray {
                        add(0)
                        add("dot_9")
                    }
                    addJsonArray {
                        add(8)
                        add("")
                    }
                }
            }
            put("text-anchor", "bottom")
            put("text-field", "{name_en}")
            putNonNull("text-font", theme.placeCityTextFont)
            put("text-max-width", 8)
            putJsonArray("text-offset") {
                add(0)
                add(0)
            }
            putJsonObject("text-size") {
                put("base", 1.2)
                putJsonArray("stops") {
                    addJsonArray {
                        add(7)
                        add(14)
                    }
                    addJsonArray {
                        add(11)
                        add(24)
                    }
                }
            }
            put("icon-allow-overlap", true)
            put("icon-optional", false)
        }
    }
    // Layer: state
    addJsonObject {
        put("id", "state")
        put("type", "symbol")
        put("source", "openmaptiles")
        put("source-layer", "place")
        put("maxzoom", 6)
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("class")
                add("state")
            }
        }
        putJsonObject("paint") {
            put("text-color", "#633")
            put("text-halo-color", "rgba(255,255,255,0.7)")
            put("text-halo-width", 1)
        }
        putJsonObject("layout") {
            put("text-field", "{name_en}")
            putJsonArray("text-font") {
                add("Roboto Condensed Italic")
            }
            putJsonObject("text-size") {
                putJsonArray("stops") {
                    addJsonArray {
                        add(4)
                        add(11)
                    }
                    addJsonArray {
                        add(6)
                        add(15)
                    }
                }
            }
            put("text-transform", "uppercase")
        }
    }
    // Layer: country_3
    addJsonObject {
        put("id", "country_3")
        put("type", "symbol")
        put("source", "openmaptiles")
        put("source-layer", "place")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add(">=")
                add("rank")
                add(3)
            }
            addJsonArray {
                add("==")
                add("class")
                add("country")
            }
        }
        putJsonObject("paint") {
            put("text-color", "#334")
            put("text-halo-blur", 1)
            put("text-halo-color", "rgba(255,255,255,0.8)")
            put("text-halo-width", 1)
        }
        putJsonObject("layout") {
            put("text-field", "{name_en}")
            putJsonArray("text-font") {
                add("Roboto Condensed Italic")
            }
            put("text-max-width", 6.25)
            putJsonObject("text-size") {
                putJsonArray("stops") {
                    addJsonArray {
                        add(3)
                        add(11)
                    }
                    addJsonArray {
                        add(7)
                        add(17)
                    }
                }
            }
            put("text-transform", "none")
        }
    }
    // Layer: country_2
    addJsonObject {
        put("id", "country_2")
        put("type", "symbol")
        put("source", "openmaptiles")
        put("source-layer", "place")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("rank")
                add(2)
            }
            addJsonArray {
                add("==")
                add("class")
                add("country")
            }
        }
        putJsonObject("paint") {
            put("text-color", "#334")
            put("text-halo-blur", 1)
            put("text-halo-color", "rgba(255,255,255,0.8)")
            put("text-halo-width", 1)
        }
        putJsonObject("layout") {
            put("text-field", "{name_en}")
            putJsonArray("text-font") {
                add("Roboto Condensed Italic")
            }
            put("text-max-width", 6.25)
            putJsonObject("text-size") {
                putJsonArray("stops") {
                    addJsonArray {
                        add(2)
                        add(11)
                    }
                    addJsonArray {
                        add(5)
                        add(17)
                    }
                }
            }
            put("text-transform", "none")
        }
    }
    // Layer: country_1
    addJsonObject {
        put("id", "country_1")
        put("type", "symbol")
        put("source", "openmaptiles")
        put("source-layer", "place")
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("rank")
                add(1)
            }
            addJsonArray {
                add("==")
                add("class")
                add("country")
            }
        }
        putJsonObject("paint") {
            put("text-color", "#334")
            put("text-halo-blur", 1)
            put("text-halo-color", "rgba(255,255,255,0.8)")
            put("text-halo-width", 1)
        }
        putJsonObject("layout") {
            put("text-field", "{name_en}")
            putJsonArray("text-font") {
                add("Roboto Condensed Italic")
            }
            put("text-max-width", 6.25)
            putJsonObject("text-size") {
                putJsonArray("stops") {
                    addJsonArray {
                        add(1)
                        add(11)
                    }
                    addJsonArray {
                        add(4)
                        add(17)
                    }
                }
            }
            put("text-transform", "none")
        }
    }
    // Layer: continent
    addJsonObject {
        put("id", "continent")
        put("type", "symbol")
        put("source", "openmaptiles")
        put("source-layer", "place")
        put("maxzoom", 1)
        putJsonArray("filter") {
            add("all")
            addJsonArray {
                add("==")
                add("class")
                add("continent")
            }
        }
        putJsonObject("paint") {
            put("text-color", "#633")
            put("text-halo-color", "rgba(255,255,255,0.7)")
            put("text-halo-width", 1)
        }
        putJsonObject("layout") {
            put("text-field", "{name_en}")
            putJsonArray("text-font") {
                add("Roboto Condensed Italic")
            }
            put("text-size", 13)
            put("text-transform", "uppercase")
            put("text-justify", "center")
        }
    }
    } // end putJsonArray("layers")
    } // end buildJsonObject

    return BaseStyle.Json(applyOverrides(baseStyle, overrides, symbolForegroundColor, symbolBackgroundColor))
}
