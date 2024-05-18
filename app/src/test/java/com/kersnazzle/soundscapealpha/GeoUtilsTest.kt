package com.kersnazzle.soundscapealpha

import com.kersnazzle.soundscapealpha.geojsonparser.geojson.LineString
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.LngLatAlt
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.MultiPoint
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.Point
import com.kersnazzle.soundscapealpha.utils.distance
import com.kersnazzle.soundscapealpha.utils.getBoundingBoxOfLineString
import com.kersnazzle.soundscapealpha.utils.getBoundingBoxOfMultiPoint
import com.kersnazzle.soundscapealpha.utils.getBoundingBoxOfPoint
import com.kersnazzle.soundscapealpha.utils.getPixelXY
import com.kersnazzle.soundscapealpha.utils.getQuadKey
import com.kersnazzle.soundscapealpha.utils.groundResolution
import com.kersnazzle.soundscapealpha.utils.mapSize
import com.kersnazzle.soundscapealpha.utils.pixelXYToLatLon
import org.junit.Assert
import org.junit.Test

class GeoUtilsTest {

    @Test
    fun getDistanceBetweenTwoCoordinatesTest() {
        // Used this to check https://www.omnicalculator.com/other/latitude-longitude-distance
        // However Soundscape uses a different earth radius
        val testDistanceBetweenTwoPoints = distance(
            0.0,
            0.0,
            0.0,
            1.0
        )
        // could be 111194.93 depending on earth radius const used
        Assert.assertEquals(111319.49, testDistanceBetweenTwoPoints, 0.01)
    }

    @Test
    fun getMapSizeTest() {
        val testMapSize = mapSize(16)
        Assert.assertEquals(16777216, testMapSize)
    }

    @Test
    fun getGroundResolutionTest() {
        // test ground resolution in meter per pixel at the equator
        val testGroundResolution = groundResolution(0.0, 16)
        Assert.assertEquals(2.38, testGroundResolution, 0.01)
    }

    @Test
    fun getQuadKeyTest() {
        val testQuadKey1 = getQuadKey(3, 5, 3)
        Assert.assertEquals("213", testQuadKey1)
        val testQuadKey2 = getQuadKey(8619, 5859, 14)
        Assert.assertEquals("12022132301033", testQuadKey2)
        val testQuadKey3 = getQuadKey(32277, 21812, 16)
        Assert.assertEquals("0313131200230301", testQuadKey3)
    }

    @Test
    fun getPixelXYTest() {
        val testGetPixelXY = getPixelXY(51.43699, -2.693095, 16)
        Assert.assertEquals(8263101.176323555, testGetPixelXY.first, 0.0000001)
        Assert.assertEquals(5584120.917661605, testGetPixelXY.second, 0.0000001)
    }

    @Test
    fun pixelXYToLatLonTest() {
        val testPixelXY = pixelXYToLatLon(8263101.176323555, 5584120.917661605, 16)
        Assert.assertEquals(51.43699, testPixelXY.first, 0.00001)
        Assert.assertEquals(-2.693095, testPixelXY.second, 0.0001)
    }

    @Test
    fun getBoundingBoxOfPointTest() {
        val testPoint = Point(0.5, 0.5)
        // testing min lon, min lat, max lon, max lat
        val testBoundingBox = getBoundingBoxOfPoint(testPoint)
        Assert.assertEquals(0.5, testBoundingBox.westLongitude, 0.1)
        Assert.assertEquals(0.5, testBoundingBox.southLatitude, 0.1)
        Assert.assertEquals(0.5, testBoundingBox.eastLongitude, 0.1)
        Assert.assertEquals(0.5, testBoundingBox.northLatitude, 0.1)

    }

    @Test
    fun getBoundingBoxOfLineStringTest() {
        // simplest line string made of two coordinates (diagonal)
        val testLngLatAlt1 = LngLatAlt(0.0, 1.0)
        val testLngLatAlt2 = LngLatAlt(1.0, 0.0)
        val testLineString1 = LineString(testLngLatAlt1, testLngLatAlt2)
        val testBoundingBox1 = getBoundingBoxOfLineString(testLineString1)
        Assert.assertEquals(0.0, testBoundingBox1.westLongitude, 0.01)
        Assert.assertEquals(0.0, testBoundingBox1.southLatitude, 0.01)
        Assert.assertEquals(1.0, testBoundingBox1.eastLongitude, 0.01)
        Assert.assertEquals(1.0, testBoundingBox1.northLatitude, 0.01)

        // three point line string
        val testThreePointLineString1 = LngLatAlt(0.0, 1.0)
        val testThreePointLineString2 = LngLatAlt(1.0, 0.0)
        val testThreePointLineString3 = LngLatAlt(2.0, 2.0)
        val testLineString2 = LineString(
            testThreePointLineString1,
            testThreePointLineString2,
            testThreePointLineString3
        )
        val testBoundingBox2 = getBoundingBoxOfLineString(testLineString2)
        Assert.assertEquals(0.0, testBoundingBox2.westLongitude, 0.01)
        Assert.assertEquals(0.0, testBoundingBox2.southLatitude, 0.01)
        Assert.assertEquals(2.0, testBoundingBox2.eastLongitude, 0.01)
        Assert.assertEquals(2.0, testBoundingBox2.northLatitude, 0.01)
    }

    @Test
    fun getBoundingBoxOfMultiPointsTest() {
        val multiLineStringObject = MultiPoint().also {
            it.coordinates = arrayListOf(
                LngLatAlt(0.0, 0.0),
                LngLatAlt(1.0, 1.0)
            )
        }
        val testBoundingBox = getBoundingBoxOfMultiPoint(multiLineStringObject)
        Assert.assertEquals(0.0, testBoundingBox.westLongitude, 0.01)
        Assert.assertEquals(0.0, testBoundingBox.southLatitude, 0.01)
        Assert.assertEquals(1.0, testBoundingBox.eastLongitude, 0.01)
        Assert.assertEquals(1.0, testBoundingBox.northLatitude, 0.01)

    }




}