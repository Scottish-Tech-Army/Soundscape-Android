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
    //  Geometries can contain multiple line segments if they have the same tags e.g. unnamed
    //  minor roads. This is why we have to return a List of line segments.
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
    val nameLayer : Boolean,
    val id : String,
    var lineEnd : Boolean = false
)
private fun intersectionCheck(highwayPoints : HashMap< Int, ArrayList<IntersectionDetails>>,
                              line : ArrayList<Pair<Int, Int>>,
                              details : IntersectionDetails) {
    for (point in line) {
        if((point.first < 0) || (point.first > 4095) ||
            (point.second < 0) || (point.second > 4095)) {
            continue
        }

        val coordinateKey = point.first.shl(12) + point.second
        if (highwayPoints[coordinateKey] == null) {
            highwayPoints[coordinateKey] = arrayListOf(details)
        }
        else {
            // If the road type matches but we're on a different layer then skip
            var add = true
            for(listDetails in highwayPoints[coordinateKey]!!) {
                if((listDetails.nameLayer != details.nameLayer) &&
                    (listDetails.type == details.type)) {
                    add = false
                    break
                }
            }
            if(!add) continue
            if((point == line.first()) || (point == line.last())) {
                details.lineEnd = true
            }
            highwayPoints[coordinateKey]?.add(details)
            //
            // On initial testing, this intersection spotting is unreliable with the
            // maptiler tiles :-( There's a good explanation of why here:
            // https://gis.stackexchange.com/questions/319422/mapbox-vector-tiles-appear-to-lack-accurate-intersection-nodes
            //
            // There's no Roselea Drive/Strathblane Drive intersection because the
            // Strathblane section was drawn first and then Roselea Drive joined in
            // half way between two nodes. That node doesn't affect how you'd draw
            // Strathblane Road and so it isn't included in its list of nodes.
            //
            // It's possible that we can generate the tiles so that they don't
            // exclude intersection nodes by disabling simplification at the max
            // zoom level, see:
            // https://github.com/Scottish-Tech-Army/Soundscape-Android/actions/workflows/nightly.yaml
            //
            // This would make our tiles a little larger, but that's what you'd expect!
            //
            // One remaining question is whether it would then be  possible to have
            // a single road be made up of two separate lines which would mean that
            // we end up finding an intersection where there isn't one in the real
            // world? Also, does that render properly on the UI map?
            // Most roads segments wouldn't split other than at a junction, but the
            // code has to deal with that correctly too.
            val roads = highwayPoints[coordinateKey]
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

fun vectorTileToGeoJson(tileX: Int,
                        tileY: Int,
                        mvt: VectorTile.Tile,
                        cropPoints: Boolean = true): FeatureCollection {

    val tileZoom = ZOOM_LEVEL

    val collection = FeatureCollection()
    val intersectionPoints : HashMap< Int, ArrayList<IntersectionDetails>> = hashMapOf()

    // When processing the layers, we want to handle transportation_name before transportation.
    // This is so that we can calculate intersections with the named lines and then discard their
    // un-named duplicates in the transportation layer.
    // The only other layers we want to look at is poi.
    for (layerId in arrayOf("transportation_name", "transportation", "poi")) {
        for(layer in mvt.layersList) {
            if(layer.name != layerId)
                continue

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

                        var name = ""
                        var type = ""
                        var subclass = ""
                        var brunnel = ""
                        properties?.let {
                            name = properties["name"].toString()
                            if(name == "null") {
                                name = properties["class"].toString()
                            }
                            type = properties["class"].toString()
                            subclass = properties["subclass"].toString()
                            brunnel = properties["brunnel"].toString()
                        }

                        for (line in lines) {
                            val details = IntersectionDetails(name,
                                type,
                                subclass,
                                brunnel,
                                layerId == "transportation_name",
                                feature.id.toString())
                            intersectionCheck(intersectionPoints, line, details)
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

                    VectorTile.Tile.GeomType.UNKNOWN -> {
                        assert(false)
                    }
                }

                for (geometry in listOfGeometries) {
                    // And map the tags
                    val geoFeature = Feature()
                    geoFeature.id = feature.id.toString()
                    geoFeature.geometry = geometry
                    geoFeature.properties = properties
                    geoFeature.foreign = translateProperties(properties, feature.id.toString())
                    collection.addFeature(geoFeature)
                }
            }
        }
    }
    // Add points for the intersections that we found
    for((key, intersections)  in intersectionPoints) {
        if(intersections.size > 1) {

            // Skip any intersections where there are only two lines that are both ending and the
            // road type isn't changing
            var skip = false
            if(intersections.size == 2) {
                val road1 = intersections[0]
                val road2 = intersections[1]
                if((road1.lineEnd && road2.lineEnd) &&
                    (road1.type == road2.type)) {
                    skip = true
                }
            }
            if(skip) continue

            val intersection = Feature()
            val x = key.shr(12)
            val y = key.and(0xfff)
            val point = arrayListOf(Pair(x, y))
            val coordinates = convertGeometry(tileX, tileY, tileZoom, point)
            intersection.geometry =
                Point(coordinates[0].longitude, coordinates[0].latitude)
            intersection.foreign = HashMap()
            intersection.foreign!!["feature_type"] = "highway"
            intersection.foreign!!["feature_value"] = "gd_intersection"
            var name = ""
            val osmIds = arrayListOf<String>()
            for (road in intersections) {
                if(road.brunnel != "null")
                    name += "${road.brunnel}/"
                else if(road.subClass != "null")
                    name += "${road.subClass}/"
                else
                    name += "${road.name}/"

                osmIds.add(road.id)
            }
            intersection.foreign!!["osm_ids"] = osmIds
            intersection.properties = HashMap()
            intersection.properties!!["name"] = name
            collection.addFeature(intersection)
        }
    }
    return collection
}

fun translateProperties(properties: HashMap<String, Any?>?, id: String): HashMap<String, Any?> {
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
    val osmIds = arrayListOf<String>()
    osmIds.add(id)
    foreign["osm_ids"] = osmIds

    return foreign
}
