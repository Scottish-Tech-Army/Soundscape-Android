package org.scottishtecharmy.soundscape

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToLog
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test
import org.scottishtecharmy.soundscape.screens.onboarding.language.LanguageComposable
import org.scottishtecharmy.soundscape.screens.onboarding.language.LanguageSelectionBox
import org.scottishtecharmy.soundscape.screens.onboarding.language.MockLanguagePreviewData
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme

// This is very helpful:
// https://developer.android.com/develop/ui/compose/testing/testing-cheatsheet
class LanguageScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun languageScreenTest(){
        composeTestRule.setContent {
            LanguageComposable(
                supportedLanguages = MockLanguagePreviewData.languages,
                onNavigate = {},
                onLanguageSelected = {},
                selectedLanguageIndex = -1,
            )
        }
        val context: Context = ApplicationProvider.getApplicationContext()
        val stringLanguageTitle = context.resources.getString(R.string.first_launch_soundscape_language)
        val stringLanguageText = context.resources.getString(R.string.first_launch_soundscape_language_text)

        composeTestRule.onNodeWithText(stringLanguageTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(stringLanguageText).assertIsDisplayed()
    }

    @Test
    fun languageSelectionBoxTest(){
        val selectedIndex = 1
        composeTestRule.setContent {
            SoundscapeTheme {
                LanguageSelectionBox(
                    allLanguages = MockLanguagePreviewData.languages,
                    onLanguageSelected = {},
                    selectedLanguageIndex = selectedIndex
                )
            }
        }
        composeTestRule.onRoot().printToLog("languageSelectionBoxTest") // TO print tree in logcat for debugging

        composeTestRule.onNodeWithTag(
            testTag = "LANGUAGE_SELECTION_${MockLanguagePreviewData.languages[selectedIndex].code}",
            useUnmergedTree = true
        ).assertIsSelected()
    }
}