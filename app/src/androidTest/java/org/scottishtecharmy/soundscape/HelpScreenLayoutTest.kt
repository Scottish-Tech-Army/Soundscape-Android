@file:OptIn(ExperimentalComposeUiApi::class)

package org.scottishtecharmy.soundscape

import androidx.compose.ui.ExperimentalComposeUiApi
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.scottishtecharmy.soundscape.screens.home.home.StructureLog

@RunWith(Parameterized::class)
class HelpScreenLayoutTest(testTopic: String) : HelpScreenRegressionTestBase(testTopic) {
    override fun makeStructureLog(): StructureLog = StructureLog { }

    override fun compareAgainstBaseline(structureLog: StructureLog) {
        // Compare against baseline file
        assertLayoutMatchesHybridBaseline(
            "help_screen_layouts/${filenameSafeTestTopic}.txt",
            composeTestRule.dumpLayoutTree(),
            "Layout"
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
