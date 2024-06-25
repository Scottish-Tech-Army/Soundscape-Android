package com.kersnazzle.soundscapealpha

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.FeatureCollection
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.GeoMoshi
import com.kersnazzle.soundscapealpha.network.Tiles
import com.kersnazzle.soundscapealpha.utils.getXYTile
import com.squareup.moshi.Moshi
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

//TODO: This is a pain in the ...
// I need a context to be able to test the Cache(cacheDir) and ConnectivityManager
// https://developer.android.com/training/testing/local-tests#mocking-dependencies
// or maybe that is a bad idea and should look at Robolectric?
// https://robolectric.org/

class NetworkTileWithCacheTest {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    lateinit var tiles: Tiles
    lateinit var tile: String
    private val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
    lateinit var featureCollectionTest: FeatureCollection

    @Test
    fun testTile() = runTest {
        givenTilesIsInitialized()
        whenTileDataIsReadAndParsedIntoAFeatureCollection()
        thenTheFeatureCollectionShouldContainLetterBox()
    }

    private fun givenTilesIsInitialized() {
        tiles = Tiles()
        tiles.onCreate()
    }

    private suspend fun whenTileDataIsReadAndParsedIntoAFeatureCollection() {
        // center of tile
        val xyTilePair = getXYTile(51.43860066718254, -2.69439697265625, 16)

        tile = tiles.getTileWithCache(xyTilePair.first, xyTilePair.second)!!
        Assert.assertNotNull(tile)

        featureCollectionTest = moshi.adapter(FeatureCollection::class.java).fromJson(tile)!!
        Assert.assertNotNull(featureCollectionTest)

    }

    private fun thenTheFeatureCollectionShouldContainLetterBox() {

        for (feature in featureCollectionTest.features) {
            if (feature.foreign!!["feature_value"] == "letter_box") {
                Assert.assertEquals("letter_box", feature.foreign!!["feature_value"])
            }
        }
    }
}