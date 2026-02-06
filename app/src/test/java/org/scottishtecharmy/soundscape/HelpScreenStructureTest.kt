package org.scottishtecharmy.soundscape

import androidx.compose.ui.ExperimentalComposeUiApi
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.scottishtecharmy.soundscape.screens.home.home.StructureLog

@OptIn(ExperimentalComposeUiApi::class)
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE, qualifiers = "w1000dp-h100000dp")
class HelpScreenStructureTest(testTopic: String) : HelpScreenRegressionTestBase(testTopic) {
    private val structure = StringBuilder()

    override fun makeStructureLog(): StructureLog = StructureLog { structure.appendLine(it) }

    override fun compareAgainstBaseline(structureLog: StructureLog) {
        // Compare against baseline file
        assertLayoutMatchesHybridBaseline(
            "help_screen_layouts/${filenameSafeTestTopic}.structure.txt",
            structure.toString(),
            "Structure"
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
