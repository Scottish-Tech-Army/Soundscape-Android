package org.scottishtecharmy.soundscape

import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiLineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiPoint
import org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiPolygon
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon
import org.scottishtecharmy.soundscape.geoengine.utils.bearingFromTwoPoints
import org.scottishtecharmy.soundscape.geoengine.utils.distance
import org.scottishtecharmy.soundscape.geoengine.utils.getBoundingBoxCorners
import org.scottishtecharmy.soundscape.geoengine.utils.getBoundingBoxOfLineString
import org.scottishtecharmy.soundscape.geoengine.utils.getBoundingBoxOfMultiLineString
import org.scottishtecharmy.soundscape.geoengine.utils.getBoundingBoxOfMultiPoint
import org.scottishtecharmy.soundscape.geoengine.utils.getBoundingBoxOfPoint
import org.scottishtecharmy.soundscape.geoengine.utils.getBoundingBoxOfPolygon
import org.scottishtecharmy.soundscape.geoengine.utils.getCenterOfBoundingBox
import org.scottishtecharmy.soundscape.geoengine.utils.getDestinationCoordinate
import org.scottishtecharmy.soundscape.geoengine.utils.getPixelXY
import org.scottishtecharmy.soundscape.geoengine.utils.getPolygonOfBoundingBox
import org.scottishtecharmy.soundscape.geoengine.utils.getReferenceCoordinate
import org.scottishtecharmy.soundscape.geoengine.utils.groundResolution
import org.scottishtecharmy.soundscape.geoengine.utils.mapSize
import org.scottishtecharmy.soundscape.geoengine.utils.pixelXYToLatLon
import org.scottishtecharmy.soundscape.geoengine.utils.polygonContainsCoordinates
import org.junit.Assert
import org.junit.Test
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.geoengine.utils.Triangle
import org.scottishtecharmy.soundscape.geoengine.utils.calculateCenterOfCircle
import org.scottishtecharmy.soundscape.geoengine.utils.circleToPolygon
import org.scottishtecharmy.soundscape.geoengine.utils.createPolygonFromTriangle
import org.scottishtecharmy.soundscape.geoengine.utils.distanceToPolygon
import org.scottishtecharmy.soundscape.geoengine.utils.getBoundingBoxesOfMultiPolygon
import org.scottishtecharmy.soundscape.geoengine.utils.lineStringsIntersect
import org.scottishtecharmy.soundscape.geoengine.utils.mergePolygons
import org.scottishtecharmy.soundscape.geoengine.utils.straightLinesIntersect
import org.scottishtecharmy.soundscape.geoengine.utils.straightLinesIntersectLngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.moshi.GeoJsonObjectMoshiAdapter
import java.io.FileOutputStream


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
        val boundingBoxesOfMultiPolygon = getBoundingBoxesOfMultiPolygon(multiPolygonObject)
        val firstBox = boundingBoxesOfMultiPolygon[0]
        // min lon
        Assert.assertEquals(0.0, firstBox.westLongitude, 0.000001)
        //min lat
        Assert.assertEquals(0.0, firstBox.southLatitude, 0.000001)
        //max lon
        Assert.assertEquals(1.0, firstBox.eastLongitude, 0.000001)
        // max lat
        Assert.assertEquals(1.0, firstBox.northLatitude, 0.000001)

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
            LngLatAlt(0.0, 0.0),
            LngLatAlt(0.0, 1.0))
        Assert.assertEquals(0.0, testBearingBetweenTwoPointsSouthToNorth, 0.1)

        val testBearingBetweenTwoPointsNorthToSouth = bearingFromTwoPoints(
            LngLatAlt(0.0,1.0),
            LngLatAlt(0.0,0.0)
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

        val polygonTriangleFOV = createPolygonFromTriangle(
            Triangle(
                LngLatAlt(0.5, 0.0),    // origin
                LngLatAlt(0.0, 1.0),    // left
                LngLatAlt(1.0, 1.0)     // right
            )
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
    fun distanceToPolygonTest(){
        val lngLat = 0.01
        val accuracy = 0.01
        val ruler = CheapRuler(lngLat)
        val polygonObject = Polygon().also {
            it.coordinates = arrayListOf(
                arrayListOf(
                    LngLatAlt(0.0, lngLat),
                    LngLatAlt(0.0, 0.0),
                    LngLatAlt(lngLat, 0.0),
                    LngLatAlt(lngLat, lngLat),
                    LngLatAlt(0.0, lngLat)
                )
            )
        }
        val minDistanceToPolygon1 = distanceToPolygon(LngLatAlt(0.0, -lngLat), polygonObject, ruler)
        Assert.assertEquals(1105.74, minDistanceToPolygon1, accuracy)
        val minDistanceToPolygon2 = distanceToPolygon(LngLatAlt(0.0, lngLat * 2), polygonObject, ruler)
        Assert.assertEquals(1105.74, minDistanceToPolygon2, accuracy)
        val minDistanceToPolygon3 = distanceToPolygon(LngLatAlt(0.0, 0.0), polygonObject, ruler)
        Assert.assertEquals(0.0, minDistanceToPolygon3, accuracy)
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
        Assert.assertEquals(0.11, distance(actualCircleCenter.latitude, actualCircleCenter.longitude, circle5.center.latitude, circle5.center.longitude), 0.11)
    }


    fun cheapTestPoint(ruler: CheapRuler, point: LngLatAlt, line: LineString, fc: FeatureCollection) {
        val pdh = ruler.distanceToLineString(point, line)
        val pointFeature1 = Feature()
        pointFeature1.geometry = Point(point.longitude, point.latitude)
        pointFeature1.properties = hashMapOf()
        fc.addFeature(pointFeature1)

        val pointFeature2 = Feature()
        pointFeature2.geometry = Point(pdh.point.longitude, pdh.point.latitude)
        pointFeature2.properties = hashMapOf()
        fc.addFeature(pointFeature2)
    }

    @Test
    fun cheapRulerTest() {
        // Create circle
        val circle = circleToPolygon(
            32,
            51.431658,
            -2.653228,
            200.0
        )
        // Turn the circle into a line
        val line = LineString()
        line.coordinates.addAll(circle.coordinates[0])

        // Test some nearest points on the line
        val ruler = CheapRuler(51.431658)
        val fc = FeatureCollection()

        val testCircle = circleToPolygon(
            320,
            51.431658,
            -2.653228,
            190.0
        )
        for(point in testCircle.coordinates[0]) {
            cheapTestPoint(ruler, point, line, fc)
        }

        val lineFeature = Feature()
        lineFeature.geometry = line
        lineFeature.properties = hashMapOf()
        fc. addFeature(lineFeature)

        val adapter = GeoJsonObjectMoshiAdapter()
        val mapMatchingOutput = FileOutputStream("cheap-ruler.geojson")
        mapMatchingOutput.write(adapter.toJson(fc).toByteArray())
        mapMatchingOutput.close()
    }

    @Test
    fun intersectingLinesTest(){

        val testLinesIntersectLngLatAlt1 = straightLinesIntersect(
            LngLatAlt(0.0,0.5),
            LngLatAlt(1.0, 0.5),
            LngLatAlt(0.5, 0.0),
            LngLatAlt(0.5,1.0)
        )

        //we have a vertical and a horizontal line here (cross), so they should intersect at 0.5, 0.5
        Assert.assertEquals(true, testLinesIntersectLngLatAlt1)

        val testLinesIntersectLngLatAlt2 = straightLinesIntersect(
            LngLatAlt(0.0,0.5),
            LngLatAlt(1.0, 0.5),
            LngLatAlt(0.0, 0.0),
            LngLatAlt(0.0,1.0)
        )
        //crossing lines (T shape) should intersect
        Assert.assertEquals(true, testLinesIntersectLngLatAlt2)

        val testLinesIntersectLngLatAlt3 = straightLinesIntersect(
            LngLatAlt(0.0,0.0),
            LngLatAlt (1.0,1.0),
            LngLatAlt(1.0,0.0),
            LngLatAlt(0.0, 1.0)
        )
        // These lines are diagonal and should intersect
        Assert.assertEquals(true, testLinesIntersectLngLatAlt3)

        val testLinesIntersectLngLatAlt4 = straightLinesIntersect(
            LngLatAlt(0.0, 0.0),
            LngLatAlt(1.0, 0.0),
            LngLatAlt(0.0, 1.0),
            LngLatAlt(1.0, 1.0)
        )
        //These lines are horizontal and parallel and should not intersect
        Assert.assertEquals(false, testLinesIntersectLngLatAlt4)

        val testLinesIntersectLngLatAlt5 = straightLinesIntersect(
            LngLatAlt(0.0, 0.0),
            LngLatAlt(1.0, 0.0),
            LngLatAlt(0.0, 0.0),
            LngLatAlt(1.0, 0.0)
        )
        // these lines are both horizontal and occupy the same coordinates and should intersect
        Assert.assertEquals(true, testLinesIntersectLngLatAlt5)

        val testLinesIntersectLngLatAlt6 = straightLinesIntersect(
            LngLatAlt(0.0, 0.0),
            LngLatAlt(0.0, 1.0),
            LngLatAlt(1.0, 0.0),
            LngLatAlt(1.0, 1.0)
        )
        // These lines are both vertical and parallel and do not intersect
        Assert.assertEquals(false, testLinesIntersectLngLatAlt6)

        val testLinesIntersectLngLatAlt7 = straightLinesIntersect(
            LngLatAlt(0.0, 0.0),
            LngLatAlt(0.0, 1.0),
            LngLatAlt(0.0, 0.0),
            LngLatAlt(1.0, 0.0)
        )
        //One line is vertical and one horizontal but they intersect
        Assert.assertEquals(true, testLinesIntersectLngLatAlt7)

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

        // These linestrings do intersect - they share a vertex
        val intersect3 = straightLinesIntersect(
            LngLatAlt(-4.305750131607056,55.947229354934144),
            LngLatAlt(-4.30576890707016,55.947257891396816),
            LngLatAlt(-4.306174131587852,55.946848076693094),
            LngLatAlt(-4.305750131607056,55.947229354934144)
        )
        Assert.assertEquals(true, intersect3)
    }

    @Test
    fun intersectingStraightLngLatAltTest(){
        val testLinesIntersectLngLatAlt1 = straightLinesIntersectLngLatAlt(
            LngLatAlt(0.0,0.5),
            LngLatAlt(1.0, 0.5),
            LngLatAlt(0.5, 0.0),
            LngLatAlt(0.5,1.0)
        )

        //we have a vertical and a horizontal line here (cross), so they should intersect at 0.5, 0.5
        Assert.assertEquals(LngLatAlt(0.5, 0.5), testLinesIntersectLngLatAlt1)

        val testLinesIntersectLngLatAlt2 = straightLinesIntersectLngLatAlt(
            LngLatAlt(0.0,0.5),
            LngLatAlt(1.0, 0.5),
            LngLatAlt(0.0, 0.0),
            LngLatAlt(0.0,1.0)
        )
        //crossing lines (T shape) should intersect at 0.0, 0.5 as we have a horizontal line and a vertical line
        Assert.assertEquals(LngLatAlt(0.0, 0.5), testLinesIntersectLngLatAlt2)

        val testLinesIntersectLngLatAlt3 = straightLinesIntersectLngLatAlt(
            LngLatAlt(0.0,0.0),
            LngLatAlt (1.0,1.0),
            LngLatAlt(1.0,0.0),
            LngLatAlt(0.0, 1.0)
        )
        // These lines are diagonal and should cross at 0.5, 0.5
        Assert.assertEquals(LngLatAlt(0.5, 0.5), testLinesIntersectLngLatAlt3)

        val testLinesIntersectLngLatAlt4 = straightLinesIntersectLngLatAlt(
            LngLatAlt(0.0, 0.0),
            LngLatAlt(1.0, 0.0),
            LngLatAlt(0.0, 1.0),
            LngLatAlt(1.0, 1.0)
        )
        //These lines are horizontal and parallel and should not intersect
        Assert.assertEquals(null, testLinesIntersectLngLatAlt4)

        val testLinesIntersectLngLatAlt5 = straightLinesIntersectLngLatAlt(
            LngLatAlt(0.0, 0.0),
            LngLatAlt(1.0, 0.0),
            LngLatAlt(0.0, 0.0),
            LngLatAlt(1.0, 0.0)
        )
        // these lines are both horizontal and occupy the same coordinates and should intersect at 0.0, 0.0
        Assert.assertEquals(LngLatAlt(0.0, 0.0), testLinesIntersectLngLatAlt5)

        val testLinesIntersectLngLatAlt6 = straightLinesIntersectLngLatAlt(
            LngLatAlt(0.0, 0.0),
            LngLatAlt(0.0, 1.0),
            LngLatAlt(1.0, 0.0),
            LngLatAlt(1.0, 1.0)
        )
        // These lines are both vertical and parallel and do not intersect
        Assert.assertEquals(null, testLinesIntersectLngLatAlt6)

        val testLinesIntersectLngLatAlt7 = straightLinesIntersectLngLatAlt(
            LngLatAlt(0.0, 0.0),
            LngLatAlt(0.0, 1.0),
            LngLatAlt(0.0, 0.0),
            LngLatAlt(1.0, 0.0)
        )
        //One line is vertical and one horizontal but they intersect at 0.0, 0.0 (L shape)
        Assert.assertEquals(LngLatAlt(0.0, 0.0), testLinesIntersectLngLatAlt7)

    }






    @Test
    fun nearestCoordinateOnPolygonSegmentTest(){

        val currentLocation1 = LngLatAlt(0.0, 0.0)
        //closed triangle/polygon
        val polygon1 = Polygon().also {
            it.coordinates = arrayListOf(
                arrayListOf(
                    LngLatAlt(0.0, 1.0),
                    LngLatAlt(1.0, 1.0),
                    LngLatAlt(1.0, 0.0),
                    LngLatAlt(0.0, 1.0),
                )
            )
        }

        val nearestDistance1 = distanceToPolygon(currentLocation1, polygon1, currentLocation1.createCheapRuler())
        val nearestPoint1 = nearestPointOnPolygonSegment(currentLocation1, polygon1)

        // distance to midpoint of hypotenuse (large error used is because the distance doesn't use
        // haversine calculation and so over 78km is off by a large amount)
        Assert.assertEquals(78714.27, nearestDistance1, 300.0) // We're using CheapRuler which is inaccurate at these distances
        // midpoint of hypotenuse
        Assert.assertEquals(LngLatAlt(0.5, 0.5), nearestPoint1)

    }

    private fun nearestPointOnPolygonSegment(point: LngLatAlt, polygon: Polygon): LngLatAlt? {
        var nearestPoint: LngLatAlt? = null
        var minDistance = Double.MAX_VALUE


        for (ring in polygon.coordinates) {
            // not sure if we really need to do this as any point on the outer ring is going to be closer than the inner ring(s)
            // assuming the location is outside the polygon
            for (i in ring.indices) {
                val start = ring[i]
                val end = ring[(i + 1) % ring.size]

                val nearestPointOnSegment = nearestPointOnSegment(point, start, end)
                val distance = distance(point.latitude, point.longitude, nearestPointOnSegment.latitude, nearestPointOnSegment.longitude)

                if (distance < minDistance) {
                    minDistance = distance
                    nearestPoint = nearestPointOnSegment
                }
            }
        }

        return nearestPoint
    }

    private fun nearestPointOnSegment(point: LngLatAlt, start: LngLatAlt, end: LngLatAlt): LngLatAlt {
        val segment = subtractLngLatAlt(end, start)
        val segmentLengthSquared = dotProductLngLatAlt(segment, segment)

        if (segmentLengthSquared == 0.0) {
            return start
        }

        val t = (dotProductLngLatAlt(subtractLngLatAlt(point, start), segment) / segmentLengthSquared).coerceIn(0.0, 1.0)
        return addLngLatAlt(start, multiplyLngLatAltByScalar(segment, t))
    }

    private fun subtractLngLatAlt(a: LngLatAlt, b: LngLatAlt): LngLatAlt {
        return LngLatAlt(a.longitude - b.longitude, a.latitude - b.latitude, a.altitude)
    }

    private fun addLngLatAlt(a: LngLatAlt, b: LngLatAlt): LngLatAlt {
        return LngLatAlt(a.longitude + b.longitude, a.latitude + b.latitude, a.altitude)
    }

    private fun multiplyLngLatAltByScalar(a: LngLatAlt, scalar: Double): LngLatAlt {
        return LngLatAlt(a.longitude * scalar, a.latitude * scalar, a.altitude)
    }

    private fun dotProductLngLatAlt(a: LngLatAlt, b: LngLatAlt): Double {
        return a.longitude * b.longitude + a.latitude * b.latitude
    }


    @Test
    fun mergePolygonsTest1() {

        // Merge polygons to create a hole

        // Rectangle
        val polygon1 = Polygon().also {
            it.coordinates = arrayListOf(
                arrayListOf(
                    LngLatAlt(0.0, 2.0),
                    LngLatAlt(1.0, 2.0),
                    LngLatAlt(1.0, 0.0),
                    LngLatAlt(0.0, 0.0),
                    LngLatAlt(0.0, 2.0),
                )
            )
        }

        // Reversed C that overlaps with the Rectangle
        val polygon2 = Polygon().also {
            it.coordinates = arrayListOf(
                arrayListOf(
                    LngLatAlt(0.5, 2.0),
                    LngLatAlt(4.0, 2.0),
                    LngLatAlt(4.0, 0.0),
                    LngLatAlt(0.5, 0.0),
                    LngLatAlt(0.5, 0.5),
                    LngLatAlt(3.0, 0.5),
                    LngLatAlt(3.0, 1.5),
                    LngLatAlt(0.5, 1.5),
                    LngLatAlt(0.5, 2.0),
                )
            )
        }

        val feature1 = MvtFeature()
        val feature2 = MvtFeature()
        feature1.geometry = polygon1
        feature2.geometry = polygon2

        val mergedFeature = mergePolygons(feature1, feature2)
        val mergedPolygon = mergedFeature.geometry as Polygon
        // Check we have one outer and one inner ring
        assert(mergedPolygon.coordinates.size == 2)

        // Merge a polygon which covers both of the previous polygons. This isn't like any that we will
        // merge from a tile, but it should get rid of the inner ring.
        val bigPolygon = Polygon().also {
            it.coordinates = arrayListOf(
                arrayListOf(
                    LngLatAlt(0.0, 2.0),
                    LngLatAlt(4.0, 2.0),
                    LngLatAlt(4.0, 0.0),
                    LngLatAlt(0.0, 0.0),
                    LngLatAlt(0.0, 2.0),
                )
            )
        }
        val bigFeature = Feature()
        bigFeature.geometry = bigPolygon

        val secondMergedFeature = mergePolygons(mergedFeature, bigFeature)
        val secondMergedPolygon = secondMergedFeature.geometry as Polygon
        // Check we have just one outer ring
        assert(secondMergedPolygon.coordinates.size == 1)
    }

    @Test
    fun mergePolygonsTest2() {

        // Divide a square donut into two polygons and then merge them and ensure that the donut
        // remains whole
        val polygon1 = Polygon().also {
            it.coordinates = arrayListOf(
                arrayListOf(
                    LngLatAlt(0.0, 3.0),
                    LngLatAlt(3.0, 3.0),
                    LngLatAlt(3.0, 0.0),
                    LngLatAlt(0.0, 0.0),
                    LngLatAlt(0.0, 3.0),
                )
            )
            it.addInteriorRing(
                arrayListOf(
                    LngLatAlt(1.0, 1.0),
                    LngLatAlt(1.0, 2.0),
                    LngLatAlt(2.0, 2.0),
                    LngLatAlt(2.0, 1.0),
                    LngLatAlt(1.0, 1.0),
                )
            )
        }

        val polygon2 = Polygon().also {
            it.coordinates = arrayListOf(
                arrayListOf(
                    LngLatAlt(2.0, 3.0),
                    LngLatAlt(3.0, 3.0),
                    LngLatAlt(3.0, 0.0),
                    LngLatAlt(2.0, 0.0),
                    LngLatAlt(2.0, 3.0),
                )
            )
        }

        val feature1 = MvtFeature()
        val feature2 = MvtFeature()
        feature1.geometry = polygon1
        feature2.geometry = polygon2

        val mergedFeature = mergePolygons(feature1, feature2)
        val mergedPolygon = mergedFeature.geometry as Polygon
        // Check we have one outer and one inner ring
        assert(mergedPolygon.coordinates.size == 2)
   }
    @Test
    fun cheapRulerWrapTest(){
        var a = LngLatAlt(0.0, 0.0)
        var b = LngLatAlt(0.0, 0.0)
        a.createCheapRuler().distance(b, a)

        a.longitude = -181.0
        b.longitude = 181.0
        a.createCheapRuler().distance(b, a)

        a.longitude = -1810.0
        b.longitude = 1810.0
        a.createCheapRuler().distance(b, a)

        a.longitude = 181.0
        b.longitude = -181.0
        a.createCheapRuler().distance(b, a)
    }
}