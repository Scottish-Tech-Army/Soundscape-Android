package org.scottishtecharmy.soundscape

import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeoMoshi
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon
import org.scottishtecharmy.soundscape.geoengine.utils.getPoiFeatureCollectionBySuperCategory
import org.scottishtecharmy.soundscape.geoengine.utils.getTilesForRegion
import org.scottishtecharmy.soundscape.geoengine.utils.getXYTile
import org.scottishtecharmy.soundscape.geoengine.utils.polygonContainsCoordinates
import com.squareup.moshi.Moshi
import org.junit.Assert
import org.junit.Test
import org.scottishtecharmy.soundscape.geoengine.GRID_SIZE
import org.scottishtecharmy.soundscape.geoengine.MAX_ZOOM_LEVEL
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.utils.RelativeDirections
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geoengine.utils.TileGrid.Companion.getTileGrid
import org.scottishtecharmy.soundscape.geoengine.utils.Triangle
import org.scottishtecharmy.soundscape.geoengine.utils.createPolygonFromTriangle
import org.scottishtecharmy.soundscape.geoengine.utils.explodeLineString
import org.scottishtecharmy.soundscape.geoengine.utils.explodePolygon
import org.scottishtecharmy.soundscape.geoengine.utils.getDistanceToFeature
import org.scottishtecharmy.soundscape.geoengine.utils.getDistanceToFeatureCollection
import org.scottishtecharmy.soundscape.geoengine.utils.getFovTriangle
import org.scottishtecharmy.soundscape.geoengine.utils.getGpsFromNormalizedMapCoordinates
import org.scottishtecharmy.soundscape.geoengine.utils.getIntersectionRoadNames
import org.scottishtecharmy.soundscape.geoengine.utils.getNormalizedFromGpsMapCoordinates
import org.scottishtecharmy.soundscape.geoengine.utils.getRelativeDirectionsPolygons
import org.scottishtecharmy.soundscape.geoengine.utils.getSuperCategoryElements
import org.scottishtecharmy.soundscape.geoengine.utils.removeDuplicateOsmIds
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.geoengine.utils.sortedByDistanceTo

class TileUtilsTest {
    private val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()

    @Test
    fun getXYTileTest() {
        val testSlippyMapTileName = getXYTile(LngLatAlt(0.5, 0.5), 16)
        Assert.assertEquals(32859, testSlippyMapTileName.first)
        Assert.assertEquals(32676, testSlippyMapTileName.second)
    }

    @Test
    fun getRoadsFeatureCollectionFromTileFeatureCollectionTest() {
        val gridState = getGridStateForLocation(sixtyAcresCloseTestLocation, MAX_ZOOM_LEVEL, 1)
        val testRoadsCollectionFromTileFeatureCollection =
            gridState.getFeatureCollection(TreeId.ROADS)
        for (feature in testRoadsCollectionFromTileFeatureCollection) {
            Assert.assertEquals("highway", feature.foreign!!["feature_type"])
        }
        Assert.assertEquals(if(MAX_ZOOM_LEVEL == 15) 38 else 135, testRoadsCollectionFromTileFeatureCollection.features.size)
    }

    @Test
    fun getBusStopsFeatureCollectionFromTileFeatureCollectionTest() {
        val gridState = getGridStateForLocation(longAshtonRoadTestLocation, MAX_ZOOM_LEVEL, 1)
        val testBusStopFeatureCollectionFromTileFeatureCollection =
            gridState.getFeatureCollection(TreeId.TRANSIT_STOPS)

        for (feature in testBusStopFeatureCollectionFromTileFeatureCollection) {
            Assert.assertEquals("bus_stop", feature.foreign!!["feature_value"])
        }
        Assert.assertEquals(if(MAX_ZOOM_LEVEL == 15) 4 else 8, testBusStopFeatureCollectionFromTileFeatureCollection.features.size)
    }

    @Test
    fun getCrossingsFeatureCollectionFromTileFeatureCollectionTest() {
        val gridState = getGridStateForLocation(centralManchesterTestLocation, MAX_ZOOM_LEVEL, 1)
        val testCrossingsFeatureCollection = gridState.getFeatureCollection(TreeId.CROSSINGS)
        for (feature in testCrossingsFeatureCollection) {
            Assert.assertEquals("crossing", feature.foreign!!["feature_value"])
        }
        Assert.assertEquals(if(MAX_ZOOM_LEVEL == 15) 103 else 300, testCrossingsFeatureCollection.features.size)
    }

    @Test
    fun getPathsFeatureCollectionFromTileFeatureCollectionTest() {
        val gridState = getGridStateForLocation(centralManchesterTestLocation, MAX_ZOOM_LEVEL, 1)
        val testPathsCollectionFromTileFeatureCollection =
            gridState.getFeatureCollection(TreeId.ROADS_AND_PATHS)
        val testRoadsCollectionFromTileFeatureCollection =
            gridState.getFeatureCollection(TreeId.ROADS)
        for (feature in testPathsCollectionFromTileFeatureCollection) {
            Assert.assertEquals("highway", feature.foreign!!["feature_type"])
        }
        // Check that the number of path segments (road_and_paths - roads) is correct
        Assert.assertEquals(
            if(MAX_ZOOM_LEVEL == 15) 1387 else 4719,
            testPathsCollectionFromTileFeatureCollection.features.size - testRoadsCollectionFromTileFeatureCollection.features.size
        )
    }

    @Test
    fun getIntersectionsFeatureCollectionFromTileFeatureCollectionTest() {
        val gridState = getGridStateForLocation(centralManchesterTestLocation, MAX_ZOOM_LEVEL, 1)
        val testIntersectionsCollectionFromTileFeatureCollection =
            gridState.getFeatureCollection(TreeId.INTERSECTIONS)
        for (feature in testIntersectionsCollectionFromTileFeatureCollection) {
            Assert.assertEquals("gd_intersection", feature.foreign!!["feature_value"])
        }
        Assert.assertEquals(if(MAX_ZOOM_LEVEL == 15) 1525 else 5524, testIntersectionsCollectionFromTileFeatureCollection.features.size)
    }

    @Test
    fun getEntrancesFeatureCollectionFromTileFeatureCollectionTest() {
        val gridState = getGridStateForLocation(centralManchesterTestLocation, MAX_ZOOM_LEVEL, 1)
        val testEntrancesCollectionFromTileFeatureCollection =
            gridState.getFeatureCollection(TreeId.ENTRANCES)
        for (feature in testEntrancesCollectionFromTileFeatureCollection) {
            Assert.assertEquals("entrance", feature.foreign!!["feature_type"])
        }
        Assert.assertEquals(if(MAX_ZOOM_LEVEL == 15) 38 else 363, testEntrancesCollectionFromTileFeatureCollection.features.size)
    }

    @Test
    fun getPoiFeatureCollectionFromTileFeatureCollectionTest() {
        val gridState = getGridStateForLocation(centralManchesterTestLocation, MAX_ZOOM_LEVEL, 1)
        val testPoiCollection = gridState.getFeatureCollection(TreeId.POIS)

        Assert.assertEquals(if(MAX_ZOOM_LEVEL == 15) 1106 else 3303, testPoiCollection.features.size)
    }

    @Test
    fun getPoiFeatureCollectionBySuperCategoryMobilityTest() {
        val gridState = getGridStateForLocation(centralManchesterTestLocation, MAX_ZOOM_LEVEL, 1)
        val testPoiCollection = gridState.getFeatureCollection(TreeId.POIS)

        // select "mobility" super category
        val testSuperCategoryPoiCollection =
            getPoiFeatureCollectionBySuperCategory("mobility", testPoiCollection)
        Assert.assertEquals(if(MAX_ZOOM_LEVEL == 15) 275 else 672, testSuperCategoryPoiCollection.features.size)
    }

    @Test
    fun getPoiFeatureCollectionBySuperCategoryObjectTest() {
        val gridState = getGridStateForLocation(centralManchesterTestLocation, MAX_ZOOM_LEVEL, 1)
        val testPoiCollection = gridState.getFeatureCollection(TreeId.POIS)

        // select "object" super category
        val testSuperCategoryPoiCollection =
            getPoiFeatureCollectionBySuperCategory("object", testPoiCollection)
        Assert.assertEquals(if(MAX_ZOOM_LEVEL == 15) 83 else 198, testSuperCategoryPoiCollection.features.size)

        for(feature in testSuperCategoryPoiCollection)
            println("${feature.foreign?.get("feature_type")} - ${feature.foreign?.get("feature_value")}")
    }

    @Test
    fun getPoiFeatureCollectionBySuperCategoryInformationTest() {
        val gridState = getGridStateForLocation(centralManchesterTestLocation, MAX_ZOOM_LEVEL, 1)
        val testPoiCollection = gridState.getFeatureCollection(TreeId.POIS)

        // select "information" super category
        val testSuperCategoryPoiCollection =
            getPoiFeatureCollectionBySuperCategory("information", testPoiCollection)
        Assert.assertEquals(if(MAX_ZOOM_LEVEL == 15) 4 else 7, testSuperCategoryPoiCollection.features.size)

    }

    @Test
    fun getPoiFeatureCollectionBySuperCategoryPlaceTest() {
        val gridState = getGridStateForLocation(centralManchesterTestLocation, MAX_ZOOM_LEVEL, 1)
        val testPoiCollection = gridState.getFeatureCollection(TreeId.POIS)

        // select "place" super category
        val testSuperCategoryPoiCollection =
            getPoiFeatureCollectionBySuperCategory("place", testPoiCollection)
        Assert.assertEquals(if(MAX_ZOOM_LEVEL == 15) 451 else 1270, testSuperCategoryPoiCollection.features.size)
    }

    @Test
    fun getPoiFeatureCollectionBySuperCategoryLandmarkTest() {
        val gridState = getGridStateForLocation(centralManchesterTestLocation, MAX_ZOOM_LEVEL, 1)
        val testPoiCollection = gridState.getFeatureCollection(TreeId.POIS)

        // select "landmark" super category
        val testSuperCategoryPoiCollection =
            getPoiFeatureCollectionBySuperCategory("landmark", testPoiCollection)
        Assert.assertEquals(if(MAX_ZOOM_LEVEL == 15) 68 else 189, testSuperCategoryPoiCollection.features.size)
    }

    @Test
    fun getPoiFeatureCollectionBySuperCategorySafetyTest() {
        val gridState = getGridStateForLocation(centralManchesterTestLocation, MAX_ZOOM_LEVEL, 1)
        val testPoiCollection = gridState.getFeatureCollection(TreeId.POIS)

        // select "safety" super category
        val testSuperCategoryPoiCollection =
            getPoiFeatureCollectionBySuperCategory("safety", testPoiCollection)
        Assert.assertEquals(if(MAX_ZOOM_LEVEL == 15) 51 else 258, testSuperCategoryPoiCollection.features.size)
    }

    @Test
    fun getDistanceToFeatureCollectionTest() {
        val featureCollectionTest = moshi.adapter(FeatureCollection::class.java)
            .fromJson(GeoJsonData.FEATURE_COLLECTION_JSON)

        val currentLat = 0.0
        val currentLon = 0.5
        val distanceToFeatureCollection = getDistanceToFeatureCollection(
            LngLatAlt(currentLon, currentLat),
            featureCollectionTest!!
        )

        // This is the distance from the current location to a Point of longitude(0.5) and latitude(0.5)
        Assert.assertEquals(
            55659.75,
            distanceToFeatureCollection.features[0].foreign?.get("distance_to") as Double,
            500.0 // CheapRuler is very inaccurate at these distances
        )
        // Current location is on the boundary of the Polygon so distance should be 0.0
        Assert.assertEquals(
            0.0,
            distanceToFeatureCollection.features[1].foreign?.get("distance_to")
        )

    }

    @Test
    fun getWhatsAroundMeTest() {
        val gridState = getGridStateForLocation(sixtyAcresCloseTestLocation, MAX_ZOOM_LEVEL, GRID_SIZE)
        val poiCollection = gridState.getFeatureCollection(TreeId.POIS)

        val currentLat = 51.43931965688239
        val currentLon = -2.6928249366694956
        // test flags equivalent to Settings
        val placesAndLandmarks = true
        val mobility = true

        val settingsFeatureCollection = FeatureCollection()
        if (placesAndLandmarks) {
            if (mobility) {
                val placeSuperCategory =
                    getPoiFeatureCollectionBySuperCategory("place", poiCollection)

                val tempFeatureCollection = FeatureCollection()
                for (feature in placeSuperCategory.features) {
                    if (feature.foreign?.get("feature_value") != "house") {
                        if (feature.properties?.get("name") != null) {
                            val superCategorySet = getSuperCategoryElements("place")
                            for (property in feature.properties!!) {
                                if (superCategorySet.contains(property.value)) {
                                    tempFeatureCollection.features.add(feature)
                                }
                            }
                        }
                    }
                }
                val cleanedPlaceSuperCategory = removeDuplicateOsmIds(tempFeatureCollection)
                for (feature in cleanedPlaceSuperCategory.features) {
                    settingsFeatureCollection.features.add(feature)
                }

                //val cleanedPlaceString = moshi.adapter(FeatureCollection::class.java).toJson(cleanedPlaceSuperCategory)
                //println(cleanedPlaceString)

                val landmarkSuperCategory =
                    getPoiFeatureCollectionBySuperCategory("landmark", poiCollection)
                for (feature in landmarkSuperCategory.features) {
                    settingsFeatureCollection.features.add(feature)
                }
                val mobilitySuperCategory =
                    getPoiFeatureCollectionBySuperCategory("mobility", poiCollection)
                for (feature in mobilitySuperCategory.features) {
                    settingsFeatureCollection.features.add(feature)
                }
                val settingsString =
                    moshi.adapter(FeatureCollection::class.java).toJson(settingsFeatureCollection)
                println(settingsString)

                val distanceToFeatureCollection = getDistanceToFeatureCollection(
                    LngLatAlt(currentLon, currentLat),
                    settingsFeatureCollection
                ).sortedBy { feature ->
                    feature.foreign?.get("distance_to") as? Double ?: Double.MAX_VALUE
                }
                for (feature in distanceToFeatureCollection) {
                    if (feature.properties?.get("name") != null) {
                        println(
                            "Feature: ${feature.properties?.get("name")} " +
                                    "distance to feature: ${feature.foreign?.get("distance_to")}"
                        )
                    }
                }

            } else {
                //Log.d(TAG, "placesAndLandmarks is true and mobility is false")
                // if I use the placeSuperCategory it correctly detects that I am in my house
                // and returns that as the nearest POI which isn't what original Soundscape does
                // so I need to throw away houses
                val placeSuperCategory =
                    getPoiFeatureCollectionBySuperCategory("place", poiCollection)
                for (feature in placeSuperCategory.features) {
                    if (feature.foreign?.get("feature_type") != "building" && feature.foreign?.get("feature_value") != "house") {
                        settingsFeatureCollection.features.add(feature)
                    }
                }
                val landmarkSuperCategory =
                    getPoiFeatureCollectionBySuperCategory("landmark", poiCollection)
                for (feature in landmarkSuperCategory.features) {
                    settingsFeatureCollection.features.add(feature)
                }
            }
        } else {
            if (mobility) {
                //Log.d(TAG, "placesAndLandmarks is false and mobility is true")
                val mobilitySuperCategory =
                    getPoiFeatureCollectionBySuperCategory("mobility", poiCollection)
                for (feature in mobilitySuperCategory.features) {
                    settingsFeatureCollection.features.add(feature)
                }
            } else {
                // Not sure what we are supposed to tell the user here?
                println("placesAndLandmarks and mobility are both false so what should I tell the user?")
            }
        }
    }

    @Test
    fun getIntersectionInFovTest() {
        // Fake device location and pretend the device is pointing East.
        val userGeometry = UserGeometry(
            longAshtonRoadTestLocation,
            90.0,
            50.0
        )

        val gridState = getGridStateForLocation(longAshtonRoadTestLocation, MAX_ZOOM_LEVEL, 1)
        val intersectionTree = gridState.getFeatureTree(TreeId.INTERSECTIONS)

        // Create a FOV triangle to pick up the intersection (this intersection is a transition from
        // Weston Road to Long Ashton Road)
        val triangle = getFovTriangle(userGeometry)
        val fovIntersectionsFeatureCollection =
            intersectionTree.getAllWithinTriangle(triangle)

        // Should only be one intersection in this FoV
        Assert.assertEquals(1, fovIntersectionsFeatureCollection.features.size)
    }

    @Test
    fun getRoadsInFovTest() {
        // Fake device location and pretend the device is pointing East.
        val userGeometry = UserGeometry(
            longAshtonRoadTestLocation,
            90.0,
            50.0
        )

        val gridState = getGridStateForLocation(longAshtonRoadTestLocation, MAX_ZOOM_LEVEL, 1)
        val roadsTree = gridState.getFeatureTree(TreeId.ROADS)

        // Create a FOV triangle to pick up the roads in the FoV roads.
        // In this case Weston Road and Long Ashton Road
        val triangle = getFovTriangle(userGeometry)
        val fovRoadsFeatureCollection =
            roadsTree.getAllWithinTriangle(triangle)

        // Should contain two roads - Weston Road and Long Ashton Road
        Assert.assertEquals(2, fovRoadsFeatureCollection.features.size)

    }

    @Test
    fun getPoiInFovTest() {
        // Fake device location and pretend the device is pointing East.
        val userGeometry = UserGeometry(
            LngLatAlt(-2.653228, 51.431658),
            90.0,
            200.0
        )

        val gridState = getGridStateForLocation(longAshtonRoadTestLocation, MAX_ZOOM_LEVEL, 1)
        val poiTree = gridState.getFeatureTree(TreeId.POIS)

        // Create a FOV triangle to pick up the Points of interest in the FoV
        val triangle = getFovTriangle(userGeometry)
        val fovPoiFeatureCollection =
            poiTree.getAllWithinTriangle(triangle)

        // Should contain 5 POI:
        // - Car park
        // - Social facility
        // - Bus stop
        // - Memorial
        // - Crossing
        Assert.assertEquals(6, fovPoiFeatureCollection.features.size)
    }

    @Test
    fun getNearestIntersectionTest() {
        // Fake device location and pretend the device is pointing East.
        // I've moved the device location so the FoV picks up a couple of intersections
        val userGeometry = UserGeometry(
            LngLatAlt(-2.657279900280031, 51.430461188129385),
            90.0,
            50.0
        )

        val gridState = getGridStateForLocation(longAshtonRoadTestLocation, MAX_ZOOM_LEVEL, 1)
        val intersectionTree = gridState.getFeatureTree(TreeId.INTERSECTIONS)

        // Create a FOV triangle to pick up the intersections
        val triangle = getFovTriangle(userGeometry)
        val nearestIntersection =
            intersectionTree.getNearestFeatureWithinTriangle(triangle, userGeometry.ruler)

        // Should only be the nearest intersection in this Feature Collection
        assert(nearestIntersection != null)
    }

    @Test
    fun distanceToPolygon() {
        val userGeometry = UserGeometry(
            LngLatAlt(-2.657279900280031, 51.430461188129385),
            90.0,
            50.0
        )
        val expectedNearestPoint = LngLatAlt(-2.6504250, 51.4304580)
        val ruler = CheapRuler(userGeometry.location.latitude)
        val gridState = getGridStateForLocation(longAshtonRoadTestLocation, MAX_ZOOM_LEVEL, 1)
        val poiTree = gridState.getFeatureTree(TreeId.POIS)
        val fc = poiTree.getAllCollection()
        for(feature in fc) {
            if(feature.geometry.type == "Polygon") {
                println("${feature.properties?.get("name")}")
                val nearestPoint = getDistanceToFeature(userGeometry.location, feature, ruler).point
                val offset = ruler.distance(nearestPoint, expectedNearestPoint)
                assert(offset < 1.0)
                break
            }
        }
    }

    @Test
    fun sortedByDistanceToTest() {
        // Fake device location and pretend the device is pointing East.
        // I've moved the device location so the FoV picks up a couple of intersections
        val userGeometry = UserGeometry(
            LngLatAlt(-2.657279900280031, 51.430461188129385),
            90.0,
            50.0
        )

        val gridState = getGridStateForLocation(longAshtonRoadTestLocation, MAX_ZOOM_LEVEL, 1)
        val intersectionTree = gridState.getFeatureTree(TreeId.INTERSECTIONS)

        // Create a FOV triangle to pick up the intersections
        val triangle = getFovTriangle(userGeometry)
        val fovIntersectionsFeatureCollection =
            intersectionTree.getAllWithinTriangle(triangle)

        Assert.assertEquals(2, fovIntersectionsFeatureCollection.features.size)
        // This should sort the intersections (but any feature collection wil do)
        // by distance to the current location
        val sortedByDistanceToTest = sortedByDistanceTo(
            userGeometry.location,
            fovIntersectionsFeatureCollection
        )
        Assert.assertEquals(6.24, sortedByDistanceToTest.features[0].foreign?.get("distance_to") as Double, 0.1)
        Assert.assertEquals(36.8, sortedByDistanceToTest.features[1].foreign?.get("distance_to") as Double, 0.1)

    }

    @Test
    fun getNearestRoadTest() {
        // Fake device location and pretend the device is pointing East.
        val userGeometry = UserGeometry(
            LngLatAlt(-2.657279900280031, 51.430461188129385),
            90.0, 50.0
        )

        val gridState = getGridStateForLocation(longAshtonRoadTestLocation, MAX_ZOOM_LEVEL, 1)
        val roadTree = gridState.getFeatureTree(TreeId.ROADS)

        // Create a FOV triangle to pick up the roads
        val triangle = getFovTriangle(userGeometry)
        val fovRoadsFeatureCollection =
            roadTree.getAllWithinTriangle(triangle)

        // This should pick up four road segments in the FoV
        Assert.assertEquals(4, fovRoadsFeatureCollection.features.size)
        val nearestRoad = gridState.getFeatureTree(TreeId.ROADS_AND_PATHS)
            .getNearestFeature(userGeometry.location, userGeometry.ruler)
        // Should only be the nearest road in this Feature Collection
        assert(nearestRoad != null)
        // The nearest road to the current location should be Weston Road
        Assert.assertEquals("Weston Road", nearestRoad!!.properties!!["name"])
    }

    @Test
    fun getNearestPoiTest() {
        // Fake device location and pretend the device is pointing East.
        val userGeometry = UserGeometry(
            longAshtonRoadTestLocation,
            90.0,
            100.0
        )

        val gridState = getGridStateForLocation(longAshtonRoadTestLocation, MAX_ZOOM_LEVEL, 1)
        val poiTree = gridState.getFeatureTree(TreeId.POIS)

        // Create a FOV triangle to pick up the poi
        val triangle = getFovTriangle(userGeometry)
        val nearestPoiFeature =
            poiTree.getNearestFeatureWithinTriangle(triangle, userGeometry.ruler)

        // It's a grit-bin - not the most useful, but the original Soundscape used to find houses
        // which was even less useful. Measure distance to it.
        val distance = getDistanceToFeature(userGeometry.location, nearestPoiFeature!!, userGeometry.ruler)

        Assert.assertEquals(46.82, distance.distance, 0.1) //  CheapRuler is slightly inaccurate at these distances

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

    @Test
    fun get3x3TileGridTest() {
        val gridSize = 3

        var tileGrid = getTileGrid(LngLatAlt(65.0, 0.0), MAX_ZOOM_LEVEL, gridSize)
        Assert.assertEquals(gridSize * gridSize, tileGrid.tiles.size)
        tileGrid = getTileGrid(LngLatAlt(-65.0, 0.0), MAX_ZOOM_LEVEL, gridSize)
        Assert.assertEquals(gridSize * gridSize, tileGrid.tiles.size)
        tileGrid = getTileGrid(LngLatAlt(0.0, 0.0), MAX_ZOOM_LEVEL, gridSize)
        Assert.assertEquals(gridSize * gridSize, tileGrid.tiles.size)
        tileGrid = getTileGrid(LngLatAlt(0.0, 180.0), MAX_ZOOM_LEVEL, gridSize)
        Assert.assertEquals(gridSize * gridSize, tileGrid.tiles.size)
        tileGrid = getTileGrid(LngLatAlt(0.0, -180.0), MAX_ZOOM_LEVEL, gridSize)
        Assert.assertEquals(gridSize * gridSize, tileGrid.tiles.size)
    }

    @Test
    fun get2x2TileGridTest() {
        val gridSize = 2

        var tileGrid = getTileGrid(LngLatAlt(0.001, 0.0), MAX_ZOOM_LEVEL, gridSize)
        Assert.assertEquals(gridSize * gridSize, tileGrid.tiles.size)
        tileGrid = getTileGrid(LngLatAlt(0.0, 0.0), MAX_ZOOM_LEVEL, gridSize)
        Assert.assertEquals(gridSize * gridSize, tileGrid.tiles.size)
    }

    @Test
    fun getRelativeDirectionsTest() {

        val userGeometry = UserGeometry(
            LngLatAlt(-2.657279900280031, 51.430461188129385),
            0.0,
            50.0
        )

        val combinedDirectionPolygons =
            getRelativeDirectionsPolygons(userGeometry, RelativeDirections.COMBINED)

        // Location to test relative directions. Placed in "Ahead" triangle
        val testBeaconAhead = LngLatAlt(-2.6572829456840736, 51.4307659303868)
        for (feature in combinedDirectionPolygons) {
            val iAmHere1 =
                polygonContainsCoordinates(testBeaconAhead, (feature.geometry as Polygon))
            if (iAmHere1) {
                Assert.assertEquals(4, feature.properties!!["Direction"])
            }
        }

        // Location to test relative directions. Placed in "Ahead Right" triangle
        val testBeaconAheadRight = LngLatAlt(-2.656996677668559, 51.43067289460916)
        for (feature in combinedDirectionPolygons) {
            val iAmHere2 =
                polygonContainsCoordinates(testBeaconAheadRight, (feature.geometry as Polygon))
            if (iAmHere2) {
                Assert.assertEquals(5, feature.properties!!["Direction"])
            }
        }

        // Location to test relative directions. Placed in "Right" triangle
        val testBeaconRight = LngLatAlt(-2.656649501563379, 51.430464038091515)
        for (feature in combinedDirectionPolygons) {
            val iAmHere3 =
                polygonContainsCoordinates(testBeaconRight, (feature.geometry as Polygon))
            if (iAmHere3) {
                Assert.assertEquals(6, feature.properties!!["Direction"])
            }
        }

        // Location to test relative directions. Placed in "Behind Right" triangle
        val testBeaconBehindRight = LngLatAlt(-2.6570667219705797, 51.43031404054909)
        for (feature in combinedDirectionPolygons) {
            val iAmHere4 =
                polygonContainsCoordinates(testBeaconBehindRight, (feature.geometry as Polygon))
            if (iAmHere4) {
                Assert.assertEquals(7, feature.properties!!["Direction"])
            }
        }

        // Location to test relative directions. Placed in "Behind" triangle
        val testBeaconBehind = LngLatAlt(-2.6572890364938644, 51.430274167701896)
        for (feature in combinedDirectionPolygons) {
            val iAmHere5 =
                polygonContainsCoordinates(testBeaconBehind, (feature.geometry as Polygon))
            if (iAmHere5) {
                Assert.assertEquals(0, feature.properties!!["Direction"])
            }
        }

        // Location to test relative directions. Placed in "Behind Left" triangle
        val testBeaconBehindLeft = LngLatAlt(-2.657806755246213, 51.430285559947464)
        for (feature in combinedDirectionPolygons) {
            val iAmHere6 =
                polygonContainsCoordinates(testBeaconBehindLeft, (feature.geometry as Polygon))
            if (iAmHere6) {
                Assert.assertEquals(2, feature.properties!!["Direction"])
            }
        }
        // Location to test relative directions. Placed in "Left" triangle
        val testBeaconLeft = LngLatAlt(-2.6579194352108857, 51.43053239123893)
        for (feature in combinedDirectionPolygons) {
            val iAmHere7 = polygonContainsCoordinates(testBeaconLeft, (feature.geometry as Polygon))
            if (iAmHere7) {
                Assert.assertEquals(2, feature.properties!!["Direction"])
            }
        }
        // Location to test relative directions. Placed in "Ahead Left" triangle
        val testBeaconAheadLeft = LngLatAlt(-2.657566168297052, 51.430682388064525)
        for (feature in combinedDirectionPolygons) {
            val iAmHere8 =
                polygonContainsCoordinates(testBeaconAheadLeft, (feature.geometry as Polygon))
            if (iAmHere8) {
                Assert.assertEquals(3, feature.properties!!["Direction"])
            }
        }

    }

    @Test
    fun getIntersectionRoadNamesTest() {
        // Fake device location and pretend the device is pointing East.
        // I've moved the device location so the FoV picks up a couple of intersections
        val userGeometry = UserGeometry(
            LngLatAlt(-2.657279900280031, 51.430461188129385),
            90.0,
            50.0
        )

        val gridState = getGridStateForLocation(longAshtonRoadTestLocation, MAX_ZOOM_LEVEL, 1)
        val roadTree = gridState.getFeatureTree(TreeId.ROADS)
        val intersectionTree = gridState.getFeatureTree(TreeId.INTERSECTIONS)

        // create a FOV triangle to pick up the roads
        val triangle = getFovTriangle(userGeometry)
        val fovRoadsFeatureCollection = roadTree.getAllWithinTriangle(triangle)

        // Create a FOV triangle to pick up the intersections
        val nearestIntersection = intersectionTree.getNearestFeatureWithinTriangle(triangle, userGeometry.ruler)
        assert(nearestIntersection != null)

        // how far away is the intersection?
        val nearestIntersectionPoint = nearestIntersection!!.geometry as Point
        val distanceToNearestIntersection =
            userGeometry.ruler.distance(userGeometry.location, nearestIntersectionPoint.coordinates)
        Assert.assertEquals(6.24, distanceToNearestIntersection, 0.1)

        // get the roads that make up the intersection based on the osm_ids
        val nearestIntersectionRoadNames = getIntersectionRoadNames(
            nearestIntersection,
            fovRoadsFeatureCollection
        )
        Assert.assertEquals(
            "Long Ashton Road",
            nearestIntersectionRoadNames.features[0].properties!!["name"]
        )
        Assert.assertEquals(
            "Weston Road",
            nearestIntersectionRoadNames.features[2].properties!!["name"]
        )
    }

    private fun roundtripGpsLocation(latitude: Double, longitude: Double) {
        val normalized = getNormalizedFromGpsMapCoordinates(latitude, longitude)
        val gps = getGpsFromNormalizedMapCoordinates(normalized.first, normalized.second)
        Assert.assertEquals(gps.first, latitude, 0.0000000001)
        Assert.assertEquals(gps.second, longitude, 0.0000000001)
    }

    @Test
    fun testNormalizingLocation() {
        // Roundtrip some GPS locations
        val latitude = -3.1970584
        val longitude = 55.9412409
        val normalized = getNormalizedFromGpsMapCoordinates(latitude, longitude)
        Assert.assertEquals(normalized.first, 0.6553923358333333, 0.0000000001)
        Assert.assertEquals(normalized.second, 0.5088853297949566, 0.0000000001)
        val gps = getGpsFromNormalizedMapCoordinates(normalized.first, normalized.second)
        Assert.assertEquals(gps.first, latitude, 0.0000001)
        Assert.assertEquals(gps.second, longitude, 0.0000001)

        roundtripGpsLocation(-41.5870134, 162.8204719)      // Christchurch
        roundtripGpsLocation(64.511925, -165.5752794)       // Nome
        roundtripGpsLocation(0.0, -179.0)
        roundtripGpsLocation(0.0, 179.0)
        roundtripGpsLocation(
            85.0,
            -179.0
        )                  // The projection breaks above 85 degrees
        roundtripGpsLocation(-85.0, 179.0)
    }

    @Test
    fun explodeLineStringTest() {
        val featureCollection = FeatureCollection().also {
            it.addFeature(
                Feature().also { feature ->
                    feature.geometry = LineString().also { lineString ->
                        lineString.coordinates = arrayListOf(
                            LngLatAlt(0.0, 0.0),
                            LngLatAlt(1.0, 1.0),
                            LngLatAlt(2.0, 0.0)
                        )
                    }
                }
            )
        }

        val explodedFeatureCollection = explodeLineString(featureCollection)
        Assert.assertEquals(2, explodedFeatureCollection.features.size)
    }

    @Test
    fun explodePolygonTest() {
        // create a test polygon
        val polygonTriangleFOV = createPolygonFromTriangle(
            Triangle(
                LngLatAlt(0.5, 0.0),
                LngLatAlt(0.0, 1.0),
                LngLatAlt(1.0, 1.0)
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

        // add it to a feature collection
        val polygonFeatureCollection = FeatureCollection()
        polygonFeatureCollection.addFeature(
            Feature().also { feature ->
                feature.geometry = polygonTriangleFOV
            }
        )
        // explode the triangle into segments
        val explodedPolygonFeatureCollection = explodePolygon(polygonFeatureCollection)
        // The triangle polygon should be exploded into three segments/linestrings
        Assert.assertEquals(3, explodedPolygonFeatureCollection.features.size)
        // Check the linestrings have the correct coordinates
        val firstLineString = explodedPolygonFeatureCollection.features[0].geometry as LineString
        Assert.assertEquals(0.0, firstLineString.coordinates[0].longitude, 0.1)
        Assert.assertEquals(1.0, firstLineString.coordinates[0].latitude, 0.1)
        val secondLineString = explodedPolygonFeatureCollection.features[1].geometry as LineString
        Assert.assertEquals(0.5, secondLineString.coordinates[0].longitude, 0.1)
        Assert.assertEquals(0.0, secondLineString.coordinates[0].latitude, 0.1)
        val thirdLineString = explodedPolygonFeatureCollection.features[2].geometry as LineString
        Assert.assertEquals(1.0, thirdLineString.coordinates[0].longitude, 0.1)
        Assert.assertEquals(1.0, thirdLineString.coordinates[0].latitude, 0.1)

    }
}