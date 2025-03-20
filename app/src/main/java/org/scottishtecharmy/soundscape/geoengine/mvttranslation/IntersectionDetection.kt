package org.scottishtecharmy.soundscape.geoengine.mvttranslation

import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import kotlin.String

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
                details : IntersectionDetails
    ) {
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
                val osmIds = arrayListOf<Double>()
                val names = setOf<String>().toMutableSet()
                var namedRoadPresent = false
                for (road in intersections) {
                    if (road.brunnel != "null")
                        names.add(road.brunnel)
                    else if (road.subClass != "null")
                        names.add(road.subClass)
                    else if(road.name.isNotEmpty()){
                        names.add(road.name)
                        namedRoadPresent = true
                    }
                    else {
                        names.add(road.type)
                    }

                    osmIds.add(road.id)
                }
                // Sort our names alphabetically with capitalized names first
                val sortedNames = ArrayList(names.sortedWith(
                    compareBy<String> { it.first().isLowerCase() }.thenBy { it.lowercase() }))

                val name = StringBuilder()
                if(namedRoadPresent) {
                    for (namePart in sortedNames) {
                        if (name.isNotEmpty()) {
                            name.append("/")
                        }
                        name.append(namePart)
                    }
                }
                intersection.foreign!!["osm_ids"] = osmIds
                intersection.properties = HashMap()
                intersection.properties!!["name"] = name.toString()
                collection.addFeature(intersection)
            }
        }
    }
}

data class IntersectionDetails(
    val name : String,
    val type : String,
    val subClass : String,
    val brunnel : String,
    val id : Double,
    var lineEnd : Boolean = false
)