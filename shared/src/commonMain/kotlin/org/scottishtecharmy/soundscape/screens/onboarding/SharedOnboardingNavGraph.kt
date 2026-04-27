package org.scottishtecharmy.soundscape.screens.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.audio.AudioEngine
import org.scottishtecharmy.soundscape.audio.AudioType
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.preferences.PreferenceKeys
import org.scottishtecharmy.soundscape.preferences.PreferencesProvider
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.first_launch_callouts_example_1
import org.scottishtecharmy.soundscape.resources.first_launch_callouts_example_3
import org.scottishtecharmy.soundscape.resources.first_launch_callouts_example_4
import org.scottishtecharmy.soundscape.screens.onboarding.accessibility.AccessibilityOnboardingScreen
import org.scottishtecharmy.soundscape.screens.onboarding.accessibility.isScreenReaderEnabled
import org.scottishtecharmy.soundscape.screens.onboarding.audiobeacons.AudioBeacons
import org.scottishtecharmy.soundscape.screens.onboarding.finish.FinishScreen
import org.scottishtecharmy.soundscape.screens.onboarding.hearing.Hearing
import org.scottishtecharmy.soundscape.screens.onboarding.listening.Listening
import org.scottishtecharmy.soundscape.screens.onboarding.terms.TermsScreen
import org.scottishtecharmy.soundscape.screens.onboarding.welcome.Welcome

private object OnboardingRoutes {
    const val WELCOME = "onboarding_welcome"
    const val LISTENING = "onboarding_listening"
    const val HEARING = "onboarding_hearing"
    const val AUDIO_BEACONS = "onboarding_audio_beacons"
    const val ACCESSIBILITY = "onboarding_accessibility"
    const val TERMS = "onboarding_terms"
    const val FINISH = "onboarding_finish"
}

@Composable
fun SharedOnboardingNavHost(
    audioEngine: AudioEngine,
    preferencesProvider: PreferencesProvider,
    beaconTypes: List<String>,
    onFinish: () -> Unit,
) {
    val navController = rememberNavController()

    // Audio beacon state shared across Hearing and AudioBeacons screens
    var beaconHandle by remember { mutableStateOf(0L) }
    var currentBeaconType by remember { mutableStateOf<String?>(null) }

    // Clean up audio resources when onboarding is disposed
    DisposableEffect(Unit) {
        onDispose {
            if (beaconHandle != 0L) {
                audioEngine.destroyBeacon(beaconHandle)
            }
            audioEngine.clearTextToSpeechQueue()
        }
    }

    NavHost(
        navController = navController,
        startDestination = OnboardingRoutes.WELCOME,
    ) {
        composable(OnboardingRoutes.WELCOME) {
            Welcome(
                onNavigate = { navController.navigate(OnboardingRoutes.LISTENING) },
            )
        }

        composable(OnboardingRoutes.LISTENING) {
            Listening(
                onNavigate = { navController.navigate(OnboardingRoutes.HEARING) },
            )
        }

        composable(OnboardingRoutes.HEARING) {
            val example1 = stringResource(Res.string.first_launch_callouts_example_1)
            val example3 = stringResource(Res.string.first_launch_callouts_example_3)
            val example4 = stringResource(Res.string.first_launch_callouts_example_4)
            val speechText = remember(example1, example3, example4) {
                "$example1. $example3. $example4"
            }

            Hearing(
                onContinue = {
                    audioEngine.clearTextToSpeechQueue()
                    navController.navigate(OnboardingRoutes.AUDIO_BEACONS)
                },
                onPlaySpeech = {
                    audioEngine.clearTextToSpeechQueue()
                    audioEngine.createTextToSpeech(speechText, AudioType.LOCALIZED)
                },
            )
        }

        composable(OnboardingRoutes.AUDIO_BEACONS) {
            AudioBeacons(
                beacons = beaconTypes,
                selectedBeacon = currentBeaconType,
                onBeaconSelected = { beacon ->
                    if (beacon != currentBeaconType) {
                        audioEngine.setBeaconType(beacon)
                        if (beaconHandle != 0L) {
                            audioEngine.destroyBeacon(beaconHandle)
                        }
                        beaconHandle = audioEngine.createBeacon(LngLatAlt(1.0, 0.0), true)
                        currentBeaconType = beacon
                    }
                    preferencesProvider.putString(PreferenceKeys.BEACON_TYPE, beacon)
                },
                onContinue = {
                    if (beaconHandle != 0L) {
                        audioEngine.destroyBeacon(beaconHandle)
                        beaconHandle = 0
                    }
                    navController.navigate(OnboardingRoutes.ACCESSIBILITY)
                },
            )
        }

        composable(OnboardingRoutes.ACCESSIBILITY) {
            AccessibilityOnboardingScreen(
                isScreenReaderActive = isScreenReaderEnabled(),
                onNavigate = { navController.navigate(OnboardingRoutes.TERMS) },
            )
        }

        composable(OnboardingRoutes.TERMS) {
            TermsScreen(
                onNavigate = { navController.navigate(OnboardingRoutes.FINISH) },
            )
        }

        composable(OnboardingRoutes.FINISH) {
            FinishScreen(
                onFinish = onFinish,
            )
        }
    }
}
