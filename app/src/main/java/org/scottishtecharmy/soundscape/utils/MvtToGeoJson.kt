package org.scottishtecharmy.soundscape.utils

import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeoJsonObject
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Geometry
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiPoint
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon
import org.scottishtecharmy.soundscape.utils.TileGrid.Companion.ZOOM_LEVEL
import vector_tile.VectorTile

fun pointIsOffTile(x: Int, y: Int) : Boolean {
    return (x < 0 || y < 0 || x >= 4096 || y >= 4096)
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
    val id : Double,
    var lineEnd : Boolean = false
)

class InterpolatedPointsJoiner {

    private val interpolatedPoints: HashMap<Double, MutableList<LngLatAlt>> = hashMapOf()

    fun addInterpolatedPoints(feature: Feature): Boolean {
        // We add all edgePoint coordinates to our HashMap of interpolated points by OSM id
        if (feature.properties?.containsKey("class")!!) {
            if (feature.properties!!["class"] == "edgePoint") {
                val geometry = feature.geometry as Geometry<LngLatAlt>
                val osmId = feature.foreign!!["osm_id"] as Double
                if (!interpolatedPoints.containsKey(osmId)) {
                    interpolatedPoints[osmId] = mutableListOf()
                }
                for (point in geometry.coordinates) {
                    // Add the point
                    interpolatedPoints[osmId]!!.add(point)
                }
                return false
            }
        }
        return true
    }

    fun addJoiningLines(featureCollection : FeatureCollection) {
        for (entries in interpolatedPoints) {
            if (entries.value.size > 1) {
                // We want to find points that we can join together. Go through the list of points
                // for the OSM id comparing against the other members in the list to see if any are
                // almost at the same point.
                for ((index1, point1) in entries.value.withIndex()) {
                    for ((index2, point2) in entries.value.withIndex()) {
                        if (index1 != index2) {
                            if (distance(
                                    point1.latitude,
                                    point1.longitude,
                                    point2.latitude,
                                    point2.longitude
                                ) < 0.1
                            ) {
                                // If the points are within 10cm of each other, then join their
                                // LineStrings together.
                                val joining = Feature()
                                val foreign: HashMap<String, Any?> = hashMapOf()
                                val osmIds = arrayListOf<Double>()
                                osmIds.add(entries.key)
                                foreign["osm_ids"] = entries.key
                                joining.foreign = foreign
                                joining.geometry = LineString(point1, point2)
                                joining.properties = foreign

                                featureCollection.addFeature(joining)
                            }
                        }
                    }
                }
            } else {
                // This is point must be on the outer edge or our grid, so we need do nothing
            }
        }
    }
}

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
            val detailsCopy = details.copy()
            detailsCopy.lineEnd = ((point == line.first()) || (point == line.last()))
            if (highwayNodes[coordinateKey] == null) {
                highwayNodes[coordinateKey] = arrayListOf(detailsCopy)
            }
            else {
                highwayNodes[coordinateKey]?.add(detailsCopy)
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

                if(intersections.size == 2) {
                    // An intersection with only 2 lines might just be the same line but it's been
                    // drawn in two separate segments. It's an an intersection if both lines aren't
                    // ending (i.e. one line is joining half way along the other line) or the type
                    // of line changes, or the name changes etc. Check these and don't add the
                    // intersection if we don't believe it meets the criteria.
                    val line1 = intersections[0]
                    val line2 = intersections[1]
                    if((line1.type == line2.type) &&
                        (line1.name == line2.name) &&
                        (line1.brunnel == line2.brunnel) &&
                        (line1.subClass == line2.subClass) &&
                        line1.lineEnd && line2.lineEnd) {
                        // This isn't an intersection, simply two line segments with the same
                        // properties joining at a point.
                        continue
                    }

                }

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
                val osmIds = arrayListOf<Double>()
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

data class EntranceDetails(
    val name : String?,
    val entranceType : String?,
    val poi: Boolean,
    val osmId : Double,
)
class EntranceMatching {

    /**
     * buildingNodes is a sparse map which maps from a location within the tile to a list of
     * building polygons which have nodes at that point. Every node on any `POI` polygon will appear
     * in the map along with any entrance. After processing it should be straightforward to match
     * up entrances to their POI polygons.
     */
    private val buildingNodes : HashMap< Int, ArrayList<EntranceDetails>> = hashMapOf()

    /**
     * addLine is called for any line feature that is being added to the FeatureCollection.
     * @param line is a new `transportation` layer line to add to the map
     * @param details describes the line that is being added.
     *
     */
    fun addPolygon(line : ArrayList<Pair<Int, Int>>,
                details : EntranceDetails) {
        for (point in line) {
            if((point.first < 0) || (point.first > 4095) ||
                (point.second < 0) || (point.second > 4095)) {
                continue
            }

            // Rather than have a 2D sparse array, turn the coordinates into a single int so that we
            // can have a 1D sparse array instead.
            val coordinateKey = point.first.shl(12) + point.second
            if (buildingNodes[coordinateKey] == null) {
                buildingNodes[coordinateKey] = arrayListOf(details.copy())
            }
            else {
                buildingNodes[coordinateKey]?.add(details.copy())
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
    fun generateEntrances(collection: FeatureCollection, tileX : Int, tileY : Int, tileZoom : Int) {
        // Add points for the intersections that we found
        for ((key, nodes) in buildingNodes) {

            // Generate an entrance with a matching POI polygon
            var entranceDetails : EntranceDetails? = null
            var poiDetails : EntranceDetails? = null
            for(node in nodes) {
                if(!node.poi) {
                    // We have an entrance!
                    entranceDetails = node
                } else {
                    poiDetails = node
                }
            }

            // If we have an entrance at this point then we generate a feature to represent it
            // using the POI that it is coincident with if there is one.
            if(entranceDetails != null) {
                // Turn our coordinate key back into tile relative x,y coordinates
                val x = key.shr(12)
                val y = key.and(0xfff)
                // Convert the tile relative coordinate into a LatLngAlt
                val point = arrayListOf(Pair(x, y))
                val coordinates = convertGeometry(tileX, tileY, tileZoom, point)

                // Create our entrance feature to match those from soundscape-backend
                val entrance = Feature()
                entrance.geometry =
                    Point(coordinates[0].longitude, coordinates[0].latitude)
                entrance.foreign = HashMap()
                entrance.foreign!!["feature_type"] = "entrance"
                entrance.foreign!!["feature_value"] = entranceDetails.entranceType
                val osmIds = arrayListOf<Double>()
                osmIds.add(entranceDetails.osmId)
                entrance.foreign!!["osm_ids"] = osmIds

                entrance.properties = HashMap()
                entrance.properties!!["name"] = entranceDetails.name
                if(entranceDetails.name == null)
                    entrance.properties!!["name"] = poiDetails?.name

                collection.addFeature(entrance)

//                println("Entrance: ${poiDetails?.name} ${entranceDetails.entranceType} ")
            }
        }
    }
}

fun calculateSlope(aConst: Double, a1: Double, a2: Double) : Double? {
    if (a1 == a2) {
        // Parallel lines, so no intersection
        return null
    }
    val t = (aConst - a1) / (a2 - a1)
    if (t < 0.0 || t > 1.0) {
        // Intersection point is outside the segment
        return null
    }
    return t
}

// Function to calculate the intersection with a vertical line (x = constant)
fun intersectVertical(xConst: Double, y1: Double, y2: Double, x1: Double, x2: Double): Double? {
    val t = calculateSlope(xConst, x1, x2)
    if(t != null) {
        return y1 + t * (y2 - y1)
    }
    return null
}

// Function to calculate the intersection with a horizontal line (y = constant)
fun intersectHorizontal(yConst: Double, x1: Double, x2: Double, y1: Double, y2: Double): Double? {
    val t = calculateSlope(yConst, y1, y2)
    if(t != null) {
        return x1 + t * (x2 - x1)
    }
    return null
}

/** getTileCrossingPoint returns the point at which the line connecting lastPoint and point crosses
 * the tile boundary. If both points are outside the tile there can be two intersection points
 * returned. Otherwise there can only be a single intersection point.
 * @param point1 Point on line that might cross tile boundary
 * @param point2 Another point on the line that might cross the tile boundary
 *
 * @return The coordinates at which the line crosses the tile boundary as a list of pairs of Doubles
 * to give  us the best precision.
 */
fun getTileCrossingPoint(point1 : Pair<Int, Int>, point2 : Pair<Int, Int>) : List<Pair<Double, Double>> {

    // Extract the coordinates of the points and square boundaries
    val x1 = point1.first.toDouble()
    val y1 = point1.second.toDouble()
    val x2 = point2.first.toDouble()
    val y2 = point2.second.toDouble()

    val intersections = mutableListOf<Pair<Double, Double>>()

    // Check intersections with the four sides of the square

    // Left side (x = 0)
    intersectVertical(0.0, y1, y2, x1, x2)?.let { yIntersection ->
        if (yIntersection in 0.0..4096.0) {
            intersections.add(Pair(0.0, yIntersection))
        }
    }

    // Right side (x = 4096)
    intersectVertical(4096.0, y1, y2, x1, x2)?.let { yIntersection ->
        if (yIntersection in 0.0..4096.0) {
            intersections.add(Pair(4096.0, yIntersection))
        }
    }

    // Bottom side (y = 0.0)
    intersectHorizontal(0.0, x1, x2, y1, y2)?.let { xIntersection ->
        if (xIntersection in 0.0..4096.0) {
            intersections.add(Pair(xIntersection, 0.0))
        }
    }

    // Top side (y = 4096)
    intersectHorizontal(4096.0, x1, x2, y1, y2)?.let { xIntersection ->
        if (xIntersection in 0.0..4096.0) {
            intersections.add(Pair(xIntersection, 4096.0))
        }
    }

    // Return any intersections that we found
    return intersections
}

/**
 * convertGeometryAndClipLineToTile takes a line and converts it into a List of LineStrings. In the
 * simplest case, the points are all within the tile and so there will just be a single LineString
 * output. However, if the line goes off and on the tile (bouncing around in the buffer region) then
 * there can be multiple segments returned.
 * We also store all of the interpolated points that we've been created so that we can more easily
 * connect them to the adjacent tiles in the grid.
 */
fun convertGeometryAndClipLineToTile(
    tileX: Int,
    tileY: Int,
    tileZoom: Int,
    line: ArrayList<Pair<Int, Int>>,
    interpolatedNodes: MutableList<LngLatAlt>
) : List<LineString> {
    val returnList = mutableListOf<LineString>()

    if(line.isEmpty()) {
        return returnList
    }

    // We want to iterate through the line detecting when it goes off/on tile and creating line
    // segments for each. The ends of the line as it goes off tile need to be in LatLng as we want
    // to interpolate as precisely as possible so that the line end is at the same point on adjacent
    // tiles. The only other thing to bear in mind is that it's possible for two points to be off
    // tile but the line between them to cross through the tile.
    var offTile = pointIsOffTile(line[0].first, line[0].second)
    val segment = arrayListOf<LngLatAlt>()
    var lastPoint = line[0]
    for(point in line) {
        if(pointIsOffTile(point.first, point.second) != offTile){
            if(offTile) {
                // We started off tile and this point is now on tile
                // Add interpolated point from lastPoint to this point
                val interpolatedPoint = getTileCrossingPoint(lastPoint, point)
                val interpolatedLatLon = getLatLonTileWithOffset(tileX,
                    tileY,
                    tileZoom,
                    interpolatedPoint[0].first/4096.0,
                    interpolatedPoint[0].second/4096.0)
                segment.add(interpolatedLatLon)
                interpolatedNodes.add(interpolatedLatLon)

                // Add the new point
                segment.add(getLatLonTileWithOffset(tileX,
                    tileY,
                    tileZoom,
                    point.first.toDouble()/4096.0,
                    point.second.toDouble()/4096.0))
            } else {
                // We started on tile and this point is now off tile
                // Add interpolated point from lastPoint to this point
                val interpolatedPoint = getTileCrossingPoint(lastPoint, point)
                val interpolatedLatLon = getLatLonTileWithOffset(tileX,
                    tileY,
                    tileZoom,
                    interpolatedPoint[0].first/4096.0,
                    interpolatedPoint[0].second/4096.0)

                segment.add(interpolatedLatLon)
                interpolatedNodes.add(interpolatedLatLon)
                returnList.add(LineString(ArrayList(segment)))
                segment.clear()
            }

            // Update the current point state
            offTile = offTile.xor(true)
        }
        else if(!offTile) {
            segment.add(getLatLonTileWithOffset(tileX,
                tileY,
                tileZoom,
                point.first.toDouble()/4096.0,
                point.second.toDouble()/4096.0))
        } else {
            // We're continuing off tile, but we need to check if the line between the two off tile
            // points crossed over the tile.
            val interpolatedPoints = getTileCrossingPoint(lastPoint, point)
            for(ip in interpolatedPoints) {
                val interpolatedLatLon = getLatLonTileWithOffset(tileX,
                    tileY,
                    tileZoom,
                    ip.first/4096.0,
                    ip.second/4096.0)
                segment.add(interpolatedLatLon)
                interpolatedNodes.add(interpolatedLatLon)
            }
            if(segment.isNotEmpty()) {
                returnList.add(LineString(ArrayList(segment)))
                segment.clear()
            }
        }

        lastPoint = point
    }
    if(segment.isNotEmpty()) {
        returnList.add(LineString(segment))
    }
    return returnList
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
    val mapPolygonFeatures : HashMap<Double, Feature> = hashMapOf()
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
                    for (polygon in polygons) {
                        if(layer.name == "poi") {
                            if(properties?.get("name") != null) {
                                val entranceDetails = EntranceDetails(properties["name"]?.toString(),
                                    null,
                                    true,
                                    id)
                                entranceMatching.addPolygon(polygon, entranceDetails)
                            }
                        }

                        if (polygon.first() != polygon.last()) {
                            polygon.add(polygon.first())
                        }
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
                        if (feature.type == VectorTile.Tile.GeomType.POLYGON) {
                            mapPolygonFeatures[id] = geoFeature
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
    for (feature in mapPolygonFeatures) {
        collection.addFeature(feature.value)
        // If we add as a polygon feature, then remove any point feature for the same id
        mapPointFeatures.remove(feature.key)
        mapBuildingFeatures.remove(feature.key)
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
