package org.scottishtecharmy.soundscape.screens.home

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun SetUpHomeNavGraph(
    navController: NavHostController,
) {
    NavHost(
        navController = navController,
        startDestination = HomeScreens.Home.route
    ) {
        composable(HomeScreens.Home.route) {
            Home(onNavigate = { dest -> navController.navigate(dest) }, useView = true)
        }
        composable(HomeScreens.Settings.route) {
            // Always just pop back out of settings, don't add to the queue
            Settings(onNavigate = { navController.navigateUp() })
        }
    }
}