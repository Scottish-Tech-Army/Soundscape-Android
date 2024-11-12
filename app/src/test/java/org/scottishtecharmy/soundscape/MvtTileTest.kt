package org.scottishtecharmy.soundscape

import org.junit.Test
import org.scottishtecharmy.soundscape.dto.BoundingBox
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiLineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiPoint
import org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiPolygon
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon
import org.scottishtecharmy.soundscape.geojsonparser.moshi.GeoJsonObjectMoshiAdapter
import org.scottishtecharmy.soundscape.utils.getBoundingBoxOfLineString
import org.scottishtecharmy.soundscape.utils.getBoundingBoxOfMultiLineString
import org.scottishtecharmy.soundscape.utils.getBoundingBoxOfMultiPoint
import org.scottishtecharmy.soundscape.utils.getBoundingBoxOfMultiPolygon
import org.scottishtecharmy.soundscape.utils.getBoundingBoxOfPoint
import org.scottishtecharmy.soundscape.utils.getBoundingBoxOfPolygon
import org.scottishtecharmy.soundscape.utils.getLatLonTileWithOffset
import org.scottishtecharmy.soundscape.utils.searchFeaturesByName
import org.scottishtecharmy.soundscape.utils.vectorTileToGeoJson
import vector_tile.VectorTile
import java.io.FileInputStream
import java.io.FileOutputStream


class MvtTileTest {

    private fun vectorTileToGeoJsonFromFile(tileX: Int,
                                            tileY: Int,
                                            filename: String,
                                            cropPoints: Boolean = true): FeatureCollection {

        val path = "src/test/res/org/scottishtecharmy/soundscape/"
        val remoteTile = FileInputStream(path + filename)
        val tile: VectorTile.Tile = VectorTile.Tile.parseFrom(remoteTile)

        val featureCollection = vectorTileToGeoJson(tileX, tileY, tile, cropPoints, 15)

        // We want to check that all of the coordinates generated are within the buffered
        // bounds of the tile. The tile edges are 4/256 further out, so we adjust for that.
        val nwPoint = getLatLonTileWithOffset(tileX, tileY, 15, -4/256.0, -4/256.0)
        val sePoint = getLatLonTileWithOffset(tileX+1, tileY+1, 15, 4/256.0, 4/256.0)
        for(feature in featureCollection) {
            var box = BoundingBox()
            when(feature.geometry.type) {
                "Point" -> box = getBoundingBoxOfPoint(feature.geometry as Point)
                "MultiPoint" ->  box = getBoundingBoxOfMultiPoint(feature.geometry as MultiPoint)
                "LineString" -> box = getBoundingBoxOfLineString(feature.geometry as LineString)
                "MultiLineString" -> box = getBoundingBoxOfMultiLineString(feature.geometry as MultiLineString)
                "Polygon" -> box = getBoundingBoxOfPolygon(feature.geometry as Polygon)
                "MultiPolygon" -> box = getBoundingBoxOfMultiPolygon(feature.geometry as MultiPolygon)
                else -> assert(false)
            }
//            // Check that the feature bounding box is within the tileBoundingBox
//            assert(box.westLongitude >= nwPoint.longitude) { "${box.westLongitude} vs. ${nwPoint.longitude}" }
//            assert(box.eastLongitude <= sePoint.longitude) { "${box.eastLongitude} vs. ${sePoint.longitude}" }
//            assert(box.southLatitude >= sePoint.latitude) { "${box.southLatitude} vs. ${sePoint.latitude}" }
//            assert(box.northLatitude <= nwPoint.latitude) { "${box.northLatitude} vs. ${nwPoint.latitude}" }
        }
        return featureCollection
    }


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

        // Make a large grid to aid analysis
        val featureCollection = FeatureCollection()
        for(x in 15990..15992) {
            for (y in 10212..10213) {
                val geojson = vectorTileToGeoJsonFromFile(x, y, "${x}x${y}.mvt")
                for(feature in geojson) {
                    featureCollection.addFeature(feature)
                }
            }
        }

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
}