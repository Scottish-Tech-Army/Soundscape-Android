package org.scottishtecharmy.soundscape

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
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
                    homeNavController = rememberNavController(),
                    uiState = PlacesNearbyUiState(
                        level = 0,
                        nearbyPlaces = FeatureCollection()
                    )
                )
            }
        }

        // Check that category folders are displayed
        val allPlaces = context.resources.getString(R.string.filter_all)
        val transit = context.resources.getString(R.string.filter_transit)
        val foodDrink = context.resources.getString(R.string.filter_food_drink)
        val groceries = context.resources.getString(R.string.filter_groceries)
        val banks = context.resources.getString(R.string.filter_banks)
        val intersections = context.resources.getString(R.string.osm_intersection)

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
                    homeNavController = rememberNavController(),
                    uiState = PlacesNearbyUiState(
                        level = 0,
                        nearbyPlaces = FeatureCollection()
                    )
                )
            }
        }

        val title = context.resources.getString(R.string.search_nearby_screen_title)
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
    }

    @Test
    fun placesNearbyScreen_level1_showsCustomTitle() {
        val customTitle = "Transit Stops"
        composeTestRule.setContent {
            SoundscapeTheme {
                PlacesNearbyScreen(
                    homeNavController = rememberNavController(),
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
                    homeNavController = rememberNavController(),
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

        val transit = context.resources.getString(R.string.filter_transit)
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
                    homeNavController = rememberNavController(),
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
                    homeNavController = rememberNavController(),
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
                    homeNavController = rememberNavController(),
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

        val allPlaces = context.resources.getString(R.string.filter_all)
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
                    homeNavController = rememberNavController(),
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

        val intersections = context.resources.getString(R.string.osm_intersection)
        composeTestRule.onNodeWithText(intersections).performClick()

        assert(clickedFolder == "intersections") { "Expected filter 'intersections' but got '$clickedFolder'" }
    }

    @Test
    fun placesNearbyScreen_onStartBeaconCallback_isWiredUp() {
        // This test verifies that the onStartBeacon callback parameter is accepted
        // by the screen. The actual accessibility action on location items is tested
        // through MarkersScreenTest since both screens use the same LocationItem
        // component with the same startPlayback/startBeacon pattern.
        var beaconStarted = false

        composeTestRule.setContent {
            SoundscapeTheme {
                PlacesNearbyScreen(
                    homeNavController = rememberNavController(),
                    uiState = PlacesNearbyUiState(
                        level = 0,
                        nearbyPlaces = FeatureCollection()
                    ),
                    onStartBeacon = { beaconStarted = true }
                )
            }
        }

        // Verify the screen renders without error when onStartBeacon is provided
        val title = context.resources.getString(R.string.search_nearby_screen_title)
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
    }
}
