package org.scottishtecharmy.soundscape

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.first_launch_headphones_message_1
import org.scottishtecharmy.soundscape.resources.first_launch_headphones_message_2
import org.scottishtecharmy.soundscape.resources.first_launch_headphones_title
import org.scottishtecharmy.soundscape.resources.ui_continue
import org.scottishtecharmy.soundscape.screens.onboarding.listening.Listening
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme


// This is very helpful:
// https://developer.android.com/develop/ui/compose/testing/testing-cheatsheet
class ListeningScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun listeningScreenTest() {
        composeTestRule.setContent {
            SoundscapeTheme {
                Listening(onNavigate = {})
            }
        }

        val context: Context = ApplicationProvider.getApplicationContext()
        // Original iOS Soundscape doesn't have a content description for the image
        // so skipping it as we don't have the translation strings for it.
        val stringListeningTitle =
            kotlinx.coroutines.runBlocking { org.jetbrains.compose.resources.getString(Res.string.first_launch_headphones_title) }
        val stringListeningMessage1 =
            kotlinx.coroutines.runBlocking { org.jetbrains.compose.resources.getString(Res.string.first_launch_headphones_message_1) }
        val stringListeningMessage2 =
            kotlinx.coroutines.runBlocking { org.jetbrains.compose.resources.getString(Res.string.first_launch_headphones_message_2) }
        val stringListeningContinue =
            kotlinx.coroutines.runBlocking { org.jetbrains.compose.resources.getString(Res.string.ui_continue) }

        // The content lives in a verticalScroll container that can be taller than the
        // viewport on smaller screens/emulators, so scroll each node into view before
        // asserting - otherwise "Continue" (at the bottom) can fail as "not displayed"
        // purely because it's scrolled out of the visible area, not because it's missing.
        composeTestRule.onNodeWithText(stringListeningTitle).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(stringListeningMessage1).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(stringListeningMessage2).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(stringListeningContinue).performScrollTo().assertIsDisplayed()

        // Delay so I can see it appear on my device screen. Remove when using CI
        //Thread.sleep(5000)

    }
}