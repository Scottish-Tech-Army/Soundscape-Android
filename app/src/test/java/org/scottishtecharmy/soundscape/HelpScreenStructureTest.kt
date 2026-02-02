package org.scottishtecharmy.soundscape

import androidx.compose.ui.ExperimentalComposeUiApi
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

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
        @ParameterizedRobolectricTestRunner.Parameters(name = "{index}: {0}")
        fun data(): Iterable<Array<String>> {
            return HelpScreenTestShared.data()
        }
    }
}
