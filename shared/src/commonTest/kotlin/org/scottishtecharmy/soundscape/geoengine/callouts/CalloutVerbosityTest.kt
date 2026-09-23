@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.scottishtecharmy.soundscape.geoengine.callouts

import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Intersection
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayEnd
import org.scottishtecharmy.soundscape.geoengine.utils.DESTINATION_KIND
import org.scottishtecharmy.soundscape.geoengine.utils.DESTINATION_KIND_ROAD
import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geoengine.utils.SuperCategoryId
import org.scottishtecharmy.soundscape.geoengine.utils.getDestinationCoordinate
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.preferences.PreferenceKeys
import org.scottishtecharmy.soundscape.preferences.PreferencesListener
import org.scottishtecharmy.soundscape.preferences.PreferencesProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** In-memory [PreferencesProvider] holding only what a test sets on it. */
private class VerbosityPreferences : PreferencesProvider {
    private val booleans = mutableMapOf<String, Boolean>()
    private val strings = mutableMapOf<String, String>()

    override fun getBoolean(key: String, default: Boolean): Boolean = booleans[key] ?: default
    override fun getString(key: String, default: String): String = strings[key] ?: default
    override fun getFloat(key: String, default: Float): Float = default

    override fun putBoolean(key: String, value: Boolean) { booleans[key] = value }
    override fun putString(key: String, value: String) { strings[key] = value }

    override fun clearAll() { booleans.clear(); strings.clear() }

    override fun addListener(listener: PreferencesListener) {}
    override fun removeListener(listener: PreferencesListener) {}
}

class CalloutVerbosityTest {

    private fun road(roadName: String?, highway: String) = Way().apply {
        name = roadName
        featureType = "highway"
        featureValue = highway
    }

    private fun junction(vararg ways: Way) = Intersection().apply { members.addAll(ways) }

    @Test
    fun roadTiersFollowHighwayClass() {
        assertEquals(RoadTier.MAJOR, RoadTier.of(road("A81", "primary")))
        assertEquals(RoadTier.MAJOR, RoadTier.of(road("Hope Street", "tertiary")))
        assertEquals(RoadTier.MINOR, RoadTier.of(road("Station Road", "minor")))
        // Buchanan Street: pedestrianised, but a street rather than a footpath
        assertEquals(RoadTier.MINOR, RoadTier.of(road("Buchanan Street", "pedestrian")))
        assertEquals(RoadTier.OTHER, RoadTier.of(road(null, "service")))
        assertEquals(RoadTier.OTHER, RoadTier.of(road(null, "footway")))
        assertEquals(RoadTier.OTHER, RoadTier.of(Way().apply { featureType = "railway" }))
    }

    @Test
    fun aServiceRoadOffAStreetOnlyMeetsTheLowestTier() {
        val street = road("Station Road", "minor")
        val intersection =
            junction(street, road("Station Road", "minor"), road(null, "service"))

        assertTrue(intersectionMeetsRoadTier(intersection, street, RoadTier.OTHER))
        assertFalse(intersectionMeetsRoadTier(intersection, street, RoadTier.MINOR))
        assertFalse(intersectionMeetsRoadTier(intersection, street, RoadTier.MAJOR))
    }

    @Test
    fun aSideStreetMeetsMinorButNotMajor() {
        val street = road("Main Street", "tertiary")
        val intersection =
            junction(street, road("Main Street", "tertiary"), road("Park Road", "minor"))

        assertTrue(intersectionMeetsRoadTier(intersection, street, RoadTier.MINOR))
        // The major road here is the one the user is already on, so it doesn't count
        assertFalse(intersectionMeetsRoadTier(intersection, street, RoadTier.MAJOR))
    }

    @Test
    fun reachingAMainRoadFromASideStreetMeetsMajor() {
        val street = road("Park Road", "minor")
        val intersection =
            junction(street, road("Main Street", "tertiary"), road("Main Street", "tertiary"))

        assertTrue(intersectionMeetsRoadTier(intersection, street, RoadTier.MAJOR))
    }

    @Test
    fun pavementsDoNotCountTowardsTheTier() {
        val street = road("Park Road", "minor")
        val pavement = road("Main Street", "tertiary").apply {
            properties = hashMapOf("footway" to "sidewalk")
        }
        assertFalse(
            intersectionMeetsRoadTier(
                junction(street, pavement, road(null, "footway")), street, RoadTier.MINOR
            )
        )
    }

    /**
     * An unnamed footpath leaving [junction] from its START end, whose far end has been confected
     * as leading to something of [kind] - see DESTINATION_KIND.
     */
    private fun pathAwayFrom(junction: Intersection, kind: String?) = road(null, "footway").apply {
        intersections[WayEnd.START.id] = junction
        properties = hashMapOf<String, Any?>().apply {
            if (kind != null) {
                put("destination:forward", "Somewhere")
                put("$DESTINATION_KIND:forward", kind)
            }
        }
    }

    private fun meetsMinorWith(path: (Intersection) -> Way): Boolean {
        val street = road("Station Road", "minor")
        val intersection = junction(street, road("Station Road", "minor"))
        intersection.members.add(path(intersection))
        return intersectionMeetsRoadTier(intersection, street, RoadTier.MINOR)
    }

    @Test
    fun aNamedFootpathCountsAsAStreet() {
        assertTrue(meetsMinorWith { road("West Highland Way", "footway") })
    }

    @Test
    fun aPavementNamedFromItsRoadDoesNotCount() {
        assertFalse(meetsMinorWith {
            road("Pavement next to Main Street", "footway").apply {
                properties = hashMapOf("pavement" to "Main Street")
            }
        })
    }

    @Test
    fun aPathLeadingToAStreetOrLandmarkCountsAsAStreet() {
        assertTrue(meetsMinorWith { pathAwayFrom(it, DESTINATION_KIND_ROAD) })
        assertTrue(meetsMinorWith { pathAwayFrom(it, SuperCategoryId.LANDMARK.name) })
        assertTrue(meetsMinorWith { pathAwayFrom(it, SuperCategoryId.MARKER.name) })
    }

    @Test
    fun aPathLeadingToAShopCarParkOrNowhereDoesNot() {
        assertFalse(meetsMinorWith { pathAwayFrom(it, SuperCategoryId.PLACE.name) })
        assertFalse(meetsMinorWith { pathAwayFrom(it, SuperCategoryId.SAFETY.name) })
        assertFalse(meetsMinorWith { pathAwayFrom(it, null) })
    }

    /** Only where the path goes *from here* counts - not where it came from. */
    @Test
    fun aPathIsJudgedByWhereItLeadsAwayFromTheJunction() {
        assertFalse(meetsMinorWith { junction ->
            pathAwayFrom(junction, DESTINATION_KIND_ROAD).apply {
                intersections[WayEnd.START.id] = null
                intersections[WayEnd.END.id] = junction
            }
        })
    }

    /**
     * Walking north up an unnamed path to a junction, where the path back south leads to a street
     * (the one just walked from) and the others lead nowhere in particular. The path back mustn't
     * count - without the bearing it does.
     */
    @Test
    fun thePathTheUserArrivedByDoesNotCount() {
        val centre = LngLatAlt(-4.3, 55.94)
        val intersection = Intersection().apply { location = centre }
        fun leaving(bearing: Double, kind: String?) = pathAwayFrom(intersection, kind).apply {
            geometry = LineString(centre, getDestinationCoordinate(centre, bearing, 20.0))
        }
        val cameFrom = leaving(180.0, DESTINATION_KIND_ROAD)
        intersection.members.addAll(
            listOf(cameFrom, leaving(270.0, null), leaving(90.0, SuperCategoryId.PLACE.name))
        )
        // The user is matched to a different, unnamed Way than the one reaching the junction
        val matched = road(null, "footway")

        assertTrue(intersectionMeetsRoadTier(intersection, matched, RoadTier.MINOR))
        assertFalse(
            intersectionMeetsRoadTier(
                intersection, matched, RoadTier.MINOR, comingFromBearing = 180.0
            )
        )
    }

    private fun poi(poiName: String, category: SuperCategoryId, value: String = "") =
        MvtFeature().apply {
            name = poiName
            superCategory = category
            featureValue = value
        }

    private val everything = setOf(PlacesToCallOut.EVERYTHING)

    @Test
    fun quietKeepsLandmarksAndMarkersOnly() {
        val quiet = CalloutVerbosity.QUIET
        assertTrue(poiAllowedBySettings(poi("Park", SuperCategoryId.LANDMARK), quiet, everything))
        assertTrue(poiAllowedBySettings(poi("Home", SuperCategoryId.MARKER), quiet, everything))
        assertFalse(poiAllowedBySettings(poi("Shop", SuperCategoryId.PLACE), quiet, everything))
        assertFalse(poiAllowedBySettings(poi("Steps", SuperCategoryId.MOBILITY), quiet, everything))
        assertTrue(
            poiAllowedBySettings(
                poi("Shop", SuperCategoryId.PLACE), CalloutVerbosity.DETAILED, everything
            )
        )
    }

    @Test
    fun aNarrowerChoiceDropsOtherPlacesBusStopsAndLandmarks() {
        val food = setOf(PlacesToCallOut.FOOD_AND_DRINK)
        val detailed = CalloutVerbosity.DETAILED
        assertTrue(
            poiAllowedBySettings(poi("Cafe", SuperCategoryId.PLACE, "cafe"), detailed, food)
        )
        assertFalse(
            poiAllowedBySettings(poi("Boots", SuperCategoryId.PLACE, "chemist"), detailed, food)
        )
        assertFalse(
            poiAllowedBySettings(poi("Stop", SuperCategoryId.MOBILITY, "bus_stop"), detailed, food)
        )
        // Landmarks are a kind of their own now, so they go unless they were ticked as well
        assertFalse(poiAllowedBySettings(poi("Park", SuperCategoryId.LANDMARK), detailed, food))
        // ...whereas getting about - crossings, steps, lifts - isn't a kind anyone ticks
        assertTrue(
            poiAllowedBySettings(poi("Lift", SuperCategoryId.MOBILITY, "elevator"), detailed, food)
        )
        assertTrue(poiAllowedBySettings(poi("Home", SuperCategoryId.MARKER), detailed, food))
    }

    /** The point of a list rather than a single choice: landmarks *and* the bus stops. */
    @Test
    fun severalKindsTogether() {
        val chosen = setOf(PlacesToCallOut.LANDMARKS, PlacesToCallOut.TRANSIT)
        val detailed = CalloutVerbosity.DETAILED
        assertTrue(poiAllowedBySettings(poi("Park", SuperCategoryId.LANDMARK), detailed, chosen))
        assertTrue(
            poiAllowedBySettings(
                poi("Stop", SuperCategoryId.MOBILITY, "bus_stop"), detailed, chosen
            )
        )
        assertFalse(
            poiAllowedBySettings(poi("Cafe", SuperCategoryId.PLACE, "cafe"), detailed, chosen)
        )
        assertTrue(chosen.includesBusAndTramStops)
    }

    @Test
    fun transitIncludesBusStops() {
        assertTrue(setOf(PlacesToCallOut.TRANSIT).includesBusAndTramStops)
        assertTrue(everything.includesBusAndTramStops)
        assertFalse(setOf(PlacesToCallOut.LANDMARKS).includesBusAndTramStops)
    }

    @Test
    fun landmarksOnlyAndNoPlaces() {
        val detailed = CalloutVerbosity.DETAILED
        val landmarks = setOf(PlacesToCallOut.LANDMARKS)
        assertTrue(poiAllowedBySettings(poi("Park", SuperCategoryId.LANDMARK), detailed, landmarks))
        assertFalse(poiAllowedBySettings(poi("Shop", SuperCategoryId.PLACE), detailed, landmarks))

        val nothing = setOf(PlacesToCallOut.NOTHING)
        assertFalse(poiAllowedBySettings(poi("Park", SuperCategoryId.LANDMARK), detailed, nothing))
        assertFalse(
            poiAllowedBySettings(poi("Lift", SuperCategoryId.MOBILITY, "elevator"), detailed, nothing)
        )
        assertTrue(poiAllowedBySettings(poi("Home", SuperCategoryId.MARKER), detailed, nothing))
    }

    /** What the user ticked is called out even when Quiet would otherwise drop it. */
    @Test
    fun aChosenKindOfPlaceIsCalledOutEvenWhenQuiet() {
        assertTrue(
            poiAllowedBySettings(
                poi("Cafe", SuperCategoryId.PLACE, "cafe"),
                CalloutVerbosity.QUIET,
                setOf(PlacesToCallOut.FOOD_AND_DRINK)
            )
        )
    }

    @Test
    fun theStoredListIsReadBackAsItWasWritten() {
        val chosen = setOf(PlacesToCallOut.TRANSIT, PlacesToCallOut.LANDMARKS)
        assertEquals("Landmarks,Transit", chosen.toPreference())
        assertEquals(chosen, PlacesToCallOut.fromPreference(chosen.toPreference()))
        // A single name, as older versions and the carry-over write it
        assertEquals(setOf(PlacesToCallOut.TRANSIT), PlacesToCallOut.fromPreference("Transit"))
    }

    @Test
    fun unknownPreferenceValuesFallBackToTheOriginalBehaviour() {
        assertEquals(CalloutVerbosity.DETAILED, CalloutVerbosity.fromPreference(null))
        assertEquals(CalloutVerbosity.DETAILED, CalloutVerbosity.fromPreference("Loud"))
        assertEquals(everything, PlacesToCallOut.fromPreference("Shoes"))
        assertEquals(everything, PlacesToCallOut.fromPreference(""))
        // ...but a name it does know, alongside one it doesn't, is still honoured
        assertEquals(
            setOf(PlacesToCallOut.BANKS), PlacesToCallOut.fromPreference("Shoes,Banks")
        )
    }

    /** Everything and No Places each say something about all the others, so they stand alone. */
    @Test
    fun tickingEverythingOrNoPlacesClearsTheRest() {
        val chosen = setOf(PlacesToCallOut.LANDMARKS, PlacesToCallOut.BANKS)

        assertEquals(
            setOf(PlacesToCallOut.EVERYTHING),
            (chosen + PlacesToCallOut.EVERYTHING).normalized(PlacesToCallOut.EVERYTHING)
        )
        assertEquals(
            setOf(PlacesToCallOut.NOTHING),
            (chosen + PlacesToCallOut.NOTHING).normalized(PlacesToCallOut.NOTHING)
        )
        // ...and ticking a kind alongside one of them drops it instead
        assertEquals(
            setOf(PlacesToCallOut.BANKS),
            setOf(PlacesToCallOut.EVERYTHING, PlacesToCallOut.BANKS)
                .normalized(PlacesToCallOut.BANKS)
        )
        // Unticking the last kind is No Places rather than a setting that means nothing
        assertEquals(setOf(PlacesToCallOut.NOTHING), emptySet<PlacesToCallOut>().normalized())
    }

    private fun migrated(placesAndLandmarks: Boolean?, mobility: Boolean?): String {
        val preferences = VerbosityPreferences().apply {
            placesAndLandmarks?.let { putBoolean(PreferenceKeys.LEGACY_PLACES_AND_LANDMARKS, it) }
            mobility?.let { putBoolean(PreferenceKeys.STREETS_AND_JUNCTIONS, it) }
        }
        PlacesToCallOut.migrate(preferences)
        return preferences.getString(PreferenceKeys.PLACES_TO_CALL_OUT, "")
    }

    @Test
    fun theOldSwitchesCarryOverOnce() {
        assertEquals("Everything", migrated(null, null))
        assertEquals("Everything", migrated(true, false))
        assertEquals("Transit", migrated(false, true))
        assertEquals("Nothing", migrated(false, false))

        // ...and once only: a choice already made is left alone
        val preferences = VerbosityPreferences().apply {
            putString(PreferenceKeys.PLACES_TO_CALL_OUT, "Banks")
            putBoolean(PreferenceKeys.LEGACY_PLACES_AND_LANDMARKS, false)
        }
        PlacesToCallOut.migrate(preferences)
        assertEquals("Banks", preferences.getString(PreferenceKeys.PLACES_TO_CALL_OUT, ""))
    }

    private val userLocation = LngLatAlt(-4.2546, 55.8609)

    private fun walkingFix(timestamp: Long, beacon: LngLatAlt? = null) = UserGeometry(
        location = userLocation,
        phoneHeading = 0.0,
        speed = 1.0,
        timestampMilliseconds = timestamp,
        currentBeacon = beacon,
    )

    /** Two shops and a marker just north of [userLocation], all within trigger range. */
    private fun busyStreet(): GridState {
        val grid = GridState().apply { validateContext = false }
        val features = FeatureCollection()
        listOf("Greggs" to 5.0, "Boots" to 8.0).forEach { (shopName, distance) ->
            features.addFeature(poi(shopName, SuperCategoryId.PLACE).apply {
                geometry = Point(getDestinationCoordinate(userLocation, 0.0, distance))
            })
        }
        grid.featureTrees[TreeId.SELECTED_SUPER_CATEGORIES.id] = FeatureTree(features)
        return grid
    }

    private fun calloutText(callout: org.scottishtecharmy.soundscape.geoengine.filters.TrackedCallout?) =
        callout?.positionedStrings?.joinToString { it.text }

    @Test
    fun detailedCallsOutTheNextShopStraightAway() {
        val autoCallout = AutoCallout(null, null)
        val grid = busyStreet()
        assertEquals("Greggs", calloutText(autoCallout.updateLocation(walkingFix(0), grid, GridState())))
        assertEquals("Boots", calloutText(autoCallout.updateLocation(walkingFix(6_000), grid, GridState())))
    }

    @Test
    fun balancedLeavesAGapBetweenPoiCallouts() {
        val preferences = VerbosityPreferences().apply {
            putString(PreferenceKeys.CALLOUT_VERBOSITY, CalloutVerbosity.BALANCED.preferenceValue)
        }
        val autoCallout = AutoCallout(null, preferences)
        val grid = busyStreet()
        assertEquals("Greggs", calloutText(autoCallout.updateLocation(walkingFix(0), grid, GridState())))
        assertNull(autoCallout.updateLocation(walkingFix(6_000), grid, GridState()))
        assertEquals("Boots", calloutText(autoCallout.updateLocation(walkingFix(16_000), grid, GridState())))
    }

    @Test
    fun aMarkerIsNotHeldBackByTheGap() {
        val preferences = VerbosityPreferences().apply {
            putString(PreferenceKeys.CALLOUT_VERBOSITY, CalloutVerbosity.BALANCED.preferenceValue)
        }
        val autoCallout = AutoCallout(null, preferences)
        val grid = busyStreet()
        assertEquals("Greggs", calloutText(autoCallout.updateLocation(walkingFix(0), grid, GridState())))

        grid.featureTrees[TreeId.SELECTED_SUPER_CATEGORIES.id] = FeatureTree(
            FeatureCollection().apply {
                addFeature(poi("Bus to work", SuperCategoryId.MARKER).apply {
                    geometry = Point(getDestinationCoordinate(userLocation, 0.0, 10.0))
                })
            }
        )
        val marker = calloutText(autoCallout.updateLocation(walkingFix(6_000), grid, GridState()))
        assertTrue(marker?.endsWith("Bus to work") == true, "Expected the marker, got $marker")
    }

    @Test
    fun quietDoesNotCallOutShops() {
        val preferences = VerbosityPreferences().apply {
            putString(PreferenceKeys.CALLOUT_VERBOSITY, CalloutVerbosity.QUIET.preferenceValue)
        }
        assertNull(AutoCallout(null, preferences).updateLocation(walkingFix(0), busyStreet(), GridState()))
    }

    /** Silent took over from Allow Callouts: nothing automatic, but the beacon still reports. */
    @Test
    fun silentMakesNoAutomaticCalloutsButKeepsTheBeacon() {
        val preferences = VerbosityPreferences().apply {
            putString(PreferenceKeys.CALLOUT_VERBOSITY, CalloutVerbosity.SILENT.preferenceValue)
        }
        assertNull(
            AutoCallout(null, preferences).updateLocation(walkingFix(0), busyStreet(), GridState())
        )

        val beacon = getDestinationCoordinate(userLocation, 90.0, 300.0)
        val empty = GridState().apply { validateContext = false }
        val callout = AutoCallout(null, preferences)
            .updateLocation(walkingFix(0, beacon), empty, GridState())
        assertTrue(calloutText(callout)!!.startsWith("Distance to beacon"))
    }

    /** The headphone button: quieter each press, and round again rather than a dead end. */
    @Test
    fun cyclingStepsQuieterAndWrapsToTheMostDetailed() {
        val preferences = VerbosityPreferences()
        assertEquals(CalloutVerbosity.DETAILED, readCalloutVerbosity(preferences))
        assertEquals(CalloutVerbosity.BALANCED, cycleCalloutVerbosity(preferences))
        assertEquals(CalloutVerbosity.QUIET, cycleCalloutVerbosity(preferences))
        assertEquals(CalloutVerbosity.SILENT, cycleCalloutVerbosity(preferences))
        assertEquals(CalloutVerbosity.DETAILED, cycleCalloutVerbosity(preferences))
        // ...and it is saved, so Settings and the next callout agree with what was said
        assertEquals(CalloutVerbosity.DETAILED, readCalloutVerbosity(preferences))
    }

    /** Every level is reached from every starting point, whichever way it was chosen. */
    @Test
    fun cyclingFromAnyLevelVisitsThemAll() {
        for (start in CalloutVerbosity.entries) {
            val preferences = VerbosityPreferences().apply {
                putString(PreferenceKeys.CALLOUT_VERBOSITY, start.preferenceValue)
            }
            val visited = CalloutVerbosity.entries.map { cycleCalloutVerbosity(preferences) }
            assertEquals(CalloutVerbosity.entries.toSet(), visited.toSet(), "from $start")
            assertEquals(start, readCalloutVerbosity(preferences), "from $start")
        }
    }

    @Test
    fun allowCalloutsOffBecomesSilentOnce() {
        val preferences = VerbosityPreferences().apply {
            putBoolean(PreferenceKeys.LEGACY_ALLOW_CALLOUTS, false)
        }
        CalloutVerbosity.migrate(preferences)
        assertEquals("Silent", preferences.getString(PreferenceKeys.CALLOUT_VERBOSITY, ""))

        // Having carried over, a later choice isn't overwritten on the next start
        preferences.putString(PreferenceKeys.CALLOUT_VERBOSITY, "Balanced")
        CalloutVerbosity.migrate(preferences)
        assertEquals("Balanced", preferences.getString(PreferenceKeys.CALLOUT_VERBOSITY, ""))
    }

    @Test
    fun allowCalloutsOnLeavesTheAmountOfDetailAlone() {
        val preferences = VerbosityPreferences()
        CalloutVerbosity.migrate(preferences)
        assertEquals("", preferences.getString(PreferenceKeys.CALLOUT_VERBOSITY, ""))
    }

    @Test
    fun beaconDistanceFollowsItsSetting() {
        val beacon = getDestinationCoordinate(userLocation, 90.0, 300.0)
        val empty = GridState().apply { validateContext = false }

        val on = AutoCallout(null, null).updateLocation(walkingFix(0, beacon), empty, GridState())
        assertNotNull(on)
        assertTrue(calloutText(on)!!.startsWith("Distance to beacon"))

        val preferences = VerbosityPreferences().apply {
            putBoolean(PreferenceKeys.DISTANCE_TO_BEACON, false)
        }
        assertNull(
            AutoCallout(null, preferences).updateLocation(walkingFix(0, beacon), empty, GridState())
        )
    }
}
