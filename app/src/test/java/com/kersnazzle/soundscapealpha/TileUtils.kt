package com.kersnazzle.soundscapealpha

import com.kersnazzle.soundscapealpha.geojsonparser.geojson.FeatureCollection
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.GeoMoshi
import com.kersnazzle.soundscapealpha.utils.getEntrancesFeatureCollectionFromTileFeatureCollection
import com.kersnazzle.soundscapealpha.utils.getIntersectionsFeatureCollectionFromTileFeatureCollection
import com.kersnazzle.soundscapealpha.utils.getPathsFeatureCollection
import com.kersnazzle.soundscapealpha.utils.getPoiFeatureCollectionBySuperCategory
import com.kersnazzle.soundscapealpha.utils.getPointsOfInterestFeatureCollectionFromTileFeatureCollection
import com.kersnazzle.soundscapealpha.utils.getRoadsFeatureCollectionFromTileFeatureCollection
import com.kersnazzle.soundscapealpha.utils.getTilesForRegion
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

    @Test
    fun getEntrancesFeatureCollectionFromTileFeatureCollectionTest() {
        val featureCollectionTest = moshi.adapter(FeatureCollection::class.java)
            .fromJson(GeoJsonEntrancesEtcData.featureCollectionWithEntrances)
        val testEntrancesCollectionFromTileFeatureCollection =
            getEntrancesFeatureCollectionFromTileFeatureCollection(featureCollectionTest!!)
        for (feature in testEntrancesCollectionFromTileFeatureCollection) {
            Assert.assertEquals("gd_entrance_list", feature.foreign!!["feature_type"])
        }
        Assert.assertEquals(1, testEntrancesCollectionFromTileFeatureCollection.features.size)

    }

    @Test
    fun getPoiFeatureCollectionFromTileFeatureCollectionTest() {
        val featureCollectionTest = moshi.adapter(FeatureCollection::class.java)
            .fromJson(GeoJsonDataReal.featureCollectionJsonRealSoundscapeGeoJson)
        val testPoiCollection =
            getPointsOfInterestFeatureCollectionFromTileFeatureCollection(
                featureCollectionTest!!
            )
        //There are 16 roads, 10 intersections, 0 entrances and 149 Features in total
        // so there should be 123 POI Features in the POI Feature Collection
        Assert.assertEquals(123, testPoiCollection.features.size)

    }

    @Test
    fun getPoiFeatureCollectionBySuperCategoryMobilityTest() {
        // This is the tile from /16/32295/21787.json as it contains a variety of shops, entrances, etc
        val featureCollectionTest = moshi.adapter(FeatureCollection::class.java)
            .fromJson(GeoJsonEntrancesEtcData.featureCollectionWithEntrances)
        val testPoiCollection =
            getPointsOfInterestFeatureCollectionFromTileFeatureCollection(
                featureCollectionTest!!
            )
        // select "mobility" super category
        val testSuperCategoryPoiCollection =
            getPoiFeatureCollectionBySuperCategory("mobility", testPoiCollection)
        Assert.assertEquals(15, testSuperCategoryPoiCollection.features.size)

    }

    @Test
    fun getPoiFeatureCollectionBySuperCategoryObjectTest() {
        // This is the tile from /16/32295/21787.json as it contains a variety of shops, entrances, etc
        val featureCollectionTest = moshi.adapter(FeatureCollection::class.java)
            .fromJson(GeoJsonEntrancesEtcData.featureCollectionWithEntrances)
        val testPoiCollection =
            getPointsOfInterestFeatureCollectionFromTileFeatureCollection(
                featureCollectionTest!!
            )
        // select "object" super category
        val testSuperCategoryPoiCollection =
            getPoiFeatureCollectionBySuperCategory("object", testPoiCollection)
        Assert.assertEquals(28, testSuperCategoryPoiCollection.features.size)

    }

    @Test
    fun getPoiFeatureCollectionBySuperCategoryInformationTest() {
        // This is the tile from /16/32295/21787.json as it contains a variety of shops, entrances, etc
        val featureCollectionTest = moshi.adapter(FeatureCollection::class.java)
            .fromJson(GeoJsonEntrancesEtcData.featureCollectionWithEntrances)
        val testPoiCollection =
            getPointsOfInterestFeatureCollectionFromTileFeatureCollection(
                featureCollectionTest!!
            )
        // select "information" super category
        val testSuperCategoryPoiCollection =
            getPoiFeatureCollectionBySuperCategory("information", testPoiCollection)
        Assert.assertEquals(1, testSuperCategoryPoiCollection.features.size)

    }

    @Test
    fun getPoiFeatureCollectionBySuperCategoryPlaceTest() {
        // This is the tile from /16/32295/21787.json as it contains a variety of shops, entrances, etc
        val featureCollectionTest = moshi.adapter(FeatureCollection::class.java)
            .fromJson(GeoJsonEntrancesEtcData.featureCollectionWithEntrances)
        val testPoiCollection =
            getPointsOfInterestFeatureCollectionFromTileFeatureCollection(
                featureCollectionTest!!
            )
        // select "place" super category
        val testSuperCategoryPoiCollection =
            getPoiFeatureCollectionBySuperCategory("place", testPoiCollection)
        Assert.assertEquals(239, testSuperCategoryPoiCollection.features.size)

    }

    @Test
    fun getPoiFeatureCollectionBySuperCategoryLandmarkTest() {
        // This is the tile from /16/32295/21787.json as it contains a variety of shops, entrances, etc
        val featureCollectionTest = moshi.adapter(FeatureCollection::class.java)
            .fromJson(GeoJsonEntrancesEtcData.featureCollectionWithEntrances)
        val testPoiCollection =
            getPointsOfInterestFeatureCollectionFromTileFeatureCollection(
                featureCollectionTest!!
            )
        // select "landmark" super category
        val testSuperCategoryPoiCollection =
            getPoiFeatureCollectionBySuperCategory("landmark", testPoiCollection)
        Assert.assertEquals(16, testSuperCategoryPoiCollection.features.size)

    }

    @Test
    fun getPoiFeatureCollectionBySuperCategorySafetyTest() {
        // This is the tile from /16/32295/21787.json as it contains a variety of shops, entrances, etc
        val featureCollectionTest = moshi.adapter(FeatureCollection::class.java)
            .fromJson(GeoJsonEntrancesEtcData.featureCollectionWithEntrances)
        val testPoiCollection =
            getPointsOfInterestFeatureCollectionFromTileFeatureCollection(
                featureCollectionTest!!
            )
        // select "safety" super category
        val testSuperCategoryPoiCollection =
            getPoiFeatureCollectionBySuperCategory("safety", testPoiCollection)
        Assert.assertEquals(11, testSuperCategoryPoiCollection.features.size)

    }

    @Test
    fun getTilesForRadius100Test() {
        // The Lat/Lon is the center of the tile with 100m radius so should only return 1 tile
        val testGetTilesForRadius =
            getTilesForRegion(51.43860066718254, -2.69439697265625, 100.0, 16)
        Assert.assertEquals(1, testGetTilesForRadius.size)
    }

    @Test
    fun getTilesForRadius200Test() {
        // The Lat/Lon is the center of the tile with 200m radius so should return 9 tiles
        val testGetTilesForRadius =
            getTilesForRegion(51.43860066718254, -2.69439697265625, 200.0, 16)
        Assert.assertEquals(9, testGetTilesForRadius.size)
    }

    @Test
    fun getTilesForRadius500Test() {
        // The Lat/Lon is the center of the tile with 500m radius so should return 25 tiles
        val testGetTilesForRadius =
            getTilesForRegion(51.43860066718254, -2.69439697265625, 500.0, 16)
        Assert.assertEquals(25, testGetTilesForRadius.size)
    }

}