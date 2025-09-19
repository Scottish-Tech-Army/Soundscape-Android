package org.scottishtecharmy.soundscape

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import org.scottishtecharmy.soundscape.screens.onboarding.welcome.Welcome
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme
import org.junit.Rule
import org.junit.Test

// This is very helpful:
// https://developer.android.com/develop/ui/compose/testing/testing-cheatsheet
class WelcomeScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun welcomeScreenTest(){
        composeTestRule.setContent {
            SoundscapeTheme {
                Welcome(onNavigate = {})
            }
        }
        // Unable to get translations strings using the stringResource() which is a composable so..
        val context: Context = ApplicationProvider.getApplicationContext()
        val stringWelcomeTitle = context.resources.getString(R.string.first_launch_welcome_title)
        val stringWelcomeDescription = context.resources.getString(R.string.first_launch_welcome_description)
        val stringWelcomeGetStarted = context.resources.getString(R.string.first_launch_welcome_button)

        // format for composeTestRule is:
        // composeTestRule{.finder}{.assertion}{.action}

        composeTestRule.onNodeWithText(stringWelcomeTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(stringWelcomeDescription).assertIsDisplayed()
        composeTestRule.onNodeWithText(stringWelcomeGetStarted).assertIsDisplayed()

        composeTestRule.onNodeWithTag("welcomeScreenContinueButton").assertIsDisplayed()

        // Delay so I can see it appear on my device screen. Remove when using CI
        Thread.sleep(5000)

    }
}