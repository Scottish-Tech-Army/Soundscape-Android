package com.kersnazzle.soundscapealpha.screens.navigation

sealed class Screens(val route: String)
{
    data object Home : Screens("home")
    data object Welcome : Screens("welcome")
    data object Language : Screens("language")
    data object Listening : Screens("listening")
    data object Hearing : Screens("hearing")
    data object Navigating : Screens("navigating")
    data object AudioBeacons : Screens("audiobeacons")
}