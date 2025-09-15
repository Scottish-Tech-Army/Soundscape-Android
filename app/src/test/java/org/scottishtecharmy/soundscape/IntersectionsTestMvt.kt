package org.scottishtecharmy.soundscape

import org.junit.Assert
import org.junit.Test
import org.scottishtecharmy.soundscape.geoengine.GRID_SIZE
import org.scottishtecharmy.soundscape.geoengine.MAX_ZOOM_LEVEL
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geoengine.callouts.getRoadsDescriptionFromFov
import org.scottishtecharmy.soundscape.geoengine.filters.MapMatchFilter
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Intersection
import org.scottishtecharmy.soundscape.geoengine.utils.Direction
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection

class IntersectionsTestMvt {
    private fun setupTest(currentLocation: LngLatAlt,
                          deviceHeading: Double,
                          fovDistance: Double) : Intersection? {

        val gridState = getGridStateForLocation(currentLocation, MAX_ZOOM_LEVEL, GRID_SIZE)
        val mapMatchFilter = MapMatchFilter()
        mapMatchFilter.filter(
            location = currentLocation,
            gridState = gridState,
            collection = FeatureCollection(),
            dump = false
        )
        val userGeometry = UserGeometry(
            location = currentLocation,
            phoneHeading = deviceHeading,
            fovDistance = fovDistance,
            mapMatchedWay = mapMatchFilter.matchedWay
        )

        return getRoadsDescriptionFromFov(
                    gridState,
                    userGeometry
                ).intersection
    }
    @Test
    fun intersectionsStraightAheadType(){
        // This is the same test but using GeoJSON generated from a .Mvt tile NOT the original
        // GeoJSON from the Soundscape backend
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

        // Weston Road (A) to Long Ashton Road (B)
        // https://geojson.io/#map=17/51.430494/-2.657463

        // Fake device location and pretend the device is pointing East.
        // -2.6577997643930757, 51.43041390383118
        val currentLocation = LngLatAlt(-2.6573400576040456, 51.430456817236575)
        val deviceHeading = 90.0
        val fovDistance = 50.0

        val intersection = setupTest(currentLocation, deviceHeading, fovDistance)!!

        // should be two roads that make up the intersection
        Assert.assertEquals(2, intersection.members.size )

        // they are Weston Road and Long Ashton Road and should be behind (0) and the other ahead (4)
        val indexWR = 1
        val indexLA = 0
        Assert.assertEquals(Direction.BEHIND, intersection.members[indexWR].direction(intersection, deviceHeading))
        Assert.assertEquals("Weston Road", intersection.members[indexWR].properties!!["name"])
        Assert.assertEquals(Direction.AHEAD, intersection.members[indexLA].direction(intersection, deviceHeading))
        Assert.assertEquals("Long Ashton Road", intersection.members[indexLA].properties!!["name"])

    }

    @Test
    fun intersectionsRightTurn(){
        //  Turn Right
        //   _____________
        //  |          B →
        //  | ↑  _________
        //  | * |
        //  |   |
        //  | A |
        //  | ↓ |

        // Belgrave Place (A) to Codrington Place (B)
        //https://geojson.io/#map=19/51.4579043/-2.6156923

        // Fake device location and pretend the device is pointing South West and we are located on:
        // Belgrave Place (A)
        val currentLocation = LngLatAlt(-2.615585745757045,51.457957257918395)
        val deviceHeading = 225.0 // South West
        val fovDistance = 50.0

        val intersection = setupTest(currentLocation, deviceHeading, fovDistance)!!

        // should be two roads that make up the intersection
        Assert.assertEquals(2, intersection.members.size )
        // they are Belgrave Place and Codrington Place and should be behind (0) and right (6)
        val indexBP = 0
        val indexCP = 1
        Assert.assertEquals(Direction.BEHIND, intersection.members[indexBP].direction(intersection, deviceHeading))
        Assert.assertEquals("Belgrave Place", intersection.members[indexBP].properties!!["name"])
        Assert.assertEquals(Direction.RIGHT, intersection.members[indexCP].direction(intersection, deviceHeading))
        Assert.assertEquals("Codrington Place", intersection.members[indexCP].properties!!["name"])

    }

    @Test
    fun intersectionsLeftTurn(){
        //  Turn Left
        //  _____________
        //  ← B          |
        //  _________  ↑ |
        //           | * |
        //           |   |
        //           | A |
        //           | ↓ |

        // same again just depends what road you are standing on
        // Codrington Place (A) to Belgrave Place (B)
        //https://geojson.io/#map=19/51.4579382/-2.6157338
        // Fake device location and pretend the device is pointing South East and we are standing on:
        // Codrington Place
        val currentLocation = LngLatAlt(-2.6159411752634583, 51.45799104056931)
        val deviceHeading = 135.0 // South East
        val fovDistance = 50.0

        val intersection = setupTest(currentLocation, deviceHeading, fovDistance)!!

        // should be two roads that make up the intersection
        Assert.assertEquals(2, intersection.members.size )
        // they are Codrington Place and Belgrave Place and should be behind (0) and left (2)
        val indexBP = 0
        val indexCP = 1
        Assert.assertEquals(Direction.BEHIND, intersection.members[indexCP].direction(intersection, deviceHeading))
        Assert.assertEquals("Codrington Place", intersection.members[indexCP].properties!!["name"])
        Assert.assertEquals(Direction.LEFT, intersection.members[indexBP].direction(intersection, deviceHeading))
        Assert.assertEquals("Belgrave Place", intersection.members[indexBP].properties!!["name"])

    }

    @Test
    fun intersectionsSideRoadRight(){
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
        // Long Ashton Road (A) and St Martins (B)
        // https://geojson.io/#map=18/51.430741/-2.656311
        //

        // Fake device location and pretend the device is pointing South West and we are located on:
        // Long Ashton Road
        val currentLocation = LngLatAlt(-2.656109007812404,51.43079699441145)
        val deviceHeading = 250.0 // South West
        val fovDistance = 50.0

        val intersection = setupTest(currentLocation, deviceHeading, fovDistance)!!

        // There should now be three roads that make up the intersection:
        // The road that leads up to the intersection Long Ashton Road (0)
        // The road that continues on from the intersection Long Ashton Road (4)
        // The road that is the right turn St Martins (6)
        Assert.assertEquals(3, intersection.members.size )

        val indexLA1 = 1
        val indexLA2 = 0
        val indexSM = 2
        Assert.assertEquals(Direction.BEHIND, intersection.members[indexLA1].direction(intersection, deviceHeading))
        Assert.assertEquals("Long Ashton Road", intersection.members[indexLA1].properties!!["name"])
        Assert.assertEquals(Direction.AHEAD, intersection.members[indexLA2].direction(intersection, deviceHeading))
        Assert.assertEquals("Long Ashton Road", intersection.members[indexLA2].properties!!["name"])
        Assert.assertEquals(Direction.RIGHT, intersection.members[indexSM].direction(intersection, deviceHeading))
        Assert.assertEquals("St Martins", intersection.members[indexSM].properties!!["name"])

    }

    @Test
    fun intersectionsSideRoadLeft(){
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

        // Long Ashton Road (A) and St Martins (B) same as above but location and device direction changed
        // https://geojson.io/#map=18/51.430741/-2.656311
        //
        // Fake device location and pretend the device is pointing North East and we are located on:
        // Long Ashton Road (A)
        val currentLocation = LngLatAlt(-2.656530323429564,51.43065207103919)
        val deviceHeading = 50.0 // North East
        val fovDistance = 50.0

        val intersection = setupTest(currentLocation, deviceHeading, fovDistance)!!

        // should be three roads that make up the intersection:
        // The road that lead up to the intersection Long Ashton Road (0)
        // The road that is the left turn St Martins (2)
        // The road that continues on from the intersection Long Ashton Road (4)
        Assert.assertEquals(3, intersection.members.size)

        val indexLA1 = 0
        val indexLA2 = 2
        val indexSM = 1
        Assert.assertEquals(Direction.BEHIND, intersection.members[indexLA1].direction(intersection, deviceHeading))
        Assert.assertEquals("Long Ashton Road", intersection.members[indexLA1].properties!!["name"])
        Assert.assertEquals(Direction.LEFT, intersection.members[indexLA2].direction(intersection, deviceHeading))
        Assert.assertEquals("St Martins", intersection.members[indexLA2].properties!!["name"])
        Assert.assertEquals(Direction.AHEAD, intersection.members[indexSM].direction(intersection, deviceHeading))
        Assert.assertEquals("Long Ashton Road", intersection.members[indexSM].properties!!["name"])

    }

    @Test
    fun intersectionsT1Test(){
        //  T1
        //  ___________________
        //  ← B             B →
        //  _______     _______
        //         | ↑ |
        //         | * |
        //         |   |
        //         | A |
        //         | ↓ |

        // Standing on St Martins (A) with device pointing towards Long Ashton Road (B)
        // https://geojson.io/#map=18/51.430741/-2.656311


        // Fake device location and pretend the device is pointing South West and we are located on:
        // St Martins
        val currentLocation = LngLatAlt(-2.656540700657672,51.430978147982785)
        val deviceHeading = 140.0 // South East
        val fovDistance = 50.0

        val intersection = setupTest(currentLocation, deviceHeading, fovDistance)!!

        // should be three roads that make up the intersection:
        // The road that leads up to the intersection St Martins (0)
        // The road that is the T intersection Long Ashton Road left (2) and right (6)

        Assert.assertEquals(3, intersection.members.size )

        val indexLA1 = 1
        val indexLA2 = 0
        val indexSM = 2
        Assert.assertEquals(Direction.BEHIND, intersection.members[indexSM].direction(intersection, deviceHeading))
        Assert.assertEquals("St Martins", intersection.members[indexSM].properties?.get("name") ?: "No idea")
        Assert.assertEquals(Direction.LEFT, intersection.members[indexLA1].direction(intersection, deviceHeading))
        Assert.assertEquals("Long Ashton Road", intersection.members[indexLA1].properties?.get("name") ?: "No idea")
        Assert.assertEquals(Direction.RIGHT, intersection.members[indexLA2].direction(intersection, deviceHeading))
        Assert.assertEquals("Long Ashton Road", intersection.members[indexLA2].properties?.get("name") ?: "No idea")
    }

    @Test
    fun intersectionsT2Test(){
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

        // Fake device location and pretend the device is pointing South West and we are located on:
        // Goodeve Road  The Left is Seawalls Road and Right is Knoll Hill
        val currentLocation = LngLatAlt(-2.637514213827643, 51.472589063821175)
        val deviceHeading = 225.0 // South West
        val fovDistance = 50.0

        val intersection = setupTest(currentLocation, deviceHeading, fovDistance)!!

        Assert.assertEquals(3, intersection.members.size )
        // Goodeve Road (0) Seawalls Road (2) and Knoll Hill (6)
        val indexGR = 1
        val indexSR = 2
        val indexKH = 0
        Assert.assertEquals(Direction.BEHIND, intersection.members[indexGR].direction(intersection, deviceHeading))
        Assert.assertEquals("Goodeve Road", intersection.members[indexGR].properties!!["name"])
        Assert.assertEquals(Direction.LEFT, intersection.members[indexSR].direction(intersection, deviceHeading))
        Assert.assertEquals("Seawalls Road", intersection.members[indexSR].properties!!["name"])
        Assert.assertEquals(Direction.RIGHT, intersection.members[indexKH].direction(intersection, deviceHeading))
        Assert.assertEquals("Knoll Hill", intersection.members[indexKH].properties!!["name"])
    }

    @Test
    fun intersectionsCross1Test(){
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

        // Fake device location and pretend the device is pointing North West and we are located on:
        // Grange Road  The Left and Right for the crossroad is Manilla Road and ahead is Grange Road
        val currentLocation = LngLatAlt(-2.61850147329568, 51.456953686378085)
        val deviceHeading = 340.0 // North North West
        val fovDistance = 50.0

        val intersection = setupTest(currentLocation, deviceHeading, fovDistance)!!

        Assert.assertEquals(4, intersection.members.size )
        // Grange Road (0) and (4) Manilla Road Road (2) and (6)
        val indexGR1 = 0
        val indexGR2 = 2
        val indexMR1 = 3
        val indexMR2 = 1
        Assert.assertEquals(Direction.BEHIND, intersection.members[indexGR1].direction(intersection, deviceHeading))
        Assert.assertEquals("Grange Road", intersection.members[indexGR1].properties!!["name"])
        Assert.assertEquals(Direction.LEFT, intersection.members[indexMR1].direction(intersection, deviceHeading))
        Assert.assertEquals("Manilla Road", intersection.members[indexMR1].properties!!["name"])
        Assert.assertEquals(Direction.AHEAD, intersection.members[indexGR2].direction(intersection, deviceHeading))
        Assert.assertEquals("Grange Road", intersection.members[indexGR2].properties!!["name"])
        Assert.assertEquals(Direction.RIGHT, intersection.members[indexMR2].direction(intersection, deviceHeading))
        Assert.assertEquals("Manilla Road", intersection.members[indexMR2].properties!!["name"])

    }

    @Test
    fun intersectionCross2Test(){
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
        // NOTE: This is a strange crossroads because it is actually made up of four different LineStrings
        // 2 x Lansdown Road LineStrings (would expect it to be made out of one)
        // and 1 Linestring  Manilla Road and 1 LineString Vyvyan Road

        // Fake device location and pretend the device is pointing North West and we are located on:
        // Lansdown Road and it continues on straight ahead.  The Left is Manilla Road and Right is Vyvyan Road
        val currentLocation = LngLatAlt(-2.6176822011131833, 51.457104175295484)
        val deviceHeading = 340.0 // North West
        val fovDistance = 50.0

        val intersection = setupTest(currentLocation, deviceHeading, fovDistance)!!

        Assert.assertEquals(4, intersection.members.size )

        // Lansdown Road (0) and (4) Manilla Road (2) and Vyvyan Road(6)
        val indexLR1 = 0
        val indexLR2 = 2
        val indexMR = 1
        val indexVR = 3
        Assert.assertEquals(Direction.BEHIND, intersection.members[indexLR1].direction(intersection, deviceHeading))
        Assert.assertEquals("Lansdown Road", intersection.members[indexLR1].properties!!["name"])
        Assert.assertEquals(Direction.LEFT, intersection.members[indexMR].direction(intersection, deviceHeading))
        Assert.assertEquals("Manilla Road", intersection.members[indexMR].properties!!["name"])
        Assert.assertEquals(Direction.AHEAD, intersection.members[indexLR2].direction(intersection, deviceHeading))
        Assert.assertEquals("Lansdown Road", intersection.members[indexLR2].properties!!["name"])
        Assert.assertEquals(Direction.RIGHT, intersection.members[indexVR].direction(intersection, deviceHeading))
        Assert.assertEquals("Vyvyan Road", intersection.members[indexVR].properties!!["name"])

    }

    @Test
    fun intersectionsCross3Test(){
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
        // Fake device location and pretend the device is pointing North West and we are located on:
        //  Standing on St Mary's Butts with Oxford Road on Left, West Street Ahead and Broad Street on Right
        val currentLocation = LngLatAlt(-0.9752549546655587, 51.4553843453491)
        val deviceHeading = 320.0 // North West
        val fovDistance = 50.0

        val intersection = setupTest(currentLocation, deviceHeading, fovDistance)!!

        Assert.assertEquals(4, intersection.members.size )

        // St Mary's Butts (0)  Oxford Road (2), West Street (4) and Broad Street (6)
        val indexSMB = 1
        val indexOR = 0
        val indexWS = 2
        val indexBS = 3
        Assert.assertEquals(Direction.BEHIND, intersection.members[indexSMB].direction(intersection, deviceHeading))
        Assert.assertEquals("St Mary's Butts", intersection.members[indexSMB].properties!!["name"])
        Assert.assertEquals(Direction.LEFT, intersection.members[indexOR].direction(intersection, deviceHeading))
        Assert.assertEquals("Oxford Road", intersection.members[indexOR].properties!!["name"])
        Assert.assertEquals(Direction.AHEAD, intersection.members[indexWS].direction(intersection, deviceHeading))
        Assert.assertEquals("West Street", intersection.members[indexWS].properties!!["name"])
        Assert.assertEquals(Direction.RIGHT, intersection.members[indexBS].direction(intersection, deviceHeading))
        Assert.assertEquals("Broad Street", intersection.members[indexBS].properties!!["name"])

    }

    //@Test
    fun intersectionsLoopBackTest(){
        // Some intersections can contain the same road more than once,
        // for example if one road loops back to the intersection
        // https://geojson.io/#map=18/37.339112/-122.038756

        val currentLocation = LngLatAlt(-122.03856292573965,37.33916628666543)
        val deviceHeading = 270.0
        val fovDistance = 50.0

        val intersection = setupTest(currentLocation, deviceHeading, fovDistance)!!

        // Removed the duplicate osm_ids so we should be good to go...or not
        Assert.assertEquals(3, intersection.members.size )

        val indexKC = 0
//        val indexS1 = 1
//        val indexS2 = 2
        Assert.assertEquals(Direction.BEHIND, intersection.members[indexKC].direction(intersection, deviceHeading))
        Assert.assertEquals("Kodiak Court", intersection.members[indexKC].properties!!["name"])
//        Assert.assertEquals(3, intersection.members[indexS1].direction(intersection, deviceHeading))
//        Assert.assertEquals("service", intersection.members[indexS1].properties!!["name"])
//        Assert.assertEquals(5, intersection.members[indexS2].direction(intersection, deviceHeading))
//        Assert.assertEquals("service", intersection.members[indexS2].properties!!["name"])
    }
}