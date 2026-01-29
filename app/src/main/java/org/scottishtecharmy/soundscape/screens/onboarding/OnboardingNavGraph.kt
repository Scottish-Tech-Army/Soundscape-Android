package org.scottishtecharmy.soundscape.screens.onboarding

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.scottishtecharmy.soundscape.screens.onboarding.accessibility.AccessibilityOnboardingScreenVM
import org.scottishtecharmy.soundscape.screens.onboarding.audiobeacons.AudioBeaconsScreen
import org.scottishtecharmy.soundscape.screens.onboarding.finish.FinishScreen
import org.scottishtecharmy.soundscape.screens.onboarding.hearing.HearingScreen
import org.scottishtecharmy.soundscape.screens.onboarding.language.LanguageScreen
import org.scottishtecharmy.soundscape.screens.onboarding.listening.ListeningScreen
import org.scottishtecharmy.soundscape.screens.onboarding.navigating.NavigatingScreen
import org.scottishtecharmy.soundscape.screens.onboarding.offlinestorage.OfflineStorageOnboardingScreenVM
import org.scottishtecharmy.soundscape.screens.onboarding.terms.TermsScreen
import org.scottishtecharmy.soundscape.screens.onboarding.welcome.Welcome

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SetUpOnboardingNavGraph(
    navController: NavHostController,
    onFinish: () -> Unit,
) {
    val audioViewModel: AudioOnboardingViewModel = hiltViewModel()
    NavHost(
        navController = navController,
        startDestination = OnboardingScreens.Welcome.route
    ) {
        composable(OnboardingScreens.Welcome.route) {
            Welcome(
                onNavigate = { navController.navigate(OnboardingScreens.Language.route) },
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .semantics { testTagsAsResourceId = true }
            )
        }
        composable(OnboardingScreens.Language.route) {
            LanguageScreen(
                onNavigate = { navController.navigate(OnboardingScreens.Navigating.route) },
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .semantics { testTagsAsResourceId = true }
            )
        }
        composable(OnboardingScreens.Listening.route) {
            ListeningScreen(
                onNavigate = { navController.navigate(OnboardingScreens.Hearing.route) },
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .semantics { testTagsAsResourceId = true }
            )
        }
        composable(OnboardingScreens.Hearing.route) {
            HearingScreen(
                onBack = { navController.popBackStack() },
                onNavigate = { navController.navigate( OnboardingScreens.AudioBeacons.route) },
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .semantics { testTagsAsResourceId = true },
                audioViewModel
            )
        }
        composable(OnboardingScreens.Navigating.route) {
            NavigatingScreen(
                onNavigate = { navController.navigate(OnboardingScreens.Listening.route) },
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .semantics { testTagsAsResourceId = true }
            )
        }
        composable(OnboardingScreens.AudioBeacons.route) {
            AudioBeaconsScreen(
                onBack = { navController.popBackStack() },
                onNavigate = { navController.navigate(OnboardingScreens.OfflineStorage.route) },
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .semantics { testTagsAsResourceId = true },
                audioViewModel
            )
        }
        composable(OnboardingScreens.OfflineStorage.route) {
            OfflineStorageOnboardingScreenVM(
                onNavigate = { navController.navigate(OnboardingScreens.Accessibility.route) },
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .semantics { testTagsAsResourceId = true }
            )
        }

        composable(OnboardingScreens.Accessibility.route) {
            AccessibilityOnboardingScreenVM(
                onNavigate = { navController.navigate(OnboardingScreens.Terms.route) },
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .semantics { testTagsAsResourceId = true }
            )
        }

        composable(OnboardingScreens.Terms.route) {
            TermsScreen(onNavigate = {
                navController.navigate(OnboardingScreens.Finish.route)
            },
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .semantics { testTagsAsResourceId = true }
            )
        }
        composable(OnboardingScreens.Finish.route) {
            FinishScreen(
                onFinish = onFinish,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .semantics { testTagsAsResourceId = true }
            )
        }
    }
}