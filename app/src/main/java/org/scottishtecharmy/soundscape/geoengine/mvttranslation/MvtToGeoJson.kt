package org.scottishtecharmy.soundscape.geoengine.mvttranslation

import org.scottishtecharmy.soundscape.geoengine.ZOOM_LEVEL
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
 */
fun vectorTileToGeoJson(tileX: Int,
                        tileY: Int,
                        mvt: VectorTile.Tile,
                        cropPoints: Boolean = true,
                        tileZoom: Int = ZOOM_LEVEL): FeatureCollection {

    val collection = FeatureCollection()
    val intersectionDetection = IntersectionDetection()
    val entranceMatching = EntranceMatching()

    val layerIds = arrayOf("transportation", "poi", "building")

    // POI can have duplicate entries for polygons and points and also duplicates in the Buildings
    // layer we de-duplicate them with these maps.
    val mapPolygonFeatures : HashMap<Double, MutableList<Feature>> = hashMapOf()
    val mapBuildingFeatures : HashMap<Double, Feature> = hashMapOf()
    val mapPointFeatures : HashMap<Double, Feature> = hashMapOf()

    for(layer in mvt.layersList) {
        if(!layerIds.contains(layer.name)) {
            continue
        }
        println("Process layer: " + layer.name)

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

                        if(layer.name == "poi") {
                            if(properties?.get("name") != null) {
                                val entranceDetails = EntranceDetails(properties["name"]?.toString(),
                                    null,
                                    true,
                                    id)
                                entranceMatching.addPolygon(polygon, entranceDetails)
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
                                val entranceDetails = EntranceDetails(properties["name"]?.toString(),
                                    properties["subclass"]?.toString(),
                                    false, id)
                                entranceMatching.addPolygon(point, entranceDetails)
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
                    var initialName : Any? = null
                    var type = ""
                    var subclass = ""
                    var brunnel = ""
                    properties?.let {
                        initialName = properties["name"]
                        name = initialName
                        if(name == null) {
                            // This is nameless, so use the class to describe it
                            name = properties["class"].toString()
                            properties["default_name"] = "1"
                        }
                        properties["name"] = name
                        type = properties["class"].toString()
                        subclass = properties["subclass"].toString()
                        brunnel = properties["brunnel"].toString()
                    }

                    if(layer.name == "transportation")
                    {
                        for (line in lines) {
                            if(feature.id == 0L) {
                                println("Feature ID is zero for ${name.toString()}")
                            }
                            val details = IntersectionDetails(
                                (initialName ?: "").toString(),
                                type,
                                subclass,
                                brunnel,
                                id
                            )
                            intersectionDetection.addLine(line, details)
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
                    if (layer.name == "poi") {
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
                        collection.addFeature(geoFeature)
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
    // Add all of the polygon features
    for (featureList in mapPolygonFeatures) {
        for(feature in featureList.value) {
            collection.addFeature(feature)
        }
        // If we add as a polygon feature, then remove any point feature for the same id
        mapPointFeatures.remove(featureList.key)
        mapBuildingFeatures.remove(featureList.key)
    }

    entranceMatching.generateEntrances(collection, tileX, tileY, tileZoom)

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
    intersectionDetection.generateIntersections(collection, tileX, tileY, tileZoom)

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
                    "raceway_construction",
                    "crossing" -> {
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
