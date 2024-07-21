package com.kersnazzle.soundscapealpha.screens.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

import com.kersnazzle.soundscapealpha.screens.Home
import com.kersnazzle.soundscapealpha.screens.onboarding.AudioBeacons
import com.kersnazzle.soundscapealpha.screens.onboarding.Hearing
import com.kersnazzle.soundscapealpha.screens.onboarding.Language
import com.kersnazzle.soundscapealpha.screens.onboarding.Listening
import com.kersnazzle.soundscapealpha.screens.onboarding.Navigating
import com.kersnazzle.soundscapealpha.screens.onboarding.Welcome

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
        }
        composable(Screens.Home.route) {
            Home(
                navController = navController
            )
        }

    }
}