package org.scottishtecharmy.soundscape.geoengine.mvttranslation

import org.scottishtecharmy.soundscape.geoengine.utils.SuperCategoryId
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.geojsonparser.geojson.cloneHashMap
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

class EntranceMatching {

    /**
     * buildingNodes is a sparse map which maps from a location within the tile to a list of
     * building polygons which have nodes at that point. Every node on any `POI` polygon will appear
     * in the map along with any entrance. After processing it should be straightforward to match
     * up entrances to their POI polygons.
     */
    private val buildingNodes : HashMap< Int, ArrayList<EntranceDetails>> = hashMapOf()
    private val addedIds = mutableSetOf<Long>()

    /**
     * addGeometry is called for any buildings that are found within a tile and all entrance nodes
     * @param pointArray is either a polygon or a point
     * @param details describes the line that is being added.
     *
     */
    fun addGeometry(pointArray : ArrayList<Pair<Int, Int>>,
                   details : EntranceDetails
    ) {
        if(addedIds.contains(details.osmId)) {
            if(details.layer == null) {
                // There's no layer specified in the new details so there's no extra information
                return
            }
        }
        addedIds.add(details.osmId)

        // If we're adding a polygon don't duplicate the first point by including the last point
        val dropCount = if(pointArray.size > 1) 1 else 0
        for (point in pointArray.dropLast(dropCount)) {
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
                // Remove any previous entries for this osmID as they didn't have a level set
                for(node in buildingNodes[coordinateKey]!!) {
                    if(node.osmId == details.osmId) {
                        buildingNodes[coordinateKey]?.remove(node)
                        break
                    }
                }
                buildingNodes[coordinateKey]?.add(details.copy())
            }
        }
    }

    /**
     * generateEntrances goes through our hash map and adds an intersection feature to the
     * collection wherever it finds out.
     * @param collection is where the new intersection features are added
     * @param tileX the tile x coordinate so that the tile relative location of the intersection can
     * be turned into a latitude/longitude
     * @param tileY the tile y coordinate so that the tile relative location of the intersection can
     *      * be turned into a latitude/longitude
     */
    fun generateEntrances(collection: FeatureCollection,
                          poiMap : HashMap<Long, MutableList<Feature>>,
                          buildingMap: HashMap<Long, Feature>,
                          tileX : Int,
                          tileY : Int,
                          tileZoom : Int) {
        // Add points for the intersections that we found
        for ((key, nodes) in buildingNodes) {

            // Generate an entrance with a matching POI polygon
            var entranceDetails : EntranceDetails? = null
            for(node in nodes) {
                if(!node.poi) {
                    // We have an entrance!
                    entranceDetails = node
                    break
                }
            }

            // If we have an entrance at this point then we generate a feature to represent it
            // using the POI that it is coincident with if there is one.
            if(entranceDetails != null) {
                var poiDetails : EntranceDetails? = null
                // Where there are multiple buildings, try and match the `layer` of the entrance
                // with that of the POI. If there is no `layer` then it means that it is zero.
                // There's also the confusing factor of `layer` vs. `level`. `level` is what is
                // used inside and `layer` is really for outside use showing where roads pass
                // over or under each other. However, I've found cases of both being used for
                // entrances:
                //
                // https://www.openstreetmap.org/node/2032127103 could be Buchanan Street Galleries
                // of the Royal Concert Hall (it's the former and the layer can be used here)
                //
                // https://www.openstreetmap.org/node/2039002274 is on level 1 and matches the
                // Main Concourse, but there's all sorts of confusion here as the adjacent
                // https://www.openstreetmap.org/node/2039002279 is part of the Grand Central
                // Hotel only, so doesn't even get named as a station entrance.
                var poiCount = 0
                for (node in nodes) {
                    if (node.poi) {
                        if(node.layer == entranceDetails.layer) {
                            poiCount++
                            if (poiCount > 1) {
                                // There are multiple buildings at this point and we don't know
                                // which the entrance belongs to, so rather than be wrong, don't
                                // label it.
                                poiDetails = null
                                break
                            }
                            poiDetails = node
                        }
                    }
                }
                // We didn't find a perfect match, lets try for a match where we don't compare
                // the layers
                if(poiDetails == null) {
                    for (node in nodes) {
                        if (node.poi) {
                            poiCount++
                            if (poiCount > 1) {
                                // There are multiple buildings at this point and we don't know
                                // which the entrance belongs to, so rather than be wrong, don't
                                // label it.
                                //println("Multiple buildings found for entrance ${entranceDetails.osmId.toLong() / 10}, skipping it")
                                poiDetails = null
                                break
                            }
                            poiDetails = node
                        }
                    }
                }

                when(entranceDetails.entranceType) {
                    // Filter on the type of entrances that we want to add using values from:
                    //    https://taginfo.openstreetmap.org/keys/entrance#values
                    // Amongst others we're excluding home which are for individual homes, garage,
                    // service and emergency entrances
                    //
                    "yes",
                    "main",
                    "staircase",
                    "shop",
                    "secondary",
                    "restaurant",
                    "office",
                    "subway_entrance",
                    "entrance" -> {

                        // Turn our coordinate key back into tile relative x,y coordinates
                        val x = key.shr(12)
                        val y = key.and(0xfff)
                        // Convert the tile relative coordinate into a LatLngAlt
                        val point = arrayListOf(Pair(x, y))
                        val coordinates = convertGeometry(tileX, tileY, tileZoom, point)

                        if(poiDetails != null) {
                            // If there's no matching POI then there's no entrance feature to make
                            val poiList = poiMap[poiDetails.osmId]
                            val poi = if((poiList != null) && (poiList.isNotEmpty()))
                                poiList[0]
                            else
                                buildingMap[poiDetails.osmId]

                            if(poi != null) {
                                // We're going to duplicate the POI, but change it to being a point
                                // instead of a polygon, and add the entrance name if it has one
                                val entrance = MvtFeature()
                                entrance.copyProperties(poi as MvtFeature)
                                entrance.geometry =
                                    Point(coordinates[0].longitude, coordinates[0].latitude)
                                entrance.properties = (cloneHashMap(poi.properties) ?: HashMap()).apply {
                                    set("entrance", entranceDetails.entranceType)

                                    // This is an entrance, so remove any marking that the POI has
                                    remove("has_entrances")

                                    if (entranceDetails.name != null)
                                        set( "entrance_name", entranceDetails.name)
                                }
                                collection.addFeature(entrance)
                                //println("POI entrance: ${entrance.properties?.get("name")} ${entranceDetails.entranceType} ${entranceDetails.osmId} ")

                                // We're also going to mark the POI to indicate that it has entrances.
                                // This will be used by PlacesNearby so that it will only display
                                // the entrances and not the POI itself.
                                poi.setProperty("has_entrances", "yes")
                                continue
                            }
                        }
                        // Try and figure out how to name the entrance from its properties.
                        val entrance = MvtFeature()
                        entrance.geometry =
                            Point(coordinates[0].longitude, coordinates[0].latitude)
                        entrance.properties = HashMap()

                        var confected = (entranceDetails.name != null)
                        if(entranceDetails.entranceType == "subway_entrance") {
                            // Subway station entrances
                            entrance.featureClass = "railway"
                            entrance.featureSubClass = "subway"
                            entrance.featureType = "railway"
                            entrance.featureValue = "subway"
                            entrance.superCategory = SuperCategoryId.MOBILITY
                            confected = true
                        }
                        else if((entranceDetails.properties?.get("railway") == "train_station_entrance") ||
                                (entranceDetails.properties?.get("railway") == "entrance")) {
                            // Train station entrances
                            entrance.featureClass = "railway"
                            entrance.featureSubClass = "station"
                            entrance.featureType = "railway"
                            entrance.featureValue = "station"
                            entrance.superCategory = SuperCategoryId.MOBILITY
                            confected = true
                        }
                        if(confected)  {
                            entrance.setProperty("entrance", entranceDetails.entranceType)
                            entrance.name = entranceDetails.name
                            entrance.superCategory = SuperCategoryId.PLACE
                            collection.addFeature(entrance)
                            //println("Confected Entrance: ${entrance.name} ${entranceDetails.entranceType} ${entranceDetails.osmId} ${entrance.featureClass} ${entrance.featureSubClass}")
                        }
                    }
                    else -> {
                        // Ignore other types of entrance for now
                    }
                }
            }
        }
    }
}

data class EntranceDetails(
    val name : String?,
    val entranceType : String?,
    val layer: String?,
    val properties: HashMap<String, Any?>?,
    val poi: Boolean,
    val osmId : Long,
)