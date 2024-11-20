package org.scottishtecharmy.soundscape.geoengine.mvttranslation

import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point

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
                details : EntranceDetails
    ) {
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

data class EntranceDetails(
    val name : String?,
    val entranceType : String?,
    val poi: Boolean,
    val osmId : Double,
)