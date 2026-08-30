package org.scottishtecharmy.soundscape

import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.scottishtecharmy.soundscape.geoengine.GRID_SIZE
import org.scottishtecharmy.soundscape.geoengine.MAX_ZOOM_LEVEL
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.formatDistanceAndDirection
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayEnd
import org.scottishtecharmy.soundscape.geoengine.utils.geocoders.OfflineGeocoder
import org.scottishtecharmy.soundscape.geoengine.utils.geocoders.StreetDescription
import org.scottishtecharmy.soundscape.geoengine.utils.geocoders.TileSearch
import org.scottishtecharmy.soundscape.geoengine.utils.searchFeaturesByName
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.utils.process
import kotlin.test.assertEquals

class SearchTest {

    // Builds a fresh OfflineGeocoder (and the GridState/settlementState it needs), centered on
    // `location`, so each case is independent rather than sharing one geocoder/grid across
    // unrelated query points.
    private fun assertOfflineReverseGeocode(
        location: LngLatAlt,
        heading: Double,
        expectedName: String,
        ignoreHouseNumbers: Boolean = true,
        // Real GPS map-matching (MapMatchFilter) snaps onto the nearest way in TreeId.WAYS_SELECTION
        // regardless of whether it's named, unlike OfflineGeocoder's own unmatched fallback search
        // which only considers named roads. Set this to reproduce being map-matched onto whatever
        // way (named or not) is actually nearest `location`.
        mapMatch: Boolean = false
    ) = runBlocking {
        val gridState = getGridStateForLocation(location, MAX_ZOOM_LEVEL, GRID_SIZE)
        val settlementState = getGridStateForLocation(location, 12, 3)
        val tileSearch = TileSearch(offlineExtractPath, gridState, settlementState)
        val offlineGeocoder = OfflineGeocoder(
            gridState,
            settlementState,
            tileSearch,
            processor = { it.process() })

        val mapMatchedWay = if (mapMatch) {
            gridState.getNearestFeature(TreeId.WAYS_SELECTION, gridState.ruler, location, 30.0) as Way?
        } else null

        val result = offlineGeocoder.getAddressFromLngLat(
            UserGeometry(location, heading, mapMatchedWay = mapMatchedWay),
            null,
            ignoreHouseNumbers
        )
        assertEquals(expectedName, result!!.name)
    }

    // Check that the street address also works
    @Test
    fun offlineReverseGeocode_houseNumber() = assertOfflineReverseGeocode(
        LngLatAlt(-4.3069511, 55.9400774),
        90.0,
        "21 Dougalston Avenue",
        ignoreHouseNumbers = false
    )

    @Test
    fun offlineReverseGeocode_crossveggateBetweenServiceRoadAndGlasgowRoad() = assertOfflineReverseGeocode(
        LngLatAlt(-4.3118, 55.9400),
        90.0,
        "On Crossveggate between Service to Lennox Park via bridge and Glasgow Road"
    )

    @Test
    fun offlineReverseGeocode_craigmillarAvenueUntilTannochDrive() = assertOfflineReverseGeocode(
        LngLatAlt(-4.3108846, 55.9495440),
        180.0,
        "On Craigmillar Avenue, 340 metres until Tannoch Drive"
    )

    @Test
    fun offlineReverseGeocode_craigmillarAvenueSinceTannochDrive() = assertOfflineReverseGeocode(
        LngLatAlt(-4.3117919, 55.9486518),
        0.0,
        "On Craigmillar Avenue, 220 metres since Tannoch Drive"
    )

    @Test
    fun offlineReverseGeocode_strathblaneRoadBetweenKerslandLaneAndPathToKerslandDrive() = assertOfflineReverseGeocode(
        LngLatAlt(-4.3107229, 55.9438725),
        0.0,
        "On Strathblane Road between Kersland Lane and Path to Kersland Drive"
    )

    @Test
    fun offlineReverseGeocode_dougalstonAvenueBetweenGlasgowRoadAndPathToDougalstonGardensNorth() = assertOfflineReverseGeocode(
        LngLatAlt(-4.3103500, 55.9396160),
        90.0,
        "On Dougalston Avenue between Glasgow Road and Path to Dougalston Gardens North"
    )

    @Test
    fun offlineReverseGeocode_dougalstonAvenueBetweenSouthGlassfordStreetAndBriarwellRoad() = assertOfflineReverseGeocode(
        LngLatAlt(-4.3088339, 55.9396215),
        90.0,
        "On Dougalston Avenue between South Glassford Street and Briarwell Road"
    )

    @Test
    fun offlineReverseGeocode_dougalstonAvenueBetweenBriarwellRoadAndConnellCrescentToDeadEnd() = assertOfflineReverseGeocode(
        LngLatAlt(-4.3069511, 55.9400774),
        90.0,
        "On Dougalston Avenue between Briarwell Road and Connell Crescent to dead end"
    )

    // Regression test: an unnamed path with no adjacent named road matched into its "pavement"
    // property used to produce a blank first argument to StreetDescriptionBetween, i.e. "On
    // between X and Y" (double space after "On"). This only happens when the path is map-matched
    // directly (mapMatch = true) - OfflineGeocoder's own unmatched fallback search only considers
    // named roads, so it can never land on this branch by itself.
    @Test
    fun offlineReverseGeocode_unnamedPathBetweenTwoNamedRoads() = assertOfflineReverseGeocode(
        LngLatAlt(-4.312731921672821, 55.93184213588228),
        90.0,
        "On Path that joins OSM Feature playground playground and Allander Leisure Centre " +
            "between Path to Bennie Place and Path to Allander Leisure Centre",
        mapMatch = true
    )

    @Test
    fun offlineReverseGeocode_houseNumbersOddFacingNorth() = assertOfflineReverseGeocode(
        LngLatAlt(-4.3122090, 55.9436836),
        0.0,
        "11 Kersland Drive",
        ignoreHouseNumbers = false,
        mapMatch = true
    )

    @Test
    fun offlineReverseGeocode_houseNumbersOddFacingSouth() = assertOfflineReverseGeocode(
        LngLatAlt(-4.3122090, 55.9436836),
        180.0,
        "11 Kersland Drive",
        ignoreHouseNumbers = false,
        mapMatch = true
    )

    @Test
    fun offlineReverseGeocode_houseNumbersEvenFacingNorth() = assertOfflineReverseGeocode(
        LngLatAlt(-4.3119106, 55.9437322),
        0.0,
        "18 Kersland Drive",
        ignoreHouseNumbers = false,
        mapMatch = true
    )

    @Test
    fun offlineReverseGeocode_houseNumbersEvenFacingSouth() = assertOfflineReverseGeocode(
        LngLatAlt(-4.3119106, 55.9437322),
        180.0,
        "18 Kersland Drive",
        ignoreHouseNumbers = false,
        mapMatch = true
    )

    /**
     * The address row on a POI's details screen reverse geocodes the POI's own location, so the
     * nearest named feature to it is always the POI itself: James Gale Memorial is a fountain in
     * a Milngavie park, with nothing named within 30m and no named road within 90m, so the
     * nearby-POI fallback used to hand back "James Gale Memorial" as the memorial's address -
     * a copy of the title already at the top of the screen. getAddressForFeature leaves those
     * fallbacks out and answers with the settlement instead, while getAddressFromLngLat (which
     * is what "My Location" uses, where naming the nearest feature is exactly what's wanted)
     * keeps naming the memorial.
     */
    @Test
    fun offlineReverseGeocode_poiWithNoStreetIsNotItsOwnAddress() = runBlocking {
        val location = LngLatAlt(-4.3092868, 55.9503336)
        val gridState = getGridStateForLocation(location, MAX_ZOOM_LEVEL, GRID_SIZE)
        val settlementState = getGridStateForLocation(location, 12, 3)
        val offlineGeocoder = OfflineGeocoder(
            gridState,
            settlementState,
            TileSearch(offlineExtractPath, gridState, settlementState),
            processor = { it.process() })

        val userGeometry = UserGeometry(location)
        assertEquals(
            "James Gale Memorial",
            offlineGeocoder.getAddressFromLngLat(userGeometry, null, false)?.name
        )
        assertEquals(
            "Milngavie",
            offlineGeocoder.getAddressForFeature(userGeometry, null, false)?.name
        )
    }

    @Test
    fun offlineSearch() {
        runBlocking {

            val currentLocation = LngLatAlt(-4.3060165, 55.9475021)
            val gridState = getGridStateForLocation(currentLocation, MAX_ZOOM_LEVEL, GRID_SIZE)
            val settlementState = getGridStateForLocation(currentLocation, 12, 3)
            val tileSearch = TileSearch(offlineExtractPath, gridState, settlementState)
            val offlineGeocoder = OfflineGeocoder(
                gridState,
                settlementState,
                tileSearch,
                processor = { it.process() })

            var results =
                offlineGeocoder.getAddressFromLocationName("Greggs", currentLocation, null)!!
            assertEquals("Greggs", results[0].name)
            assertEquals("OSM Feature fast_food fast_food", results[0].featureName?.additionalText ?: "")

            results = offlineGeocoder.getAddressFromLocationName(
                "5 buchanan street milngavie",
                currentLocation,
                null
            )!!
            assertEquals("5 Buchanan Street", results[0].name)
            assertEquals("5 Buchanan Street, Milngavie", results[0].description)

            results = offlineGeocoder.getAddressFromLocationName(
                "12 roselea drive",
                currentLocation,
                null
            )!!
            assertEquals(1, results.size)

            results = offlineGeocoder.getAddressFromLocationName(
                "5 buchanan street mulngaviy",
                currentLocation,
                null
            )!!
            assertEquals("5 Buchanan Street", results[0].name)
            assertEquals("5 Buchanan Street, Milngavie", results[0].description)

            results = offlineGeocoder.getAddressFromLocationName(
                "5 buchanan street",
                currentLocation,
                null
            )!!
            assertEquals("5 Buchanan Street", results[0].name)
            assertEquals("5 Buchanan Street, Milngavie", results[0].description)

            results = offlineGeocoder.getAddressFromLocationName(
                "5 buchanan street clachan of campsie",
                currentLocation,
                null
            )!!
            assertEquals("5 Buchanan Street", results[0].name)
            assertEquals("5 Buchanan Street, Milngavie", results[0].description)

            results = offlineGeocoder.getAddressFromLocationName(
                "5 buchanan street",
                currentLocation,
                null
            )!!
            assertEquals("5 Buchanan Street", results[0].name)
            assertEquals("5 Buchanan Street, Milngavie", results[0].description)

            results =
                offlineGeocoder.getAddressFromLocationName("post office", currentLocation, null)!!
            assertEquals("Craigash Road Post Office", results[0].name)
            assertEquals("34 Craigash Road, Milngavie", results[0].description)

            results =
                offlineGeocoder.getAddressFromLocationName("roselea dr 8", currentLocation, null)!!
            assertEquals("8 Roselea Drive", results[0].name)
            assertEquals("8 Roselea Drive, Milngavie", results[0].description)

            results = offlineGeocoder.getAddressFromLocationName(
                "20 roselea drive milngavie",
                currentLocation,
                null
            )!!
            assertEquals("20 Roselea Drive", results[0].name)
            assertEquals("20 Roselea Drive, Milngavie", results[0].description)

            results =
                offlineGeocoder.getAddressFromLocationName("greggs     ", currentLocation, null)!!
            assertEquals("Greggs", results[0].name)
            assertEquals("22 Main Street, Milngavie", results[0].description)

            results = offlineGeocoder.getAddressFromLocationName(
                "milverton avenue",
                currentLocation,
                null
            )!!
            assertEquals("Milverton Avenue", results[0].name)
            assertEquals("Milverton Avenue, Bearsden", results[0].description)

            results = offlineGeocoder.getAddressFromLocationName(
                "milverto avenue",
                currentLocation,
                null
            )!!
            assertEquals("Milverton Avenue", results[0].name)
            assertEquals("Milverton Avenue, Bearsden", results[0].description)

            results =
                offlineGeocoder.getAddressFromLocationName("roselea dr", currentLocation, null)!!
            assertEquals("Roselea Drive", results[0].name)
            assertEquals("Roselea Drive, Milngavie", results[0].description)

            results =
                offlineGeocoder.getAddressFromLocationName("dirleton gate", currentLocation, null)!!
            assertEquals("Dirleton Gate", results[0].name)
            assertEquals("Dirleton Gate, Netherton", results[0].description)

            results =
                offlineGeocoder.getAddressFromLocationName("dirleton gate", currentLocation, null)!!
            assertEquals("Dirleton Gate", results[0].name)
            assertEquals("Dirleton Gate, Netherton", results[0].description)

            results = offlineGeocoder.getAddressFromLocationName(
                "dirleton gate 20",
                currentLocation,
                null
            )!!
            assertEquals("Dirleton Gate", results[0].name)
            assertEquals("Dirleton Gate, Netherton", results[0].description)

            results =
                offlineGeocoder.getAddressFromLocationName("craigton road", currentLocation, null)!!
            assertEquals("Craigton Road", results[0].name)
            assertEquals("Craigton Road, Milngavie", results[0].description)
        }
    }

    @Test
    fun testSearch() {
        // This does a really crude search through the "name" property of the POI features
        val gridState =
            getGridStateForLocation(sixtyAcresCloseTestLocation, MAX_ZOOM_LEVEL, GRID_SIZE)
        val testPoiCollection = gridState.getFeatureCollection(TreeId.POIS)

        // As there isn't much going on in the tiles then it should return the local village shop/coffee place
        val searchResults = searchFeaturesByName(testPoiCollection, "honey")
        Assert.assertEquals(2, searchResults.features.size)
    }

    @Test
    fun testHouseNumbers() {
        val bearsden = LngLatAlt(-4.3067702, 55.9473970)
        val gridState = getGridStateForLocation(bearsden, MAX_ZOOM_LEVEL, GRID_SIZE)

        // We have a tree per street and finding the nearest house is a case of looking up the
        // street and then doing a single search.
        val streetTrees = gridState.gridStreetNumberTreeMap

        val streetName = "Heathfield Drive"

        // Most of the code here is getting some locations along a known road to walk along
        val roadTree = gridState.getFeatureTree(TreeId.ROADS)
        var way: Way? = null
        for (road in roadTree.getAllCollection()) {
            if ((road as Way).name == streetName) {
                way = road
                break
            }
        }
        if (way != null) {
            val ways = mutableListOf<Pair<Boolean, Way>>()
            val intersection =
                way.intersections[WayEnd.END.id] ?: way.intersections[WayEnd.START.id]
            if (intersection != null) {
                way.followWays(intersection, ways)

                for (segment in ways) {
                    for (point in (segment.second.geometry as LineString).coordinates) {
                        val nearestHouse = streetTrees[streetName]?.getNearestFeature(
                            point,
                            gridState.ruler
                        ) as? MvtFeature
                        if (nearestHouse != null) {
                            val distance = gridState.ruler.distance(
                                point,
                                (nearestHouse.geometry as Point).coordinates
                            )
                            println("${nearestHouse.housenumber} $streetName - $distance from $point")
                        }
                    }
                }
            }
        }
    }

    fun streetDescription(
        location: LngLatAlt,
        streetName: String,
        describeLocations: List<LngLatAlt>
    ) {
        val gridState = getGridStateForLocation(location, MAX_ZOOM_LEVEL, GRID_SIZE)
        val nearbyWays = gridState.getFeatureTree(TreeId.WAYS_SELECTION)
            .getNearestCollection(
                location,
                100.0,
                5,
                gridState.ruler
            )
        var matchedWay: Way? = null
        for (way in nearbyWays) {
            if ((way as Way).name == streetName) {
                matchedWay = way
                break
            }
        }
        if (matchedWay == null) return

        val description = StreetDescription(streetName, gridState)
        description.createDescription(matchedWay, null)
        description.describeStreet()
        for (location in describeLocations) {
            val nearestWay = description.nearestWayOnStreet(location)
            if (nearestWay != null) {
                val houseNumber = description.getStreetNumber(nearestWay.first, location)
                println("Interpolated address: ${if (houseNumber.second) "Opposite" else ""} ${houseNumber.first} ${nearestWay.first.name}")
                val result = description.describeLocation(location, 90.0, nearestWay.first, null)

                if (
                    (result.ahead.distance < 10.0) &&
                    ((result.ahead.distance < result.behind.distance) || result.behind.name.isEmpty())
                ) {
                    println("At ${result.ahead.name}")
                } else if (result.behind.distance < 10.0) {
                    println("At ${result.behind.name}")
                } else if (result.behind.name.isNotEmpty() && result.ahead.name.isNotEmpty()) {
                    // TODO: Ideally we'd know the user direction here so that we could give the fraction
                    //  as an increasing value e.g. half way, three quarters of the way etc.
                    val fraction =
                        result.behind.distance / (result.behind.distance + result.ahead.distance)
                    when (fraction) {
                        in 0.2..0.3 -> println("Quarter of the way between: ${result.behind.name} and ${result.ahead.name}")
                        in 0.4..0.6 -> println("Half way between: ${result.behind.name} and ${result.ahead.name}")
                        in 0.7..0.8 -> println("Three quarters way between: ${result.behind.name} and ${result.ahead.name}")
                        else -> {
                            if (result.ahead.distance < result.behind.distance)
                                println("Between: ${result.ahead.name} and ${result.behind.name}, ${result.ahead.distance} from ${result.ahead.name}")
                            else
                                println("Between: ${result.behind.name} and ${result.ahead.name}, ${result.behind.distance} from ${result.behind.name}")
                        }
                    }
                } else if (result.behind.distance < result.ahead.distance) {
                    val distance = formatDistanceAndDirection(result.behind.distance, null, null)
                    println("$distance along from ${result.behind.name}")
                } else {
                    val distance = formatDistanceAndDirection(result.ahead.distance, null, null)
                    println("$distance along from ${result.ahead.name}")
                }
            }
        }
    }

    @Test
    fun testStreetDescription() {
        streetDescription(
            LngLatAlt(-4.3060126, 55.9474004),
            "Roselea Drive",
            listOf(
                LngLatAlt(-4.3054676, 55.9469630) // 2-4 Roselea Drive
            )
        )

        streetDescription(
            LngLatAlt(-4.3133672, 55.9439536),
            "Buchanan Street",
            listOf(
                LngLatAlt(-4.3130768, 55.9446026)
            )
        )
        // Opposite test
        streetDescription(
            LngLatAlt(-4.3133672, 55.9439536),
            "Buchanan Street",
            listOf(
                LngLatAlt(-4.3135689, 55.9440448)
            )
        )
        streetDescription(
            LngLatAlt(-4.3177683, 55.9415574),
            "Douglas Street",
            listOf(
                LngLatAlt(-4.3186897, 55.9410192)
            )
        )
        streetDescription(
            LngLatAlt(-4.2627887, 55.8622846),
            "St Vincent Street",
            listOf(
                LngLatAlt(-4.2637612, 55.8622651)
            )
        )
        streetDescription(
            LngLatAlt(-4.2627887, 55.8622846),
            "St Vincent Street",
            listOf(
                LngLatAlt(-4.2642336, 55.8624708)
            )
        )

        streetDescription(
            LngLatAlt(-4.2559200, 55.8645353),
            "Sauchiehall Street",
            listOf(
                LngLatAlt(-4.2544240, 55.8644774),  // Ryman
                LngLatAlt(-4.2553615, 55.8643427),              // Jollibee
                LngLatAlt(-4.2559443, 55.8644524),              // Art piece
                LngLatAlt(-4.2566746, 55.8647423),              // Route One
                LngLatAlt(-4.2584339, 55.8647259),              // Waterstones
            )
        )
    }

    private fun testLineSearchLocation(location: LngLatAlt, streetName: String) {
        // Search for house numbers near Buchanan Street. There are two which don't have the street
        // address in the OSM data.
        val gridState = getGridStateForLocation(location, MAX_ZOOM_LEVEL, GRID_SIZE)

        val nearbyWays = gridState.getFeatureTree(TreeId.WAYS_SELECTION)
            .getNearestCollection(
                location,
                100.0,
                5,
                gridState.ruler
            )
        var matchedWay: Way? = null
        for (way in nearbyWays) {
            if ((way as Way).name == streetName) {
                matchedWay = way
                break
            }
        }
        assert(matchedWay != null)

        // Look for nearby street numbers which didn't have a named street
        val tree = gridState.gridStreetNumberTreeMap["null"]

        val results = tree!!.getNearbyLine(
            matchedWay!!.geometry as LineString,
            50.0,
            gridState.ruler
        )
        results.forEach { println((it as MvtFeature).name) }

        // Look POI near the road
        val poiTree = gridState.getFeatureTree(TreeId.POIS)
        val poiResults = poiTree.getNearbyLine(
            matchedWay.geometry as LineString,
            25.0,
            gridState.ruler
        )
        poiResults.forEach { println((it as MvtFeature).name) }

        // These searches form the basis of extending StreetDescription beyond Features which contain
        // the street address.
    }

    @Test
    fun testLineSearch() {
        val buchananStreet = LngLatAlt(-4.3134938, 55.9449487)
        testLineSearchLocation(buchananStreet, "Buchanan Street")

        val buchananStreet2 = LngLatAlt(-4.3136986, 55.9455014)
        testLineSearchLocation(buchananStreet2, "Buchanan Street")
    }

    @Test
    fun multilingualSearch() {
        runBlocking {
            val currentLocation = LngLatAlt(-3.2003818, 55.9487360)
            val gridState = getGridStateForLocation(currentLocation, MAX_ZOOM_LEVEL, GRID_SIZE)
            val settlementState = getGridStateForLocation(currentLocation, 12, 3)
            val tileSearch = TileSearch(offlineExtractPath, gridState, settlementState)
            val offlineGeocoder = OfflineGeocoder(gridState, settlementState, tileSearch)

            var results =
                offlineGeocoder.getAddressFromLocationName("एडिनबर्ग किला", currentLocation, null)!!
            assertEquals("एडिनबर्ग किला", results[0].name)

            results =
                offlineGeocoder.getAddressFromLocationName("エディンバラ城", currentLocation, null)!!
            assertEquals("エディンバラ城", results[0].name)

            results =
                offlineGeocoder.getAddressFromLocationName("Эдинбург қамалы", currentLocation, null)!!
            assertEquals("Эдинбург қамалы", results[0].name)
        }
    }

    @Test
    fun interpolatedAddressSearchWithPolygonHouseNumber() {
        // Regression test for a real crash: "Carnoustie Library" (osm id 6068429362) is real
        // data within glasgow-gb.pmtiles, tagged housenumber=21, street=High Street, but with
        // Polygon (building outline) geometry rather than the Point geometry that
        // StreetDescription.getInterpolateLocation used to assume every house number had.
        // Resolving any house number on this street used to throw
        // "ClassCastException: Polygon cannot be cast to Point".
        runBlocking {
            val currentLocation = LngLatAlt(-2.7080655097961426, 56.50118028013476)
            val gridState = getGridStateForLocation(currentLocation, MAX_ZOOM_LEVEL, GRID_SIZE)

            val tileSearch = TileSearch(offlineExtractPath, gridState, gridState)
            val nearestWay = tileSearch.findNearestNamedWay(currentLocation, "High Street")!!

            val streetDescription = StreetDescription("High Street", gridState)
            streetDescription.createDescription(nearestWay, null)

            val result = streetDescription.getLocationFromStreetNumber("21")
            assertEquals("21", result?.second)
        }
    }
}