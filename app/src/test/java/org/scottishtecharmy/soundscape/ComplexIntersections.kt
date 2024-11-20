package org.scottishtecharmy.soundscape

import com.squareup.moshi.Moshi
import org.junit.Assert
import org.junit.Test
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeoMoshi
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.geoengine.utils.RelativeDirections
import org.scottishtecharmy.soundscape.geoengine.utils.checkIntersection
import org.scottishtecharmy.soundscape.geoengine.utils.getFovIntersectionFeatureCollection
import org.scottishtecharmy.soundscape.geoengine.utils.getFovRoadsFeatureCollection
import org.scottishtecharmy.soundscape.geoengine.utils.getIntersectionRoadNames
import org.scottishtecharmy.soundscape.geoengine.utils.getIntersectionRoadNamesRelativeDirections
import org.scottishtecharmy.soundscape.geoengine.utils.getIntersectionsFeatureCollectionFromTileFeatureCollection
import org.scottishtecharmy.soundscape.geoengine.utils.getNearestIntersection
import org.scottishtecharmy.soundscape.geoengine.utils.getNearestRoad
import org.scottishtecharmy.soundscape.geoengine.utils.getRelativeDirectionsPolygons
import org.scottishtecharmy.soundscape.geoengine.utils.getRoadBearingToIntersection
import org.scottishtecharmy.soundscape.geoengine.utils.getRoadsFeatureCollectionFromTileFeatureCollection
import org.scottishtecharmy.soundscape.geoengine.utils.sortedByDistanceTo

class ComplexIntersections {

    @Test
    fun complexIntersections1Test(){
        //https://geojson.io/#map=18.61/51.4439294/-2.6974316
        // this is probably the simplest example of a complex intersection where we have
        // a road that splits into two one way roads before joining another road. There are also
        // multiple gd_intersections detected in the FoV so we need to determine which ones to ignore
        // and which ones are useful to call out to the user
        // Fake location, heading and Field of View for testing
        val currentLocation = LngLatAlt(-2.697291022799874,51.44378095087524)
        val deviceHeading = 320.0
        val fovDistance = 50.0

        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val featureCollectionTest = moshi.adapter(FeatureCollection::class.java)
            .fromJson(GeoJSONDataComplexIntersection1.complexintersection1GeoJSON)

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
        // Create a FOV triangle to pick up the intersections
        val fovIntersectionsFeatureCollection = getFovIntersectionFeatureCollection(
            currentLocation,
            deviceHeading,
            fovDistance,
            testIntersectionsCollectionFromTileFeatureCollection
        )

        val roadsFOVString =
            moshi.adapter(FeatureCollection::class.java).toJson(fovRoadsFeatureCollection)
        println("Roads in FOV: $roadsFOVString")
        val intersectionsFOVString =
            moshi.adapter(FeatureCollection::class.java).toJson(fovIntersectionsFeatureCollection)
        println("Intersections in FOV: $intersectionsFOVString")

        // I will need a feature collection of all the intersections in the FOV sorted by distance to the current location
        val intersectionsSortedByDistance = sortedByDistanceTo(
            currentLocation.latitude,
            currentLocation.longitude,
            fovIntersectionsFeatureCollection
        )
        val intersectionsSortedByDistanceString =
            moshi.adapter(FeatureCollection::class.java).toJson(intersectionsSortedByDistance)
        println("Intersections in FOV sorted by distance: $intersectionsSortedByDistanceString")
        println("Number of intersections in FOV: ${intersectionsSortedByDistance.features.size}")

        val testNearestRoad = getNearestRoad(currentLocation, fovRoadsFeatureCollection)
        val intersectionsNeedsFurtherCheckingFC = FeatureCollection()

        for (i in 0 until intersectionsSortedByDistance.features.size) {
            val testNearestIntersection = FeatureCollection()
            testNearestIntersection.addFeature(intersectionsSortedByDistance.features[i])
            val intersectionRoadNames = getIntersectionRoadNames(testNearestIntersection, fovRoadsFeatureCollection)
            val intersectionsNeedsFurtherChecking = checkIntersection(i, intersectionRoadNames, testNearestRoad)
            if(intersectionsNeedsFurtherChecking) {
                intersectionsNeedsFurtherCheckingFC.addFeature(intersectionsSortedByDistance.features[i])
            }
        }
        println("Intersections that need further checking: ${intersectionsNeedsFurtherCheckingFC.features.size}")

        // Approach 1: find the intersection feature with the most osm_ids and use that?
        // The code doesn't give us the correct result as it jumps to the wrong/next intersection
        /*val featureWithMostOsmIds: Feature? = intersectionsNeedsFurtherCheckingFC.features.maxByOrNull {
                feature ->
            (feature.foreign?.get("osm_ids") as? List<*>)?.size ?: 0
        }
        val newIntersectionFeatureCollection = FeatureCollection()
        if (featureWithMostOsmIds != null) {
            newIntersectionFeatureCollection.addFeature(featureWithMostOsmIds)
        }*/

        // Approach 2: Use the nearest "checked" intersection to the device location?
        val intersectionsToCheckSortedByDistance = sortedByDistanceTo(
            currentLocation.latitude,
            currentLocation.longitude,
            intersectionsNeedsFurtherCheckingFC
        )


        val nearestIntersection = getNearestIntersection(currentLocation, fovIntersectionsFeatureCollection)
        val nearestRoadBearing = getRoadBearingToIntersection(nearestIntersection, testNearestRoad, deviceHeading)
        val intersectionLocation = intersectionsToCheckSortedByDistance.features[0].geometry as Point
        val intersectionRelativeDirections = getRelativeDirectionsPolygons(
            LngLatAlt(intersectionLocation.coordinates.longitude,
                intersectionLocation.coordinates.latitude),
            nearestRoadBearing,
            //fovDistance,
            5.0,
            RelativeDirections.COMBINED
        )
        val relativeDirectionsString =
            moshi.adapter(FeatureCollection::class.java).toJson(intersectionRelativeDirections)
        println("relative directions polygons: $relativeDirectionsString")

        val newIntersectionFeatureCollection = FeatureCollection()
        newIntersectionFeatureCollection.addFeature(intersectionsToCheckSortedByDistance.features[0])
        val intersectionThatWeAreChecking =
            moshi.adapter(FeatureCollection::class.java).toJson(newIntersectionFeatureCollection)
        println("Intersection we are checking: $intersectionThatWeAreChecking")


        val intersectionRoadNames = getIntersectionRoadNames(newIntersectionFeatureCollection, fovRoadsFeatureCollection)
        val intersectionRoadNamesString =
            moshi.adapter(FeatureCollection::class.java).toJson(intersectionRoadNames)
        println("Intersection roads: $intersectionRoadNamesString")
        val roadRelativeDirections = getIntersectionRoadNamesRelativeDirections(
            intersectionRoadNames,
            newIntersectionFeatureCollection,
            intersectionRelativeDirections
        )

        Assert.assertEquals(3, roadRelativeDirections.features.size )

        Assert.assertEquals(0, roadRelativeDirections.features[0].properties!!["Direction"])
        Assert.assertEquals("Flax Bourton Road", roadRelativeDirections.features[0].properties!!["name"])
        Assert.assertEquals(3, roadRelativeDirections.features[1].properties!!["Direction"])
        Assert.assertEquals("Clevedon Road", roadRelativeDirections.features[1].properties!!["name"])
        Assert.assertEquals(7, roadRelativeDirections.features[2].properties!!["Direction"])
        Assert.assertEquals("Clevedon Road", roadRelativeDirections.features[2].properties!!["name"])

    }

    @Test
    fun complexIntersections2Test() {
        // This is a more complex junction than the above test.
        // There are multiple gd_intersections detected in the FoV so we need to determine which ones to ignore
        // and which ones are useful to call out to the user
        // https://geojson.io/#map=18.65/51.4405486/-2.6851813
        // Fake location, heading and Field of View for testing
        val currentLocation = LngLatAlt(-2.6854420947740323, 51.44036284885249)
        val deviceHeading = 45.0
        val fovDistance = 50.0

        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val featureCollectionTest = moshi.adapter(FeatureCollection::class.java)
            .fromJson(GeoJSONDataComplexIntersection.complexIntersectionGeoJSON)

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
        // Create a FOV triangle to pick up the intersections
        val fovIntersectionsFeatureCollection = getFovIntersectionFeatureCollection(
            currentLocation,
            deviceHeading,
            fovDistance,
            testIntersectionsCollectionFromTileFeatureCollection
        )

        val roadsFOVString =
            moshi.adapter(FeatureCollection::class.java).toJson(fovRoadsFeatureCollection)
        println("Roads in FOV: $roadsFOVString")
        val intersectionsFOVString =
            moshi.adapter(FeatureCollection::class.java).toJson(fovIntersectionsFeatureCollection)
        println("Intersections in FOV: $intersectionsFOVString")
        // TODO A lot more processing to make sense of the data above to enable a callout to the user...

        // I will need a feature collection of all the intersections in the FOV sorted by distance to the current location
        val intersectionsSortedByDistance = sortedByDistanceTo(
            currentLocation.latitude,
            currentLocation.longitude,
            fovIntersectionsFeatureCollection
        )
        val intersectionsSortedByDistanceString =
            moshi.adapter(FeatureCollection::class.java).toJson(intersectionsSortedByDistance)
        println("Intersections in FOV sorted by distance: $intersectionsSortedByDistanceString")
        println("Number of intersections in FOV: ${intersectionsSortedByDistance.features.size}")

        val testNearestRoad = getNearestRoad(currentLocation, fovRoadsFeatureCollection)
        val intersectionsNeedsFurtherCheckingFC = FeatureCollection()

        for (i in 0 until intersectionsSortedByDistance.features.size) {
            val testNearestIntersection = FeatureCollection()
            testNearestIntersection.addFeature(intersectionsSortedByDistance.features[i])
            val intersectionRoadNames = getIntersectionRoadNames(testNearestIntersection, fovRoadsFeatureCollection)
            val intersectionsNeedsFurtherChecking = checkIntersection(i, intersectionRoadNames, testNearestRoad)
            if(intersectionsNeedsFurtherChecking) {
                intersectionsNeedsFurtherCheckingFC.addFeature(intersectionsSortedByDistance.features[i])
            }
        }
        println("Intersections that need further checking: ${intersectionsNeedsFurtherCheckingFC.features.size}")

        // Approach 1: find the intersection feature with the most osm_ids and use that?
        // The code does give us the correct result as it jumps to the intersection with the most osm_ids
        val featureWithMostOsmIds: Feature? = intersectionsNeedsFurtherCheckingFC.features.maxByOrNull {
                feature ->
            (feature.foreign?.get("osm_ids") as? List<*>)?.size ?: 0
        }
        val newIntersectionFeatureCollection = FeatureCollection()
        if (featureWithMostOsmIds != null) {
            newIntersectionFeatureCollection.addFeature(featureWithMostOsmIds)
        }

        // Approach 2: Use the nearest "checked" intersection to the device location?
        /*val intersectionsToCheckSortedByDistance = sortedByDistanceTo(
            currentLocation.latitude,
            currentLocation.longitude,
            intersectionsNeedsFurtherCheckingFC
        )*/
        val nearestIntersection = getNearestIntersection(currentLocation, fovIntersectionsFeatureCollection)
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
        //val newIntersectionFeatureCollection = FeatureCollection()
        //newIntersectionFeatureCollection.addFeature(intersectionsToCheckSortedByDistance.features[0])

        val intersectionRoadNames = getIntersectionRoadNames(newIntersectionFeatureCollection, fovRoadsFeatureCollection)
        val intersectionRoadNamesString =
            moshi.adapter(FeatureCollection::class.java).toJson(intersectionRoadNames)
        println("Intersection roads: $intersectionRoadNamesString")
        val roadRelativeDirections = getIntersectionRoadNamesRelativeDirections(
            intersectionRoadNames,
            newIntersectionFeatureCollection,
            intersectionRelativeDirections
        )

        val intersectionRelativeDirectionsString =
            moshi.adapter(FeatureCollection::class.java).toJson(intersectionRelativeDirections)
        println("Intersection relative directions polygons: $intersectionRelativeDirectionsString")

        Assert.assertEquals(4, roadRelativeDirections.features.size )
        //
        Assert.assertEquals(1, roadRelativeDirections.features[0].properties!!["Direction"])
        Assert.assertEquals("Clevedon Road", roadRelativeDirections.features[0].properties!!["name"])
        Assert.assertEquals(3, roadRelativeDirections.features[1].properties!!["Direction"])
        Assert.assertEquals("Beggar Bush Lane", roadRelativeDirections.features[1].properties!!["name"])
        Assert.assertEquals(5, roadRelativeDirections.features[2].properties!!["Direction"])
        Assert.assertEquals("Clevedon Road", roadRelativeDirections.features[2].properties!!["name"])
        Assert.assertEquals(7, roadRelativeDirections.features[3].properties!!["Direction"])
        Assert.assertEquals("Weston Road", roadRelativeDirections.features[3].properties!!["name"])

    }


}