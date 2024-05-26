package com.kersnazzle.soundscapealpha

import com.kersnazzle.soundscapealpha.geojsonparser.geojson.FeatureCollection
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.GeoMoshi
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.LineString
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.LngLatAlt
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.Point
import com.kersnazzle.soundscapealpha.geojsonparser.geojson.Polygon
import com.kersnazzle.soundscapealpha.utils.RelativeDirections
import com.kersnazzle.soundscapealpha.utils.RoadDirectionAtIntersection
import com.kersnazzle.soundscapealpha.utils.bearingFromTwoPoints
import com.kersnazzle.soundscapealpha.utils.getDirectionAtIntersection
import com.kersnazzle.soundscapealpha.utils.getFovIntersectionFeatureCollection
import com.kersnazzle.soundscapealpha.utils.getFovRoadsFeatureCollection
import com.kersnazzle.soundscapealpha.utils.getIntersectionRoadNames
import com.kersnazzle.soundscapealpha.utils.getIntersectionsFeatureCollectionFromTileFeatureCollection
import com.kersnazzle.soundscapealpha.utils.getNearestIntersection
import com.kersnazzle.soundscapealpha.utils.getNearestRoad
import com.kersnazzle.soundscapealpha.utils.getReferenceCoordinate
import com.kersnazzle.soundscapealpha.utils.getRelativeDirectionsPolygons
import com.kersnazzle.soundscapealpha.utils.getRoadBearingToIntersection
import com.kersnazzle.soundscapealpha.utils.getRoadsFeatureCollectionFromTileFeatureCollection
import com.kersnazzle.soundscapealpha.utils.polygonContainsCoordinates
import com.kersnazzle.soundscapealpha.utils.splitRoadByIntersection
import com.squareup.moshi.Moshi
import org.junit.Test

 /**
 * These aren't really tests. At this point just figuring our how to handle various
 * simple intersection types.
 */

 // TODO Calculate the bearing of the road that we are currently on in relation to the intersection
 //  and use that rather than device bearing for the relative directions.

 //-----------------------------------------------//
 // Intersection Types - from original Soundscape //
 //----------------------------------------------//

//  Road Switch
//
//  | ↑ |
//  | B |
//  |   |
//  | ↑ |
//  | * |
//  |   |
//  | A |
//  | ↓ |

 // Weston Road to Long Ashton Road
 // https://geojson.io/#map=17/51.430494/-2.657463

//  Turn Right
//   _____________
//  |          B →
//  | ↑  _________
//  | * |
//  |   |
//  | A |
//  | ↓ |

 // Belgrave Place to Codrington Place
//https://geojson.io/#map=19/51.4579043/-2.6156923

//  Turn Left
//  _____________
//  ← B          |
//  _________  ↑ |
//           | * |
//           |   |
//           | A |
//           | ↓ |

 // same again just depends what road you are standing on
 // Codrington Place to Belgrave Place
 //https://geojson.io/#map=19/51.4579382/-2.6157338

//  Side Road Right
//
//  | ↑ |
//  | A |
//  |   |_________
//  |          B →
//  | ↑  _________
//  | * |
//  |   |
//  | A |
//  | ↓ |
//
 // Long Ashton Road and St Martins
// https://geojson.io/#map=18/51.430741/-2.656311
//

//  Side Road Left
//
//           | ↑ |
//           | A |
//  _________|   |
//  ← B          |
//  _________  ↑ |
//           | * |
//           |   |
//           | A |
//           | ↓ |

 // Long Ashton Road and St Martins same as above but location and device direction changed
// https://geojson.io/#map=18/51.430741/-2.656311
//

//  T1
//  ___________________
//  ← B             B →
//  _______     _______
//         | ↑ |
//         | * |
//         |   |
//         | A |
//         | ↓ |

// Standing on St Martins with device pointing towards Long Ashton Road
 // https://geojson.io/#map=18/51.430741/-2.656311

//  T2
//  ___________________
//  ← B             C →
//  _______     _______
//         | ↑ |
//         | * |
//         |   |
//         | A |
//         | ↓ |

 // standing on Goodeve Road with device pointing towards SeaWalls Road (Left) and Knoll Hill (Right)
 // https://geojson.io/#map=18/51.472469/-2.637757

//  Cross1
//         | ↑ |
//         | A |
//  _______|   |_______
//  ← B             B →
//  _______     _______
//         | ↑ |
//         | * |
//         |   |
//         | A |
//         | ↓ |

 // Standing on Grange Road which continues on ahead and the left and right are Manilla Road
// https://geojson.io/#map=18/51.4569979/-2.6185285

//  Cross2
//         | ↑ |
//         | A |
//  _______|   |_______
//  ← B             C →
//  _______     _______
//         | ↑ |
//         | * |
//         |   |
//         | A |
//         | ↓ |

 // Standing on Lansdown Road which continues on ahead. Left is Manilla Road and Right is Vyvyan Road
// https://geojson.io/#map=18/51.4571879/-2.6178348/-31.2/14

//  Cross3
//         | ↑ |
//         | D |
//  _______|   |_______
//  ← B             C →
//  _______     _______
//         | ↑ |
//         | * |
//         |   |
//         | A |
//         | ↓ |
//
// Example: Standing on St Mary's Butts with Oxford Road on Left, West Street Ahead and Broad Street on Right
// https://geojson.io/#map=18/51.455426/-0.975279/-25.6
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
        // what road are we nearest to?
        val testNearestRoad = getNearestRoad(currentLocation, fovRoadsFeatureCollection)
        // what is the road direction type in relation to the nearest intersection and nearest road


        // first create a relative direction polygon and put it on the intersection node with the same
        // heading as the device
        val intersectionLocation = testNearestIntersection.features[0].geometry as Point
        val relativeDirectionsAtIntersection = getRelativeDirectionsPolygons(
            LngLatAlt(intersectionLocation.coordinates.longitude, intersectionLocation.coordinates.latitude),
            deviceHeading,
            fovDistance,
            RelativeDirections.COMBINED
        )
        val relativeDirectionsAtCurrentLocation = getRelativeDirectionsPolygons(
            currentLocation,
            deviceHeading,
            fovDistance,
            RelativeDirections.COMBINED
        )
        // where's the intersection relative to our current location - direction 4 - ahead
        for(direction in relativeDirectionsAtCurrentLocation ) {
            val intersectionHere = polygonContainsCoordinates(
                LngLatAlt(intersectionLocation.coordinates.longitude, intersectionLocation.coordinates.latitude),
                (direction.geometry as Polygon)
            )
            if (intersectionHere){
                println("Intersection direction: ${direction.properties!!["Direction"]}")
            }
        }

        // The relative directions begin at 6 o'clock and are clockwise
        // the road we are on should have a relative direction of 0
        // (relative direction 0 at the intersection) - Weston Road
        // the other road which makes up the intersection is ahead left
        // (relative direction 3 at the intersection) - Long Ashton Road
        for (direction in relativeDirectionsAtIntersection){
            for (road in testIntersectionRoadNames) {
                val testReferenceCoordinateForward = getReferenceCoordinate(
                    road.geometry as LineString, 25.0, false)
                val iAmHere1 = polygonContainsCoordinates(
                    testReferenceCoordinateForward, (direction.geometry as Polygon))
                // LineString Coordinates don't have a set "first" and "last" so the
                // reference coordinate we created above could be at the other "end" of the road
                // TODO change getReferenceCoordinate to accept the intersection coordinate so we know the "first"
                if (iAmHere1){
                    println("Road name: ${road.properties!!["name"]}")
                    println("Road direction: ${direction.properties!!["Direction"]}")
                } else {
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
    }

    @Test
    fun intersectionsRightTurn(){
        // Fake device location and pretend the device is pointing South West and we are located on:
        // Belgrave Place
        val currentLocation = LngLatAlt(-2.615585745757045,51.457957257918395)
        val deviceHeading = 225.0 // South West
        val fovDistance = 50.0

        val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        val featureCollectionTest = moshi.adapter(FeatureCollection::class.java)
            .fromJson(GeoJsonIntersectionRightAndLeftTurn.intersectionRightAndLeftTurn)
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
        // Create a FOV triangle to pick up the intersection (this intersection is
        // a right turn transition from Belgrave Place to Codrington Place)
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
        // so the first road will be the road we are on (direction 0) - Belgrave PLace
        // the second road which makes up the intersection is right (direction 6) etc. Codrington Place
        for (direction in relativeDirections){
            for (road in testIntersectionRoadNames) {
                val testReferenceCoordinateForward = getReferenceCoordinate(
                    road.geometry as LineString, 25.0, false)
                val iAmHere1 = polygonContainsCoordinates(
                    testReferenceCoordinateForward, (direction.geometry as Polygon))
                if (iAmHere1){
                    println("Road name: ${road.properties!!["name"]}")
                    println("Road direction: ${direction.properties!!["Direction"]}")
                } else {
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
    }

     @Test
     fun intersectionsLeftTurn(){
         // Fake device location and pretend the device is pointing South East and we are standing on:
         // Codrington Place
         val currentLocation = LngLatAlt(-2.6159411752634583, 51.45799104056931)
         val deviceHeading = 135.0 // South East
         val fovDistance = 50.0

         val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
         val featureCollectionTest = moshi.adapter(FeatureCollection::class.java)
             .fromJson(GeoJsonIntersectionRightAndLeftTurn.intersectionRightAndLeftTurn)
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
         // Create a FOV triangle to pick up the intersection (this intersection is
         // a left turn transition from Codrington Place to Belgrave Place to)
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
         // so the first road will be the road we are on (direction 0) - Codrington Place
         // the second road which makes up the intersection is left (direction 2) etc. Belgrave Place
         for (direction in relativeDirections){
             for (road in testIntersectionRoadNames) {
                 val testReferenceCoordinateForward = getReferenceCoordinate(
                     road.geometry as LineString, 25.0, false)
                 val iAmHere1 = polygonContainsCoordinates(
                     testReferenceCoordinateForward, (direction.geometry as Polygon))
                 if (iAmHere1){
                     println("Road name: ${road.properties!!["name"]}")
                     println("Road direction: ${direction.properties!!["Direction"]}")
                 } else {
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

     }

     @Test
     fun intersectionsSideRoadRight(){

         // Fake device location and pretend the device is pointing South West and we are located on:
         // Long Ashton Road
         val currentLocation = LngLatAlt(-2.656109007812404,51.43079699441145)
         val deviceHeading = 250.0 // South West
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
         // Create a FOV triangle to pick up the intersection (this intersection is
         // a side road right from Long Ashton Road to St Martins)
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
         // so the first road will be the road we are on but it continues on from
         // the intersection so (direction 4) - Long Ashton Road
         // the second road which makes up the intersection is right (direction 6) - St Martins
         for (direction in relativeDirections){
             for (road in testIntersectionRoadNames) {
                 val testReferenceCoordinateForward = getReferenceCoordinate(
                     road.geometry as LineString, 25.0, false)
                 val iAmHere1 = polygonContainsCoordinates(
                     testReferenceCoordinateForward, (direction.geometry as Polygon))
                 if (iAmHere1){
                     println("Road name: ${road.properties!!["name"]}")
                     println("Road direction: ${direction.properties!!["Direction"]}")
                 } else {
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
     }

     @Test
     fun intersectionsSideRoadLeft(){
         // Fake device location and pretend the device is pointing South West and we are located on:
         // Long Ashton Road
         val currentLocation = LngLatAlt(-2.656530323429564,51.43065207103919)
         val deviceHeading = 50.0 // North East
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
         // Create a FOV triangle to pick up the intersection (this intersection is
         // a side road Left from Long Ashton Road to St Martins)
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
         // so the first road will be the road we are on but it continues on from
         // the intersection so (direction 4) - Long Ashton Road
         // the second road which makes up the intersection is left (direction 2) - St Martins
         for (direction in relativeDirections){
             for (road in testIntersectionRoadNames) {
                 val testReferenceCoordinateForward = getReferenceCoordinate(
                     road.geometry as LineString, 25.0, false)
                 val iAmHere1 = polygonContainsCoordinates(
                     testReferenceCoordinateForward, (direction.geometry as Polygon))
                 if (iAmHere1){
                     println("Road name: ${road.properties!!["name"]}")
                     println("Road direction: ${direction.properties!!["Direction"]}")
                 } else {
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

     }

     @Test
     fun intersectionsT1Test(){

         // Fake device location and pretend the device is pointing South West and we are located on:
         // St Martins
         val currentLocation = LngLatAlt(-2.656540700657672,51.430978147982785)
         val deviceHeading = 140.0 // South East
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
         // Create a FOV triangle to pick up the intersection (this intersection is
         // a T junction with the device on St Martins and the main road is Long Ashton)
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
         val intersectionRelativeDirections = getRelativeDirectionsPolygons(
             LngLatAlt(intersectionLocation.coordinates.longitude, intersectionLocation.coordinates.latitude),
             deviceHeading,
             fovDistance,
             RelativeDirections.COMBINED
         )

         // the second road which makes up the intersection is left and right (direction 2 and direction 6) - Long Ashton Road
         // However our getReferenceCoordinate function only works with the ends of LineStrings so what to do?
         // Check the type of road: leading, trailing or leading_and_trailing. If the road is leading_and_trailing - DONE
         // split the LineString coordinates into two based on intersection coordinate, - DONE
         // create two new LineStrings. DONE
         // insert a ref coordinate into both and work out the directions for the two LineStrings - DONE
         // so we should be able to return Long Ashton Road "Left" and Long Ashton Road "Right" DONE
         // TODO Have a think about the below as it is making me sad looking at it!

         for (road in testIntersectionRoadNames){
             val testRoadDirectionAtIntersection = getDirectionAtIntersection(testNearestIntersection.features[0], road)
             println("Road name: ${road.properties!!["name"]} and $testRoadDirectionAtIntersection")
             if (testRoadDirectionAtIntersection == RoadDirectionAtIntersection.LEADING_AND_TRAILING) {
                 // split the road into two
                 val roadCoordinatesSplitIntoTwo = splitRoadByIntersection(testNearestIntersection.features[0], road)
                 // for each split road work out the relative direction from the intersection
                 for (splitRoad in roadCoordinatesSplitIntoTwo) {
                     val testReferenceCoordinateForRoad = getReferenceCoordinate(
                         splitRoad.geometry as LineString, 25.0, false)
                     // test if the reference coordinate we've created is in any of the relative direction triangles
                     for(direction in intersectionRelativeDirections){
                         val iAmHere1 = polygonContainsCoordinates(
                             testReferenceCoordinateForRoad, (direction.geometry as Polygon))
                         if (iAmHere1){
                             println("Road name: ${splitRoad.properties!!["name"]}")
                             println("Road direction: ${direction.properties!!["Direction"]}")
                         } else {
                             // reverse the LineString, create the ref coordinate and test it again
                             val testReferenceCoordinateReverse = getReferenceCoordinate(
                                 splitRoad.geometry as LineString, 25.0, true
                             )
                             val iAmHere2 = polygonContainsCoordinates(
                                 testReferenceCoordinateReverse, (direction.geometry as Polygon)
                             )
                             if (iAmHere2) {
                                 println("Road name: ${splitRoad.properties!!["name"]}")
                                 println("Road direction: ${direction.properties!!["Direction"]}")
                             }
                         }
                     }
                 }
             }
         }
     }

     @Test
     fun intersectionsT2Test(){
         // Fake device location and pretend the device is pointing South West and we are located on:
         // Goodeve Road  The Left is Seawalls Road and Right is Knoll Hill
         val currentLocation = LngLatAlt(-2.637514213827643, 51.472589063821175)
         val deviceHeading = 225.0 // South West
         val fovDistance = 50.0

         val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
         val featureCollectionTest = moshi.adapter(FeatureCollection::class.java)
             .fromJson(GeoJsonIntersectionT2.intersectionT2FeatureCollection)
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
         // Create a FOV triangle to pick up the intersection (this intersection is
         // a T junction and we are located on Goodeve Road.
         // The Left is Seawalls Road and Right is Knoll Hill
         val fovIntersectionsFeatureCollection = getFovIntersectionFeatureCollection(
             currentLocation,
             deviceHeading,
             fovDistance,
             testIntersectionsCollectionFromTileFeatureCollection
         )
         // get the nearest intersection in the FoV and the roads that make up the intersection
         val testNearestIntersection = getNearestIntersection(
             currentLocation,fovIntersectionsFeatureCollection)
         val testIntersectionRoadNames = getIntersectionRoadNames(
             testNearestIntersection, fovRoadsFeatureCollection)
         // first create a relative direction polygon and put it on the intersection node with the same
         // heading as the device
         val intersectionLocation = testNearestIntersection.features[0].geometry as Point
         val intersectionRelativeDirections = getRelativeDirectionsPolygons(
             LngLatAlt(intersectionLocation.coordinates.longitude, intersectionLocation.coordinates.latitude),
             deviceHeading,
             fovDistance,
             RelativeDirections.COMBINED
         )

         for (road in testIntersectionRoadNames) {
             val testRoadDirectionAtIntersection =
                 getDirectionAtIntersection(testNearestIntersection.features[0], road)
             println("Road name: ${road.properties!!["name"]} and $testRoadDirectionAtIntersection")
             if (testRoadDirectionAtIntersection != RoadDirectionAtIntersection.LEADING_AND_TRAILING){
                 for (direction in intersectionRelativeDirections){
                         val testReferenceCoordinateForward = getReferenceCoordinate(
                             road.geometry as LineString, 25.0, false)
                         val iAmHere1 = polygonContainsCoordinates(
                             testReferenceCoordinateForward, (direction.geometry as Polygon))
                         if (iAmHere1){
                             println("Road name: ${road.properties!!["name"]}")
                             println("Road direction: ${direction.properties!!["Direction"]}")
                         } else {
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
         }
     }

     @Test
     fun intersectionsCross1Test(){

         // Fake device location and pretend the device is pointing North West and we are located on:
         // Grange Road  The Left and Right for the crossroad is Manilla Road and ahead is Grange Road
         val currentLocation = LngLatAlt(-2.61850147329568, 51.456953686378085)
         val deviceHeading = 340.0 // North North West
         val fovDistance = 50.0

         val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
         val featureCollectionTest = moshi.adapter(FeatureCollection::class.java)
             .fromJson(GeoJsonIntersectionCross1.intersectionCross1FeatureCollection)

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
         // Create a FOV triangle to pick up the intersection. This intersection is
         // a crossroads and we are located on Grange Road (direction 0) which continues on ahead (direction 4)
         // from the intersection. Manilla Road is left (direction 2) and right (direction 6)
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

         val testNearestRoadBearing = getRoadBearingToIntersection(testNearestIntersection, testNearestRoad)

         val testIntersectionRoadNames = getIntersectionRoadNames(
             testNearestIntersection, fovRoadsFeatureCollection)
         // first create a relative direction polygon and put it on the intersection node with the same
         // heading as the device
         val intersectionLocation = testNearestIntersection.features[0].geometry as Point
         val intersectionRelativeDirections = getRelativeDirectionsPolygons(
             LngLatAlt(intersectionLocation.coordinates.longitude, intersectionLocation.coordinates.latitude),
             testNearestRoadBearing,
             fovDistance,
             RelativeDirections.COMBINED
         )

         for (road in testIntersectionRoadNames) {
             val testRoadDirectionAtIntersection =
                 getDirectionAtIntersection(testNearestIntersection.features[0], road)
             println("Road name: ${road.properties!!["name"]} and $testRoadDirectionAtIntersection")
             if (testRoadDirectionAtIntersection == RoadDirectionAtIntersection.LEADING_AND_TRAILING){
                 // split the road into two
                 val roadCoordinatesSplitIntoTwo = splitRoadByIntersection(testNearestIntersection.features[0], road)
                 // for each split road work out the relative direction from the intersection
                 for (splitRoad in roadCoordinatesSplitIntoTwo) {
                     val testReferenceCoordinateForRoad = getReferenceCoordinate(
                         splitRoad.geometry as LineString, 25.0, false)
                     // test if the reference coordinate we've created is in any of the relative direction triangles
                     for(direction in intersectionRelativeDirections){
                         val iAmHere1 = polygonContainsCoordinates(
                             testReferenceCoordinateForRoad, (direction.geometry as Polygon))
                         if (iAmHere1){
                             println("Road name: ${splitRoad.properties!!["name"]}")
                             println("Road direction: ${direction.properties!!["Direction"]}")
                         } else {
                             // reverse the LineString, create the ref coordinate and test it again
                             val testReferenceCoordinateReverse = getReferenceCoordinate(
                                 splitRoad.geometry as LineString, 25.0, true
                             )
                             val iAmHere2 = polygonContainsCoordinates(
                                 testReferenceCoordinateReverse, (direction.geometry as Polygon)
                             )
                             if (iAmHere2) {
                                 println("Road name: ${splitRoad.properties!!["name"]}")
                                 println("Road direction: ${direction.properties!!["Direction"]}")
                             }
                         }
                     }
                 }
             }
         }
     }

     @Test
     fun intersectionCross2Test(){
         // NOTE: This is a strange crossroads because it is actually made up of four different LineStrings
         // 2 x Lansdown Road LineStrings (would expect it to be made out of one)
         // and 1 Linestring  Manilla Road and 1 LineString Vyvyan Road

         // Fake device location and pretend the device is pointing North West and we are located on:
         // Lansdown Road and it continues on straight ahead.  The Left is Manilla Road and Right is Vyvyan Road
         val currentLocation = LngLatAlt(-2.6176822011131833, 51.457104175295484)
         val deviceHeading = 340.0 // North West
         val fovDistance = 50.0

         val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
         val featureCollectionTest = moshi.adapter(FeatureCollection::class.java)
             .fromJson(GeoJsonIntersectionCross1.intersectionCross1FeatureCollection)

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
         // Create a FOV triangle to pick up the intersection. This intersection is
         // a crossroads and we are located on Grange Road (direction 0) which continues on ahead (direction 4)
         // from the intersection. Manilla Road is left (direction 2) and Vyvyan Road is right (direction 6)
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

         val testNearestRoadBearing = getRoadBearingToIntersection(testNearestIntersection, testNearestRoad)

         val testIntersectionRoadNames = getIntersectionRoadNames(
             testNearestIntersection, fovRoadsFeatureCollection)
         // first create a relative direction polygon and put it on the intersection node with the same
         // heading as the device
         val intersectionLocation = testNearestIntersection.features[0].geometry as Point
         val intersectionRelativeDirections = getRelativeDirectionsPolygons(
             LngLatAlt(intersectionLocation.coordinates.longitude, intersectionLocation.coordinates.latitude),
             testNearestRoadBearing,
             fovDistance,
             RelativeDirections.COMBINED
         )

         for (road in testIntersectionRoadNames) {
             val testRoadDirectionAtIntersection =
                 getDirectionAtIntersection(testNearestIntersection.features[0], road)
             println("Road name: ${road.properties!!["name"]} and $testRoadDirectionAtIntersection")
             if (testRoadDirectionAtIntersection == RoadDirectionAtIntersection.LEADING_AND_TRAILING){
                 // split the road into two
                 val roadCoordinatesSplitIntoTwo = splitRoadByIntersection(testNearestIntersection.features[0], road)
                 // for each split road work out the relative direction from the intersection
                 for (splitRoad in roadCoordinatesSplitIntoTwo) {
                     val testReferenceCoordinateForRoad = getReferenceCoordinate(
                         splitRoad.geometry as LineString, 25.0, false)
                     // test if the reference coordinate we've created is in any of the relative direction triangles
                     for(direction in intersectionRelativeDirections){
                         val iAmHere1 = polygonContainsCoordinates(
                             testReferenceCoordinateForRoad, (direction.geometry as Polygon))
                         if (iAmHere1){
                             println("Road name: ${splitRoad.properties!!["name"]}")
                             println("Road direction: ${direction.properties!!["Direction"]}")
                         } else {
                             // reverse the LineString, create the ref coordinate and test it again
                             val testReferenceCoordinateReverse = getReferenceCoordinate(
                                 splitRoad.geometry as LineString, 25.0, true
                             )
                             val iAmHere2 = polygonContainsCoordinates(
                                 testReferenceCoordinateReverse, (direction.geometry as Polygon)
                             )
                             if (iAmHere2) {
                                 println("Road name: ${splitRoad.properties!!["name"]}")
                                 println("Road direction: ${direction.properties!!["Direction"]}")
                             }
                         }
                     }
                 }
             }
             else{
                 for (direction in intersectionRelativeDirections){
                     val testReferenceCoordinateForward = getReferenceCoordinate(
                             road.geometry as LineString, 25.0, false)
                     val iAmHere1 = polygonContainsCoordinates(
                             testReferenceCoordinateForward, (direction.geometry as Polygon))
                     if (iAmHere1){
                             println("Road name: ${road.properties!!["name"]}")
                             println("Road direction: ${direction.properties!!["Direction"]}")
                         } else {
                             // reverse the LineString, create the ref coordinate and test it again
                             val testReferenceCoordinateReverse = getReferenceCoordinate(
                                 road.geometry as LineString, 25.0, true
                             )
                             val iAmHere2 = polygonContainsCoordinates(
                                 testReferenceCoordinateReverse,
                                 (direction.geometry as Polygon)
                             )
                             if (iAmHere2) {
                                 println("Road name: ${road.properties!!["name"]}")
                                 println("Road direction: ${direction.properties!!["Direction"]}")
                             }
                         }
                 }
             }

         }
     }

     @Test
     fun intersectionsCross3Test(){
         // Fake device location and pretend the device is pointing North West and we are located on:
         //  Standing on St Mary's Butts with Oxford Road on Left, West Street Ahead and Broad Street on Right
         val currentLocation = LngLatAlt(-0.9752549546655587, 51.4553843453491)
         val deviceHeading = 320.0 // North West
         val fovDistance = 50.0

         val moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
         val featureCollectionTest = moshi.adapter(FeatureCollection::class.java)
             .fromJson(GeoJsonIntersectionCross3.intersectionCross3FeatureCollection)

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
         // Create a FOV triangle to pick up the intersection. This intersection is
         // a crossroad type 3 and we are located on St Mary's Butts (direction 0)
         // Oxford Road is Left (direction 2) West Street is ahead (direction 4)
         // and Broad Street is Right (direction 6)
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

         val testNearestRoadBearing = getRoadBearingToIntersection(testNearestIntersection, testNearestRoad)

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

         for (road in testIntersectionRoadNames) {
             val testRoadDirectionAtIntersection =
                 getDirectionAtIntersection(testNearestIntersection.features[0], road)
             println("Road name: ${road.properties!!["name"]} and $testRoadDirectionAtIntersection")
             if (testRoadDirectionAtIntersection == RoadDirectionAtIntersection.LEADING_AND_TRAILING){
                 // split the road into two
                 val roadCoordinatesSplitIntoTwo = splitRoadByIntersection(testNearestIntersection.features[0], road)
                 // for each split road work out the relative direction from the intersection
                 for (splitRoad in roadCoordinatesSplitIntoTwo) {
                     val testReferenceCoordinateForRoad = getReferenceCoordinate(
                         splitRoad.geometry as LineString, 25.0, false)
                     // test if the reference coordinate we've created is in any of the relative direction triangles
                     for(direction in intersectionRelativeDirections){
                         val iAmHere1 = polygonContainsCoordinates(
                             testReferenceCoordinateForRoad, (direction.geometry as Polygon))
                         if (iAmHere1){
                             println("Road name: ${splitRoad.properties!!["name"]}")
                             println("Road direction: ${direction.properties!!["Direction"]}")
                         } else {
                             // reverse the LineString, create the ref coordinate and test it again
                             val testReferenceCoordinateReverse = getReferenceCoordinate(
                                 splitRoad.geometry as LineString, 25.0, true
                             )
                             val iAmHere2 = polygonContainsCoordinates(
                                 testReferenceCoordinateReverse, (direction.geometry as Polygon)
                             )
                             if (iAmHere2) {
                                 println("Road name: ${splitRoad.properties!!["name"]}")
                                 println("Road direction: ${direction.properties!!["Direction"]}")
                             }
                         }
                     }
                 }
             }
             else{
                 for (direction in intersectionRelativeDirections){
                     val testReferenceCoordinateForward = getReferenceCoordinate(
                         road.geometry as LineString, 3.0, false)
                     val iAmHere1 = polygonContainsCoordinates(
                         testReferenceCoordinateForward, (direction.geometry as Polygon))
                     if (iAmHere1){
                         println("Road name: ${road.properties!!["name"]}")
                         println("Road direction: ${direction.properties!!["Direction"]}")
                     } else {
                         // reverse the LineString, create the ref coordinate and test it again
                         val testReferenceCoordinateReverse = getReferenceCoordinate(
                             road.geometry as LineString, 3.0, true
                         )
                         val iAmHere2 = polygonContainsCoordinates(
                             testReferenceCoordinateReverse,
                             (direction.geometry as Polygon)
                         )
                         if (iAmHere2) {
                             println("Road name: ${road.properties!!["name"]}")
                             println("Road direction: ${direction.properties!!["Direction"]}")
                         }
                     }
                 }
             }
         }
     }



}