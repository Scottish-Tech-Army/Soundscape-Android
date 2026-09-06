@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.scottishtecharmy.soundscape.geoengine.callouts

import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.MOBILITY_KEY
import org.scottishtecharmy.soundscape.geoengine.PLACES_AND_LANDMARKS_KEY
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geoengine.utils.PointAndDistanceAndHeading
import org.scottishtecharmy.soundscape.geoengine.utils.getDestinationCoordinate
import org.scottishtecharmy.soundscape.geoengine.utils.SuperCategoryId
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.preferences.PreferencesListener
import org.scottishtecharmy.soundscape.preferences.PreferencesProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** In-memory [PreferencesProvider] holding only what a test sets on it. */
private class FakePreferences : PreferencesProvider {
    private val booleans = mutableMapOf<String, Boolean>()

    override fun getBoolean(key: String, default: Boolean): Boolean = booleans[key] ?: default
    override fun getString(key: String, default: String): String = default
    override fun getFloat(key: String, default: Float): Float = default

    override fun putBoolean(key: String, value: Boolean) { booleans[key] = value }
    override fun putString(key: String, value: String) {}

    override fun clearAll() { booleans.clear() }

    override fun addListener(listener: PreferencesListener) {}
    override fun removeListener(listener: PreferencesListener) {}
}

/**
 * The "Mobility" and "Places and Landmarks" callout settings each cover more than one thing, as
 * they do on iOS - see CalloutSettingsCellView.swift, where one switch sets three senses. These
 * cover the parts of that which aren't simply "a super-category goes in the tree".
 */
class CalloutCategorySettingsTest {

    /**
     * A user standing on a named road, travelling along it - the minimum needed for
     * buildCalloutForIntersections to produce its "Ahead <road>" callout. No intersection is
     * required: the road on its own is enough to tell a callout from no callout, which is all
     * these two tests are separating.
     */
    private fun aheadOnARoad(): Pair<UserGeometry, GridState> {
        val userGeometry = UserGeometry(
            location = LngLatAlt(-2.657, 51.430),
            phoneHeading = 0.0,
            mapMatchedWay = Way().apply { name = "Some Road" },
            mapMatchedLocation = PointAndDistanceAndHeading(heading = 0.0),
        )
        return userGeometry to GridState().apply { validateContext = false }
    }

    @Test
    fun intersectionCalloutIsMadeWhenMobilityIsOn() {
        val (userGeometry, gridState) = aheadOnARoad()
        val preferences = FakePreferences().apply { putBoolean(MOBILITY_KEY, true) }

        val callout = AutoCallout(null, preferences)
            .buildCalloutForIntersections(userGeometry, gridState)

        assertNotNull(callout)
        assertEquals("Ahead Some Road", callout.positionedStrings[0].text)
    }

    /**
     * Turning Mobility off silences the intersection callouts, which is what makes that setting's
     * description ("Intersection and transportation information") true. It was inherited from iOS
     * along with the wording, where the same switch sets intersectionSenseEnabled.
     */
    @Test
    fun intersectionCalloutIsSilencedWhenMobilityIsOff() {
        val (userGeometry, gridState) = aheadOnARoad()
        val preferences = FakePreferences().apply { putBoolean(MOBILITY_KEY, false) }

        assertNull(
            AutoCallout(null, preferences)
                .buildCalloutForIntersections(userGeometry, gridState)
        )
    }

    /** No preferences at all - every test that builds an AutoCallout directly - keeps them on. */
    @Test
    fun intersectionCalloutIsMadeWithNoPreferencesAtAll() {
        val (userGeometry, gridState) = aheadOnARoad()

        assertNotNull(
            AutoCallout(null, null).buildCalloutForIntersections(userGeometry, gridState)
        )
    }

    /**
     * A guidepost or a notice board is an information POI, and on iOS the Places and Landmarks
     * switch turns those on along with places and landmarks (placeSense, landmarkSense *and*
     * informationSense - see CalloutSettingsCellView.swift). Without this they were classified at
     * tile load time and then never reached a callout: TreeId.INFORMATION_POIS had no reader.
     */
    @Test
    fun informationPoisAreSelectedWithPlacesAndLandmarks() {
        val collections = classify(setOf(PLACES_AND_LANDMARKS_KEY))
        val selected = collections[TreeId.SELECTED_SUPER_CATEGORIES.id].features

        assertTrue(
            selected.any { (it as MvtFeature).name == "Guidepost" },
            "Expected the information POI to be selected, got ${selected.map { (it as MvtFeature).name }}"
        )
        assertTrue(selected.any { (it as MvtFeature).name == "Post Office" })
    }

    @Test
    fun informationPoisAreNotSelectedWithPlacesAndLandmarksOff() {
        val collections = classify(setOf(MOBILITY_KEY))
        val selected = collections[TreeId.SELECTED_SUPER_CATEGORIES.id].features

        assertTrue(selected.none { (it as MvtFeature).name == "Guidepost" })
        // ...and the mobility POI it was sorted alongside is still there, so this is the setting
        // choosing rather than the classification dropping everything.
        assertTrue(selected.any { (it as MvtFeature).name == "Lift" })
    }

    /**
     * The Mobility guard in buildCalloutForRoadSense sits *below* that function's vehicle
     * bookkeeping, not at the top of it: lastVehicleTimestampMs is recorded there on every update
     * and read by callouts this setting has nothing to do with, so it has to keep being updated
     * whether or not road sense itself is allowed to speak.
     *
     * What that buys is the sticky window - a vehicle stopping at a red light shouldn't
     * immediately expose pedestrian-style POI callouts. Here the vehicle fix is taken with
     * Mobility off, so if the guard had been placed above the bookkeeping the window would never
     * arm and the walking fix a second later would call the shop out.
     */
    @Test
    fun theVehicleStickyWindowStillArmsWhenMobilityIsOff() {
        val preferences = FakePreferences().apply { putBoolean(MOBILITY_KEY, false) }

        // A walking fix on its own names the shop - so the fixture can produce a callout, and the
        // silence below is the sticky window rather than an empty grid.
        val alone = AutoCallout(null, preferences).updateLocation(
            walkingFix(2000L), poiGrid(), GridState()
        )
        assertNotNull(alone)
        assertTrue(
            alone.positionedStrings.any { it.text.contains("Corner Shop") },
            "Expected the shop named, got ${alone.positionedStrings.map { it.text }}"
        )

        // The same walking fix a second after a vehicle fix is silent.
        val autoCallout = AutoCallout(null, preferences)
        val grid = poiGrid()
        autoCallout.updateLocation(vehicleFix(1000L), grid, GridState())
        assertNull(autoCallout.updateLocation(walkingFix(2000L), grid, GridState()))
    }

    private val userLocation = LngLatAlt(-2.657, 51.430)

    private fun walkingFix(timestamp: Long) = UserGeometry(
        location = userLocation,
        phoneHeading = 0.0,
        speed = 1.0,
        timestampMilliseconds = timestamp,
    )

    private fun vehicleFix(timestamp: Long) = UserGeometry(
        location = userLocation,
        phoneHeading = 0.0,
        speed = 15.0,
        timestampMilliseconds = timestamp,
    )

    /** A grid holding one named place POI, 5m north of [userLocation] and well within range. */
    private fun poiGrid(): GridState {
        val grid = GridState().apply { validateContext = false }
        val shop = poi("Corner Shop", SuperCategoryId.PLACE).apply {
            geometry = Point(getDestinationCoordinate(userLocation, 0.0, 5.0))
        }
        grid.featureTrees[TreeId.SELECTED_SUPER_CATEGORIES.id] =
            FeatureTree(FeatureCollection().apply { addFeature(shop) })
        return grid
    }

    /** Runs [GridState.classifyPois] over one POI of each category under test. */
    private fun classify(enabledCategories: Set<String>): Array<FeatureCollection> {
        val collections = Array(TreeId.MAX_COLLECTION_ID.id) { FeatureCollection() }
        collections[TreeId.POIS.id] = FeatureCollection().apply {
            addFeature(poi("Post Office", SuperCategoryId.PLACE))
            addFeature(poi("Guidepost", SuperCategoryId.INFORMATION))
            addFeature(poi("Lift", SuperCategoryId.MOBILITY))
        }
        GridState().classifyPois(collections, enabledCategories)
        return collections
    }

    private fun poi(poiName: String, category: SuperCategoryId) = MvtFeature().apply {
        name = poiName
        superCategory = category
        geometry = Point(LngLatAlt(-2.657, 51.430))
    }
}
