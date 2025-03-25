package org.scottishtecharmy.soundscape.geoengine.mvttranslation

import org.scottishtecharmy.soundscape.geoengine.utils.toRadians
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.geojsonparser.moshi.GeoJsonObjectMoshiAdapter
import java.io.FileOutputStream
import kotlin.collections.set
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asinh
import kotlin.math.tan
import kotlin.math.truncate

enum class IntersectionType(
    val id: Int,
) {
    REGULAR(0),
    JOINER(1),
    TILE_EDGE(2)
}

class Intersection {
    var members: MutableList<Way> = emptyList<Way>().toMutableList()    // Ways that make up this intersection
    var name = ""                                                       // Name of the intersection
    var location = LngLatAlt()                                          // Location of the intersection
    var type = IntersectionType.REGULAR
}

class Way {
    var segment: List<Feature> = emptyList()    // List of Features that make up the way (often just 1)
    var length = 0.0                            // We could easily calculate this from the segments.

    var startIntersection: Intersection? = null // Intersections at either end
    var endIntersection: Intersection? = null
}

fun convertBackToTileCoordinates(location: LngLatAlt,
                                 tileX : Int,
                                 tileY : Int,
                                 tileZoom : Int) : Pair<Int, Int> {


    val latRad = toRadians(location.latitude)
    var x = ((location.longitude + 180.0) / 360.0) * (1 shl tileZoom)
    var y = (1 shl tileZoom) * (1.0 - asinh(tan(toRadians(location.latitude))) / PI) / 2

    var xInt = (abs(x - truncate(x)) * 4096).toInt()
    var yInt = (abs(y - truncate(y)) * 4096).toInt()

    return Pair(xInt, yInt)
}

class WayGenerator {

    /**
    * highwayPoints is a sparse map which maps from a location within the tile to a list of
    * lines which have nodes at that point. Every node on any `transportation` line will appear in the
    * map and if after processing all of the lines there's an intersection at that point, the map
     * entry will have information for more than one line.
    */
    private val highwayNodes : HashMap< Int, Int> = hashMapOf()
    private val wayFeatures : MutableList<Feature> = emptyList<Feature>().toMutableList()

    private val ways : MutableList<Way> = emptyList<Way>().toMutableList()

    private val intersections : HashMap<Int, Intersection> = hashMapOf()

    /**
     * addLine is called for any line feature that is being added to the FeatureCollection.
     * @param line is a new `transportation` layer line to add to the map
     *
     */
    fun addLine(line : ArrayList<Pair<Int, Int>>) {
        for (point in line) {
            if((point.first < 0) || (point.first > 4095) ||
                (point.second < 0) || (point.second > 4095)) {
                continue
            }

            // Rather than have a 2D sparse array, turn the coordinates into a single int so that we
            // can have a 1D sparse array instead.
            val coordinateKey = point.first.shl(12) + point.second
            val currentCount = highwayNodes[coordinateKey]
            if (currentCount == null) {
                highwayNodes[coordinateKey] = 1
            }
            else {
                highwayNodes[coordinateKey] = currentCount + 1
            }
        }
    }

    fun addFeature(feature: Feature) {
        wayFeatures.add(feature)
    }
    /**
    *  Inside generateIntersections, first traverse every line that was added and generate a new
    *  segment Feature at every intersection that we hit. Add these to Ways as we go. Intersections are spotted using the
    *  coordinate key (x + shr(y)). Put those features in two HashMaps a 'start' an 'end' one, again
    *  keyed by the coordinate key. Once we've traversed all of the lines we should have a Way for
    *  every segment between intersections. Now we generate the intersections and add the Ways directly
    *  to them. Let's do this in a separate class for now so that we can test it.
    */
    fun addSegmentFeatureToWay(feature: Feature,
                               currentSegment: LineString,
                               segmentIndex: Int,
                               way: Way) {
        // Add feature with the segment up until this point
        val newFeature = Feature()
        feature.properties?.let { properties ->
            newFeature.properties = hashMapOf<String, Any?>()
            for((key, prop) in properties) {
                newFeature.properties!![key] = prop
            }
            newFeature.properties?.set("segmentIndex", segmentIndex.toString())
        }
        feature.foreign?.let { foreign ->

            newFeature.foreign = hashMapOf<String, Any?>()
            for((key, prop) in foreign) {
                newFeature.foreign!![key] = prop
            }
        }
        newFeature.geometry = currentSegment
        way.segment = listOf(newFeature)
    }

    fun generateWays(intersectionCollection: FeatureCollection,
                     waysCollection: FeatureCollection,
                     tileX : Int, tileY : Int, tileZoom : Int) {

        for(feature in wayFeatures) {
            if(feature.geometry.type == "LineString") {
                val line = feature.geometry as LineString
                var currentWay = Way()
                var currentSegment = LineString()
                var segmentIndex = 0
                var coordinateKey : Int = 0
                for (coordinate in line.coordinates) {

                    currentSegment.coordinates.add(coordinate)

                    // Is this coordinate at an intersection?
                    val tileCoordinates =
                        convertBackToTileCoordinates(coordinate, tileX, tileY, tileZoom)
                    coordinateKey = tileCoordinates.first.shl(12) + tileCoordinates.second
                    highwayNodes[coordinateKey]?.let {
                        if (it > 1) {
                            // Create an intersection if we don't have one already
                            var intersection = intersections.get(coordinateKey)
                            if(intersection == null) {
                                intersection = Intersection()
                                intersection.name = ""
                                intersection.location = coordinate
                                intersection.type = IntersectionType.REGULAR
                                intersections[coordinateKey] = intersection
                            }

                            if(currentSegment.coordinates.size > 1) {
                                addSegmentFeatureToWay(
                                    feature,
                                    currentSegment,
                                    segmentIndex,
                                    currentWay
                                )
                                ++segmentIndex
                                currentWay.endIntersection = intersection
                                ways.add(currentWay)

                                // Add completed way to intersection at end and at start if there is one
                                intersection.members.add(currentWay)
                                currentWay.startIntersection?.members?.add(currentWay)

                                // Reset the segment accumulator
                                currentSegment = LineString()
                                currentSegment.coordinates.add(coordinate)
                            }

                            // Create a new Way feature for the upcoming segment
                            currentWay = Way().also { way ->
                                way.startIntersection = intersection
                            }
                        }
                    }
                }

                if(currentSegment.coordinates.size > 1) {
                    addSegmentFeatureToWay(feature, currentSegment, segmentIndex, currentWay)
                    ways.add(currentWay)
                    // Add completed way to intersection at start if there is one
                    if(currentWay.startIntersection != null) {
                        currentWay.startIntersection!!.members.add(currentWay)
                    }
                }
            }
        }
        for(way in ways) {
            for (segment in way.segment) {
                waysCollection.addFeature(segment)
            }
        }

        for(intersection in intersections) {

            // Name the intersection
            val name = StringBuilder()
            val osmIds = arrayListOf<Double>()
            for(way in intersection.value.members) {
                if(name.isNotEmpty()) {
                    name.append("/")
                }
                val segment = way.segment[0]
                var segmentName = segment.properties?.get("name")
                if(segmentName == null) {
                    segmentName = segment.properties?.get("subClass")
                }
                val segmentIndex = segment.properties?.get("segmentIndex")
                name.append("$segmentName $segmentIndex")

                val id = segment.properties?.get("osm_ids").toString().toDouble()
                osmIds.add(id)
            }
            intersection.value.name = name.toString()

            val feature = Feature()
            feature.geometry = Point(intersection.value.location.longitude, intersection.value.location.latitude)
            feature.properties = hashMapOf<String, Any?>()
            feature.properties?.set("name", intersection.value.name)
            feature.foreign = hashMapOf<String, Any?>()
            feature.foreign?.set("feature_type", "highway")
            feature.foreign?.set("feature_value", "gd_intersection")
            feature.foreign?.set("osm_ids", osmIds)
            intersectionCollection.addFeature(feature)
        }

//        val fc = FeatureCollection()
//        fc.features.addAll(waysCollection.features)
//        fc.features.addAll(intersectionCollection.features)
//        val adapter = GeoJsonObjectMoshiAdapter()
//        val outputFile = FileOutputStream("graph.geojson")
//        outputFile.write(adapter.toJson(fc).toByteArray())
//        outputFile.close()
    }
}