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
import java.util.Locale
import androidx.navigation.NavHostController
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.scottishtecharmy.soundscape.screens.home.home.HelpScreen
import org.scottishtecharmy.soundscape.screens.home.home.SectionType
import org.scottishtecharmy.soundscape.screens.home.home.helpPages
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme

class HelpScreenRegressionTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Retrieves a string resource for the default locale (e.g., from `values/strings.xml`).
     *
     * @param id The ID of the string resource to retrieve (e.g., R.string.something).
     * @return The string value from the default resource file.
     */
    fun getDefaultString(id: Int): String {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val defaultConfig = targetContext.resources.configuration
        // Use Locale.ROOT to be explicit about the base resource
        defaultConfig.setLocale(Locale.ROOT)
        val defaultContext = targetContext.createConfigurationContext(defaultConfig)
        return defaultContext.getString(id)
    }

    @Test
    fun dump_help_topics() {
        val entryPage = helpPages.find { it.titleId == R.string.menu_help_and_tutorials }!!

        println("Topics from entry page:")
        val topicsFromEntryPage = entryPage.sections.filter { it.type == SectionType.Link }
        for (section in topicsFromEntryPage) {
            println("page_" + getDefaultString(section.textId).toFilenameSafe())
        }

        println("Topics for individual FAQ pages:")
        val faqPage = helpPages.find { it.titleId == R.string.faq_title }!!
        val individualFaqTopics = faqPage.sections.filter { it.type == SectionType.Faq }
        for (faq in individualFaqTopics) {
            println("faq_" + getDefaultString(faq.textId).toFilenameSafe())
        }
    }

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

    companion object {
        private val alphanumeric = Regex("[a-zA-Z0-9_]")

        fun String.toFilenameSafe(): String {
            val builder = StringBuilder()
            for (char in this) {
                when {
                    char == ' ' -> builder.append("__")
                    alphanumeric.matches(char.toString()) -> builder.append(char)
                    else -> builder.append('_').append(char.code.toHexString())
                }
            }
            return builder.toString()
        }

//        fun String.fromFilenameSafe(): String {
//\
//            ./.val builder = StringBuilder()
//            for (char in this) {
//                when {
//                    char == ' ' -> builder.append("__")
//                    alphanumeric.matches(char.toString()) -> builder.append(char)
//                    else -> builder.append('_').append(char.code.toHexString())
//                }
//            }
//            return builder.toString()
//        }
    }

}