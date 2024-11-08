package org.scottishtecharmy.soundscape

import com.squareup.moshi.Moshi
import org.junit.Assert
import org.junit.Test
import org.scottishtecharmy.soundscape.dto.BoundingBox
import org.scottishtecharmy.soundscape.dto.Tile
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeoMoshi
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.geojsonparser.moshi.GeoJsonObjectMoshiAdapter
import org.scottishtecharmy.soundscape.utils.RelativeDirections
import org.scottishtecharmy.soundscape.utils.TileGrid
import org.scottishtecharmy.soundscape.utils.TileGrid.Companion.ZOOM_LEVEL
import org.scottishtecharmy.soundscape.utils.bearingFromTwoPoints
import org.scottishtecharmy.soundscape.utils.checkIntersection
import org.scottishtecharmy.soundscape.utils.createTriangleFOV
import org.scottishtecharmy.soundscape.utils.distance
import org.scottishtecharmy.soundscape.utils.getBusStopsFeatureCollectionFromTileFeatureCollection
import org.scottishtecharmy.soundscape.utils.getCrossingsFromTileFeatureCollection
import org.scottishtecharmy.soundscape.utils.getDestinationCoordinate
import org.scottishtecharmy.soundscape.utils.getFovIntersectionFeatureCollection
import org.scottishtecharmy.soundscape.utils.getFovRoadsFeatureCollection
import org.scottishtecharmy.soundscape.utils.getIntersectionRoadNames
import org.scottishtecharmy.soundscape.utils.getIntersectionRoadNamesRelativeDirections
import org.scottishtecharmy.soundscape.utils.getIntersectionsFeatureCollectionFromTileFeatureCollection
import org.scottishtecharmy.soundscape.utils.getNearestIntersection
import org.scottishtecharmy.soundscape.utils.getNearestRoad
import org.scottishtecharmy.soundscape.utils.getPathsFeatureCollectionFromTileFeatureCollection
import org.scottishtecharmy.soundscape.utils.getQuadKey
import org.scottishtecharmy.soundscape.utils.getQuadrants
import org.scottishtecharmy.soundscape.utils.getRelativeDirectionsPolygons
import org.scottishtecharmy.soundscape.utils.getRoadBearingToIntersection
import org.scottishtecharmy.soundscape.utils.getRoadsFeatureCollectionFromTileFeatureCollection
import org.scottishtecharmy.soundscape.utils.getXYTile
import org.scottishtecharmy.soundscape.utils.mapSize
import org.scottishtecharmy.soundscape.utils.pixelXYToLatLon
import org.scottishtecharmy.soundscape.utils.sortedByDistanceTo
import org.scottishtecharmy.soundscape.utils.traceLineString
import org.scottishtecharmy.soundscape.utils.vectorTileToGeoJson
import vector_tile.VectorTile
import java.io.FileInputStream
import java.io.FileOutputStream

class GenerateGeoJsonFromMvt {
    // Theses aren't really tests I'm just looking at the GeoJSON that is being generated from the MvtTiles
    // rather than the old Soundscape backend.
    @Test
    fun generateGeoJsonFromMvt1Test(){
        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        // tricky location 55.941929,-4.316524
        val location = LngLatAlt(-4.316524, 55.941929)
        // mvt tile so zoom at 15
        val slippyTileName = getXYTile(location.latitude, location.longitude, 15)
        // output wget
        // wget https://d1wzlzgah5gfol.cloudfront.net/protomaps/15/{x}/{y}.pbf -O {x}x{y}.mvt
        // wget https://d1wzlzgah5gfol.cloudfront.net/protomaps/15/15991/10213.pbf -O 15991x10213.mvt
        //println("wget https://d1wzlzgah5gfol.cloudfront.net/protomaps/15/${slippyTileName.first}/${slippyTileName.second}.pbf -O ${slippyTileName.first}x${slippyTileName.second}.mvt")
        val geoJson = vectorTileToGeoJsonFromFile(slippyTileName.first, slippyTileName.second, "15991x10213.mvt")

        val adapter = GeoJsonObjectMoshiAdapter()
        val outputFile = FileOutputStream("${slippyTileName.first}x${slippyTileName.second}.geojson")
        outputFile.write(adapter.toJson(geoJson).toByteArray())
        outputFile.close()

        // get the roads from the feature collection
        val testRoadsCollectionFromTileFeatureCollection =
            getRoadsFeatureCollectionFromTileFeatureCollection(geoJson)

        //val roadString = moshi.adapter(FeatureCollection::class.java).toJson(testRoadsCollectionFromTileFeatureCollection)
        // visual check of roads
        //println("Roads: $roadString")

        // get the paths from the feature collection
        val testPathsCollectionFromTileFeatureCollection =
            getPathsFeatureCollectionFromTileFeatureCollection(geoJson)
        //val pathString = moshi.adapter(FeatureCollection::class.java).toJson(testPathsCollectionFromTileFeatureCollection)
        // visual check of paths
        //println("Paths: $pathString")

        // get the intersections from the feature collection
        val testIntersectionsCollectionFromTileFeatureCollection =
            getIntersectionsFeatureCollectionFromTileFeatureCollection(geoJson)
        //val intersectionString = moshi.adapter(FeatureCollection::class.java).toJson(testIntersectionsCollectionFromTileFeatureCollection)
        // visual check of intersections
        //println("Intersections: $intersectionString")

        // merge roads and intersections
        val roadAndIntersectionFeatureCollection = FeatureCollection()
        roadAndIntersectionFeatureCollection.features.addAll(testRoadsCollectionFromTileFeatureCollection.features)
        roadAndIntersectionFeatureCollection.features.addAll(testIntersectionsCollectionFromTileFeatureCollection.features)
        //val roadAndIntersectionString = moshi.adapter(FeatureCollection::class.java).toJson(roadAndIntersectionFeatureCollection)
        // visual check of roads and intersections
        //println("Roads and intersections: $roadAndIntersectionString")

        // merge paths and intersections
        val pathAndIntersectionFeatureCollection = FeatureCollection()
        pathAndIntersectionFeatureCollection.features.addAll(testPathsCollectionFromTileFeatureCollection.features)
        pathAndIntersectionFeatureCollection.features.addAll(testIntersectionsCollectionFromTileFeatureCollection.features)
        //val pathAndIntersectionString = moshi.adapter(FeatureCollection::class.java).toJson(pathAndIntersectionFeatureCollection)
        // visual check of paths and intersections
        //println("Paths and intersections: $pathAndIntersectionString")

        // ***** Check simple intersection detection ****
        // fake location, device direction standing on Buchanan Street with an intersection
        // ahead ~13 metres. Left at the intersection is Grange Avenue. Ahead is Buchanan Street
        val currentLocation = LngLatAlt(-4.313349391621443,55.94257732192659)
        val deviceHeading = 0.0
        val fovDistance = 50.0

        // create FOV to pick up the roads
        val fovRoadsFeatureCollection = getFovRoadsFeatureCollection(
            currentLocation,
            deviceHeading,
            fovDistance,
            testRoadsCollectionFromTileFeatureCollection
        )
        // create FOV to pick up the intersections
        val fovIntersectionsFeatureCollection = getFovIntersectionFeatureCollection(
            currentLocation,
            deviceHeading,
            fovDistance,
            testIntersectionsCollectionFromTileFeatureCollection
        )
        // get the nearest intersection in the FoV and the roads that make up the intersection
        val testNearestIntersection = getNearestIntersection(
            currentLocation,fovIntersectionsFeatureCollection)

        val testNearestRoad = getNearestRoad(currentLocation, fovRoadsFeatureCollection)

        val testNearestRoadBearing = getRoadBearingToIntersection(testNearestIntersection, testNearestRoad, deviceHeading)

        val testIntersectionRoadNames = getIntersectionRoadNames(
            testNearestIntersection, fovRoadsFeatureCollection)
        // first create a relative direction polygon and put it on the intersection node with the same
        // heading as the road we are currently nearest to
        val intersectionLocation = testNearestIntersection.features[0].geometry as Point

        val intersectionRelativeDirections = getRelativeDirectionsPolygons(
            LngLatAlt(intersectionLocation.coordinates.longitude, intersectionLocation.coordinates.latitude),
            testNearestRoadBearing,
            fovDistance,
            RelativeDirections.COMBINED
        )

        // pass the roads that make up the intersection, the intersection and the relative directions polygons
        // this should give us a feature collection with the roads and their relative direction
        // inserted as a "Direction" property for each Road feature that makes up the intersection
        val roadRelativeDirections = getIntersectionRoadNamesRelativeDirections(
            testIntersectionRoadNames,
            testNearestIntersection,
            intersectionRelativeDirections)

        Assert.assertEquals(3, roadRelativeDirections.features.size)
        // Road we are on Buchanan Street
        Assert.assertEquals(0, roadRelativeDirections.features[0].properties!!["Direction"])
        Assert.assertEquals("Buchanan Street", roadRelativeDirections.features[0].properties!!["name"])
        // Road on the left at the intersection Grange Avenue
        Assert.assertEquals(2, roadRelativeDirections.features[1].properties!!["Direction"])
        Assert.assertEquals("Grange Avenue", roadRelativeDirections.features[1].properties!!["name"])
        // Road continuing on ahead from intersection
        Assert.assertEquals(4, roadRelativeDirections.features[2].properties!!["Direction"])
        Assert.assertEquals("Buchanan Street", roadRelativeDirections.features[2].properties!!["name"])

    }

    @Test
    fun streetPreviewMvtTest1(){
        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        // Road/LineString trace and walk along it. No Callouts
        val location = LngLatAlt(-4.309524074865749,55.94175394950997)
        val slippyTileName = getXYTile(location.latitude, location.longitude, 15)
        // output wget
        // wget https://d1wzlzgah5gfol.cloudfront.net/protomaps/15/{x}/{y}.pbf -O {x}x{y}.mvt
        // wget https://d1wzlzgah5gfol.cloudfront.net/protomaps/15/15991/10213.pbf -O 15991x10213.mvt
        //println("wget https://d1wzlzgah5gfol.cloudfront.net/protomaps/15/${slippyTileName.first}/${slippyTileName.second}.pbf -O ${slippyTileName.first}x${slippyTileName.second}.mvt")
        val geoJson = vectorTileToGeoJsonFromFile(slippyTileName.first, slippyTileName.second, "15991x10213.mvt")
        // get the roads from the feature collection
        val testRoadsCollectionFromTileFeatureCollection =
            getRoadsFeatureCollectionFromTileFeatureCollection(geoJson)

        val nearestRoadTest = getNearestRoad(
            LngLatAlt(-4.309524074865749,
                55.94175394950997),
            testRoadsCollectionFromTileFeatureCollection
        )
        val nearestRoadString = moshi.adapter(FeatureCollection::class.java).toJson(nearestRoadTest)
        // copy and paste into GeoJSON.io
        println("Nearest road/linestring $nearestRoadString")
        // trace along the road with equidistant points
        val roadTrace = traceLineString(nearestRoadTest, 20.0)
        val roadTraceString = moshi.adapter(FeatureCollection::class.java).toJson(roadTrace)
        // copy and paste into GeoJSON.io
        println("Road trace: $roadTraceString")
        val fovFeatureCollection = FeatureCollection()
        var i = 2
        // walk down the road using the Points from the roadTrace FeatureCollection as a track
        for (feature in roadTrace.features.subList(1, roadTrace.features.size - 1)) {
            val currentPoint = feature.geometry as Point
            val currentTraceLocation = LngLatAlt(
                currentPoint.coordinates.longitude,
                currentPoint.coordinates.latitude
            )
            val nextLocation = roadTrace.features[i++].geometry as Point
            // fake the device heading by "looking" at the next Point
            val deviceHeadingTrace = bearingFromTwoPoints(
                currentTraceLocation.latitude,
                currentTraceLocation.longitude,
                nextLocation.coordinates.latitude,
                nextLocation.coordinates.longitude
            )
            println("Device Heading: $deviceHeadingTrace")
            val fovTriangle = generateFOVTriangle(currentTraceLocation, deviceHeadingTrace)
            fovFeatureCollection.addFeature(fovTriangle)
        }
        val fovFeatureCollectionString = moshi.adapter(FeatureCollection::class.java).toJson(fovFeatureCollection)
        // copy and paste into GeoJSON.io
        println("FoV triangles for road trace: $fovFeatureCollectionString")
    }

    @Test
    fun streetPreviewMvtTest2() {
        // Print the callouts to the console for a walk down a road.
        // This will only detect road and intersections
        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        // Location -4.309524074865749, 55.94175394950997 Baldernock Road
        val location = LngLatAlt(-4.309524074865749,
            55.94175394950997)
        // mvt tile so zoom at 15
        val slippyTileName = getXYTile(location.latitude, location.longitude, 15)
        // output wget
        // wget https://d1wzlzgah5gfol.cloudfront.net/protomaps/15/{x}/{y}.pbf -O {x}x{y}.mvt
        // wget https://d1wzlzgah5gfol.cloudfront.net/protomaps/15/15991/10213.pbf -O 15991x10213.mvt
        //println("wget https://d1wzlzgah5gfol.cloudfront.net/protomaps/15/${slippyTileName.first}/${slippyTileName.second}.pbf -O ${slippyTileName.first}x${slippyTileName.second}.mvt")
        val geoJson = vectorTileToGeoJsonFromFile(slippyTileName.first, slippyTileName.second, "15991x10213.mvt")
        // get the roads from the feature collection
        val testRoadsCollectionFromTileFeatureCollection =
            getRoadsFeatureCollectionFromTileFeatureCollection(geoJson)
        // get the intersections from the feature collection
        val testIntersectionsCollectionFromTileFeatureCollection =
            getIntersectionsFeatureCollectionFromTileFeatureCollection(geoJson)
        // Road/LineString trace and walk along it
        val nearestRoadTest = getNearestRoad(
            LngLatAlt(location.longitude, location.latitude),
            testRoadsCollectionFromTileFeatureCollection
        )
        //val nearestRoadString = moshi.adapter(FeatureCollection::class.java).toJson(nearestRoadTest)
        // trace along the road with equidistant points 30m apart.
        val roadTrace = traceLineString(nearestRoadTest, 20.0)
        var i = 2
        // walk down the road using the Points from the roadTrace FeatureCollection as a track
        for (feature in roadTrace.features.subList(1, roadTrace.features.size - 1)) {
            // Hold the text for the callouts:
            val results : MutableList<String> = mutableListOf()

            val currentPoint = feature.geometry as Point
            val currentLocation = LngLatAlt(
                currentPoint.coordinates.longitude,
                currentPoint.coordinates.latitude
            )
            val nextLocation = roadTrace.features[i++].geometry as Point
            // fake the device heading by "looking" at the next Point
            val deviceHeading = bearingFromTwoPoints(
                currentLocation.latitude,
                currentLocation.longitude,
                nextLocation.coordinates.latitude,
                nextLocation.coordinates.longitude
            )
            val fovDistance = 50.0
            if (testRoadsCollectionFromTileFeatureCollection.features.isNotEmpty()) {
                val fovRoadsFeatureCollection = getFovRoadsFeatureCollection(
                    LngLatAlt(
                        currentLocation.longitude,
                        currentLocation.latitude
                    ),
                    deviceHeading,
                    fovDistance,
                    testRoadsCollectionFromTileFeatureCollection
                )
                val fovIntersectionsFeatureCollection =
                    getFovIntersectionFeatureCollection(
                        LngLatAlt(
                            currentLocation.longitude,
                            currentLocation.latitude
                        ),
                        deviceHeading,
                        fovDistance,
                        testIntersectionsCollectionFromTileFeatureCollection
                    )


                if (fovRoadsFeatureCollection.features.isNotEmpty()) {
                    val nearestRoad = getNearestRoad(
                        LngLatAlt(
                            currentLocation.longitude,
                            currentLocation.latitude
                        ),
                        fovRoadsFeatureCollection
                    )

                    if (nearestRoad.features[0].properties?.get("name") != null) {
                        results.add(
                            "Ahead ${nearestRoad.features[0].properties!!["name"]}"
                        )
                    } else {
                        // we are detecting an unnamed road here but pretending there is nothing here
                        results.add(
                            "There is nothing to call out right now"
                        )
                    }

                    if (fovIntersectionsFeatureCollection.features.isNotEmpty()) {

                        val intersectionsSortedByDistance = sortedByDistanceTo(
                            currentLocation.latitude,
                            currentLocation.longitude,
                            fovIntersectionsFeatureCollection
                        )

                        val testNearestRoad = getNearestRoad(
                            LngLatAlt(
                                currentLocation.longitude,
                                currentLocation.latitude
                            ),
                            fovRoadsFeatureCollection
                        )
                        val intersectionsNeedsFurtherCheckingFC = FeatureCollection()

                        for (y in 0 until intersectionsSortedByDistance.features.size) {
                            val testNearestIntersection = FeatureCollection()
                            testNearestIntersection.addFeature(intersectionsSortedByDistance.features[y])
                            val intersectionRoadNames = getIntersectionRoadNames(testNearestIntersection, fovRoadsFeatureCollection)
                            val intersectionsNeedsFurtherChecking =
                                checkIntersection(y, intersectionRoadNames, testNearestRoad)
                            if(intersectionsNeedsFurtherChecking) {
                                intersectionsNeedsFurtherCheckingFC.addFeature(intersectionsSortedByDistance.features[y])
                            }
                        }
                        if (intersectionsNeedsFurtherCheckingFC.features.size > 0) {
                            // Approach 1: find the intersection feature with the most osm_ids and use that?
                            val featureWithMostOsmIds: Feature? = intersectionsNeedsFurtherCheckingFC.features.maxByOrNull { intersectionFeature ->
                                (intersectionFeature.foreign?.get("osm_ids") as? List<*>)?.size ?: 0
                            }
                            val newIntersectionFeatureCollection = FeatureCollection()
                            if (featureWithMostOsmIds != null) {
                                newIntersectionFeatureCollection.addFeature(featureWithMostOsmIds)
                            }

                            val nearestIntersection = getNearestIntersection(
                                LngLatAlt(currentLocation.longitude,
                                    currentLocation.latitude),
                                fovIntersectionsFeatureCollection
                            )
                            val nearestRoadBearing = getRoadBearingToIntersection(nearestIntersection, testNearestRoad, deviceHeading)
                            val intersectionLocation = newIntersectionFeatureCollection.features[0].geometry as Point
                            val intersectionRelativeDirections = getRelativeDirectionsPolygons(
                                LngLatAlt(intersectionLocation.coordinates.longitude,
                                    intersectionLocation.coordinates.latitude),
                                nearestRoadBearing,
                                //fovDistance,
                                5.0,
                                RelativeDirections.COMBINED
                            )
                            val distanceToNearestIntersection = distance(
                                currentLocation.latitude,
                                currentLocation.longitude,
                                intersectionLocation.coordinates.latitude,
                                intersectionLocation.coordinates.longitude
                            )
                            val intersectionRoadNames = getIntersectionRoadNames(newIntersectionFeatureCollection, fovRoadsFeatureCollection)
                            results.add(
                                "Approaching intersection ${distanceToNearestIntersection.toInt()} metres"
                            )

                            val roadRelativeDirections = getIntersectionRoadNamesRelativeDirections(
                                intersectionRoadNames,
                                newIntersectionFeatureCollection,
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

    @Test
    fun streetPreviewMvtTest3(){
        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        //This contains the merged data from a 2x2 grid as the road spans them
        val featureCollectionTest = moshi.adapter(FeatureCollection::class.java)
            .fromJson(GeoJSON2x2gridMvt.geoJSON2x2gridMvt)
        // Pull out the data layers that we would need for Ahead Of Me
        val roadFeatureCollectionTest = featureCollectionTest?.let {
            getRoadsFeatureCollectionFromTileFeatureCollection(
                it
            )
        }
        val intersectionsFeatureCollectionTest = featureCollectionTest?.let {
            getIntersectionsFeatureCollectionFromTileFeatureCollection(
                it
            )
        }
        val crossingsFeatureCollectionTest = featureCollectionTest?.let {
            getCrossingsFromTileFeatureCollection(
                it
            )
        }
        val busStopsGridFeatureCollection = featureCollectionTest?.let {
            getBusStopsFeatureCollectionFromTileFeatureCollection(
                it
            )
        }
        val nearestRoadTest = roadFeatureCollectionTest?.let {
            getNearestRoad(
                LngLatAlt(-2.693002695425122,51.43938442591545),
                it
            )
        }
        // trace along the road with equidistant points 30m apart.
        val roadTrace = nearestRoadTest?.let { traceLineString(it, 30.0) }

        var i = 2
        // walk down the road using the Points from the roadTrace FeatureCollection as a track
        if (roadTrace != null) {
            // I'm not including the start of the road and jumping to second point (Point 2) and then
            // looping through until second to last point (Point 14)
            for (feature in roadTrace.features.subList(1, roadTrace.features.size - 1)) {
                // Hold the text for the callouts:
                val results : MutableList<String> = mutableListOf()

                val currentPoint = feature.geometry as Point
                val currentLocation = LngLatAlt(
                    currentPoint.coordinates.longitude,
                    currentPoint.coordinates.latitude
                )
                val nextLocation = roadTrace.features[i++].geometry as Point
                // fake the device heading by "looking" at the next Point
                val deviceHeading = bearingFromTwoPoints(
                    currentLocation.latitude,
                    currentLocation.longitude,
                    nextLocation.coordinates.latitude,
                    nextLocation.coordinates.longitude
                )
                val fovDistance = 50.0
                if (roadFeatureCollectionTest.features.size > 0) {
                    val fovRoadsFeatureCollection = getFovRoadsFeatureCollection(
                        LngLatAlt(
                            currentLocation.longitude,
                            currentLocation.latitude
                        ),
                        deviceHeading,
                        fovDistance,
                        roadFeatureCollectionTest
                    )
                    val fovIntersectionsFeatureCollection = intersectionsFeatureCollectionTest?.let {
                        getFovIntersectionFeatureCollection(
                            LngLatAlt(
                                currentLocation.longitude,
                                currentLocation.latitude
                            ),
                            deviceHeading,
                            fovDistance,
                            it
                        )
                    }
                    val fovCrossingsFeatureCollection = crossingsFeatureCollectionTest?.let {
                        getFovIntersectionFeatureCollection(
                            LngLatAlt(
                                currentLocation.longitude,
                                currentLocation.latitude
                            ),
                            deviceHeading,
                            fovDistance,
                            it
                        )
                    }
                    val fovBusStopsFeatureCollection = busStopsGridFeatureCollection?.let {
                        getFovIntersectionFeatureCollection(
                            LngLatAlt(
                                currentLocation.longitude,
                                currentLocation.latitude
                            ),
                            deviceHeading,
                            fovDistance,
                            it
                        )
                    }

                    if (fovRoadsFeatureCollection.features.size > 0) {
                        val nearestRoad = getNearestRoad(
                            LngLatAlt(
                                currentLocation.longitude,
                                currentLocation.latitude
                            ),
                            fovRoadsFeatureCollection
                        )

                        if (nearestRoad.features[0].properties?.get("name") != null) {
                            results.add(
                                "Ahead ${nearestRoad.features[0].properties!!["name"]}"
                            )
                        } else {
                            // we are detecting an unnamed road here but pretending there is nothing here
                            results.add(
                                "There is nothing to call out right now"
                            )
                        }

                        if (fovIntersectionsFeatureCollection != null) {
                            if (fovIntersectionsFeatureCollection.features.size > 0) {

                                val intersectionsSortedByDistance = sortedByDistanceTo(
                                    currentLocation.latitude,
                                    currentLocation.longitude,
                                    fovIntersectionsFeatureCollection
                                )

                                val testNearestRoad = getNearestRoad(
                                    LngLatAlt(
                                        currentLocation.longitude,
                                        currentLocation.latitude
                                    ),
                                    fovRoadsFeatureCollection
                                )
                                val intersectionsNeedsFurtherCheckingFC = FeatureCollection()

                                for (y in 0 until intersectionsSortedByDistance.features.size) {
                                    val testNearestIntersection = FeatureCollection()
                                    testNearestIntersection.addFeature(intersectionsSortedByDistance.features[y])
                                    val intersectionRoadNames = getIntersectionRoadNames(testNearestIntersection, fovRoadsFeatureCollection)
                                    val intersectionsNeedsFurtherChecking = checkIntersection(y, intersectionRoadNames, testNearestRoad)
                                    if(intersectionsNeedsFurtherChecking) {
                                        intersectionsNeedsFurtherCheckingFC.addFeature(intersectionsSortedByDistance.features[y])
                                    }
                                }
                                if (intersectionsNeedsFurtherCheckingFC.features.size > 0) {
                                    // Approach 1: find the intersection feature with the most osm_ids and use that?
                                    val featureWithMostOsmIds: Feature? = intersectionsNeedsFurtherCheckingFC.features.maxByOrNull { intersectionFeature ->
                                        (intersectionFeature.foreign?.get("osm_ids") as? List<*>)?.size ?: 0
                                    }
                                    val newIntersectionFeatureCollection = FeatureCollection()
                                    if (featureWithMostOsmIds != null) {
                                        newIntersectionFeatureCollection.addFeature(featureWithMostOsmIds)
                                    }

                                    val nearestIntersection = getNearestIntersection(
                                        LngLatAlt(currentLocation.longitude,
                                            currentLocation.latitude),
                                        fovIntersectionsFeatureCollection
                                    )
                                    val nearestRoadBearing = getRoadBearingToIntersection(nearestIntersection, testNearestRoad, deviceHeading)
                                    val intersectionLocation = newIntersectionFeatureCollection.features[0].geometry as Point
                                    val intersectionRelativeDirections = getRelativeDirectionsPolygons(
                                        LngLatAlt(intersectionLocation.coordinates.longitude,
                                            intersectionLocation.coordinates.latitude),
                                        nearestRoadBearing,
                                        //fovDistance,
                                        5.0,
                                        RelativeDirections.COMBINED
                                    )
                                    val distanceToNearestIntersection = distance(
                                        currentLocation.latitude,
                                        currentLocation.longitude,
                                        intersectionLocation.coordinates.latitude,
                                        intersectionLocation.coordinates.longitude
                                    )
                                    val intersectionRoadNames = getIntersectionRoadNames(newIntersectionFeatureCollection, fovRoadsFeatureCollection)
                                    results.add(
                                        "Approaching intersection ${distanceToNearestIntersection.toInt()} metres"
                                    )

                                    val roadRelativeDirections = getIntersectionRoadNamesRelativeDirections(
                                        intersectionRoadNames,
                                        newIntersectionFeatureCollection,
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
                    }
                    // detect if there is a crossing in the FOV
                    if (fovCrossingsFeatureCollection != null) {
                        if (fovCrossingsFeatureCollection.features.size > 0) {

                            val nearestCrossing = getNearestIntersection(
                                LngLatAlt(
                                    currentLocation.longitude,
                                    currentLocation.latitude
                                ),
                                fovCrossingsFeatureCollection
                            )
                            val crossingLocation = nearestCrossing.features[0].geometry as Point
                            val distanceToCrossing = distance(
                                currentLocation.latitude,
                                currentLocation.longitude,
                                crossingLocation.coordinates.latitude,
                                crossingLocation.coordinates.longitude
                            )
                            // Confirm which road the crossing is on
                            val nearestRoadToCrossing = getNearestRoad(
                                LngLatAlt(
                                    crossingLocation.coordinates.longitude,
                                    crossingLocation.coordinates.latitude
                                ),
                                fovRoadsFeatureCollection
                            )

                            val crossingCallout = buildString {
                                append("Crossing")
                                append(". ")
                                append("${distanceToCrossing.toInt()} metres")
                                append(". ")
                                if (nearestRoadToCrossing.features[0].properties?.get("name") != null){
                                    append(nearestRoadToCrossing.features[0].properties?.get("name"))
                                }
                            }
                            results.add(crossingCallout)
                        }
                    }

                    // detect if there is a bus_stop in the FOV
                    if (fovBusStopsFeatureCollection != null) {
                        if (fovBusStopsFeatureCollection.features.size > 0) {
                            val nearestBusStop = getNearestIntersection(
                                LngLatAlt(
                                    currentLocation.longitude,
                                    currentLocation.latitude
                                ),
                                fovBusStopsFeatureCollection
                            )
                            val busStopLocation = nearestBusStop.features[0].geometry as Point
                            val distanceToBusStop = distance(
                                currentLocation.latitude,
                                currentLocation.latitude,
                                busStopLocation.coordinates.latitude,
                                busStopLocation.coordinates.longitude
                            )
                            // Confirm which road the crossing is on
                            val nearestRoadToBus = getNearestRoad(
                                LngLatAlt(
                                    busStopLocation.coordinates.longitude,
                                    busStopLocation.coordinates.latitude
                                ),
                                fovRoadsFeatureCollection
                            )

                            val busStopCallout = buildString {
                                append("Bus Stop")
                                append(". ")
                                append("${distanceToBusStop.toInt()} metres")
                                append(". ")
                                if (nearestRoadToBus.features[0].properties?.get("name") != null){
                                    append(nearestRoadToBus.features[0].properties?.get("name"))
                                }
                            }
                            results.add(busStopCallout)
                        }
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

    @Test
    fun getTileGridTest() {
        val currentLocation = LngLatAlt(-2.6929845426577117,51.43947396619484)
        val tileGrid = get2x2TileGrid(currentLocation.latitude, currentLocation.longitude, 15)
        Assert.assertEquals(4, tileGrid.tiles.size)
        for (tile in tileGrid.tiles) {
            println("wget https://d1wzlzgah5gfol.cloudfront.net/protomaps/15/${tile.tileX}/${tile.tileY}.pbf -O ${tile.tileX}x${tile.tileY}.mvt")
        }
    }

    @Test
    fun mergeTilesInto2x2Grid(){
        // Make a 2x2 grid and merge them into a single FeatureCollection
        val featureCollection = FeatureCollection()
        for(x in 16138..16139) {
            for (y in 10905..10906) {
                val geojson = vectorTileToGeoJsonFromFile(x, y, "${x}x${y}.mvt")
                for(feature in geojson) {
                    featureCollection.addFeature(feature)
                }
            }
        }

        val adapter = GeoJsonObjectMoshiAdapter()
        val outputFile = FileOutputStream("16138x10906.geojson")
        outputFile.write(adapter.toJson(featureCollection).toByteArray())
        outputFile.close()
    }

    private fun get2x2TileGrid(
        currentLatitude: Double = 0.0,
        currentLongitude: Double = 0.0,
        zoomLevel: Int = 15
    ): TileGrid {

        // Get tile that contains current location
        val tileXY = getXYTile(currentLatitude, currentLongitude, zoomLevel)
        // Scale up the tile xy
        val scaledTile = Pair(tileXY.first * 2, tileXY.second * 2)

        // And the quadrant within that tile
        val tileQuadrant = getXYTile(currentLatitude, currentLongitude, zoomLevel + 1)

        // The center of the grid is the corner of the tile that is shared with the quadrant that the
        // location is within.
        val maxCoordinate = mapSize(zoomLevel) / 256
        val xValues = IntArray(2)
        val yValues = IntArray(2)
        if (tileQuadrant.first == scaledTile.first) {
            if (tileQuadrant.second == scaledTile.second) {
                // Top left quadrant as the coordinates match
                xValues[0] = (tileXY.first - 1).mod(maxCoordinate)
                xValues[1] = tileXY.first
                yValues[0] = (tileXY.second - 1).mod(maxCoordinate)
                yValues[1] = tileXY.second
            } else {
                // Bottom left quadrant as the x coordinate matches
                xValues[0] = (tileXY.first - 1).mod(maxCoordinate)
                xValues[1] = tileXY.first
                yValues[0] = tileXY.second
                yValues[1] = (tileXY.second + 1).mod(maxCoordinate)
            }
        } else {
            if (tileQuadrant.second == scaledTile.second) {
                // Top right quadrant as only the y coordinates match
                xValues[0] = tileXY.first
                xValues[1] = (tileXY.first + 1).mod(maxCoordinate)
                yValues[0] = (tileXY.second - 1).mod(maxCoordinate)
                yValues[1] = tileXY.second
            } else {
                // Bottom right quadrant as neither coordinate matches
                xValues[0] = tileXY.first
                xValues[1] = (tileXY.first + 1).mod(maxCoordinate)
                yValues[0] = tileXY.second
                yValues[1] = (tileXY.second + 1).mod(maxCoordinate)
            }
        }

        // Center of grid is the top left corner of the bottom right tile
        val centerX = (xValues[1] * 256)
        val centerY = (yValues[1] * 256)
        val southWest = pixelXYToLatLon((centerX - 160).toDouble(), (centerY + 160).toDouble(), zoomLevel)
        val northEast = pixelXYToLatLon((centerX + 160).toDouble(), (centerY - 160).toDouble(), zoomLevel)
        val centralBoundingBox = BoundingBox(southWest.second,
            southWest.first,
            northEast.second,
            northEast.first)

        val tiles: MutableList<Tile> = mutableListOf()
        for (y in yValues) {
            for (x in xValues) {
                val surroundingTile = Tile("", x, y, zoomLevel)
                surroundingTile.quadkey = getQuadKey(x, y, zoomLevel)
                tiles.add(surroundingTile)
            }
        }
        return TileGrid(tiles, centralBoundingBox)
    }

    private fun vectorTileToGeoJsonFromFile(
        tileX: Int,
        tileY: Int,
        filename: String,
        cropPoints: Boolean = true
    ): FeatureCollection {

        val path = "src/test/res/org/scottishtecharmy/soundscape/"
        val remoteTile = FileInputStream(path + filename)
        val tile: VectorTile.Tile = VectorTile.Tile.parseFrom(remoteTile)

        val featureCollection = vectorTileToGeoJson(tileX, tileY, tile, cropPoints, 15)

        return featureCollection
    }

    private fun generateFOVTriangle(
        currentLocation: LngLatAlt,
        deviceHeading: Double,
        fovDistance: Double = 50.0
    ): Feature {
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
            ars3 += Pair("FoV", "blah")
            it.properties = ars3
        }
        featureFOVTriangle.geometry = polygonTriangleFOV
        return featureFOVTriangle
    }

    // Putting the two functions below as the real app code uses context to get locale and I don't want
    // to use instrumented tests to get to context as they run quite slowly on my rubbish laptop.
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

    fun getRelativeDirectionLabelStreetPreview(relativeDirection: Int): String {
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