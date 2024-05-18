package com.kersnazzle.soundscapealpha

import com.kersnazzle.soundscapealpha.geojsonparser.geojson.FeatureCollection
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.GeoMoshi
import com.kersnazzle.soundscapealpha.utils.getIntersectionsFeatureCollectionFromTileFeatureCollection
import com.kersnazzle.soundscapealpha.utils.getPathsFeatureCollection
import com.kersnazzle.soundscapealpha.utils.getRoadsFeatureCollectionFromTileFeatureCollection
import com.kersnazzle.soundscapealpha.utils.getXYTile
import com.squareup.moshi.Moshi
import org.junit.Assert
import org.junit.Test

class TileUtilsTest {
    private val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
    @Test
    fun getXYTileTest() {
        val testSlippyMapTileName = getXYTile(0.5, 0.5, 16)
        Assert.assertEquals(32859, testSlippyMapTileName.first)
        Assert.assertEquals(32676, testSlippyMapTileName.second)
    }

    @Test
    fun getRoadsFeatureCollectionFromTileFeatureCollectionTest() {
        val featureCollectionTest = moshi.adapter(FeatureCollection::class.java)
            .fromJson(GeoJsonDataReal.featureCollectionJsonRealSoundscapeGeoJson)
        val testRoadsCollectionFromTileFeatureCollection =
            getRoadsFeatureCollectionFromTileFeatureCollection(featureCollectionTest!!)
        for (feature in testRoadsCollectionFromTileFeatureCollection) {
            Assert.assertEquals("highway", feature.foreign!!["feature_type"])
        }
        Assert.assertEquals(16, testRoadsCollectionFromTileFeatureCollection.features.size)
    }

    @Test
    fun getPathsFeatureCollectionFromTileFeatureCollectionTest() {
        // This is the tile from /16/32295/21787.json as it contains footway, cycleway
        val featureCollectionTest = moshi.adapter(FeatureCollection::class.java)
            .fromJson(GeoJsonEntrancesEtcData.featureCollectionWithEntrances)
        val testPathsCollectionFromTileFeatureCollection =
            getPathsFeatureCollection(featureCollectionTest!!)
        for (feature in testPathsCollectionFromTileFeatureCollection){
            Assert.assertEquals("highway", feature.foreign!!["feature_type"])
        }
        Assert.assertEquals(54, testPathsCollectionFromTileFeatureCollection.features.size)
    }

    @Test
    fun getIntersectionsFeatureCollectionFromTileFeatureCollectionTest() {
        val featureCollectionTest = moshi.adapter(FeatureCollection::class.java)
            .fromJson(GeoJsonDataReal.featureCollectionJsonRealSoundscapeGeoJson)
        val testIntersectionsCollectionFromTileFeatureCollection =
            getIntersectionsFeatureCollectionFromTileFeatureCollection(
                featureCollectionTest!!
            )
        for (feature in testIntersectionsCollectionFromTileFeatureCollection) {
            Assert.assertEquals("gd_intersection", feature.foreign!!["feature_value"])
        }
        Assert.assertEquals(10, testIntersectionsCollectionFromTileFeatureCollection.features.size)

    }
}