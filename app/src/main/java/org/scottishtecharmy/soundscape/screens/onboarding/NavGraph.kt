package org.scottishtecharmy.soundscape.screens.onboarding

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun SetUpNavGraph(
    navController: NavHostController,
) {
    NavHost(
        navController = navController,
        startDestination = Screens.Welcome.route
    ) {
        composable(Screens.Welcome.route) {
            Welcome(onNavigate = { dest -> navController.navigate(dest) })
        }
        composable(Screens.Language.route) {
            Language(onNavigate = { dest -> navController.navigate(dest) }, null)
        }
        composable(Screens.Listening.route) {
            Listening(onNavigate = { dest -> navController.navigate(dest) })
        }
        composable(Screens.Hearing.route) {
            Hearing(onNavigate = { dest -> navController.navigate(dest) }, true)
        }
        composable(Screens.Navigating.route) {
            Navigating(onNavigate = { dest -> navController.navigate(dest) })
        }
        composable(Screens.AudioBeacons.route) {
            AudioBeacons(onNavigate = { dest -> navController.navigate(dest) }, null)
        }
        composable(Screens.Terms.route) {
            Terms(onNavigate = { dest -> navController.navigate(dest) })
        }
        composable(Screens.Finish.route) {
            Finish()
        }
    }
}