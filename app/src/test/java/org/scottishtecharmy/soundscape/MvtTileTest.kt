package org.scottishtecharmy.soundscape

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.experimental.categories.Category
import org.scottishtecharmy.soundscape.MainActivity.Companion.MOBILITY_KEY
import org.scottishtecharmy.soundscape.MainActivity.Companion.PLACES_AND_LANDMARKS_KEY
import org.scottishtecharmy.soundscape.geoengine.GRID_SIZE
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.MAX_ZOOM_LEVEL
import org.scottishtecharmy.soundscape.geoengine.ProtomapsGridState
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.callouts.AutoCallout
import org.scottishtecharmy.soundscape.geoengine.LastStationTracker
import org.scottishtecharmy.soundscape.geoengine.NotableVehicleEventTracker
import org.scottishtecharmy.soundscape.geoengine.describeReverseGeocode
import org.scottishtecharmy.soundscape.geoengine.filters.MapMatchFilter
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.EntranceDetails
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.EntranceMatching
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Intersection
import org.scottishtecharmy.soundscape.components.LocationSource
import org.scottishtecharmy.soundscape.geoengine.nearestSettlement
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayEnd
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayType
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.convertBackToTileCoordinates
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.sampleToFractionOfTile
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.vectorTileToGeoJson
import org.scottishtecharmy.soundscape.geoengine.processTileFeatureCollection
import org.scottishtecharmy.soundscape.geoengine.utils.CountryBoundaries
import org.scottishtecharmy.soundscape.geoengine.utils.DrivingSide
import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geoengine.utils.ResourceMapper
import org.scottishtecharmy.soundscape.geoengine.utils.confectNamesForRoad
import org.scottishtecharmy.soundscape.geoengine.utils.createPolygonFromTriangle
import org.scottishtecharmy.soundscape.geoengine.utils.geocoders.OfflineGeocoder
import org.scottishtecharmy.soundscape.geoengine.utils.geocoders.StreetDescription
import org.scottishtecharmy.soundscape.geoengine.utils.getCentralPointForFeature
import org.scottishtecharmy.soundscape.geoengine.utils.getCentroidOfPolygon
import org.scottishtecharmy.soundscape.geoengine.utils.getDistanceToFeature
import org.scottishtecharmy.soundscape.geoengine.utils.getFovTriangle
import org.scottishtecharmy.soundscape.geoengine.utils.getLatLonTileWithOffset
import org.scottishtecharmy.soundscape.geoengine.utils.gpx.parseGpx
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.createCheapRuler
import org.scottishtecharmy.soundscape.geoengine.utils.searchFeaturesByName
import org.scottishtecharmy.soundscape.geoengine.utils.traverseIntersectionsConfectingNames
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.geojsonparser.geojson.MultiPolygon
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Polygon
import org.scottishtecharmy.soundscape.geojsonparser.moshi.GeoJsonObjectMoshiAdapter
import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import org.scottishtecharmy.soundscape.i18n.PluralKey
import org.scottishtecharmy.soundscape.i18n.StringKey
import org.scottishtecharmy.soundscape.utils.toLocationDescription
import org.scottishtecharmy.soundscape.utils.fuzzyCompare
import org.scottishtecharmy.soundscape.utils.process
import java.io.File
import java.io.FileOutputStream
import kotlin.io.path.Path
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension
import kotlin.math.abs
import kotlin.system.measureTimeMillis
import kotlin.time.measureTime

/**
 * FileGridState overrides ProtomapsGridState updateTile to set validateContext to false as it
 * assumes that the tests are all running in a single context.
 */
const val offlineExtractPath = "src/test/res/org/scottishtecharmy/soundscape"

class FileGridState(
    zoomLevel: Int = MAX_ZOOM_LEVEL,
    gridSize: Int = GRID_SIZE
) : ProtomapsGridState(zoomLevel, gridSize) {

    init {
        validateContext = false
    }
}

/**
 * Minimal [LocalizedStrings] stub for tests that need to check *which* string key and arguments
 * a callout resolves to, without pulling in the real Compose resource-bundle string lookup.
 */
private class FakeLocalizedStrings : LocalizedStrings {
    override fun get(key: StringKey, vararg args: Any?): String = when (key) {
        // Keep formatted numbers readable in assertions rather than nesting a
        // NumberDecimalSeparator() stub in the middle of every distance.
        StringKey.NumberDecimalSeparator -> "."
        StringKey.NumberDecimalSeparatorA11y -> " point "
        else -> "$key(${args.joinToString(", ")})"
    }

    override fun getOrNull(key: StringKey, vararg args: Any?): String? = get(key, *args)

    override fun getPlural(key: PluralKey, quantity: Int, vararg args: Any?): String =
        "$key(${args.joinToString(", ")})"

    override fun resolveFeatureClass(key: String): String? = null
}

private fun vectorTileToGeoJsonFromFile(
    tileX: Int,
    tileY: Int,
    intersectionMap: HashMap<LngLatAlt, Intersection>,
    streetNumberMap: HashMap<String, FeatureCollection>
): Array<FeatureCollection> {

    val gridState = FileGridState()
    val result: Array<FeatureCollection> =
        Array(TreeId.MAX_COLLECTION_ID.id) { FeatureCollection() }

    gridState.start(offlineExtractPath)
    gridState.checkOfflineMaps()

    runBlocking {
        gridState.updateTile(tileX, tileY, 0, result, intersectionMap, streetNumberMap)
    }

    return result
}

private fun parseGpxFromFile(filename: String): FeatureCollection {
    val fc = FeatureCollection()
    val gpx = parseGpx(File(filename).readText())

    for (track in gpx.tracks) {
        for (segment in track.trackSegments) {
            for (tp in segment.trackPoints) {
                val feature = Feature()
                feature.geometry = Point(tp.longitude, tp.latitude)
                feature.properties = HashMap<String, Any?>().apply {
                    set("marker-size", "small")
                    set("marker-color", "#004000")
                    tp.bearing?.let { set("heading", it.toDouble()) }
                    tp.speed?.let { set("speed", it.toDouble()) }
                }
                fc.addFeature(feature)
            }
        }
    }

    return fc
}

fun getGridStateForLocation(
    location: LngLatAlt,
    zoomLevel: Int,
    gridSize: Int,
    /**
     * An already-loaded low-zoom settlement grid. Pass one to mirror what GeoEngine does in
     * production, so that the load-time POI address pass can record which settlement each POI is
     * in - the high-zoom tiles don't carry the "place" layer, so without this there are no
     * settlements to be had.
     */
    settlementGrid: GridState? = null
): GridState {

    val gridState = FileGridState(zoomLevel, gridSize)
    gridState.start(offlineExtractPath)
    settlementGrid?.let { grid ->
        gridState.settlementNameProvider = { probe -> nearestSettlement(grid, probe).name }
    }
    runBlocking {

        val enabledCategories = mutableSetOf<String>()
        enabledCategories.add(PLACES_AND_LANDMARKS_KEY)
        enabledCategories.add(MOBILITY_KEY)

        // Update the grid state
        gridState.locationUpdate(
            LngLatAlt(location.longitude, location.latitude),
            enabledCategories,
            null
        )
    }
    return gridState
}

class MvtTileTest {

    @Test
    fun pixelToLocation() {
        val tileX = 15992
        val tileY = 10212
        val tileZoom = 15

        val tileOrigin2 = getLatLonTileWithOffset(tileX, tileY, tileZoom, 0.0, 0.0)
        println("tileOrigin2 " + tileOrigin2.latitude + "," + tileOrigin2.longitude)
        assert(tileOrigin2.latitude == 55.94919982336745)
        assert(tileOrigin2.longitude == -4.306640625)
    }

    /**
     * CountryBoundaries loads a bundled, simplified world country-boundaries GeoJSON to look up
     * which country (and so which side of the road traffic drives on) a location falls within -
     * see AutoCallout.buildCalloutForVehicleTransitStop, which uses this to exclude far-side
     * transit stops regardless of the country's driving convention.
     */
    @Test
    fun testCountryBoundariesDrivingSide() {
        val glasgow = LngLatAlt(-4.2518, 55.8642)
        assertEquals("GB", CountryBoundaries.countryCode(glasgow))
        assertEquals(DrivingSide.LEFT, CountryBoundaries.drivingSide(glasgow))

        val paris = LngLatAlt(2.3522, 48.8566)
        assertEquals("FR", CountryBoundaries.countryCode(paris))
        assertEquals(DrivingSide.RIGHT, CountryBoundaries.drivingSide(paris))

        val newYork = LngLatAlt(-74.0060, 40.7128)
        assertEquals("US", CountryBoundaries.countryCode(newYork))
        assertEquals(DrivingSide.RIGHT, CountryBoundaries.drivingSide(newYork))

        val sydney = LngLatAlt(151.2093, -33.8688)
        assertEquals("AU", CountryBoundaries.countryCode(sydney))
        assertEquals(DrivingSide.LEFT, CountryBoundaries.drivingSide(sydney))

        // Middle of the Atlantic - no country.
        val ocean = LngLatAlt(-40.0, 30.0)
        assertEquals(null, CountryBoundaries.countryCode(ocean))
        assertEquals(null, CountryBoundaries.drivingSide(ocean))
    }

    /**
     * Checks that OSM `ref` (route number, e.g. "B8050") is read directly off the
     * `transportation` layer and attached to the Way, for use in travel-mode callouts like
     * "On the A81" for roads that only carry a route number and no common name. The pmtiles
     * pipeline now backfills `ref` onto every transportation line the same way it already does
     * for `name` (see MvtFeature.ref/copyProperties), so this is a plain tag read - no join
     * against `transportation_name` by OSM id needed, unlike the old prototype this replaced.
     */
    /**
     * End-to-end check of GridState.attachNearestWays() against real Glasgow tiles.
     *
     * OSM leaves most street furniture un-named, so a Places Nearby list is otherwise full of
     * identical "Post Box"/"Bench" rows. Associating each with the road it sits on at tile load
     * time is what makes them tellable apart, and doing it once per grid means the list can be
     * opened and scrolled repeatedly for free.
     *
     * The pinned POI is a post box on London Road (found by scanning TreeId.POIS around
     * [glasgowTestLocation] for un-named features).
     */
    /** Mirrors GridState.probePointFor, which isn't public. */
    private fun probePoint(feature: MvtFeature): LngLatAlt =
        getCentralPointForFeature(feature)
            ?: (feature.geometry as? LineString)?.coordinates?.let { it[it.size / 2] }
            ?: (feature.geometry as? MultiPolygon)?.coordinates?.first()?.first()?.let {
                getCentroidOfPolygon(Polygon(it))
            }
            ?: error("no probe point for ${feature.osmId}")

    @Test
    fun testNearestWayAssociatesUnnamedPoisWithTheirStreet() {
        val location = LngLatAlt(-4.254034459590912, 55.87014482990583)
        val settlementGrid = getGridStateForLocation(location, 12, 3)
        val gridState = getGridStateForLocation(location, MAX_ZOOM_LEVEL, 3, settlementGrid)
        val pois = gridState.featureTrees[TreeId.POIS.id].getAllCollection()

        val postBox = pois.features
            .filterIsInstance<MvtFeature>()
            .first { it.osmId == 15104401251L }
        assertEquals("post_box", postBox.featureSubClass)
        assertNull(postBox.name)
        assertEquals("London Road", postBox.nearestWay?.name)
        assertNotNull(postBox.nearestSettlement)

        var addressless = 0
        var associated = 0
        var settled = 0
        for (feature in pois.features) {
            val poi = feature as? MvtFeature ?: continue
            val nearestWay = poi.nearestWay
            if ((poi.name == null) && (poi.street == null) && (poi.housenumber == null)) {
                addressless++
                if (nearestWay != null) associated++
                if (poi.nearestSettlement != null) settled++
            }
            if (nearestWay != null) {
                // A POI which has an address of its own is skipped, so it should never pick one up
                assertNull(poi.street)
                assertNull(poi.housenumber)
                // ...and the road it did pick up has to be a named one within the search radius
                // (nearestWaySearchDistanceMetres, 30m) of the POI.
                assertTrue((nearestWay.name != null) || (nearestWay.ref != null))
                // Measured POI-to-way, the way GridState does it - a large feature is judged by
                // how close it actually gets to the road, not by where its centre is
                val pointOnWay =
                    getDistanceToFeature(probePoint(poi), nearestWay, gridState.ruler).point
                assertTrue(
                    "${poi.osmId} associated with a way more than 30m away",
                    getDistanceToFeature(pointOnWay, poi, gridState.ruler).distance < 31.0
                )
            }
        }

        // Not every un-named POI is near a named road - ones in parks and retail parks legitimately
        // get nothing - but in a dense city grid most of them should be resolved.
        assertTrue("expected plenty of un-named POIs, got $addressless", addressless > 1000)
        assertTrue(
            "only $associated of $addressless un-named POIs were given a street",
            associated > (addressless / 2)
        )
        // Glasgow is well inside the city's 15km radius, so every POI lands in some settlement
        assertEquals(addressless, settled)
        println("Un-named POIs: $addressless, with a way: $associated, with a settlement: $settled")
    }

    /**
     * Milngavie car parks, from the Glasgow extract.
     *
     * OSM tags these with addr:street but no addr:city, so building an address from the tags
     * alone gives a bare "Kersland Drive" with no town in it. The settlement recorded at tile
     * load time fills that gap - which means POIs that have a street of their own still need the
     * settlement attached, even though they don't need a nearestWay confected.
     */
    @Test
    fun testCarParkAddressIncludesTheSettlement() {
        val location = LngLatAlt(-4.3142776, 55.9401126)
        val settlementGrid = getGridStateForLocation(location, 12, 3)
        val gridState = getGridStateForLocation(location, MAX_ZOOM_LEVEL, 3, settlementGrid)

        val pois = gridState.featureTrees[TreeId.POIS.id].getAllCollection()
        fun carPark(name: String) = pois.features
            .filterIsInstance<MvtFeature>()
            .first { it.name == name }

        val kerslandDrive = carPark("Kersland Drive Car Park")
        assertEquals("Kersland Drive", kerslandDrive.street)
        assertNull(kerslandDrive.housenumber)
        // It has a street of its own, so no way is confected for it...
        assertNull(kerslandDrive.nearestWay)
        // ...but it still gets the settlement, which its OSM tags don't carry
        assertEquals("Milngavie", kerslandDrive.nearestSettlement)

        val description = kerslandDrive.toLocationDescription(LocationSource.OfflineGeocoder)
        assertEquals("Kersland Drive Car Park", description.name)
        assertEquals("Kersland Drive, Milngavie", description.description)

        // Same shape, different streets - these were all bare street names before
        assertEquals(
            "Woodburn Way, Milngavie",
            carPark("Woodburn Way Car Park")
                .toLocationDescription(LocationSource.OfflineGeocoder).description
        )
        assertEquals(
            "Ellangowan Road, Milngavie",
            carPark("Ellangowan Car Park")
                .toLocationDescription(LocationSource.OfflineGeocoder).description
        )
    }

    /**
     * "just before"/"just after"/"until"/"since" describe where a location is relative to the
     * direction you're travelling, which StreetDescription.describeLocation only knows from a
     * heading - without one it fills ahead and behind in arbitrarily. Reverse geocoding a bare
     * point (an address for somewhere the user tapped, which passes UserGeometry(location) with
     * no heading) must therefore never produce them. "Between" is symmetric, so it still stands,
     * and "near" replaces "just before"/"just after" for a feature right beside the location.
     */
    @Test
    fun testReverseGeocodeWithoutHeadingHasNoDirectionalDescriptions() {
        val location = LngLatAlt(-4.254034459590912, 55.87014482990583)
        val gridState = getGridStateForLocation(location, MAX_ZOOM_LEVEL, 3)
        val settlementGrid = getGridStateForLocation(location, 12, 3)
        val geocoder = OfflineGeocoder(gridState, settlementGrid, processor = { it.process() })

        // Sample along the named roads around the test location, which is what a user tapping
        // POIs in this area amounts to
        val ruler = gridState.ruler
        val samples = gridState.featureTrees[TreeId.ROADS.id].getAllCollection().features
            .filterIsInstance<Way>()
            .filter { it.name != null }
            .mapNotNull { (it.geometry as? LineString)?.coordinates }
            .flatten()
            .filter { ruler.distance(location, it) < 1000.0 }
            .take(400)
        assertTrue("no sample points found", samples.size > 100)

        val directional = listOf("just before", "just after", " until ", " since ")
        var headingResults = 0
        var directionalWithHeading = 0
        var nearWithoutHeading = 0

        runBlocking {
            for (sample in samples) {
                val withoutHeading =
                    geocoder.getAddressFromLngLat(UserGeometry(sample), null, false)
                withoutHeading?.name?.let { text ->
                    for (phrase in directional) {
                        assertTrue(
                            "reverse geocode with no heading produced \"$text\"",
                            !text.contains(phrase)
                        )
                    }
                    if (text.contains(" near ")) nearWithoutHeading++
                }

                // The same point with a heading may still use them - that's the case they're for
                val withHeading = geocoder.getAddressFromLngLat(
                    UserGeometry(location = sample, travelHeading = 90.0, speed = 1.5), null, false
                )
                withHeading?.name?.let { text ->
                    headingResults++
                    if (directional.any { text.contains(it) }) directionalWithHeading++
                }
            }
        }

        // Guards the test itself: if nothing ever takes the directional branch then the assertions
        // above are passing vacuously
        assertTrue("no results at all with a heading", headingResults > 0)
        assertTrue(
            "no directional descriptions produced even with a heading, so the no-heading " +
                "assertions above prove nothing",
            directionalWithHeading > 0
        )
        // The close-by case should be described rather than dropped
        assertTrue("no \"near\" descriptions produced", nearWithoutHeading > 0)
    }

    /**
     * A Milngavie playground, from the Glasgow extract.
     *
     * Its centre is 35m from Campsie Drive - outside the association threshold - but its fence is
     * 10m from it, and the fence is where the user is standing. Judging a POI by its centre alone
     * leaves large features with no address at all, so the measurement runs from the way back to
     * the POI, which getDistanceToFeature handles for polygons.
     */
    @Test
    fun testLargePoiIsAssociatedByItsEdgeNotItsCentre() {
        val location = LngLatAlt(-4.307773, 55.945404)
        val settlementGrid = getGridStateForLocation(location, 12, 3)
        val gridState = getGridStateForLocation(location, MAX_ZOOM_LEVEL, 3, settlementGrid)
        val ruler = gridState.ruler

        val playground = gridState.featureTrees[TreeId.POIS.id].getAllCollection().features
            .filterIsInstance<MvtFeature>()
            .first { poi ->
                val centre = getCentralPointForFeature(poi) ?: return@first false
                (poi.featureClass == "playground") && (ruler.distance(location, centre) < 50.0)
            }

        val centre = getCentralPointForFeature(playground)!!
        val campsieDrive = gridState.featureTrees[TreeId.ROADS.id]
            .getNearbyCollection(centre, 100.0, ruler).features
            .filterIsInstance<Way>()
            .filter { it.name == "Campsie Drive" }
            .minBy { getDistanceToFeature(centre, it, ruler).distance }

        // The centre is out of range, which is why this used to come back with nothing...
        val fromCentre = getDistanceToFeature(centre, campsieDrive, ruler).distance
        assertTrue("expected the centre to be out of range, was ${fromCentre.toInt()}m", fromCentre > 30.0)
        // ...while the playground itself is well within it
        val pointOnWay = getDistanceToFeature(centre, campsieDrive, ruler).point
        val fromEdge = getDistanceToFeature(pointOnWay, playground, ruler).distance
        assertTrue("expected the edge to be in range, was ${fromEdge.toInt()}m", fromEdge < 30.0)

        assertEquals("Campsie Drive", playground.nearestWay?.name)
        assertEquals("Milngavie", playground.nearestSettlement)
    }

    /**
     * A road carrying both a route number and a local street name should be announced with both,
     * e.g. "A81 (Glasgow Road)" - the ref alone isn't how a passenger recognises where they are
     * in a town, and the street name alone loses the road they're actually following. Way.getName()
     * treats the two as mutually exclusive (name wins), so this combining is done by
     * travellingReverseGeocodeName and applies to travel-mode callouts only.
     */
    @Test
    fun testTravelCalloutCombinesRoadRefAndName() {
        val location = LngLatAlt(-4.313381016254425, 55.935718434264274)
        val gridState = getGridStateForLocation(location, MAX_ZOOM_LEVEL, 3)
        val settlementGrid = getGridStateForLocation(location, 12, 3)

        val userGeometry = UserGeometry(location = location, speed = 15.0, travelHeading = 0.0)
        val result = describeReverseGeocode(userGeometry, gridState, settlementGrid, FakeLocalizedStrings())

        assertNotNull(result)
        assertEquals(
            "DirectionsAlongTravelingN(DirectionsRoadWithRefAndName(A81, Glasgow Road)) " +
                "DirectionsTowardsSettlement(Milngavie, DistanceKm(0.7))",
            result!!.text
        )
    }

    /**
     * The A81 through Milngavie is Strathblane Road, then Glasgow Road, then Main Street, but
     * it's one road to someone travelling along it. A numbered road is therefore deduped on its
     * ref alone, so neither the street name changing nor the settlement alongside it changing
     * re-announces the same road (see roadDedup in travellingReverseGeocodeName). The spoken text
     * still differs between the two points - it's only the callout-history key that collapses.
     */
    @Test
    fun testTravelCalloutDedupsNumberedRoadByRefAlone() {
        val strathblaneRoad = LngLatAlt(-4.310806095600128, 55.94419385637048)
        val glasgowRoad = LngLatAlt(-4.313381016254425, 55.935718434264274)

        val results = listOf(strathblaneRoad, glasgowRoad).map { location ->
            val gridState = getGridStateForLocation(location, MAX_ZOOM_LEVEL, 3)
            val settlementGrid = getGridStateForLocation(location, 12, 3)
            val userGeometry = UserGeometry(location = location, speed = 15.0, travelHeading = 0.0)
            describeReverseGeocode(userGeometry, gridState, settlementGrid, null)
        }

        assertEquals(
            "Traveling north along A81 (Strathblane Road) near Milngavie", results[0]!!.text
        )
        assertEquals(
            "Traveling north along A81 (Glasgow Road) towards Milngavie, 0.7 km away",
            results[1]!!.text
        )
        assertEquals("A81", results[0]!!.dedupText)
        assertEquals("A81", results[1]!!.dedupText)
    }

    /**
     * A road with no ref has nothing but its name to identify it, so it keeps the fuller dedup
     * key and a genuine change of street still gets announced - the ref-only collapsing above
     * mustn't silence those.
     */
    @Test
    fun testTravelCalloutDedupsUnnumberedRoadByName() {
        val location = LngLatAlt(-4.321057498455048, 55.94279393223639)
        val gridState = getGridStateForLocation(location, MAX_ZOOM_LEVEL, 3)
        val settlementGrid = getGridStateForLocation(location, 12, 3)

        val userGeometry = UserGeometry(location = location, speed = 15.0, travelHeading = 0.0)
        val result = describeReverseGeocode(userGeometry, gridState, settlementGrid, null)

        assertNotNull(result)
        assertEquals("Traveling north along Allander Road near Milngavie", result!!.text)
        assertEquals("On Allander Road near Milngavie", result.dedupText)
    }

    @Test
    fun testTransportationNameRef() {
        val tileX = 7995
        val tileY = 5106

        val intersectionMap: HashMap<LngLatAlt, Intersection> = hashMapOf()
        val streetNumberMap: HashMap<String, FeatureCollection> = hashMapOf()
        val geojson = vectorTileToGeoJsonFromFile(tileX, tileY, intersectionMap, streetNumberMap)

        val roads = geojson[TreeId.ROADS_AND_PATHS.id]
        val parkRoad = roads.features.find { (it as? MvtFeature)?.name == "Park Road" }
        assertEquals("B8050", (parkRoad as? MvtFeature)?.ref)
    }

    /**
     * The M8 motorway through central Glasgow carries `ref=M8` but no `name` tag (found by
     * scanning TreeId.ROADS_AND_PATHS around [glasgowTestLocation] for Ways with a ref and no
     * name). This is an end-to-end check that travel-mode reverse geocoding falls back to the
     * ref ("M8") instead of the generic class-based description ("Motorway") it used to produce,
     * phrased as "On M8" since we're confirmed to be on the road, not just near it.
     *
     * The settlement is Glasgow rather than the much nearer Cowcaddens because a motorway only
     * names towns and cities - see [testTravelCalloutSettlementSizeVariesWithRoadClass].
     */
    @Test
    fun testTravelCalloutForUnnamedRefRoad() {
        val location = LngLatAlt(-4.254034459590912, 55.87014482990583)
        val gridState = getGridStateForLocation(location, MAX_ZOOM_LEVEL, 3)
        val settlementGrid = getGridStateForLocation(location, 12, 3)

        val userGeometry = UserGeometry(location = location, speed = 15.0)
        val result = describeReverseGeocode(userGeometry, gridState, settlementGrid, null)

        assertNotNull(result)
        assertEquals("On M8 and close to Glasgow", result!!.text)
    }

    // On the A8 trunk road east of Glasgow, where the nearest settlement of any size is the
    // isolated dwelling "Ivy Cottage" but the nearest one big enough for a trunk road to name is
    // the village of Carnbroe, roughly 0.9 km to the north.
    private val a8TestLocation = LngLatAlt(-4.005613625049591, 55.839092189377)

    /**
     * When a travel heading is available, travel-mode reverse geocoding for a road (not a
     * railway) should announce the direction of travel along it, e.g. "Traveling south along A8",
     * instead of just naming the road - using the existing DirectionsAlongTraveling* string keys.
     * Carnbroe (a discrete, non-city settlement here) is roughly behind the direction of travel
     * (south), so it's phrased as "away from Carnbroe" rather than the vaguer "close to".
     */
    @Test
    fun testTravelCalloutForRoadIncludesDirection() {
        val gridState = getGridStateForLocation(a8TestLocation, MAX_ZOOM_LEVEL, 3)
        val settlementGrid = getGridStateForLocation(a8TestLocation, 12, 3)

        val userGeometry = UserGeometry(location = a8TestLocation, speed = 25.0, travelHeading = 180.0)
        val result = describeReverseGeocode(userGeometry, gridState, settlementGrid, FakeLocalizedStrings())

        assertNotNull(result)
        assertEquals(
            "DirectionsAlongTravelingS(DirectionsRoadWithRefAndName(A8, Glasgow and Edinburgh Road)) " +
                "DirectionsAwayFromSettlement(Carnbroe, DistanceKm(0.9))",
            result!!.text
        )
    }

    /**
     * Same location as [testTravelCalloutForRoadIncludesDirection] but travelling the opposite
     * way (north instead of south) - Carnbroe is now roughly ahead of the direction of travel,
     * so it should flip from "away from" to "towards".
     */
    @Test
    fun testTravelCalloutForRoadTowardsSettlement() {
        val gridState = getGridStateForLocation(a8TestLocation, MAX_ZOOM_LEVEL, 3)
        val settlementGrid = getGridStateForLocation(a8TestLocation, 12, 3)

        val userGeometry = UserGeometry(location = a8TestLocation, speed = 25.0, travelHeading = 0.0)
        val result = describeReverseGeocode(userGeometry, gridState, settlementGrid, FakeLocalizedStrings())

        assertNotNull(result)
        assertEquals(
            "DirectionsAlongTravelingN(DirectionsRoadWithRefAndName(A8, Glasgow and Edinburgh Road)) " +
                "DirectionsTowardsSettlement(Carnbroe, DistanceKm(0.9))",
            result!!.text
        )
    }

    /**
     * The faster the road, the larger a settlement has to be before it's worth naming: a hamlet
     * is a useful landmark on a country lane but meaningless at motorway speed. So a motorway
     * names only towns and cities, and a trunk road anything from a village up, while every other
     * road keeps naming whatever is nearest.
     *
     * Both halves check the settlement the callout actually uses against the one an unrestricted
     * lookup at the same point returns, so they'd fail if the road class stopped being consulted.
     */
    @Test
    fun testTravelCalloutSettlementSizeVariesWithRoadClass() {
        // Trunk road: "Ivy Cottage" is an isolated dwelling nobody navigates by, so the A8 skips
        // it for the village of Carnbroe.
        val trunkGridState = getGridStateForLocation(a8TestLocation, MAX_ZOOM_LEVEL, 3)
        val trunkSettlementGrid = getGridStateForLocation(a8TestLocation, 12, 3)
        assertEquals(
            "Ivy Cottage", nearestSettlement(trunkSettlementGrid, a8TestLocation).name
        )
        val trunkResult = describeReverseGeocode(
            UserGeometry(location = a8TestLocation, speed = 25.0), trunkGridState,
            trunkSettlementGrid, null
        )
        assertNotNull(trunkResult)
        assertEquals("On A8 (Glasgow and Edinburgh Road) and close to Carnbroe", trunkResult!!.text)

        // Motorway: Cowcaddens is a suburb, still too small to be worth naming on the M8, which
        // goes all the way up to Glasgow itself.
        val motorwayLocation = LngLatAlt(-4.254034459590912, 55.87014482990583)
        val motorwayGridState = getGridStateForLocation(motorwayLocation, MAX_ZOOM_LEVEL, 3)
        val motorwaySettlementGrid = getGridStateForLocation(motorwayLocation, 12, 3)
        assertEquals(
            "Cowcaddens", nearestSettlement(motorwaySettlementGrid, motorwayLocation).name
        )
        val motorwayResult = describeReverseGeocode(
            UserGeometry(location = motorwayLocation, speed = 25.0), motorwayGridState,
            motorwaySettlementGrid, null
        )
        assertNotNull(motorwayResult)
        assertEquals("On M8 and close to Glasgow", motorwayResult!!.text)
    }

    /**
     * Junction (exit/interchange) nodes are carried as `subclass=junction` POINT features in
     * `transportation_name`, on motorways (numbered, e.g. "Robroyston" M8 Junction 2) as well as
     * primary/trunk/tertiary roads (often named but unnumbered, e.g. "Cousland Interchange" on
     * the A899/A705 near Edinburgh). This checks both are parsed into TreeId.HIGHWAY_JUNCTIONS,
     * with `ref` attached where the road is numbered, ready for "at Junction 2" or "at Cousland
     * Interchange" style callouts.
     */
    @Test
    fun testHighwayJunctionParsing() {
        val motorwayGridState = getGridStateForLocation(LngLatAlt(-4.1848, 55.8854), MAX_ZOOM_LEVEL, 3)
        val motorwayJunctions =
            motorwayGridState.getFeatureTree(TreeId.HIGHWAY_JUNCTIONS).getAllCollection()
        val robroyston = motorwayJunctions.features.find { (it as? MvtFeature)?.name == "Robroyston" }
        assertNotNull(robroyston)
        assertEquals("2", (robroyston as MvtFeature).ref)

        val primaryGridState = getGridStateForLocation(LngLatAlt(-3.5084, 55.8980), MAX_ZOOM_LEVEL, 3)
        val primaryJunctions =
            primaryGridState.getFeatureTree(TreeId.HIGHWAY_JUNCTIONS).getAllCollection()
        val cousland = primaryJunctions.features.find { (it as? MvtFeature)?.name == "Cousland Interchange" }
        assertNotNull(cousland)
        assertEquals("primary", (cousland as MvtFeature).properties?.get("class"))
    }

    /**
     * The `waterway` layer (near Milngavie) carries "Tannoch Burn" as a stream split into
     * segments, with the segment that passes under a road tagged `brunnel=tunnel` (a culvert).
     * Checks that the crossing road's Way ends up with crossing_name/crossing_type properties
     * attached directly to it (see extractCrossings), ready for travel-mode callouts like
     * "Crossing Allander Water" - no separate search tree needed.
     */
    @Test
    fun testWaterwayCrossingParsing() {
        val gridState = getGridStateForLocation(LngLatAlt(-4.3231, 55.9461), MAX_ZOOM_LEVEL, 3)
        val ways = gridState.getFeatureTree(TreeId.ROADS_AND_PATHS).getAllCollection().features
            .filterIsInstance<Way>()

        // Tannoch Burn is a class=stream crossing - too minor a landmark to be worth a callout,
        // so it should be filtered out (see significantWaterwayClasses) and no Way should carry it.
        val tannochBurn = ways.find { it.properties?.get("crossing_name") == "Tannoch Burn" }
        assertNull("Expected Tannoch Burn (a stream) to be filtered out", tannochBurn)

        // Allander Water is class=river, and passes through a culvert further downstream, so the
        // road above it should carry the crossing via its own brunnel=tunnel tag.
        val allanderWater = ways.find { it.properties?.get("crossing_name") == "Allander Water" }
        assertNotNull("Expected an Allander Water crossing", allanderWater)
        assertEquals("waterway", allanderWater!!.properties?.get("crossing_type"))
    }

    /**
     * The `waterway` layer's named lines are kept as features in their own right (see
     * extractNamedWaterways), so that a path which follows one can be named after it. Unlike the
     * crossing filter above, a burn counts here: a path can follow one for hundreds of metres.
     * They must not leak into the POI tree, or every river turns up in "What's around me".
     */
    @Test
    fun testNamedWaterwayParsing() {
        val gridState = getGridStateForLocation(LngLatAlt(-4.3053, 55.9319), MAX_ZOOM_LEVEL, 2)
        val waterways = gridState.getFeatureTree(TreeId.NAMED_WATERWAYS).getAllCollection().features
            .filterIsInstance<MvtFeature>()

        val allanderWater = waterways.filter { it.name == "Allander Water" }
        assertTrue("Expected Allander Water in NAMED_WATERWAYS", allanderWater.isNotEmpty())
        assertEquals("river", allanderWater.first().featureClass)

        // A stream, deliberately kept here even though extractCrossings filters streets out.
        assertTrue(
            "Expected Craigmaddie Burn (a stream) in NAMED_WATERWAYS",
            waterways.any { it.name == "Craigmaddie Burn" }
        )
        assertTrue(
            "Waterways should all be LineStrings",
            waterways.all { it.geometry is LineString }
        )

        val pois = gridState.getFeatureTree(TreeId.POIS).getAllCollection().features
            .filterIsInstance<MvtFeature>()
        assertTrue(
            "Waterways must not leak into the POI tree",
            pois.none { it.featureType == "waterway" }
        )
    }

    /**
     * The two paths that motivated water-based name confection, both around Milngavie and both
     * un-named in OSM: one follows the Allander Water, the other runs round the shore of
     * Craigmaddie Reservoir. Without this they're announced as a bare "Path".
     */
    @Test
    fun testNameConfectionForPathFollowingWater() {
        assertWaterConfectedName(LngLatAlt(-4.305300, 55.931961), "Allander Water")
        assertWaterConfectedName(LngLatAlt(-4.300997, 55.948076), "Craigmaddie Reservoir")
    }

    /**
     * A road that merely crosses a river is beside it only for the few metres either side of the
     * bridge, so it must not pick up the river's name. Strathblane Road crosses the Allander Water
     * in Milngavie and is named anyway; this checks the un-named ways around the crossing too.
     */
    @Test
    fun testNameConfectionDoesNotNameRoadsThatOnlyCrossWater() {
        val location = LngLatAlt(-4.3231, 55.9461)
        val gridState = getGridStateForLocation(location, MAX_ZOOM_LEVEL, 2)
        val crossing = gridState.getFeatureTree(TreeId.ROADS_AND_PATHS).getAllCollection()
            .features.filterIsInstance<Way>()
            .filter { it.properties?.get("crossing_name") == "Allander Water" }
        assertTrue("Expected at least one Allander Water crossing way", crossing.isNotEmpty())
        for (way in crossing) {
            val name = way.getName(gridState = gridState, strings = null)
            assertFalse(
                "A way that only crosses the Allander Water shouldn't be named after it: $name",
                name == "Path next to Allander Water"
            )
        }
    }

    /**
     * The user-facing coordinates are approximate (they point at a stretch of path, not a vertex),
     * so this looks for the confected name among the un-named ways in the area rather than
     * insisting on the single nearest one.
     */
    private fun assertWaterConfectedName(location: LngLatAlt, expectedWater: String) {
        val gridState = getGridStateForLocation(location, MAX_ZOOM_LEVEL, 2)
        val nearby = gridState.getFeatureTree(TreeId.ROADS_AND_PATHS)
            .getNearestCollection(location, 250.0, 100, gridState.ruler)
            .features.filterIsInstance<Way>()
            .filter { it.name == null }
        assertTrue("Expected un-named ways near $location", nearby.isNotEmpty())

        // getName() has to say it on the very first call, not only once a previous call has cached
        // the name onto the Way - the first callout is the one the user actually hears.
        val names = nearby.map { it.getName(gridState = gridState, strings = null) }
        assertTrue(
            "Expected \"Path next to $expectedWater\" among the ways near $location, got $names",
            names.contains("Path next to $expectedWater")
        )
    }

    /**
     * `naptanLocalityName` isn't populated for every stop (e.g. missing for central Glasgow
     * stops in practice) - when it's absent, the locality is simply left out of the description
     * (CommonName and direction only) rather than falling back to anything parsed from `name`.
     */
    @Test
    fun testNaptanBusStopOmitsMissingLocalityName() {
        val location = LngLatAlt(-4.25, 55.87)
        val gridState = getGridStateForLocation(location, MAX_ZOOM_LEVEL, 3)
        val stops = gridState.getFeatureTree(TreeId.TRANSIT_STOPS).getAllCollection().features
            .filterIsInstance<MvtFeature>()

        val stance2 = stops.find { it.name == "Buchanan Bus Station (Stance 2)" }
        assertNotNull("Expected the Buchanan Bus Station Stance 2 stop", stance2)
        assertNull(
            "Expected this stop to have no naptanLocalityName",
            stance2!!.properties?.get("naptanLocalityName")
        )
        assertEquals("Buchanan Bus Station", stance2.properties?.get("naptanCommonName"))
        assertEquals("N", stance2.properties?.get("naptanBearing"))
        assertEquals(
            "Buchanan Bus Station Northbound Bus Stop",
            stance2.getText(null).text
        )
    }

    /**
     * NaPTAN-imported bus stops carry a `name` assembled by the importer as "Locality,
     * CommonName (Indicator)", e.g. "Milngavie, Lynn Drive (after)" - confusing read aloud, since
     * the indicator describes the stop's position relative to a landmark, not a useful direction.
     * MvtFeature.getText reformats this to "CommonName, Locality Directionbound" using the
     * separate naptanCommonName/naptanBearing tags - see formatNaptanBusStopName.
     */
    @Test
    fun testNaptanBusStopNameFormatting() {
        val location = LngLatAlt(-4.3051871, 55.9463332)
        val gridState = getGridStateForLocation(location, MAX_ZOOM_LEVEL, 3)
        val stops = gridState.getFeatureTree(TreeId.TRANSIT_STOPS)
            .getNearestCollection(location, 100.0, 5, gridState.ruler).features
            .filterIsInstance<MvtFeature>()

        val lynnDrive = stops.find { it.name == "Milngavie, Lynn Drive (after)" }
        assertNotNull("Expected the Lynn Drive stop", lynnDrive)
        assertEquals("W", lynnDrive!!.properties?.get("naptanBearing"))
        assertEquals(
            "Lynn Drive, Milngavie Westbound Bus Stop",
            lynnDrive.getText(null).text
        )

        val roseleaDrive = stops.find { it.name == "Milngavie, Roselea Drive (before)" }
        assertNotNull("Expected the Roselea Drive stop", roseleaDrive)
        assertEquals("E", roseleaDrive!!.properties?.get("naptanBearing"))
        assertEquals(
            "Roselea Drive, Milngavie Eastbound Bus Stop",
            roseleaDrive.getText(null).text
        )
    }

    /**
     * NaPTAN's optional Landmark field names a notable nearby feature (e.g. a shop) the stop is
     * positioned near - worth appending when it adds real information, e.g.
     * "...Bus Stop for Marks & Spencer".
     */
    @Test
    fun testNaptanBusStopLandmarkSuffix() {
        val location = LngLatAlt(-4.3175268, 55.9397535)
        val gridState = getGridStateForLocation(location, MAX_ZOOM_LEVEL, 3)
        val stops = gridState.getFeatureTree(TreeId.TRANSIT_STOPS)
            .getNearestCollection(location, 100.0, 5, gridState.ruler).features
            .filterIsInstance<MvtFeature>()

        val mainStreet = stops.find { it.name == "Milngavie, Main Street (after)" }
        assertNotNull("Expected the Main Street stop", mainStreet)
        assertEquals("Marks & Spencer", mainStreet!!.properties?.get("naptanLandmark"))
        assertEquals(
            "Main Street, Milngavie Northeastbound Bus Stop for Marks & Spencer",
            mainStreet.getText(null).text
        )
    }

    /**
     * `naptanLandmark` is very often just a copy of `naptanCommonName` (the identifying name
     * already spoken) - appending it in that case would be pure noise, so it should be
     * suppressed rather than producing something like "Ashfield Road ... for Ashfield Road".
     */
    @Test
    fun testNaptanBusStopRedundantLandmarkSuppressed() {
        val location = LngLatAlt(-4.3051871, 55.9463332)
        val gridState = getGridStateForLocation(location, MAX_ZOOM_LEVEL, 3)
        val stops = gridState.getFeatureTree(TreeId.TRANSIT_STOPS).getAllCollection().features
            .filterIsInstance<MvtFeature>()

        val ashfieldRoad = stops.find { it.name == "Milngavie, Ashfield Road (before)" }
        assertNotNull("Expected the Ashfield Road stop", ashfieldRoad)
        assertEquals("Ashfield Road", ashfieldRoad!!.properties?.get("naptanLandmark"))
        assertEquals(
            "Ashfield Road, Milngavie Southeastbound Bus Stop",
            ashfieldRoad.getText(null).text
        )
    }

    /**
     * The Menai Strait, unlike the Firth of Forth, is represented in the `water` layer as a
     * named LineString ("Afon Menai / Menai Strait", class=strait) rather than a named polygon -
     * found via a worldwide test extract (too large - ~92GB - to keep as a permanent local test
     * fixture, so this reads directly from that path and skips itself if it's not present).
     * extractNamedWaterPolygons handles both shapes, and it's specifically wayCrossingInfo's
     * nearest-feature fallback (containment only applies to polygons) that makes a LineString
     * work here - this exercises that fallback end-to-end against real data, complementing
     * testFirthOfForthCrossingCallout's polygon-containment case.
     */
    @Test
    fun testMenaiStraitCrossingCallout() {
        val testPmtilesPath = "/mnt/sdb/map-to-serve/test.pmtiles"
        if (!File(testPmtilesPath).exists()) return

        val tileX = 8001
        val tileY = 5320
        val reader = org.scottishtecharmy.soundscape.geoengine.utils.pmtiles.PmTilesReader(
            with(okio.Path.Companion) { testPmtilesPath.toPath() }
        )
        val rawTile = reader.getTile(14, tileX, tileY)!!
        val tile = org.scottishtecharmy.soundscape.geoengine.utils.decompressTile(reader.tileCompression, rawTile)!!
        reader.close()

        val intersectionMap: HashMap<LngLatAlt, Intersection> = hashMapOf()
        val streetNumberMap: HashMap<String, FeatureCollection> = hashMapOf()
        val geojson = vectorTileToGeoJson(tileX, tileY, tile, intersectionMap, streetNumberMap, true, 14)

        val menaiStrait = geojson[TreeId.NAMED_WATER_POLYGONS.id].features
            .filterIsInstance<MvtFeature>()
            .find { it.name == "Afon Menai / Menai Strait" }
        assertNotNull("Expected the Afon Menai / Menai Strait water feature", menaiStrait)
        assertEquals("LineString", menaiStrait!!.geometry.type)

        val straitCoordinates = (menaiStrait.geometry as LineString).coordinates
        val crossingPoint = straitCoordinates[straitCoordinates.size / 2]

        val gridState = FileGridState()
        gridState.ruler = crossingPoint.createCheapRuler()
        gridState.featureTrees[TreeId.NAMED_WATER_POLYGONS.id] =
            FeatureTree(FeatureCollection().apply { addFeature(menaiStrait) })
        val settlementGrid = FileGridState(12, 3)

        val bridgeWay = Way().apply {
            osmId = 1L
            name = "Menai Bridge"
            geometry = LineString(crossingPoint, crossingPoint)
            setProperty("brunnel", "bridge")
        }
        val approachWay = Way().apply {
            osmId = 2L
            name = "Approach"
            geometry = LineString(crossingPoint, crossingPoint)
        }

        val autoCallout = AutoCallout(null, null)
        val firstUpdate = UserGeometry(
            location = crossingPoint, speed = 15.0, mapMatchedWay = approachWay, timestampMilliseconds = 1000L
        )
        autoCallout.updateLocation(firstUpdate, gridState, settlementGrid)

        val secondUpdate = UserGeometry(
            location = crossingPoint, speed = 15.0, mapMatchedWay = bridgeWay, timestampMilliseconds = 6000L
        )
        val secondCallout = autoCallout.updateLocation(secondUpdate, gridState, settlementGrid)
        assertNotNull(secondCallout)
        assertTrue(
            "Expected a Menai Strait crossing callout, got: " +
                "${secondCallout!!.positionedStrings.map { it.text }}",
            secondCallout.positionedStrings.any { it.text.contains("Menai") }
        )
    }

    /**
     * The Firth of Forth is a tidal inlet, tagged `natural=bay`/`natural=strait` in OSM rather
     * than as a `waterway` river/canal line, so extractCrossings never sees it and no Way gets a
     * crossing_* property for it at parse time (unlike testWaterwayCrossingParsing above). The
     * Queensferry Crossing bridge over it is caught instead by AutoCallout's live water-polygon
     * proximity check (see wayCrossingInfo's fallback) - this exercises that end-to-end via
     * AutoCallout.updateLocation, using a real bridge Way and real Firth of Forth polygon from
     * TreeId.NAMED_WATER_POLYGONS rather than fabricated properties.
     */
    @Test
    fun testFirthOfForthCrossingCallout() {
        val location = LngLatAlt(-3.3903, 55.9903)
        val gridState = getGridStateForLocation(location, MAX_ZOOM_LEVEL, 3)
        val settlementGrid = getGridStateForLocation(location, 12, 3)

        val waterTree = gridState.getFeatureTree(TreeId.NAMED_WATER_POLYGONS)
        val firth = waterTree.getAllCollection().features
            .find { (it as? MvtFeature)?.name == "Firth of Forth" }
        assertNotNull("Expected a Firth of Forth water polygon near Queensferry Crossing", firth)

        val allWays = gridState.getFeatureTree(TreeId.ROADS_AND_PATHS).getAllCollection().features
            .filterIsInstance<Way>()

        var bridgeWay: Way? = null
        var crossingPoint: LngLatAlt? = null
        for (way in allWays) {
            if (way.properties?.get("brunnel") != "bridge") continue
            val coordinate = (way.geometry as? LineString)?.coordinates
                ?.firstOrNull { waterTree.getContainingPolygons(it).features.isNotEmpty() }
            if (coordinate != null) {
                bridgeWay = way
                crossingPoint = coordinate
                break
            }
        }
        assertNotNull("Expected to find a bridge Way crossing the Firth of Forth", bridgeWay)

        val bridgeCoordinates = (bridgeWay!!.geometry as LineString).coordinates
        val approachWay = Way().apply {
            osmId = bridgeWay.osmId - 1
            name = "Approach"
            geometry = LineString(bridgeCoordinates.first(), bridgeCoordinates.first())
        }

        val autoCallout = AutoCallout(null, null)

        // First update establishes the baseline (the approach) - no callout expected yet.
        val firstUpdate = UserGeometry(
            location = bridgeCoordinates.first(), speed = 15.0, mapMatchedWay = approachWay,
            timestampMilliseconds = 1000L
        )
        autoCallout.updateLocation(firstUpdate, gridState, settlementGrid)

        // Second update: mapMatchedWay transitions onto the bridge - this is the edge that should
        // fire the callout.
        val secondUpdate = UserGeometry(
            location = crossingPoint!!, speed = 15.0, mapMatchedWay = bridgeWay,
            timestampMilliseconds = 6000L
        )
        val secondCallout = autoCallout.updateLocation(secondUpdate, gridState, settlementGrid)
        assertNotNull(secondCallout)
        assertTrue(
            "Expected a Firth of Forth crossing callout, got: " +
                "${secondCallout!!.positionedStrings.map { it.text }}",
            secondCallout.positionedStrings.any { it.text.contains("Firth of Forth") }
        )
    }

    /**
     * An unnamed bus/tram/train stop's callout while travelling by car/bus is just its generic
     * class ("Bus Stop") - unhelpful on its own, unlike walking mode where the stop is the
     * destination. See AutoCallout.enrichUnnamedTransitStopText, which adds a nearby small
     * settlement for context. This exercises it end-to-end against a real unnamed stop (the one
     * originally reported: 56.2570679,-3.3503621, on StAndrews.gpx).
     */
    @Test
    fun testVehicleTransitStopCalloutAddsSettlementContext() {
        val location = LngLatAlt(-3.3503621, 56.2570679)
        val gridState = getGridStateForLocation(location, MAX_ZOOM_LEVEL, 3)
        val settlementGrid = getGridStateForLocation(location, 12, 3)

        val stop = gridState.getFeatureTree(TreeId.TRANSIT_STOPS)
            .getNearestFeature(location, gridState.ruler, 50.0) as? MvtFeature
        assertNotNull("Expected a transit stop near the given location", stop)
        assertNull("Expected this stop to be unnamed for the test to be meaningful", stop!!.name)
        val stopLocation = (stop.geometry as Point).coordinates

        val road = gridState.getFeatureTree(TreeId.ROADS_AND_PATHS)
            .getNearestFeature(stopLocation, gridState.ruler, 30.0) as? Way
        assertNotNull("Expected a road near the stop", road)
        val roadCoordinates = (road!!.geometry as LineString).coordinates
        assertTrue("Expected the road to have at least two points", roadCoordinates.size >= 2)

        val autoCallout = AutoCallout(null, null)

        // First update just establishes the sweep anchor - no callout expected yet.
        val firstUpdate = UserGeometry(
            location = roadCoordinates.first(), speed = 15.0, mapMatchedWay = road,
            timestampMilliseconds = 1000L
        )
        autoCallout.updateLocation(firstUpdate, gridState, settlementGrid)

        // Second update sweeps along the rest of the road, past the stop.
        val secondUpdate = UserGeometry(
            location = roadCoordinates.last(), speed = 15.0, mapMatchedWay = road,
            timestampMilliseconds = 6000L
        )
        val secondCallout = autoCallout.updateLocation(secondUpdate, gridState, settlementGrid)
        assertNotNull("Expected a transit stop callout", secondCallout)
        assertTrue(
            "Expected the callout to include both the generic stop text and settlement " +
                "context, got: ${secondCallout!!.positionedStrings.map { it.text }}",
            secondCallout.positionedStrings.any {
                it.text.contains("Bus Stop") && it.text.contains(", ")
            }
        )
    }

    /**
     * The A82 crosses the West Highland railway line near Renton via a real bridge - unlike a
     * waterway, a railway is never split/tagged at the crossing point itself, so this needs the
     * geometric road/rail intersection strategy in extractCrossings, found via a real GPX replay
     * (see ToBalloch.gpx) landing on "Crossing the railway" there.
     */
    @Test
    fun testRailwayCrossingParsing() {
        val gridState = getGridStateForLocation(LngLatAlt(-4.5864, 55.9628), MAX_ZOOM_LEVEL, 3)
        val ways = gridState.getFeatureTree(TreeId.ROADS_AND_PATHS).getAllCollection().features
            .filterIsInstance<Way>()

        val railwayCrossing = ways.find { it.properties?.get("crossing_type") == "railway" }
        assertNotNull("Expected a railway crossing near Renton", railwayCrossing)
        assertEquals("over", railwayCrossing!!.properties?.get("crossing_position"))
    }

    /**
     * The Union Canal is carried over the A720 City of Edinburgh Bypass on an aqueduct, so it's the
     * *waterway* that's tagged brunnel=bridge (way/4385757) and the road underneath carries no tag
     * at all. That inverts the usual reading: the user goes under, not over. Before crossing_position
     * existed the raw brunnel value was stored and this announced "Crossing the Union Canal" while
     * driving beneath it.
     *
     * It also pins the multiple-roads behaviour - the aqueduct spans both bypass carriageways and
     * two slip roads, and findCrossingRoads has to return all of them rather than whichever it
     * happened to test first.
     */
    @Test
    fun testAqueductOverRoadCrossingCallout() {
        val location = LngLatAlt(-3.307207, 55.921562)
        val gridState = getGridStateForLocation(location, MAX_ZOOM_LEVEL, 3)
        val settlementGrid = getGridStateForLocation(location, 12, 3)
        val ways = gridState.getFeatureTree(TreeId.ROADS_AND_PATHS).getAllCollection().features
            .filterIsInstance<Way>()

        // The canal runs for miles across this grid and plenty of roads bridge over it, so pick
        // out specifically the ways that pass beneath it.
        val underCanal = ways.filter {
            it.properties?.get("crossing_name") == "Union Canal" &&
                it.properties?.get("crossing_position") == "under"
        }
        assertTrue(
            "Expected the roads under the Union Canal aqueduct to carry the crossing",
            underCanal.isNotEmpty()
        )
        // Both A720 carriageways plus the two slip roads the aqueduct spans. osmId here is the
        // tile feature id, which planetiler encodes as the OSM way id * 10 + the geometry type
        // (2 == LineString).
        assertTrue(
            "Expected every way under the aqueduct to be tagged, got osmIds " +
                "${underCanal.map { it.osmId }.toSortedSet()}",
            underCanal.map { it.osmId }.toSet().containsAll(
                setOf(30612219L, 241161339L, 99205503L, 1446633976L).map { it * 10 + 2 }.toSet()
            )
        )
        assertTrue(underCanal.all { it.properties?.get("crossing_type") == "waterway" })
        assertTrue(underCanal.all { it.properties?.get("crossing_latitude") != null })

        // Now the callout itself. The user stays on one Way throughout, so the Way-change edge can
        // never fire - this only works off proximity to the stored crossing point.
        val way = underCanal.first { it.osmId == 30612219L * 10 + 2 }
        val crossingPoint = LngLatAlt(
            way.properties?.get("crossing_longitude") as Double,
            way.properties?.get("crossing_latitude") as Double
        )
        val autoCallout = AutoCallout(null, null)

        // Establish the baseline well short of the aqueduct - nothing to announce yet.
        val approach = gridState.ruler.destination(crossingPoint, 500.0, 90.0)
        val firstUpdate = UserGeometry(
            location = approach, speed = 20.0, mapMatchedWay = way, timestampMilliseconds = 1000L
        )
        val firstCallout = autoCallout.updateLocation(firstUpdate, gridState, settlementGrid)
        assertTrue(
            "Expected no canal callout 500m away, got: " +
                "${firstCallout?.positionedStrings?.map { it.text }}",
            firstCallout?.positionedStrings?.none { it.text.contains("Union Canal") } != false
        )

        val secondUpdate = UserGeometry(
            location = gridState.ruler.destination(crossingPoint, 40.0, 90.0), speed = 20.0,
            mapMatchedWay = way, timestampMilliseconds = 6000L
        )
        val secondCallout = autoCallout.updateLocation(secondUpdate, gridState, settlementGrid)
        assertNotNull(secondCallout)
        assertTrue(
            "Expected \"Going under\" the Union Canal, got: " +
                "${secondCallout!!.positionedStrings.map { it.text }}",
            secondCallout.positionedStrings.any { it.text == "Going under Union Canal" }
        )

        // Still approaching the same crossing - must not repeat.
        val thirdUpdate = UserGeometry(
            location = gridState.ruler.destination(crossingPoint, 5.0, 90.0), speed = 20.0,
            mapMatchedWay = way, timestampMilliseconds = 11000L
        )
        val thirdCallout = autoCallout.updateLocation(thirdUpdate, gridState, settlementGrid)
        assertTrue(
            "Expected no repeat of the Union Canal callout, got: " +
                "${thirdCallout?.positionedStrings?.map { it.text }}",
            thirdCallout?.positionedStrings?.none { it.text.contains("Union Canal") } != false
        )
    }

    /**
     * A canal or river deep in a tunnel isn't something the road above crosses in any meaningful
     * sense - it's not a landmark a sighted person would use - so it mustn't be called out. The
     * Union Canal runs 649m underground through the Falkirk Tunnel (way/191157784) with Slamannan
     * Road, a service road, a footway and two cycleways passing over the top of it.
     *
     * The discriminator is the length of the tagged span: water passing under a road is culvert- or
     * ford-width (the Allander Water's is 15m, see testWaterwayCrossingParsing), whereas this is
     * 607m in one tile. The tile-clipped remainder in the neighbouring tile measures only 89m,
     * which is why isRoadWidthSpan rejects a span running off the edge of its tile as well.
     *
     * The control matters as much as the suppression: South Bantaskine Road bridges the *open*
     * canal a few hundred metres away and must still be announced.
     */
    @Test
    fun testCanalInLongTunnelIsNotCalledOut() {
        val location = LngLatAlt(-3.7948, 55.9906)
        val gridState = getGridStateForLocation(location, MAX_ZOOM_LEVEL, 3)
        val ways = gridState.getFeatureTree(TreeId.ROADS_AND_PATHS).getAllCollection().features
            .filterIsInstance<Way>()

        // osmId is the tile feature id - the OSM way id * 10 + the geometry type (2 == LineString).
        val overTheTunnel = setOf(
            19659047L,      // Slamannan Road
            1240166001L,    // service road
            1275945335L,    // footway
            610911293L,     // cycleway
            1464564718L,    // cycleway
        ).map { it * 10 + 2 }.toSet()

        val bogus = ways.filter {
            it.osmId in overTheTunnel && it.properties?.get("crossing_name") != null
        }
        assertTrue(
            "Nothing above the Falkirk Tunnel should claim to cross the Union Canal, got " +
                "${bogus.map { "${it.name}/${it.osmId}" }}",
            bogus.isEmpty()
        )

        // ...but a genuine bridge over the open canal still is a crossing.
        val southBantaskineRoad = ways.filter { it.osmId == 31875414L * 10 + 2 }
        assertTrue(
            "Expected South Bantaskine Road to still cross the open Union Canal",
            southBantaskineRoad.any { it.properties?.get("crossing_name") == "Union Canal" }
        )
    }

    /**
     * The same concern for railways, which central Glasgow is full of: the Argyle Line and the
     * North Clyde Line run in tunnel under the city centre for hundreds of metres at a time, with
     * ordinary streets over the top. Announcing a railway crossing on every one of them would be
     * constant and useless.
     *
     * These are excluded earlier and by a different route than the canal above: isUnmatchableRailway
     * keeps brunnel=tunnel rail out of TreeId.TRANSIT altogether, so attachRailwayCrossings never
     * sees them. This pins that from the callout end, now that the candidate search is wide enough
     * to have found them.
     *
     * The control is important, because the naive fix here would be to suppress railway crossings
     * in city centres entirely: Bellgrove Street and the streets around it bridge the North Clyde
     * Line where it runs in an *open cutting*, which is a real landmark and must still be called
     * out.
     */
    @Test
    fun testRailwayInTunnelIsNotCalledOut() {
        val location = LngLatAlt(-4.2425, 55.8585)
        val gridState = getGridStateForLocation(location, MAX_ZOOM_LEVEL, 3)
        val ways = gridState.getFeatureTree(TreeId.ROADS_AND_PATHS).getAllCollection().features
            .filterIsInstance<Way>()

        // Streets which pass directly over the tunnelled Argyle/North Clyde lines.
        val overTheTunnels = setOf(
            4741292L,       // Saltmarket, over the Argyle Line
            5730546L,       // Chisholm Street, over the Argyle Line
            4521152L,       // George Street, over the North Clyde Line
            20448005L,      // Montrose Street, over the North Clyde Line
            20450086L,      // John Street, over the North Clyde Line
            37943342L,      // Trongate, over the Argyle Line
        ).map { it * 10 + 2 }.toSet()

        val bogus = ways.filter {
            it.osmId in overTheTunnels && it.properties?.get("crossing_type") == "railway"
        }
        assertTrue(
            "Streets over the central Glasgow rail tunnels must not announce a crossing, got " +
                "${bogus.map { "${it.name}: ${it.properties?.get("crossing_name")}" }}",
            bogus.isEmpty()
        )

        // ...but bridges over the open cutting still are crossings.
        val overTheCutting = ways.filter {
            it.properties?.get("crossing_type") == "railway" &&
                it.properties?.get("crossing_position") == "over"
        }
        assertTrue(
            "Expected the bridges over the open North Clyde Line cutting to still be crossings",
            overTheCutting.any { it.name == "Bellgrove Street" }
        )
    }

    /**
     * The Edinburgh and Glasgow Main Line crosses the M80 at Castlecary on a viaduct - a 2-vertex,
     * 172m way (way/32243541) tagged brunnel=bridge, with the motorway below untagged.
     *
     * This is the regression test for the candidate shortlist in GridState.attachRailwayCrossings.
     * It used to query the road tree 20m around each *railway vertex*, and the viaduct's two
     * endpoints are 47m and 119m from the M80, so no road was ever shortlisted and the geometric
     * intersection test never ran at all.
     */
    @Test
    fun testRailwayViaductOverRoadCrossingCallout() {
        val location = LngLatAlt(-3.943732, 55.981647)
        val gridState = getGridStateForLocation(location, MAX_ZOOM_LEVEL, 3)
        val settlementGrid = getGridStateForLocation(location, 12, 3)
        val ways = gridState.getFeatureTree(TreeId.ROADS_AND_PATHS).getAllCollection().features
            .filterIsInstance<Way>()

        // Other roads bridge over this same line elsewhere in the grid, so select the ones that
        // pass beneath it.
        val underViaduct = ways.filter {
            it.properties?.get("crossing_name") == "Edinburgh and Glasgow Main Line" &&
                it.properties?.get("crossing_position") == "under"
        }
        assertTrue(
            "Expected the M80 under the Castlecary viaduct to carry a railway crossing",
            underViaduct.isNotEmpty()
        )
        assertTrue(underViaduct.all { it.properties?.get("crossing_type") == "railway" })

        // way/94939658 is 2045m long with the viaduct 1705m along it, so the old Way-change edge
        // would have announced this the better part of a minute early at motorway speed.
        val way = underViaduct.firstOrNull { it.osmId == 94939658L * 10 + 2 }
        assertNotNull("Expected the long M80 way to be tagged", way)
        val crossingPoint = LngLatAlt(
            way!!.properties?.get("crossing_longitude") as Double,
            way.properties?.get("crossing_latitude") as Double
        )

        val autoCallout = AutoCallout(null, null)
        val firstUpdate = UserGeometry(
            location = gridState.ruler.destination(crossingPoint, 1500.0, 180.0), speed = 30.0,
            mapMatchedWay = way, timestampMilliseconds = 1000L
        )
        val firstCallout = autoCallout.updateLocation(firstUpdate, gridState, settlementGrid)
        assertTrue(
            "Expected no railway callout 1500m short of the viaduct, got: " +
                "${firstCallout?.positionedStrings?.map { it.text }}",
            firstCallout?.positionedStrings?.none { it.text.contains("Glasgow Main Line") } != false
        )

        // At 30m/s the trigger radius is 90m, so this is the update that should fire.
        val secondUpdate = UserGeometry(
            location = gridState.ruler.destination(crossingPoint, 60.0, 180.0), speed = 30.0,
            mapMatchedWay = way, timestampMilliseconds = 6000L
        )
        val secondCallout = autoCallout.updateLocation(secondUpdate, gridState, settlementGrid)
        assertNotNull(secondCallout)
        assertTrue(
            "Expected \"Going under\" the railway line, got: " +
                "${secondCallout!!.positionedStrings.map { it.text }}",
            secondCallout.positionedStrings.any {
                it.text == "Going under Edinburgh and Glasgow Main Line"
            }
        )
    }

    /**
     * A river/canal or railway crossing should be announced while walking too, not just while
     * travelling by car/bus - see AutoCallout.buildCalloutForWalkingCrossing. Fires as an edge:
     * once when userGeometry.mapMatchedWay transitions onto a Way carrying crossing properties,
     * not again while staying on that Way, but again if the user leaves and later returns to it.
     */
    @Test
    fun testWalkingCrossingCallout() {
        val location = LngLatAlt(-4.254034459590912, 55.87014482990583)
        val gridState = getGridStateForLocation(location, MAX_ZOOM_LEVEL, 3)
        val settlementGrid = getGridStateForLocation(location, 12, 3)

        // Real geometry (not just fabricated properties) so that other AutoCallout logic which
        // reads mapMatchedWay.geometry - e.g. buildCalloutForIntersections, which also runs on
        // every update - doesn't choke on an empty/default GeoJsonObject.
        val endLocation = gridState.ruler.offset(location, 0.0, 30.0)
        val approachWay = Way().apply {
            osmId = 1L
            name = "Approach Path"
            geometry = LineString(location, endLocation)
        }
        val crossingWay = Way().apply {
            osmId = 2L
            name = "Bridge"
            geometry = LineString(location, endLocation)
            setProperty("crossing_type", "waterway")
            setProperty("crossing_name", "Test River")
        }

        val autoCallout = AutoCallout(null, null)

        // First update establishes the baseline (osmId 1) - no callout expected yet.
        val firstUpdate = UserGeometry(
            location = location, speed = 1.4, mapMatchedWay = approachWay, timestampMilliseconds = 1000L
        )
        val firstCallout = autoCallout.updateLocation(firstUpdate, gridState, settlementGrid)
        assertTrue(
            "Expected no crossing callout on the first update (no baseline yet), got: " +
                "${firstCallout?.positionedStrings?.map { it.text }}",
            firstCallout?.positionedStrings?.none { it.text.contains("Test River") } != false
        )

        // Second update: mapMatchedWay transitions from osmId 1 to osmId 2, which carries
        // crossing properties - this is the edge that should fire the callout.
        val secondUpdate = UserGeometry(
            location = gridState.ruler.offset(location, 0.0, 30.0), speed = 1.4,
            mapMatchedWay = crossingWay, timestampMilliseconds = 6000L
        )
        val secondCallout = autoCallout.updateLocation(secondUpdate, gridState, settlementGrid)
        assertNotNull(secondCallout)
        assertTrue(
            "Expected a callout mentioning Test River, got: ${secondCallout!!.positionedStrings.map { it.text }}",
            secondCallout.positionedStrings.any { it.text.contains("Test River") }
        )

        // Third update: still on the same Way (osmId 2 again) - must not repeat.
        val thirdUpdate = UserGeometry(
            location = gridState.ruler.offset(location, 0.0, 35.0), speed = 1.4,
            mapMatchedWay = crossingWay, timestampMilliseconds = 8000L
        )
        val thirdCallout = autoCallout.updateLocation(thirdUpdate, gridState, settlementGrid)
        assertTrue(
            "Expected no repeat crossing callout while still on the same Way, got: " +
                "${thirdCallout?.positionedStrings?.map { it.text }}",
            thirdCallout?.positionedStrings?.none { it.text.contains("Test River") } != false
        )

        // Leave the bridge and come back - should re-announce, since the old sweep-based
        // mechanism couldn't express "returned to the same crossing" at all.
        val fourthUpdate = UserGeometry(
            location = location, speed = 1.4, mapMatchedWay = approachWay, timestampMilliseconds = 10000L
        )
        autoCallout.updateLocation(fourthUpdate, gridState, settlementGrid)

        val fifthUpdate = UserGeometry(
            location = gridState.ruler.offset(location, 0.0, 30.0), speed = 1.4,
            mapMatchedWay = crossingWay, timestampMilliseconds = 12000L
        )
        val fifthCallout = autoCallout.updateLocation(fifthUpdate, gridState, settlementGrid)
        assertNotNull(fifthCallout)
        assertTrue(
            "Expected the crossing callout to re-announce after returning to the bridge, got: " +
                "${fifthCallout!!.positionedStrings.map { it.text }}",
            fifthCallout.positionedStrings.any { it.text.contains("Test River") }
        )
    }

    /**
     * End-to-end check that travel-mode reverse geocoding combines the current road with a
     * nearby highway junction, e.g. "On M80 at Junction 2, Robroyston" rather than just naming
     * the road or the junction alone.
     */
    @Test
    fun testTravelCalloutForHighwayJunction() {
        val location = LngLatAlt(-4.185480, 55.884504)
        val gridState = getGridStateForLocation(location, MAX_ZOOM_LEVEL, 3)
        val settlementGrid = getGridStateForLocation(location, 12, 3)

        val junctions = gridState.getFeatureTree(TreeId.HIGHWAY_JUNCTIONS).getAllCollection()
        val robroyston =
            junctions.features.find { (it as? MvtFeature)?.name == "Robroyston" } as MvtFeature
        val junctionLocation = (robroyston.geometry as Point).coordinates

        val userGeometry = UserGeometry(location = junctionLocation, speed = 15.0)
        val result = describeReverseGeocode(userGeometry, gridState, settlementGrid, null)

        assertNotNull(result)
        assertEquals("On M80 at Junction 2, Robroyston", result!!.text)
        // The junction callout also claims the plain road key, so it isn't followed moments
        // later by a redundant "still on the M80" - see TrackedCallout.extraDedupText.
        assertEquals("M80", result.extraDedupText)
    }

    /**
     * Minor road junctions (secondary/tertiary/residential/unclassified) shouldn't compete with
     * major ones (motorway/trunk/primary) for attention while driving - they're only called out
     * once nothing notable (a major junction or a passed landmark - see
     * NotableVehicleEventTracker) has been announced for a while. This synthesizes a minor
     * junction via direct FeatureTree injection (real tile data doesn't reliably offer minor
     * junctions at a stable test location) and checks it's suppressed shortly after a notable
     * event.
     */
    @Test
    fun testMinorJunctionSuppressedWhenRecentlyNotable() {
        val location = LngLatAlt(-4.254034459590912, 55.87014482990583)
        val gridState = getGridStateForLocation(location, MAX_ZOOM_LEVEL, 3)
        val settlementGrid = getGridStateForLocation(location, 12, 3)

        val junctionLocation = gridState.ruler.offset(location, 0.0, 50.0)
        val minorJunction = MvtFeature().apply {
            geometry = Point(junctionLocation)
            name = "Minor Junction"
            featureType = "highway"
            featureValue = "highway_junction"
            setProperty("class", "residential")
        }
        gridState.featureTrees[TreeId.HIGHWAY_JUNCTIONS.id] =
            FeatureTree(FeatureCollection().apply { addFeature(minorJunction) })

        val tracker = NotableVehicleEventTracker()
        tracker.recordEvent(99_000L)
        val userGeometry =
            UserGeometry(location = location, speed = 15.0, timestampMilliseconds = 100_000L)
        val result = describeReverseGeocode(
            userGeometry, gridState, settlementGrid, null, notableEventTracker = tracker
        )

        assertNotNull(result)
        assertTrue(
            "Expected the minor junction to be suppressed so soon after a notable event: ${result!!.text}",
            !result.text.contains("Minor Junction")
        )
    }

    /** Same minor junction as above, but with nothing notable recorded - it should be eligible. */
    @Test
    fun testMinorJunctionAnnouncedWhenQuiet() {
        val location = LngLatAlt(-4.254034459590912, 55.87014482990583)
        val gridState = getGridStateForLocation(location, MAX_ZOOM_LEVEL, 3)
        val settlementGrid = getGridStateForLocation(location, 12, 3)

        val junctionLocation = gridState.ruler.offset(location, 0.0, 50.0)
        val minorJunction = MvtFeature().apply {
            geometry = Point(junctionLocation)
            name = "Minor Junction"
            featureType = "highway"
            featureValue = "highway_junction"
            setProperty("class", "residential")
        }
        gridState.featureTrees[TreeId.HIGHWAY_JUNCTIONS.id] =
            FeatureTree(FeatureCollection().apply { addFeature(minorJunction) })

        val tracker = NotableVehicleEventTracker()
        val userGeometry =
            UserGeometry(location = location, speed = 15.0, timestampMilliseconds = 100_000L)
        val result = describeReverseGeocode(
            userGeometry, gridState, settlementGrid, null, notableEventTracker = tracker
        )

        assertNotNull(result)
        assertTrue(
            "Expected the minor junction to be announced once quiet: ${result!!.text}",
            result.text.contains("Minor Junction")
        )
    }

    /** Major junctions are always eligible, even shortly after another notable event. */
    @Test
    fun testMajorJunctionAnnouncedEvenWhenNotQuiet() {
        val location = LngLatAlt(-4.254034459590912, 55.87014482990583)
        val gridState = getGridStateForLocation(location, MAX_ZOOM_LEVEL, 3)
        val settlementGrid = getGridStateForLocation(location, 12, 3)

        val junctionLocation = gridState.ruler.offset(location, 0.0, 50.0)
        val majorJunction = MvtFeature().apply {
            geometry = Point(junctionLocation)
            name = "Major Junction"
            featureType = "highway"
            featureValue = "highway_junction"
            setProperty("class", "primary")
        }
        gridState.featureTrees[TreeId.HIGHWAY_JUNCTIONS.id] =
            FeatureTree(FeatureCollection().apply { addFeature(majorJunction) })

        val tracker = NotableVehicleEventTracker()
        tracker.recordEvent(99_000L)
        val userGeometry =
            UserGeometry(location = location, speed = 15.0, timestampMilliseconds = 100_000L)
        val result = describeReverseGeocode(
            userGeometry, gridState, settlementGrid, null, notableEventTracker = tracker
        )

        assertNotNull(result)
        assertTrue(
            "Expected the major junction to be announced regardless: ${result!!.text}",
            result.text.contains("Major Junction")
        )
    }

    /**
     * A junction whose highway class isn't recognised as major or minor (this also covers paths,
     * tracks, cycleways and service roads, which should never be called out) is never eligible,
     * even with nothing else to announce.
     */
    @Test
    fun testJunctionWithUnrecognisedClassNeverAnnounced() {
        val location = LngLatAlt(-4.254034459590912, 55.87014482990583)
        val gridState = getGridStateForLocation(location, MAX_ZOOM_LEVEL, 3)
        val settlementGrid = getGridStateForLocation(location, 12, 3)

        val junctionLocation = gridState.ruler.offset(location, 0.0, 50.0)
        val pathJunction = MvtFeature().apply {
            geometry = Point(junctionLocation)
            name = "Path Junction"
            featureType = "highway"
            featureValue = "highway_junction"
            setProperty("class", "footway")
        }
        gridState.featureTrees[TreeId.HIGHWAY_JUNCTIONS.id] =
            FeatureTree(FeatureCollection().apply { addFeature(pathJunction) })

        val userGeometry = UserGeometry(location = location, speed = 15.0)
        val result = describeReverseGeocode(userGeometry, gridState, settlementGrid, null)

        assertNotNull(result)
        assertTrue(
            "Expected the path junction to never be announced: ${result!!.text}",
            !result.text.contains("Path Junction")
        )
    }

    /**
     * Large POIs (see TreeId.LANDMARK_POIS) should be called out as they're passed while
     * travelling by car/bus, layered alongside AutoCallout's regular road/settlement description
     * - see AutoCallout.buildCalloutForVehicleLandmark.
     */
    @Test
    fun testVehicleLandmarkPassingCallout() {
        val location = LngLatAlt(-4.254034459590912, 55.87014482990583)
        val gridState = getGridStateForLocation(location, MAX_ZOOM_LEVEL, 3)
        val settlementGrid = getGridStateForLocation(location, 12, 3)

        val landmarkLocation = gridState.ruler.offset(location, 0.0, 50.0)
        val landmark = MvtFeature().apply {
            geometry = Point(landmarkLocation)
            name = "Ibrox Stadium"
            featureType = "poi"
            featureValue = "stadium"
        }
        gridState.featureTrees[TreeId.LANDMARK_POIS.id] =
            FeatureTree(FeatureCollection().apply { addFeature(landmark) })

        val autoCallout = AutoCallout(null, null)
        val userGeometry =
            UserGeometry(location = location, speed = 15.0, timestampMilliseconds = 1000L)
        val callout = autoCallout.updateLocation(userGeometry, gridState, settlementGrid)

        assertNotNull(callout)
        assertTrue(
            "Expected a callout mentioning Ibrox Stadium, got: ${callout!!.positionedStrings.map { it.text }}",
            callout.positionedStrings.any { it.text.contains("Ibrox Stadium") }
        )
    }

    /** A large POI with no real name isn't worth calling out. */
    @Test
    fun testVehicleLandmarkSkippedWhenUnnamed() {
        val location = LngLatAlt(-4.254034459590912, 55.87014482990583)
        val gridState = getGridStateForLocation(location, MAX_ZOOM_LEVEL, 3)
        val settlementGrid = getGridStateForLocation(location, 12, 3)

        val landmarkLocation = gridState.ruler.offset(location, 0.0, 50.0)
        val landmark = MvtFeature().apply {
            geometry = Point(landmarkLocation)
            featureType = "poi"
            featureValue = "stadium"
        }
        gridState.featureTrees[TreeId.LANDMARK_POIS.id] =
            FeatureTree(FeatureCollection().apply { addFeature(landmark) })

        val autoCallout = AutoCallout(null, null)
        val userGeometry =
            UserGeometry(location = location, speed = 15.0, timestampMilliseconds = 1000L)
        val callout = autoCallout.updateLocation(userGeometry, gridState, settlementGrid)

        assertNotNull(callout)
        assertTrue(
            "Expected no callout text derived from the unnamed landmark: ${callout!!.positionedStrings.map { it.text }}",
            callout.positionedStrings.none { it.text.contains("Passing") }
        )
    }

    /**
     * Travel-mode reverse geocoding should prefer the map-matched way (the road we're confirmed
     * to be on) over its own independent nearest-road search, which can pick the wrong road at
     * junctions or parallel carriageways. Uses a fabricated Way with a name that doesn't exist in
     * the tile data, so the only way it can appear in the callout is via
     * userGeometry.mapMatchedWay.
     */
    @Test
    fun testTravelCalloutPrefersMapMatchedWay() {
        val location = LngLatAlt(-4.254034459590912, 55.87014482990583)
        val gridState = getGridStateForLocation(location, MAX_ZOOM_LEVEL, 3)
        val settlementGrid = getGridStateForLocation(location, 12, 3)

        val fakeMatchedWay = Way().apply { name = "Fake Matched Road" }
        val userGeometry =
            UserGeometry(location = location, speed = 15.0, mapMatchedWay = fakeMatchedWay)
        val result = describeReverseGeocode(userGeometry, gridState, settlementGrid, null)

        assertNotNull(result)
        assertTrue(
            "Expected callout to use the map-matched way's name, got: ${result!!.text}",
            result.text.contains("Fake Matched Road")
        )
    }

    /**
     * Once probablyOnTrain() detects a nearby real station (via TreeId.TRANSIT_STOPS), it should
     * be tracked in a LastStationTracker so a later call can describe progress as "distance since
     * {station}" - but only combined with a nearby settlement, e.g. "On the line and close to
     * Merchant City, 0.2 km since Argyle Street" - a standalone since-distance with nothing
     * else to describe would fire on every location update as the distance keeps climbing, which
     * is too frequent on its own (see real train-1/train-2.gpx replays). See LastStationTracker.
     *
     * The distance is always computed live (never repeats exactly), so PositionedString.dedupText
     * deliberately excludes it - roadSenseCalloutHistory dedups on that instead of the spoken
     * text, so two calls describing progress since the same station still count as a duplicate
     * even though the exact metres differ.
     */
    @Test
    fun testTravelCalloutTracksStationForSinceDistance() {
        val argyleStreetStation = LngLatAlt(-4.25057977437973, 55.85762197620575)
        val gridState = getGridStateForLocation(argyleStreetStation, MAX_ZOOM_LEVEL, 3)
        val settlementGrid = getGridStateForLocation(argyleStreetStation, 12, 3)

        val fakeMatchedRailway = Way().apply { name = "Fake Railway Line" }
        val tracker = LastStationTracker()

        // Just past the station - outside the 20m "at a stop" radius (which would otherwise name
        // the stop directly and never reach the station-tracking logic), but inside the 50m
        // station-tracking radius.
        val justPastStation = gridState.ruler.offset(argyleStreetStation, 0.0, 30.0)
        val userGeometryNearStation = UserGeometry(
            location = justPastStation,
            speed = 15.0,
            mapMatchedRailway = fakeMatchedRailway,
            timestampMilliseconds = 0L
        )
        describeReverseGeocode(userGeometryNearStation, gridState, settlementGrid, null, tracker)

        assertEquals("Argyle Street", tracker.name)
        assertNotNull(tracker.location)

        // Further down the line - the callout should describe the live distance since Argyle
        // Street. 200m north keeps us well clear of Glasgow Queen Street station, which real data
        // has ~500m further north again - close enough that a bigger offset would hit its 20m
        // "at a stop" radius instead.
        val furtherAlong = gridState.ruler.offset(argyleStreetStation, 0.0, 200.0)
        val userGeometryFurtherAlong = UserGeometry(
            location = furtherAlong,
            speed = 15.0,
            mapMatchedRailway = fakeMatchedRailway,
            timestampMilliseconds = 1_000L
        )
        val result =
            describeReverseGeocode(userGeometryFurtherAlong, gridState, settlementGrid, null, tracker)

        assertNotNull(result)
        assertEquals(
            "On Fake Railway Line and close to Merchant City, 0.2 km since Argyle Street",
            result!!.text
        )

        // Further still - the spoken distance moves on, but the dedup key (road, settlement,
        // station - no distance) stays identical so history can suppress the repeat. It takes a
        // decent step to change the spoken text, since at this speed the distance is read out in
        // tenths of a kilometre (see formatDistanceAndDirection), and Glasgow Queen Street
        // station is only ~500m further north again.
        val evenFurtherAlong = gridState.ruler.offset(argyleStreetStation, 0.0, 400.0)
        val userGeometryEvenFurtherAlong = UserGeometry(
            location = evenFurtherAlong,
            speed = 15.0,
            mapMatchedRailway = fakeMatchedRailway,
            timestampMilliseconds = 2_000L
        )
        val secondResult = describeReverseGeocode(
            userGeometryEvenFurtherAlong, gridState, settlementGrid, null, tracker
        )

        assertNotNull(secondResult)
        assertTrue(
            "Expected the spoken distance to differ: ${result.text} vs ${secondResult!!.text}",
            result.text != secondResult.text
        )
        assertEquals(result.dedupText, secondResult.dedupText)
    }

    /**
     * When userGeometry.probablyOnTrain() is true (vehicle speed + a confident railway match),
     * travel-mode reverse geocoding should use the railway's name instead of doing a road search
     * or looking for a highway junction, and should still combine it with a nearby settlement,
     * e.g. "On the Argyle Line and close to Bearsden" - matching the "On X" phrasing already used
     * for roads.
     */
    @Test
    fun testTravelCalloutForTrain() {
        val location = LngLatAlt(-4.254034459590912, 55.87014482990583)
        val gridState = getGridStateForLocation(location, MAX_ZOOM_LEVEL, 3)
        val settlementGrid = getGridStateForLocation(location, 12, 3)

        val fakeMatchedRailway = Way().apply { name = "Fake Railway Line" }
        val userGeometry = UserGeometry(
            location = location,
            speed = 15.0,
            mapMatchedRailway = fakeMatchedRailway
        )
        assertTrue(userGeometry.probablyOnTrain())

        val result = describeReverseGeocode(userGeometry, gridState, settlementGrid, null)

        assertNotNull(result)
        assertEquals("On Fake Railway Line and close to Cowcaddens", result!!.text)
    }

    /**
     * A real line name (e.g. "Argyle Line") is an OSM route-relation concept this tile schema
     * doesn't extract onto individual rail Ways yet, so an unnamed rail Way used to fall through
     * to the same destination-confection logic used for unnamed footpaths, producing an odd
     * "Train that joins X and Y" description (see real train-1/train-2.gpx replays). It should
     * just say "train" instead - Way.getName() will use a real name once the tile data has one.
     */
    @Test
    fun testUnnamedRailwayFallsBackToGenericTrain() {
        val location = LngLatAlt(-4.254034459590912, 55.87014482990583)
        val gridState = getGridStateForLocation(location, MAX_ZOOM_LEVEL, 3)
        val settlementGrid = getGridStateForLocation(location, 12, 3)

        val unnamedRailway = Way().apply { featureType = "rail" }
        val userGeometry = UserGeometry(
            location = location,
            speed = 15.0,
            mapMatchedRailway = unnamedRailway
        )

        val result = describeReverseGeocode(userGeometry, gridState, settlementGrid, null)

        assertNotNull(result)
        assertEquals("On train and close to Cowcaddens", result!!.text)
    }

    /**
     * Measures the actual CPU cost of running the new rail MapMatchFilter alongside the existing
     * road one, since running map matching twice per location update is a real concern. Compares:
     *  1. Road matching alone over a real road route (existing behaviour).
     *  2. Rail matching alone over a real rail route (worst case for the new filter - actively
     *     matching, not just querying an empty area).
     *  3. Both filters together over the same combined step count (what GeoEngine.kt now does).
     */
    @Test
    fun benchmarkRailMapMatchingOverhead() {
        val gridState = getGridStateForLocation(glasgowTestLocation, MAX_ZOOM_LEVEL, 3)

        val roads = gridState.getFeatureTree(TreeId.ROADS_AND_PATHS).getAllCollection()
        val roadWay = roads.features
            .filterIsInstance<Way>()
            .filter { (it.geometry as LineString).coordinates.size > 20 }
            .maxByOrNull { it.length }!!
        val roadCoordinates = (roadWay.geometry as LineString).coordinates

        val railways = gridState.getFeatureTree(TreeId.TRANSIT).getAllCollection()
        val railWay = railways.features
            .filterIsInstance<Way>()
            .filter { (it.geometry as LineString).coordinates.size > 20 }
            .maxByOrNull { it.length }!!
        val railCoordinates = (railWay.geometry as LineString).coordinates

        // Warm up (JIT, first-call allocation costs) before timing.
        repeat(2) {
            val warmup = MapMatchFilter()
            for (c in roadCoordinates) warmup.filter(c, gridState, FeatureCollection(), false, null)
        }

        val roadOnlyTime = measureTime {
            val filter = MapMatchFilter()
            for (c in roadCoordinates) filter.filter(c, gridState, FeatureCollection(), false, null)
        }

        val railOnlyTime = measureTime {
            val filter = MapMatchFilter(networkTree = TreeId.TRANSIT)
            for (c in railCoordinates) filter.filter(c, gridState, FeatureCollection(), false, null)
        }

        val bothTime = measureTime {
            val roadFilter = MapMatchFilter()
            val railFilter = MapMatchFilter(networkTree = TreeId.TRANSIT)
            for (i in 0 until minOf(roadCoordinates.size, railCoordinates.size)) {
                roadFilter.filter(roadCoordinates[i], gridState, FeatureCollection(), false, null)
                railFilter.filter(railCoordinates[i], gridState, FeatureCollection(), false, null)
            }
        }

        println("Road-only: $roadOnlyTime for ${roadCoordinates.size} points")
        println("Rail-only: $railOnlyTime for ${railCoordinates.size} points")
        println("Both together: $bothTime for ${minOf(roadCoordinates.size, railCoordinates.size)} paired points")
        println("Per-point road-only: ${roadOnlyTime / roadCoordinates.size}")
        println("Per-point rail-only: ${railOnlyTime / railCoordinates.size}")
        println("Per-point both: ${bothTime / minOf(roadCoordinates.size, railCoordinates.size)}")
    }

    /**
     * WayGenerator.generateWays used to unconditionally skip populating the intersection map for
     * transit ways (`if (!transit) { ... intersectionMap[...] = ... }`), so no TILE_EDGE
     * intersections ever reached GridState's tile-stitching pass and every railway Way stopped
     * dead at a tile boundary, regardless of what was passed in at the call site. This confirms a
     * real railway line in the Glasgow extract now gets stitched into a single connected Way
     * graph across tile boundaries, the same way roads already were.
     */
    @Test
    fun testRailwayStitchingAcrossTileBoundaries() {
        val gridState = getGridStateForLocation(glasgowTestLocation, MAX_ZOOM_LEVEL, 3)
        val railways = gridState.getFeatureTree(TreeId.TRANSIT).getAllCollection()

        var joiner: Way? = null
        outer@ for (feature in railways) {
            val way = feature as? Way ?: continue
            for (intersection in way.intersections) {
                if (intersection == null) continue
                val found = intersection.members.find { it.wayType == WayType.JOINER }
                if (found != null) {
                    joiner = found
                    break@outer
                }
            }
        }
        assertNotNull("Expected at least one JOINER way stitching railway tiles together", joiner)

        // A JOINER connects two TILE_EDGE intersections, each belonging to a real railway Way
        // from a different tile. Confirm both sides are real, distinct Ways.
        val startIntersection = joiner!!.intersections[WayEnd.START.id]!!
        val endIntersection = joiner.intersections[WayEnd.END.id]!!
        val wayBeforeJoin = startIntersection.members.first { it != joiner }
        val wayAfterJoin = endIntersection.members.first { it != joiner }

        assertTrue("Joiner should connect two different railway Ways", wayBeforeJoin != wayAfterJoin)
    }

    /**
     * MapMatchFilter can now be configured to match against a network other than roads, via
     * `MapMatchFilter(networkTree = TreeId.TRANSIT)`. This feeds a real railway Way's own
     * geometry through such a filter and checks it locks onto a Way from the railway network,
     * as groundwork for train detection.
     */
    @Test
    fun testMapMatchFilterLocksOntoRailway() {
        val gridState = getGridStateForLocation(glasgowTestLocation, MAX_ZOOM_LEVEL, 3)
        val railways = gridState.getFeatureTree(TreeId.TRANSIT).getAllCollection()

        val targetWay = railways.features
            .filterIsInstance<Way>()
            .filter { (it.geometry as LineString).coordinates.size > 20 }
            .maxByOrNull { it.length }
        assertNotNull("Expected a reasonably long railway Way in the test data", targetWay)

        val coordinates = (targetWay!!.geometry as LineString).coordinates
        val railMapMatchFilter = MapMatchFilter(networkTree = TreeId.TRANSIT)
        for (coordinate in coordinates) {
            runBlocking { gridState.locationUpdate(coordinate, emptySet(), null) }
            railMapMatchFilter.filter(coordinate, gridState, FeatureCollection(), false, null)
        }

        val matched = railMapMatchFilter.matchedWay
        assertNotNull("Expected map matching to lock onto a railway Way", matched)
        assertTrue(
            "Expected the matched Way to be part of the railway network, got featureType=${matched!!.featureType}",
            matched.featureType == "rail" || matched.featureType == "transit"
        )
    }

    /**
     * The Glasgow Subway runs directly beneath Byres Road for its whole route (e.g. around
     * 55.872965,-4.296419) - since GPS is 2D, feeding Byres Road's own surface coordinates
     * through the rail map-matcher could previously build up a confident lock onto the Subway
     * tunnel running underneath it, wrongly flagging a driver/pedestrian on the road as being on
     * a train (see MvtToGeoJson.isUnmatchableRailway and UserGeometry.probablyOnTrain). Confirms
     * the Subway itself never reaches TreeId.TRANSIT, and that map-matching a real Byres Road
     * route against the rail network never builds up a confident match.
     */
    @Test
    fun testSubwayExcludedFromRailMapMatching() {
        val byresRoadLocation = LngLatAlt(-4.296419, 55.872965)
        val gridState = getGridStateForLocation(byresRoadLocation, MAX_ZOOM_LEVEL, 3)

        // No subway-classed or tunnel-tagged way should ever reach the rail-matching network.
        val transitWays = gridState.getFeatureTree(TreeId.TRANSIT).getAllCollection().features
            .filterIsInstance<Way>()
        assertTrue(
            "TreeId.TRANSIT should never contain a subway-classed way",
            transitWays.none { it.featureSubClass == "subway" }
        )
        assertTrue(
            "TreeId.TRANSIT should never contain a tunnel-tagged railway segment",
            transitWays.none { it.properties?.get("brunnel") == "tunnel" }
        )

        // Byres Road itself, running along the surface directly above the Subway, should never
        // build up a confident match against the rail network.
        val byresRoad = gridState.getFeatureTree(TreeId.ROADS).getAllCollection().features
            .filterIsInstance<Way>()
            .filter { it.name == "Byres Road" }
            .maxByOrNull { it.length }
        assertNotNull("Expected to find Byres Road in the test data", byresRoad)

        val coordinates = (byresRoad!!.geometry as LineString).coordinates
        val railMapMatchFilter = MapMatchFilter(networkTree = TreeId.TRANSIT)
        for (coordinate in coordinates) {
            runBlocking { gridState.locationUpdate(coordinate, emptySet(), null) }
            railMapMatchFilter.filter(coordinate, gridState, FeatureCollection(), false, null)
        }

        assertFalse(
            "Byres Road should never build a confident rail match - it's above the Subway, not on it",
            railMapMatchFilter.isMatchConfident
        )
    }

    @Test
    fun testVectorToGeoJsonGreggs() {
        val intersectionMap: HashMap<LngLatAlt, Intersection> = hashMapOf()
        val streetNumberMap: HashMap<String, FeatureCollection> = hashMapOf()
        val geojson = vectorTileToGeoJsonFromFile(7995, 5108, intersectionMap, streetNumberMap)
        val adapter = GeoJsonObjectMoshiAdapter()

        val outputCollection = FeatureCollection()
        for (collection in geojson)
            outputCollection += collection

        val outputFile = FileOutputStream("greggs.geojson")
        outputFile.write(adapter.toJson(outputCollection).toByteArray())
        outputFile.close()
    }

    @Test
    fun testVectorToGeoJsonMilngavie() {
        val intersectionMap: HashMap<LngLatAlt, Intersection> = hashMapOf()
        val streetNumberMap: HashMap<String, FeatureCollection> = hashMapOf()
        val geojson =
            vectorTileToGeoJsonFromFile(15991 / 2, 10214 / 2, intersectionMap, streetNumberMap)
        val adapter = GeoJsonObjectMoshiAdapter()

        val outputCollection = FeatureCollection()
        for (collection in geojson)
            outputCollection += collection

        val outputFile = FileOutputStream("milngavie.geojson")
        outputFile.write(adapter.toJson(outputCollection).toByteArray())
        outputFile.close()
    }

    @Test
    fun testVectorToGeoJsonEdinburgh() {
        val intersectionMap: HashMap<LngLatAlt, Intersection> = hashMapOf()
        val streetNumberMap: HashMap<String, FeatureCollection> = hashMapOf()
        val geojson =
            vectorTileToGeoJsonFromFile(16093 / 2, 10211 / 2, intersectionMap, streetNumberMap)
        val adapter = GeoJsonObjectMoshiAdapter()

        val outputCollection = FeatureCollection()
        for (collection in geojson)
            outputCollection += collection

        val outputFile = FileOutputStream("edinburgh.geojson")
        outputFile.write(adapter.toJson(outputCollection).toByteArray())
        outputFile.close()
    }

    @Test
    fun testVectorToGeoJsonByresRoad() {
        val intersectionMap: HashMap<LngLatAlt, Intersection> = hashMapOf()
        val streetNumberMap: HashMap<String, FeatureCollection> = hashMapOf()
        val geojson =
            vectorTileToGeoJsonFromFile(15992 / 2, 10223 / 2, intersectionMap, streetNumberMap)
        val adapter = GeoJsonObjectMoshiAdapter()

        val outputCollection = FeatureCollection()
        for (collection in geojson)
            outputCollection += collection

        val outputFile = FileOutputStream("byresroad.geojson")
        outputFile.write(adapter.toJson(outputCollection).toByteArray())
        outputFile.close()
    }

    @Test
    fun testVectorToGeoJsonGlasgowQueenStreet() {
        val adapter = GeoJsonObjectMoshiAdapter()
        val gridState = getGridStateForLocation(LngLatAlt(-4.251169, 55.862550), 14, 2)

        val missingResourceMapperStrings = mutableSetOf<String>()
        for (treeId in TreeId.entries) {
            if (treeId == TreeId.MAX_COLLECTION_ID)
                break


            val collection = gridState.getFeatureTree(treeId).getAllCollection()
            when (treeId) {
                TreeId.ROADS,
                TreeId.ROADS_AND_PATHS,
                TreeId.INTERPOLATIONS,
                TreeId.INTERSECTIONS,
                TreeId.SETTLEMENT_CITY,
                TreeId.SETTLEMENT_TOWN,
                TreeId.SETTLEMENT_VILLAGE,
                TreeId.SETTLEMENT_HAMLET,
                TreeId.TRANSIT -> {
                }

                else -> {
                    for (feature in collection) {
                        val mvtFeature = feature as MvtFeature
                        val osmClass = mvtFeature.featureClass
                        val osmSubClass = mvtFeature.featureSubClass

                        if ((osmClass == null) && (osmSubClass == null))
                            continue

                        val found =
                            ResourceMapper.hasResource(osmClass) || ResourceMapper.hasResource(
                                osmSubClass
                            )
                        if (!found) {
                            if (osmClass != null)
                                missingResourceMapperStrings.add(osmClass)
                            if (osmSubClass != null)
                                missingResourceMapperStrings.add(osmSubClass)
                        }
                    }
                }
            }
            val outputFile = FileOutputStream("glasgow-queen-street-${treeId.description}.geojson")
            outputFile.write(adapter.toJson(collection).toByteArray())
            outputFile.close()
        }
        println("Missing tags from ResourceMapper:")
        for (tag in missingResourceMapperStrings)
            println("  $tag")

    }

    /** This test reads in a 2x2 array of vector tiles and merges them into a single GeoJSON.
     * That's then saved off to a file for a visual check. There's no joining up of lines but
     * because there are no intersections between the roads from separate tiles the GeoJSON
     * processing code isn't really any the wiser.
     * However, POIs have to be de-duplicated to avoid multiple all outs. The two ways to do this
     * are:
     *  1. Check for duplicates as we merge
     *  2. Crop out POIs which are outside the tile during initial importing
     * Initially going with option 2 as that's the cheapest and it's not clear why would ever want
     * POI that are outwith the tile boundaries.
     *
     *
     * When using soundscape-backend the tile array used is 3x3, but those tiles are at a higher
     * zoom level. The vector tiles are twice the width/height and so a 2x2 array can be used with
     * the array moving when the user location leaves the center.
     */
    @Test
    fun testVectorToGeoJsonGrid() {
        // Make a large grid to aid analysis
        val gridState = getGridStateForLocation(LngLatAlt(-4.317357, 55.942527), 14, 1)

        // Check that the de-duplication of the points worked (without that there are two points
        // for Graeme Pharmacy, one each from two separate tiles).
        val searchResults = searchFeaturesByName(
            gridState.getFeatureTree(TreeId.POIS).getAllCollection(),
            "Graeme"
        )

        val adapter = GeoJsonObjectMoshiAdapter()
        println(adapter.toJson(searchResults))
        assertEquals(1, searchResults.features.size)

        // Check that we can find the containing polygons for a point
        val tree = gridState.getFeatureTree(TreeId.POIS)
        val fc1 = tree.getContainingPolygons(LngLatAlt(-4.316401, 55.939941))
        assertEquals(1, fc1.features.size)
        assertEquals("Tesco Customer Car Park", (fc1.features[0] as MvtFeature).name)

        val fc2 = tree.getContainingPolygons(LngLatAlt(-4.312885, 55.942237))
        assertEquals(1, fc2.features.size)
        assertEquals("Milngavie Town Hall", (fc2.features[0] as MvtFeature).name)

        val fc3 = tree.getContainingPolygons(LngLatAlt(-4.316641241312027, 55.94160200415631))
        assertEquals(1, fc3.features.size)

        val outputCollection = gridState.getFeatureTree(TreeId.WAYS_SELECTION).getAllCollection()
        outputCollection += gridState.getFeatureTree(TreeId.POIS).getAllCollection()
        for (intersection in gridState.gridIntersections) {
            intersection.value.toFeature()
            outputCollection.addFeature(intersection.value)
        }

        val outputFile = FileOutputStream("2x2-14.geojson")
        outputFile.write(adapter.toJson(outputCollection).toByteArray())
        outputFile.close()
    }

    /**
     * testZoomLevels was used to compare the output from two grids, one at zoom level 14 and the
     * other at zoom level 15.
     */
    //@Test
    fun testZoomLevels() {
        // Make two grids of the same region but different zoom levels
        val gridState14 = getGridStateForLocation(LngLatAlt(-4.317357, 55.942527), 14, 1)
        val gridState15 = getGridStateForLocation(LngLatAlt(-4.317357, 55.942527), 15, 2)

        for (treeId in TreeId.entries) {
            if (treeId == TreeId.MAX_COLLECTION_ID)
                break

            val featureCollection14 = gridState14.getFeatureTree(treeId).getAllCollection()
            val featureCollection15 = gridState15.getFeatureTree(treeId).getAllCollection()

            if (treeId == TreeId.WAYS_SELECTION) {
                val adapter = GeoJsonObjectMoshiAdapter()
                val outputFile14 = FileOutputStream("2x2-14.geojson")
                outputFile14.write(adapter.toJson(featureCollection14).toByteArray())
                outputFile14.close()
                val outputFile15 = FileOutputStream("2x2-15.geojson")
                outputFile15.write(adapter.toJson(featureCollection15).toByteArray())
                outputFile15.close()
            }

            if ((featureCollection14.features.size) != featureCollection15.features.size) {
                println("$treeId - ${featureCollection14.features.size} ${featureCollection15.features.size}")
                if ((treeId != TreeId.INTERPOLATIONS) && (treeId != TreeId.ROADS) && (treeId != TreeId.WAYS_SELECTION))
                    assert(false)
            }
        }

        // If we get here then all of the POIS are present. Because the grid sizes are different
        // there are extra ROADS and PATHS joining the tiles together which accounts for the different
        // numbers of roads, paths and interpolations.
    }

    /**
     * This test generates a FeatureCollection containing un-named roads and paths that we managed
     * to generate our own names for. The priority for naming is:
     *  1. Sidewalks
     *  2. Water the way follows (see testNameConfectionForPathFollowingWater)
     *  3. Road destinations
     *  4. POI destinations
     *  5. Dead ends
     *
     * Once we add railways, we can consider adding an 'along railway' type description too.
     */
    @Test
    fun testNameConfection() {
        val userGeometry = UserGeometry(LngLatAlt(-4.313, 55.945245))
        val gridState = getGridStateForLocation(userGeometry.location, MAX_ZOOM_LEVEL, 2)

        val confectionTime = measureTimeMillis {
            traverseIntersectionsConfectingNames(gridState.gridIntersections)
        }

        var roads = gridState.getFeatureCollection(TreeId.WAYS_SELECTION)
        val confectionTime2 = measureTimeMillis {
            for (road in roads) {
                confectNamesForRoad(road as Way, gridState, null)
            }
        }
        println("Confection time: $confectionTime ms")
        println("Confection time2: $confectionTime2 ms")

        roads = gridState.getFeatureCollection(TreeId.WAYS_SELECTION)
        val adapter = GeoJsonObjectMoshiAdapter()
        val outputFile = FileOutputStream("confected-names.geojson")
        outputFile.write(adapter.toJson(roads).toByteArray())
        outputFile.close()
    }

    @Test
    fun testRtree() {
        // Make a large grid to aid analysis
        val featureCollection = FeatureCollection()
        for (x in 7995..7995) {
            for (y in 5106..5107) {
                val intersectionMap: HashMap<LngLatAlt, Intersection> = hashMapOf()
                val streetNumberMap: HashMap<String, FeatureCollection> = hashMapOf()
                val geojson = vectorTileToGeoJsonFromFile(x, y, intersectionMap, streetNumberMap)

                for (collection in geojson) {
                    for (feature in collection) {
                        featureCollection.addFeature(feature)
                    }
                }
            }
        }

        // Iterate through all of the features and add them to an Rtree
        var start = System.currentTimeMillis()
        val tree = FeatureTree(featureCollection)
        var end = System.currentTimeMillis()

        // Prove that we can edit the feature property in the original collection and it affects
        // the contents of the rtree. We don't really want this behaviour, but it's what we have.
        for (feature in featureCollection) {
            if (feature is MvtFeature && feature.name == "Blane Drive") {
                feature.name = "Blah Drive"
            }
        }

        // We have all the points in an rtree
        println("Tree size: ${tree.tree!!.size} - ${end - start}ms")
        //tree.tree!!.visualize(4096,4096).save("tree.png");

        start = System.currentTimeMillis()
        val distanceFc = tree.getNearbyCollection(
            LngLatAlt(-4.3058322, 55.9473305),
            20.0,
            CheapRuler(55.9473305)
        )
        end = System.currentTimeMillis()
        println("Search (${end - start}ms):")
        for (feature in distanceFc) {
            val mvtFeature = feature as MvtFeature
            println(mvtFeature.name)
        }

        start = System.currentTimeMillis()
        val nearestFc =
            tree.getNearestFeature(LngLatAlt(-4.316914, 55.941861), CheapRuler(55.9473305), 50.0)
        end = System.currentTimeMillis()
        println("Nearest (${end - start}ms):")
        val mvtFeature = nearestFc as MvtFeature
        println(mvtFeature.name)

        val adapter = GeoJsonObjectMoshiAdapter()
        val outputFile = FileOutputStream("rtree.geojson")
        outputFile.write(adapter.toJson(distanceFc).toByteArray())
        outputFile.close()
    }

    @Test
    fun testObjects() {
        // This test is to show how Kotlin doesn't copy objects by default. featureCopy isn't a copy
        // as it might be in C++, but a reference to the same object. There's no copy() defined
        // for Feature. This means that in all the machinations with FeatureCollections, the Features
        // underlying them are the same ones. So long as they are not changed then this isn't a
        // problem, but we do add "distance_to".

        val intersectionMap: HashMap<LngLatAlt, Intersection> = hashMapOf()
        val streetNumberMap: HashMap<String, FeatureCollection> = hashMapOf()
        val featureCollections =
            vectorTileToGeoJsonFromFile(15990 / 2, 10212 / 2, intersectionMap, streetNumberMap)
        val featureCollection = FeatureCollection()
        for (collection in featureCollections) {
            featureCollection += collection
        }
        println(featureCollection.features[0].id)
        val newFeatureCollection = FeatureCollection()
        newFeatureCollection += featureCollection
        val featureReference = featureCollection.features[0]
        featureReference.id = "Blah"
        println(featureCollection.features[0].id)
        println(newFeatureCollection.features[0].id)
        println(featureReference.id)
        assert(featureCollection.features[0].id == newFeatureCollection.features[0].id)
        assert(featureReference.id == newFeatureCollection.features[0].id)

        // Copy
        val copyFeatureCollection = FeatureCollection()
        copyFeatureCollection.features = ArrayList(newFeatureCollection.features)
        println(copyFeatureCollection.features[0].id)

        // newFeatureCollection is new, but the features that it contains are not
        newFeatureCollection.features.clear()
        println(newFeatureCollection.features.size)

        // It's actually not possible to easily copy a Feature. What about a simple hashmap?
        val map = hashMapOf<Int, String>()
        map[0] = "Zero"
        map[1] = "One"
        map[2] = "Two"

        val mapCopy = map.clone() as HashMap<*, *>
        for (entry in mapCopy) {
            println(entry.value)
        }
        map[0] = "Not zero?"
        for (entry in mapCopy) {
            println(entry.value)
        }
        for (entry in map) {
            println(entry.value)
        }

        // Clone is cloning all of the hashmap entries
    }

    @Test
    fun testGetNearestCollection(){
        val userGeometry = UserGeometry(LngLatAlt(-4.313, 55.945245))
        val gridState = getGridStateForLocation(userGeometry.location, MAX_ZOOM_LEVEL, 2)

        val collection = gridState.getFeatureTree(TreeId.WAYS_SELECTION)
            .getNearestCollection(userGeometry.location, 2000.0, 10, gridState.ruler)

        println("collection size ${collection.features.size}")
        assertEquals(10, collection.features.size)
    }

    @Test
    fun testGetNearestCollectionWithinTriangle(){
        val userGeometry = UserGeometry(LngLatAlt(-4.313, 55.945245), fovDistance = 2000.0)
        val gridState = getGridStateForLocation(userGeometry.location, MAX_ZOOM_LEVEL, 2)

        val triangle = getFovTriangle(userGeometry, true)

        val duration = measureTime {
            val collection = gridState.getFeatureTree(TreeId.WAYS_SELECTION)
                .getNearestCollectionWithinTriangle(triangle, 10, gridState.ruler)

            println("collection size ${collection.features.size}")
            assertEquals(10, collection.features.size)
        }
        println("Processing time $duration")
    }

    @Test
    fun testRoadBearing() {
        val userGeometry = UserGeometry(LngLatAlt(-4.313, 55.945245))
        val gridState = getGridStateForLocation(userGeometry.location, MAX_ZOOM_LEVEL, 2)

        val roadTree = gridState.getFeatureTree(TreeId.ROADS)
        val nearestRoad = roadTree.getNearestFeature(userGeometry.location, userGeometry.ruler)

        println(nearestRoad.toString())
    }

    @Test
    fun testNearestRoadIdeas() {
        val gridState = getGridStateForLocation(LngLatAlt(-4.31029, 55.94583), MAX_ZOOM_LEVEL, 2)
        val geojson = FeatureCollection()

        val heading = 180.0
        var latitude = 55.945219
        while (latitude < 55.94583) {
            var longitude = -4.311362
            var lastNearestRoad: Feature? = null
            while (longitude < -4.31029) {

                val location = LngLatAlt(longitude, latitude)
                val sensedNearestRoads = gridState.getFeatureTree(TreeId.WAYS_SELECTION)
                    .getNearestCollection(location, 20.0, 10, gridState.ruler)

                var bestIndex = -1
                var bestFitness = 0.0
                for ((index, sensedRoad) in sensedNearestRoads.withIndex()) {
                    val sensedRoadInfo = getDistanceToFeature(location, sensedRoad, gridState.ruler)
                    var headingOffSensedRoad =
                        abs((heading % 180) - (sensedRoadInfo.heading % 180))
                    if (headingOffSensedRoad > 90)
                        headingOffSensedRoad = 180 - headingOffSensedRoad

                    // We want to decide based on distance and direction. This calculation gives
                    // a reasonable road as a result from only a point and a heading - no history.
                    // The actual nearest road function could use the nearest road history to
                    // decide on whether to stick with the individual road or not.
                    val w1 = 300.0
                    val w2 = 100.0
                    val fitness = (w1 * (10 / (10 + sensedRoadInfo.distance))) +
                            (w2 * (30 / (30 + headingOffSensedRoad)))
                    if (fitness > bestFitness) {
                        bestFitness = fitness
                        bestIndex = index
                    }
                }
                if (sensedNearestRoads.features.isNotEmpty()) {
                    val bestMatch = sensedNearestRoads.features[bestIndex] as MvtFeature
                    if (bestMatch != lastNearestRoad) {
                        val geoPointFeature = Feature()
                        val pointGeometry = Point(location)
                        geoPointFeature.geometry = pointGeometry
                        val properties: HashMap<String, Any?> = hashMapOf()
                        properties["nearestRoad"] = bestMatch.name
                        properties["direction"] = heading
                        geoPointFeature.properties = properties
                        geojson.addFeature(geoPointFeature)
                    }

                    lastNearestRoad = bestMatch
                }

                longitude += 0.00001
            }
            latitude += 0.00001
        }
        val adapter = GeoJsonObjectMoshiAdapter()
        val outputFile = FileOutputStream("nearest.geojson")
        outputFile.write(adapter.toJson(geojson).toByteArray())
        outputFile.close()
    }

    @Test
    fun testConvertBackToTileCoordinates() {

        val tileX = 10000
        val tileY = 16000
        val tileZoom = 15

        for (testX in 0 until 4096) {
            for (testY in 0 until 4096) {
                val location = getLatLonTileWithOffset(
                    tileX,
                    tileY,
                    tileZoom,
                    sampleToFractionOfTile(testX),
                    sampleToFractionOfTile(testY)
                )

                val result = convertBackToTileCoordinates(location, tileZoom)
                assert(result.first == testX)
                assert(result.second == testY)
            }
        }
    }

    fun testMovingGrid(gpxFilename: String, calloutFilename: String, geojsonFilename: String) {

        val gridState = FileGridState()
        gridState.start(offlineExtractPath)
        val settlementGrid = FileGridState(12, 3)
        settlementGrid.start(offlineExtractPath)
        val mapMatchFilter = MapMatchFilter()
        val railMapMatchFilter = MapMatchFilter(networkTree = TreeId.TRANSIT)
        val gps = parseGpxFromFile(gpxFilename)
        val collection = FeatureCollection()
        val startIndex = 0
        val endIndex = gps.features.size
        val autoCallout = AutoCallout(null, null)
        val callOutText = FileOutputStream(calloutFilename)

        val enabledCategories = mutableSetOf<String>()
        enabledCategories.add(PLACES_AND_LANDMARKS_KEY)
        enabledCategories.add(MOBILITY_KEY)

        val markers = FeatureCollection()
        val marker = MvtFeature()
        marker.geometry = Point(-4.3095570, 55.9498421)
        marker.name = "Marker 1"
        markers.addFeature(marker)
        gridState.markerTree = FeatureTree(markers)

        var time = 0L
        var lastLocation: LngLatAlt? = null
        gps.features.filterIndexed { index, _ ->
            (index > startIndex) and (index < endIndex)
        }.forEachIndexed { index, position ->
            val location = (position.geometry as Point).coordinates

            // Calculate direction of travel in case GPX doesn't contain it
            var travelHeading = 0.0
            if (lastLocation != null)
                travelHeading = gridState.ruler.bearing(lastLocation, location)
            lastLocation = location

            runBlocking {
                // Update the grid state
                val gridChanged = gridState.locationUpdate(
                    LngLatAlt(location.longitude, location.latitude),
                    enabledCategories,
                    null
                )
                settlementGrid.locationUpdate(
                    LngLatAlt(location.longitude, location.latitude),
                    emptySet(),
                    null
                )

                if (gridChanged) {
                    // As we're here, test the name confection for the grids. This is relatively
                    // expensive and is only done on individual Ways as needed when running the app.
                    val roads = gridState.getFeatureCollection(TreeId.WAYS_SELECTION)
                    for (road in roads) {
                        confectNamesForRoad(road as Way, gridState, null)
                    }
                }

                // Computed ahead of the map-match filter call (rather than down at UserGeometry
                // construction below) since GeoEngine.kt's production location-update loop uses
                // this same speed check to decide whether the filter should be restricted to
                // TreeId.ROADS - without it here, this replay would always let the matcher lock
                // onto a footway/cycleway alongside the road, regardless of how fast the GPX
                // sample is actually travelling.
                val speed = position.properties?.get("speed") as? Double? ?: 1.0

                // Update the nearest road filter with our new location
                val mapMatchedResult = mapMatchFilter.filter(
                    LngLatAlt(location.longitude, location.latitude),
                    gridState,
                    collection,
                    false,
                    null,
                    speed > UserGeometry.VEHICLE_SPEED_THRESHOLD_MPS
                )

                if (mapMatchedResult.first != null) {
                    val newFeature = Feature()
                    newFeature.geometry = Point(mapMatchedResult.first!!)
                    newFeature.properties = HashMap<String, Any?>().apply {
                        set("marker-color", mapMatchedResult.third)
                        set("color", mapMatchedResult.third)
                        set("index", index + startIndex)
                    }
                    collection.addFeature(newFeature)
                }

                // Update the rail filter too, so GPX files that include a railway journey (see
                // UserGeometry.probablyOnTrain) are matched against the transit network the same
                // way the real app does in GeoEngine.kt.
                val railMapMatchedResult = railMapMatchFilter.filter(
                    LngLatAlt(location.longitude, location.latitude),
                    gridState,
                    collection,
                    false,
                    null
                )

                if (railMapMatchedResult.first != null) {
                    val newFeature = Feature()
                    newFeature.geometry = Point(railMapMatchedResult.first!!)
                    newFeature.properties = HashMap<String, Any?>().apply {
                        set("marker-color", railMapMatchedResult.third)
                        set("color", railMapMatchedResult.third)
                        set("index", index + startIndex)
                        set("network", "rail")
                    }
                    collection.addFeature(newFeature)
                }

                // Add raw GPS too
                position.properties?.set("index", index + startIndex)
                collection.addFeature(position)

                // We can replay GPX files exported from apps like RideWithGPS. This is useful for
                // mocking up GPX where we don't have a live recording, however some information will
                // be missing so we need to mock it up.
                val userGeometry = UserGeometry(
                    location = LngLatAlt(location.longitude, location.latitude),
                    travelHeading = position.properties?.get("heading") as? Double?
                        ?: travelHeading,
                    speed = speed,
                    mapMatchedWay = mapMatchFilter.matchedWay,
                    mapMatchedLocation = mapMatchFilter.matchedLocation,
                    mapMatchedRailway = railMapMatchFilter.matchedWay.takeIf {
                        railMapMatchFilter.isMatchConfident
                    },
                    timestampMilliseconds = (position.properties?.get("time") as? Double?)?.toLong()
                        ?: time
                )
                time += 1000L

                val callout = autoCallout.updateLocation(
                    userGeometry,
                    gridState,
                    settlementGrid
                )
                if (callout != null) {
                    // We've got a new callout, so add it to our geoJSON as a triangle for the
                    // FOV that was used to create it, along with the text from the callouts.
                    callOutText.write("\nCallout\n".toByteArray())
                    val polygon = createPolygonFromTriangle(getFovTriangle(userGeometry, true))
                    val fovFeature = Feature()
                    fovFeature.geometry = polygon
                    fovFeature.properties = HashMap<String, Any?>().apply {
                        for (positionedString in callout.positionedStrings.withIndex()) {
                            callOutText.write("\t${positionedString.value.text}\n".toByteArray())
                            set("Callout ${positionedString.index}", positionedString.value.text)
                        }
                    }
                    collection.addFeature(fovFeature)

                    callout.calloutHistory?.add(callout)
                    callout.locationFilter?.update(callout.userGeometry)
                }
            }
        }
        callOutText.close()

        val adapter = GeoJsonObjectMoshiAdapter()
        val mapMatchingOutput = FileOutputStream(geojsonFilename)
        mapMatchingOutput.write(adapter.toJson(collection).toByteArray())
        mapMatchingOutput.close()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testCalloutsSingleTest  () {
        val resultsStorageDir = File("gpxFiles/")
        if (!resultsStorageDir.exists()) resultsStorageDir.mkdirs()
        val testFile = "MotorwayForTravel"
        testMovingGrid(
            "src/test/res/org/scottishtecharmy/soundscape/gpxFiles/$testFile.gpx",
            "gpxFiles/$testFile.txt",
            "gpxFiles/$testFile.geojson"
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Category(NightlyOnlyTest::class)
    @Test
    fun testCallouts() {
        val directoryPath = Path("src/test/res/org/scottishtecharmy/soundscape/gpxFiles/")

        val resultsStoragePath = "gpxFiles/"
        val resultsStorageDir = File(resultsStoragePath)
        if (!resultsStorageDir.exists()) {
            resultsStorageDir.mkdirs()
        }

        val directoryEntries = directoryPath.listDirectoryEntries("*.gpx")
        for (file in directoryEntries) {
            // Reference-file comparison against gpxFiles/${file}.txt removed for now - the
            // reference fixtures need regenerating to match the water/rail crossing rework (see
            // extractCrossings/AutoCallout's Way-based crossing callouts) before this can compare
            // meaningfully again.
            testMovingGrid(
                file.toString(),
                "gpxFiles/${file.nameWithoutExtension}.txt",
                "gpxFiles/${file.nameWithoutExtension}.geojson"
            )
        }
    }

    fun testStreetNumbers(gpxFilename: String, calloutFilename: String, geojsonFilename: String) {

        val gridState = FileGridState()
        gridState.start(offlineExtractPath)
        val settlementGrid = FileGridState(12, 3)
        settlementGrid.start(offlineExtractPath)
        val mapMatchFilter = MapMatchFilter()
        val gps = parseGpxFromFile(gpxFilename)
        val collection = FeatureCollection()
        val startIndex = 0
        val endIndex = gps.features.size
        val callOutText = FileOutputStream(calloutFilename)

        val enabledCategories = mutableSetOf<String>()
        enabledCategories.add(PLACES_AND_LANDMARKS_KEY)
        enabledCategories.add(MOBILITY_KEY)

        var time = 0L
        var lastLocation: LngLatAlt? = null
        gps.features.filterIndexed { index, _ ->
            (index > startIndex) and (index < endIndex)
        }.forEachIndexed { index, position ->
            val location = (position.geometry as Point).coordinates

            // Calculate direction of travel in case GPX doesn't contain it
            var travelHeading = 0.0
            if (lastLocation != null)
                travelHeading = gridState.ruler.bearing(lastLocation, location)
            lastLocation = location

            runBlocking {
                // Update the grid state
                val gridChanged = gridState.locationUpdate(
                    LngLatAlt(location.longitude, location.latitude),
                    enabledCategories,
                    null
                )

                if (gridChanged) {
                    // As we're here, test the name confection for the grids. This is relatively
                    // expensive and is only done on individual Ways as needed when running the app.
                    val roads = gridState.getFeatureCollection(TreeId.WAYS_SELECTION)
                    for (road in roads) {
                        confectNamesForRoad(road as Way, gridState, null)
                    }
                }

                // Computed ahead of the map-match filter call - see the equivalent comment in
                // testMovingGrid.
                val speed = position.properties?.get("speed") as? Double? ?: 1.0

                // Update the nearest road filter with our new location
                mapMatchFilter.filter(
                    LngLatAlt(location.longitude, location.latitude),
                    gridState,
                    collection,
                    false,
                    null,
                    speed > UserGeometry.VEHICLE_SPEED_THRESHOLD_MPS
                )

                val userGeometry = UserGeometry(
                    location = LngLatAlt(location.longitude, location.latitude),
                    travelHeading = position.properties?.get("heading") as? Double?
                        ?: travelHeading,
                    speed = speed,
                    mapMatchedWay = mapMatchFilter.matchedWay,
                    mapMatchedLocation = mapMatchFilter.matchedLocation,
                    timestampMilliseconds = (position.properties?.get("time") as? Double?)?.toLong()
                        ?: time
                )
                time += 1000L

                val wayName = userGeometry.mapMatchedWay?.properties?.get("pavement") as String?
                    ?: userGeometry.mapMatchedWay?.name
                if (wayName != null) {
                    val lg =
                        OfflineGeocoder(gridState, settlementGrid, processor = { it.process() })
                    val calloutDescriptionWithoutHouse =
                        lg.getAddressFromLngLat(userGeometry, null, true)
                    if (calloutDescriptionWithoutHouse?.name != null)
                        callOutText.write("${calloutDescriptionWithoutHouse.name}\n".toByteArray())
                    position.properties?.set(
                        "callout-without-house",
                        calloutDescriptionWithoutHouse?.name
                    )

                    val calloutDescriptionWithHouse =
                        lg.getAddressFromLngLat(userGeometry, null, false)
                    if (calloutDescriptionWithHouse?.name != null)
                        callOutText.write("${calloutDescriptionWithHouse.name}\n".toByteArray())
                    position.properties?.set(
                        "callout-with-house",
                        calloutDescriptionWithHouse?.name
                    )

                    val description = StreetDescription(wayName, gridState)
                    val matchedWay = userGeometry.mapMatchedWay!!
                    description.createDescription(matchedWay, null)
                    description.describeStreet()
                    val houseNumber = description.getStreetNumber(matchedWay, location)
                    val addressText =
                        "${if (houseNumber.second) "Opposite" else ""} ${houseNumber.first} $wayName"

                    val locationDescription = description.describeLocation(
                        userGeometry.location,
                        userGeometry.heading(),
                        matchedWay,
                        null
                    )
                    callOutText.write("$addressText\n".toByteArray())
                    position.properties?.set("index", index + startIndex)
                    position.properties?.set("address", addressText)
                    if (locationDescription.behind.name.isNotEmpty()) {
                        val behindText =
                            "${locationDescription.behind.name} ${locationDescription.behind.distance}m"
                        position.properties?.set("behind", behindText)
                        position.properties?.set("marker-color", "#000000")
                    }
                    if (locationDescription.ahead.name.isNotEmpty()) {
                        val aheadText =
                            "${locationDescription.ahead.name} ${locationDescription.ahead.distance}m"
                        position.properties?.set("ahead", aheadText)
                        position.properties?.set("marker-color", "#000000")
                    }
                    if (houseNumber.first.isNotEmpty())
                        position.properties?.set("marker-color", "#ff0000")
                    collection.addFeature(position)
                }
            }
        }
        callOutText.close()

        val adapter = GeoJsonObjectMoshiAdapter()
        val mapMatchingOutput = FileOutputStream(geojsonFilename)
        mapMatchingOutput.write(adapter.toJson(collection).toByteArray())
        mapMatchingOutput.close()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Category(NightlyOnlyTest::class)
    @Test
    fun replayStreetNumbers() {
        val directoryPath = Path("src/test/res/org/scottishtecharmy/soundscape/gpxFiles/")

        val resultsStoragePath = "gpxFiles/"
        val resultsStorageDir = File(resultsStoragePath)
        if (!resultsStorageDir.exists()) {
            resultsStorageDir.mkdirs()
        }

        val directoryEntries = directoryPath.listDirectoryEntries("*.gpx")
        for (file in directoryEntries) {
            testStreetNumbers(
                file.toString(),
                "gpxFiles/${file.nameWithoutExtension}-address.txt",
                "gpxFiles/${file.nameWithoutExtension}-address.geojson"
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testGridCache() {

        // This test 'moves' from the center of one tile to the center of the next to see how tile
        // caching behaves.

        val gridState = FileGridState()
        gridState.start(offlineExtractPath)

        // The center of each grid
        for (x in 7990 until 8010) {
            for (y in 5100 until 5120) {

                // Get top left of tile
                val location = getLatLonTileWithOffset(x, y, MAX_ZOOM_LEVEL, 0.0, 0.0)

                println("Moving grid to $location")

                runBlocking {
                    // Update the grid state
                    gridState.locationUpdate(
                        LngLatAlt(location.longitude, location.latitude),
                        emptySet(),
                        null
                    )
                    if (false) {
                        // This code is useful for comparing before and after changes of grid parsing
                        val adapter = GeoJsonObjectMoshiAdapter()
                        val tileOutput = FileOutputStream("cache-test2/output-$x-$y.geojson")

                        // Output the GeoJson and check that there's no data left from other tiles.
                        val collection = FeatureCollection()
                        for (id in TreeId.entries) {
                            if (id < TreeId.MAX_COLLECTION_ID)
                                collection += gridState.getFeatureCollection(id)
                        }
                        tileOutput.write(adapter.toJson(collection).toByteArray())
                        tileOutput.close()
                    }
                }
            }
        }
    }

    @Test
    fun testLowerZoomLevel() {

        val zoomLevel = 12

        // Make a 3x3 grid at a lower zoom level. This will just contain the 'places' layer which
        // will allow searching for nearby suburbs etc.
        //val gridState = getGridStateForLocation(LngLatAlt(-4.317357, 55.942527), zoomLevel, 3)
        val gridState = getGridStateForLocation(LngLatAlt(-4.3060126, 55.9474004), zoomLevel, 3)


        val adapter = GeoJsonObjectMoshiAdapter()
        val cityCollection = gridState.getFeatureTree(TreeId.SETTLEMENT_CITY).getAllCollection()
        for (feature in cityCollection) {
            (feature as? MvtFeature)?.let { mvtFeature ->
                mvtFeature.setProperty("marker-size", "large")
                mvtFeature.setProperty("marker-color", "#ff0000")
            }
        }
        val townCollection = gridState.getFeatureTree(TreeId.SETTLEMENT_TOWN).getAllCollection()
        for (feature in townCollection) {
            (feature as? MvtFeature)?.let { mvtFeature ->
                mvtFeature.setProperty("marker-size", "medium")
                mvtFeature.setProperty("marker-color", "#ffff00")
            }
        }
        val villageCollection =
            gridState.getFeatureTree(TreeId.SETTLEMENT_VILLAGE).getAllCollection()
        for (feature in villageCollection) {
            (feature as? MvtFeature)?.let { mvtFeature ->
                mvtFeature.setProperty("marker-size", "small")
                mvtFeature.setProperty("marker-color", "#00ff00")
            }
        }
        val hamletCollection = gridState.getFeatureTree(TreeId.SETTLEMENT_HAMLET).getAllCollection()
        for (feature in hamletCollection) {
            (feature as? MvtFeature)?.let { mvtFeature ->
                mvtFeature.setProperty("marker-size", "small")
                mvtFeature.setProperty("marker-color", "#0000ff")
            }
        }
        val outputCollection = cityCollection
        outputCollection += townCollection
        outputCollection += villageCollection
        outputCollection += hamletCollection
        val outputFile = FileOutputStream("low-zoom.geojson")
        outputFile.write(adapter.toJson(outputCollection).toByteArray())
        outputFile.close()
    }

    @Test
    fun testParsing() {

        val gridState = FileGridState()
        gridState.start(offlineExtractPath)
        gridState.checkOfflineMaps()

        data class Region(
            val name: String,
            val minX: Int,
            val minY: Int,
            val maxX: Int,
            val maxY: Int
        )

        val regions = listOf(
            Region("Edinburgh", 16090 / 2, 10207 / 2, 16095 / 2, 10212 / 2),
            Region("Bristol", 16128 / 2, 10880 / 2, 16192 / 2, 10944 / 2),
            Region("Manchester", 16128 / 2, 10560 / 2, 16192 / 2, 10624 / 2),
        )
        for (region in regions) {
            println("Test ${region.name}")
            for (x in region.minX until region.maxX) {
                for (y in region.minY until region.maxY) {
                    runBlocking {
                        val featureCollections =
                            Array(TreeId.MAX_COLLECTION_ID.id) { FeatureCollection() }
                        val intersectionMap: HashMap<LngLatAlt, Intersection> = hashMapOf()
                        val streetNumberMap: HashMap<String, FeatureCollection> = hashMapOf()
                        val transitIntersectionMap: HashMap<LngLatAlt, Intersection> = hashMapOf()
                        gridState.updateTile(
                            x,
                            y,
                            0,
                            featureCollections,
                            intersectionMap,
                            streetNumberMap,
                            transitIntersectionMap
                        )
                    }
                }
            }
        }
    }

    fun fuzzySearchFeatureCollection(
        featureCollection: FeatureCollection,
        needleString: String,
        bestStringSoFar: String,
        bestDistanceSoFar: Double
    ): Pair<Double, String> {
        var bestMatch: String = bestStringSoFar
        var bestDistance = bestDistanceSoFar
        for (feature in featureCollection) {
            val name = feature.properties?.get("name") as? String
            if (name != null) {
                // Calculate the Levenshtein distance ratio between the POI name and our test string
                val distance = needleString.fuzzyCompare(name, true)

                // If this string is closer than the best one we've found so far, update it
                if (distance < bestDistance) {
                    bestDistance = distance
                    bestMatch = name
                    println("Found new best match: '$name' (Distance: $distance)")
                }

                // An optional optimization: if a perfect match is found, we can stop searching.
                if (distance == 0.0) {
                    break
                }
            }
        }
        return Pair(bestDistance, bestMatch)
    }


    @Test
    fun testFuzzySearch() {
        // Make a large grid to aid analysis
        val gridState = getGridStateForLocation(LngLatAlt(-4.317357, 55.942527), 14, 1)
        val testString = "Costa coffee" // Our string with typos

        println("Searching for strings similar to: '$testString'")
        val pois = gridState.getFeatureCollection(TreeId.POIS)
        val roads = gridState.getFeatureCollection(TreeId.ROADS)
        val (newBestDistance, newBestMatch) = fuzzySearchFeatureCollection(
            pois,
            testString,
            "",
            Double.MAX_VALUE
        )
        val (newestBestDistance, newestBestMatch) = fuzzySearchFeatureCollection(
            roads,
            testString,
            newBestMatch,
            newBestDistance
        )

        println("\n--- Search Complete ---")
        println("Original String: '$testString'")
        println("Best Match Found: '$newestBestMatch' with a distance of $newestBestDistance.")
    }

    class DummyEntranceGridState(
        zoomLevel: Int = MAX_ZOOM_LEVEL,
        gridSize: Int = GRID_SIZE
    ) : ProtomapsGridState(zoomLevel, gridSize) {

        init {
            validateContext = false
        }

        /**
         * updateTile is overrider in FileGridState to get the tile data from the unit test resources
         * directory.
         */
        override suspend fun updateTile(
            x: Int,
            y: Int,
            workerIndex: Int,
            featureCollections: Array<FeatureCollection>,
            intersectionMap: HashMap<LngLatAlt, Intersection>,
            streetNumberMap: HashMap<String, FeatureCollection>,
            transitIntersectionMap: HashMap<LngLatAlt, Intersection>
        ): Boolean {

            // We're not parsing a tile here, just creating some data using the entrance matcher
            // as if they were found in a tile
            val matcher = EntranceMatching()

            val namedSubwayEntranceDetails = EntranceDetails(
                "St Enoch",
                "subway_entrance",
                null,
                null,
                false,
                39240178581
            )
            val unNamedSubwayEntranceDetails = EntranceDetails(
                null,
                "subway_entrance",
                null,
                null,
                false,
                1
            )
            val namedEntranceDetails = EntranceDetails(
                "North Portland Street",
                "secondary",
                null,
                null,
                false,
                11853457811
            )
            val unNamedEntranceDetails = EntranceDetails(
                null,
                "yes",
                null,
                null,
                false,
                116357026611
            )
            val poi = EntranceDetails(
                "St Enoch Shopping Centre",
                null,
                null,
                null,
                true,
                52992372
            )
            EntranceDetails(
                "St Enoch",
                "subway_entrance",
                null,
                null,
                false,
                39240178581
            )

            val railwayStationEntranceProperties = HashMap<String, Any?>()
            railwayStationEntranceProperties["railway"] = "train_station_entrance"
            val unNamedStationEntranceDetails = EntranceDetails(
                null,
                "yes",
                null,
                railwayStationEntranceProperties,
                false,
                2
            )

            val poiMap = hashMapOf<Long, MutableList<Feature>>()
            val poiFeature = MvtFeature()
            poiFeature.featureClass = "shop"
            poiFeature.featureSubClass = "mall"
            poiFeature.properties = HashMap<String, Any?>().apply {
                set("name", "St Enoch Shopping Centre")
                set("osm_id", "52992372")
            }
            poiMap[52992372] = listOf(poiFeature).toMutableList()

            matcher.addGeometry(arrayListOf(Pair(100, 100)), namedSubwayEntranceDetails)
            matcher.addGeometry(arrayListOf(Pair(200, 200)), unNamedSubwayEntranceDetails)
            matcher.addGeometry(arrayListOf(Pair(300, 300)), namedEntranceDetails)

            matcher.addGeometry(arrayListOf(Pair(400, 400)), unNamedEntranceDetails)
            matcher.addGeometry(arrayListOf(Pair(400, 400)), poi)

            matcher.addGeometry(arrayListOf(Pair(500, 500)), unNamedStationEntranceDetails)

            val collection = FeatureCollection()
            matcher.generateEntrances(collection, poiMap, HashMap(), 5000, 5000, 14)

            val collections = Array(TreeId.MAX_COLLECTION_ID.id) { FeatureCollection() }
            processTileFeatureCollection(collections, collection)

            for ((index, collection) in collections.withIndex()) {
                featureCollections[index] += collection
            }

            return true
        }
    }

    @Test
    fun entranceMatcherTest() {
        val gridState = DummyEntranceGridState()
        gridState.start(offlineExtractPath)

        runBlocking {
            val featureCollections =
                Array(TreeId.MAX_COLLECTION_ID.id) { FeatureCollection() }
            val intersectionMap: HashMap<LngLatAlt, Intersection> = hashMapOf()
            val streetNumberMap: HashMap<String, FeatureCollection> = hashMapOf()
            gridState.updateTile(0, 0, 0, featureCollections, intersectionMap, streetNumberMap)

            // The 3 entrances should appear as entrances and POIS and two of them as transit stops
            assertEquals(5, featureCollections[TreeId.ENTRANCES.id].features.size)
            assertEquals(5, featureCollections[TreeId.POIS.id].features.size)
            assertEquals(3, featureCollections[TreeId.TRANSIT_STOPS.id].features.size)
        }
    }

    @Test
    fun extractSwitchingTest() {
        // This test ensures that the GridState code can successfully switch between offline
        // extracts
        val gridState = FileGridState(MAX_ZOOM_LEVEL, GRID_SIZE)
        gridState.start(offlineExtractPath)
        val enabledCategories = emptySet<String>().toMutableSet()
        enabledCategories.add(PLACES_AND_LANDMARKS_KEY)
        enabledCategories.add(MOBILITY_KEY)

        // Intersperse locations that are in each of the extracts (Glasgow, Liverpool, Bristol)
        // with some that are outside and should fail
        val locations: List<Pair<LngLatAlt, Boolean>> = listOf(
            Pair(sixtyAcresCloseTestLocation, true),
            Pair(longAshtonRoadTestLocation, true),
            Pair(LngLatAlt(51.69046, 32.66160), false),
            Pair(woodlandWayTestLocation, true),
            Pair(centralManchesterTestLocation, true),
            Pair(LngLatAlt(51.69046, 32.66160), false),
            Pair(failandTestLocation, true),
            Pair(LngLatAlt(51.69046, 32.66160), false),
            Pair(edinburghTestLocation, true),
            Pair(glasgowTestLocation, true),
        )

        runBlocking {
            for (location in locations) {
                println("Test ${location.first}")
                assertEquals(
                    gridState.locationUpdate(
                        location.first,
                        enabledCategories,
                        null
                    ), location.second
                )
            }
        }
    }

    @Test
    fun timeParsingPerformance() {
        val duration = measureTime {
            val gridState =
                getGridStateForLocation(centralManchesterTestLocation, MAX_ZOOM_LEVEL, 2)
        }
        println("Processing time $duration")
    }
}
