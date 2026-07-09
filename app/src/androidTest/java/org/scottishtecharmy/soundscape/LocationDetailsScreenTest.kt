package org.scottishtecharmy.soundscape

import android.content.Context
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.core.content.edit
import androidx.navigation.compose.rememberNavController
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.locationDetails.LocationDetails
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme

class LocationDetailsScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val destination = LngLatAlt(7.4213, 43.7339)

    @Before
    fun setUp() {
        PreferenceManager.getDefaultSharedPreferences(context).edit(commit = true) {
            putBoolean(MainActivity.SHOW_MAP_KEY, false)
        }
    }

    @Test
    fun startDirectionsAction_hasTalkBackHintAndStartsTurnByTurn() {
        var turnByTurnDestination: LngLatAlt? = null

        composeTestRule.setContent {
            SoundscapeTheme {
                LocationDetails(
                    locationDescription = LocationDescription(
                        name = "Monaco destination",
                        location = destination,
                        description = "21 Rue Princesse Caroline"
                    ),
                    navController = rememberNavController(),
                    location = LngLatAlt(7.4246, 43.7384),
                    heading = 0.0F,
                    createBeacon = {},
                    startTurnByTurn = { turnByTurnDestination = it },
                    saveMarker = { _, _, _, _ -> },
                    deleteMarker = {},
                    enableStreetPreview = {},
                    shareLocation = { _, _ -> },
                    offlineMaps = {},
                    showDialog = {},
                    getLocationDescription = { LocationDescription("Current location", it) },
                )
            }
        }

        val directionsHint = context.resources.getString(
            R.string.location_detail_action_directions_hint
        )
        val directionsAction = composeTestRule.onNodeWithTag(
            "locationDetailsStartDirections",
            useUnmergedTree = true
        )

        directionsAction
            .assertIsDisplayed()
            .assert(hasClickAction())

        val clickAction = directionsAction.fetchSemanticsNode()
            .config
            .getOrElseNullable(SemanticsActions.OnClick) { null }

        assertNotNull("Start directions should expose a TalkBack click action", clickAction)
        assertEquals(directionsHint, clickAction?.label)

        directionsAction.performClick()

        assertEquals(destination, turnByTurnDestination)
    }
}
