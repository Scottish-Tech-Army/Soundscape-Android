package org.scottishtecharmy.soundscape.screens.onboarding.audiobeacons

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.components.OnboardButton
import org.scottishtecharmy.soundscape.screens.onboarding.component.BoxWithGradientBackground
import org.scottishtecharmy.soundscape.ui.theme.spacing

fun getBeaconResourceId(beaconName: String) : Int {
    when (beaconName) {
        "Original" -> return R.string.beacon_styles_original
        "Current" -> return R.string.beacon_styles_current
        "Tactile" -> return R.string.beacon_styles_tactile
        "Flare" -> return R.string.beacon_styles_flare
        "Shimmer" -> return R.string.beacon_styles_shimmer
        "Ping" -> return R.string.beacon_styles_ping
        "Drop" -> return R.string.beacon_styles_drop
        "Signal" -> return R.string.beacon_styles_signal
        "Signal Slow" -> return R.string.beacon_styles_signal_slow
        "Signal Very Slow" -> return R.string.beacon_styles_signal_very_slow
        "Mallet" -> return R.string.beacon_styles_mallet
        "Mallet Slow" -> return R.string.beacon_styles_mallet_slow
        "Mallet Very Slow" -> return R.string.beacon_styles_mallet_very_slow
        else -> assert(false)
    }
    return 0
}

@Composable
fun AudioBeaconsScreen(
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AudioBeaconsViewModel = hiltViewModel()
) {
    val uiState: AudioBeaconsViewModel.AudioBeaconsUiState by viewModel.state.collectAsStateWithLifecycle()

    AudioBeacons(
        beacons = uiState.beaconTypes,
        selectedBeacon = uiState.selectedBeacon,
        onContinue = {
            viewModel.silenceBeacon()
            onNavigate()
        },
        onBeaconSelected = { beacon ->
            // change the audio beacon
            viewModel.setAudioBeaconType(beacon)
            Log.d(
                "AudioBeacon",
                "Audio beacon category changed to $beacon")

        },
        modifier = modifier
    )
}

@Composable
fun AudioBeacons(
    beacons: List<String>,
    onBeaconSelected: (String) -> Unit,
    selectedBeacon: String?,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithGradientBackground(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface
    ){
        Column(
            modifier = Modifier
                .padding(horizontal = spacing.large)
                .padding(top = spacing.large)
                .fillMaxWidth()
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // TODO translations
            Text(
                text = stringResource(R.string.first_launch_beacon_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics {
                    heading()
                }
            )
            Spacer(modifier = Modifier.height(spacing.large))
            Text(
                text = stringResource(R.string.first_launch_beacon_message_1),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(spacing.large))
            Text(
                text = stringResource(R.string.first_launch_beacon_message_2),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(spacing.large))
            Text(
                text = stringResource(R.string.first_launch_beacon_message_3),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(spacing.large))

            LazyColumn(
                modifier = Modifier
                    .clip(RoundedCornerShape(spacing.extraSmall))
                    .fillMaxWidth()
                    .heightIn(spacing.extraLarge, spacing.extraLarge * 5)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
                items(beacons) { beacon ->
                    AudioBeaconItem(
                        text = stringResource(getBeaconResourceId(beacon)),
                        foregroundColor = MaterialTheme.colorScheme.onSurface,
                        isSelected = beacon == selectedBeacon,
                        onSelect = {
                            onBeaconSelected(beacon)
                        }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = spacing.medium, vertical = spacing.extraLarge)
                    .requiredHeight(spacing.targetSize)
            ) {
                OnboardButton(
                    text = stringResource(R.string.ui_continue),
                    onClick = {
                        onContinue()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedBeacon != null,
                )
            }

        }
    }
}

// Data used by preview
data object MockHearingPreviewData {
    val names = listOf(
        "Original",
        "Current",
        "Tactile",
        "Flare",
        "Shimmer",
        "Ping",
        "Drop",
        "Signal",
        "Signal Slow",
        "Signal Very Slow",
        "Mallet",
        "Mallet Slow",
        "Mallet Very Slow"
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