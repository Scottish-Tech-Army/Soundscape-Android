package org.scottishtecharmy.soundscape

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon
import org.scottishtecharmy.soundscape.geojsonparser.moshi.GeoJsonObjectMoshiAdapter
import org.scottishtecharmy.soundscape.utils.getLatLonTileWithOffset
import vector_tile.VectorTile
import vector_tile.VectorTile.Tile
import java.net.URL
import java.util.HashMap


@RunWith(AndroidJUnit4::class)
class VectorTileTest {

    @Test
    fun simpleVectorTile() {

        // This test does the simplest vector tile test:
        //
        //  1. It gets a vector tile from the protomaps server
        //  2. Parses it with the code auto-generated from the vector_tile.proto specification
        //  3. Prints it out
        val remoteTile = URL("https://api.protomaps.com/tiles/v3/15/15992/10212.mvt?key=9f3c764359583830")
        val tile: Tile = Tile.parseFrom(remoteTile.openStream())
        println(tile.layersList)
    }

    private fun convertGeometry(tileX : Int, tileY : Int, tileZoom : Int, geometry: MutableList<Int>) : ArrayList<LngLatAlt> {

        //  Converting the geometry coordinates requires some effort. See
        //      https://github.com/mapbox/vector-tile-spec/blob/master/2.1/README.md#43-geometry-encoding
        //
        var x = 0
        var y = 0
        val results = arrayListOf<LngLatAlt>()
        var id : Int
        var count = 0
        var deltaX  = 0
        var deltaY : Int
        var firstOfPair = true
        for(commandOrParameterInteger in geometry) {
            if(count == 0) {
                id = commandOrParameterInteger.and(0x7)
                count = commandOrParameterInteger.shr(3)
                when(id) {
                    1 -> {
                        deltaX = 0
                        firstOfPair = true
                    }
                    2 -> {
                        deltaX = 0
                    }

                    7 -> {
                        // Close the polygon?
                    }
                    else ->
                        Log.e(TAG, "Unexpected id $id")
                }
            }
            else {
                val value = ((commandOrParameterInteger.shr(1)).xor(-(commandOrParameterInteger.and(1))))

                if(firstOfPair) {
                    deltaX = value
                    firstOfPair = false
                } else {
                    deltaY = value
                    firstOfPair = true

                    x += deltaX
                    y += deltaY

                    // The vector tile has pixel coordinates relative to the tile origin. Convert
                    // these to global coordinates
                    results.add(getLatLonTileWithOffset(tileX,
                                                        tileY,
                                                        tileZoom,
                                                 x.toDouble()/4096.0,
                                                 y.toDouble()/4096.0))
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
        Log.e(TAG, "tileOrigin2 " + tileOrigin2.latitude + "," + tileOrigin2.longitude)
    }

    @Test
    fun vectorTileToGeoJson() {

        // Do we really want to go via GeoJSON, or instead go direct to our parsed format?
        val tileX = 15992
        val tileY = 10212
        val tileZoom = 15
        val remoteTile = URL("https://api.protomaps.com/tiles/v3/$tileZoom/$tileX/$tileY.mvt?key=9f3c764359583830")
        val tile: Tile = Tile.parseFrom(remoteTile.openStream())

        val collection = FeatureCollection()
        for(layer in tile.layersList) {
            Log.d(TAG, "Process layer: " + layer.name)
            if((layer.name == "pois") ||
               (layer.name == "roads")) {
                for (feature in layer.featuresList) {
                    // Convert coordinates to GeoJSON

                    // And map the tags
                    val geoFeature = Feature()
                    geoFeature.id = feature.id.toString()

                    when(feature.type) {
                        VectorTile.Tile.GeomType.POLYGON ->
                            geoFeature.geometry = Polygon(convertGeometry(tileX, tileY, tileZoom, feature.geometryList))

                        VectorTile.Tile.GeomType.POINT -> {
                            val coordinates = convertGeometry(tileX, tileY, tileZoom, feature.geometryList)
                            geoFeature.geometry = Point(coordinates[0].longitude, coordinates[0].latitude)
                        }

                        VectorTile.Tile.GeomType.LINESTRING ->
                            geoFeature.geometry = LineString(convertGeometry(tileX, tileY, tileZoom, feature.geometryList))

                        Tile.GeomType.UNKNOWN -> Log.e(TAG, "Unknown GeomType")
                    }

                    var firstInPair = true
                    var key = ""
                    var value : Any? = null
                    for(tag in feature.tagsList) {
                        if(firstInPair)
                            key = layer.getKeys(tag)
                        else {
                            val raw = layer.getValues(tag)
                            if(raw.hasBoolValue())
                                value = layer.getValues(tag).boolValue
                            else if(raw.hasIntValue())
                                value = layer.getValues(tag).intValue
                            else if(raw.hasSintValue())
                                value = layer.getValues(tag).sintValue
                            else if(raw.hasFloatValue())
                                value = layer.getValues(tag).doubleValue
                            else if(raw.hasDoubleValue())
                                value = layer.getValues(tag).floatValue
                            else if(raw.hasStringValue())
                                value = layer.getValues(tag).stringValue
                            else if(raw.hasUintValue())
                                value = layer.getValues(tag).uintValue
                        }

                        if(!firstInPair) {
                            if(geoFeature.properties == null) {
                                geoFeature.properties = HashMap<String, Any?>()
                            }
                            geoFeature.properties?.put(key, value)
                            firstInPair = true
                        }
                        else
                            firstInPair = false
                    }
                    collection.addFeature(geoFeature)
                }
            }
        }
        val adapter = GeoJsonObjectMoshiAdapter()
        Log.d(TAG, adapter.toJson(collection))
    }

    companion object {
        const val TAG = "VectorTileTest"
    }
}