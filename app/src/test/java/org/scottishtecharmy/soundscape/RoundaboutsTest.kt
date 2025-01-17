package org.scottishtecharmy.soundscape

import com.squareup.moshi.Moshi
import org.junit.Assert
import org.junit.Test
import org.scottishtecharmy.soundscape.dto.Circle
import org.scottishtecharmy.soundscape.geoengine.GeoEngine
import org.scottishtecharmy.soundscape.geoengine.GridState.Companion.createFromGeoJson
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeoMoshi
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon
import org.scottishtecharmy.soundscape.geoengine.utils.RelativeDirections
import org.scottishtecharmy.soundscape.geoengine.utils.calculateCenterOfCircle
import org.scottishtecharmy.soundscape.geoengine.utils.createTriangleFOV
import org.scottishtecharmy.soundscape.geoengine.utils.getBoundingBoxCorners
import org.scottishtecharmy.soundscape.geoengine.utils.getBoundingBoxOfLineString
import org.scottishtecharmy.soundscape.geoengine.utils.getCenterOfBoundingBox
import org.scottishtecharmy.soundscape.geoengine.utils.getDistanceToFeature
import org.scottishtecharmy.soundscape.geoengine.utils.getFovFeatureCollection
import org.scottishtecharmy.soundscape.geoengine.utils.getFovTrianglePoints
import org.scottishtecharmy.soundscape.geoengine.utils.getIntersectionRoadNames
import org.scottishtecharmy.soundscape.geoengine.utils.getIntersectionRoadNamesRelativeDirections
import org.scottishtecharmy.soundscape.geoengine.utils.getNearestRoad
import org.scottishtecharmy.soundscape.geoengine.utils.getRelativeDirectionsPolygons
import org.scottishtecharmy.soundscape.geoengine.utils.getRoadBearingToIntersection
import org.scottishtecharmy.soundscape.geoengine.utils.lineStringIsCircular
import org.scottishtecharmy.soundscape.geoengine.utils.polygonContainsCoordinates
import org.scottishtecharmy.soundscape.geoengine.utils.removeDuplicateOsmIds
import org.scottishtecharmy.soundscape.geoengine.utils.removeDuplicates
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Geometry
import org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiLineString

class RoundaboutsTest {
    //TODO There are a lot of different types of roundabouts so this might take me a while to work out
    // https://wiki.openstreetmap.org/wiki/Tag:junction=roundabout?uselang=en-GB
    @Test
    fun roundaboutsTest() {
        // This is a proof of concept so this is the simplest, cleanest roundabout I could find
        // and is only using a single tile from /16/32267/21812.json
        val userGeometry = GeoEngine.UserGeometry(
            LngLatAlt(-2.747119798700794, 51.43854214667482),
            225.0,
            50.0
        )

        val gridState = createFromGeoJson(GeoJSONRoundabout.featureCollectionRoundabout)
        val testRoadsCollectionFromTileFeatureCollection = gridState.getFeatureCollection(TreeId.ROADS)

        // create FOV to pickup the road(s) and roundabout
        val fovRoadsFeatureCollection = getFovFeatureCollection(
            userGeometry,
            FeatureTree(testRoadsCollectionFromTileFeatureCollection)
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
        val testIntersectionsCollectionFromTileFeatureCollection = gridState.getFeatureCollection(TreeId.INTERSECTIONS)
        val fovIntersectionsFeatureCollection = getFovFeatureCollection(
            userGeometry,
            FeatureTree(testIntersectionsCollectionFromTileFeatureCollection)
        )

        val points = getFovTrianglePoints(userGeometry)
        val nearestIntersection = FeatureTree(testIntersectionsCollectionFromTileFeatureCollection).getNearestFeatureWithinTriangle(
            userGeometry.location,
            points.left,
            points.right)

        val distanceToRoundabout = getDistanceToFeature(userGeometry.location, nearestIntersection)
        // Original string "directions_roundabout_with_exits_distance" Roundabout with %1$@ exits %2$@ away
        println("Roundabout with ${roundaboutExitRoads.features.size} " +
                "exits $distanceToRoundabout away")

        // Detect the relative directions of the exit roads ahead left, ahead, ahead right, blah
        // easiest way is probably to put a bounding box around the roundabout circle, find the center of the circle,
        // splat a relative directions polygon (this won't work for large roundabouts as the directions polygon will be too small but can fix that later)
        // and detect where the intersections are in the
        // triangles and hook up the roads using the osm_ids
        val boundingBoxOfCircle = getBoundingBoxOfLineString(roundaboutCircleRoad.features[0].geometry as LineString)
        val boundingBoxOfCircleCorners = getBoundingBoxCorners(boundingBoxOfCircle)
        val centerOfBoundingBox = getCenterOfBoundingBox(boundingBoxOfCircleCorners)
        val testNearestRoad = getNearestRoad(userGeometry.location, FeatureTree(fovRoadsFeatureCollection))
        val testNearestRoadBearing =
            getRoadBearingToIntersection(nearestIntersection, testNearestRoad, userGeometry.heading)
        val geometry = GeoEngine.UserGeometry(centerOfBoundingBox, testNearestRoadBearing, userGeometry.fovDistance)
        val roundaboutRoadsRelativeDirections = getRelativeDirectionsPolygons(
            geometry,
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
                    point.coordinates,
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

        // We can now construct our FOV polygon (triangle)
        val polygonTriangleFOV = createTriangleFOV(
            points.left,
            userGeometry.location,
            points.right
        )

        val featureFOVTriangle = Feature().also {
            val ars3: HashMap<String, Any?> = HashMap()
            ars3 += Pair("FoV", "45 degrees ~35 meters")
            it.properties = ars3
        }
        featureFOVTriangle.geometry = polygonTriangleFOV
        fovRoadsFeatureCollection.addFeature(featureFOVTriangle)

        // copy and paste into GeoJSON.io
        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val roundabouts =
            moshi.adapter(FeatureCollection::class.java).toJson(fovRoadsFeatureCollection)
        println(roundabouts)
    }

    @Test
    fun miniRoundaboutTest(){

        // Soundscape seems to turn mini roundabouts into intersections so
        // using a single tile from /16/32268/21813.json to test this and the location
        // where there is definitely a mini roundabout
        val userGeometry = GeoEngine.UserGeometry(
            LngLatAlt(-2.7428307423190006,51.43595874012766),
            0.0,
            50.0
        )

        val gridState = createFromGeoJson(GeoJSONRoundaboutMini.featureCollectionRoundaboutMini)

        // Get the roads from the tile
        val testRoadsCollectionFromTileFeatureCollection = gridState.getFeatureCollection(TreeId.ROADS)

        // create FOV to pickup the roads
        val fovRoadsFeatureCollection = getFovFeatureCollection(
            userGeometry,
            FeatureTree(testRoadsCollectionFromTileFeatureCollection)
        )
        // Get the intersections from the tile
        val testIntersectionsCollectionFromTileFeatureCollection = gridState.getFeatureCollection(TreeId.INTERSECTIONS)

        // get the nearest intersection in the FoV and the roads that make up the intersection
        val points = getFovTrianglePoints(userGeometry)
        val nearestIntersection = FeatureTree(testIntersectionsCollectionFromTileFeatureCollection).getNearestFeatureWithinTriangle(
            userGeometry.location,
            points.left,
            points.right)

        // This will remove the duplicate "osm_ids" from the intersection
        val cleanNearestIntersection = removeDuplicates(nearestIntersection)

        val testNearestRoad = getNearestRoad(userGeometry.location, FeatureTree(fovRoadsFeatureCollection))

        val testNearestRoadBearing = getRoadBearingToIntersection(cleanNearestIntersection, testNearestRoad, userGeometry.heading)

        val testIntersectionRoadNames = getIntersectionRoadNames(
            cleanNearestIntersection, fovRoadsFeatureCollection)

        // are any of the roads that make up the intersection circular?
        for(road in testIntersectionRoadNames){
            if (lineStringIsCircular(road.geometry as LineString)){
                println("Circular path")
            }

        }
        val intersectionLocation = cleanNearestIntersection!!.geometry as Point

        val geometry = GeoEngine.UserGeometry(
            intersectionLocation.coordinates,
            testNearestRoadBearing,
            userGeometry.fovDistance)
        val intersectionRelativeDirections = getRelativeDirectionsPolygons(
            geometry,
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

    @Test
    fun simpleCompoundRoundaboutTest(){
        // Roundabouts can be a compound of intersections and roads
        // Using a single tile from /16/32267/21812.json
        // map of roundabout: https://geojson.io/#map=19.25/51.4379032/-2.74764
        // further away  -2.7474554685902604, 51.43822549502224
        // further away -2.7474735115298756, 51.4381923217035
        // closer but not complete -2.7475109456763676,51.43813340231313
        val userGeometry = GeoEngine.UserGeometry(
            LngLatAlt(-2.7474554685902604, 51.43822549502224),
            200.0,
            50.0
        )

        val gridState = createFromGeoJson(GeoJSONRoundabout.featureCollectionRoundabout)
        val testRoadsCollectionFromTileFeatureCollection = gridState.getFeatureCollection(TreeId.ROADS)
        val testIntersectionsCollectionFromTileFeatureCollection = gridState.getFeatureCollection(TreeId.INTERSECTIONS)

        // create FOV to pickup the road(s) and roundabout (this won't detect every road as we are too far away)
        val fovRoadsFeatureCollection = getFovFeatureCollection(
            userGeometry,
            FeatureTree(testRoadsCollectionFromTileFeatureCollection)
        )
        // get the nearest intersection in the FoV
        val points = getFovTrianglePoints(userGeometry)
        val testNearestIntersection = FeatureTree(testIntersectionsCollectionFromTileFeatureCollection).getNearestFeatureWithinTriangle(
            userGeometry.location,
            points.left,
            points.right)

        // get the roads that make up the intersection - this is where the road splits into two "oneway" roads
        val intersectionRoadNames = getIntersectionRoadNames(testNearestIntersection, fovRoadsFeatureCollection)
        println("Number of roads that make up the nearest intersection ${intersectionRoadNames.features.size}")
        // I need to test that the intersection roads have
        // "oneway" and "yes" tags and that the road names are all the same
        val testNearestRoad = getNearestRoad(userGeometry.location, FeatureTree(fovRoadsFeatureCollection))
        for (road in intersectionRoadNames) {
            if(testNearestRoad!!.properties?.get("name") == road.properties?.get("name")
                && road.properties?.get("oneway") == "yes"){
                println("Intersection is probably a compound roundabout or compound intersection and we don't want to call it out.")
            }
        }
        // Once we've ignored the intersection above it is straightforward to detect a roundabout
        // if it has been tagged properly with the "junction" tag set to "roundabout" but
        // we don't yet know what road names connect to the roundabout and their relative directions
        var thereIsARoundabout = false
        for (road in fovRoadsFeatureCollection) {
            if (road.properties?.get("junction") == "roundabout") {
                // original string: directions_approaching_roundabout
                println("Approaching roundabout")
                thereIsARoundabout = true
                break
            }
        }
        // this now gets messy as not all roundabouts are mapped consistently
        if (thereIsARoundabout) {
            // new feature collection to hold the roundabout exit roads but not the circle road
            val roundaboutExitRoads = FeatureCollection()
            // this contains the circle if it exists
            val roundaboutCircleRoad = FeatureCollection()
            // if the circle doesn't exist but is made up of segments/linestrings then I'm going to add each segment to a new multilinestring
            val roundaboutCircleSegments = MultiLineString()
            // coordinates for center of roundabout
            var roundaboutCenter = Circle()

            for (road in fovRoadsFeatureCollection) {
                if (lineStringIsCircular(road.geometry as LineString)) {
                    // If we get to this point then -> "yay" the "circle" has been mapped but
                    // potentially a "circle" can be made up of a series of road segments not just
                    // one circle -> "boo"
                    roundaboutCircleRoad.addFeature(road)
                } else if (road.properties?.get("junction") == "roundabout") {
                    // I want to add each tagged road/linestring segment to a multilinestring
                    // as I don't want to throw them away
                    // but need to work out the center of the roundabout by using the segment
                    roundaboutCenter = calculateCenterOfCircle(road.geometry as LineString)
                    roundaboutCircleSegments.coordinates.add((road.geometry as LineString).coordinates)

                } else {
                    roundaboutExitRoads.addFeature(road)
                }
            }
            // if the circle doesn't exist (which it doesn't in this test)
            if (roundaboutCircleRoad.features.size == 0) {

                val testNearestRoadBearing = getRoadBearingToIntersection(testNearestIntersection, testNearestRoad, userGeometry.heading)
                //create a relative directions polygon based on our calculated approx center of the roundabout
                // How big do we want out polygon to be? Ideally we need the radius of the roundabout plus a few meters
                val intersectionRelativeDirectionsPolygons = getRelativeDirectionsPolygons(
                    GeoEngine.UserGeometry(
                        roundaboutCenter.center,
                        testNearestRoadBearing,
                        // TODO adding 3.0 is a fudge as the roundaboutCenter.center is not 100% accurate
                        roundaboutCenter.radius + 3.0
                    ),
                    RelativeDirections.COMBINED
                )
                // temp see the relative directions polygons and bounding box of roundabout
                //intersectionRelativeDirectionsPolygons.addFeature(featurePolygonBB)
                val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
                val relativeDirectionPolygons =
                    moshi.adapter(FeatureCollection::class.java).toJson(intersectionRelativeDirectionsPolygons)
                println("relative directions polygons: $relativeDirectionPolygons")

                val allRoadsInRoundabout = FeatureCollection()
                val allIntersectionsInRoundabout = FeatureCollection()
                // this is looping through each FoV triangle in the roundabout
                for (triangle in intersectionRelativeDirectionsPolygons){

                    val geometry = triangle.geometry as Geometry<ArrayList<LngLatAlt>>
                    val fovRoundaboutRoadsFeatureCollection = FeatureTree(testRoadsCollectionFromTileFeatureCollection).generateFeatureCollectionWithinTriangle(
                        geometry.coordinates[0][0],
                        geometry.coordinates[0][1],
                        geometry.coordinates[0][2]
                    )
                    for (road in fovRoundaboutRoadsFeatureCollection){
                        // roads can span multiple triangles so we need to check for duplicates later
                        if (road.properties?.get("name") != null){
                            allRoadsInRoundabout.addFeature(road)
                        }
                    }
                    val fovRoundaboutIntersectionsFeatureCollection = FeatureTree(testIntersectionsCollectionFromTileFeatureCollection).generateFeatureCollectionWithinTriangle(
                        geometry.coordinates[0][0],
                        geometry.coordinates[0][1],
                        geometry.coordinates[0][2]
                    )
                    for (intersection in fovRoundaboutIntersectionsFeatureCollection){
                        allIntersectionsInRoundabout.addFeature(intersection)
                    }
                }
                // roads can potentially span multiple FoV triangles so we need to check for duplicates
                val cleanAllRoadsInRoundabout = removeDuplicateOsmIds(allRoadsInRoundabout)
                // throw away one of the roads if they have the same name
                val duplicateRoadsRemoved = FeatureCollection()
                val roadNames = mutableSetOf<String?>()
                for (road in cleanAllRoadsInRoundabout) {
                    val roadName = road.properties?.get("name")
                    if (!roadNames.contains(roadName)) {
                        roadNames.add(roadName.toString())
                        duplicateRoadsRemoved.features.add(road)
                    }
                }

                val roadsInRelativeDirectionsPolygons =
                    moshi.adapter(FeatureCollection::class.java).toJson(duplicateRoadsRemoved)
                println("This should display all the named roads only once at the roundabout: $roadsInRelativeDirectionsPolygons")
                val intersectionsInRelativeDirectionsPolygons =
                    moshi.adapter(FeatureCollection::class.java).toJson(allIntersectionsInRoundabout)
                println("This should display all the intersections at the roundabout: $intersectionsInRelativeDirectionsPolygons")

                // I just want to keep the intersections which match with the roads we have left so..
                val intersectionsWithMatchingRoads = FeatureCollection()

                for (intersection in allIntersectionsInRoundabout) {
                    var hasMatch = false
                    val intersectionOsmIds = intersection.foreign?.get("osm_ids") as? List<*>

                    if (intersectionOsmIds != null) {
                        for (intersectionOsmId in intersectionOsmIds) {
                            for (road in duplicateRoadsRemoved) {
                                val roadOsmIds = road.foreign?.get("osm_ids") as? List<*>

                                if (roadOsmIds != null) {
                                    if (roadOsmIds.contains(intersectionOsmId)) {
                                        hasMatch = true
                                        break
                                    }
                                }
                            }
                            if (hasMatch) break
                        }
                    }

                    if (hasMatch) {
                        intersectionsWithMatchingRoads.addFeature(intersection)
                    }
                }
                // temp
                val matchingIntersectionsInRelativeDirectionsPolygons =
                    moshi.adapter(FeatureCollection::class.java).toJson(intersectionsWithMatchingRoads)
                println("This should display only the matching intersections at the roundabout: $matchingIntersectionsInRelativeDirectionsPolygons")
                // TODO I need to fix this
                /*val newFeatureCollection = FeatureCollection()
                for (road in duplicateRoadsRemoved){
                    for (intersection in intersectionsWithMatchingRoads) {
                        for(direction in intersectionRelativeDirectionsPolygons) {
                            val test = intersection.geometry as Point
                            val iAmHere1 = polygonContainsCoordinates(
                                test.coordinates, (direction.geometry as Polygon)
                            )
                            if (iAmHere1) {
                                val newFeature = mergeRoadAndDirectionFeatures(road, direction)
                                newFeatureCollection.addFeature(newFeature)
                            }
                        }
                    }
                }
                val newFeatureCollectionJson = moshi.adapter(FeatureCollection::class.java).toJson(newFeatureCollection)
                println("This should display the combined roads and directions: $newFeatureCollectionJson")*/

                val distanceToRoundabout = userGeometry.location.distance(roundaboutCenter.center)

                // Original string "directions_roundabout_with_exits_distance" Roundabout with %1$@ exits %2$@ away
                println(
                    "Roundabout with ${duplicateRoadsRemoved.features.size} " +
                            "exits ${distanceToRoundabout.toInt()} meters away"
                )
                println("Still TODO what are the exit road names and their relative directions?")


            } else {
                // we won't ever get here in this test as there is no circle just segments/linestrings
                println("Circle exists")
            }
        }

        val polygonTriangleFOV = createTriangleFOV(
            points.left,
            userGeometry.location,
            points.right
        )

        val featureFOVTriangle = Feature().also {
            val ars3: HashMap<String, Any?> = HashMap()
            ars3 += Pair("FoV", "45 degrees ~35 meters")
            it.properties = ars3
        }
        featureFOVTriangle.geometry = polygonTriangleFOV
        fovRoadsFeatureCollection.addFeature(featureFOVTriangle)

        // copy and paste into GeoJSON.io
        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val roundabouts =
            moshi.adapter(FeatureCollection::class.java).toJson(fovRoadsFeatureCollection)
        println(roundabouts)

    }
}