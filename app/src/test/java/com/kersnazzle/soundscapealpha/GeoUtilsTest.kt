package com.kersnazzle.soundscapealpha

import com.kersnazzle.soundscapealpha.utils.distance
import com.kersnazzle.soundscapealpha.utils.getQuadKey
import com.kersnazzle.soundscapealpha.utils.getXYTile
import com.kersnazzle.soundscapealpha.utils.groundResolution
import com.kersnazzle.soundscapealpha.utils.mapSize
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
}