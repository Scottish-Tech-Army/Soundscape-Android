package org.scottishtecharmy.soundscape

import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiLineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiPoint
import org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiPolygon
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon
import org.scottishtecharmy.soundscape.utils.bearingFromTwoPoints
import org.scottishtecharmy.soundscape.utils.createTriangleFOV
import org.scottishtecharmy.soundscape.utils.distance
import org.scottishtecharmy.soundscape.utils.getBoundingBoxCorners
import org.scottishtecharmy.soundscape.utils.getBoundingBoxOfLineString
import org.scottishtecharmy.soundscape.utils.getBoundingBoxOfMultiLineString
import org.scottishtecharmy.soundscape.utils.getBoundingBoxOfMultiPoint
import org.scottishtecharmy.soundscape.utils.getBoundingBoxOfMultiPolygon
import org.scottishtecharmy.soundscape.utils.getBoundingBoxOfPoint
import org.scottishtecharmy.soundscape.utils.getBoundingBoxOfPolygon
import org.scottishtecharmy.soundscape.utils.getCenterOfBoundingBox
import org.scottishtecharmy.soundscape.utils.getDestinationCoordinate
import org.scottishtecharmy.soundscape.utils.getPixelXY
import org.scottishtecharmy.soundscape.utils.getPolygonOfBoundingBox
import org.scottishtecharmy.soundscape.utils.getQuadKey
import org.scottishtecharmy.soundscape.utils.getQuadrants
import org.scottishtecharmy.soundscape.utils.getReferenceCoordinate
import org.scottishtecharmy.soundscape.utils.groundResolution
import org.scottishtecharmy.soundscape.utils.mapSize
import org.scottishtecharmy.soundscape.utils.pixelXYToLatLon
import org.scottishtecharmy.soundscape.utils.polygonContainsCoordinates
import org.junit.Assert
import org.junit.Test
import org.scottishtecharmy.soundscape.utils.calculateCenterOfCircle
import org.scottishtecharmy.soundscape.utils.distanceToPolygon
import org.scottishtecharmy.soundscape.utils.lineStringsIntersect
import org.scottishtecharmy.soundscape.utils.straightLinesIntersect
import kotlin.math.abs


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

    @Test
    fun distanceToPolygonTest(){
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
        val minDistanceToPolygon1 = distanceToPolygon(LngLatAlt(0.0, -1.0), polygonObject)
        Assert.assertEquals(111319.49, minDistanceToPolygon1, 0.1)
        val minDistanceToPolygon2 = distanceToPolygon(LngLatAlt(0.0, 2.0), polygonObject)
        Assert.assertEquals(111319.49, minDistanceToPolygon2, 0.1)
        val minDistanceToPolygon3 = distanceToPolygon(LngLatAlt(0.0, 0.0), polygonObject)
        Assert.assertEquals(0.0, minDistanceToPolygon3, 0.1)
    }

    @Test
    fun calculateCenterOfCircleFromSegmentTest(){
        //this test uses coordinates for a circle generated from:
        // val circleTest = circleToPolygon(32,51.4378656388476,-2.747654754997829, 5.0)
        // segments as linestrings/segments to test
        val segmentLineString1 = LineString().also {
            it.coordinates = arrayListOf(
                LngLatAlt(-2.7475827010838696,
                    51.437865683763356),
                LngLatAlt(-2.747584099636648,
                    51.43787444552683),
                LngLatAlt(-2.747588213435333,
                    51.437882868854565),
                LngLatAlt(-2.7475948843889473,
                    51.437890630042794),
                LngLatAlt(-2.7476038561364855,
                    51.4378974308334),
                LngLatAlt(-2.7476147838987206,
                    51.43790300987583
                ),
                LngLatAlt(-2.7476272477278783,
                    51.4379071527706
                ),
                LngLatAlt(-2.747640768645994,
                    51.437909700308595)
            )
        }
        val segmentLineString2 = LineString().also {
            it.coordinates = arrayListOf(
                LngLatAlt(-2.747654827051767,
                    51.437910554589344),
                LngLatAlt(-2.7476688826885476,
                    51.437909682783335),
                LngLatAlt(-2.747682395406097,
                    51.43790711839357),
                LngLatAlt(-2.7476948459182573,
                    51.43790295996812),
                LngLatAlt(-2.7477057557588287,
                    51.437897367312935),
                LngLatAlt(-2.747714705668757,
                    51.43789055535062
                ),
                LngLatAlt(-2.7477213517080217,
                    51.43788278586107
                ),
                LngLatAlt(-2.747725438473062,
                    51.43787435742141)
            )
        }
        val segmentLineString3 = LineString().also {
            it.coordinates = arrayListOf(
                LngLatAlt(-2.7477268089117883,
                    51.43786559393184),
                LngLatAlt(-2.74772541035901,
                    51.437856832168364),
                LngLatAlt(-2.747721296560325,
                    51.43784840884063),
                LngLatAlt(-2.7477146256067106,
                    51.4378406476524),
                LngLatAlt(-2.7477056538591724,
                    51.43783384686179),
                LngLatAlt(-2.7476947260969373,
                    51.43782826781936
                ),
                LngLatAlt(-2.7476822622677797,
                    51.437824124924596
                ),
                LngLatAlt(-2.747668741349664,
                    51.4378215773866)
            )
        }
        val segmentLineString4 = LineString().also {
            it.coordinates = arrayListOf(
                LngLatAlt(-2.747654682943891,
                    51.43782072310585),
                LngLatAlt( -2.7476406273071103,
                    51.43782159491186),
                LngLatAlt(-2.747627114589561,
                    51.43782415930163),
                LngLatAlt(-2.7476146640774006,
                    51.437828317727075),
                LngLatAlt(-2.7476037542368292,
                    51.43783391038226),
                LngLatAlt(-2.747594804326901,
                    51.437840722344575
                ),
                LngLatAlt(-2.7475881582876363,
                    51.437848491834124
                ),
                LngLatAlt(-2.747584071522596,
                    51.43785692027379)
            )
        }
        // minimum segment length possible (three points)
        val segmentLineString5 = LineString().also {
            it.coordinates = arrayListOf(
                LngLatAlt(-2.747654682943891,
                    51.43782072310585),
                LngLatAlt( -2.7476406273071103,
                    51.43782159491186),
                LngLatAlt(-2.747627114589561,
                    51.43782415930163)
            )
        }
        val actualCircleCenter = LngLatAlt(-2.747654754997829, 51.4378656388476)

        val circle1 = calculateCenterOfCircle(segmentLineString1)
        Assert.assertEquals(0.11, distance(actualCircleCenter.latitude, actualCircleCenter.longitude, circle1.center.latitude, circle1.center.longitude), 0.1)

        val circle2 = calculateCenterOfCircle(segmentLineString2)
        Assert.assertEquals(0.11, distance(actualCircleCenter.latitude, actualCircleCenter.longitude, circle2.center.latitude, circle2.center.longitude), 0.1)

        val circle3 = calculateCenterOfCircle(segmentLineString3)
        Assert.assertEquals(0.11, distance(actualCircleCenter.latitude, actualCircleCenter.longitude, circle3.center.latitude, circle3.center.longitude), 0.1)

        val circle4 = calculateCenterOfCircle(segmentLineString4)
        Assert.assertEquals(0.11, distance(actualCircleCenter.latitude, actualCircleCenter.longitude, circle4.center.latitude, circle4.center.longitude), 0.1)

        val circle5 = calculateCenterOfCircle(segmentLineString5)
        Assert.assertEquals(0.11, distance(actualCircleCenter.latitude, actualCircleCenter.longitude, circle5.center.latitude, circle5.center.longitude), 0.1)
    }

    @Test
    fun intersectingLinesTest(){

        val testLinesIntersect1 = straightLinesIntersect(
            LngLatAlt(0.0,0.0),
            LngLatAlt(0.0, 1.0),
            LngLatAlt(0.0, 0.0),
            LngLatAlt(1.0,1.0)
        )
        // touching lines should intersect
        Assert.assertEquals(true, testLinesIntersect1)

        val testLinesIntersect2 = straightLinesIntersect(
            LngLatAlt(0.0,0.0),
            LngLatAlt(0.0, 1.0),
            LngLatAlt(1.0, 0.0),
            LngLatAlt(1.0,1.0)
        )
        // parallel lines should not intersect
        Assert.assertEquals(false, testLinesIntersect2)

        val testLinesIntersect3 = straightLinesIntersect(
            LngLatAlt(0.0,0.5),
            LngLatAlt(1.0, 0.5),
            LngLatAlt(0.5, 0.0),
            LngLatAlt(0.5,1.0)
        )
        //crossing lines should intersect
        Assert.assertEquals(true, testLinesIntersect3)


    }

    @Test
    fun intersectingLineStringsTest(){
        val lineString1 = LineString().also {
            it.coordinates = arrayListOf(
                LngLatAlt(-2.6856311440872105,51.44095049507263),
                LngLatAlt(-2.6854046355432217,51.44085784067977),
                LngLatAlt(-2.6852524501146036,51.440941670852794)
            )
        }

        val lineString2 = LineString().also {
            it.coordinates = arrayListOf(
                LngLatAlt(-2.685340930014803,51.4409946161459),
                LngLatAlt(-2.6853480084065495,51.44081371947439)
            )
        }

        val lineString3 = LineString().also {
            it.coordinates = arrayListOf(
                LngLatAlt(-2.6851321174495126,51.44103432507555),
                LngLatAlt(-2.685135656646054,51.44082474977969)
            )
        }

        // These linestrings do intersect
        val intersect1 = lineStringsIntersect(lineString1, lineString2)
        Assert.assertEquals(true, intersect1)

        // These linestrings do not intersect
        val intersect2 = lineStringsIntersect(lineString1, lineString3)
        Assert.assertEquals(false, intersect2)
    }
}