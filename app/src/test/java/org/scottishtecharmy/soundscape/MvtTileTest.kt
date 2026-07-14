package org.scottishtecharmy.soundscape

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Test
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
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayEnd
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayType
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.convertBackToTileCoordinates
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.sampleToFractionOfTile
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.vectorTileToGeoJson
import org.scottishtecharmy.soundscape.geoengine.processTileFeatureCollection
import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geoengine.utils.ResourceMapper
import org.scottishtecharmy.soundscape.geoengine.utils.confectNamesForRoad
import org.scottishtecharmy.soundscape.geoengine.utils.createPolygonFromTriangle
import org.scottishtecharmy.soundscape.geoengine.utils.geocoders.OfflineGeocoder
import org.scottishtecharmy.soundscape.geoengine.utils.geocoders.StreetDescription
import org.scottishtecharmy.soundscape.geoengine.utils.getDistanceToFeature
import org.scottishtecharmy.soundscape.geoengine.utils.getFovTriangle
import org.scottishtecharmy.soundscape.geoengine.utils.getLatLonTileWithOffset
import org.scottishtecharmy.soundscape.geoengine.utils.gpx.parseGpx
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.geoengine.utils.searchFeaturesByName
import org.scottishtecharmy.soundscape.geoengine.utils.traverseIntersectionsConfectingNames
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.geojsonparser.moshi.GeoJsonObjectMoshiAdapter
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
import vector_tile.Tile

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
    gridSize: Int
): GridState {

    val gridState = FileGridState(zoomLevel, gridSize)
    gridState.start(offlineExtractPath)
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
     * Prototype test for reading OSM `ref` (route number, e.g. "B8050") out of the
     * `transportation_name` layer and attaching it to the corresponding `transportation` Way, for
     * use in travel-mode callouts like "On the A81" for roads that only carry a route number and
     * no common name.
     */
    @Test
    fun testTransportationNameRef() {
        val tileX = 15990
        val tileY = 10213
        val tileFile = File("src/main/assets/${tileX}x${tileY}.mvt")
        val tile = Tile.ADAPTER.decode(tileFile.readBytes())

        val intersectionMap: HashMap<LngLatAlt, Intersection> = hashMapOf()
        val streetNumberMap: HashMap<String, FeatureCollection> = hashMapOf()
        val geojson = vectorTileToGeoJson(
            tileX,
            tileY,
            tile,
            intersectionMap,
            streetNumberMap,
            true,
            15
        )

        val roads = geojson[TreeId.ROADS_AND_PATHS.id]
        val parkRoad = roads.features.find { (it as? MvtFeature)?.name == "Park Road" }
        assertEquals("B8050", (parkRoad as? MvtFeature)?.properties?.get("ref"))
    }

    /**
     * The M8 motorway through central Glasgow carries `ref=M8` but no `name` tag (found by
     * scanning TreeId.ROADS_AND_PATHS around [glasgowTestLocation] for Ways with a ref and no
     * name). This is an end-to-end check that travel-mode reverse geocoding falls back to the
     * ref ("M8") instead of the generic class-based description ("Motorway") it used to produce,
     * phrased as "On M8" since we're confirmed to be on the road, not just near it.
     */
    @Test
    fun testTravelCalloutForUnnamedRefRoad() {
        val location = LngLatAlt(-4.254034459590912, 55.87014482990583)
        val gridState = getGridStateForLocation(location, MAX_ZOOM_LEVEL, 3)
        val settlementGrid = getGridStateForLocation(location, 12, 3)

        val userGeometry = UserGeometry(location = location, speed = 15.0)
        val result = describeReverseGeocode(userGeometry, gridState, settlementGrid, null)

        assertNotNull(result)
        assertEquals("On M8 and close to Cowcaddens", result!!.text)
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
        assertEquals("2", (robroyston as MvtFeature).properties?.get("ref"))

        val primaryGridState = getGridStateForLocation(LngLatAlt(-3.5084, 55.8980), MAX_ZOOM_LEVEL, 3)
        val primaryJunctions =
            primaryGridState.getFeatureTree(TreeId.HIGHWAY_JUNCTIONS).getAllCollection()
        val cousland = primaryJunctions.features.find { (it as? MvtFeature)?.name == "Cousland Interchange" }
        assertNotNull(cousland)
        assertEquals("primary", (cousland as MvtFeature).properties?.get("class"))
    }

    /**
     * End-to-end check that travel-mode reverse geocoding combines the current road with a
     * nearby highway junction, e.g. "On M80 at Junction 2, Robroyston" rather than just naming
     * the road or the junction alone.
     */
    @Test
    fun testTravelCalloutForHighwayJunction() {
        val location = LngLatAlt(-4.1848, 55.8854)
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
     * Merchant City, 200 metres since Argyle Street" - a standalone since-distance with nothing
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
            "On Fake Railway Line and close to Merchant City, 200 metres since Argyle Street",
            result!!.text
        )

        // A little further still - the spoken distance moves on, but the dedup key (road,
        // settlement, station - no distance) stays identical so history can suppress the repeat.
        val evenFurtherAlong = gridState.ruler.offset(argyleStreetStation, 0.0, 250.0)
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
            result.text != secondResult!!.text
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
            for (c in roadCoordinates) warmup.filter(c, gridState, FeatureCollection(), false)
        }

        val roadOnlyTime = measureTime {
            val filter = MapMatchFilter()
            for (c in roadCoordinates) filter.filter(c, gridState, FeatureCollection(), false)
        }

        val railOnlyTime = measureTime {
            val filter = MapMatchFilter(networkTree = TreeId.TRANSIT)
            for (c in railCoordinates) filter.filter(c, gridState, FeatureCollection(), false)
        }

        val bothTime = measureTime {
            val roadFilter = MapMatchFilter()
            val railFilter = MapMatchFilter(networkTree = TreeId.TRANSIT)
            for (i in 0 until minOf(roadCoordinates.size, railCoordinates.size)) {
                roadFilter.filter(roadCoordinates[i], gridState, FeatureCollection(), false)
                railFilter.filter(railCoordinates[i], gridState, FeatureCollection(), false)
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
            runBlocking { gridState.locationUpdate(coordinate, emptySet()) }
            railMapMatchFilter.filter(coordinate, gridState, FeatureCollection(), false)
        }

        val matched = railMapMatchFilter.matchedWay
        assertNotNull("Expected map matching to lock onto a railway Way", matched)
        assertTrue(
            "Expected the matched Way to be part of the railway network, got featureType=${matched!!.featureType}",
            matched.featureType == "rail" || matched.featureType == "transit"
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
     *  2. Road destinations
     *  3. POI destinations
     *  4. Dead ends
     *
     * Once we add water and railways, we can consider adding 'along canal' and 'along railway' type
     * descriptions too.
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

                // Update the nearest road filter with our new location
                val mapMatchedResult = mapMatchFilter.filter(
                    LngLatAlt(location.longitude, location.latitude),
                    gridState,
                    collection,
                    false,
                    null
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
                    false
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
                    speed = position.properties?.get("speed") as? Double? ?: 1.0,
                    mapMatchedWay = mapMatchFilter.matchedWay,
                    mapMatchedLocation = mapMatchFilter.matchedLocation,
                    mapMatchedRailway = railMapMatchFilter.matchedWay,
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
    fun testCalloutsTrain1Only() {
        val resultsStorageDir = File("gpxFiles/")
        if (!resultsStorageDir.exists()) resultsStorageDir.mkdirs()
        testMovingGrid(
            "src/test/res/org/scottishtecharmy/soundscape/gpxFiles/train-1.gpx",
            "gpxFiles/train-1-debug.txt",
            "gpxFiles/train-1-debug.geojson"
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
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
            testMovingGrid(
                file.toString(),
                "gpxFiles/${file.nameWithoutExtension}.txt",
                "gpxFiles/${file.nameWithoutExtension}.geojson"
            )
            val referenceFile = File("$directoryPath/${file.nameWithoutExtension}.txt")
            if (false) {//referenceFile.exists()) {
                // Compare our new callout file with the reference one.
                val generatedFile = File("gpxFiles/${file.nameWithoutExtension}.txt")

                // Read all lines from both files
                val generatedLines = generatedFile.readLines()
                val referenceLines = referenceFile.readLines()

                // Assert that the contents are identical.
                println("Compare ${file.nameWithoutExtension} results to reference")

                for ((index, line) in referenceLines.withIndex()) {
                    assertEquals(
                        "File content for ${file.nameWithoutExtension} does not match the reference file.",
                        line,
                        generatedLines[index]
                    )
                }
            }
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

                // Update the nearest road filter with our new location
                mapMatchFilter.filter(
                    LngLatAlt(location.longitude, location.latitude),
                    gridState,
                    collection,
                    false,
                    null
                )

                val userGeometry = UserGeometry(
                    location = LngLatAlt(location.longitude, location.latitude),
                    travelHeading = position.properties?.get("heading") as? Double?
                        ?: travelHeading,
                    speed = position.properties?.get("speed") as? Double? ?: 1.0,
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
                        gridState.updateTile(
                            x,
                            y,
                            0,
                            featureCollections,
                            intersectionMap,
                            streetNumberMap
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
