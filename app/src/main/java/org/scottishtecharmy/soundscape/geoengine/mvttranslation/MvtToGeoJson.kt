package org.scottishtecharmy.soundscape.geoengine.mvttranslation

import org.scottishtecharmy.soundscape.geoengine.MAX_ZOOM_LEVEL
import org.scottishtecharmy.soundscape.geoengine.MIN_MAX_ZOOM_LEVEL
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeoJsonObject
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiPoint
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon
import org.scottishtecharmy.soundscape.geoengine.utils.getLatLonTileWithOffset
import vector_tile.VectorTile

fun pointIsOffTile(x: Int, y: Int) : Boolean {
    return (x < 0 || y < 0 || x >= 4096 || y >= 4096)
}

fun sampleToFractionOfTile(sample: Int) : Double {
    return (sample.toDouble() + 0.5) / 4096.0
}

private fun parseGeometry(
    cropToTile: Boolean,
    geometry: MutableList<Int>
): List<ArrayList<Pair<Int, Int>>> {

    //  Converting the geometry coordinates requires some effort. See
    //      https://github.com/mapbox/vector-tile-spec/blob/master/2.1/README.md#43-geometry-encoding
    //
    //  Geometries can contain multiple line segments if they have the same tags e.g. a road with a
    //  single name that has multiple spurs. For this reason the function returns a List of line
    //  segments.
    //
    var x = 0
    var y = 0
    val results = mutableListOf(arrayListOf<Pair<Int,Int>>())
    var id : Int
    var count = 0
    var deltaX = 0
    var deltaY: Int
    var firstOfPair = true
    var lineCount = 0
    for (commandOrParameterInteger in geometry) {
        if (count == 0) {
            id = commandOrParameterInteger.and(0x7)
            count = commandOrParameterInteger.shr(3)
            when (id) {
                1 -> {
                    deltaX = 0
                    firstOfPair = true
                    ++lineCount
                    if((lineCount > 1) && results.last().isNotEmpty())
                        results.add(arrayListOf())
                }

                2 -> {
                    deltaX = 0
                }

                7 -> {
                    // Close the polygon
                    results.last().add(results.last().first())
                    count = 0
                }

                else -> {
                    println("Unknown command id $id")
                    //assert(false)
                }
            }
        } else {
            val value =
                ((commandOrParameterInteger.shr(1)).xor(-(commandOrParameterInteger.and(1))))

            if (firstOfPair) {
                deltaX = value
                firstOfPair = false
            } else {
                deltaY = value
                firstOfPair = true

                x += deltaX
                y += deltaY

                // The vector tile has pixel coordinates relative to the tile origin. Convert
                // these to global coordinates
                var add = true
                if(cropToTile) {
                    if(pointIsOffTile(x, y))
                        add = false
                }
                if(add) {
                    results.last().add(Pair(x,y))
                }
                --count
            }
        }
    }

    return results
}

fun convertGeometry(tileX : Int, tileY : Int, tileZoom : Int, geometry: ArrayList<Pair<Int, Int>>) : ArrayList<LngLatAlt> {
    val results = arrayListOf<LngLatAlt>()
    for(point in geometry) {
        results.add(
            getLatLonTileWithOffset(tileX,
            tileY,
            tileZoom,
            sampleToFractionOfTile(point.first),
            sampleToFractionOfTile(point.second))
        )
    }
    return results
}

fun areCoordinatesClockwise(
    coordinates: ArrayList<Pair<Int, Int>>
): Boolean {

    // The coordinates are for a closed polygon - the last coordinate is the same as the first
    var area = 0.0
    for(i in 0 until coordinates.size - 1) {
        area += (coordinates[i + 1].first - coordinates[i].first) * (coordinates[i + 1].second + coordinates[i].second)

    }
    return area < 0
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
 * lines to improve the graphical UI is untouched.
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

fun vectorTileToGeoJson(tileX: Int,
                        tileY: Int,
                        mvt: VectorTile.Tile,
                        intersectionMap:  HashMap<LngLatAlt, Intersection>,
                        cropPoints: Boolean = true,
                        tileZoom: Int = MAX_ZOOM_LEVEL): FeatureCollection {

    val collection = FeatureCollection()
    val wayGenerator = WayGenerator()
    val entranceMatching = EntranceMatching()

    // The main TileGrid is at the MAX_ZOOM_LEVEL and we parse transportation, poi and building
    // layers. However, we also create TileGrids at lower zoom levels to get towns, cities etc. from
    // the place layer.
    val layerIds = if(tileZoom >= MIN_MAX_ZOOM_LEVEL) {
        arrayOf("transportation", "poi", "building")
    } else {
        arrayOf("place")
    }

    // POI can have duplicate entries for polygons and points and also duplicates in the Buildings
    // layer we de-duplicate them with these maps.
    val mapPolygonFeatures : HashMap<Double, MutableList<Feature>> = hashMapOf()
    val mapBuildingFeatures : HashMap<Double, Feature> = hashMapOf()
    val mapPointFeatures : HashMap<Double, Feature> = hashMapOf()

    for(layer in mvt.layersList) {
        if(!layerIds.contains(layer.name)) {
            continue
        }
        //println("Process layer: " + layer.name)

        val mapInterpolatedNodes : HashMap<Double, Feature> = hashMapOf()
        for (feature in layer.featuresList) {

            var entrance = false
            // We use Double to store the OSM id as JSON doesn't support Long
            val id = feature.id.toDouble()

            // Convert coordinates to GeoJSON. This is where we find out how many features
            // we're actually dealing with as there can be multiple features that have the
            // same properties.
            assert(feature.type != null)
            val listOfGeometries = mutableListOf<GeoJsonObject>()

            // Parse tags
            var firstInPair = true
            var key = ""
            var value: Any? = null
            var properties: java.util.HashMap<String, Any?>? = null
            for (tag in feature.tagsList) {
                if (firstInPair)
                    key = layer.getKeys(tag)
                else {
                    val raw = layer.getValues(tag)
                    if (raw.hasBoolValue())
                        value = layer.getValues(tag).boolValue
                    else if (raw.hasIntValue())
                        value = layer.getValues(tag).intValue
                    else if (raw.hasSintValue())
                        value = layer.getValues(tag).sintValue
                    else if (raw.hasFloatValue())
                        value = layer.getValues(tag).doubleValue
                    else if (raw.hasDoubleValue())
                        value = layer.getValues(tag).floatValue
                    else if (raw.hasStringValue())
                        value = layer.getValues(tag).stringValue
                    else if (raw.hasUintValue())
                        value = layer.getValues(tag).uintValue
                }

                if (!firstInPair) {
                    if (properties == null) {
                        properties = HashMap()
                    }
                    properties[key] = value
                    firstInPair = true
                } else
                    firstInPair = false
            }

            if(layer.name == "building") {
                // Check that we have a name, otherwise we're not interested
                if((properties == null) || (properties["name"] == null))
                    continue
            }

            // Parse geometries
            when (feature.type) {
                VectorTile.Tile.GeomType.POLYGON -> {
                    val polygons = parseGeometry(
                        false,
                        feature.geometryList
                    )

                    // If all of the polygon points are outside the tile, then we can immediately
                    // discard it
                    var allOutside = true
                    for (polygon in polygons) {
                        for(point in polygon) {
                            if(!pointIsOffTile(point.first, point.second)) {
                                allOutside = false
                                break
                            }
                        }
                        if(!allOutside)
                            break
                    }
                    if(allOutside)
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

                        if(areCoordinatesClockwise(polygon)) {
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
                            if(lastClockwisePolygon != null) {
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

                        if(layer.name == "poi" || layer.name == "building") {
                            if(properties?.get("name") != null) {
                                val entranceDetails = EntranceDetails(properties["name"]?.toString(),
                                    null,
                                    properties["layer"]?.toString(),
                                    true,
                                    id)
                                entranceMatching.addGeometry(polygon, entranceDetails)
                            }
                        }
                    }
                }

                VectorTile.Tile.GeomType.POINT -> {
                    val points =
                        parseGeometry(cropPoints, feature.geometryList)
                    for (point in points) {
                        if (point.isNotEmpty()) {
                            val coordinates = convertGeometry(tileX, tileY, tileZoom, point)
                            listOfGeometries.add(
                                Point(coordinates[0].longitude, coordinates[0].latitude)
                            )

                            if(properties?.get("class") == "entrance") {
                                val entranceDetails = EntranceDetails(
                                    properties["name"]?.toString(),
                                    properties["subclass"]?.toString(),
                                    properties["layer"]?.toString(),
                                    false,
                                    id)
                                entranceMatching.addGeometry(point, entranceDetails)
                                entrance = true
                            }
                        }
                    }
                }

                VectorTile.Tile.GeomType.LINESTRING -> {
                    val lines = parseGeometry(
                        false,
                        feature.geometryList
                    )

                    var name : Any? = null
                    properties?.let {
                        name = properties["name"]
                        properties["name"] = name
                    }

                    if(layer.name == "transportation")
                    {
                        for (line in lines) {
                            if(feature.id == 0L) {
                                println("Feature ID is zero for ${name.toString()}")
                            }
                            wayGenerator.addLine(line)
                            val interpolatedNodes : MutableList<LngLatAlt> = mutableListOf()
                            val clippedLines = convertGeometryAndClipLineToTile(tileX,
                                                                                tileY,
                                                                                tileZoom,
                                                                                line,
                                                                                interpolatedNodes)
                            for(clippedLine in clippedLines) {
                                listOfGeometries.add(clippedLine)
                            }

                            if(interpolatedNodes.isNotEmpty()) {
                                // If the line went off the edge of the tile then we will have
                                // generated an interpolated node at the tile edge. We store this in
                                // a Feature which is a list of those nodes for this OSM id. It may
                                // just be a single point, or the line may have gone on and off the
                                // tile multiple times.
                                if (mapInterpolatedNodes.containsKey(id)) {
                                    // If we've already got this OSM id, we want to extend it with
                                    // the new points
                                    val currentLine = mapInterpolatedNodes[id]?.geometry as MultiPoint
                                    for(node in interpolatedNodes) {
                                        currentLine.coordinates.add(node)
                                    }
                                } else {
                                    val interpolatedFeature = Feature()
                                    val foreign: HashMap<String, Any?> = hashMapOf()
                                    foreign["osm_id"] = id
                                    interpolatedFeature.foreign = foreign
                                    interpolatedFeature.geometry =
                                        MultiPoint(ArrayList(interpolatedNodes))
                                    interpolatedFeature.properties = hashMapOf()
                                    interpolatedFeature.properties!!["class"] = "edgePoint"
                                    mapInterpolatedNodes[id] = interpolatedFeature
                                }
                            }
                        }
                    }
                }

                // Assert on all other geometry enum values
                null,
                VectorTile.Tile.GeomType.UNKNOWN -> {
                    assert(false)
                }
            }

            if(entrance) {
                // We've added the entrance to our matching code and so we don't need to add it as
                // as feature now
                continue
            }

            for (geometry in listOfGeometries) {
                // And map the tags
                val geoFeature = Feature()
                geoFeature.geometry = geometry
                properties!!["osm_ids"] = id
                geoFeature.properties = properties
                val foreign = translateProperties(properties, id)
                if(foreign.isNotEmpty()) {
                    geoFeature.foreign = foreign
                    if ((layer.name == "poi") || (layer.name == "place")) {
                        // If this is an un-named garden, then we can discard it
                        if(foreign["feature_value"] == "garden") {
                            if(!properties.containsKey("name"))
                                continue
                        }
                        if (feature.type == VectorTile.Tile.GeomType.POLYGON) {
                            if(!mapPolygonFeatures.contains(id)) {
                                mapPolygonFeatures[id] = MutableList(1){ geoFeature }
                            } else {
                                mapPolygonFeatures[id]!!.add(geoFeature)
                            }
                        } else {
                            mapPointFeatures[id] = geoFeature
                        }
                    } else if (layer.name == "transportation") {
                        if(geoFeature.geometry.type != "LineString") {
                            collection.addFeature(geoFeature)
                        } else {
                            wayGenerator.addFeature(geoFeature)
                        }
                    } else {
                        mapBuildingFeatures[id] = geoFeature
                    }
                }
            }
        }

        if(layer.name == "transportation") {
            // Add all of our interpolated nodes
            for (feature in mapInterpolatedNodes) {
                collection.addFeature(feature.value)
            }
        }
    }

    entranceMatching.generateEntrances(
        collection,
        mapPolygonFeatures,
        tileX,
        tileY,
        tileZoom
    )

    // Add all of the polygon features
    for (featureList in mapPolygonFeatures) {
        for(feature in featureList.value) {
            collection.addFeature(feature)
        }
        // If we add as a polygon feature, then remove any point feature for the same id
        mapPointFeatures.remove(featureList.key)
        mapBuildingFeatures.remove(featureList.key)
    }

    // And then add the remaining non-duplicated point features
    for (feature in mapPointFeatures) {
        collection.addFeature(feature.value)
        mapBuildingFeatures.remove(feature.key)
    }
    // And then any remaining buildings that weren't POIs
    for (feature in mapBuildingFeatures) {
        collection.addFeature(feature.value)
    }
    // Add intersections
    wayGenerator.generateWays(collection, collection, intersectionMap, tileX, tileY, tileZoom)

    return collection
}

/**
 * translateProperties takes the properties stored in the MVT and translates them into a set of
 * foreign properties that nearer matches those returned by the soundscape-backend.
 *
 * @param properties is a map of the tags from the MVT feature
 * @param id is the feature id which is ((OSM_ID * 10) + offset) where offset is
 *   1 for an OSM node
 *   2 for an OSM  way
 *   3 for an OSM  relation
 *
 * @return a map of properties that can be used in the same way as those from soundscape-backend
 */

fun translateProperties(properties: HashMap<String, Any?>?, id: Double): HashMap<String, Any?> {
    val foreign : HashMap<String, Any?> = hashMapOf()
    if(properties != null) {
        for(property in properties) {
            if (property.key == "class") {
                // This mapping is constructed from the class description in:
                // https://github.com/davecraig/openmaptiles/blob/master/layers/transportation/transportation.yaml
                when (property.value) {
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
                        foreign["feature_type"] = "highway"
                        foreign["feature_value"] = property.value
                    }

                    "crossing" -> {
                        if(properties["crossing"] == "unmarked") {
                            if((properties["tactile_paving"] == "no") || (!properties.containsKey("tactile_paving"))) {
                                // Unmarked crossings without tactile paving should be ignored.
                                return hashMapOf()
                            }
                        }

                        foreign["feature_type"] = "highway"
                        foreign["feature_value"] = property.value
                    }

                    "path" -> {
                        // Paths can have a more descriptive type in their subclass
                        foreign["feature_type"] = "highway"
                        foreign["feature_value"] = properties["subclass"]
                    }

                    "bus" -> {
                        foreign["feature_type"] = "highway"
                        foreign["feature_value"] = "bus_stop"
                    }

                    // These are the features which we don't add to POI (for now at least)
                    "cycle_barrier",
                    "bollard",
                    "gate" -> {
                        return hashMapOf()
                    }

                    else -> {
                        foreign["feature_type"] = property.value
                        val subclass = properties["subclass"]
                        if(subclass != null) {
                            foreign["feature_value"] = subclass
                        }
                    }
                }
            } else if (property.key == "building") {
                // This is used for mapping warehouses
                foreign["feature_type"] = property.key
                foreign["feature_value"] = property.value
            }
        }
    }
    val osmIds = arrayListOf<Double>()
    osmIds.add(id)
    foreign["osm_ids"] = osmIds

    return foreign
}
