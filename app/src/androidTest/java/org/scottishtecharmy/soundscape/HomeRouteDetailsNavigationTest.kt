package org.scottishtecharmy.soundscape

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.scottishtecharmy.soundscape.database.local.model.MarkerEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.home.RouteFunctions
import org.scottishtecharmy.soundscape.services.RoutePlayerState
import org.scottishtecharmy.soundscape.screens.home.home.SharedHomeContent
import org.scottishtecharmy.soundscape.screens.home.home.StreetPreviewFunctions
import kotlinx.coroutines.flow.MutableStateFlow
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme
import org.scottishtecharmy.soundscape.ThemeState

/**
 * The "route details" button on the home screen's route player used to navigate to the string
 * "route_details_screen/<routeId>", but the shared graph has no such destination - it registers a
 * bare "route_details_screen" and passes the route through the NavigationStateHolder, keyed by back
 * stack entry, like every other per-entry payload. Tapping it therefore threw
 * IllegalArgumentException ("Navigation destination that matches route route_details_screen/0
 * cannot be found in the navigation graph") and killed the app.
 *
 * This pins the contract that broke: the button hands over a LocationDescription identifying the
 * route, and does not try to navigate to a route string of its own.
 */
class HomeRouteDetailsNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val route = RouteWithMarkers(
        RouteEntity(name = "Route to shops", description = "Milngavie", routeId = 7L),
        listOf(
            MarkerEntity(
                name = "Craigton Road",
                longitude = -4.3239319,
                latitude = 55.9446396,
                markerId = 1L,
            ),
            MarkerEntity(
                name = "Clober Road",
                longitude = -4.3210534,
                latitude = 55.9417227,
                markerId = 2L,
            ),
        ),
    )

    @Test
    fun routeDetailsButton_handsOverTheRoute_ratherThanNavigatingToAnUnknownRoute() {
        var routeDetailsFor: LocationDescription? = null
        val navigatedTo = mutableListOf<String>()
        // Hoisted out of the composition - creating it inside would be recreated on every
        // recomposition (and lint rejects it).
        val fullscreenMap = mutableStateOf(false)

        composeTestRule.setContent {
            SoundscapeTheme(MutableStateFlow(ThemeState())) {
                SharedHomeContent(
                    location = LngLatAlt(-4.3239319, 55.9446396),
                    beaconState = null,
                    routePlayerState = RoutePlayerState(routeData = route, currentWaypoint = 0),
                    heading = 0.0f,
                    onNavigate = { navigatedTo.add(it) },
                    onSelectLocation = {},
                    onShowRouteDetails = { routeDetailsFor = it },
                    onMapLongClick = null,
                    getCurrentLocationDescription = {
                        LocationDescription(name = "Milngavie", location = LngLatAlt())
                    },
                    searchBar = {},
                    streetPreviewState = org.scottishtecharmy.soundscape.geoengine.StreetPreviewState(),
                    streetPreviewFunctions = StreetPreviewFunctions(),
                    routeFunctions = RouteFunctions(),
                    goToAppSettings = {},
                    fullscreenMap = fullscreenMap,
                    permissionsRequired = false,
                    showMap = false,
                )
            }
        }

        composeTestRule.onNodeWithTag("routeDetails").assertIsDisplayed()
        composeTestRule.onNodeWithTag("routeDetails").performClick()

        // The route is identified by its database id - that's what the details screen loads by.
        assertEquals(7L, routeDetailsFor?.databaseId)
        assertEquals("Route to shops", routeDetailsFor?.name)
        // The first waypoint stands in for the route's location, matching RoutesViewModel.
        assertEquals(-4.3239319, routeDetailsFor?.location?.longitude ?: 0.0, 1e-9)

        // Nothing route-details-shaped may go through the plain string navigation, which is what
        // crashed: the graph has no destination matching it.
        assertNull(navigatedTo.firstOrNull { it.startsWith("route_details_screen") })
        assertTrue(navigatedTo.none { it.contains("/") })
    }
}
