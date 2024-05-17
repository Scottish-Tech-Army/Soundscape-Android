package com.kersnazzle.soundscapealpha

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
}