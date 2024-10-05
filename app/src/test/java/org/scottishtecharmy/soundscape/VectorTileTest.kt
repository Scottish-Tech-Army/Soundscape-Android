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
        val remoteTile = FileInputStream(path + "10212.mvt")
        val tile: VectorTile.Tile = VectorTile.Tile.parseFrom(remoteTile)
        println(tile.layersList)
        assert(tile.layersList.isNotEmpty())
    }

    private fun convertGeometry(
        tileX: Int,
        tileY: Int,
        tileZoom: Int,
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
                        if(lineCount > 1)
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
                    results.last().add(
                        getLatLonTileWithOffset(
                            tileX,
                            tileY,
                            tileZoom,
                            x.toDouble() / 4096.0,
                            y.toDouble() / 4096.0
                        )
                    )
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

    private fun vectorTileToGeoJson(tileX: Int, tileY: Int, filename: String): FeatureCollection {

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
                            feature.geometryList)
                        for(polygon in polygons) {
                            if(polygon.first() != polygon.last())
                                polygon.add(polygon.first())
                            listOfGeometries.add(Polygon(polygon))
                        }
                    }

                    VectorTile.Tile.GeomType.POINT -> {
                        val points =
                            convertGeometry(tileX, tileY, tileZoom, feature.geometryList)
                        for(point in points) {
                            listOfGeometries.add(Point(point[0].longitude, point[0].latitude))
                        }
                    }

                    VectorTile.Tile.GeomType.LINESTRING -> {
                        val lines = convertGeometry(
                            tileX,
                            tileY,
                            tileZoom,
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
        val geojson = vectorTileToGeoJson(15992, 10212, "10212.mvt")
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
}