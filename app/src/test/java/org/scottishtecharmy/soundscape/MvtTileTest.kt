package org.scottishtecharmy.soundscape

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.scottishtecharmy.soundscape.MainActivity.Companion.MOBILITY_KEY
import org.scottishtecharmy.soundscape.MainActivity.Companion.PLACES_AND_LANDMARKS_KEY
import org.scottishtecharmy.soundscape.geoengine.GRID_SIZE
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.PositionedString
import org.scottishtecharmy.soundscape.geoengine.ProtomapsGridState
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.MAX_ZOOM_LEVEL
import org.scottishtecharmy.soundscape.geoengine.callouts.AutoCallout
import org.scottishtecharmy.soundscape.geoengine.filters.MapMatchFilter
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.convertBackToTileCoordinates
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.sampleToFractionOfTile
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.vectorTileToGeoJson
import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geoengine.utils.confectNamesForRoad
import org.scottishtecharmy.soundscape.geoengine.utils.getDistanceToFeature
import org.scottishtecharmy.soundscape.geoengine.utils.getLatLonTileWithOffset
import org.scottishtecharmy.soundscape.geoengine.utils.searchFeaturesByName
import org.scottishtecharmy.soundscape.geoengine.utils.traverseIntersectionsConfectingNames
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.geojsonparser.moshi.GeoJsonObjectMoshiAdapter
import vector_tile.VectorTile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.sequences.forEach
import kotlin.system.measureTimeMillis
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Intersection
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.geoengine.utils.createPolygonFromTriangle
import org.scottishtecharmy.soundscape.geoengine.utils.getFovTriangle
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import kotlin.io.path.Path
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension

/**
 * FileGridState overrides ProtomapsGridState updateTile to get tiles from test resources instead of
 * over the network. It also sets validateContext to false as it assumes that the tests are all
 * running in a single context.
 */
class FileGridState(
    zoomLevel: Int = MAX_ZOOM_LEVEL,
    gridSize: Int = GRID_SIZE) : ProtomapsGridState(zoomLevel, gridSize) {

    init {
        validateContext = false
    }

    /**
     * updateTile is overrider in FileGridState to get the tile data from the unit test resources
     * directory.
     */
    override suspend fun updateTile(
        x: Int,
        y: Int,
        featureCollections: Array<FeatureCollection>,
        intersectionMap: HashMap<LngLatAlt, Intersection>
    ): Boolean {
        var ret = false
        val path = "src/test/res/org/scottishtecharmy/soundscape/"
        try {
            val remoteTile = FileInputStream(path + "${x}x${y}x${zoomLevel}.mvt")
            val result = VectorTile.Tile.parseFrom(remoteTile)
            if (result != null) {
                val tileFeatureCollection = vectorTileToGeoJson(
                    tileX = x,
                    tileY = y,
                    mvt = result,
                    intersectionMap = intersectionMap,
                    tileZoom = zoomLevel)
                val collections = processTileFeatureCollection(tileFeatureCollection)

                for ((index, collection) in collections.withIndex()) {
                    featureCollections[index].plusAssign(collection)
                }

                ret = true
            } else {
                println("Failed to load tile from file")
            }
        } catch(e:Exception) {
            println("$e")
        }
        return ret
    }
}

private fun vectorTileToGeoJsonFromFile(
    tileX: Int,
    tileY: Int,
    filename: String,
    intersectionMap:  HashMap<LngLatAlt, Intersection>,
    cropPoints: Boolean = true
): FeatureCollection {

    val path = "src/test/res/org/scottishtecharmy/soundscape/"
    val remoteTile = FileInputStream(path + filename)
    val tile: VectorTile.Tile = VectorTile.Tile.parseFrom(remoteTile)

    return vectorTileToGeoJson(tileX, tileY, tile, intersectionMap, cropPoints, MAX_ZOOM_LEVEL)
}

private fun parseGpxFromFile(filename: String): FeatureCollection {
    val fc = FeatureCollection()

    var currentFeature = Feature()
    File(filename).useLines { lines ->
        lines.forEach { line ->

            // Get the location
            val regex = Regex("/*<trkpt.*lat=\"(.*)\" lon=\"(.*)\".*")
            val matchResult = regex.find(line)
            if (matchResult != null) {
                // We have a match on the link
                val latitudeString = matchResult.groupValues[1]
                val longitudeString = matchResult.groupValues[2]

                val latitude = latitudeString.toDouble()
                val longitude = longitudeString.toDouble()

                if(currentFeature.properties != null) {
                    fc.addFeature(currentFeature)
                }

                currentFeature = Feature()
                currentFeature.geometry = Point(longitude, latitude)
                currentFeature.properties = hashMapOf()
                currentFeature.properties?.set("marker-size", "small")
                currentFeature.properties?.set("marker-color", "#004000")

            } else {
                val regex2 = Regex("/*<bearing>(.*)</bearing>.*")
                val matchResult2 = regex2.find(line)
                if (matchResult2 != null) {
                    currentFeature.properties?.set("heading", matchResult2.groupValues[1].toDouble())
                }
                else {
                    val regex3 = Regex("/*<speed>(.*)</speed>.*")
                    val matchResult3 = regex3.find(line)
                    if (matchResult3 != null) {
                        currentFeature.properties?.set(
                            "speed",
                            matchResult3.groupValues[1].toDouble()
                        )
                    }
                    else {
                        val regex4 = Regex("/*<time>(.*)</time>.*")
                        val matchResult4 = regex4.find(line)
                        if (matchResult4 != null) {
                            currentFeature.properties?.set(
                                "time",
                                matchResult4.groupValues[1].toDouble()
                            )
                        }
                    }
                }
            }
        }
    }

    return fc
}

fun getGridStateForLocation(
    location: LngLatAlt,
    zoomLevel: Int,
    gridSize: Int
): GridState {

    val gridState = FileGridState(zoomLevel, gridSize)
    runBlocking {

        val enabledCategories = emptySet<String>().toMutableSet()
        enabledCategories.add(PLACES_AND_LANDMARKS_KEY)
        enabledCategories.add(MOBILITY_KEY)

        // Update the grid state
        gridState.locationUpdate(
            LngLatAlt(location.longitude, location.latitude),
            enabledCategories,
            true
        )
    }
    return gridState
}

class MvtTileTest {

    @Test
    fun simpleVectorTile() {

        // This test does the simplest vector tile test:
        //
        //  1. It gets a vector tile from the protomaps server
        //  2. Parses it with the code auto-generated from the vector_tile.proto specification
        //  3. Prints it out
        val path = "src/test/res/org/scottishtecharmy/soundscape/"
        val remoteTile = FileInputStream(path + "16093x10211x15.mvt")
        val tile: VectorTile.Tile = VectorTile.Tile.parseFrom(remoteTile)
        assert(tile.layersList.isNotEmpty())

        for (layer in tile.layersList) {
            println("Layer ${layer.name}")
        }
    }

    @Test
    fun pixelToLocation() {
        val tileX = 15992
        val tileY = 10212
        val tileZoom = 15

        val tileOrigin2 = getLatLonTileWithOffset(tileX, tileY, tileZoom, 0.0, 0.0)
        println("tileOrigin2 " + tileOrigin2.latitude + "," + tileOrigin2.longitude)
        assert(tileOrigin2.latitude == 55.94919982336745)
        assert(tileOrigin2.longitude == -4.306640625)
    }

    @Test
    fun testVectorToGeoJsonMilngavie() {
        val intersectionMap: HashMap<LngLatAlt, Intersection> = hashMapOf()
        val geojson = vectorTileToGeoJsonFromFile(15991, 10212, "15991x10212x15.mvt", intersectionMap)
        val adapter = GeoJsonObjectMoshiAdapter()

        val outputFile = FileOutputStream("milngavie.geojson")
        outputFile.write(adapter.toJson(geojson).toByteArray())
        outputFile.close()
    }

    @Test
    fun testVectorToGeoJsonEdinburgh() {
        val intersectionMap: HashMap<LngLatAlt, Intersection> = hashMapOf()
        val geojson = vectorTileToGeoJsonFromFile(16093, 10211, "16093x10211x15.mvt", intersectionMap)
        val adapter = GeoJsonObjectMoshiAdapter()

        val outputFile = FileOutputStream("edinburgh.geojson")
        outputFile.write(adapter.toJson(geojson).toByteArray())
        outputFile.close()
    }

    @Test
    fun testVectorToGeoJsonByresRoad() {
        val intersectionMap: HashMap<LngLatAlt, Intersection> = hashMapOf()
        val geojson = vectorTileToGeoJsonFromFile(15992, 10223, "15992x10223x15.mvt", intersectionMap)
        val adapter = GeoJsonObjectMoshiAdapter()

        val outputFile = FileOutputStream("byresroad.geojson")
        outputFile.write(adapter.toJson(geojson).toByteArray())
        outputFile.close()
    }

    /** This test reads in a 2x2 array of vector tiles and merges them into a single GeoJSON.
     * That's then saved off to a file for a visual check. There's no joining up of lines but
     * because there are no intersections between the roads from separate tiles the GeoJSON
     * processing code isn't really any the wiser.
     * However, POIs have to be de-duplicated to avoid multiple all outs. The two ways to do this
     * are:
     *  1. Check for duplicates as we merge
     *  2. Crop out POIs which are outside the tile during initial importing
     * Initially going with option 2 as that's the cheapest and it's not clear why would ever want
     * POI that are outwith the tile boundaries.
     *
     *
     * When using soundscape-backend the tile array used is 3x3, but those tiles are at a higher
     * zoom level. The vector tiles are twice the width/height and so a 2x2 array can be used with
     * the array moving when the user location leaves the center.
     */
    @Test
    fun testVectorToGeoJsonGrid() {

        // Make a large grid to aid analysis
        val gridState = getGridStateForLocation(LngLatAlt(-4.317357, 55.942527), MAX_ZOOM_LEVEL, 2)

        // Check that the de-duplication of the points worked (without that there are two points
        // for Graeme Pharmacy, one each from two separate tiles).
        val searchResults = searchFeaturesByName(
            gridState.featureTrees[TreeId.POIS.id].getAllCollection(),
            "Graeme")

        val adapter = GeoJsonObjectMoshiAdapter()
        println(adapter.toJson(searchResults))
        assert(searchResults.features.size == 1)

        // Check that we can find the containing polygons for a point
        val tree = gridState.featureTrees[TreeId.POIS.id]
        val fc1 = tree.getContainingPolygons(LngLatAlt(-4.316401, 55.939941))
        assert(fc1.features.size == 1)
        assert(fc1.features[0].properties?.get("name") == "Tesco Customer Car Park")

        val fc2 = tree.getContainingPolygons(LngLatAlt(-4.312885, 55.942237))
        assert(fc2.features.size == 1)
        assert(fc2.features[0].properties?.get("name") == "Milngavie Town Hall")

//        val fc3 = tree.getContainingPolygons(LngLatAlt(-4.296998, 55.948270))
//        assert(fc3.features.size == 2)
//        assert(fc3.features[0].properties?.get("name") == "Milngavie Fitness & Wellbeing Gym")
//        assert(fc3.features[1].properties?.get("class") == "parking")

        val fc4 = tree.getContainingPolygons(LngLatAlt(-4.316641241312027,55.94160200415631))
        assert(fc4.features.size == 1)

        val outputCollection = gridState.featureTrees[TreeId.ROADS_AND_PATHS.id].getAllCollection()
        for(intersection in gridState.gridIntersections) {
            intersection.value.toFeature()
            outputCollection.addFeature(intersection.value)
        }

        val outputFile = FileOutputStream("2x2.geojson")
        outputFile.write(adapter.toJson(outputCollection).toByteArray())
        outputFile.close()
    }

    /**
     * This test generates a FeatureCollection containing un-named roads and paths that we managed
     * to generate our own names for. The priority for naming is:
     *  1. Sidewalks
     *  2. Road destinations
     *  3. POI destinations
     *  4. Dead ends
     *
     * Once we add water and railways, we can consider adding 'along canal' and 'along railway' type
     * descriptions too.
     */
    @Test
    fun testNameConfection() {
        val userGeometry = UserGeometry(LngLatAlt(-4.313, 55.945245))
        val gridState = getGridStateForLocation(userGeometry.location, MAX_ZOOM_LEVEL, 2)

        val confectionTime = measureTimeMillis {
            traverseIntersectionsConfectingNames(gridState.gridIntersections)
        }

        var roads = gridState.getFeatureCollection(TreeId.ROADS_AND_PATHS)
        val confectionTime2 = measureTimeMillis {
            for (road in roads) {
                confectNamesForRoad(road, gridState)
            }
        }
        println("Confection time: $confectionTime ms")
        println("Confection time2: $confectionTime2 ms")

        roads = gridState.getFeatureCollection(TreeId.ROADS_AND_PATHS)
        val adapter = GeoJsonObjectMoshiAdapter()
        val outputFile = FileOutputStream("confected-names.geojson")
        outputFile.write(adapter.toJson(roads).toByteArray())
        outputFile.close()
    }

    @Test
    fun testRtree() {

        // Make a large grid to aid analysis
        val featureCollection = FeatureCollection()
        for (x in 15990..15992) {
            for (y in 10212..10213) {
                val intersectionMap: HashMap<LngLatAlt, Intersection> = hashMapOf()
                val geojson = vectorTileToGeoJsonFromFile(x, y, "${x}x${y}x15.mvt", intersectionMap)
                for (feature in geojson) {
                    featureCollection.addFeature(feature)
                }
            }
        }

        // Iterate through all of the features and add them to an Rtree
        var start = System.currentTimeMillis()
        val tree = FeatureTree(featureCollection)
        var end = System.currentTimeMillis()

        // Prove that we can edit the feature property in the original collection and it affects
        // the contents of the rtree. We don't really want this behaviour, but it's what we have.
        for(feature in featureCollection) {
            if(feature.properties?.get("name") == "Blane Drive") {
                feature.properties!!["name"] = "Blah Drive"
            }
        }

        // We have all the points in an rtree
        println("Tree size: ${tree.tree!!.size()}, depth ${tree.tree!!.calculateDepth()} - ${end-start}ms")
        //tree.tree!!.visualize(4096,4096).save("tree.png");

        start = System.currentTimeMillis()
        val distanceFc = tree.getNearbyCollection(LngLatAlt(-4.3058322, 55.9473305), 20.0, CheapRuler(55.9473305))
        end = System.currentTimeMillis()
        println("Search (${end-start}ms):")
        for(feature in distanceFc) {
            println(feature.properties?.get("name"))
        }

        start = System.currentTimeMillis()
        val nearestFc = tree.getNearestFeature(LngLatAlt(-4.316914, 55.941861), CheapRuler(55.9473305), 50.0)
        end = System.currentTimeMillis()
        println("Nearest (${end-start}ms):")
        println(nearestFc?.properties?.get("name"))

        val adapter = GeoJsonObjectMoshiAdapter()
        val outputFile = FileOutputStream("rtree.geojson")
        outputFile.write(adapter.toJson(distanceFc).toByteArray())
        outputFile.close()
    }

    @Test
    fun testObjects() {
        // This test is to show how Kotlin doesn't copy objects by default. featureCopy isn't a copy
        // as it might be in C++, but a reference to the same object. There's no copy() defined
        // for Feature. This means that in all the machinations with FeatureCollections, the Features
        // underlying them are the same ones. So long as they are not changed then this isn't a
        // problem, but we do add "distance_to".

        val intersectionMap: HashMap<LngLatAlt, Intersection> = hashMapOf()
        val featureCollection = vectorTileToGeoJsonFromFile(15990, 10212, "15990x10212x15.mvt", intersectionMap)
        println(featureCollection.features[0].id)
        val newFeatureCollection = FeatureCollection()
        newFeatureCollection.plusAssign(featureCollection)
        val featureReference = featureCollection.features[0]
        featureReference.id = "Blah"
        println(featureCollection.features[0].id)
        println(newFeatureCollection.features[0].id)
        println(featureReference.id)
        assert(featureCollection.features[0].id == newFeatureCollection.features[0].id)
        assert(featureReference.id == newFeatureCollection.features[0].id)

        // Copy
        val copyFeatureCollection = FeatureCollection()
        copyFeatureCollection.features = newFeatureCollection.features.clone() as ArrayList<Feature>
        println(copyFeatureCollection.features[0].id)

        // newFeatureCollection is new, but the features that it contains are not
        newFeatureCollection.features.clear()
        println(newFeatureCollection.features.size)

        // It's actually not possible to easily copy a Feature. What about a simple hashmap?
        val map = hashMapOf<Int, String>()
        map[0] = "Zero"
        map[1] = "One"
        map[2] = "Two"

        val mapCopy = map.clone() as HashMap<*, *>
        for (entry in mapCopy) {
            println(entry.value)
        }
        map[0] = "Not zero?"
        for (entry in mapCopy) {
            println(entry.value)
        }
        for (entry in map) {
            println(entry.value)
        }

        // Clone is cloning all of the hashmap entries
    }


    @Test
    fun testRoadBearing(){
        val userGeometry = UserGeometry(LngLatAlt(-4.313, 55.945245))
        val gridState = getGridStateForLocation(userGeometry.location, MAX_ZOOM_LEVEL, 2)

        val roadTree = gridState.getFeatureTree(TreeId.ROADS)
        val nearestRoad = roadTree.getNearestFeature(userGeometry.location, userGeometry.ruler)

        println(nearestRoad.toString())
    }

    @Test
    fun testNearestRoadIdeas() {

        val gridState = getGridStateForLocation(LngLatAlt(-4.31029, 55.94583), MAX_ZOOM_LEVEL, 2)
        val geojson = FeatureCollection()

        val heading = 180.0
        var latitude = 55.945219
        while(latitude < 55.94583) {
            var longitude = -4.311362
            var lastNearestRoad: Feature? = null
            while(longitude < -4.31029) {

                val location = LngLatAlt(longitude, latitude)
                val sensedNearestRoads = gridState.getFeatureTree(TreeId.ROADS_AND_PATHS)
                    .getNearestCollection(location, 20.0, 10, gridState.ruler)

                var bestIndex = -1
                var bestFitness = 0.0
                for ((index, sensedRoad) in sensedNearestRoads.withIndex()) {
                    val sensedRoadInfo = getDistanceToFeature(location, sensedRoad, gridState.ruler)
                    var headingOffSensedRoad =
                        abs((heading % 180) - (sensedRoadInfo.heading % 180))
                    if(headingOffSensedRoad > 90)
                        headingOffSensedRoad = 180 - headingOffSensedRoad

                    // We want to decide based on distance and direction. This calculation gives
                    // a reasonable road as a result from only a point and a heading - no history.
                    // The actual nearest road function could use the nearest road history to
                    // decide on whether to stick with the individual road or not.
                    val w1 = 300.0
                    val w2 = 100.0
                    val fitness = (w1 * (10 / (10 + sensedRoadInfo.distance))) +
                                  (w2 * (30 / (30 + headingOffSensedRoad)))
                    if(fitness > bestFitness) {
                        bestFitness = fitness
                        bestIndex = index
                    }
                }
                if(sensedNearestRoads.features.isNotEmpty()) {
                    val bestMatch = sensedNearestRoads.features[bestIndex]
                    if(bestMatch != lastNearestRoad) {
                        val geoPointFeature = Feature()
                        val pointGeometry = Point(location.longitude, location.latitude)
                        geoPointFeature.geometry = pointGeometry
                        val foreign: HashMap<String, Any?> = hashMapOf()
                        foreign["nearestRoad"] = bestMatch.properties?.get("name")
                        foreign["direction"] = heading
                        geoPointFeature.properties = foreign
                        geojson.addFeature(geoPointFeature)
                    }

                    lastNearestRoad = bestMatch
                }

                longitude += 0.00001
            }
            latitude += 0.00001
        }
        val adapter = GeoJsonObjectMoshiAdapter()
        val outputFile = FileOutputStream("nearest.geojson")
        outputFile.write(adapter.toJson(geojson).toByteArray())
        outputFile.close()
    }

    @Test fun testConvertBackToTileCoordinates() {

        val tileX = 10000
        val tileY = 16000
        val tileZoom = 15

        for(testX in 0 until 4096) {
            for (testY in 0 until 4096) {
                val location = getLatLonTileWithOffset(
                    tileX,
                    tileY,
                    tileZoom,
                    sampleToFractionOfTile(testX),
                    sampleToFractionOfTile(testY)
                )

                val result = convertBackToTileCoordinates(location, tileZoom)
                assert(result.first ==testX)
                assert(result.second ==testY)
            }
        }
    }

    fun testMovingGrid(gpxFilename: String, calloutFilename: String, geojsonFilename: String) {

        val gridState = FileGridState()
        val settlementGrid = FileGridState(12, 3)
        val mapMatchFilter = MapMatchFilter()
        val gps = parseGpxFromFile(gpxFilename)
        val collection = FeatureCollection()
        val startIndex = 0
        val endIndex = gps.features.size
        val autoCallout = AutoCallout(null, null)
        val callOutText = FileOutputStream(calloutFilename)

        val enabledCategories = emptySet<String>().toMutableSet()
        enabledCategories.add(PLACES_AND_LANDMARKS_KEY)
        enabledCategories.add(MOBILITY_KEY)

        val markers = FeatureCollection()
        val marker = Feature()
        marker.geometry = Point(-4.3095570, 55.9498421)
        val properties = java.util.HashMap<String, Any?>()
        properties["name"] = "Marker 1"
        marker.properties = properties
        markers.addFeature(marker)
        gridState.markerTree = FeatureTree(markers)

        gps.features.filterIndexed {
                index, _ -> (index > startIndex) and (index < endIndex)
        }.forEachIndexed { index, position ->
            val location = (position.geometry as Point).coordinates
            runBlocking {
                // Update the grid state
                val gridChanged = gridState.locationUpdate(
                    LngLatAlt(location.longitude, location.latitude),
                    enabledCategories,
                    true
                )
                settlementGrid.locationUpdate(
                    LngLatAlt(location.longitude, location.latitude),
                    emptySet(),
                    true
                )

                if(gridChanged) {
                    // As we're here, test the name confection for the grids. This is relatively
                    // expensive and is only done on individual Ways as needed when running the app.
                    val roads = gridState.getFeatureCollection(TreeId.ROADS_AND_PATHS)
                    for (road in roads) {
                        confectNamesForRoad(road, gridState)
                    }
                }

                // Update the nearest road filter with our new location
                val mapMatchedResult = mapMatchFilter.filter(
                    LngLatAlt(location.longitude, location.latitude),
                    gridState,
                    collection,
                    false
                )

                if(mapMatchedResult.first != null) {
                    val newFeature = Feature()
                    newFeature.geometry = Point(mapMatchedResult.first!!.longitude, mapMatchedResult.first!!.latitude)
                    newFeature.properties = hashMapOf()
                    newFeature.properties?.set("marker-color", mapMatchedResult.third)
                    newFeature.properties?.set("color", mapMatchedResult.third)
                    newFeature.properties?.set("index", index + startIndex)
                    collection.addFeature(newFeature)
                }
                // Add raw GPS too
                position.properties?.set("index", index + startIndex)
                collection.addFeature(position)

                val userGeometry = UserGeometry(
                    location = LngLatAlt(location.longitude, location.latitude),
                    travelHeading = position.properties?.get("heading") as Double?,
                    speed = position.properties?.get("speed") as Double,
                    mapMatchedWay = mapMatchFilter.matchedWay,
                    mapMatchedLocation = mapMatchFilter.matchedLocation,
                    timestampMilliseconds = (position.properties?.get("time") as Double).toLong()
                )

                val callout = autoCallout.updateLocation(userGeometry, gridState, settlementGrid)
                if(callout != null) {
                    // We've got a new callout, so add it to our geoJSON as a triangle for the
                    // FOV that was used to create it, along with the text from the callouts.
                    val polygon = createPolygonFromTriangle(getFovTriangle(userGeometry, true))
                    val fovFeature = Feature()
                    fovFeature.geometry = polygon
                    fovFeature.properties = hashMapOf()
                    callOutText.write("\nCallout\n".toByteArray())
                    for (positionedString in callout.positionedStrings.withIndex()) {
                        callOutText.write("\t${positionedString.value.text}\n".toByteArray())
                        fovFeature.properties?.set(
                            "Callout ${positionedString.index}",
                            positionedString.value.text
                        )
                    }
                    collection.addFeature(fovFeature)

                    callout.calloutHistory?.add(callout)
                    callout.locationFilter?.update(callout.userGeometry)
                }
            }
        }
        callOutText.close()

        val adapter = GeoJsonObjectMoshiAdapter()
        val mapMatchingOutput = FileOutputStream(geojsonFilename)
        mapMatchingOutput.write(adapter.toJson(collection).toByteArray())
        mapMatchingOutput.close()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testCallouts() {

        val directoryPath = Path("src/test/res/org/scottishtecharmy/soundscape/gpxFiles/")


        val resultsStoragePath =  "gpxFiles/"
        val resultsStorageDir = File(resultsStoragePath)
        if (!resultsStorageDir.exists()) {
            resultsStorageDir.mkdirs()
        }

        val directoryEntries = directoryPath.listDirectoryEntries("*.gpx")
        for(file in directoryEntries) {
            testMovingGrid(file.toString(), "gpxFiles/${file.nameWithoutExtension}.txt", "gpxFiles/${file.nameWithoutExtension}.geojson")
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testGridCache() {

        // This test 'moves' from the center of one tile to the center of the next to see how tile
        // caching behaves. We're using Edinburgh which we already have tile from 16092-16096 and
        // 10209-10214 i.e. 30 tiles in total in 20 grids as each grid has 4 tiles in it.

        val gridState = FileGridState()

        // The center of each grid
        for(x in 16093 until 16097) {
            for (y in 10210 until 10215) {

                // Get top left of tile
                val location = getLatLonTileWithOffset(x, y, MAX_ZOOM_LEVEL, 0.0, 0.0)

                println("Moving grid to $location")

                runBlocking {
                    // Update the grid state
                    gridState.locationUpdate(
                        LngLatAlt(location.longitude, location.latitude),
                        emptySet(),
                        true
                    )
                }
            }
        }
        val adapter = GeoJsonObjectMoshiAdapter()
        val mapMatchingOutput = FileOutputStream("total-output.geojson")

        // Output the GeoJson and check that there's no data left from other tiles.
        val collection = gridState.getFeatureCollection(TreeId.ROADS_AND_PATHS)
        collection.plusAssign(gridState.getFeatureCollection(TreeId.INTERSECTIONS))
        collection.plusAssign(gridState.getFeatureCollection(TreeId.POIS))
        mapMatchingOutput.write(adapter.toJson(collection).toByteArray())
        mapMatchingOutput.close()

    }

    @Test
    fun testLowerZoomLevel() {

        val zoomLevel = 12

        // Make a 3x3 grid at a lower zoom level. This will just contain the 'places' layer which
        // will allow searching for nearby suburbs etc.
        //val gridState = getGridStateForLocation(LngLatAlt(-4.317357, 55.942527), zoomLevel, 3)
        val gridState = getGridStateForLocation(LngLatAlt(-4.25391, 55.86226), zoomLevel, 3)


        val adapter = GeoJsonObjectMoshiAdapter()
        val cityCollection = gridState.featureTrees[TreeId.SETTLEMENT_CITY.id].getAllCollection()
        for(feature in cityCollection) {
            feature.properties?.set("marker-size", "large")
            feature.properties?.set("marker-color", "#ff0000")
        }
        val townCollection = gridState.featureTrees[TreeId.SETTLEMENT_TOWN.id].getAllCollection()
        for(feature in townCollection) {
            feature.properties?.set("marker-size", "medium")
            feature.properties?.set("marker-color", "#ffff00")
        }
        val villageCollection = gridState.featureTrees[TreeId.SETTLEMENT_VILLAGE.id].getAllCollection()
        for(feature in villageCollection) {
            feature.properties?.set("marker-size", "small")
            feature.properties?.set("marker-color", "#00ff00")
        }
        val hamletCollection = gridState.featureTrees[TreeId.SETTLEMENT_HAMLET.id].getAllCollection()
        for(feature in hamletCollection) {
            feature.properties?.set("marker-size", "small")
            feature.properties?.set("marker-color", "#0000ff")
        }
        val outputCollection = cityCollection
        outputCollection.plusAssign(townCollection)
        outputCollection.plusAssign(villageCollection)
        outputCollection.plusAssign(hamletCollection)
        val outputFile = FileOutputStream("low-zoom.geojson")
        outputFile.write(adapter.toJson(outputCollection).toByteArray())
        outputFile.close()
    }
}
