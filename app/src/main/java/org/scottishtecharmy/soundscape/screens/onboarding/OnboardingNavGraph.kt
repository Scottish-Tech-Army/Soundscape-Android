package org.scottishtecharmy.soundscape.screens.onboarding

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun SetUpOnboardingNavGraph(
    navController: NavHostController,
) {
    NavHost(
        navController = navController,
        startDestination = OnboardingScreens.Welcome.route
    ) {
        composable(OnboardingScreens.Welcome.route) {
            Welcome(onNavigate = { dest -> navController.navigate(dest) })
        }
        composable(OnboardingScreens.Language.route) {
            Language(onNavigate = { dest -> navController.navigate(dest) }, null)
        }
        composable(OnboardingScreens.Listening.route) {
            Listening(onNavigate = { dest -> navController.navigate(dest) })
        }
        composable(OnboardingScreens.Hearing.route) {
            Hearing(onNavigate = { dest -> navController.navigate(dest) }, true)
        }
        composable(OnboardingScreens.Navigating.route) {
            Navigating(onNavigate = { dest -> navController.navigate(dest) })
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