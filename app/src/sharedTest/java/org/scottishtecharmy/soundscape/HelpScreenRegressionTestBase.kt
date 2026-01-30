@file:OptIn(ExperimentalComposeUiApi::class)

package org.scottishtecharmy.soundscape

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
import org.junit.runners.Parameterized
import org.scottishtecharmy.soundscape.screens.home.home.HelpScreen
import org.scottishtecharmy.soundscape.screens.home.home.MarkdownHelpScreen
import org.scottishtecharmy.soundscape.screens.home.home.StructureLog
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme

abstract class HelpScreenRegressionTestBase(protected val testTopic: String) {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun structure_regression() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val topic = HelpScreenTestShared.getTopic(testTopic)

        val structureLog = StringBuilder()
        composeTestRule.setContent {
            SoundscapeTheme {
                // TODO 2025-12-12 Hugh Greene: This is just a special case for the root page,
                // for now.
                if (topic == "Help and Tutorials") {
                    MarkdownHelpScreen(
                        topic = topic,
                        navController = NavHostController(targetContext),
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .semantics { testTagsAsResourceId = true },
                        structureLog = StructureLog { structureLog.append(it).append("\n") }
                    )
                }
                else {
                    HelpScreen(
                        topic = topic,
                        navController = NavHostController(targetContext),
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .semantics { testTagsAsResourceId = true },
                        structureLog = StructureLog { structureLog.append(it).append("\n") }
                    )
                }
            }
        }

        compareAgainstBaseline(structureLog.toString())
    }

    protected abstract fun compareAgainstBaseline(actualLayout: String)
}
