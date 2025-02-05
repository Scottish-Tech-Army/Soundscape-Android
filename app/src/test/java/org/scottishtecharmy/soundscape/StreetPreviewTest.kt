package org.scottishtecharmy.soundscape

import com.squareup.moshi.Moshi
import org.junit.Test
import org.scottishtecharmy.soundscape.geoengine.GridState.Companion.createFromGeoJson
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeoMoshi
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.geoengine.utils.RelativeDirections
import org.scottishtecharmy.soundscape.geoengine.utils.bearingFromTwoPoints
import org.scottishtecharmy.soundscape.geoengine.utils.checkWhetherIntersectionIsOfInterest
import org.scottishtecharmy.soundscape.geoengine.utils.createPolygonFromTriangle
import org.scottishtecharmy.soundscape.geoengine.utils.getFovFeatureCollection
import org.scottishtecharmy.soundscape.geoengine.utils.getFovTriangle
import org.scottishtecharmy.soundscape.geoengine.utils.getIntersectionRoadNames
import org.scottishtecharmy.soundscape.geoengine.utils.getIntersectionRoadNamesRelativeDirections
import org.scottishtecharmy.soundscape.geoengine.utils.getNearestRoad
import org.scottishtecharmy.soundscape.geoengine.utils.getRelativeDirectionsPolygons
import org.scottishtecharmy.soundscape.geoengine.utils.getRoadBearingToIntersection
import org.scottishtecharmy.soundscape.geoengine.utils.sortedByDistanceTo
import org.scottishtecharmy.soundscape.geoengine.utils.traceLineString

class StreetPreviewTest {

    @Test
    fun streetPreviewTest1() {
        // Start of PoC to track along a road from start to finish and generate field of view triangles
        // as the device moves along the road.
        val gridState = createFromGeoJson(GeoJsonDataReal.featureCollectionJsonRealSoundscapeGeoJson)
        val roadFeatureCollectionTest = gridState.getFeatureCollection(TreeId.ROADS)
        val nearestRoad = getNearestRoad(
            LngLatAlt(-2.693002695425122,51.43938442591545),
            FeatureTree(roadFeatureCollectionTest)
        )
        val nearestRoadTest = FeatureCollection()
        nearestRoadTest.addFeature(nearestRoad!!)

        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val nearestRoadString = moshi.adapter(FeatureCollection::class.java).toJson(nearestRoadTest)
        // copy and paste into GeoJSON.io
        println("Nearest road/linestring $nearestRoadString")
        // trace along the road with equidistant points
        val roadTrace = traceLineString(nearestRoadTest, 30.0)
        val roadTraceString = moshi.adapter(FeatureCollection::class.java).toJson(roadTrace)
        // copy and paste into GeoJSON.io
        println("Road trace: $roadTraceString")
        val fovFeatureCollection = FeatureCollection()
        var i = 1
        // walk down the road using the Points from the roadTrace FeatureCollection as a track
        for (feature in roadTrace.features.subList(0, roadTrace.features.size - 1)) {
            val currentPoint = feature.geometry as Point
            val currentLocation = currentPoint.coordinates

            val nextLocation = roadTrace.features[i++].geometry as Point
            // fake the device heading by "looking" at the next Point
            val deviceHeading = bearingFromTwoPoints(currentLocation, nextLocation.coordinates)
            println("Device Heading: $deviceHeading")
            val geometry = UserGeometry(currentLocation, deviceHeading)
            val fovTriangle = generateFOVTriangle(geometry)
            fovFeatureCollection.addFeature(fovTriangle)
        }
        val fovFeatureCollectionString = moshi.adapter(FeatureCollection::class.java).toJson(fovFeatureCollection)
        // copy and paste into GeoJSON.io
        println("FoV triangles for road trace: $fovFeatureCollectionString")

    }

    @Test
    fun streetPreviewTest2() {
        // Start of PoC to track along a road from start to finish and generate field of view triangles
        // as the device moves along the road and print the Callouts to the console

        val gridState = createFromGeoJson(GeoJSONStreetPreviewTest.streetPreviewTest)

        // Pull out the data layers that we would need for Ahead Of Me
        val roadFeatureCollectionTest = gridState.getFeatureCollection(TreeId.ROADS)
        val intersectionsFeatureCollectionTest = gridState.getFeatureCollection(TreeId.INTERSECTIONS)
        val crossingsFeatureCollectionTest = gridState.getFeatureCollection(TreeId.CROSSINGS)
        val busStopsGridFeatureCollection = gridState.getFeatureCollection(TreeId.BUS_STOPS)
        val nearestRoadTest =  getNearestRoad(
            LngLatAlt(-2.693002695425122,51.43938442591545),
            FeatureTree(roadFeatureCollectionTest)
        )

        // trace along the road with equidistant points 30m apart.
        val roadTrace = nearestRoadTest?.let {
            val fcIt =FeatureCollection()
            fcIt.addFeature(it)
            traceLineString(fcIt, 30.0)
        }

        var i = 2
        // walk down the road using the Points from the roadTrace FeatureCollection as a track
        if (roadTrace != null) {
            // I'm not including the start of the road and jumping to second point (Point 2) and then
            // looping through until second to last point (Point 14)
            for (feature in roadTrace.features.subList(1, roadTrace.features.size - 1)) {
                // Hold the text for the callouts:
                val results : MutableList<String> = mutableListOf()

                val currentPoint = feature.geometry as Point

                val nextLocation = roadTrace.features[i++].geometry as Point
                // fake the device heading by "looking" at the next Point
                val userGeometry = UserGeometry(
                    currentPoint.coordinates,
                    bearingFromTwoPoints(currentPoint.coordinates, nextLocation.coordinates),
                    50.0
                )

                if (roadFeatureCollectionTest.features.size > 0) {
                    val fovRoadsFeatureCollection = getFovFeatureCollection(
                        userGeometry,
                        FeatureTree(roadFeatureCollectionTest)
                    )
                    val fovIntersectionsFeatureCollection = getFovFeatureCollection(
                        userGeometry,
                        FeatureTree(intersectionsFeatureCollectionTest)
                    )
                    val fovCrossingsFeatureCollection = getFovFeatureCollection(
                        userGeometry,
                        FeatureTree(crossingsFeatureCollectionTest)
                    )
                    val fovBusStopsFeatureCollection = getFovFeatureCollection(
                        userGeometry,
                        FeatureTree(busStopsGridFeatureCollection)
                    )

                    if (fovRoadsFeatureCollection.features.size > 0) {
                        val nearestRoad = getNearestRoad(
                            userGeometry.location,
                            FeatureTree(roadFeatureCollectionTest)
                        )

                        if (nearestRoad!!.properties?.get("name") != null) {
                            results.add(
                                "Ahead ${nearestRoad.properties!!["name"]}"
                            )
                        } else {
                            // we are detecting an unnamed road here but pretending there is nothing here
                            results.add(
                                "There is nothing to call out right now"
                            )
                        }

                        if (fovIntersectionsFeatureCollection.features.size > 0) {

                            val intersectionsSortedByDistance = sortedByDistanceTo(
                                userGeometry.location,
                                fovIntersectionsFeatureCollection
                            )

                            val testNearestRoad = getNearestRoad(
                                userGeometry.location,
                                FeatureTree(roadFeatureCollectionTest)
                            )
                            val intersectionsNeedsFurtherCheckingFC = FeatureCollection()

                            for (y in 0 until intersectionsSortedByDistance.features.size) {
                                val intersectionRoadNames = getIntersectionRoadNames(intersectionsSortedByDistance.features[y], fovRoadsFeatureCollection)
                                val intersectionsNeedsFurtherChecking = checkWhetherIntersectionIsOfInterest(intersectionRoadNames, testNearestRoad)
                                if(intersectionsNeedsFurtherChecking) {
                                    intersectionsNeedsFurtherCheckingFC.addFeature(intersectionsSortedByDistance.features[y])
                                }
                            }
                            if (intersectionsNeedsFurtherCheckingFC.features.size > 0) {
                                // Approach 1: find the intersection feature with the most osm_ids and use that?
                                val featureWithMostOsmIds: Feature? = intersectionsNeedsFurtherCheckingFC.features.maxByOrNull { intersectionFeature ->
                                    (intersectionFeature.foreign?.get("osm_ids") as? List<*>)?.size ?: 0
                                }

                                val nearestIntersection = FeatureTree(fovIntersectionsFeatureCollection).getNearestFeature(
                                    userGeometry.location
                                )
                                val nearestRoadBearing = getRoadBearingToIntersection(nearestIntersection, testNearestRoad, userGeometry.heading())
                                val intersectionLocation = featureWithMostOsmIds!!.geometry as Point
                                val geometry = UserGeometry(
                                    intersectionLocation.coordinates,
                                    nearestRoadBearing,
                                    5.0
                                )
                                val intersectionRelativeDirections = getRelativeDirectionsPolygons(
                                    geometry,
                                    RelativeDirections.COMBINED
                                )
                                val distanceToNearestIntersection = userGeometry.location.distance(
                                    intersectionLocation.coordinates
                                )
                                val intersectionRoadNames = getIntersectionRoadNames(featureWithMostOsmIds, fovRoadsFeatureCollection)
                                results.add(
                                    "Approaching intersection ${distanceToNearestIntersection.toInt()} metres"
                                )

                                val roadRelativeDirections = getIntersectionRoadNamesRelativeDirections(
                                    intersectionRoadNames,
                                    featureWithMostOsmIds,
                                    intersectionRelativeDirections
                                )
                                for (directionFeature in roadRelativeDirections.features) {
                                    val direction =
                                        directionFeature.properties?.get("Direction").toString().toIntOrNull()
                                    // Don't call out the road we are on (0) as part of the intersection
                                    if (direction != null && direction != 0) {

                                        val relativeDirectionString = getRelativeDirectionLabelStreetPreview( direction)

                                        if (directionFeature.properties?.get("name") != null) {
                                            val intersectionCallout =  "Intersection with ${directionFeature.properties?.get("name")} $relativeDirectionString"
                                            results.add(
                                                intersectionCallout
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // detect if there is a crossing in the FOV
                    val nearestCrossing = FeatureTree(fovCrossingsFeatureCollection).getNearestFeature(
                        userGeometry.location
                    )
                    if (nearestCrossing != null) {
                        val crossingLocation = nearestCrossing.geometry as Point
                        val distanceToCrossing = userGeometry.location.distance(crossingLocation.coordinates)
                        // Confirm which road the crossing is on
                        val nearestRoadToCrossing = getNearestRoad(
                            crossingLocation.coordinates,
                            FeatureTree(roadFeatureCollectionTest)
                        )

                        val crossingCallout = buildString {
                            append("Crossing")
                            append(". ")
                            append("${distanceToCrossing.toInt()} metres")
                            append(". ")
                            if (nearestRoadToCrossing!!.properties?.get("name") != null){
                                append(nearestRoadToCrossing.properties?.get("name"))
                            }
                        }
                        results.add(crossingCallout)
                    }

                    // detect if there is a bus_stop in the FOV
                    val nearestBusStop = FeatureTree(fovBusStopsFeatureCollection).getNearestFeature(
                        userGeometry.location
                    )
                    if (nearestBusStop != null) {
                        val busStopLocation = nearestBusStop.geometry as Point
                        val distanceToBusStop = userGeometry.location.distance(
                            busStopLocation.coordinates
                        )
                        // Confirm which road the crossing is on
                        val nearestRoadToBus = getNearestRoad(
                            busStopLocation.coordinates,
                            FeatureTree(roadFeatureCollectionTest)
                        )

                        val busStopCallout = buildString {
                            append("Bus Stop")
                            append(". ")
                            append("${distanceToBusStop.toInt()} metres")
                            append(". ")
                            if (nearestRoadToBus!!.properties?.get("name") != null){
                                append(nearestRoadToBus.properties?.get("name"))
                            }
                        }
                        results.add(busStopCallout)
                    }

                } else {

                    results.add(
                        "There is nothing to call out right now"
                    )
                }
                println("Where am I standing? Point ${feature.properties?.get("id")}")
                for (result in results) {
                    println(result)
                }
            }
        }
    }


    private fun generateFOVTriangle(
        userGeometry: UserGeometry
    ): Feature {

        val triangle = getFovTriangle(userGeometry)

        // We can now construct our FOV polygon (triangle)
        val polygonTriangleFOV = createPolygonFromTriangle(triangle)

        val featureFOVTriangle = Feature().also {
            val ars3: HashMap<String, Any?> = HashMap()
            ars3 += Pair("FoV", "35 meters")
            it.properties = ars3
        }
        featureFOVTriangle.geometry = polygonTriangleFOV
        return featureFOVTriangle
    }

    // Putting the two functions below as the real app code uses context to get locale and I don't want
    // to use instrumented tests to get to context as they run quite slowly.
    fun getCompassLabelFacingDirectionAlong(degrees: Int, placeholder: String): String {
        val directionString: String = when (degrees) {
            in 338..360, in 0..22 -> "Facing north along $placeholder"
            in 23..67 -> "Facing northeast along $placeholder"
            in 68..112 -> "Facing east along $placeholder"
            in 113..157 -> "Facing southeast along $placeholder"
            in 158..202 -> "Facing south along $placeholder"
            in 203..247 -> "Facing southwest along $placeholder"
            in 248..292 -> "Facing west along $placeholder"
            in 293..337 -> "Facing northwest along $placeholder"
            else -> 0.toString()
        }

        return directionString
    }

    private fun getRelativeDirectionLabelStreetPreview(relativeDirection: Int): String {
        return when (relativeDirection) {
            0 -> "behind"
            1 -> "behind to the left"
            2 -> "to the left"
            3 -> "ahead to the left"
            4 -> "ahead"
            5 -> "ahead to the right"
            6 -> "to the right"
            7 -> "behind to the right"
            else -> "Unknown"
        }
    }

}

