package org.scottishtecharmy.soundscape

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import org.scottishtecharmy.soundscape.screens.onboarding.Welcome
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme
import org.junit.Rule
import org.junit.Test

class WelcomeScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun welcomeScreenTest(){
        composeTestRule.setContent {
            SoundscapeTheme {
                Welcome(navController = rememberNavController())
            }
        }
        // Unable to get translations strings using the stringResource() which is a composable so..
        val context: Context = ApplicationProvider.getApplicationContext()
        val stringWelcome = context.resources.getString(R.string.first_launch_welcome_title)

        // format for composeTestRule is:
        // composeTestRule{.finder}{.assertion}{.action}
        composeTestRule.onNodeWithText(stringWelcome).assertIsDisplayed()

        // deliberate fail as kersnazzle shouldn't be in the UI
        //composeTestRule.onNodeWithText("kersnazzle").assertIsDisplayed()

        // Delay so I can see it appear on my device screen
        Thread.sleep(5000)

    }
}