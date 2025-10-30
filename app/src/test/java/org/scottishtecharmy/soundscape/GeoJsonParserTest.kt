package org.scottishtecharmy.soundscape

import org.scottishtecharmy.soundscape.geojsonparser.geojson.*
import org.scottishtecharmy.soundscape.geojsonparser.moshi.*
import com.squareup.moshi.Moshi
import org.junit.Assert
import org.junit.Test
import kotlin.collections.get

class GeoJsonParserTest {
    private val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()

    @Test
    @Throws(Exception::class)
    fun testIntelligentDeserialization() {
        val point = moshi.adapter(GeoJsonObject::class.java).fromJson(GeoJsonData.POINT_JSON)
        Assert.assertTrue(point is Point)

        val multiPoint =
            moshi.adapter(GeoJsonObject::class.java).fromJson(GeoJsonData.MULTI_POINT_JSON)
        Assert.assertTrue(multiPoint is MultiPoint)

        val lineString =
            moshi.adapter(GeoJsonObject::class.java).fromJson(GeoJsonData.LINE_STRING_JSON)
        Assert.assertTrue(lineString is LineString)

        val multiLineString =
            moshi.adapter(GeoJsonObject::class.java).fromJson(GeoJsonData.MULTI_LINE_STRING_JSON)
        Assert.assertTrue(multiLineString is MultiLineString)

        val polygon = moshi.adapter(GeoJsonObject::class.java).fromJson(GeoJsonData.POLYGON_JSON)
        Assert.assertTrue(polygon is Polygon)

        val multiPolygon =
            moshi.adapter(GeoJsonObject::class.java).fromJson(GeoJsonData.MULTI_POLYGON_JSON)
        Assert.assertTrue(multiPolygon is MultiPolygon)

        val feature = moshi.adapter(GeoJsonObject::class.java).fromJson(GeoJsonData.FEATURE_JSON)
        Assert.assertTrue(feature is Feature)

        val featureCollection =
            moshi.adapter(GeoJsonObject::class.java).fromJson(GeoJsonData.FEATURE_COLLECTION_JSON)
        Assert.assertTrue(featureCollection is FeatureCollection)

        val emptyFeatureCollection = moshi.adapter(GeoJsonObject::class.java)
            .fromJson(GeoJsonData.EMPTY_FEATURE_COLLECTION_JSON)
        Assert.assertTrue(emptyFeatureCollection is FeatureCollection)

        val geometryCollection =
            moshi.adapter(GeoJsonObject::class.java).fromJson(GeoJsonData.GEOMETRY_COLLECTION_JSON)
        Assert.assertTrue(geometryCollection is GeometryCollection)
    }

    @Test
    fun testEmptyFeatureCollection() {
        val feature = moshi.adapter(FeatureCollection::class.java)
            .fromJson(GeoJsonData.EMPTY_FEATURE_COLLECTION_JSON)

        Assert.assertNotNull(feature)
        Assert.assertTrue(feature is FeatureCollection)
        Assert.assertNotNull(feature!!.features)
        Assert.assertEquals(0, feature.features.size)
    }

    @Test
    @Throws(Exception::class)
    fun itShouldDeserializeAPoint() {
        val value = moshi.adapter(Point::class.java).fromJson(GeoJsonData.POINT_JSON)
        Assert.assertNotNull(value)
        Assert.assertTrue(value is Point)
        val point2 = value as Point
        assertLngLatAlt(0.0, 0.0, Double.NaN, point2.coordinates)
    }

    @Test
    @Throws(Exception::class)
    fun itShouldSerializeAPoint() {
        Assert.assertEquals(
            GeoJsonData.POINT_JSON,
            moshi.adapter(GeoJsonObject::class.java).toJson(GeoJsonData.pointObject)
        )
    }

    @Test
    @Throws(Exception::class)
    fun itShouldDeserializeAPointWithProperties() {
        val value = moshi.adapter(Point::class.java).fromJson(
            """
			{
				"type":"Point",
				"coordinates":[0.5,0.5],
				"properties": {
					"name": "value",
					"name2": 2
				},
				"bbox": [0.5, 0.5, 0.5, 0.5, 0.5],
				"other": "foreign"
			}
		"""
        )
        Assert.assertNotNull(value)
        Assert.assertTrue(value is Point)
        assertLngLatAlt(0.5, 0.5, Double.NaN, value!!.coordinates)
        Assert.assertTrue(!value.properties.isNullOrEmpty())
        Assert.assertEquals("value", value.properties!!["name"] as String)
        Assert.assertEquals(2.0, value.properties!!["name2"] as Double, 0.00001)
        Assert.assertNotNull(value.bbox)
        Assert.assertEquals(0.5, value.bbox!![0], 0.00001)
        Assert.assertEquals(0.5, value.bbox!![1], 0.00001)
        Assert.assertEquals(0.5, value.bbox!![2], 0.00001)
        Assert.assertEquals(0.5, value.bbox!![3], 0.00001)

    }

    @Test
    @Throws(Exception::class)
    fun itShouldDeserializeLineString() {
        val lineString = moshi.adapter(LineString::class.java).fromJson(GeoJsonData.LINE_STRING_JSON)
        Assert.assertNotNull(lineString)
        Assert.assertTrue(lineString is LineString)

        with(lineString as LineString) {
            val coordinates = lineString.coordinates
            assertLngLatAlt(0.0, 0.0, null, coordinates[0])
            assertLngLatAlt(1.0, 1.0, null, coordinates[1])
        }
    }

    @Test
    @Throws(Exception::class)
    fun itShouldSerializeLineString() {
        Assert.assertEquals(
            GeoJsonData.LINE_STRING_JSON,
            moshi.adapter(GeoJsonObject::class.java).toJson(GeoJsonData.lineStringObject)
        )
    }

    @Test
    @Throws(Exception::class)
    fun itShouldDeserializeMultiLineString() {
        val multiLineString =
            moshi.adapter(MultiLineString::class.java).fromJson(GeoJsonData.MULTI_LINE_STRING_JSON)
        Assert.assertNotNull(multiLineString)
        Assert.assertTrue(multiLineString is MultiLineString)

        with(multiLineString as MultiLineString) {
            assertListEquals(GeoJsonData.multiLineStringObject.coordinates[0], coordinates[0])
        }
    }

    @Test
    @Throws(Exception::class)
    fun itShouldSerializeMultiLineString() {
        Assert.assertEquals(
            GeoJsonData.MULTI_LINE_STRING_JSON,
            moshi.adapter(GeoJsonObject::class.java).toJson(GeoJsonData.multiLineStringObject)
        )
    }

    @Test
    @Throws(Exception::class)
    fun itShouldDeserializeMultiPoint() {
        val multiPoint = moshi.adapter(MultiPoint::class.java).fromJson(GeoJsonData.MULTI_POINT_JSON)
        Assert.assertTrue(multiPoint is MultiPoint)
        Assert.assertNotNull(multiPoint)
        val coordinates = multiPoint!!.coordinates
        assertLngLatAlt(0.0, 0.0, null, coordinates[0])
        assertLngLatAlt(1.0, 1.0, null, coordinates[1])
    }

    @Test
    @Throws(Exception::class)
    fun itShouldSerializeMultiPoint() {
        Assert.assertEquals(
            GeoJsonData.MULTI_POINT_JSON,
            moshi.adapter(GeoJsonObject::class.java).toJson(GeoJsonData.multiPointObject)
        )
    }

    @Test
    @Throws(Exception::class)
    fun itShouldDeserializeMultiPolygon() {
        val multiPolygon =
            moshi.adapter(MultiPolygon::class.java).fromJson(GeoJsonData.MULTI_POLYGON_JSON)
        Assert.assertNotNull(multiPolygon)
        Assert.assertTrue(multiPolygon is MultiPolygon)

        Assert.assertEquals(2, multiPolygon!!.coordinates.size)
        Assert.assertEquals(1, multiPolygon.coordinates[0].size)
        Assert.assertEquals(5, multiPolygon.coordinates[0][0].size)
        Assert.assertEquals(5, multiPolygon.coordinates[1][0].size)
    }

    private fun assertListEquals(
        expectedList: ArrayList<LngLatAlt>,
        actualList: ArrayList<LngLatAlt>
    ) {
        for (x in actualList.indices) {
            val expected = expectedList[x]
            val actual = actualList[x]
            assertLngLatAlt(expected.longitude, expected.latitude, expected.altitude, actual)
        }
    }

    private fun assertLngLatAlt(
        expectedLongitude: Double,
        expectedLatitude: Double,
        expectedAltitude: Double?,
        point: LngLatAlt
    ) {
        Assert.assertEquals(expectedLongitude, point.longitude, 0.00001)
        Assert.assertEquals(expectedLatitude, point.latitude, 0.00001)

        if (expectedAltitude?.isNaN() == true || expectedAltitude == null) {
            Assert.assertFalse(point.hasAltitude())
        } else {
            Assert.assertEquals(expectedAltitude, point.altitude!!, 0.00001)
        }
    }
}