package org.scottishtecharmy.soundscape

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test
import org.scottishtecharmy.soundscape.screens.onboarding.language.LanguageComposable
import org.scottishtecharmy.soundscape.screens.onboarding.language.MockLanguagePreviewData
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme
import org.scottishtecharmy.soundscape.utils.TestTags

// This is very helpful:
// https://developer.android.com/develop/ui/compose/testing/testing-cheatsheet
class LanguageScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun WHEN_no_language_selected_THEN_no_language_label_displayed_AND_continue_button_disabled(){
        composeTestRule.setContent {
            LanguageComposable(
                supportedLanguages = MockLanguagePreviewData.languages,
                onNavigate = {},
                onLanguageSelected = {},
                selectedLanguageIndex = -1,
            )
        }
        val stringLanguageTitle = context.resources.getString(R.string.first_launch_soundscape_language)
        val stringLanguageText = context.resources.getString(R.string.first_launch_soundscape_language_text)

        composeTestRule.onNodeWithText(stringLanguageTitle)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(stringLanguageText)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(context.resources.getString(R.string.no_language_selected))
            .assertIsDisplayed()

        composeTestRule.onNodeWithText(context.resources.getString(R.string.ui_continue))
            .assertIsNotEnabled()

    }

    @Test
    fun WHEN_a_language_is_selected_THEN_its_name_appears_in_the_default_button_AND_language_is_marked_as_selected_in_the_menu(){
        val selectedIndex = 1
        composeTestRule.setContent {
            SoundscapeTheme {
                LanguageComposable(
                    supportedLanguages = MockLanguagePreviewData.languages,
                    onLanguageSelected = {},
                    selectedLanguageIndex = selectedIndex,
                    onNavigate = {}
                )
            }
        }

        composeTestRule.onNodeWithText(MockLanguagePreviewData.languages[selectedIndex].name)
            .assertIsDisplayed()
            .performClick()

        MockLanguagePreviewData.languages.forEachIndexed { index, language ->
            if( index == selectedIndex ) {
                composeTestRule.onNodeWithTag(
                    testTag = "${TestTags.LANGUAGE_DROPDOWN_ITEM}${MockLanguagePreviewData.languages[index].code}",
                    useUnmergedTree = true
                ).assertIsSelected()
            } else {
                composeTestRule.onNodeWithTag(
                    testTag = "${TestTags.LANGUAGE_DROPDOWN_ITEM}${MockLanguagePreviewData.languages[index].code}",
                    useUnmergedTree = true
                ).assertIsNotSelected()
            }

        }

        composeTestRule.onNodeWithText(context.resources.getString(R.string.ui_continue))
            .assertIsEnabled()
    }
}