@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

package org.scottishtecharmy.soundscape.geoengine.mvttranslation

import org.scottishtecharmy.soundscape.geoengine.MAX_ZOOM_LEVEL
import org.scottishtecharmy.soundscape.geoengine.MIN_MAX_ZOOM_LEVEL
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.processTileFeatureCollection
import org.scottishtecharmy.soundscape.geoengine.utils.SuperCategoryId
import org.scottishtecharmy.soundscape.geoengine.utils.findLineIntersectionPoint
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.createCheapRuler
import org.scottishtecharmy.soundscape.geoengine.utils.superCategoryMap
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeoJsonObject
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiPoint
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon
import vector_tile.Tile


private fun addToStreetNumberMap(
    mvt: MvtFeature,
    streetNumberMap: HashMap<String, FeatureCollection>
) {
    if (mvt.housenumber != null) {
        // The tag-parsing loop above stores a parsed "street" tag in the dedicated `street`
        // field, not in `properties` (it's a special-cased key, never copied into the generic
        // properties map) - reading `properties["street"]` here always sees null, so every POI/
        // building feature that also carries a housenumber (e.g. a car park tagged with both a
        // name and an address) silently lands in the "null" (unknown street) bucket regardless
        // of whether it actually has a real street tag.
        val street = mvt.street
        val streetString = street.toString()
        if (!streetNumberMap.containsKey(streetString)) {
            streetNumberMap[streetString] = FeatureCollection()
        }
        streetNumberMap[streetString]?.addFeature(mvt)
    }
}

/**
 * The `transportation_name` layer also carries road junction (exit/interchange) nodes as POINT
 * features tagged `subclass=junction`, with `ref` as the junction number where the road is
 * numbered (e.g. motorway junction "2") and `name` as the interchange name (e.g. "Robroyston",
 * "Cousland Interchange"). These aren't just on motorways - primary/trunk/tertiary roads carry
 * named interchanges too - and they aren't duplicated anywhere else in the tile, so - unlike the
 * rest of `transportation_name` - we do need to turn them into proper Features here, for
 * travel-mode callouts like "at Junction 2" or "at Cousland Interchange".
 */
private fun extractHighwayJunctions(
    mvt: Tile,
    tileX: Int,
    tileY: Int,
    tileZoom: Int
): List<MvtFeature> {
    val junctions = mutableListOf<MvtFeature>()
    for (layer in mvt.layers) {
        if (layer.name != "transportation_name") continue
        for (feature in layer.features) {
            if (feature.type != Tile.GeomType.POINT) continue

            var firstInPair = true
            var key = ""
            var name: String? = null
            var ref: String? = null
            var featureClass: String? = null
            var featureSubClass: String? = null
            for (tag in feature.tags) {
                if (firstInPair) {
                    key = layer.keys[tag]
                } else {
                    val value = layer.values[tag].string_value
                    when (key) {
                        "name" -> name = value
                        "ref" -> ref = value
                        "class" -> featureClass = value
                        "subclass" -> featureSubClass = value
                    }
                }
                firstInPair = !firstInPair
            }

            if (featureSubClass != "junction") continue

            for (point in parseGeometry(true, feature.geometry)) {
                if (point.isEmpty()) continue
                for (coordinate in convertGeometry(tileX, tileY, tileZoom, point)) {
                    val junction = MvtFeature()
                    junction.geometry = Point(coordinate)
                    junction.osmId = feature.id ?: 0L
                    junction.name = name
                    junction.ref = ref
                    junction.featureType = "highway"
                    junction.featureValue = "highway_junction"
                    if (featureClass != null) junction.setProperty("class", featureClass)
                    junctions.add(junction)
                }
            }
        }
    }
    return junctions
}

/**
 * The `water` layer's named polygons - the pmtiles pipeline now passes every water polygon's OSM
 * `name` through (previously only the separate `water_name` layer carried names). This is what
 * makes a firth/bay/strait crossing detectable at all: OSM tags those as `natural=bay`/
 * `natural=strait`, not as a `waterway` line, so extractCrossings' river/canal-only logic never
 * sees them - see AutoCallout's water-crossing proximity check, which searches this collection
 * for a containing/nearby named polygon while travelling on a `brunnel=bridge` way, instead of
 * trying to compute a specific crossing point (unreliable for a body wider than an MVT tile -
 * see the comment on TreeId.NAMED_WATER_POLYGONS).
 */
private fun extractNamedWaterPolygons(
    mvt: Tile,
    tileX: Int,
    tileY: Int,
    tileZoom: Int
): List<MvtFeature> {
    val waterFeatures = mutableListOf<MvtFeature>()
    for (layer in mvt.layers) {
        if (layer.name != "water") continue
        for (feature in layer.features) {
            if (feature.type != Tile.GeomType.POLYGON && feature.type != Tile.GeomType.LINESTRING) {
                continue
            }

            var firstInPair = true
            var key = ""
            var name: String? = null
            for (tag in feature.tags) {
                if (firstInPair) {
                    key = layer.keys[tag]
                } else if (key == "name") {
                    name = layer.values[tag].string_value
                }
                firstInPair = !firstInPair
            }
            if (name.isNullOrEmpty()) continue

            if (feature.type == Tile.GeomType.LINESTRING) {
                // A strait/sound (e.g. "Afon Menai / Menai Strait") is sometimes represented as a
                // named centerline rather than a polygon - the crossing check in AutoCallout's
                // wayCrossingInfo falls back to nearest-feature search for exactly this case, so a
                // LineString works there just as well as a polygon.
                for (line in parseGeometry(true, feature.geometry)) {
                    if (line.isEmpty()) continue
                    val coordinates = convertGeometry(tileX, tileY, tileZoom, line)
                    if (coordinates.size < 2) continue
                    val waterFeature = MvtFeature()
                    waterFeature.geometry = LineString(ArrayList(coordinates))
                    waterFeature.osmId = feature.id ?: 0L
                    waterFeature.name = name
                    waterFeature.featureType = "water"
                    waterFeature.featureValue = "named_water_polygon"
                    waterFeatures.add(waterFeature)
                }
                continue
            }

            var lastClockwisePolygon: Polygon? = null
            for (ring in parseGeometry(false, feature.geometry)) {
                if (ring.isEmpty()) continue
                if (areCoordinatesClockwise(ring)) {
                    lastClockwisePolygon = Polygon(convertGeometry(tileX, tileY, tileZoom, ring))
                    val waterFeature = MvtFeature()
                    waterFeature.geometry = lastClockwisePolygon
                    waterFeature.osmId = feature.id ?: 0L
                    waterFeature.name = name
                    waterFeature.featureType = "water"
                    waterFeature.featureValue = "named_water_polygon"
                    waterFeatures.add(waterFeature)
                } else {
                    lastClockwisePolygon?.addInteriorRing(convertGeometry(tileX, tileY, tileZoom, ring))
                }
            }
        }
    }
    return waterFeatures
}

// OpenMapTiles waterway `class` values, in roughly descending size/significance: river, canal,
// stream, drain, ditch. A stream is often little more than a culverted ditch under a road - not
// really a landmark - so only the two biggest classes are worth a callout. This can't be inferred
// from how the crossing happens (e.g. a stream can still pass under a real bridge, not just a
// culvert), so it's judged on the waterway's own class rather than its brunnel value.
private val significantWaterwayClasses = setOf("river", "canal")

// The `waterway` classes worth naming an adjacent path after. Deliberately wider than
// significantWaterwayClasses above, because the two sets answer different questions. That one asks
// "is crossing this worth announcing?", where a culverted stream under a road is not. This one asks
// "does following this for hundreds of metres identify the path?", and a burn very much does - the
// path at 55.931961,-4.305300 is known by the Allander Water it runs beside. Drains and ditches
// stay out: they're field drainage, not a landmark anyone navigates by.
private val nameableWaterwayClasses = setOf("river", "canal", "stream")

/**
 * The named lines of the `waterway` layer - rivers, canals and burns - kept as features in their
 * own right so that an un-named path which follows one for its whole length can be described by it
 * ("Path next to Allander Water") - see addWaterAdjacency in WayNaming.kt.
 *
 * extractCrossings below also reads this layer, but only to work out which road crosses which
 * waterway; it discards the waterway geometry afterwards, and filters to river/canal only. Neither
 * is what naming needs, hence the separate pass.
 *
 * Segments carrying a `brunnel` tag are skipped: those are the culverted/tunnelled stretches, and a
 * path isn't meaningfully "next to" a watercourse that's buried under it at that point.
 */
private fun extractNamedWaterways(
    mvt: Tile,
    tileX: Int,
    tileY: Int,
    tileZoom: Int
): List<MvtFeature> {
    val waterways = mutableListOf<MvtFeature>()
    for (layer in mvt.layers) {
        if (layer.name != "waterway") continue
        for (feature in layer.features) {
            if (feature.type != Tile.GeomType.LINESTRING) continue

            var firstInPair = true
            var key = ""
            var name: String? = null
            var featureClass: String? = null
            var brunnel: String? = null
            for (tag in feature.tags) {
                if (firstInPair) {
                    key = layer.keys[tag]
                } else {
                    val value = layer.values[tag].string_value
                    when (key) {
                        "name" -> name = value
                        "class" -> featureClass = value
                        "brunnel" -> brunnel = value
                    }
                }
                firstInPair = !firstInPair
            }
            if (name.isNullOrEmpty()) continue
            if (featureClass !in nameableWaterwayClasses) continue
            if (brunnel != null) continue

            for (line in parseGeometry(true, feature.geometry)) {
                if (line.isEmpty()) continue
                val coordinates = convertGeometry(tileX, tileY, tileZoom, line)
                if (coordinates.size < 2) continue
                val waterway = MvtFeature()
                waterway.geometry = LineString(ArrayList(coordinates))
                waterway.osmId = feature.id ?: 0L
                waterway.name = name
                waterway.featureType = "waterway"
                waterway.featureValue = "named_waterway"
                waterway.featureClass = featureClass
                waterways.add(waterway)
            }
        }
    }
    return waterways
}

// OpenMapTiles `transportation` classes that are a railway rather than a road - see the equivalent
// check in MvtToGeoJson's main Way-building loop.
private val railwayClasses = setOf("rail", "transit")

/**
 * A `subclass=subway` line - the whole Glasgow Subway, for example - is excluded from
 * TreeId.TRANSIT, the network railMapMatchFilter matches GPS fixes against (see GeoEngine.kt).
 *
 * GPS is 2D: it can't tell a road apart from a railway running directly beneath it. Where a road
 * sits right above a buried line for a sustained stretch - e.g. Byres Road above the Glasgow
 * Subway, around 55.872965,-4.296419 - the line's horizontal projection coincides with the road
 * closely enough, for long enough, to build up the same kind of sustained frechetQueue history as
 * a genuine train ride (see MapMatchFilter.isMatchConfident), which would wrongly flip
 * UserGeometry.probablyOnTrain for a driver or pedestrian who was never anywhere near a train. A
 * brief level crossing is already handled by isMatchConfident's history requirement; a subway
 * running underneath for hundreds of metres is not "brief". A subway line is underground for its
 * entire length, so there's nothing to be gained by keeping it: a rider on one has no usable GPS
 * fix to match with anyway.
 *
 * A `brunnel=tunnel` heavy-rail segment is a different case and is deliberately *not* excluded
 * here, even though it poses exactly the same road-above-the-line hazard (Kent Road sits directly
 * over the North Clyde Line at Charing Cross). Unlike a subway, such a segment is a buried stretch
 * of an otherwise surface line, and recordings show GPS keeps producing genuinely good fixes for a
 * couple of hundred metres past the tunnel mouth - tracking the tunnel centreline to within 8m
 * while reporting 6-26m accuracy. Excluding the tunnel used to leave the road overhead as the only
 * thing left to match against, so a train through the Charing Cross tunnel was announced as
 * "Traveling east along Kent Road". Keeping the tunnel matchable fixes that; the road-above hazard
 * is handled instead by RailMatchArbiter, which lets a tunnel match sustain a train lock but never
 * acquire one.
 *
 * Keeping tunnels in also means the tunnel line reaches WayGenerator.addLine, so the node at the
 * tunnel mouth is counted twice and an Intersection is created there. That's what joins the
 * surface and tunnel Ways into one connected network, which MapMatchFilter's reachability check
 * needs in order to follow a train underground.
 */
private fun isUnmatchableRailway(subClass: String?): Boolean {
    return subClass == "subway"
}

private class NamedLine(val name: String?, val featureClass: String?, val coordinates: List<LngLatAlt>)

// Every non-railway "transportation" LineString in the tile, brunnel-tagged or not - broader than
// just the brunnel-tagged ones, since a self-tagged waterway culvert (case a below) needs to find
// which road crosses it, and that road isn't necessarily itself brunnel-tagged.
private class RoadLine(val osmId: Long, val brunnel: String?, val coordinates: List<LngLatAlt>)

// A waterway segment self-tagged brunnel=tunnel/bridge/ford (case a below) - deferred until
// roadLines is fully populated, since resolving which road crosses it needs the complete list.
private class PendingCulvert(val coordinates: List<LngLatAlt>, val name: String, val brunnel: String)

// The crossing info to attach to the crossing road's Way, keyed by that road's osmId - see
// extractCrossings. type is "waterway" (railway crossings are resolved separately, after tile
// stitching - see GridState.attachRailwayCrossings).
//
// `position` is always the *user's* relationship to the named structure - "over" or "under" - not
// a raw OSM brunnel value. That distinction matters because the brunnel evidence arrives from
// either side and the two invert each other: a road tagged brunnel=bridge is over the river,
// whereas a waterway tagged brunnel=bridge is an aqueduct, so the road below it goes under.
// Storing the OSM tag instead of the resolved relationship is what made every aqueduct announce
// "Crossing the Union Canal" while the user was driving underneath it.
//
// `point` is where the two lines actually cross. The road below a structure carries no brunnel of
// its own, so OSM never splits it there and it can run for kilometres either side; the callout
// needs the real crossing point to know when to fire (see AutoCallout.crossingToAnnounce).
private data class CrossingInfo(
    val type: String,
    val name: String?,
    val position: String,
    val point: LngLatAlt?
)

/**
 * Crossings of a named river/canal while travelling by car/bus or on foot - a major navigation
 * point worth a callout in its own right, e.g. "Passing over Allander Water"/"Passing over the River
 * Leven". The road/path doing the crossing can be any highway class, including footway/path, so a
 * pedestrian on a footbridge gets the same callout as a vehicle on a road bridge at the same spot.
 *
 * Returns the crossing info keyed by the OSM id of the crossing road/path, ready to be attached
 * directly to that road's Way(s) (see the `crossingsByOsmId[id]?.let { ... }` call in
 * vectorTileToGeoJson, which propagates it into Way.properties) - this lets travel-mode callouts
 * read it straight off userGeometry.mapMatchedWay with no further search needed.
 *
 * A small stream culverted under a road is already split at the crossing point and tagged there
 * (`brunnel=tunnel`, occasionally `bridge`/`ford`) - the tagged segment itself IS the crossing,
 * with its own `name`/`class` already attached, no line-to-line intersection needed to find the
 * crossing point itself. But unlike the old point-based representation, we now also need to know
 * *which road* crosses it - see findCrossingRoadOsmId, which first tries genuine geometric
 * intersection against every road in the tile, then falls back to the nearest road within a small
 * tolerance, since independently digitised waterway/road geometry doesn't always align exactly. A
 * major river crossed by a real bridge is different: the river's own LineString is never split or
 * tagged at the crossing, only the road carries `brunnel=bridge` - only "river"/"canal" class
 * waterways are worth a callout (see significantWaterwayClasses).
 *
 * Railway crossings are NOT handled here, even though they need the same kind of geometric
 * road/rail intersection test - see GridState.attachRailwayCrossings for why: unlike a short
 * culverted stream, a road and a railway can straddle an MVT tile boundary right at the point
 * they cross, so detecting the crossing needs the already tile-stitched road/rail geometry that's
 * only available after a whole grid's tiles have been merged, not from a single tile in isolation
 * here.
 */
private fun extractCrossings(
    mvt: Tile,
    tileX: Int,
    tileY: Int,
    tileZoom: Int
): HashMap<Long, CrossingInfo> {
    val crossingsByOsmId = HashMap<Long, CrossingInfo>()
    val namedWaterways = mutableListOf<NamedLine>()
    val roadLines = mutableListOf<RoadLine>()
    val pendingCulverts = mutableListOf<PendingCulvert>()

    for (layer in mvt.layers) {
        if (layer.name != "waterway" && layer.name != "transportation") continue
        for (feature in layer.features) {
            if (feature.type != Tile.GeomType.LINESTRING) continue

            var firstInPair = true
            var key = ""
            var name: String? = null
            var featureClass: String? = null
            var brunnel: String? = null
            for (tag in feature.tags) {
                if (firstInPair) {
                    key = layer.keys[tag]
                } else {
                    val value = layer.values[tag].string_value
                    when (key) {
                        "name" -> name = value
                        "class" -> featureClass = value
                        "brunnel" -> brunnel = value
                    }
                }
                firstInPair = !firstInPair
            }

            if (layer.name == "waterway") {
                if (brunnel != null && !name.isNullOrEmpty() && featureClass in significantWaterwayClasses) {
                    for (line in parseGeometry(true, feature.geometry)) {
                        if (line.isEmpty()) continue
                        val coordinates = convertGeometry(tileX, tileY, tileZoom, line)
                        if (coordinates.isEmpty()) continue
                        if (brunnel != "bridge" && !isRoadWidthSpan(coordinates, line)) continue
                        pendingCulverts.add(PendingCulvert(coordinates, name, brunnel))
                    }
                }
                // Also keep every significant named waterway (regardless of brunnel) to check
                // against bridged roads below - a major river is rarely tagged brunnel on its own
                // LineString.
                if (!name.isNullOrEmpty() && featureClass in significantWaterwayClasses) {
                    for (line in parseGeometry(true, feature.geometry)) {
                        if (line.isEmpty()) continue
                        val coordinates = convertGeometry(tileX, tileY, tileZoom, line)
                        if (coordinates.size >= 2) {
                            namedWaterways.add(NamedLine(name, featureClass, coordinates))
                        }
                    }
                }
            } else if (featureClass in railwayClasses) {
                // Railways are excluded from roadLines below (they're not a road/path a waterway
                // could be crossed by) - their own crossings are handled by
                // GridState.attachRailwayCrossings instead, see the class doc comment above.
            } else {
                for (line in parseGeometry(true, feature.geometry)) {
                    if (line.isEmpty()) continue
                    val coordinates = convertGeometry(tileX, tileY, tileZoom, line)
                    if (coordinates.size >= 2) {
                        roadLines.add(RoadLine(feature.id ?: 0L, brunnel, coordinates))
                    }
                }
            }
        }
    }

    // Case (a): the tagged culvert span IS the crossing, but carries no reference to which road
    // crosses it.
    for (culvert in pendingCulverts) {
        // The brunnel here belongs to the *waterway*, so it inverts: an aqueduct (brunnel=bridge)
        // carries the water over the road, meaning the user passes under it, while a culvert or
        // ford takes the water beneath the road, meaning the user passes over it.
        val position = if (culvert.brunnel == "bridge") "under" else "over"
        for (road in findCrossingRoads(culvert.coordinates, roadLines)) {
            crossingsByOsmId[road.osmId] =
                CrossingInfo("waterway", culvert.name, position, road.point)
        }
    }

    // Case (b): a named river/canal geometrically crossed by a road tagged brunnel=bridge.
    for (waterway in namedWaterways) {
        for (road in roadLines) {
            // Here the brunnel belongs to the *road*, so it reads directly: a bridge carries the
            // user over the water, a tunnel takes them under it. Note that the widest rivers are
            // mapped as water polygons rather than waterway lines, so a road tunnel under one of
            // those is picked up by AutoCallout.wayCrossingInfo's fallback instead of here.
            val position = when (road.brunnel) {
                "bridge" -> "over"
                "tunnel" -> "under"
                else -> continue
            }
            val point =
                findLineIntersectionPoint(waterway.coordinates, road.coordinates) ?: continue
            // A road crossing two different named features (e.g. a viaduct over both a river and
            // a railway) is rare enough that last-write-wins here is fine - Way.properties is a
            // flat map, so supporting more than one crossing per Way isn't worth the complexity.
            crossingsByOsmId[road.osmId] =
                CrossingInfo("waterway", waterway.name, position, point)
        }
    }

    return crossingsByOsmId
}

// Tile data isn't always perfectly aligned (the waterway and the road above it are digitised
// independently), so a self-tagged culvert's short span doesn't always geometrically intersect the
// road exactly - this is the fallback for that. Runs once per tile at parse time, not on any hot
// path, so a full scan of roadLines per culvert (normally only a handful per tile) is fine.
private const val CULVERT_ROAD_MATCH_TOLERANCE_METRES = 15.0

// A waterway only counts as being crossed where it dips beneath the road - a culvert or a ford,
// something road-width. Beyond that it's a genuine tunnel, and a canal or river tens of metres
// underground isn't a landmark anyone crosses: the Union Canal runs 649m through the Falkirk
// Tunnel with Slamannan Road passing over the top, which has nothing to do with the canal. The
// Allander Water's culvert, by contrast, is a 15m span directly under the road.
//
// This deliberately doesn't apply to brunnel=bridge: an aqueduct carries the water overhead, so
// however long it is, the road beneath genuinely passes under it.
private const val CULVERT_MAX_SPAN_METRES = 100.0

/**
 * Whether a brunnel-tagged waterway span is short enough to be the water passing under a road
 * rather than a tunnel in its own right.
 *
 * The tile-space geometry is needed as well as the length: a long tunnel is clipped at the tile
 * boundary, so the far side of it can arrive here as a short-looking remnant (the Falkirk Tunnel
 * measures 607m in one tile and 89m in the next). A span that runs off the edge of the tile
 * continues somewhere we can't see, so it's never treated as a road-width crossing.
 */
private fun isRoadWidthSpan(coordinates: List<LngLatAlt>, tileLine: List<Pair<Int, Int>>): Boolean {
    if (tileLine.any { pointIsOffTile(it.first, it.second) }) return false
    if (coordinates.size < 2) return true
    val ruler = coordinates.first().createCheapRuler()
    return ruler.lineLength(LineString(ArrayList(coordinates))) <= CULVERT_MAX_SPAN_METRES
}

private class CrossingRoad(val osmId: Long, val point: LngLatAlt)

/**
 * Every road the culvert/aqueduct span genuinely crosses, not just the first: one structure
 * regularly spans a dual carriageway plus its slip roads (the Union Canal aqueduct crosses four
 * separate ways over the A720), and tagging only the first of them makes the callout depend on
 * which direction the user happens to be travelling.
 *
 * The nearest-line fallback stays single-result and only runs when nothing genuinely intersects -
 * it exists to cover mis-aligned digitising, so running it alongside real intersections would
 * start attributing the structure to roads on the far side of the block.
 */
private fun findCrossingRoads(
    culvertCoordinates: List<LngLatAlt>,
    roadLines: List<RoadLine>
): List<CrossingRoad> {
    val matches = mutableListOf<CrossingRoad>()
    for (road in roadLines) {
        findLineIntersectionPoint(culvertCoordinates, road.coordinates)?.let {
            matches.add(CrossingRoad(road.osmId, it))
        }
    }
    if (matches.isNotEmpty()) return matches

    val midpoint = culvertCoordinates[culvertCoordinates.size / 2]
    val ruler = midpoint.createCheapRuler()
    var bestOsmId: Long? = null
    var bestDistance = CULVERT_ROAD_MATCH_TOLERANCE_METRES
    for (road in roadLines) {
        val nearest = ruler.distanceToLineString(midpoint, LineString(ArrayList(road.coordinates)))
        if (nearest.distance < bestDistance) {
            bestDistance = nearest.distance
            bestOsmId = road.osmId
        }
    }
    return bestOsmId?.let { listOf(CrossingRoad(it, midpoint)) } ?: emptyList()
}

/**
 * vectorTileToGeoJson generates a GeoJSON FeatureCollection from a Mapbox Vector Tile.
 * @param tileX is the x coordinate of the tile
 * @param tileY is the y coordinate of the tile
 * @param mvt is the Tile which has been decoded from the protobuf on its way into the application
 * @param cropPoints is a flag to indicate whether or not crop points to be within the tile
 * @param tileZoom defaults to ZOOM_LEVEL but can be forced to 15 to run unit tests even when the
 * backend is not configured to be protomaps.
 *
 * There are really two parts of this function:
 *
 * 1. Iterating over the features in each layers and turning their tags and geometries into GeoJSON.
 * This is done by 'simply' following the [MVT specification](https://github.com/mapbox/vector-tile-spec/tree/master/2.1).
 * 2. Adding some locally calculated metadata e.g. the location of intersections, and adding the
 * ability to knit together lines that cross tile boundaries.
 *
 * The input tile geometries are all tile relative and using `tileX` and `tileY` we turn those into
 * latitudes and longitudes for the GeoJSON. Although the locally calculated metadata could be done
 * as a second pass after the initial parsing has been done, it's much more efficient to do them in
 * a single pass. By doing that the geometries are still tile relative and much easier to handle
 * than latitudes and longitudes.
 *
 * The vector tiles come from a protomaps server which is hosting a map file that we generate using
 * `planetiler`. A stock running of `planetiler` is missing some data that we need, so we disable
 * simplification at the maximum zoom level (which is what we're using here) and we also force the
 * addition of a Feature id on all Features within the transportation layer. This allows us to more
 * easily identify roads and paths for intersection handling. We also add a name tag to every
 * feature in the transportation layer. This ensures that we always have an OSM id and a name where
 * there's one available. The `transportation_name` layer is left unused and so its merging of
 * lines to improve the graphical UI is untouched. Two further tags are carried through that aren't
 * in the stock schema at all: `ref`, and `tunnel_name` from OSM's `tunnel:name` - the latter being
 * the only way to name a tunnel, since a tunnelled way's own `name` is the road or line running
 * through it rather than the tunnel (see buildCalloutForTunnel in AutoCallout.kt).
 * Note that these changes are  only in our builds and won't be in upstream `planetiler`. None of
 * these changes should affect the graphical rendering of the tiles which is important as we're
 * using the tiles for that too.
 *
 * This means that we only look at 2 layers which are defined here https://openmaptiles.org/schema/:
 *
 * 1. `transportation` contains all footways/roads etc. including named and unnamed and so is a
 * superset of `transportation_name`.  We use the lines from this and along with the names which we
 * added in our custom map.
 * 2. `poi` contains points of interest.
 *
 *
 *
 * Future plans:
 * A Feature is generated for every geometry within a line. There are multiple geometries when a
 * line goes off tile and then comes back on again. All Features for the line have the same contents
 * other than their geometry. The intersections only contain IntersectionDetails which contains
 *
 *     val name : String,
 *     val type : String,
 *     val subClass : String,
 *     val brunnel : String,
 *     val id : Double,
 *     var lineEnd : Boolean
 *
 * which is all that's required for determining if it classifies as an intersection, otherwise it's
 * just a meeting of two segments. When an intersection is created, it has a location and a list of
 * OSM ids. What we really want is:
 *
 *  - Every line between intersections can be a list of Features
 *  - No Feature contains more than 2 intersections i.e. one at each end. Any line which has more
 *  than one intersection is split into multiple Features.
 *
 *  If I'm at an intersection, the Features that connect to it should all be traversable to get to
 *  the next intersection and either the first of last of their string list coordinates should be
 *  the current intersection. The intersection should never be part way along a string - as it
 *  should have been split.
 *
 *  class FeatureMetadata {
 *      // The contents of properties/foreign, but not in a hash map, instead stored in sensible
 *      // format
 *  }
 *
 *  class Way {
 *      val segment: Feature                    // List of Features that make up the way (often just 1)
 *      val length: Double                      // We could easily calculate this from the segments.
 *                                              // It could be useful for context, or for navigation.
 *      val nextIntersection: Intersection      // Link to the intersection at the other end of the
 *                                              // segments
 *
 *      fun getMetadata() : FeatureMetadata     // Returns the metadata for the way, taken from the
 *                                              // first segment. Anything needing OSM ids needs to
 *                                              // be traversing the segments anyway.
 *  }
 *
 *  Should segments contain a List<LineString> rather than Feature and have all the data for Feature
 *  inside the Way instead? If a road is extended with a new OSM id then this would be a problem as
 *  each segment would have a different OSM id. We could merge the segments in the list if the data
 *  is the same, but unsure if that helps much.
 *
 *  class Intersection {
 *      val members: List<Ways>                 // Ways that make up this intersection
 *      val name: String                        // Name of the intersection
 *      val location: LngLatAlt                 // Location of the intersection
 *      val type: Enum                          // Type of intersection:
 *                                              //  REGULAR - a real intersection like we hav now
 *                                              //  JOINER - joins two segments together, skip over
 *                                              //  TILE_EDGE - joins two tiles together, skip over
 *  }
 *
 *  Tile joining. We should have special tile joining intersections. These are like normal
 *  intersections except they are marked to ignore when traversing to the next intersection. The
 *  data in the Features being joined can be slightly tweaked - just moving the coordinates so that
 *  they match i.e. avoiding the 15cm long roads that we currently use to join tiles. When the tile
 *  grid is changed, we can throw away all of these tile joining intersections and recalculate new
 *  ones (some may still be required, so this behaviour could be improved).
 *
 *  Street Preview - this should remove the searching and extending of road lines to find the next
 *  intersection. We should just be able to:
 *  1. Jump immediately to the next intersection or the end of the line (dead-end or tile boundary
 *  that hasn't been joined)
 *  2. If it's a tile joiner, jump through it to the next intersection.
 *  3. Creating the list of ways will be much easier
 *
 *  Name confection - jump through the nextIntersection until we have a REGULAR one and pick a name
 *  from there if there is one.
 *
 *  Routing - We could do routing between intersections fairly easily with all of this data. Instead
 *  of exploding every line into segments as per `explodeLineString` and using every line node,
 *  we can use the intersections as the nodes instead. We can pre-calculate their lengths and store
 *  it in the Way (NOTE: calculating the distance using the tile x/y integer coordinates is likely
 *  accurate enough and more efficient than full blown LngLat calculation). The routing algorithm
 *  can then use the Ways with their length as weights which should be fairly efficient. Most of the
 *  time the user will not be at an intersection and neither will the destination be. But we can
 *  do the calculation from either end of the current Way that we're on and then figure out which
 *  is the shortest route when including the distance to the intersection.
 *
 *  NearestRoad - This data means that we could do a better job via something like this:
 *  https://medium.com/@jabrioussama1/how-to-match-gps-positions-to-roads-b6b13a5e6c20
 *  A good introduction video here https://www.youtube.com/watch?v=ChtumoDfZXI
 *  We could keep a short history of GPS locations with their hidden markov states (nearest roads)
 *  and run viterbi on them to find the most likely path that we're on. This relies on the routing
 *  algorithm to give the shortest navigable route between hidden states which is then compared
 *  with the haversine distance. https://github.com/bmwcarit/offline-map-matching/tree/master has
 *  an example implementation.
 *
 *
 *  Implementation - create Features for lines as we do now, but add them to a list inside the
 *  intersection detection class (new addFeature function). The original addLine only has to
 *  increment a node use count, no other details required.
 *  Inside generateIntersections, first traverse every line that was added and generate a new
 *  segment Feature at every intersection that we hit. Add these to Ways as we go. Intersections are spotted using the
 *  coordinate key (x + shr(y)). Put those features in two HashMaps a 'start' an 'end' one, again
 *  keyed by the coordinate key. Once we've traversed all of the lines we should have a Way for
 *  every segment between intersections. Now we generate the intersections and add the Ways directly
 *  to them. Let's do this in a separate class for now so that we can test it.
 */
fun vectorTileToGeoJson(
    tileX: Int,
    tileY: Int,
    mvt: Tile,
    intersectionMap: HashMap<LngLatAlt, Intersection>,
    streetNumberMap: HashMap<String, FeatureCollection>,
    cropPoints: Boolean = true,
    tileZoom: Int = MAX_ZOOM_LEVEL,
    transitIntersectionMap: HashMap<LngLatAlt, Intersection> = hashMapOf()
): Array<FeatureCollection> {

    val collection = FeatureCollection()
    val wayGenerator = WayGenerator()
    val transitGenerator = WayGenerator(transit = true)
    val entranceMatching = EntranceMatching()

    // The main TileGrid is at the MAX_ZOOM_LEVEL and we parse transportation, poi and building
    // layers. However, we also create TileGrids at lower zoom levels to get towns, cities etc. from
    // the place layer.
    val layerIds = if (tileZoom >= MIN_MAX_ZOOM_LEVEL) {
        arrayOf("transportation", "poi", "building", "housenumber")
    } else {
        arrayOf("place")
    }

    val crossingsByOsmId =
        if (tileZoom >= MIN_MAX_ZOOM_LEVEL) extractCrossings(mvt, tileX, tileY, tileZoom) else hashMapOf()
    if (tileZoom >= MIN_MAX_ZOOM_LEVEL) {
        for (junction in extractHighwayJunctions(mvt, tileX, tileY, tileZoom)) {
            collection.addFeature(junction)
        }
        for (waterPolygon in extractNamedWaterPolygons(mvt, tileX, tileY, tileZoom)) {
            collection.addFeature(waterPolygon)
        }
        for (waterway in extractNamedWaterways(mvt, tileX, tileY, tileZoom)) {
            collection.addFeature(waterway)
        }
    }

    // POI can have duplicate entries for polygons and points and also duplicates in the Buildings
    // layer we de-duplicate them with these maps.
    val mapPolygonFeatures: HashMap<Long, MutableList<Feature>> = hashMapOf()
    val mapBuildingFeatures: HashMap<Long, Feature> = hashMapOf()
    val mapPointFeatures: HashMap<Long, Feature> = hashMapOf()

    for (layer in mvt.layers) {
        if (!layerIds.contains(layer.name)) {
            continue
        }
        //println("Process layer: " + layer.name)

        val mapInterpolatedNodes: HashMap<Long, Feature> = hashMapOf()
        for (feature in layer.features) {

            var entrance = false
            val id = feature.id ?: 0L
            var name: String? = null
            var ref: String? = null
            var featureClass: String? = null
            var featureSubClass: String? = null
            var housenumber: String? = null
            var street: String? = null

            // Convert coordinates to GeoJSON. This is where we find out how many features
            // we're actually dealing with as there can be multiple features that have the
            // same properties.
            check(feature.type != null)
            val listOfGeometries = mutableListOf<GeoJsonObject>()

            // Parse tags
            var firstInPair = true
            var key = ""
            var value: Any? = null
            var properties: HashMap<String, Any?>? = null
            for (tag in feature.tags) {
                if (firstInPair)
                    key = layer.keys[tag]
                else {
                    val raw = layer.values[tag]
                    if (raw.bool_value != null)
                        value = raw.bool_value
                    else if (raw.int_value != null)
                        value = raw.int_value
                    else if (raw.sint_value != null)
                        value = raw.sint_value
                    else if (raw.float_value != null)
                        value = raw.double_value
                    else if (raw.double_value != null)
                        value = raw.float_value
                    else if (raw.string_value != null)
                        value = raw.string_value
                    else if (raw.uint_value != null)
                        value = raw.uint_value
                }

                if (!firstInPair) {
                    when (key) {
                        "name" -> name = value.toString()
                        "ref" -> ref = value.toString()
                        "class" -> featureClass = value.toString()
                        "subclass" -> featureSubClass = value.toString()
                        "housenumber" -> housenumber = value.toString()
                        "street" -> street = value.toString()
                        else -> {
                            if (properties == null) {
                                properties = HashMap()
                            }
                            properties[key] = value
                        }
                    }
                    firstInPair = true
                } else
                    firstInPair = false
            }

            if (layer.name == "building") {
                // Check that we have a name, otherwise we're not interested
                if (name == null)
                    continue
            }

            // Parse geometries
            when (feature.type) {
                Tile.GeomType.POLYGON -> {
                    val polygons = parseGeometry(
                        false,
                        feature.geometry
                    )

                    // If all of the polygon points are outside the tile, then we can immediately
                    // discard it
                    var allOutside = true
                    for (polygon in polygons) {
                        for (point in polygon) {
                            if (!pointIsOffTile(point.first, point.second)) {
                                allOutside = false
                                break
                            }
                        }
                        if (!allOutside)
                            break
                    }
                    if (allOutside)
                        continue

                    // The polygon geometry encoding has some subtleties:
                    //
                    // A Polygon in MVT can consist of multiple polygons. If each polygon has a
                    // positive winding order then they are all individual polygons. If any have
                    // negative winding order, then they make up a MultiPolygon along with the last
                    // positive winding order Polygon that was found.
                    //
                    // So the MVT polygon can intersperse a number of Polygons and MultiPolygons and
                    // some care is required when decoding them.
                    //
                    var lastClockwisePolygon: Polygon? = null
                    for (polygon in polygons) {

                        if (areCoordinatesClockwise(polygon)) {
                            // We have an exterior ring, so create a new Polygon
                            lastClockwisePolygon = Polygon(
                                convertGeometry(
                                    tileX,
                                    tileY,
                                    tileZoom,
                                    polygon
                                )
                            )
                            listOfGeometries.add(lastClockwisePolygon)
                        } else {
                            // We have an inner ring, add it to the last polygon
                            if (lastClockwisePolygon != null) {
                                lastClockwisePolygon.addInteriorRing(
                                    convertGeometry(
                                        tileX,
                                        tileY,
                                        tileZoom,
                                        polygon
                                    )
                                )
                            } else {
                                println("Interior ring without any exterior ring!")
                            }
                        }

                        if (layer.name == "poi" || layer.name == "building") {
                            if (name != null) {
                                val entranceDetails = EntranceDetails(
                                    name,
                                    null,
                                    properties?.get("layer")?.toString(),
                                    null,
                                    true,
                                    id
                                )
                                entranceMatching.addGeometry(polygon, entranceDetails)
                            }
                        }
                    }
                }

                Tile.GeomType.POINT -> {
                    val points =
                        parseGeometry(cropPoints, feature.geometry)
                    for (point in points) {
                        if (point.isNotEmpty()) {
                            val coordinates = convertGeometry(tileX, tileY, tileZoom, point)
                            for (coordinate in coordinates) {
                                listOfGeometries.add(
                                    Point(coordinate)
                                )

                                if (featureClass == "entrance") {
                                    // If the access is set to no, then don't add the entrance
                                    if ((properties?.get("access") != "no")) {

                                        // Add the entrance
                                        val entranceDetails = EntranceDetails(
                                            name,
                                            featureSubClass,
                                            properties?.get("layer")?.toString(),
                                            properties,
                                            false,
                                            id
                                        )
                                        entranceMatching.addGeometry(point, entranceDetails)
                                        entrance = true
                                    }
                                }
                            }
                        }
                    }
                }

                Tile.GeomType.LINESTRING -> {
                    val lines = parseGeometry(
                        false,
                        feature.geometry
                    )

                    if (layer.name == "transportation") {
                        for (line in lines) {
                            if (id == 0L) {
                                println("Feature ID is zero for $name")
                            }
                            if ((featureClass == "transit") || (featureClass == "rail")) {
                                if (!isUnmatchableRailway(featureSubClass)) {
                                    transitGenerator.addLine(line)
                                }
                            } else {
                                wayGenerator.addLine(line)
                            }
                            val interpolatedNodes: MutableList<LngLatAlt> = mutableListOf()
                            val clippedLines = convertGeometryAndClipLineToTile(
                                tileX,
                                tileY,
                                tileZoom,
                                line,
                                interpolatedNodes
                            )
                            for (clippedLine in clippedLines) {
                                listOfGeometries.add(clippedLine)
                            }

                            if (interpolatedNodes.isNotEmpty()) {
                                // If the line went off the edge of the tile then we will have
                                // generated an interpolated node at the tile edge. We store this in
                                // a Feature which is a list of those nodes for this OSM id. It may
                                // just be a single point, or the line may have gone on and off the
                                // tile multiple times.
                                if (mapInterpolatedNodes.containsKey(id)) {
                                    // If we've already got this OSM id, we want to extend it with
                                    // the new points
                                    val currentLine =
                                        mapInterpolatedNodes[id]?.geometry as MultiPoint
                                    for (node in interpolatedNodes) {
                                        currentLine.coordinates.add(node)
                                    }
                                } else {
                                    val interpolatedFeature = MvtFeature()
                                    interpolatedFeature.geometry =
                                        MultiPoint(ArrayList(interpolatedNodes))
                                    interpolatedFeature.properties = hashMapOf()
                                    interpolatedFeature.featureClass = "edgePoint"
                                    interpolatedFeature.osmId = id
                                    mapInterpolatedNodes[id] = interpolatedFeature
                                }
                            }
                        }
                    }
                }

                // Assert on all other geometry enum values
                Tile.GeomType.UNKNOWN -> {
                    check(false) { "Unexpected geometry type: ${feature.type}" }
                }
            }

            if (entrance) {
                // We've added the entrance to our matching code and so we don't need to add it as
                // as feature now
                continue
            }

            for (geometry in listOfGeometries) {
                // And map the tags
                val geoFeature = MvtFeature()
                geoFeature.geometry = geometry
                geoFeature.osmId = id
                geoFeature.housenumber = housenumber
                if (layer.name == "housenumber") {
                    // We store house numbers in a FeatureCollection per named street
                    // TODO: What if there's no street? That's an OSM error, but there are plenty of
                    //  cases where it happens.
                    geoFeature.superCategory = SuperCategoryId.HOUSENUMBER
                    if (!streetNumberMap.containsKey(street.toString())) {
                        streetNumberMap[street.toString()] = FeatureCollection()
                    }
                    streetNumberMap[street]?.addFeature(geoFeature)
                } else {
                    geoFeature.name = name
                    geoFeature.ref = ref
                    geoFeature.street = street
                    geoFeature.featureClass = featureClass
                    geoFeature.featureSubClass = featureSubClass
                    geoFeature.properties = properties
                    if (layer.name == "transportation") {
                        crossingsByOsmId[id]?.let { crossing ->
                            geoFeature.setProperty("crossing_type", crossing.type)
                            crossing.name?.let { geoFeature.setProperty("crossing_name", it) }
                            geoFeature.setProperty("crossing_position", crossing.position)
                            // Two Doubles rather than an LngLatAlt: GeoJsonObjectMoshiAdapter
                            // writes any non-primitive property value as JSON null, so an object
                            // here would silently vanish from the debug GeoJSON dumps.
                            crossing.point?.let {
                                geoFeature.setProperty("crossing_latitude", it.latitude)
                                geoFeature.setProperty("crossing_longitude", it.longitude)
                            }
                        }
                    }
                    if (translateProperties(geoFeature)) {
                        // Categorise as we go, picking the highest ranking category
                        val ft = superCategoryMap[geoFeature.featureType]
                            ?: SuperCategoryId.UNCATEGORIZED
                        val fv = superCategoryMap[geoFeature.featureValue]
                            ?: SuperCategoryId.UNCATEGORIZED
                        if (ft > fv)
                            geoFeature.superCategory = ft
                        else
                            geoFeature.superCategory = fv

                        if ((layer.name == "poi") || (layer.name == "place")) {
                            // If this is an un-named garden, then we can discard it
                            if (geoFeature.featureValue == "garden") {
                                if (name == null)
                                    continue
                            }
                            if (feature.type == Tile.GeomType.POLYGON) {
                                mapPolygonFeatures.getOrPut(id) { mutableListOf() }.add(geoFeature)
                            } else {
                                mapPointFeatures[id] = geoFeature
                            }
                        } else if (layer.name == "transportation") {
                            if (geoFeature.geometry.type != "LineString") {
                                collection.addFeature(geoFeature)
                            } else {
                                if ((featureClass == "transit") || (featureClass == "rail")) {
                                    if (!isUnmatchableRailway(featureSubClass)) {
                                        transitGenerator.addFeature(geoFeature)
                                    }
                                } else {
                                    wayGenerator.addFeature(geoFeature)
                                }

                                if (geoFeature.superCategory != SuperCategoryId.UNCATEGORIZED) {
                                    // Features like Piers and steps are POIs as well as ways, so ensure
                                    // that we add them
                                    collection.addFeature(geoFeature)
                                }
                            }
                        } else {
                            mapBuildingFeatures[id] = geoFeature
                        }
                    }
                }
            }
        }

        if (layer.name == "transportation") {
            // Add all of our interpolated nodes
            for (feature in mapInterpolatedNodes) {
                collection.addFeature(feature.value)
            }
        }
    }

    entranceMatching.generateEntrances(
        collection,
        mapPolygonFeatures,
        mapBuildingFeatures,
        tileX,
        tileY,
        tileZoom
    )

    // Add all of the polygon features
    for (featureList in mapPolygonFeatures) {
        for (feature in featureList.value) {
            collection.addFeature(feature)
            addToStreetNumberMap(feature as MvtFeature, streetNumberMap)
        }
        // If we add as a polygon feature, then remove any point feature for the same id
        mapPointFeatures.remove(featureList.key)
        mapBuildingFeatures.remove(featureList.key)
    }

    // And then add the remaining non-duplicated point features
    for (feature in mapPointFeatures) {
        collection.addFeature(feature.value)
        addToStreetNumberMap(feature.value as MvtFeature, streetNumberMap)
        mapBuildingFeatures.remove(feature.key)
    }
    // And then any remaining buildings that weren't POIs
    for (feature in mapBuildingFeatures) {
        collection.addFeature(feature.value)
        addToStreetNumberMap(feature.value as MvtFeature, streetNumberMap)
    }

    val tileData = Array(TreeId.MAX_COLLECTION_ID.id) { FeatureCollection() }
    // Add intersections
    wayGenerator.generateWays(
        tileData[TreeId.INTERSECTIONS.id],
        tileData[TreeId.ROADS_AND_PATHS.id],
        tileData[TreeId.ROADS.id],
        collection,
        intersectionMap,
        tileX, tileY, tileZoom
    )

    // We don't need an INTERSECTIONS-style output collection or a roads-only split for transit,
    // but we do collect the per-tile intersection map so that GridState can stitch railway Ways
    // across tile boundaries the same way it does for roads.
    transitGenerator.generateWays(
        null,
        tileData[TreeId.TRANSIT.id],
        null,
        collection,
        transitIntersectionMap,
        tileX, tileY, tileZoom
    )

    // TODO:
    //  This is the first step towards categorising Features as we go rather than returning
    //  a full FeatureCollection and leaving it up to the GridState. For example, we can stop
    //  WayGenerators from putting their results into the global collection and put them into the
    //  filtered collections immediately.
    processTileFeatureCollection(tileData, collection)

    return tileData
}

/**
 * translateProperties takes the properties stored in the MVT and translates them into a set of
 * foreign properties that nearer matches those returned by the soundscape-backend.
 *
 * @param feature is the MvtFeature to have its properties translated
 *
 * @return a map of properties that can be used in the same way as those from soundscape-backend
 */

fun translateProperties(feature: MvtFeature): Boolean {
    // This mapping is constructed from the class description in:
    // https://github.com/davecraig/openmaptiles/blob/master/layers/transportation/transportation.yaml
    when (feature.featureClass) {
        "motorway",
        "trunk",
        "primary",
        "secondary",
        "tertiary",
        "minor",
        "service",
        "track",
        "raceway",
        "busway",
        "bus_guideway",
        "ferry",
        "motorway_construction",
        "trunk_construction",
        "primary_construction",
        "secondary_construction",
        "tertiary_construction",
        "minor_construction",
        "path_construction",
        "service_construction",
        "track_construction",
        "raceway_construction" -> {
            feature.featureType = "highway"
            feature.featureValue = feature.featureClass
        }

        "crossing" -> {
            if (feature.properties?.get("crossing") == "unmarked") {
                if ((feature.properties?.get("tactile_paving") == "no") || (feature.properties?.containsKey(
                        "tactile_paving"
                    ) == false)
                ) {
                    // Unmarked crossings without tactile paving should be ignored.
                    return false
                }
            }

            feature.featureType = "highway"
            feature.featureValue = feature.featureClass
        }

        "path" -> {
            // Paths can have a more descriptive type in their subclass
            feature.featureType = "highway"
            feature.featureValue = feature.featureSubClass
        }

        "bus" -> {
            feature.featureType = "highway"
            feature.featureValue = "bus_stop"
        }

        // These are the features which we don't add to POI (for now at least)
        "cycle_barrier",
        "bicycle_parking",
        "waste_basket",
        "grit_bin",
        "vacant",
        "bollard",
        "gate" -> {
            return false
        }

        else -> {
            feature.featureType = feature.featureClass
            feature.featureValue = feature.featureSubClass
        }
    }
    val building = feature.properties?.get("building")
    if (building != null) {
        feature.featureType = "building"
        feature.featureValue = building.toString()
    }

    return true
}
