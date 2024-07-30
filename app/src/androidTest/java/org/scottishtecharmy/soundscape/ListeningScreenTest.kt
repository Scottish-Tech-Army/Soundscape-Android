package org.scottishtecharmy.soundscape

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import org.scottishtecharmy.soundscape.screens.onboarding.Listening
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme
import org.junit.Rule
import org.junit.Test


// This is very helpful:
// https://developer.android.com/develop/ui/compose/testing/testing-cheatsheet
class ListeningScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun listeningScreenTest(){
        composeTestRule.setContent {
            SoundscapeTheme {
                Listening(navController = rememberNavController())
            }
        }

        val context: Context = ApplicationProvider.getApplicationContext()
        // Original iOS Soundscape doesn't have a content description for the image
        // so skipping it as we don't have the translation strings for it.
        val stringListeningTitle = context.resources.getString(R.string.first_launch_headphones_title)
        val stringListeningMessage1 = context.resources.getString(R.string.first_launch_headphones_message_1)
        val stringListeningMessage2 = context.resources.getString(R.string.first_launch_headphones_message_2)
        val stringListeningContinue = context.resources.getString(R.string.ui_continue)

        composeTestRule.onNodeWithText(stringListeningTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(stringListeningMessage1).assertIsDisplayed()
        composeTestRule.onNodeWithText(stringListeningMessage2).assertIsDisplayed()
        composeTestRule.onNodeWithText(stringListeningContinue).assertIsDisplayed()

        // Delay so I can see it appear on my device screen. Remove when using CI
        //Thread.sleep(5000)

    }
}