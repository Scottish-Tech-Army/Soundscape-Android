package org.scottishtecharmy.soundscape.screens.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

import org.scottishtecharmy.soundscape.screens.Home
import org.scottishtecharmy.soundscape.screens.onboarding.AudioBeacons
import org.scottishtecharmy.soundscape.screens.onboarding.Finish
import org.scottishtecharmy.soundscape.screens.onboarding.Hearing
import org.scottishtecharmy.soundscape.screens.onboarding.Language
import org.scottishtecharmy.soundscape.screens.onboarding.Listening
import org.scottishtecharmy.soundscape.screens.onboarding.Navigating
import org.scottishtecharmy.soundscape.screens.onboarding.Terms
import org.scottishtecharmy.soundscape.screens.onboarding.Welcome

@Composable
fun SetUpNavGraph(
    isFirstLaunch: Boolean,
    navController: NavHostController,
) {
    val startDestination =
        if (isFirstLaunch) {
            Screens.Welcome.route
        } else {
            Screens.Home.route
        }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {


        if (isFirstLaunch) {
            composable(Screens.Welcome.route) {
                Welcome(navController = navController)
            }
            composable(Screens.Language.route) {
                Language(navController = navController)
            }
            composable(Screens.Listening.route) {
                Listening(navController = navController)
            }
            composable(Screens.Hearing.route) {
                Hearing(navController = navController)
            }
            composable(Screens.Navigating.route) {
                Navigating(navController = navController)
            }
            composable(Screens.AudioBeacons.route) {
                AudioBeacons(navController = navController)
            }
            composable(Screens.Terms.route) {
                Terms(navController = navController)
            }
            composable(Screens.Finish.route) {
                Finish(navController = navController)
            }
        }
        composable(Screens.Home.route) {
            Home(navController = navController)
        }
    }
}