package org.scottishtecharmy.soundscape

import com.squareup.moshi.Moshi
import org.junit.Assert
import org.junit.Test
import org.scottishtecharmy.soundscape.geoengine.GeoEngine
import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeoMoshi
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.geoengine.utils.RelativeDirections
import org.scottishtecharmy.soundscape.geoengine.utils.createTriangleFOV
import org.scottishtecharmy.soundscape.geoengine.utils.getCrossingsFromTileFeatureCollection
import org.scottishtecharmy.soundscape.geoengine.utils.getFovFeatureCollection
import org.scottishtecharmy.soundscape.geoengine.utils.getFovTrianglePoints
import org.scottishtecharmy.soundscape.geoengine.utils.getIntersectionRoadNames
import org.scottishtecharmy.soundscape.geoengine.utils.getIntersectionRoadNamesRelativeDirections
import org.scottishtecharmy.soundscape.geoengine.utils.getIntersectionsFeatureCollectionFromTileFeatureCollection
import org.scottishtecharmy.soundscape.geoengine.utils.getNearestRoad
import org.scottishtecharmy.soundscape.geoengine.utils.getRelativeDirectionsPolygons
import org.scottishtecharmy.soundscape.geoengine.utils.getRoadBearingToIntersection
import org.scottishtecharmy.soundscape.geoengine.utils.getRoadsFeatureCollectionFromTileFeatureCollection
import org.scottishtecharmy.soundscape.geoengine.utils.sortedByDistanceTo

class VisuallyCheckIntersectionLayers {

    // Checking how to process a complex intersection that also has a crossing
    @Test
    fun layeredIntersectionsFieldOfView1(){

        // Fake device location and device direction.
        val userGeometry = GeoEngine.UserGeometry(
            LngLatAlt(-2.6972713998905533,51.44374766171788),
            340.0,
            50.0
        )

        // Get the tile feature collection from the GeoJSON
        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val featureCollectionTest = moshi.adapter(FeatureCollection::class.java)
            .fromJson(GeoJSONDataComplexIntersection1.complexintersection1GeoJSON)
        // Get all the intersections from the tile
        val testIntersectionsCollectionFromTileFeatureCollection =
            getIntersectionsFeatureCollectionFromTileFeatureCollection(
                featureCollectionTest!!
            )
        // Get all the roads from the tile
        val testRoadsCollectionFromTileFeatureCollection = getRoadsFeatureCollectionFromTileFeatureCollection(
            featureCollectionTest
        )
        // Get all the crossings from the tile
        val testCrossingsCollectionFromTileFeatureCollection = getCrossingsFromTileFeatureCollection(
            featureCollectionTest
        )

        // Create a FOV triangle to pick up the intersections
        val fovIntersectionsFeatureCollection = getFovFeatureCollection(
            userGeometry,
            FeatureTree(testIntersectionsCollectionFromTileFeatureCollection)
        )
        // Create a FOV triangle to pick up the roads
        val fovRoadsFeatureCollection = getFovFeatureCollection(
            userGeometry,
            FeatureTree(testRoadsCollectionFromTileFeatureCollection)
        )
        // Create a FOV triangle to pick up the crossings
        // (crossings are Points so we can use the same function as for intersections)
        val fovCrossingsFeatureCollection = getFovFeatureCollection(
            userGeometry,
            FeatureTree(testCrossingsCollectionFromTileFeatureCollection)
        )
        // At this point we have three field of view FeatureCollections:
        // roads, intersections and crossings

        // *** This part is the intersection and road bothering ***
        // I will need a feature collection of all the intersections in the FOV sorted by distance to the current location
        val intersectionsSortedByDistance = sortedByDistanceTo(
            userGeometry.location,
            fovIntersectionsFeatureCollection
        )
        // Get the nearest Road in the FoV
        val testNearestRoad = getNearestRoad(userGeometry.location, FeatureTree(testRoadsCollectionFromTileFeatureCollection))
        val intersectionsNeedsFurtherCheckingFC = FeatureCollection()
        for (i in 0 until intersectionsSortedByDistance.features.size) {
            val intersectionRoadNames = getIntersectionRoadNames(intersectionsSortedByDistance.features[i], fovRoadsFeatureCollection)
            val intersectionsNeedsFurtherChecking = checkIntersection(i, intersectionRoadNames, testNearestRoad)
            if(intersectionsNeedsFurtherChecking) {
                intersectionsNeedsFurtherCheckingFC.addFeature(intersectionsSortedByDistance.features[i])
            }
        }
        // Approach 1: find the intersection feature with the most osm_ids and use that?
        val featureWithMostOsmIds: Feature? = intersectionsNeedsFurtherCheckingFC.features.maxByOrNull {
                feature ->
            (feature.foreign?.get("osm_ids") as? List<*>)?.size ?: 0
        }

        val points = getFovTrianglePoints(userGeometry)
        val nearestIntersection = FeatureTree(fovIntersectionsFeatureCollection).getNearestFeatureWithinTriangle(
            userGeometry.location,
            points.left,
            points.right)
        val nearestRoadBearing = getRoadBearingToIntersection(nearestIntersection, testNearestRoad, userGeometry.heading)
        val intersectionLocation = featureWithMostOsmIds!!.geometry as Point
        val intersectionRelativeDirections = getRelativeDirectionsPolygons(
            GeoEngine.UserGeometry(
                LngLatAlt(
                    intersectionLocation.coordinates.longitude,
                    intersectionLocation.coordinates.latitude
                ),
                nearestRoadBearing,
                5.0
            ),
            RelativeDirections.COMBINED
        )
        val intersectionRoadNames = getIntersectionRoadNames(featureWithMostOsmIds, fovRoadsFeatureCollection)
        val roadRelativeDirections = getIntersectionRoadNamesRelativeDirections(
            intersectionRoadNames,
            featureWithMostOsmIds,
            intersectionRelativeDirections
        )
        // *** End of Intersection and Road
        // *** Start of Crossing

        // This is interesting as this is the nearest crossing in the FoV
        // but the next nearest crossing is the one that contains additional information about
        // a traffic island for the road that we are currently on.
        // Original Soundscape doesn't flag that a crossing is a traffic island
        // or has tactile paving, etc.
        val nearestCrossing = FeatureTree(fovCrossingsFeatureCollection).getNearestFeatureWithinTriangle(
            userGeometry.location,
            points.left,
            points.right)
        // Confirm which road the crossing is on
        val crossingLocation = nearestCrossing!!.geometry as Point
        val nearestRoadToCrossing = getNearestRoad(
            LngLatAlt(crossingLocation.coordinates.longitude,crossingLocation.coordinates.latitude),
            FeatureTree(testRoadsCollectionFromTileFeatureCollection)
        )
        // *** End of Crossing

        // Road with nearest crossing
        Assert.assertEquals("Flax Bourton Road", nearestRoadToCrossing!!.properties?.get("name"))
        Assert.assertEquals("yes", nearestCrossing.properties?.get("tactile_paving"))
        // Junction info
        Assert.assertEquals(3, roadRelativeDirections.features.size)
        Assert.assertEquals(0, roadRelativeDirections.features[0].properties!!["Direction"])
        Assert.assertEquals("Flax Bourton Road", roadRelativeDirections.features[0].properties!!["name"])
        Assert.assertEquals(3, roadRelativeDirections.features[1].properties!!["Direction"])
        Assert.assertEquals("Clevedon Road", roadRelativeDirections.features[1].properties!!["name"])
        Assert.assertEquals(7, roadRelativeDirections.features[2].properties!!["Direction"])
        Assert.assertEquals("Clevedon Road", roadRelativeDirections.features[2].properties!!["name"])

        // *************************************************************
        // *** Display Field of View triangle ***

        // We can now construct our FOV polygon (triangle)
        val polygonTriangleFOV = createTriangleFOV(
            points.left,
            userGeometry.location,
            points.right
        )

        val featureFOVTriangle = Feature().also {
            val ars3: HashMap<String, Any?> = HashMap()
            ars3 += Pair("FoV", "45 degrees 35 meters")
            it.properties = ars3
        }
        featureFOVTriangle.geometry = polygonTriangleFOV

        fovIntersectionsFeatureCollection.addFeature(featureFOVTriangle)
        fovRoadsFeatureCollection.addFeature(featureFOVTriangle)
        fovCrossingsFeatureCollection.addFeature(featureFOVTriangle)

        val fovIntersections = moshi.adapter(FeatureCollection::class.java).toJson(fovIntersectionsFeatureCollection)
        // copy and paste into GeoJSON.io
        println("FOV Intersections: $fovIntersections")
        val fovRoads = moshi.adapter(FeatureCollection::class.java).toJson(fovRoadsFeatureCollection)
        // copy and paste into GeoJSON.io
        println("FOV Roads: $fovRoads")
        val fovCrossings = moshi.adapter(FeatureCollection::class.java).toJson(fovCrossingsFeatureCollection)
        // copy and paste into GeoJSON.io
        println("FOV Crossings: $fovCrossings")


    }

    private fun checkIntersection(
        intersectionNumber: Int,
        intersectionRoadNames: FeatureCollection,
        testNearestRoad:Feature?
    ): Boolean {
        println("Number of roads that make up intersection ${intersectionNumber}: ${intersectionRoadNames.features.size}")
        for (road in intersectionRoadNames) {
            val roadName = road.properties?.get("name")
            val isOneWay = road.properties?.get("oneway") == "yes"
            val isMatch = testNearestRoad!!.properties?.get("name") == roadName

            println("The road name is: $roadName")
            if (isMatch && isOneWay) {
                println("Intersection $intersectionNumber is probably a compound roundabout or compound intersection and we don't want to call it out.")
                return false
            } else if (isMatch) {
                println("Intersection $intersectionNumber is probably a compound roundabout or compound intersection and we don't want to call it out.")
                return false
            } else {
                println("Intersection $intersectionNumber is probably NOT a compound roundabout or compound intersection and we DO want to call it out.")
                return true
            }

        }
        return false
    }
}