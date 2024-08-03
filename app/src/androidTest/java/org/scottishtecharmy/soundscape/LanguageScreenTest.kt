package org.scottishtecharmy.soundscape

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test
import org.scottishtecharmy.soundscape.screens.onboarding.Language
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme

// This is very helpful:
// https://developer.android.com/develop/ui/compose/testing/testing-cheatsheet
class LanguageScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun languageScreenTest(){
        //Thread.sleep(20000)
        composeTestRule.setContent {
            SoundscapeTheme {
                Language(navController = rememberNavController())
            }
        }
        val context: Context = ApplicationProvider.getApplicationContext()
        val stringLanguageTitle = context.resources.getString(R.string.first_launch_soundscape_language)
        //val stringLanguageText = context.resources.getString(R.string.first_launch_soundscape_language_text)
        //val stringLanguageContinue = context.resources.getString(R.string.ui_continue)

        composeTestRule.onNodeWithText(stringLanguageTitle).assertIsDisplayed()
        //composeTestRule.onNodeWithText(stringLanguageText).assertIsDisplayed()
        //composeTestRule.onNodeWithText(stringLanguageContinue).assertIsDisplayed()


        // Delay so I can see it appear on my device screen. Remove when using CI
        //Thread.sleep(5000)
    }

    /*@Test
    fun languageSelectionBoxTest(){
        composeTestRule.setContent {
            SoundscapeTheme {
                LanguageSelectionBox()
            }
        }
        val languageList = listOf(
            org.scottishtecharmy.soundscape.components.Language(
                name = "Dansk",
                code = "da"
            ),
            org.scottishtecharmy.soundscape.components.Language(
                name = "Deutsch",
                code = "de"
            ),
            org.scottishtecharmy.soundscape.components.Language(
                name = "Ελληνικά",
                code = "el"
            ),
            org.scottishtecharmy.soundscape.components.Language(
                name = "English",
                code = "en"
            ),
            org.scottishtecharmy.soundscape.components.Language(
                name = "Español",
                code = "es"
            ),
            org.scottishtecharmy.soundscape.components.Language(
                name = "Suomi",
                code = "fi"
            ),
            org.scottishtecharmy.soundscape.components.Language(
                name = "Français",
                code = "fr"
            ),
            org.scottishtecharmy.soundscape.components.Language(
                name = "Italiano",
                code = "it"
            ),
            org.scottishtecharmy.soundscape.components.Language(
                name = "日本語",
                code = "ja"
            )
        )

        for(language in languageList){
            composeTestRule.onNodeWithText(language.name).assertIsDisplayed()
        }
        // Delay so I can see it appear on my device screen. Remove when using CI
        //Thread.sleep(10000)
    }*/
}