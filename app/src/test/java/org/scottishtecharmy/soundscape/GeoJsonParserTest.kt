package org.scottishtecharmy.soundscape

import org.scottishtecharmy.soundscape.geojsonparser.geojson.*
import org.scottishtecharmy.soundscape.geojsonparser.moshi.*
import com.squareup.moshi.Moshi
import org.junit.Assert
import org.junit.Test

class GeoJsonParserTest {
    private val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()

    @Test
    fun testFeatureCollectionNullProperties() {
        val collection = moshi.adapter(FeatureCollection::class.java)
            .fromJson(GeoJsonData.featureCollectionNullPropertiesTestJson)
        Assert.assertNotNull(collection)
        Assert.assertNull(collection!!.features.first().properties)
    }

    @Test
    @Throws(Exception::class)
    fun testIntelligentDeserialization() {
        val point = moshi.adapter(GeoJsonObject::class.java).fromJson(GeoJsonData.pointJson)
        Assert.assertTrue(point is Point)

        val multiPoint =
            moshi.adapter(GeoJsonObject::class.java).fromJson(GeoJsonData.multiPointJson)
        Assert.assertTrue(multiPoint is MultiPoint)

        val lineString =
            moshi.adapter(GeoJsonObject::class.java).fromJson(GeoJsonData.lineStringJson)
        Assert.assertTrue(lineString is LineString)

        val multiLineString =
            moshi.adapter(GeoJsonObject::class.java).fromJson(GeoJsonData.multiLineStringJson)
        Assert.assertTrue(multiLineString is MultiLineString)

        val polygon = moshi.adapter(GeoJsonObject::class.java).fromJson(GeoJsonData.polygonJson)
        Assert.assertTrue(polygon is Polygon)

        val multiPolygon =
            moshi.adapter(GeoJsonObject::class.java).fromJson(GeoJsonData.multiPolygonJson)
        Assert.assertTrue(multiPolygon is MultiPolygon)

        val feature = moshi.adapter(GeoJsonObject::class.java).fromJson(GeoJsonData.featureJson)
        Assert.assertTrue(feature is Feature)

        val featureCollection =
            moshi.adapter(GeoJsonObject::class.java).fromJson(GeoJsonData.featureCollectionJson)
        Assert.assertTrue(featureCollection is FeatureCollection)

        val emptyFeatureCollection = moshi.adapter(GeoJsonObject::class.java)
            .fromJson(GeoJsonData.emptyFeatureCollectionJson)
        Assert.assertTrue(emptyFeatureCollection is FeatureCollection)

        val geometryCollection =
            moshi.adapter(GeoJsonObject::class.java).fromJson(GeoJsonData.geometryCollectionJson)
        Assert.assertTrue(geometryCollection is GeometryCollection)
    }

    @Test
    fun testEmptyFeatureCollection() {
        val feature = moshi.adapter(FeatureCollection::class.java)
            .fromJson(GeoJsonData.emptyFeatureCollectionJson)

        Assert.assertNotNull(feature)
        Assert.assertTrue(feature is FeatureCollection)
        Assert.assertNotNull(feature!!.features)
        Assert.assertEquals(0, feature.features.size)
    }

    @Test
    fun testFeatureForeign() {
        val str = """
			{
				"type": "FeatureCollection",
				"features": [{
					"type": "Feature",
					"geometry": {
						"type": "Point",
						"coordinates": [0.5, 0.5]
					},
					"properties": {
						"direction": "outbound",
						"line": "5"
					},
					"_embedded": {
						"line": {
							"id": "5",
							"name": "5",
							"title": "5",
							"description": "Test model",
							"colors": {
								"background": "#149934",
								"foreground": "#FFFFFF"
							},
							"href": "http://google.com"
						}
					}
				}]
			}
		"""
        val json = moshi.adapter(GeoJsonObject::class.java).fromJson(str)
        Assert.assertTrue(json is FeatureCollection)
        Assert.assertTrue((json as FeatureCollection).features[0].foreign!!["_embedded"] != null)
        Assert.assertTrue((json.features[0].foreign!!["_embedded"] as Map<*, *>)["line"] != null)

        val map = ((json.features[0].foreign!!["_embedded"] as Map<*, *>)["line"] as Map<*, *>)
        Assert.assertEquals("5", map["id"])
        Assert.assertTrue(map["colors"] is Map<*, *>)
        Assert.assertEquals("#149934", (map["colors"] as Map<*, *>)["background"])
    }

    @Test
    @Throws(Exception::class)
    fun testForeignMembers() {
        val point = moshi.adapter(GeoJsonObject::class.java).fromJson(
            """
			{
				"type": "Point",
				"coordinates": [0.5,0.5],
				"key": "value",
				"abc": "123"
			}
		"""
        )
        Assert.assertTrue(point is Point)
        Assert.assertNotNull(point!!.foreign)
        Assert.assertNotNull(point.foreign!!["key"])
        Assert.assertNotNull(point.foreign!!["abc"])
        Assert.assertEquals("value", point.foreign!!["key"])
        Assert.assertEquals("123", point.foreign!!["abc"])
    }

    @Test
    @Throws(Exception::class)
    fun itShouldDeserializeAPoint() {
        val value = moshi.adapter(Point::class.java).fromJson(GeoJsonData.pointJson)
        Assert.assertNotNull(value)
        Assert.assertTrue(value is Point)
        val point2 = value as Point
        assertLngLatAlt(0.0, 0.0, Double.NaN, point2.coordinates)
    }

    @Test
    @Throws(Exception::class)
    fun itShouldSerializeAPoint() {
        Assert.assertEquals(
            GeoJsonData.pointJson,
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
        Assert.assertTrue(!value.foreign.isNullOrEmpty())
        Assert.assertEquals("foreign", value.foreign!!["other"] as String)
        Assert.assertNotNull(value.bbox)
        Assert.assertEquals(0.5, value.bbox!![0], 0.00001)
        Assert.assertEquals(0.5, value.bbox!![1], 0.00001)
        Assert.assertEquals(0.5, value.bbox!![2], 0.00001)
        Assert.assertEquals(0.5, value.bbox!![3], 0.00001)

    }

    @Test
    @Throws(Exception::class)
    fun itShouldDeserializeLineString() {
        val lineString = moshi.adapter(LineString::class.java).fromJson(GeoJsonData.lineStringJson)
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
            GeoJsonData.lineStringJson,
            moshi.adapter(GeoJsonObject::class.java).toJson(GeoJsonData.lineStringObject)
        )
    }

    @Test
    @Throws(Exception::class)
    fun itShouldDeserializeMultiLineString() {
        val multiLineString =
            moshi.adapter(MultiLineString::class.java).fromJson(GeoJsonData.multiLineStringJson)
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
            GeoJsonData.multiLineStringJson,
            moshi.adapter(GeoJsonObject::class.java).toJson(GeoJsonData.multiLineStringObject)
        )
    }

    @Test
    @Throws(Exception::class)
    fun itShouldDeserializeMultiPoint() {
        val multiPoint = moshi.adapter(MultiPoint::class.java).fromJson(GeoJsonData.multiPointJson)
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
            GeoJsonData.multiPointJson,
            moshi.adapter(GeoJsonObject::class.java).toJson(GeoJsonData.multiPointObject)
        )
    }

    @Test
    @Throws(Exception::class)
    fun itShouldDeserializeMultiPolygon() {
        val multiPolygon =
            moshi.adapter(MultiPolygon::class.java).fromJson(GeoJsonData.multiPolygonJson)
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