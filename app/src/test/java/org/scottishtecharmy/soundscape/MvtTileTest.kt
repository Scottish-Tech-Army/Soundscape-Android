package org.scottishtecharmy.soundscape

import org.junit.Test
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.InterpolatedPointsJoiner
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.vectorTileToGeoJson
import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geoengine.utils.TileGrid.Companion.getTileGrid
import org.scottishtecharmy.soundscape.geoengine.utils.getLatLonTileWithOffset
import org.scottishtecharmy.soundscape.geoengine.utils.searchFeaturesByName
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.moshi.GeoJsonObjectMoshiAdapter
import vector_tile.VectorTile
import java.io.FileInputStream
import java.io.FileOutputStream


private fun vectorTileToGeoJsonFromFile(
    tileX: Int,
    tileY: Int,
    filename: String,
    cropPoints: Boolean = true
): FeatureCollection {

    val path = "src/test/res/org/scottishtecharmy/soundscape/"
    val remoteTile = FileInputStream(path + filename)
    val tile: VectorTile.Tile = VectorTile.Tile.parseFrom(remoteTile)

    val featureCollection = vectorTileToGeoJson(tileX, tileY, tile, cropPoints, 15)

//            // We want to check that all of the coordinates generated are within the buffered
//            // bounds of the tile. The tile edges are 4/256 further out, so we adjust for that.
//            val nwPoint = getLatLonTileWithOffset(tileX, tileY, 15, -4 / 256.0, -4 / 256.0)
//            val sePoint = getLatLonTileWithOffset(tileX + 1, tileY + 1, 15, 4 / 256.0, 4 / 256.0)
//            for (feature in featureCollection) {
//                var box = BoundingBox()
//                when (feature.geometry.type) {
//                    "Point" -> box = getBoundingBoxOfPoint(feature.geometry as Point)
//                    "MultiPoint" -> box = getBoundingBoxOfMultiPoint(feature.geometry as MultiPoint)
//                    "LineString" -> box = getBoundingBoxOfLineString(feature.geometry as LineString)
//                    "MultiLineString" -> box =
//                        getBoundingBoxOfMultiLineString(feature.geometry as MultiLineString)
//
//                    "Polygon" -> box = getBoundingBoxOfPolygon(feature.geometry as Polygon)
//                    "MultiPolygon" -> box =
//                        getBoundingBoxOfMultiPolygon(feature.geometry as MultiPolygon)
//
//                    else -> assert(false)
//                }
//                // Check that the feature bounding box is within the tileBoundingBox. This has been
//                // broken by the addition of POI polygons which go beyond tile boundaries.
//                assert(box.westLongitude >= nwPoint.longitude) { "${box.westLongitude} vs. ${nwPoint.longitude}" }
//                assert(box.eastLongitude <= sePoint.longitude) { "${box.eastLongitude} vs. ${sePoint.longitude}" }
//                assert(box.southLatitude >= sePoint.latitude) { "${box.southLatitude} vs. ${sePoint.latitude}" }
//                assert(box.northLatitude <= nwPoint.latitude) { "${box.northLatitude} vs. ${nwPoint.latitude}" }
//            }
    return featureCollection
}

fun getGeoJsonForLocation(
    location: LngLatAlt,
    soundscapeBackend: Boolean = false
): FeatureCollection {

    var gridSize = 2
    if (soundscapeBackend) {
        gridSize = 3
    }
    // Get a grid around the location
    val grid = getTileGrid(location.latitude, location.longitude, gridSize)
    for (tile in grid.tiles) {
        println("Need tile ${tile.tileX}x${tile.tileY}")
    }

    // This is implemented for the soundscape-backend yet, so assert to make that clear
    assert(!soundscapeBackend)

    // Read in the files
    val joiner = InterpolatedPointsJoiner()
    val featureCollection = FeatureCollection()
    for (tile in grid.tiles) {
        val geojson = vectorTileToGeoJsonFromFile(
            tile.tileX,
            tile.tileY,
            "${tile.tileX}x${tile.tileY}.mvt"
        )
        for (feature in geojson) {
            val addFeature = joiner.addInterpolatedPoints(feature)
            if (addFeature) {
                featureCollection.addFeature(feature)
            }
        }
    }
    // Add lines to connect all of the interpolated points
    joiner.addJoiningLines(featureCollection)

    val adapter = GeoJsonObjectMoshiAdapter()
    val outputFile = FileOutputStream("${grid.tiles[0].tileX}-${grid.tiles[1].tileX}x${grid.tiles[0].tileY}-${grid.tiles[3].tileY}.geojson")
    outputFile.write(adapter.toJson(featureCollection).toByteArray())
    outputFile.close()

    return featureCollection
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
        val geojson = vectorTileToGeoJsonFromFile(15992, 10212, "15992x10212.mvt")
        val adapter = GeoJsonObjectMoshiAdapter()

        val outputFile = FileOutputStream("milngavie.geojson")
        outputFile.write(adapter.toJson(geojson).toByteArray())
        outputFile.close()
    }

    @Test
    fun testVectorToGeoJsonEdinburgh() {
        val geojson = vectorTileToGeoJsonFromFile(16093, 10211, "16093x10211.mvt")
        val adapter = GeoJsonObjectMoshiAdapter()

        val outputFile = FileOutputStream("edinburgh.geojson")
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
                val geojson = vectorTileToGeoJsonFromFile(x, y, "${x}x${y}.mvt")
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

        val adapter = GeoJsonObjectMoshiAdapter()

        // Check that the de-duplication of the points worked (without that there are two points
        // for Graeme Pharmacy, one each from two separate tiles).
        val searchResults = searchFeaturesByName(featureCollection, "Graeme")
        println(adapter.toJson(searchResults))
        assert(searchResults.features.size == 1)

        val outputFile = FileOutputStream("2x2.geojson")
        outputFile.write(adapter.toJson(featureCollection).toByteArray())
        outputFile.close()
    }

    @Test
    fun testRtree() {

        val joiner = InterpolatedPointsJoiner()

        // Make a large grid to aid analysis
        val featureCollection = FeatureCollection()
        for (x in 15990..15992) {
            for (y in 10212..10213) {
                val geojson = vectorTileToGeoJsonFromFile(x, y, "${x}x${y}.mvt")
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
        val distanceFc = tree.generateNearbyFeatureCollection(LngLatAlt(-4.3058322, 55.9473305), 20.0)
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

        val featureCollection = vectorTileToGeoJsonFromFile(15990, 10212, "15990x10212.mvt")
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
}

