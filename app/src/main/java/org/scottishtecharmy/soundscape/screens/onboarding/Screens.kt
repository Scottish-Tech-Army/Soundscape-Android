package org.scottishtecharmy.soundscape.screens.onboarding

sealed class Screens(val route: String)
{
    data object Home : Screens("home")
    data object Welcome : Screens("welcome")
    data object Language : Screens("language")
    data object Listening : Screens("listening")
    data object Hearing : Screens("hearing")
    data object Navigating : Screens("navigating")
    data object AudioBeacons : Screens("audiobeacons")
    data object Terms : Screens("terms")
    data object Finish : Screens("finish")
}