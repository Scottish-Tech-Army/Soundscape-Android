package org.scottishtecharmy.soundscape.screens.home

sealed class HomeScreens(val route: String)
{
    data object Home : HomeScreens("home")
    data object Settings : HomeScreens("settings")
}