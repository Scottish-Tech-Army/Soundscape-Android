package org.scottishtecharmy.soundscape.screens.onboarding

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.os.LocaleListCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.koin.androidx.compose.koinViewModel
import org.scottishtecharmy.soundscape.screens.onboarding.accessibility.AccessibilityOnboardingScreenVM
import org.scottishtecharmy.soundscape.screens.onboarding.audiobeacons.AudioBeaconsScreen
import org.scottishtecharmy.soundscape.screens.onboarding.battery.BatteryOptimizationScreen
import org.scottishtecharmy.soundscape.screens.onboarding.finish.FinishScreen
import org.scottishtecharmy.soundscape.screens.onboarding.hearing.HearingScreen
import org.scottishtecharmy.soundscape.screens.onboarding.language.SharedLanguageScreen
import org.scottishtecharmy.soundscape.screens.onboarding.language.getAppLocale
import org.scottishtecharmy.soundscape.screens.onboarding.language.getSystemLocale
import org.scottishtecharmy.soundscape.screens.onboarding.language.indexOfBestLanguageMatch
import org.scottishtecharmy.soundscape.screens.onboarding.language.supportedLanguages
import org.scottishtecharmy.soundscape.screens.onboarding.listening.ListeningScreen
import org.scottishtecharmy.soundscape.screens.onboarding.navigating.NavigatingScreen
import org.scottishtecharmy.soundscape.screens.onboarding.offlinestorage.OfflineStorageOnboardingScreenVM
import org.scottishtecharmy.soundscape.screens.onboarding.terms.TermsScreen
import org.scottishtecharmy.soundscape.screens.onboarding.welcome.Welcome

@Composable
fun SetUpOnboardingNavGraph(
    navController: NavHostController,
    onFinish: () -> Unit,
) {
    val audioViewModel: AudioOnboardingViewModel = koinViewModel()
    NavHost(
        navController = navController,
        startDestination = OnboardingScreens.Welcome.route
    ) {
        composable(OnboardingScreens.Welcome.route) {
            Welcome(
                onNavigate = { navController.navigate(OnboardingScreens.Language.route) },
                modifier = Modifier
            )
        }
        composable(OnboardingScreens.Language.route) {
            val initialIndex = remember {
                indexOfBestLanguageMatch(getAppLocale() ?: getSystemLocale())
            }
            var selectedIndex by remember { mutableIntStateOf(initialIndex) }
            SharedLanguageScreen(
                supportedLanguages = supportedLanguages,
                selectedLanguageIndex = selectedIndex,
                onLanguageSelected = { language ->
                    selectedIndex = supportedLanguages.indexOf(language)
                    AppCompatDelegate.setApplicationLocales(
                        LocaleListCompat.forLanguageTags("${language.code}-${language.region}"),
                    )
                },
                onContinue = { navController.navigate(OnboardingScreens.Navigating.route) },
                modifier = Modifier,
            )
        }
        composable(OnboardingScreens.Listening.route) {
            ListeningScreen(
                onNavigate = { navController.navigate(OnboardingScreens.Hearing.route) }
            )
        }
        composable(OnboardingScreens.Hearing.route) {
            HearingScreen(
                onBack = { navController.popBackStack() },
                onNavigate = { navController.navigate(OnboardingScreens.AudioBeacons.route) },
                audioViewModel
            )
        }
        composable(OnboardingScreens.Navigating.route) {
            NavigatingScreen(
                onNavigate = { navController.navigate(OnboardingScreens.BatteryOptimization.route) }
            )
        }
        composable(OnboardingScreens.BatteryOptimization.route) {
            BatteryOptimizationScreen(
                onNavigate = { navController.navigate(OnboardingScreens.Listening.route) }
            )
        }
        composable(OnboardingScreens.AudioBeacons.route) {
            AudioBeaconsScreen(
                onBack = { navController.popBackStack() },
                onNavigate = { navController.navigate(OnboardingScreens.OfflineStorage.route) },
                audioViewModel
            )
        }
        composable(OnboardingScreens.OfflineStorage.route) {
            OfflineStorageOnboardingScreenVM(
                onNavigate = { navController.navigate(OnboardingScreens.Accessibility.route) },
            )
        }

        composable(OnboardingScreens.Accessibility.route) {
            AccessibilityOnboardingScreenVM(
                onNavigate = { navController.navigate(OnboardingScreens.Terms.route) },
            )
        }

        composable(OnboardingScreens.Terms.route) {
            TermsScreen(
                onNavigate = {
                    navController.navigate(OnboardingScreens.Finish.route)
                },
            )
        }
        composable(OnboardingScreens.Finish.route) {
            FinishScreen(
                onFinish = onFinish,
            )
        }
    }
}