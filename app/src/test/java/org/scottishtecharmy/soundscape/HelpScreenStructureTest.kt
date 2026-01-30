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
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.scottishtecharmy.soundscape.screens.home.home.HelpScreen
import org.scottishtecharmy.soundscape.screens.home.home.MarkdownHelpScreen
import org.scottishtecharmy.soundscape.screens.home.home.StructureLog
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme

@OptIn(ExperimentalComposeUiApi::class)
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class HelpScreenStructureTest(testTopic: String) : HelpScreenRegressionTestBase(testTopic) {
    override fun compareAgainstBaseline(actualLayout: String) {
        // Compare against baseline file
        composeTestRule.assertLayoutMatchesHybridBaseline(
            "help_screen_layouts/${testTopic}",
            actualLayout,
            includeLayout = false
        )
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0}")
        fun data(): Iterable<Array<String>> {
            return HelpScreenTestShared.data()
        }
    }
}
