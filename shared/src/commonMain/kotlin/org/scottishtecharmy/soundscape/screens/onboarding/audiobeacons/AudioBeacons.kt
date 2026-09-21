package org.scottishtecharmy.soundscape.screens.onboarding.audiobeacons

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.components.OnboardButton
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.beacon_styles_current
import org.scottishtecharmy.soundscape.resources.beacon_styles_drop
import org.scottishtecharmy.soundscape.resources.beacon_styles_flare
import org.scottishtecharmy.soundscape.resources.beacon_styles_mallet
import org.scottishtecharmy.soundscape.resources.beacon_styles_mallet_slow
import org.scottishtecharmy.soundscape.resources.beacon_styles_mallet_very_slow
import org.scottishtecharmy.soundscape.resources.beacon_styles_original
import org.scottishtecharmy.soundscape.resources.beacon_styles_ping
import org.scottishtecharmy.soundscape.resources.beacon_styles_shimmer
import org.scottishtecharmy.soundscape.resources.beacon_styles_signal
import org.scottishtecharmy.soundscape.resources.beacon_styles_signal_slow
import org.scottishtecharmy.soundscape.resources.beacon_styles_signal_very_slow
import org.scottishtecharmy.soundscape.resources.beacon_styles_tactile
import org.scottishtecharmy.soundscape.resources.first_launch_beacon_message_1
import org.scottishtecharmy.soundscape.resources.first_launch_beacon_message_2
import org.scottishtecharmy.soundscape.resources.first_launch_beacon_message_3
import org.scottishtecharmy.soundscape.resources.first_launch_beacon_title
import org.scottishtecharmy.soundscape.resources.ui_continue
import org.scottishtecharmy.soundscape.screens.onboarding.component.BoxWithGradientBackground
import org.scottishtecharmy.soundscape.ui.theme.spacing

fun getBeaconResourceId(beaconName: String): StringResource {
    when (beaconName) {
        "Original" -> return Res.string.beacon_styles_original
        "Current" -> return Res.string.beacon_styles_current
        "Tactile" -> return Res.string.beacon_styles_tactile
        "Flare" -> return Res.string.beacon_styles_flare
        "Shimmer" -> return Res.string.beacon_styles_shimmer
        "Ping" -> return Res.string.beacon_styles_ping
        "Drop" -> return Res.string.beacon_styles_drop
        "Signal" -> return Res.string.beacon_styles_signal
        "Signal Slow" -> return Res.string.beacon_styles_signal_slow
        "Signal Very Slow" -> return Res.string.beacon_styles_signal_very_slow
        "Mallet" -> return Res.string.beacon_styles_mallet
        "Mallet Slow" -> return Res.string.beacon_styles_mallet_slow
        "Mallet Very Slow" -> return Res.string.beacon_styles_mallet_very_slow
        else -> throw IllegalArgumentException("Unknown beacon name: $beaconName")
    }
}

@Composable
fun AudioBeacons(
    beacons: List<String>,
    onBeaconSelected: (String) -> Unit,
    selectedBeacon: String?,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }

    BoxWithGradientBackground(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface
    ) {
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
            Text(
                text = stringResource(Res.string.first_launch_beacon_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
                    .focusRequester(focusRequester).focusable()
            )
            Spacer(modifier = Modifier.height(spacing.large))
            Text(
                text = stringResource(Res.string.first_launch_beacon_message_1),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.focusable()
            )
            Spacer(modifier = Modifier.height(spacing.large))
            Text(
                text = stringResource(Res.string.first_launch_beacon_message_2),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.focusable(),
            )
            Spacer(modifier = Modifier.height(spacing.large))
            Text(
                text = stringResource(Res.string.first_launch_beacon_message_3),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.focusable()
            )
            Spacer(modifier = Modifier.height(spacing.large))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(spacing.extraSmall))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .semantics {
                        collectionInfo = CollectionInfo(rowCount = beacons.size, columnCount = 1)
                    }
            ) {
                beacons.forEachIndexed { index, beacon ->
                    AudioBeaconItem(
                        text = stringResource(getBeaconResourceId(beacon)),
                        foregroundColor = MaterialTheme.colorScheme.onSurface,
                        isSelected = beacon == selectedBeacon,
                        onSelect = {
                            onBeaconSelected(beacon)
                        },
                        modifier = Modifier
                            .testTag("${beacon}Button")
                            .focusable()
                            .semantics {
                                collectionItemInfo = CollectionItemInfo(
                                    rowIndex = index,
                                    rowSpan = 1,
                                    columnIndex = 0,
                                    columnSpan = 1
                                )
                            },
                    )
                }
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = spacing.medium, vertical = spacing.extraLarge)
                    .requiredHeight(spacing.targetSize)
            ) {
                OnboardButton(
                    text = stringResource(Res.string.ui_continue),
                    onClick = { onContinue() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusable()
                        .testTag("audioBeaconsContinueButton"),
                    enabled = selectedBeacon != null,
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        focusRequester.requestFocus()
    }
}
