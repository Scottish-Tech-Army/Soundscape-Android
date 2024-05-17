package com.kersnazzle.soundscapealpha

import com.kersnazzle.soundscapealpha.utils.distance
import com.kersnazzle.soundscapealpha.utils.getXYTile
import org.junit.Assert
import org.junit.Test

class GeoUtilsTest {

    @Test
    fun getXYTileTest() {

        val testSlippyMapTileName = getXYTile(0.5, 0.5, 16)
        Assert.assertEquals(32859, testSlippyMapTileName.first)
        Assert.assertEquals(32676, testSlippyMapTileName.second)
    }

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
}