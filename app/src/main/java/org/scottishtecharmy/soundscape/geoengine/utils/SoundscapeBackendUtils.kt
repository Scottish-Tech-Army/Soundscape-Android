package org.scottishtecharmy.soundscape.geoengine.utils

import com.squareup.moshi.Moshi
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeoMoshi
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiLineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiPoint
import org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiPolygon
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon

/**
 * There's a bug in the backend API where the GeoJSON can contain data that isn't part of the tile
 * for example the 16/32277/21812.json contains MultiPolygons for the University of Law,
 * University of Bristol, and Monarchs Way walking route. This strips out Features that someone has
 * wrapped giant polygons around in the original OSM data. Need to see if there is a fix for this on the backend...
 * @param tileX
 * Slippy tile X.
 * @param tileY
 * Slippy tile Y.
 * @param zoom
 * Zoom level should be 16.0
 * @param geoJSONTile
 * String that represents the tile.
 * @return String representing the cleaned tile.
 */
fun cleanTileGeoJSON(
    tileX: Int,
    tileY: Int,
    zoom: Int,
    geoJSONTile: String
): String {
    // create a feature collection from the string
    val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
    val tileFeatureCollection: FeatureCollection? =
        moshi.adapter(FeatureCollection::class.java).fromJson(geoJSONTile)

    val tileBoundingBox = tileToBoundingBox(tileX, tileY, zoom)
    val tilePolygon = getPolygonOfBoundingBox(tileBoundingBox)
    // loop through the tile feature collection and add anything that is contained in the tile polygon
    // to a new feature collection and dump the weirdness
    val cleanTileFeatureCollection = FeatureCollection()

    if (tileFeatureCollection != null) {
        for (feature in tileFeatureCollection){
            when (feature.geometry.type) {
                "Point" -> {
                    val lngLatAlt = (feature.geometry as Point).coordinates
                    val testPoint = polygonContainsCoordinates(lngLatAlt, tilePolygon)
                    if (testPoint){
                        cleanTileFeatureCollection.addFeature(feature)
                    }
                }

                "MultiPoint" -> {
                    for (point in (feature.geometry as MultiPoint).coordinates) {
                        val testPoint = polygonContainsCoordinates(point, tilePolygon)
                        if (testPoint){
                            cleanTileFeatureCollection.addFeature(feature)
                            // at least one of the points is in the tile so add the entire
                            // MultiPoint Feature and break
                            break
                        }
                    }
                }

                "LineString" -> {
                    for (point in (feature.geometry as LineString).coordinates){
                        val testPoint = polygonContainsCoordinates(point, tilePolygon)
                        if (testPoint) {
                            cleanTileFeatureCollection.addFeature(feature)
                            // at least one of the points is in the tile so add the entire
                            // LineString Feature and break
                            break
                        }
                    }
                }

                "MultiLineString" -> {
                    for (lineString in (feature.geometry as MultiLineString).coordinates) {
                        for (point in lineString) {
                            val testPoint = polygonContainsCoordinates(point, tilePolygon)
                            if (testPoint) {
                                cleanTileFeatureCollection.addFeature(feature)
                                // at least one of the points is in the tile so add the entire
                                // MultiLineString Feature and break
                                break
                            }
                        }
                    }
                }

                "Polygon" -> {
                    for (geometry in (feature.geometry as Polygon).coordinates) {
                        for (point in geometry) {
                            val testPoint = polygonContainsCoordinates(point, tilePolygon)
                            if (testPoint) {
                                cleanTileFeatureCollection.addFeature(feature)
                                // at least one of the points is in the tile so add the entire
                                // Polygon Feature and break
                                break
                            }
                        }
                    }
                }

                "MultiPolygon" -> {
                    for (polygon in (feature.geometry as MultiPolygon).coordinates) {
                        for (linearRing in polygon) {
                            for (point in linearRing) {
                                val testPoint = polygonContainsCoordinates(point, tilePolygon)
                                if (testPoint) {
                                    cleanTileFeatureCollection.addFeature(feature)
                                    // at least one of the points is in the tile so add the entire
                                    // MultiPolygon Feature and break
                                    break
                                }
                            }
                        }
                    }
                }

                else -> println("Unknown type ${feature.geometry.type}")
            }
        }
    }
    return moshi.adapter(FeatureCollection::class.java).toJson(cleanTileFeatureCollection)
}