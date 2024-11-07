package org.scottishtecharmy.soundscape.utils

import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeoJsonObject
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon
import org.scottishtecharmy.soundscape.utils.TileGrid.Companion.ZOOM_LEVEL
import vector_tile.VectorTile

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
    var id = 0
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
                    if(x < 0 || y < 0 || x >= 4096 || y >= 4096)
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

private fun convertGeometry(tileX : Int, tileY : Int, tileZoom : Int, geometry: ArrayList<Pair<Int, Int>>) : ArrayList<LngLatAlt> {
    val results = arrayListOf<LngLatAlt>()
    for(point in geometry) {
        results.add(getLatLonTileWithOffset(tileX,
            tileY,
            tileZoom,
            point.first.toDouble()/4096.0,
            point.second.toDouble()/4096.0))
    }
    return results
}

data class IntersectionDetails(
    val name : String,
    val type : String,
    val subClass : String,
    val brunnel : String,
    val id : Long,
    var lineEnd : Boolean = false
)

class IntersectionDetection {

    /**
    * highwayPoints is a sparse map which maps from a location within the tile to a list of
    * lines which have nodes at that point. Every node on any `transportation` line will appear in the
    * map and if after processing all of the lines there's an intersection at that point, the map
     * entry will have information for more than one line.
    */
    private val highwayNodes : HashMap< Int, ArrayList<IntersectionDetails>> = hashMapOf()

    /**
     * addLine is called for any line feature that is being added to the FeatureCollection.
     * @param line is a new `transportation` layer line to add to the map
     * @param details describes the line that is being added.
     *
     */
    fun addLine(line : ArrayList<Pair<Int, Int>>,
                details : IntersectionDetails) {
        for (point in line) {
            if((point.first < 0) || (point.first > 4095) ||
                (point.second < 0) || (point.second > 4095)) {
                continue
            }

            // Rather than have a 2D sparse array, turn the coordinates into a single int so that we
            // can have a 1D sparse array instead.
            val coordinateKey = point.first.shl(12) + point.second
            if (highwayNodes[coordinateKey] == null) {
                highwayNodes[coordinateKey] = arrayListOf(details)
            }
            else {
                details.lineEnd = ((point == line.first()) || (point == line.last()))
                highwayNodes[coordinateKey]?.add(details)

                val roads = highwayNodes[coordinateKey]
                if (roads != null) {
                    var intersectionNames = ""
                    var firstRoad = true
                    for (road in roads) {
                        if (!firstRoad)
                            intersectionNames += ","
                        intersectionNames += road.name
                        firstRoad = false
                    }
                    //println("Intersection: $intersectionNames")
                }
            }
        }
    }

    /**
     * generateIntersections goes through our hash map and adds an intersection feature to the
     * collection wherever it finds out.
     * @param collection is where the new intersection features are added
     * @param tileX the tile x coordinate so that the tile relative location of the intersection can
     * be turned into a latitude/longitude
     * @param tileY the tile y coordinate so that the tile relative location of the intersection can
     *      * be turned into a latitude/longitude
     */
    fun generateIntersections(collection: FeatureCollection, tileX : Int, tileY : Int, tileZoom : Int) {
        // Add points for the intersections that we found
        for ((key, intersections) in highwayNodes) {

            // An intersection exists where there are nodes from multiple line at the same location
            if (intersections.size > 1) {

                // Turn our coordinate key back into tile relative x,y coordinates
                val x = key.shr(12)
                val y = key.and(0xfff)
                // Convert the tile relative coordinate into a LatLngAlt
                val point = arrayListOf(Pair(x, y))
                val coordinates = convertGeometry(tileX, tileY, tileZoom, point)

                // Create our intersection feature to match those from soundscape-backend
                val intersection = Feature()
                intersection.geometry =
                    Point(coordinates[0].longitude, coordinates[0].latitude)
                intersection.foreign = HashMap()
                intersection.foreign!!["feature_type"] = "highway"
                intersection.foreign!!["feature_value"] = "gd_intersection"
                var name = ""
                val osmIds = arrayListOf<Long>()
                for (road in intersections) {
                    if(name.isNotEmpty()) {
                        name += "/"
                    }
                    if (road.brunnel != "null")
                        name += road.brunnel
                    else if (road.subClass != "null")
                        name += road.subClass
                    else
                        name += road.name

                    osmIds.add(road.id)
                }
                intersection.foreign!!["osm_ids"] = osmIds
                intersection.properties = HashMap()
                intersection.properties!!["name"] = name
                collection.addFeature(intersection)
            }
        }
    }
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

    val layerIds = arrayOf("transportation", "poi")

    for(layer in mvt.layersList) {
        if(!layerIds.contains(layer.name)) {
            continue
        }

        println("Process layer: " + layer.name)
        for (feature in layer.featuresList) {
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

            // Parse geometries
            when (feature.type) {
                VectorTile.Tile.GeomType.POLYGON -> {
                    val polygons = parseGeometry(
                        false,
                        feature.geometryList
                    )
                    for (polygon in polygons) {
                        if (polygon.first() != polygon.last())
                            polygon.add(polygon.first())
                        listOfGeometries.add(
                            Polygon(
                                convertGeometry(
                                    tileX,
                                    tileY,
                                    tileZoom,
                                    polygon
                                )
                            )
                        )
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
                        }
                    }
                }

                VectorTile.Tile.GeomType.LINESTRING -> {
                    val lines = parseGeometry(
                        false,
                        feature.geometryList
                    )

                    var name : Any? = null
                    var type = ""
                    var subclass = ""
                    var brunnel = ""
                    properties?.let {
                        name = properties["name"]
                        if(name == null) {
                            // This is nameless, so use the class to describe it
                            name = properties["class"].toString()
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
                                name.toString(),
                                type,
                                subclass,
                                brunnel,
                                feature.id
                            )
                            intersectionDetection.addLine(line, details)
                            listOfGeometries.add(
                                LineString(
                                    convertGeometry(
                                        tileX,
                                        tileY,
                                        tileZoom,
                                        line
                                    )
                                )
                            )
                        }
                   }
                }

                VectorTile.Tile.GeomType.UNKNOWN -> {
                    assert(false)
                }
            }

            for (geometry in listOfGeometries) {
                // And map the tags
                val geoFeature = Feature()
                geoFeature.geometry = geometry
                properties!!["osm_ids"] = feature.id
                geoFeature.properties = properties
                geoFeature.foreign = translateProperties(properties, feature.id)
                collection.addFeature(geoFeature)
            }
        }
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

fun translateProperties(properties: HashMap<String, Any?>?, id: Long): HashMap<String, Any?> {
    val foreign : HashMap<String, Any?> = hashMapOf()
    if(properties != null) {
        for(property in properties) {
            if (property.key == "class") {
                //println("Class ${property.value}")
                when (property.value) {
                    "service",
                    "secondary",
                    "tertiary",
                    "minor" -> {
                        foreign["feature_type"] = "highway"
                        foreign["feature_value"] = "unclassified"
                    }

                    "track",
                    "path" -> {
                        foreign["feature_type"] = "highway"
                        foreign["feature_value"] = "path"
                    }

                    "primary" -> {
                        foreign["feature_type"] = "highway"
                        foreign["feature_value"] = "primary"
                    }

                    "bus" -> {
                        foreign["feature_type"] = "highway"
                        foreign["feature_value"] = "bus_stop"
                    }

                    else -> {
                        foreign["feature_type"] = property.value
                    }
                }
            }
        }
    }
    val osmIds = arrayListOf<Long>()
    osmIds.add(id)
    foreign["osm_ids"] = osmIds

    return foreign
}
