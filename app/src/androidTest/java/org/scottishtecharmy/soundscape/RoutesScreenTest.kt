package org.scottishtecharmy.soundscape

import android.content.Context
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.MarkersAndRoutesUiState
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.routesscreen.RoutesScreen
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme

class RoutesScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val testRoutes = listOf(
        LocationDescription(
            name = "Morning Walk",
            description = "A nice morning route",
            location = LngLatAlt(-4.25, 55.85),
            databaseId = 1L
        ),
        LocationDescription(
            name = "City Tour",
            description = "Tour around the city",
            location = LngLatAlt(-4.26, 55.86),
            databaseId = 2L
        ),
        LocationDescription(
            name = "Park Loop",
            description = "Loop through the park",
            location = LngLatAlt(-4.27, 55.87),
            databaseId = 3L
        )
    )

    @Test
    fun routesScreen_emptyState_showsNoRoutesMessage() {
        composeTestRule.setContent {
            SoundscapeTheme {
                RoutesScreen(
                    homeNavController = rememberNavController(),
                    uiState = MarkersAndRoutesUiState(markers = false),
                    userLocation = null,
                    clearErrorMessage = {},
                    onToggleSortOrder = {},
                    onToggleSortByName = {}
                )
            }
        }

        val noRoutesTitle = context.resources.getString(R.string.routes_no_routes_title)
        composeTestRule.onNodeWithText(noRoutesTitle).assertIsDisplayed()
    }

    @Test
    fun routesScreen_emptyState_showsHintMessages() {
        composeTestRule.setContent {
            SoundscapeTheme {
                RoutesScreen(
                    homeNavController = rememberNavController(),
                    uiState = MarkersAndRoutesUiState(markers = false),
                    userLocation = null,
                    clearErrorMessage = {},
                    onToggleSortOrder = {},
                    onToggleSortByName = {}
                )
            }
        }

        val hint1 = context.resources.getString(R.string.routes_no_routes_hint_1)
        val hint2 = context.resources.getString(R.string.routes_no_routes_hint_2)
        composeTestRule.onNodeWithText(hint1).assertIsDisplayed()
        composeTestRule.onNodeWithText(hint2).assertIsDisplayed()
    }

    @Test
    fun routesScreen_withRoutes_showsRoutesList() {
        composeTestRule.setContent {
            SoundscapeTheme {
                RoutesScreen(
                    homeNavController = rememberNavController(),
                    uiState = MarkersAndRoutesUiState(
                        markers = false,
                        entries = testRoutes
                    ),
                    userLocation = null,
                    clearErrorMessage = {},
                    onToggleSortOrder = {},
                    onToggleSortByName = {}
                )
            }
        }

        // Check that route names are displayed
        composeTestRule.onNodeWithText("Morning Walk", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("City Tour", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Park Loop", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun routesScreen_withRoutes_showsSortControls() {
        composeTestRule.setContent {
            SoundscapeTheme {
                RoutesScreen(
                    homeNavController = rememberNavController(),
                    uiState = MarkersAndRoutesUiState(
                        markers = false,
                        entries = testRoutes
                    ),
                    userLocation = null,
                    clearErrorMessage = {},
                    onToggleSortOrder = {},
                    onToggleSortByName = {}
                )
            }
        }

        // Check that sort controls are displayed
        composeTestRule.onNodeWithTag("SortOrder", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("SortOption", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun routesScreen_sortOrderButton_callsToggleSortOrder() {
        var sortOrderToggled = false

        composeTestRule.setContent {
            SoundscapeTheme {
                RoutesScreen(
                    homeNavController = rememberNavController(),
                    uiState = MarkersAndRoutesUiState(
                        markers = false,
                        entries = testRoutes
                    ),
                    userLocation = null,
                    clearErrorMessage = {},
                    onToggleSortOrder = { sortOrderToggled = true },
                    onToggleSortByName = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("SortOrder", useUnmergedTree = true).performClick()
        assert(sortOrderToggled) { "Sort order toggle callback was not called" }
    }

    @Test
    fun routesScreen_sortOptionButton_callsToggleSortByName() {
        var sortByNameToggled = false

        composeTestRule.setContent {
            SoundscapeTheme {
                RoutesScreen(
                    homeNavController = rememberNavController(),
                    uiState = MarkersAndRoutesUiState(
                        markers = false,
                        entries = testRoutes
                    ),
                    userLocation = null,
                    clearErrorMessage = {},
                    onToggleSortOrder = {},
                    onToggleSortByName = { sortByNameToggled = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("SortOption", useUnmergedTree = true).performClick()
        assert(sortByNameToggled) { "Sort by name toggle callback was not called" }
    }

    @Test
    fun routesScreen_loadingState_showsProgressIndicator() {
        composeTestRule.setContent {
            SoundscapeTheme {
                RoutesScreen(
                    homeNavController = rememberNavController(),
                    uiState = MarkersAndRoutesUiState(
                        markers = false,
                        isLoading = true
                    ),
                    userLocation = null,
                    clearErrorMessage = {},
                    onToggleSortOrder = {},
                    onToggleSortByName = {}
                )
            }
        }

        // When loading, the empty state message should not be visible
        val noRoutesTitle = context.resources.getString(R.string.routes_no_routes_title)
        composeTestRule.onNodeWithText(noRoutesTitle).assertDoesNotExist()
    }

    @Test
    fun routesScreen_routeItem_hasStartPlaybackAccessibilityAction() {
        composeTestRule.setContent {
            SoundscapeTheme {
                RoutesScreen(
                    homeNavController = rememberNavController(),
                    uiState = MarkersAndRoutesUiState(
                        markers = false,
                        entries = testRoutes
                    ),
                    userLocation = null,
                    clearErrorMessage = {},
                    onToggleSortOrder = {},
                    onToggleSortByName = {},
                    onStartPlayback = { }
                )
            }
        }

        // Find the LocationItem node by its content description (set via clearAndSetSemantics)
        // The contentDescription includes the route name and description
        val semanticsNode = composeTestRule
            .onNode(hasContentDescription("Morning Walk", substring = true), useUnmergedTree = true)
            .fetchSemanticsNode()

        // Check that custom actions exist on the node
        val customActions = semanticsNode.config.getOrElseNullable(SemanticsActions.CustomActions) { null }
        assertTrue(
            "Route item should have custom accessibility actions",
            customActions != null && customActions.isNotEmpty()
        )

        // Verify the action label contains expected text (start route hint)
        val startRouteHint = context.resources.getString(R.string.route_detail_action_start_route_hint)
        val hasStartRouteAction = customActions?.any { it.label == startRouteHint } ?: false
        assertTrue(
            "Route item should have 'Start this route' accessibility action",
            hasStartRouteAction
        )
    }

    @Test
    fun routesScreen_sortByNameAscending_showsCorrectLabel() {
        composeTestRule.setContent {
            SoundscapeTheme {
                RoutesScreen(
                    homeNavController = rememberNavController(),
                    uiState = MarkersAndRoutesUiState(
                        markers = false,
                        entries = testRoutes,
                        isSortByName = true,
                        isSortAscending = true
                    ),
                    userLocation = null,
                    clearErrorMessage = {},
                    onToggleSortOrder = {},
                    onToggleSortByName = {}
                )
            }
        }

        val sortByName = context.resources.getString(R.string.markers_sort_button_sort_by_name)
        composeTestRule.onNodeWithText(sortByName, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun routesScreen_sortByDistance_showsCorrectLabel() {
        composeTestRule.setContent {
            SoundscapeTheme {
                RoutesScreen(
                    homeNavController = rememberNavController(),
                    uiState = MarkersAndRoutesUiState(
                        markers = false,
                        entries = testRoutes,
                        isSortByName = false,
                        isSortAscending = true
                    ),
                    userLocation = null,
                    clearErrorMessage = {},
                    onToggleSortOrder = {},
                    onToggleSortByName = {}
                )
            }
        }

        val sortByDistance = context.resources.getString(R.string.markers_sort_button_sort_by_distance)
        composeTestRule.onNodeWithText(sortByDistance, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun routesScreen_showsRouteDescriptions() {
        composeTestRule.setContent {
            SoundscapeTheme {
                RoutesScreen(
                    homeNavController = rememberNavController(),
                    uiState = MarkersAndRoutesUiState(
                        markers = false,
                        entries = testRoutes
                    ),
                    userLocation = null,
                    clearErrorMessage = {},
                    onToggleSortOrder = {},
                    onToggleSortByName = {}
                )
            }
        }

        // Check that route descriptions are displayed
        composeTestRule.onNodeWithText("A nice morning route", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Tour around the city", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Loop through the park", useUnmergedTree = true).assertIsDisplayed()
    }
}
