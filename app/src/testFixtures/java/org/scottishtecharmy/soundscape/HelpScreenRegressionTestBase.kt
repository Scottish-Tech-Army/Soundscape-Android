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
import org.scottishtecharmy.soundscape.screens.home.home.MarkdownHelpScreen
import org.scottishtecharmy.soundscape.screens.home.home.StructureLog
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme

abstract class HelpScreenRegressionTestBase(protected val testTopic: String) {
    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Some testTopic names contain a smart-quote apostrophe, which roboelectric fails to read from
     * the Windows filesystem, even though it works okay under an Android emulator.
     */
    protected val filenameSafeTestTopic = testTopic.replace("’", "'")

    @Test
    fun help_screen_regression() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val topic = HelpScreenTestShared.getTopic(testTopic)

        val structureLog = makeStructureLog()
        composeTestRule.setContent {
            SoundscapeTheme {
                MarkdownHelpScreen(
                    topic = topic,
                    navController = NavHostController(targetContext),
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .semantics { testTagsAsResourceId = true },
                    structureLog = structureLog
                )
            }
        }

        compareAgainstBaseline(structureLog)
    }

    protected abstract fun makeStructureLog(): StructureLog

    protected abstract fun compareAgainstBaseline(structureLog: StructureLog)
}
