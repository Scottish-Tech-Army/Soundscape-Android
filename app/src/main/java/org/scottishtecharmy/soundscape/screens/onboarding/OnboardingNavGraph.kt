package org.scottishtecharmy.soundscape.screens.onboarding

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.scottishtecharmy.soundscape.screens.onboarding.language.LanguageScreen
import org.scottishtecharmy.soundscape.screens.onboarding.navigating.NavigatingScreen
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
            Listening(onNavigate = { dest -> navController.navigate(dest) })
        }
        composable(OnboardingScreens.Hearing.route) {
            Hearing(onNavigate = { dest -> navController.navigate(dest) }, true)
        }
        composable(OnboardingScreens.Navigating.route) {
            NavigatingScreen(onNavigate = { navController.navigate(OnboardingScreens.Listening.route) })
        }
        composable(OnboardingScreens.AudioBeacons.route) {
            AudioBeacons(onNavigate = { dest -> navController.navigate(dest) }, null)
        }
        composable(OnboardingScreens.Terms.route) {
            Terms(onNavigate = { dest -> navController.navigate(dest) })
        }
        composable(OnboardingScreens.Finish.route) {
            Finish()
        }
    }
}