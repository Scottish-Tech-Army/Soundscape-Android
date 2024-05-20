package com.kersnazzle.soundscapealpha

import com.kersnazzle.soundscapealpha.geojsonparser.geojson.FeatureCollection
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.GeoMoshi
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.LngLatAlt
import com.kersnazzle.soundscapealpha.utils.getEntrancesFeatureCollectionFromTileFeatureCollection
import com.kersnazzle.soundscapealpha.utils.getFovIntersectionFeatureCollection
import com.kersnazzle.soundscapealpha.utils.getFovPoiFeatureCollection
import com.kersnazzle.soundscapealpha.utils.getFovRoadsFeatureCollection
import com.kersnazzle.soundscapealpha.utils.getIntersectionsFeatureCollectionFromTileFeatureCollection
import com.kersnazzle.soundscapealpha.utils.getNearestIntersection
import com.kersnazzle.soundscapealpha.utils.getPathsFeatureCollectionFromTileFeatureCollection
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
            getPathsFeatureCollectionFromTileFeatureCollection(featureCollectionTest!!)
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
    fun getIntersectionInFovTest(){
        // Fake device location and pretend the device is pointing East.
        val currentLocation = LngLatAlt(-2.6573400576040456, 51.430456817236575)
        val deviceHeading = 90.0
        val fovDistance = 50.0

        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val featureCollectionTest = moshi.adapter(FeatureCollection::class.java)
            .fromJson(GeoJsonIntersectionStraight.intersectionStraightAheadFeatureCollection)
        // Get the intersections from the tile
        val testIntersectionsCollectionFromTileFeatureCollection =
            getIntersectionsFeatureCollectionFromTileFeatureCollection(
                featureCollectionTest!!
            )
        // Create a FOV triangle to pick up the intersection (this intersection is a transition from
        // Weston Road to Long Ashton Road)
        val fovIntersectionsFeatureCollection = getFovIntersectionFeatureCollection(
            currentLocation,
            deviceHeading,
            fovDistance,
            testIntersectionsCollectionFromTileFeatureCollection
        )
        // Should only be one intersection in this FoV
        Assert.assertEquals(1, fovIntersectionsFeatureCollection.features.size)
    }

    @Test
    fun getRoadsInFovTest(){
        // Fake device location and pretend the device is pointing East.
        val currentLocation = LngLatAlt(-2.6573400576040456, 51.430456817236575)
        val deviceHeading = 90.0
        val fovDistance = 50.0

        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val featureCollectionTest = moshi.adapter(FeatureCollection::class.java)
            .fromJson(GeoJsonIntersectionStraight.intersectionStraightAheadFeatureCollection)
        // Get the roads from the tile
        val testRoadsCollectionFromTileFeatureCollection =
            getRoadsFeatureCollectionFromTileFeatureCollection(
                featureCollectionTest!!
            )
        // Create a FOV triangle to pick up the roads in the FoV roads.
        // In this case Weston Road and Long Ashton Road
        val fovRoadsFeatureCollection = getFovRoadsFeatureCollection(
            currentLocation,
            deviceHeading,
            fovDistance,
            testRoadsCollectionFromTileFeatureCollection
        )
        // Should contain two roads - Weston Road and Long Ashton Road
        Assert.assertEquals(2, fovRoadsFeatureCollection.features.size)

    }

    @Test
    fun getPoiInFovTest(){
        // Fake device location and pretend the device is pointing East.
        val currentLocation = LngLatAlt(-2.6573400576040456, 51.430456817236575)
        val deviceHeading = 90.0
        val fovDistance = 50.0

        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val featureCollectionTest = moshi.adapter(FeatureCollection::class.java)
            .fromJson(GeoJsonIntersectionStraight.intersectionStraightAheadFeatureCollection)
        // Get the poi from the tile
        val testPoiCollectionFromTileFeatureCollection =
            getPointsOfInterestFeatureCollectionFromTileFeatureCollection(
                featureCollectionTest!!
            )
        // Create a FOV triangle to pick up the Points of interest in the FoV
        val fovPoiFeatureCollection = getFovPoiFeatureCollection(
            currentLocation,
            deviceHeading,
            fovDistance,
            testPoiCollectionFromTileFeatureCollection
        )
        // Should contain two buildings
        // unfortunately I seem to have chosen the two dullest buildings for my FoV as
        // there doesn't appear to be any properties other than they are buildings
        Assert.assertEquals(2, fovPoiFeatureCollection.features.size)

    }

    @Test
    fun getNearestIntersectionTest(){
        // Fake device location and pretend the device is pointing East.
        // I've moved the device location so the FoV picks up a couple of intersections
        val currentLocation = LngLatAlt(-2.657279900280031, 51.430461188129385)
        val deviceHeading = 90.0
        val fovDistance = 50.0

        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val featureCollectionTest = moshi.adapter(FeatureCollection::class.java)
            .fromJson(GeoJsonIntersectionStraight.intersectionStraightAheadFeatureCollection)
        // Get the intersections from the tile
        val testIntersectionsCollectionFromTileFeatureCollection =
            getIntersectionsFeatureCollectionFromTileFeatureCollection(
                featureCollectionTest!!
            )
        // Create a FOV triangle to pick up the intersections
        val fovIntersectionsFeatureCollection = getFovIntersectionFeatureCollection(
            currentLocation,
            deviceHeading,
            fovDistance,
            testIntersectionsCollectionFromTileFeatureCollection
        )
        Assert.assertEquals(2, fovIntersectionsFeatureCollection.features.size)
        val nearestIntersectionFeatureCollection = getNearestIntersection(currentLocation, fovIntersectionsFeatureCollection)
        // Should only be the nearest intersection in this Feature Collection
        Assert.assertEquals(1, nearestIntersectionFeatureCollection.features.size)
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