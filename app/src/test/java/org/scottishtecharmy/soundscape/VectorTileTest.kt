package org.scottishtecharmy.soundscape

import org.junit.Test
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeoJsonObject
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon
import org.scottishtecharmy.soundscape.geojsonparser.moshi.GeoJsonObjectMoshiAdapter
import org.scottishtecharmy.soundscape.utils.getLatLonTileWithOffset
import org.scottishtecharmy.soundscape.utils.searchFeaturesByName
import vector_tile.VectorTile
import java.io.FileInputStream
import java.io.FileOutputStream


class VectorTileTest {

    @Test
    fun simpleVectorTile() {

        // This test does the simplest vector tile test:
        //
        //  1. It gets a vector tile from the protomaps server
        //  2. Parses it with the code auto-generated from the vector_tile.proto specification
        //  3. Prints it out
        val path = "src/test/res/org/scottishtecharmy/soundscape/"
        val remoteTile = FileInputStream(path + "15992x10212.mvt")
        val tile: VectorTile.Tile = VectorTile.Tile.parseFrom(remoteTile)
        println(tile.layersList)
        assert(tile.layersList.isNotEmpty())
    }

    private fun convertGeometry(
        tileX: Int,
        tileY: Int,
        tileZoom: Int,
        cropToTile: Boolean,
        geometry: MutableList<Int>
    ): List<ArrayList<LngLatAlt>> {

        //  Converting the geometry coordinates requires some effort. See
        //      https://github.com/mapbox/vector-tile-spec/blob/master/2.1/README.md#43-geometry-encoding
        //
        //  Geometries can contain multiple line segments if they have the same tags e.g. unnamed
        //  minor roads. This is why we have to return a List of line segments.
        //
        var x = 0
        var y = 0
        val results = mutableListOf(arrayListOf<LngLatAlt>())
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
                        results.last().add(
                            getLatLonTileWithOffset(
                                tileX,
                                tileY,
                                tileZoom,
                                x.toDouble() / 4096.0,
                                y.toDouble() / 4096.0
                            )
                        )
                    }
                    --count
                }
            }
        }

        return results
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

    private fun vectorTileToGeoJson(tileX: Int,
                                    tileY: Int,
                                    filename: String,
                                    cropPoints: Boolean = true): FeatureCollection {

        val tileZoom = 15

        val path = "src/test/res/org/scottishtecharmy/soundscape/"
        val remoteTile = FileInputStream(path + filename)
        val tile: VectorTile.Tile = VectorTile.Tile.parseFrom(remoteTile)
        val collection = FeatureCollection()
        for (layer in tile.layersList) {
            when(layer.name){
                "landuse" -> continue
                "earth" -> continue
                "natural" -> continue
                "wood" -> continue
                "buildings" -> continue
                "physical_line" -> continue
            }

            println("Process layer: " + layer.name)
            for (feature in layer.featuresList) {
                // Convert coordinates to GeoJSON. This is where we find out how many features
                // we're actually dealing with as there can be multiple features that have the
                // same properties.
                assert(feature.type != null)
                val listOfGeometries = mutableListOf<GeoJsonObject>()
                when (feature.type) {
                    VectorTile.Tile.GeomType.POLYGON -> {
                        val polygons = convertGeometry(
                            tileX,
                            tileY,
                            tileZoom,
                            false,
                            feature.geometryList)
                        for(polygon in polygons) {
                            if(polygon.first() != polygon.last())
                                polygon.add(polygon.first())
                            listOfGeometries.add(Polygon(polygon))
                        }
                    }

                    VectorTile.Tile.GeomType.POINT -> {
                        val points =
                            convertGeometry(tileX, tileY, tileZoom, cropPoints, feature.geometryList)
                        for(point in points) {
                            if(point.isNotEmpty())
                                listOfGeometries.add(Point(point[0].longitude, point[0].latitude))
                        }
                    }

                    VectorTile.Tile.GeomType.LINESTRING -> {
                        val lines = convertGeometry(
                            tileX,
                            tileY,
                            tileZoom,
                            false,
                            feature.geometryList)
                        for(line in lines) {
                            listOfGeometries.add(LineString(line))
                        }
                    }

                    VectorTile.Tile.GeomType.UNKNOWN -> {
                        assert(false)
                    }
                }

                for(geometry in listOfGeometries) {
                    // And map the tags
                    val geoFeature = Feature()
                    geoFeature.id = feature.id.toString()

                    var firstInPair = true
                    var key = ""
                    var value: Any? = null
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
                            if (geoFeature.properties == null) {
                                geoFeature.properties = HashMap()
                            }
                            geoFeature.properties?.put(key, value)
                            firstInPair = true
                        } else
                            firstInPair = false
                    }
                    geoFeature.geometry = geometry
                    collection.addFeature(geoFeature)
                }
            }
        }
        return collection
    }

    @Test
    fun testVectorToGeoJsonMilngavie() {
        val geojson = vectorTileToGeoJson(15992, 10212, "15992x10212.mvt")
        val adapter = GeoJsonObjectMoshiAdapter()

        val outputFile = FileOutputStream("milngavie.geojson")
        outputFile.write(adapter.toJson(geojson).toByteArray())
        outputFile.close()
    }

    @Test
    fun testVectorToGeoJsonEdinburgh() {
        val geojson = vectorTileToGeoJson(16093, 10211, "10211.mvt")
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
     * When using soundscape-backend the tile array used is 3x3, but those tiles are at a higher
     * zoom level. The vector tiles are twice the width/height and so a 2x2 array can be used with
     * the array moving when the user location leaves the center.
     */
    @Test
    fun testVectorToGeoJson2x2() {
        val geojsonTopLeft = vectorTileToGeoJson(15991, 10212, "15991x10212.mvt")
        val geojsonTopRight = vectorTileToGeoJson(15992, 10212, "15992x10212.mvt")
        val geojsonBottomLeft = vectorTileToGeoJson(15991, 10213, "15991x10213.mvt")
        val geojsonBottomRight = vectorTileToGeoJson(15992, 10213, "15992x10213.mvt")

        // Merge the GeoJSON into a single FeatureCollection
        val geojson = FeatureCollection()
        for(feature in geojsonTopLeft.features)
            geojson.addFeature(feature)
        for(feature in geojsonTopRight.features)
            geojson.addFeature(feature)
        for(feature in geojsonBottomLeft.features)
            geojson.addFeature(feature)
        for(feature in geojsonBottomRight.features)
            geojson.addFeature(feature)

        val adapter = GeoJsonObjectMoshiAdapter()

        // Check that the de-duplication of the points worked (without that there are two points
        // for Graeme Pharmacy, one each from two separate tiles).
        val searchResults = searchFeaturesByName(geojson, "Graeme")
        println(adapter.toJson(searchResults))
        assert(searchResults.features.size == 1)

        val outputFile = FileOutputStream("2x2.geojson")
        outputFile.write(adapter.toJson(geojson).toByteArray())
        outputFile.close()
    }
}