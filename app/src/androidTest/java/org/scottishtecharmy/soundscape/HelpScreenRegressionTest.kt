@file:OptIn(ExperimentalComposeUiApi::class)

package org.scottishtecharmy.soundscape

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class HelpScreenRegressionTest(testTopic: String) : HelpScreenRegressionTestBase(testTopic) {
    override fun compareAgainstBaseline(actualLayout: String) {
        // Compare against baseline file
        composeTestRule.assertLayoutMatchesHybridBaseline(
            "help_screen_layouts/${filenameSafeTestTopic}",
            actualLayout
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
