@file:OptIn(ExperimentalComposeUiApi::class)

package org.scottishtecharmy.soundscape

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.preference.PreferenceManager
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
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

    @Before
    fun setUp() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        PreferenceManager.getDefaultSharedPreferences(targetContext)
            .edit()
            .putBoolean(MainActivity.MARKDOWN_HELP_KEY, true)
            .commit()
    }

    @Test
    fun help_screen_regression() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val topic = HelpScreenTestShared.getTopic(testTopic)

        val structureLog = makeStructureLog()
        composeTestRule.setContent {
            SoundscapeTheme {
                if (topic == "page:faq:help-frequently-asked-questions.md:When%20should%20I%20use%20Soundscape%3F") {
                    // Wrap in a tall Box to ensure LazyColumn renders this entire long page.
                    Box(modifier = Modifier.requiredHeight(5000.dp)) {
                        HelpScreen(topic, targetContext, structureLog)
                    }
                } else {
                    HelpScreen(topic, targetContext, structureLog)
                }
            }
        }

        compareAgainstBaseline(structureLog)
    }

    @Composable
    private fun HelpScreen(
        topic: String,
        targetContext: Context,
        structureLog: StructureLog
    ) {
        MarkdownHelpScreen(
            topic = topic,
            navController = NavHostController(targetContext),
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .semantics { testTagsAsResourceId = true },
            structureLog = structureLog
        )
    }

    protected abstract fun makeStructureLog(): StructureLog

    protected abstract fun compareAgainstBaseline(structureLog: StructureLog)
}
