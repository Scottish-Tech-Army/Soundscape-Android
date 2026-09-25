package org.scottishtecharmy.soundscape

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.scottishtecharmy.soundscape.database.local.model.MarkerEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.beacon_action_callout_beacon
import org.scottishtecharmy.soundscape.resources.callouts_action_more_info
import org.scottishtecharmy.soundscape.resources.markers_action_add_to_markers
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.home.RouteFunctions
import org.scottishtecharmy.soundscape.screens.home.home.SharedHomeContent
import org.scottishtecharmy.soundscape.screens.home.home.StreetPreviewFunctions
import org.scottishtecharmy.soundscape.services.RoutePlayerState
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme

/**
 * The home screen's beacon panel used to show only the beacon's name, although both the
 * documentation and the original iOS app promise its distance too (issue #907), and it offered no
 * screen reader actions of its own (issue #908).
 *
 * These pin what a screen reader user gets: one node reading the beacon's name and its distance and
 * direction, whose actions are "Call out Beacon" (also the default activation), "More Info" and -
 * only when the beacon isn't already a saved marker - "Add to Markers". Mute and Remove are
 * deliberately absent: they are buttons in the same panel.
 */
class HomeBeaconCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val userLocation = LngLatAlt(-4.3239319, 55.9446396)

    /** A beacon, as RoutePlayer.startBeacon models it: a single-waypoint route, marker id unset. */
    private fun beaconRoute(markerId: Long = 0L) = RouteWithMarkers(
        RouteEntity(name = "Milngavie Library", description = ""),
        listOf(
            MarkerEntity(
                name = "Milngavie Library",
                longitude = -4.3210534,
                latitude = 55.9417227,
                markerId = markerId,
            ),
        ),
    )

    @Composable
    private fun content(
        route: RouteWithMarkers,
        onSaveMarker: ((LocationDescription) -> Unit)? = {},
        routeFunctions: RouteFunctions = RouteFunctions(),
    ) {
        val fullscreenMap = remember { mutableStateOf(false) }
        SoundscapeTheme(MutableStateFlow(ThemeState())) {
            SharedHomeContent(
                location = userLocation,
                beaconState = null,
                routePlayerState = RoutePlayerState(
                    routeData = route,
                    currentWaypoint = 0,
                    beaconOnly = true,
                ),
                heading = 0.0f,
                onNavigate = {},
                onSelectLocation = {},
                onShowRouteDetails = {},
                onSaveMarker = onSaveMarker,
                onMapLongClick = null,
                getCurrentLocationDescription = {
                    LocationDescription(name = "Milngavie", location = LngLatAlt())
                },
                searchBar = {},
                streetPreviewState =
                    org.scottishtecharmy.soundscape.geoengine.StreetPreviewState(),
                streetPreviewFunctions = StreetPreviewFunctions(),
                routeFunctions = routeFunctions,
                goToAppSettings = {},
                fullscreenMap = fullscreenMap,
                permissionsRequired = false,
                showMap = false,
            )
        }
    }

    @Test
    fun beaconPanel_showsDistanceAndDirection() {
        composeTestRule.setContent { content(beaconRoute()) }

        // ~390 m south east of the user location above, shown abbreviated ("390 m, SE").
        composeTestRule.onNodeWithTag("routeBeaconDistance", useUnmergedTree = true).assertIsDisplayed()
        val shown = composeTestRule
            .onNodeWithTag("routeBeaconDistance", useUnmergedTree = true)
            .fetchSemanticsNode()
            .config
            .getOrNull(androidx.compose.ui.semantics.SemanticsProperties.Text)
            ?.joinToString("") { it.text }
        assertNotNull("The beacon panel should show a distance", shown)
        assertTrue("Expected a distance and a direction, got '$shown'", shown!!.contains("SE"))

        // The screen reader gets the spelled-out direction, alongside the beacon's name.
        val description = composeTestRule
            .onNodeWithTag("routeBeaconTitle", useUnmergedTree = true)
            .fetchSemanticsNode()
            .config
            .getOrNull(androidx.compose.ui.semantics.SemanticsProperties.ContentDescription)
            ?.joinToString(", ")
        assertNotNull(description)
        assertTrue(
            "Expected the beacon name in '$description'",
            description!!.contains("Milngavie Library"),
        )
        assertTrue(
            "Expected a spelled-out direction in '$description'",
            description.contains("South East", ignoreCase = true),
        )
    }

    @Test
    fun beaconPanel_offersCalloutMoreInfoAndAddToMarkers() {
        var calledOut = 0
        var moreInfo = 0
        var saved: LocationDescription? = null
        composeTestRule.setContent {
            content(
                beaconRoute(),
                onSaveMarker = { saved = it },
                routeFunctions = RouteFunctions(
                    calloutBeacon = { calledOut++ },
                    beaconMoreInfo = { moreInfo++ },
                ),
            )
        }

        val node = composeTestRule
            .onNodeWithTag("routeBeaconTitle", useUnmergedTree = true)
            .fetchSemanticsNode()
        val labels = node.config.getOrNull(SemanticsActions.CustomActions)?.map { it.label }
        assertEquals(
            listOf(
                runBlocking { getString(Res.string.beacon_action_callout_beacon) },
                runBlocking { getString(Res.string.callouts_action_more_info) },
                runBlocking { getString(Res.string.markers_action_add_to_markers) },
            ),
            labels,
        )

        // Activating the panel calls out the beacon rather than muting it, unlike iOS.
        val onClick = node.config.getOrNull(SemanticsActions.OnClick)
        assertEquals(
            runBlocking { getString(Res.string.beacon_action_callout_beacon) },
            onClick?.label,
        )
        composeTestRule.runOnUiThread { onClick?.action?.invoke() }
        assertEquals(1, calledOut)

        // "More Info" is spoken by the service rather than announced by the screen reader, so
        // that it can be triggered without the screen too.
        val moreInfoAction = node.config.getOrNull(SemanticsActions.CustomActions)
            ?.first { it.label == runBlocking { getString(Res.string.callouts_action_more_info) } }
        composeTestRule.runOnUiThread { moreInfoAction?.action?.invoke() }
        assertEquals(1, moreInfo)

        // ...and "Add to Markers" hands the beacon's location over to be saved.
        val addToMarkers = node.config.getOrNull(SemanticsActions.CustomActions)
            ?.first { it.label == runBlocking { getString(Res.string.markers_action_add_to_markers) } }
        composeTestRule.runOnUiThread { addToMarkers?.action?.invoke() }
        assertEquals("Milngavie Library", saved?.name)
        assertEquals(-4.3210534, saved?.location?.longitude ?: 0.0, 1e-9)
    }

    @Test
    fun beaconPanel_omitsAddToMarkers_whenTheBeaconIsAlreadyAMarker() {
        composeTestRule.setContent { content(beaconRoute(markerId = 42L)) }

        val labels = composeTestRule
            .onNodeWithTag("routeBeaconTitle", useUnmergedTree = true)
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsActions.CustomActions)
            ?.map { it.label }
        assertNotNull(labels)
        assertNull(
            labels!!.firstOrNull {
                it == runBlocking { getString(Res.string.markers_action_add_to_markers) }
            },
        )
    }
}
