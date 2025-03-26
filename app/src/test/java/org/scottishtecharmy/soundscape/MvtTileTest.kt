package org.scottishtecharmy.soundscape

import android.location.Location
import android.location.LocationManager
import org.junit.Test
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.filters.NearestRoadFilter
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.InterpolatedPointsJoiner
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Intersection
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.convertBackToTileCoordinates
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.sampleToFractionOfTile
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.vectorTileToGeoJson
import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geoengine.utils.TileGrid.Companion.getTileGrid
import org.scottishtecharmy.soundscape.geoengine.utils.confectNamesForRoad
import org.scottishtecharmy.soundscape.geoengine.utils.getDistanceToFeature
import org.scottishtecharmy.soundscape.geoengine.utils.getLatLonTileWithOffset
import org.scottishtecharmy.soundscape.geoengine.utils.getPoiFeatureCollectionBySuperCategory
import org.scottishtecharmy.soundscape.geoengine.utils.mergeAllPolygonsInFeatureCollection
import org.scottishtecharmy.soundscape.geoengine.utils.searchFeaturesByName
import org.scottishtecharmy.soundscape.geoengine.utils.traverseIntersectionsConfectingNames
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.geojsonparser.moshi.GeoJsonObjectMoshiAdapter
import vector_tile.VectorTile
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.system.measureTimeMillis


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

    return vectorTileToGeoJson(tileX, tileY, tile, intersectionMap, cropPoints, 15)
}

fun getGridStateForLocation(
    location: LngLatAlt,
    gridSize: Int = 2,
    gridIntersections: HashMap<LngLatAlt, Intersection> = hashMapOf()
): GridState {

    // Get a grid around the location
    val grid = getTileGrid(location, gridSize)
    for (tile in grid.tiles) {
        println("Need tile ${tile.tileX}x${tile.tileY}")
    }

    // Read in the files
    val joiner = InterpolatedPointsJoiner()
    val featureCollection = FeatureCollection()
    for (tile in grid.tiles) {
        val intersectionMap: HashMap<LngLatAlt, Intersection> = hashMapOf()
        val geojson = vectorTileToGeoJsonFromFile(
            tile.tileX,
            tile.tileY,
            "${tile.tileX}x${tile.tileY}.mvt",
            intersectionMap
        )
        for (feature in geojson) {
            val addFeature = joiner.addInterpolatedPoints(feature)
            if (addFeature) {
                featureCollection.addFeature(feature)
            }
        }
        for(intersection in intersectionMap) {
            gridIntersections[intersection.key] = intersection.value
        }
    }
    return GridState.createFromFeatureCollection(featureCollection)
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
        val remoteTile = FileInputStream(path + "16093x10211.mvt")
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
        val geojson = vectorTileToGeoJsonFromFile(15991, 10212, "15991x10212.mvt", intersectionMap)
        val adapter = GeoJsonObjectMoshiAdapter()

        val outputFile = FileOutputStream("milngavie.geojson")
        outputFile.write(adapter.toJson(geojson).toByteArray())
        outputFile.close()
    }

    @Test
    fun testVectorToGeoJsonEdinburgh() {
        val intersectionMap: HashMap<LngLatAlt, Intersection> = hashMapOf()
        val geojson = vectorTileToGeoJsonFromFile(16093, 10211, "16093x10211.mvt", intersectionMap)
        val adapter = GeoJsonObjectMoshiAdapter()

        val outputFile = FileOutputStream("edinburgh.geojson")
        outputFile.write(adapter.toJson(geojson).toByteArray())
        outputFile.close()
    }

    @Test
    fun testVectorToGeoJsonByresRoad() {
        val intersectionMap: HashMap<LngLatAlt, Intersection> = hashMapOf()
        val geojson = vectorTileToGeoJsonFromFile(15992, 10223, "15992x10223.mvt", intersectionMap)
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

        val joiner = InterpolatedPointsJoiner()

        // Make a large grid to aid analysis
        val featureCollection = FeatureCollection()
        for (x in 15990..15992) {
            for (y in 10212..10213) {
                val intersectionMap: HashMap<LngLatAlt, Intersection> = hashMapOf()
                val geojson = vectorTileToGeoJsonFromFile(x, y, "${x}x${y}.mvt", intersectionMap)
                for (feature in geojson) {
                    val addFeature = joiner.addInterpolatedPoints(feature)
                    if (addFeature) {
                        featureCollection.addFeature(feature)
                    }
                }
            }
        }
        // Add lines to connect all of the interpolated points
        joiner.addJoiningLines(featureCollection)

        val mergedCollection = mergeAllPolygonsInFeatureCollection(featureCollection)

        val adapter = GeoJsonObjectMoshiAdapter()

        // Check that the de-duplication of the points worked (without that there are two points
        // for Graeme Pharmacy, one each from two separate tiles).
        val searchResults = searchFeaturesByName(mergedCollection, "Graeme")
        println(adapter.toJson(searchResults))
        assert(searchResults.features.size == 1)

        // Check that we can find the containing polygons for a point
        val tree = FeatureTree(mergedCollection)
        val fc1 = tree.getContainingPolygons(LngLatAlt(-4.316401, 55.939941))
        assert(fc1.features.size == 1)
        assert(fc1.features[0].properties?.get("name") == "Tesco Customer Car Park")

        val fc2 = tree.getContainingPolygons(LngLatAlt(-4.312885, 55.942237))
        assert(fc2.features.size == 1)
        assert(fc2.features[0].properties?.get("name") == "Milngavie Town Hall")

        val fc3 = tree.getContainingPolygons(LngLatAlt(-4.296998, 55.948270))
        assert(fc3.features.size == 2)
        assert(fc3.features[0].properties?.get("name") == "Milngavie Fitness & Wellbeing Gym")
        assert(fc3.features[1].properties?.get("class") == "parking")

        val fc4 = tree.getContainingPolygons(LngLatAlt(-4.316641241312027,55.94160200415631))
        assert(fc4.features.size == 1)

        val outputFile = FileOutputStream("2x2.geojson")
        outputFile.write(adapter.toJson(mergedCollection).toByteArray())
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
        val gridIntersections: HashMap<LngLatAlt, Intersection> = hashMapOf()
        val gridState = getGridStateForLocation(userGeometry.location, 2, gridIntersections)

        val confectionTime = measureTimeMillis {
            traverseIntersectionsConfectingNames(gridIntersections)
        }

        var roads = gridState.getFeatureCollection(TreeId.ROADS_AND_PATHS)
        val confectionTime2 = measureTimeMillis {
            for (road in roads) {
                confectNamesForRoad(road, gridState.featureTrees)
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

        val joiner = InterpolatedPointsJoiner()

        // Make a large grid to aid analysis
        val featureCollection = FeatureCollection()
        for (x in 15990..15992) {
            for (y in 10212..10213) {
                val intersectionMap: HashMap<LngLatAlt, Intersection> = hashMapOf()
                val geojson = vectorTileToGeoJsonFromFile(x, y, "${x}x${y}.mvt", intersectionMap)
                for (feature in geojson) {
                    val addFeature = joiner.addInterpolatedPoints(feature)
                    if (addFeature) {
                        featureCollection.addFeature(feature)
                    }
                }
            }
        }
        // Add lines to connect all of the interpolated points
        joiner.addJoiningLines(featureCollection)

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
        val distanceFc = tree.getNearbyCollection(LngLatAlt(-4.3058322, 55.9473305), 20.0)
        end = System.currentTimeMillis()
        println("Search (${end-start}ms):")
        for(feature in distanceFc) {
            println(feature.properties?.get("name"))
        }

        start = System.currentTimeMillis()
        val nearestFc = tree.getNearestFeature(LngLatAlt(-4.316914, 55.941861), 50.0)
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
        val featureCollection = vectorTileToGeoJsonFromFile(15990, 10212, "15990x10212.mvt", intersectionMap)
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
        val gridState = getGridStateForLocation(userGeometry.location)

        val roadTree = gridState.getFeatureTree(TreeId.ROADS)
        val nearestRoad = roadTree.getNearestFeature(userGeometry.location)

        // The distance returned is the shortest distance to the line, but the nearestPoint is the
        // point on the line that's the nearest.
        val nearestPoint = getDistanceToFeature(userGeometry.location, nearestRoad!!)

        println(nearestRoad.toString())
    }

    private fun createLocation(newLocation : LngLatAlt, speed : Float) : Location {

        val location = Location(LocationManager.PASSIVE_PROVIDER)
        location.latitude = newLocation.latitude
        location.longitude = newLocation.longitude
        location.speed = speed
        location.accuracy = 10.0F

        return location
    }

    @Test
    fun testNearestRoadIdeas() {

        val gridState = getGridStateForLocation(LngLatAlt(-4.31029, 55.94583))
        val geojson = FeatureCollection()

        val heading = 180.0
        var latitude = 55.945219
        while(latitude < 55.94583) {
            var longitude = -4.311362
            var lastNearestRoad: Feature? = null
            while(longitude < -4.31029) {

                val location = LngLatAlt(longitude, latitude)
                val sensedNearestRoads = gridState.getFeatureTree(TreeId.ROADS_AND_PATHS)
                    .getNearestCollection(location, 20.0, 10)

                var bestIndex = -1
                var bestFitness = 0.0
                for ((index, sensedRoad) in sensedNearestRoads.withIndex()) {
                    val sensedRoadInfo = getDistanceToFeature(location, sensedRoad)
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


    @Test
    fun testNearestRoadFilter(){
        val locations = arrayOf(
            arrayOf(-4.31029,  55.94583,  8.0, 210.0, 10.0),
            arrayOf(-4.310359, 55.945824, 8.0, 205.0, 10.0),
            arrayOf(-4.310503, 55.945682, 1.0, 190.0, 10.0),
            arrayOf(-4.310503, 55.945682, 8.0, 185.0, 10.0),
            arrayOf(-4.310549, 55.945569, 8.0, 180.0, 10.0),
            arrayOf(-4.310549, 55.945569, 8.0, 180.0, 20.0),
            arrayOf(-4.310605, 55.945456, 8.0, 180.0, 20.0),
            arrayOf(-4.310605, 55.945456, 8.0, 180.0, 20.0),
            arrayOf(-4.310654, 55.945327, 8.0, 180.0, 20.0),
            arrayOf(-4.310654, 55.945327, 8.0, 200.0, 20.0),
            arrayOf(-4.310691, 55.945218, 8.0, 270.0, 20.0),
            arrayOf(-4.310691, 55.945218, 8.0, 270.0, 10.0),
            arrayOf(-4.310887, 55.945219, 8.0, 270.0, 10.0),
            arrayOf(-4.310887, 55.945219, 8.0, 270.0, 10.0),
            arrayOf(-4.311083, 55.945221, 8.0, 270.0, 10.0),
            arrayOf(-4.311083, 55.945221, 8.0, 270.0, 10.0),
            arrayOf(-4.311362, 55.945219, 8.0, 270.0, 10.0),
        )

        val gridState = getGridStateForLocation(LngLatAlt(-4.31029,  55.94583))
        val filter = NearestRoadFilter()

        var time = 0L
        for(location in locations) {
            filter.update(
                LngLatAlt(location[0], location[1]),
                location[2],
                location[3],
                location[4],
                time,
                gridState)
            time += 1000
        }
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
}

