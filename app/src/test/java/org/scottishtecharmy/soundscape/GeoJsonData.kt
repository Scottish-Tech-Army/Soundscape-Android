package org.scottishtecharmy.soundscape

import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiLineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiPoint
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point

/**
 * Fake data for each GeoJSON on Null Island
 */
object GeoJsonData
{
    const val POINT_JSON = "{\"coordinates\":[0.0,0.0],\"type\":\"Point\"}"
    val pointObject = Point().also {
        it.coordinates = LngLatAlt(0.0, 0.0, null)
    }

    const val MULTI_POINT_JSON = "{\"coordinates\":[[0.0,0.0],[1.0,1.0]],\"type\":\"MultiPoint\"}"
    val multiPointObject = MultiPoint().also {
        it.coordinates = arrayListOf(
            LngLatAlt(0.0, 0.0),
            LngLatAlt(1.0, 1.0)
        )
    }

    const val LINE_STRING_JSON = "{\"coordinates\":[[0.0,0.0],[1.0,1.0],[0.0,1.0]],\"type\":\"LineString\"}"
    val lineStringObject = LineString().also {
        it.coordinates = arrayListOf(
            LngLatAlt(0.0, 0.0),
            LngLatAlt(1.0, 1.0),
            LngLatAlt(0.0, 1.0)
        )
    }

    const val MULTI_LINE_STRING_JSON = "{\"coordinates\":[[[0.0,0.0],[1.0,1.0]],[[1.0,2.0],[3.0,3.0]]],\"type\":\"MultiLineString\"}"
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

    const val POLYGON_JSON = "{\"coordinates\":[[[0.0,1.0],[0.0,0.0],[1.0,0.0],[1.0,1.0],[0.0,1.0]]],\"type\":\"Polygon\"}"
    const val MULTI_POLYGON_JSON = "{\"coordinates\":[[[[0.0,1.0],[0.0,0.0],[1.0,0.0],[1.0,1.0],[0.0,1.0]]],[[[0.0,2.0],[0.0,0.0],[2.0,0.0],[2.0,2.0],[0.0,2.0]]]],\"type\":\"MultiPolygon\"}"
    const val FEATURE_JSON = "{\"geometry\":{\"coordinates\":[[[0.0,1.0],[0.0,0.0],[1.0,0.0],[1.0,1.0],[0.0,1.0]]],\"type\":\"Polygon\"},\"properties\":{\"name\":\"Null Island\"},\"type\":\"Feature\"}"
    const val FEATURE_COLLECTION_JSON = "{\"features\":[{\"geometry\":{\"coordinates\":[0.5, 0.5],\"type\":\"Point\"},\"properties\":{\"name\":\"Very interesting point\",\"address\":\"Null Island Street\"},\"type\":\"Feature\"},{\"geometry\":{\"coordinates\":[[[0.0,1.0],[0.0,0.0],[1.0,0.0],[1.0,1.0],[0.0,1.0]]],\"type\":\"Polygon\"},\"properties\":{\"name\":\"Null Island\"},\"type\":\"Feature\"}],\"type\":\"FeatureCollection\"}"
    const val EMPTY_FEATURE_COLLECTION_JSON = "{\"features\":[],\"type\":\"FeatureCollection\"}"
    const val GEOMETRY_COLLECTION_JSON = "{\"geometries\":[{\"coordinates\":[0.5,0.5],\"type\":\"Point\"},{\"coordinates\":[[[0.0,1.0],[0.0,0.0],[1.0,0.0],[1.0,1.0],[0.0,1.0]]],\"type\":\"Polygon\"},{\"coordinates\":[[0.0,0.0],[1.0,1.0]],\"type\":\"LineString\"}],\"properties\":{\"key\":\"value\"},\"type\":\"GeometryCollection\"}"
    const val FEATURE_COLLECTION_NULL_PROPERTIES_TEST_JSON = "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[0.5, 0.5]},\"properties\":null}]}"
}

val sixtyAcresCloseTestLocation = LngLatAlt(-2.693002695425122,51.43938442591545)
val longAshtonRoadTestLocation = LngLatAlt(-2.6573400576040456, 51.430456817236575)
val woodlandWayTestLocation = LngLatAlt(-2.695517313268283, 51.44082881061331)
val centralManchesterTestLocation = LngLatAlt(-2.239529, 53.480408)
