package org.scottishtecharmy.soundscape.screens.onboarding.audiobeacons

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.scottishtecharmy.soundscape.screens.onboarding.AudioOnboardingViewModel

// AudioBeacons, AudioBeaconItem, and getBeaconResourceId are now in shared module

@Composable
fun AudioBeaconsScreen(
    onBack: () -> Unit,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AudioOnboardingViewModel
) {
    val uiState: AudioOnboardingViewModel.AudioBeaconsUiState by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(uiState) {
        uiState.selectedBeacon?.let { beacon ->
            viewModel.setAudioBeaconType(beacon)
        }
    }
    BackHandler(enabled = true) {
        viewModel.silenceBeacon()
        onBack()
    }

    AudioBeacons(
        beacons = uiState.beaconTypes,
        selectedBeacon = uiState.selectedBeacon,
        onContinue = {
            viewModel.silenceBeacon()
            onNavigate()
        },
        onBeaconSelected = { beacon ->
            viewModel.setAudioBeaconType(beacon)
            Log.d("AudioBeacon", "Audio beacon category changed to $beacon")
        },
        modifier = modifier
    )
}

data object MockHearingPreviewData {
    val names = listOf(
        "Original", "Current", "Tactile", "Flare", "Shimmer", "Ping", "Drop",
        "Signal", "Signal Slow", "Signal Very Slow", "Mallet", "Mallet Slow", "Mallet Very Slow"
    )
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Preview
@Composable
fun AudioBeaconPreview() {
    AudioBeacons(
        beacons = MockHearingPreviewData.names,
        selectedBeacon = MockHearingPreviewData.names[0],
        onBeaconSelected = {},
        onContinue = {},
    )
}
