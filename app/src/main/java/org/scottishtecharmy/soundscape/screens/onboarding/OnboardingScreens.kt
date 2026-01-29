package org.scottishtecharmy.soundscape.screens.onboarding

sealed class OnboardingScreens(val route: String)
{
    data object Home : OnboardingScreens("home")
    data object Welcome : OnboardingScreens("welcome")
    data object Language : OnboardingScreens("language")
    data object Listening : OnboardingScreens("listening")
    data object Hearing : OnboardingScreens("hearing")
    data object Navigating : OnboardingScreens("navigating")
    data object AudioBeacons : OnboardingScreens("audiobeacons")
    data object OfflineStorage : OnboardingScreens("offlinestorage")
    data object Accessibility : OnboardingScreens("accessibility")
    data object Terms : OnboardingScreens("terms")
    data object Finish : OnboardingScreens("finish")
}