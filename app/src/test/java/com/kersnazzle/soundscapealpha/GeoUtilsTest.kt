package com.kersnazzle.soundscapealpha

import com.kersnazzle.soundscapealpha.geojsonparser.geojson.LineString
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.LngLatAlt
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.MultiLineString
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.MultiPoint
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.MultiPolygon
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.Point
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.Polygon
import com.kersnazzle.soundscapealpha.utils.bearingFromTwoPoints
import com.kersnazzle.soundscapealpha.utils.createTriangleFOV
import com.kersnazzle.soundscapealpha.utils.distance
import com.kersnazzle.soundscapealpha.utils.getBoundingBoxCorners
import com.kersnazzle.soundscapealpha.utils.getBoundingBoxOfLineString
import com.kersnazzle.soundscapealpha.utils.getBoundingBoxOfMultiLineString
import com.kersnazzle.soundscapealpha.utils.getBoundingBoxOfMultiPoint
import com.kersnazzle.soundscapealpha.utils.getBoundingBoxOfMultiPolygon
import com.kersnazzle.soundscapealpha.utils.getBoundingBoxOfPoint
import com.kersnazzle.soundscapealpha.utils.getBoundingBoxOfPolygon
import com.kersnazzle.soundscapealpha.utils.getCenterOfBoundingBox
import com.kersnazzle.soundscapealpha.utils.getDestinationCoordinate
import com.kersnazzle.soundscapealpha.utils.getPixelXY
import com.kersnazzle.soundscapealpha.utils.getPolygonOfBoundingBox
import com.kersnazzle.soundscapealpha.utils.getQuadKey
import com.kersnazzle.soundscapealpha.utils.getQuadrants
import com.kersnazzle.soundscapealpha.utils.getReferenceCoordinate
import com.kersnazzle.soundscapealpha.utils.groundResolution
import com.kersnazzle.soundscapealpha.utils.mapSize
import com.kersnazzle.soundscapealpha.utils.pixelXYToLatLon
import com.kersnazzle.soundscapealpha.utils.polygonContainsCoordinates
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

    @Test
    fun getBoundingBoxOfMultiLineStringTest() {
        val multiLineStringObject = MultiLineString().also {
            it.coordinates = arrayListOf(
                arrayListOf(
                    LngLatAlt(0.0, 0.0),
                    LngLatAlt(0.0, 1.0)
                ),
                arrayListOf(
                    LngLatAlt(1.0, 0.0),
                    LngLatAlt(1.0, 1.0)
                )
            )
        }
        val boundingBoxOfMultiLineString = getBoundingBoxOfMultiLineString(multiLineStringObject)
        Assert.assertEquals(0.0, boundingBoxOfMultiLineString.westLongitude, 0.01)
        Assert.assertEquals(0.0, boundingBoxOfMultiLineString.southLatitude, 0.01)
        Assert.assertEquals(1.0, boundingBoxOfMultiLineString.eastLongitude, 0.01)
        Assert.assertEquals(1.0, boundingBoxOfMultiLineString.northLatitude, 0.01)
    }

    @Test
    fun getBoundingBoxOfPolygonTest() {
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
        val boundingBoxOfPolygon = getBoundingBoxOfPolygon(polygonObject)
        // min lon
        Assert.assertEquals(0.0, boundingBoxOfPolygon.westLongitude, 0.000001)
        //min lat
        Assert.assertEquals(0.0, boundingBoxOfPolygon.southLatitude, 0.000001)
        // max lon
        Assert.assertEquals(1.0, boundingBoxOfPolygon.eastLongitude, 0.00001)
        // max lat
        Assert.assertEquals(1.0, boundingBoxOfPolygon.northLatitude, 0.000001)

    }

    @Test
    fun getBoundingBoxOfMultiPolygonTest() {
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
        val boundingBoxOfMultiPolygon = getBoundingBoxOfMultiPolygon(multiPolygonObject)
        // min lon
        Assert.assertEquals(0.0, boundingBoxOfMultiPolygon.westLongitude, 0.000001)
        //min lat
        Assert.assertEquals(0.0, boundingBoxOfMultiPolygon.southLatitude, 0.000001)
        //max lon
        Assert.assertEquals(2.0, boundingBoxOfMultiPolygon.eastLongitude, 0.000001)
        // max lat
        Assert.assertEquals(2.0, boundingBoxOfMultiPolygon.northLatitude, 0.000001)

    }

    @Test
    fun getBoundingBoxCornersTest() {
        val testLngLatAlt1 = LngLatAlt(0.0, 1.0)
        val testLngLatAlt2 = LngLatAlt(1.0, 0.0)
        val testLineString1 = LineString(testLngLatAlt1, testLngLatAlt2)
        val testBoundingBox1 = getBoundingBoxOfLineString(testLineString1)

        val testBoundingBoxCorners = getBoundingBoxCorners(testBoundingBox1)

        Assert.assertEquals(0.0, testBoundingBoxCorners.northWestCorner.longitude, 0.1)
        Assert.assertEquals(1.0, testBoundingBoxCorners.northWestCorner.latitude, 0.1)

        Assert.assertEquals(0.0, testBoundingBoxCorners.southWestCorner.longitude, 0.1)
        Assert.assertEquals(0.0, testBoundingBoxCorners.southWestCorner.latitude, 0.1)

        Assert.assertEquals(1.0, testBoundingBoxCorners.southEastCorner.longitude, 0.1)
        Assert.assertEquals(0.0, testBoundingBoxCorners.southEastCorner.latitude, 0.1)

        Assert.assertEquals(1.0, testBoundingBoxCorners.northEastCorner.longitude, 0.1)
        Assert.assertEquals(1.0, testBoundingBoxCorners.northEastCorner.latitude, 0.1)

    }

    @Test
    fun getCenterOfBoundingBoxTest() {
        val testLngLatAlt1 = LngLatAlt(0.0, 1.0)
        val testLngLatAlt2 = LngLatAlt(1.0, 0.0)
        val testLineString1 = LineString(testLngLatAlt1, testLngLatAlt2)
        val testBoundingBox1 = getBoundingBoxOfLineString(testLineString1)
        val testBoundingBoxCorners1 = getBoundingBoxCorners(testBoundingBox1)

        val bbCenter1 = getCenterOfBoundingBox(testBoundingBoxCorners1)

        Assert.assertEquals(0.5, bbCenter1.latitude, 0.000001)
        Assert.assertEquals(0.5, bbCenter1.longitude, 0.000001)
    }

    @Test
    fun getPolygonFromBoundingBoxTest() {
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
        val boundingBoxOfPolygon = getBoundingBoxOfPolygon(polygonObject)

        val polygonOfBoundingBox = getPolygonOfBoundingBox(boundingBoxOfPolygon)
        // Northwest corner
        Assert.assertEquals(0.0, polygonOfBoundingBox.coordinates[0][0].longitude, 0.000001)
        Assert.assertEquals(1.0, polygonOfBoundingBox.coordinates[0][0].latitude, 0.000001)
        // Southwest corner
        Assert.assertEquals(0.0, polygonOfBoundingBox.coordinates[0][1].longitude, 0.000001)
        Assert.assertEquals(0.0, polygonOfBoundingBox.coordinates[0][1].latitude, 0.000001)
        // Southeast corner
        Assert.assertEquals(1.0, polygonOfBoundingBox.coordinates[0][2].longitude, 0.000001)
        Assert.assertEquals(0.0, polygonOfBoundingBox.coordinates[0][2].latitude, 0.000001)
        //Northeast corner
        Assert.assertEquals(1.0, polygonOfBoundingBox.coordinates[0][3].longitude, 0.000001)
        Assert.assertEquals(1.0, polygonOfBoundingBox.coordinates[0][3].latitude, 0.000001)
        // Close polygon so Northwest corner again
        Assert.assertEquals(0.0, polygonOfBoundingBox.coordinates[0][4].longitude, 0.000001)
        Assert.assertEquals(1.0, polygonOfBoundingBox.coordinates[0][4].latitude, 0.000001)

    }

    @Test
    fun getBearingForTwoPointsSouthToNorthTest() {

        val testBearingBetweenTwoPointsSouthToNorth = bearingFromTwoPoints(
            0.0,
            0.0,
            1.0,
            0.0
        )
        Assert.assertEquals(0.0, testBearingBetweenTwoPointsSouthToNorth, 0.1)

        val testBearingBetweenTwoPointsNorthToSouth = bearingFromTwoPoints(
            1.0,
            0.0,
            0.0,
            0.0
        )
        Assert.assertEquals(180.0, testBearingBetweenTwoPointsNorthToSouth, 0.1)
    }

    @Test
    fun polygonContainsCoordinatesTest() {

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

        val testPolygon1 =
            polygonContainsCoordinates(LngLatAlt(0.5, 0.5), polygonObject)
        Assert.assertEquals(true, testPolygon1)
        val testPolygon2 =
            polygonContainsCoordinates(LngLatAlt(2.0, 2.0), polygonObject)
        Assert.assertEquals(false, testPolygon2)

    }

    @Test
    fun getDestinationCoordinateTest(){

        val destinationCoordinateTest = getDestinationCoordinate(
            LngLatAlt(0.0, 0.0),
            0.0,
            111319.49)
        Assert.assertEquals(0.0, destinationCoordinateTest.longitude, 0.000001)
        Assert.assertEquals(1.0, destinationCoordinateTest.latitude, 0.000001)
    }

    @Test
    fun getReferenceCoordinateTest(){
        val lineStringObject = LineString().also {
            it.coordinates = arrayListOf(
                LngLatAlt(0.0, 0.0),
                LngLatAlt(1.0, 0.0)
            )
        }
        val distanceBetweenCoordinates = distance(0.0, 0.0, 0.0, 1.0)
        Assert.assertEquals(111319.49,distanceBetweenCoordinates, 0.1)

        val referenceCoordinateTest = getReferenceCoordinate(
            lineStringObject,
            distanceBetweenCoordinates/2,
            false
        )
        Assert.assertEquals(0.5, referenceCoordinateTest.longitude, 0.000001)
        Assert.assertEquals(0.0, referenceCoordinateTest.latitude, 0.000001)
    }

    @Test
    fun createTriangleFOVTest(){

        val polygonTriangleFOV = createTriangleFOV(
            LngLatAlt(0.0, 1.0),
            LngLatAlt(0.5, 0.0),
            LngLatAlt(1.0, 1.0)
        )

        Assert.assertEquals(0.0, polygonTriangleFOV.coordinates[0][0].longitude, 0.01)
        Assert.assertEquals(1.0, polygonTriangleFOV.coordinates[0][0].latitude, 0.01)
        Assert.assertEquals(0.5, polygonTriangleFOV.coordinates[0][1].longitude, 0.01)
        Assert.assertEquals(0.0, polygonTriangleFOV.coordinates[0][1].latitude, 0.01)
        Assert.assertEquals(1.0, polygonTriangleFOV.coordinates[0][2].longitude, 0.01)
        Assert.assertEquals(1.0, polygonTriangleFOV.coordinates[0][2].latitude, 0.01)
        // check it is closed
        Assert.assertEquals(0.0, polygonTriangleFOV.coordinates[0][3].longitude, 0.01)
        Assert.assertEquals(1.0, polygonTriangleFOV.coordinates[0][3].latitude, 0.01)
    }

    @Test
    fun getQuadrantsTest() {

        val testQuadrant1 = getQuadrants(0.0)
        Assert.assertEquals(315.0, testQuadrant1[0].left, 0.01)
        Assert.assertEquals(45.0, testQuadrant1[0].right, 0.01)
        Assert.assertEquals(45.0, testQuadrant1[1].left, 0.01)
        Assert.assertEquals(135.0, testQuadrant1[1].right, 0.01)
        Assert.assertEquals(135.0, testQuadrant1[2].left, 0.01)
        Assert.assertEquals(225.0, testQuadrant1[2].right, 0.01)
        Assert.assertEquals(225.0, testQuadrant1[3].left, 0.01)
        Assert.assertEquals(315.0, testQuadrant1[3].right, 0.01)

        val testQuadrant2 = getQuadrants(95.0)
        Assert.assertEquals(320.0, testQuadrant2[0].left, 0.01)
        Assert.assertEquals(50.0, testQuadrant2[0].right, 0.01)
        Assert.assertEquals(50.0, testQuadrant2[1].left, 0.01)
        Assert.assertEquals(140.0, testQuadrant2[1].right, 0.01)
        Assert.assertEquals(140.0, testQuadrant2[2].left, 0.01)
        Assert.assertEquals(230.0, testQuadrant2[2].right, 0.01)
        Assert.assertEquals(230.0, testQuadrant2[3].left, 0.01)
        Assert.assertEquals(320.0, testQuadrant2[3].right, 0.01)

        val testQuadrant3 = getQuadrants(230.0)
        Assert.assertEquals(275.0, testQuadrant3[0].left, 0.01)
        Assert.assertEquals(5.0, testQuadrant3[0].right, 0.01)
        Assert.assertEquals(5.0, testQuadrant3[1].left, 0.01)
        Assert.assertEquals(95.0, testQuadrant3[1].right, 0.01)
        Assert.assertEquals(95.0, testQuadrant3[2].left, 0.01)
        Assert.assertEquals(185.0, testQuadrant3[2].right, 0.01)
        Assert.assertEquals(185.0, testQuadrant3[3].left, 0.01)
        Assert.assertEquals(275.0, testQuadrant3[3].right, 0.01)
    }



}