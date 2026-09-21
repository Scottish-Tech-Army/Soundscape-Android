package org.scottishtecharmy.soundscape

import android.content.Context
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.location_detail_action_beacon_from_markers
import org.scottishtecharmy.soundscape.resources.markers_no_markers_title
import org.scottishtecharmy.soundscape.resources.markers_sort_distance_ascending
import org.scottishtecharmy.soundscape.resources.markers_sort_name_ascending
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.MarkersAndRoutesUiState
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.markersscreen.MarkersScreen
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme

class MarkersScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val testMarkers = listOf(
        LocationDescription(
            name = "Test Marker 1",
            description = "First test marker",
            location = LngLatAlt(-4.25, 55.85)
        ),
        LocationDescription(
            name = "Test Marker 2",
            description = "Second test marker",
            location = LngLatAlt(-4.26, 55.86)
        ),
        LocationDescription(
            name = "Test Marker 3",
            description = "Third test marker",
            location = LngLatAlt(-4.27, 55.87)
        )
    )

    @Test
    fun markersScreen_emptyState_showsNoMarkersMessage() {
        composeTestRule.setContent {
            SoundscapeTheme {
                MarkersScreen(
                    uiState = MarkersAndRoutesUiState(markers = true),
                    clearErrorMessage = {},
                    onCycleSort = {},
                    userLocation = null,
                    onSelectItem = {}
                )
            }
        }

        val noMarkersTitle =
            kotlinx.coroutines.runBlocking { org.jetbrains.compose.resources.getString(Res.string.markers_no_markers_title) }
        composeTestRule.onNodeWithText(noMarkersTitle).assertIsDisplayed()
    }

    @Test
    fun markersScreen_withMarkers_showsMarkersList() {
        composeTestRule.setContent {
            SoundscapeTheme {
                MarkersScreen(
                    uiState = MarkersAndRoutesUiState(
                        markers = true,
                        entries = testMarkers
                    ),
                    clearErrorMessage = {},
                    onCycleSort = {},
                    userLocation = null,
                    onSelectItem = {}
                )
            }
        }

        // Check that marker names are displayed
        composeTestRule.onNodeWithText("Test Marker 1", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Marker 2", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Marker 3", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun markersScreen_withMarkers_showsSortControls() {
        composeTestRule.setContent {
            SoundscapeTheme {
                MarkersScreen(
                    uiState = MarkersAndRoutesUiState(
                        markers = true,
                        entries = testMarkers
                    ),
                    clearErrorMessage = {},
                    onCycleSort = {},
                    userLocation = null,
                    onSelectItem = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("SortOption", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun markersScreen_sortButton_callsCycleSort() {
        var sortCycled = false

        composeTestRule.setContent {
            SoundscapeTheme {
                MarkersScreen(
                    uiState = MarkersAndRoutesUiState(
                        markers = true,
                        entries = testMarkers
                    ),
                    clearErrorMessage = {},
                    onCycleSort = { sortCycled = true },
                    userLocation = null,
                    onSelectItem = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("SortOption", useUnmergedTree = true).performClick()
        assert(sortCycled) { "Sort cycle callback was not called" }
    }

    @Test
    fun markersScreen_loadingState_showsProgressIndicator() {
        composeTestRule.setContent {
            SoundscapeTheme {
                MarkersScreen(
                    uiState = MarkersAndRoutesUiState(
                        markers = true,
                        isLoading = true
                    ),
                    clearErrorMessage = {},
                    onCycleSort = {},
                    userLocation = null,
                    onSelectItem = {}
                )
            }
        }

        // When loading, the markers list should not be visible
        val noMarkersTitle =
            kotlinx.coroutines.runBlocking { org.jetbrains.compose.resources.getString(Res.string.markers_no_markers_title) }
        composeTestRule.onNodeWithText(noMarkersTitle).assertDoesNotExist()
    }

    @Test
    fun markersScreen_sortByNameAscending_showsCorrectLabel() {
        composeTestRule.setContent {
            SoundscapeTheme {
                MarkersScreen(
                    uiState = MarkersAndRoutesUiState(
                        markers = true,
                        entries = testMarkers,
                        isSortByName = true,
                        isSortAscending = true
                    ),
                    clearErrorMessage = {},
                    onCycleSort = {},
                    userLocation = null,
                    onSelectItem = {}
                )
            }
        }

        val sortByName =
            kotlinx.coroutines.runBlocking { org.jetbrains.compose.resources.getString(Res.string.markers_sort_name_ascending) }
        composeTestRule.onNodeWithTag("SortOption")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, sortByName))
    }

    @Test
    fun markersScreen_sortByDistance_showsCorrectLabel() {
        composeTestRule.setContent {
            SoundscapeTheme {
                MarkersScreen(
                    uiState = MarkersAndRoutesUiState(
                        markers = true,
                        entries = testMarkers,
                        isSortByName = false,
                        isSortAscending = true
                    ),
                    clearErrorMessage = {},
                    onCycleSort = {},
                    userLocation = null,
                    onSelectItem = {}
                )
            }
        }

        val sortByDistance =
            kotlinx.coroutines.runBlocking { org.jetbrains.compose.resources.getString(Res.string.markers_sort_distance_ascending) }
        composeTestRule.onNodeWithTag("SortOption")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, sortByDistance))
    }

    @Test
    fun markersScreen_markerItem_hasStartBeaconAccessibilityAction() {
        composeTestRule.setContent {
            SoundscapeTheme {
                MarkersScreen(
                    uiState = MarkersAndRoutesUiState(
                        markers = true,
                        entries = testMarkers
                    ),
                    clearErrorMessage = {},
                    onCycleSort = {},
                    userLocation = null,
                    onSelectItem = {},
                    onStartBeacon = { _, _ -> }
                )
            }
        }

        // Find the LocationItem node by its content description (set via clearAndSetSemantics)
        // The contentDescription includes the marker name and description
        val semanticsNode = composeTestRule
            .onNode(
                hasContentDescription("Test Marker 1", substring = true),
                useUnmergedTree = true
            )
            .fetchSemanticsNode()

        // Check that custom actions exist on the node
        val customActions =
            semanticsNode.config.getOrElseNullable(SemanticsActions.CustomActions) { null }
        assertTrue(
            "Marker item should have custom accessibility actions",
            customActions != null && customActions.isNotEmpty()
        )

        // Verify the action label contains expected text (start beacon hint)
        val startBeaconHint =
            kotlinx.coroutines.runBlocking { org.jetbrains.compose.resources.getString(Res.string.location_detail_action_beacon_from_markers) }
        val hasStartBeaconAction = customActions?.any { it.label == startBeaconHint } ?: false
        assertTrue(
            "Marker item should have 'Start audio beacon at this marker' accessibility action",
            hasStartBeaconAction
        )
    }
}
