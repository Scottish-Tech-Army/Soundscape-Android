package com.kersnazzle.soundscapealpha

import com.kersnazzle.soundscapealpha.utils.distance
import com.kersnazzle.soundscapealpha.utils.getPixelXY
import com.kersnazzle.soundscapealpha.utils.getQuadKey
import com.kersnazzle.soundscapealpha.utils.getXYTile
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

}