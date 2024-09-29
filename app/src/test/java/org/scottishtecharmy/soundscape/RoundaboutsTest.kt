package org.scottishtecharmy.soundscape

import com.squareup.moshi.Moshi
import org.junit.Assert
import org.junit.Test
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeoMoshi
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon
import org.scottishtecharmy.soundscape.utils.RelativeDirections
import org.scottishtecharmy.soundscape.utils.createTriangleFOV
import org.scottishtecharmy.soundscape.utils.getBoundingBoxCorners
import org.scottishtecharmy.soundscape.utils.getBoundingBoxOfLineString
import org.scottishtecharmy.soundscape.utils.getCenterOfBoundingBox
import org.scottishtecharmy.soundscape.utils.getDestinationCoordinate
import org.scottishtecharmy.soundscape.utils.getDistanceToFeatureCollection
import org.scottishtecharmy.soundscape.utils.getFovIntersectionFeatureCollection
import org.scottishtecharmy.soundscape.utils.getFovRoadsFeatureCollection
import org.scottishtecharmy.soundscape.utils.getIntersectionRoadNames
import org.scottishtecharmy.soundscape.utils.getIntersectionRoadNamesRelativeDirections
import org.scottishtecharmy.soundscape.utils.getIntersectionsFeatureCollectionFromTileFeatureCollection
import org.scottishtecharmy.soundscape.utils.getNearestIntersection
import org.scottishtecharmy.soundscape.utils.getNearestRoad
import org.scottishtecharmy.soundscape.utils.getQuadrants
import org.scottishtecharmy.soundscape.utils.getRelativeDirectionsPolygons
import org.scottishtecharmy.soundscape.utils.getRoadBearingToIntersection
import org.scottishtecharmy.soundscape.utils.getRoadsFeatureCollectionFromTileFeatureCollection
import org.scottishtecharmy.soundscape.utils.getTileXY
import org.scottishtecharmy.soundscape.utils.getXYTile
import org.scottishtecharmy.soundscape.utils.lineStringIsCircular
import org.scottishtecharmy.soundscape.utils.polygonContainsCoordinates
import org.scottishtecharmy.soundscape.utils.removeDuplicates

class RoundaboutsTest {
    //TODO There are a lot of different types of roundabouts so this might take me a while to work out
    // https://wiki.openstreetmap.org/wiki/Tag:junction=roundabout?uselang=en-GB
    @Test
    fun roundaboutsTest() {
        // This is a proof of concept so this is the simplest, cleanest roundabout I could find
        // and is only using a single tile from /16/32267/21812.json
        val currentLocation = LngLatAlt(-2.747119798700794, 51.43854214667482)
        val deviceHeading = 225.0
        val fovDistance = 50.0

        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val featureCollectionTest = moshi.adapter(FeatureCollection::class.java)
            .fromJson(GeoJSONRoundabout.featureCollectionRoundabout)
        val testRoadsCollectionFromTileFeatureCollection =
            getRoadsFeatureCollectionFromTileFeatureCollection(
                featureCollectionTest!!
            )

        // create FOV to pickup the road(s) and roundabout
        val fovRoadsFeatureCollection = getFovRoadsFeatureCollection(
            currentLocation,
            deviceHeading,
            fovDistance,
            testRoadsCollectionFromTileFeatureCollection
        )

        // It is straightforward to detect a roundabout if it has been tagged properly but
        // we don't yet know what road names connect to the roundabout and their relative directions
        // first thing is to identify the road that is the circular part as it
        // doesn't really tell the user anything in this test case
        for (road in fovRoadsFeatureCollection) {
            if (road.properties?.get("junction") == "roundabout") {
                // original string: directions_approaching_roundabout
                println("Approaching roundabout")
            }
        }
        // new feature collection to hold the roundabout exit roads but not the circle road
        val roundaboutExitRoads = FeatureCollection()
        val roundaboutCircleRoad = FeatureCollection()
        for (road in fovRoadsFeatureCollection) {
            if (lineStringIsCircular(road.geometry as LineString)) {
                roundaboutCircleRoad.addFeature(road)
            } else {
                roundaboutExitRoads.addFeature(road)
            }
        }
        val testIntersectionsCollectionFromTileFeatureCollection =
            getIntersectionsFeatureCollectionFromTileFeatureCollection(
                featureCollectionTest
            )

        val fovIntersectionsFeatureCollection = getFovIntersectionFeatureCollection(
            currentLocation,
            deviceHeading,
            fovDistance,
            testIntersectionsCollectionFromTileFeatureCollection
        )
        val nearestIntersection = getNearestIntersection(currentLocation, fovIntersectionsFeatureCollection)
        val distanceToRoundabout = getDistanceToFeatureCollection(
            currentLocation.latitude,
            currentLocation.longitude, nearestIntersection
        )
        // Original string "directions_roundabout_with_exits_distance" Roundabout with %1$@ exits %2$@ away
        println("Roundabout with ${roundaboutExitRoads.features.size} " +
                "exits ${distanceToRoundabout.features[0].foreign?.get("distance_to")} away")

        // Detect the relative directions of the exit roads ahead left, ahead, ahead right, blah
        // easiest way is probably to put a bounding box around the roundabout circle, find the center of the circle,
        // splat a relative directions polygon (this won't work for large roundabouts as the directions polygon will be too small but can fix that later)
        // and detect where the intersections are in the
        // triangles and hook up the roads using the osm_ids
        val boundingBoxOfCircle = getBoundingBoxOfLineString(roundaboutCircleRoad.features[0].geometry as LineString)
        val boundingBoxOfCircleCorners = getBoundingBoxCorners(boundingBoxOfCircle)
        val centerOfBoundingBox = getCenterOfBoundingBox(boundingBoxOfCircleCorners)
        val testNearestIntersection = getNearestIntersection(
            currentLocation,fovIntersectionsFeatureCollection)
        val testNearestRoad = getNearestRoad(currentLocation, fovRoadsFeatureCollection)
        val testNearestRoadBearing = getRoadBearingToIntersection(testNearestIntersection, testNearestRoad, deviceHeading)
        val roundaboutRoadsRelativeDirections = getRelativeDirectionsPolygons(
            centerOfBoundingBox,
            testNearestRoadBearing,
            fovDistance,
            RelativeDirections.COMBINED
        )

        // need to take out the intersection from fovIntersectionsFeatureCollection that represents
        // the circular road
        val cleanFovIntersectionsFeatureCollection = FeatureCollection()
        for (intersection in fovIntersectionsFeatureCollection) {
            val osmIds = intersection.foreign?.get("osm_ids") as? List<*>
            if (osmIds?.get(0) != osmIds?.get(1)){
                cleanFovIntersectionsFeatureCollection.addFeature(intersection)
            }
        }
        for (intersection in cleanFovIntersectionsFeatureCollection) {
            val point = intersection.geometry as Point
            for (direction in roundaboutRoadsRelativeDirections) {
                val intersectionIsHere = polygonContainsCoordinates(
                    LngLatAlt(point.coordinates.longitude, point.coordinates.latitude),
                    (direction.geometry as Polygon))
                if (intersectionIsHere){
                    println("Roundabout exit direction: ${direction.properties?.get("Direction")}")
                }
                // TODO need to get the road names to go with the direction of the intersections but in theory we can detect simple roundabouts
            }
        }

        // copy and paste into GeoJSON.io
        //val roads = moshi.adapter(FeatureCollection::class.java).toJson(roundaboutRoads)
        //println("roundabout roads? : $roads")

        //val testNearestIntersection = getNearestIntersection(currentLocation,fovIntersectionsFeatureCollection)

        // copy and paste into GeoJSON.io
        //val intersections = moshi.adapter(FeatureCollection::class.java).toJson(fovIntersectionsFeatureCollection)
        //println("intersections: $intersections")

        // Below is just the visual part to display the FOV and check we are picking up the roundabout and roads
        // Direction the device is pointing
        val quadrants = getQuadrants(deviceHeading)
        // get the quadrant index from the heading so we can construct a FOV triangle using the correct quadrant
        var quadrantIndex = 0
        for (quadrant in quadrants) {
            val containsHeading = quadrant.contains(deviceHeading)
            if (containsHeading) {
                break
            } else {
                quadrantIndex++
            }
        }
        // Get the coordinate for the "Left" of the FOV
        val destinationCoordinateLeft = getDestinationCoordinate(
            LngLatAlt(currentLocation.longitude, currentLocation.latitude),
            quadrants[quadrantIndex].left,
            fovDistance
        )

        //Get the coordinate for the "Right" of the FOV
        val destinationCoordinateRight = getDestinationCoordinate(
            LngLatAlt(currentLocation.longitude, currentLocation.latitude),
            quadrants[quadrantIndex].right,
            fovDistance
        )

        // We can now construct our FOV polygon (triangle)
        val polygonTriangleFOV = createTriangleFOV(
            destinationCoordinateLeft,
            currentLocation,
            destinationCoordinateRight
        )

        val featureFOVTriangle = Feature().also {
            val ars3: HashMap<String, Any?> = HashMap()
            ars3 += Pair("FoV", "45 degrees ~35 meters")
            it.properties = ars3
        }
        featureFOVTriangle.geometry = polygonTriangleFOV
        fovRoadsFeatureCollection.addFeature(featureFOVTriangle)

        // copy and paste into GeoJSON.io
        val roundabouts =
            moshi.adapter(FeatureCollection::class.java).toJson(fovRoadsFeatureCollection)
        println(roundabouts)
    }

    @Test
    fun miniRoundaboutTest(){

        // Soundscape seems to turn mini roundabouts into intersections so
        // using a single tile from /16/32268/21813.json to test this and the location
        // where there is definitely a mini roundabout
        val currentLocation = LngLatAlt(-2.7428307423190006,
            51.43595874012766)
        val deviceHeading = 0.0
        val fovDistance = 50.0

        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val featureCollectionTest = moshi.adapter(FeatureCollection::class.java)
            .fromJson(GeoJSONRoundaboutMini.featureCollectionRoundaboutMini)

        // Get the roads from the tile
        val testRoadsCollectionFromTileFeatureCollection =
            getRoadsFeatureCollectionFromTileFeatureCollection(
                featureCollectionTest!!
            )
        // create FOV to pickup the roads
        val fovRoadsFeatureCollection = getFovRoadsFeatureCollection(
            currentLocation,
            deviceHeading,
            fovDistance,
            testRoadsCollectionFromTileFeatureCollection
        )
        // Get the intersections from the tile
        val testIntersectionsCollectionFromTileFeatureCollection =
            getIntersectionsFeatureCollectionFromTileFeatureCollection(
                featureCollectionTest
            )

        val fovIntersectionsFeatureCollection = getFovIntersectionFeatureCollection(
            currentLocation,
            deviceHeading,
            fovDistance,
            testIntersectionsCollectionFromTileFeatureCollection
        )

        // get the nearest intersection in the FoV and the roads that make up the intersection
        val testNearestIntersection = getNearestIntersection(
            currentLocation,fovIntersectionsFeatureCollection)

        // This will remove the duplicate "osm_ids" from the intersection
        val cleanNearestIntersection = removeDuplicates(testNearestIntersection)

        val testNearestRoad = getNearestRoad(currentLocation, fovRoadsFeatureCollection)

        val testNearestRoadBearing = getRoadBearingToIntersection(cleanNearestIntersection, testNearestRoad, deviceHeading)

        val testIntersectionRoadNames = getIntersectionRoadNames(
            cleanNearestIntersection, fovRoadsFeatureCollection)

        // are any of the roads that make up the intersection circular?
        for(road in testIntersectionRoadNames){
            if (lineStringIsCircular(road.geometry as LineString)){
                println("Circular path")
            }

        }
        val intersectionLocation = cleanNearestIntersection.features[0].geometry as Point

        val intersectionRelativeDirections = getRelativeDirectionsPolygons(
            LngLatAlt(intersectionLocation.coordinates.longitude, intersectionLocation.coordinates.latitude),
            testNearestRoadBearing,
            fovDistance,
            RelativeDirections.COMBINED
        )

        val roadRelativeDirections = getIntersectionRoadNamesRelativeDirections(
            testIntersectionRoadNames,
            cleanNearestIntersection,
            intersectionRelativeDirections)

        Assert.assertEquals(0, roadRelativeDirections.features[0].properties!!["Direction"])
        Assert.assertEquals("Elm Lodge Road", roadRelativeDirections.features[0].properties!!["name"])
        Assert.assertEquals(2, roadRelativeDirections.features[1].properties!!["Direction"])
        Assert.assertEquals("Elm Lodge Road", roadRelativeDirections.features[1].properties!!["name"])
        Assert.assertEquals(5, roadRelativeDirections.features[2].properties!!["Direction"])
        Assert.assertEquals("Green Pastures Road", roadRelativeDirections.features[2].properties!!["name"])
    }
}