package org.scottishtecharmy.soundscape.screens.onboarding

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.scottishtecharmy.soundscape.screens.onboarding.audiobeacons.AudioBeaconsScreen
import org.scottishtecharmy.soundscape.screens.onboarding.hearing.HearingScreen
import org.scottishtecharmy.soundscape.screens.onboarding.language.LanguageScreen
import org.scottishtecharmy.soundscape.screens.onboarding.listening.ListeningScreen
import org.scottishtecharmy.soundscape.screens.onboarding.navigating.NavigatingScreen
import org.scottishtecharmy.soundscape.screens.onboarding.terms.TermsScreen
import org.scottishtecharmy.soundscape.screens.onboarding.welcome.Welcome

@Composable
fun SetUpOnboardingNavGraph(
    navController: NavHostController,
) {
    NavHost(
        navController = navController,
        startDestination = OnboardingScreens.Welcome.route
    ) {
        composable(OnboardingScreens.Welcome.route) {
            Welcome(onNavigate = { navController.navigate(OnboardingScreens.Language.route) })
        }
        composable(OnboardingScreens.Language.route) {
            LanguageScreen( onNavigate = { navController.navigate(OnboardingScreens.Navigating.route) })
        }
        composable(OnboardingScreens.Listening.route) {
            ListeningScreen(onNavigate = { navController.navigate(OnboardingScreens.Hearing.route) })
        }
        composable(OnboardingScreens.Hearing.route) {
            HearingScreen(onNavigate = { navController.navigate( OnboardingScreens.AudioBeacons.route) })
        }
        composable(OnboardingScreens.Navigating.route) {
            NavigatingScreen(onNavigate = { navController.navigate(OnboardingScreens.Listening.route) })
        }
        composable(OnboardingScreens.AudioBeacons.route) {
            AudioBeaconsScreen(onNavigate = { navController.navigate(OnboardingScreens.Terms.route) })
        }
        composable(OnboardingScreens.Terms.route) {
            TermsScreen(onNavigate = { navController.navigate(OnboardingScreens.Finish.route) })
        }
        composable(OnboardingScreens.Finish.route) {
            Finish()
        }
    }
}