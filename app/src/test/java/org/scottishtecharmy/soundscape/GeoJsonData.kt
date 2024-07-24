package org.scottishtecharmy.soundscape

import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeometryCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiLineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiPoint
import org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiPolygon
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon

/**
 * Fake data for each GeoJSON on Null Island
 */
object GeoJsonData
{
    val pointJson = "{\"coordinates\":[0.0,0.0],\"type\":\"Point\"}"
    val pointObject = Point().also {
        it.coordinates = LngLatAlt(0.0, 0.0, null)
    }

    val multiPointJson = "{\"coordinates\":[[0.0,0.0],[1.0,1.0]],\"type\":\"MultiPoint\"}"
    val multiPointObject = MultiPoint().also {
        it.coordinates = arrayListOf(
            LngLatAlt(0.0, 0.0),
            LngLatAlt(1.0, 1.0)
        )
    }

    val lineStringJson = "{\"coordinates\":[[0.0,0.0],[1.0,1.0],[0.0,1.0]],\"type\":\"LineString\"}"
    val lineStringObject = LineString().also {
        it.coordinates = arrayListOf(
            LngLatAlt(0.0, 0.0),
            LngLatAlt(1.0, 1.0),
            LngLatAlt(0.0, 1.0)
        )
    }

    val multiLineStringJson = "{\"coordinates\":[[[0.0,0.0],[1.0,1.0]],[[1.0,2.0],[3.0,3.0]]],\"type\":\"MultiLineString\"}"
    val multiLineStringObject = MultiLineString().also {
        it.coordinates = arrayListOf(
            arrayListOf(
                LngLatAlt(0.0, 0.0),
                LngLatAlt(1.0, 1.0)
            ),
            arrayListOf(
                LngLatAlt(1.0, 2.0),
                LngLatAlt(3.0, 3.0)
            )
        )
    }

    val polygonJson = "{\"coordinates\":[[[0.0,1.0],[0.0,0.0],[1.0,0.0],[1.0,1.0],[0.0,1.0]]],\"type\":\"Polygon\"}"
    val polygonObject = Polygon().also {
        it.coordinates = arrayListOf(
            arrayListOf(
                LngLatAlt(0.0, 1.0),
                LngLatAlt(0.0, 0.0),
                LngLatAlt(1.0, 0.0),
                LngLatAlt(1.0, 1.0),
                LngLatAlt(0.0, 1.0)
            )
        )
    }

    val multiPolygonJson = "{\"coordinates\":[[[[0.0,1.0],[0.0,0.0],[1.0,0.0],[1.0,1.0],[0.0,1.0]]],[[[0.0,2.0],[0.0,0.0],[2.0,0.0],[2.0,2.0],[0.0,2.0]]]],\"type\":\"MultiPolygon\"}"
    val multiPolygonObject = MultiPolygon().also {
        it.coordinates = arrayListOf(
            arrayListOf(
                arrayListOf(
                    LngLatAlt(0.0, 1.0),
                    LngLatAlt(0.0, 0.0),
                    LngLatAlt(1.0, 0.0),
                    LngLatAlt(1.0, 1.0),
                    LngLatAlt(0.0, 1.0)
                )
            ),
            arrayListOf(
                arrayListOf(
                    LngLatAlt(0.0, 2.0),
                    LngLatAlt(0.0, 0.0),
                    LngLatAlt(2.0, 0.0),
                    LngLatAlt(2.0, 2.0),
                    LngLatAlt(0.0, 2.0)
                )
            )
        )
    }

    val featureJson = "{\"geometry\":{\"coordinates\":[[[0.0,1.0],[0.0,0.0],[1.0,0.0],[1.0,1.0],[0.0,1.0]]],\"type\":\"Polygon\"},\"properties\":{\"name\":\"Null Island\"},\"type\":\"Feature\"}"
    val featureObject = Feature().also {
        it.geometry = Polygon().also { polygon ->
            polygon.coordinates = arrayListOf(
                arrayListOf(
                    LngLatAlt(0.0, 1.0),
                    LngLatAlt(0.0, 0.0),
                    LngLatAlt(1.0, 0.0),
                    LngLatAlt(1.0, 1.0),
                    LngLatAlt(0.0, 1.0)
                )
            )
        }

        it.properties = hashMapOf("name" to "Null Island")
    }

    val featureCollectionJson = "{\"features\":[{\"geometry\":{\"coordinates\":[0.5, 0.5],\"type\":\"Point\"},\"properties\":{\"name\":\"Very interesting point\",\"address\":\"Null Island Street\"},\"type\":\"Feature\"},{\"geometry\":{\"coordinates\":[[[0.0,1.0],[0.0,0.0],[1.0,0.0],[1.0,1.0],[0.0,1.0]]],\"type\":\"Polygon\"},\"properties\":{\"name\":\"Null Island\"},\"type\":\"Feature\"}],\"type\":\"FeatureCollection\"}"
    val featureCollectionObject = FeatureCollection().also {
        it.features = arrayListOf(
            Feature().also { feature ->
                feature.geometry = Point().also { point ->
                    point.coordinates = LngLatAlt(0.5, 0.5)
                }
                feature.properties = hashMapOf(
                    "name" to "Very interesting point",
                    "address" to "Null Island Street"
                )
            },
            featureObject
        )
    }

    val emptyFeatureCollectionJson = "{\"features\":[],\"type\":\"FeatureCollection\"}"
    val emptyFeatureCollectionObject = FeatureCollection().also {
        it.features = arrayListOf()
    }

    val geometryCollectionJson = "{\"geometries\":[{\"coordinates\":[0.5,0.5],\"type\":\"Point\"},{\"coordinates\":[[[0.0,1.0],[0.0,0.0],[1.0,0.0],[1.0,1.0],[0.0,1.0]]],\"type\":\"Polygon\"},{\"coordinates\":[[0.0,0.0],[1.0,1.0]],\"type\":\"LineString\"}],\"properties\":{\"key\":\"value\"},\"type\":\"GeometryCollection\"}"
    val gemoetryCollectionObject = GeometryCollection().also {
        it.properties = hashMapOf("key" to "value")
        it.geometries = arrayListOf(
            Point().also { point ->
                point.coordinates = LngLatAlt(0.5, 0.5)
            },
            Polygon().also { polygon ->
                polygon.coordinates = arrayListOf(
                    arrayListOf(
                        LngLatAlt(0.0, 1.0),
                        LngLatAlt(0.0, 0.0),
                        LngLatAlt(1.0, 0.0),
                        LngLatAlt(1.0, 1.0),
                        LngLatAlt(0.0, 1.0)
                    )
                )
            },
            LineString().also { lineString ->
                lineString.coordinates = arrayListOf(
                    LngLatAlt(0.0, 0.0),
                    LngLatAlt(1.0, 1.0)
                )
            }
        )
    }

    val featureCollectionNullPropertiesTestJson = "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[0.5, 0.5]},\"properties\":null}]}"
}