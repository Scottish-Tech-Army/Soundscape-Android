package org.scottishtecharmy.soundscape

import com.squareup.moshi.Moshi
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
import org.scottishtecharmy.soundscape.utils.getIntersectionsFeatureCollectionFromTileFeatureCollection
import org.scottishtecharmy.soundscape.utils.getNearestIntersection
import org.scottishtecharmy.soundscape.utils.getNearestRoad
import org.scottishtecharmy.soundscape.utils.getQuadrants
import org.scottishtecharmy.soundscape.utils.getRelativeDirectionsPolygons
import org.scottishtecharmy.soundscape.utils.getRoadBearingToIntersection
import org.scottishtecharmy.soundscape.utils.getRoadsFeatureCollectionFromTileFeatureCollection
import org.scottishtecharmy.soundscape.utils.lineStringIsCircular
import org.scottishtecharmy.soundscape.utils.polygonContainsCoordinates

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
}