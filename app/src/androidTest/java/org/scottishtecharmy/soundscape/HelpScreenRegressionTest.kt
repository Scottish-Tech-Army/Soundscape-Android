@file:OptIn(ExperimentalComposeUiApi::class)

package org.scottishtecharmy.soundscape

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.scottishtecharmy.soundscape.screens.home.home.HelpScreen
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme

class HelpScreenRegressionTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun menu_help_and_tutorials_structure_regression() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

        composeTestRule.setContent {
            SoundscapeTheme {
                HelpScreen(
                    topic = "menu_help_and_tutorials",
                    navController = NavHostController(targetContext),
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .semantics { testTagsAsResourceId = true }
                )
            }
        }

        // Compare against baseline file
        composeTestRule.assertLayoutMatchesHybridBaseline(
            "help_screen_layouts/menu_help_and_tutorials.txt"
        )
    }
}