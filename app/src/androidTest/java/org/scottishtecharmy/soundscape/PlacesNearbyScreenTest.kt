package org.scottishtecharmy.soundscape

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.filter_all
import org.scottishtecharmy.soundscape.resources.filter_banks
import org.scottishtecharmy.soundscape.resources.filter_food_drink
import org.scottishtecharmy.soundscape.resources.filter_groceries
import org.scottishtecharmy.soundscape.resources.filter_transit
import org.scottishtecharmy.soundscape.resources.osm_intersection
import org.scottishtecharmy.soundscape.resources.search_nearby_screen_title
import org.scottishtecharmy.soundscape.screens.home.placesnearby.PlacesNearbyScreen
import org.scottishtecharmy.soundscape.screens.home.placesnearby.PlacesNearbyUiState
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme

class PlacesNearbyScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun placesNearbyScreen_level0_showsFolderCategories() {
        composeTestRule.setContent {
            SoundscapeTheme {
                PlacesNearbyScreen(
                    onSelectItem = {},
                    uiState = PlacesNearbyUiState(
                        level = 0,
                        nearbyPlaces = FeatureCollection()
                    )
                )
            }
        }

        // Check that category folders are displayed
        val allPlaces =
            kotlinx.coroutines.runBlocking { org.jetbrains.compose.resources.getString(Res.string.filter_all) }
        val transit =
            kotlinx.coroutines.runBlocking { org.jetbrains.compose.resources.getString(Res.string.filter_transit) }
        val foodDrink =
            kotlinx.coroutines.runBlocking { org.jetbrains.compose.resources.getString(Res.string.filter_food_drink) }
        val groceries =
            kotlinx.coroutines.runBlocking { org.jetbrains.compose.resources.getString(Res.string.filter_groceries) }
        val banks =
            kotlinx.coroutines.runBlocking { org.jetbrains.compose.resources.getString(Res.string.filter_banks) }
        val intersections =
            kotlinx.coroutines.runBlocking { org.jetbrains.compose.resources.getString(Res.string.osm_intersection) }

        composeTestRule.onNodeWithText(allPlaces).assertIsDisplayed()
        composeTestRule.onNodeWithText(transit).assertIsDisplayed()
        composeTestRule.onNodeWithText(foodDrink).assertIsDisplayed()
        composeTestRule.onNodeWithText(groceries).assertIsDisplayed()
        composeTestRule.onNodeWithText(banks).assertIsDisplayed()
        composeTestRule.onNodeWithText(intersections).assertIsDisplayed()
    }

    @Test
    fun placesNearbyScreen_level0_showsCorrectTitle() {
        composeTestRule.setContent {
            SoundscapeTheme {
                PlacesNearbyScreen(
                    onSelectItem = {},
                    uiState = PlacesNearbyUiState(
                        level = 0,
                        nearbyPlaces = FeatureCollection()
                    )
                )
            }
        }

        val title =
            kotlinx.coroutines.runBlocking { org.jetbrains.compose.resources.getString(Res.string.search_nearby_screen_title) }
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
    }

    @Test
    fun placesNearbyScreen_level1_showsCustomTitle() {
        val customTitle = "Transit Stops"
        composeTestRule.setContent {
            SoundscapeTheme {
                PlacesNearbyScreen(
                    onSelectItem = {},
                    uiState = PlacesNearbyUiState(
                        level = 1,
                        title = customTitle,
                        filter = "transit",
                        nearbyPlaces = FeatureCollection()
                    )
                )
            }
        }

        composeTestRule.onNodeWithText(customTitle).assertIsDisplayed()
    }

    @Test
    fun placesNearbyScreen_folderClick_callsOnClickFolder() {
        var clickedFolder = ""
        var clickedTitle = ""

        composeTestRule.setContent {
            SoundscapeTheme {
                PlacesNearbyScreen(
                    onSelectItem = {},
                    uiState = PlacesNearbyUiState(
                        level = 0,
                        nearbyPlaces = FeatureCollection()
                    ),
                    onClickFolder = { folder, title ->
                        clickedFolder = folder
                        clickedTitle = title
                    }
                )
            }
        }

        val transit =
            kotlinx.coroutines.runBlocking { org.jetbrains.compose.resources.getString(Res.string.filter_transit) }
        composeTestRule.onNodeWithText(transit).performClick()

        assert(clickedFolder == "transit") { "Expected folder 'transit' but got '$clickedFolder'" }
        assert(clickedTitle == transit) { "Expected title '$transit' but got '$clickedTitle'" }
    }

    @Test
    fun placesNearbyScreen_backButton_callsOnClickBack() {
        var backClicked = false

        composeTestRule.setContent {
            SoundscapeTheme {
                PlacesNearbyScreen(
                    onSelectItem = {},
                    uiState = PlacesNearbyUiState(
                        level = 0,
                        nearbyPlaces = FeatureCollection()
                    ),
                    onClickBack = { backClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("appBarLeft").performClick()
        assert(backClicked) { "Back button callback was not called" }
    }

    @Test
    fun placesNearbyScreen_allFoldersHaveTestTags() {
        composeTestRule.setContent {
            SoundscapeTheme {
                PlacesNearbyScreen(
                    onSelectItem = {},
                    uiState = PlacesNearbyUiState(
                        level = 0,
                        nearbyPlaces = FeatureCollection()
                    )
                )
            }
        }

        // Verify test tags exist for each folder
        composeTestRule.onNodeWithTag("placesNearby-0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("placesNearby-1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("placesNearby-2").assertIsDisplayed()
        composeTestRule.onNodeWithTag("placesNearby-3").assertIsDisplayed()
        composeTestRule.onNodeWithTag("placesNearby-4").assertIsDisplayed()
        composeTestRule.onNodeWithTag("placesNearby-5").assertIsDisplayed()
    }

    @Test
    fun placesNearbyScreen_clickAllPlaces_callsCorrectFilter() {
        var clickedFolder = ""

        composeTestRule.setContent {
            SoundscapeTheme {
                PlacesNearbyScreen(
                    onSelectItem = {},
                    uiState = PlacesNearbyUiState(
                        level = 0,
                        nearbyPlaces = FeatureCollection()
                    ),
                    onClickFolder = { folder, _ ->
                        clickedFolder = folder
                    }
                )
            }
        }

        val allPlaces =
            kotlinx.coroutines.runBlocking { org.jetbrains.compose.resources.getString(Res.string.filter_all) }
        composeTestRule.onNodeWithText(allPlaces).performClick()

        // "All" filter uses empty string
        assert(clickedFolder == "") { "Expected empty filter for 'All' but got '$clickedFolder'" }
    }

    @Test
    fun placesNearbyScreen_clickIntersections_callsCorrectFilter() {
        var clickedFolder = ""

        composeTestRule.setContent {
            SoundscapeTheme {
                PlacesNearbyScreen(
                    onSelectItem = {},
                    uiState = PlacesNearbyUiState(
                        level = 0,
                        nearbyPlaces = FeatureCollection()
                    ),
                    onClickFolder = { folder, _ ->
                        clickedFolder = folder
                    }
                )
            }
        }

        val intersections =
            kotlinx.coroutines.runBlocking { org.jetbrains.compose.resources.getString(Res.string.osm_intersection) }
        composeTestRule.onNodeWithText(intersections).performClick()

        assert(clickedFolder == "intersections") { "Expected filter 'intersections' but got '$clickedFolder'" }
    }

    @Test
    fun placesNearbyScreen_onStartBeaconCallback_isWiredUp() {
        // This test verifies that the onStartBeacon callback parameter is accepted
        // by the screen. The actual accessibility action on location items is tested
        // through MarkersScreenTest since both screens use the same LocationItem
        // component with the same startPlayback/startBeacon pattern.
        composeTestRule.setContent {
            SoundscapeTheme {
                PlacesNearbyScreen(
                    onSelectItem = {},
                    uiState = PlacesNearbyUiState(
                        level = 0,
                        nearbyPlaces = FeatureCollection()
                    ),
                    onStartBeacon = { }
                )
            }
        }

        // Verify the screen renders without error when onStartBeacon is provided
        val title =
            kotlinx.coroutines.runBlocking { org.jetbrains.compose.resources.getString(Res.string.search_nearby_screen_title) }
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
    }

    /**
     * Un-named POIs all render with the same generic type label ("Post Box"), so the street each
     * one sits on - associated at tile load time by GridState.attachNearestWays() - is shown
     * underneath to tell them apart. Both halves have to be present for the line to appear at all
     * (see LocationDescriptionStreetTest.bothHalvesAreNeeded), so the fixture sets both.
     *
     * The row clears and replaces its descendants' semantics with a single combined
     * contentDescription, so that TalkBack reads it as one utterance rather than four. That means
     * the street has to be asserted twice over: once against the unmerged tree, for what a sighted
     * user sees, and once inside the row's description, for what a screen reader actually says -
     * the merged tree has no Text node to match, which is what made an earlier version of this
     * test fail.
     */
    @Test
    fun placesNearbyScreen_unnamedPoi_showsTheStreetItSitsOn() {
        val postBox = MvtFeature().apply {
            geometry = Point(LngLatAlt(-4.2540, 55.8701))
            featureClass = "post"
            featureSubClass = "post_box"
            nearestWay = Way().apply { name = "London Road" }
            nearestSettlement = "Bridgeton"
        }

        composeTestRule.setContent {
            SoundscapeTheme {
                PlacesNearbyScreen(
                    onSelectItem = {},
                    uiState = PlacesNearbyUiState(
                        level = 1,
                        filter = "",
                        userLocation = LngLatAlt(-4.2541, 55.8702),
                        nearbyPlaces = FeatureCollection().apply { addFeature(postBox) },
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("London Road, Bridgeton", useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("London Road, Bridgeton", substring = true)
            .assertIsDisplayed()
    }
}
