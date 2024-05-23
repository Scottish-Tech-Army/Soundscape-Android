package com.kersnazzle.soundscapealpha

import com.kersnazzle.soundscapealpha.geojsonparser.geojson.FeatureCollection
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.GeoMoshi
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.LineString
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.LngLatAlt
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.Point
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.Polygon
import com.kersnazzle.soundscapealpha.utils.RelativeDirections
import com.kersnazzle.soundscapealpha.utils.getFovIntersectionFeatureCollection
import com.kersnazzle.soundscapealpha.utils.getFovRoadsFeatureCollection
import com.kersnazzle.soundscapealpha.utils.getIntersectionRoadNames
import com.kersnazzle.soundscapealpha.utils.getIntersectionsFeatureCollectionFromTileFeatureCollection
import com.kersnazzle.soundscapealpha.utils.getNearestIntersection
import com.kersnazzle.soundscapealpha.utils.getReferenceCoordinate
import com.kersnazzle.soundscapealpha.utils.getRelativeDirectionsPolygons
import com.kersnazzle.soundscapealpha.utils.getRoadsFeatureCollectionFromTileFeatureCollection
import com.kersnazzle.soundscapealpha.utils.polygonContainsCoordinates
import com.squareup.moshi.Moshi
import org.junit.Test

 /**
 * These aren't really tests at this point just figuring our how to handle various intersection types.
 */
class IntersectionsTest {
    @Test
    fun intersectionsStraightAheadType(){
        // Fake device location and pretend the device is pointing East.
        // -2.6577997643930757, 51.43041390383118
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
        val testNearestIntersection = getNearestIntersection(currentLocation,fovIntersectionsFeatureCollection)
        val testIntersectionRoadNames = getIntersectionRoadNames(testNearestIntersection, fovRoadsFeatureCollection)
        // what relative direction(s) are the road(s) that make up the nearest intersection?

        // first create a relative direction polygon and put it on the intersection node with the same
        // heading as the device
        val intersectionLocation = testNearestIntersection.features[0].geometry as Point
        val relativeDirections = getRelativeDirectionsPolygons(
            LngLatAlt(intersectionLocation.coordinates.longitude, intersectionLocation.coordinates.latitude),
            deviceHeading,
            fovDistance,
            RelativeDirections.COMBINED
        )

        // this should be clockwise from 6 o'clock
        // so the first road will be the road we are on (direction 0) - Weston Road
        // the second road which makes up the intersection is ahead left (direction 3) etc. Long Ashton Road
        for (direction in relativeDirections){
            for (road in testIntersectionRoadNames) {
                val testReferenceCoordinateForward = getReferenceCoordinate(
                    road.geometry as LineString, 25.0, false)
                val iAmHere1 = polygonContainsCoordinates(
                    testReferenceCoordinateForward, (direction.geometry as Polygon))
                if (iAmHere1){
                    println("Road name: ${road.properties!!["name"]}")
                    println("Road direction: ${direction.properties!!["Direction"]}")
                }
                // reverse the LineString, create the ref coordinate and test it again
                val testReferenceCoordinateReverse = getReferenceCoordinate(
                    road.geometry as LineString, 25.0, true
                )
                val iAmHere2 = polygonContainsCoordinates(testReferenceCoordinateReverse, (direction.geometry as Polygon))
                if (iAmHere2){
                    println("Road name: ${road.properties!!["name"]}")
                    println("Road direction: ${direction.properties!!["Direction"]}")
                }
            }
        }
    }

    @Test
    fun intersectionsRightTurn(){

    }
}