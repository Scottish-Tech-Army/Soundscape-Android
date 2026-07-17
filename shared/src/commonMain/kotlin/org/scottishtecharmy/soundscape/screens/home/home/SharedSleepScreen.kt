package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.sleep_sleeping
import org.scottishtecharmy.soundscape.resources.sleep_sleeping_message
import org.scottishtecharmy.soundscape.resources.sleep_sleeping_wake_on_leave_message
import org.scottishtecharmy.soundscape.resources.sleep_wake_on_leave
import org.scottishtecharmy.soundscape.resources.sleep_wake_up_now
import org.scottishtecharmy.soundscape.ui.theme.currentAppButtonColors
import org.scottishtecharmy.soundscape.ui.theme.largePadding
import org.scottishtecharmy.soundscape.ui.theme.spacing

sealed class SleepScreenState {
    object Sleeping : SleepScreenState()
    data class Snoozing(val userLocation: LngLatAlt? = LngLatAlt()) : SleepScreenState()
}

interface ISleepScreenViewModel {
    val state: StateFlow<SleepScreenState>
    fun onWakeOnLeaveClicked()
}

expect fun provideSleepScreenViewModel(): ISleepScreenViewModel

@Composable
fun SharedSleepScreen(
    onWakeUp: () -> Unit,
    onExit: () -> Unit,
    onWakeOnLeaveClicked: () -> Unit,
    state: SleepScreenState,
    modifier: Modifier = Modifier,
) {
    DisposableEffect(Unit) {
        onDispose { onWakeUp() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row {
            Text(
                text = stringResource(Res.string.sleep_sleeping),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.largePadding(),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Row {
            Text(
                text = stringResource(
                    when (state) {
                        SleepScreenState.Sleeping -> Res.string.sleep_sleeping_message
                        is SleepScreenState.Snoozing -> Res.string.sleep_sleeping_wake_on_leave_message
                    }
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.largePadding(),
            )
        }
        WakeButtons(
            wakeUpNowOnClick = onExit,
            wakeOnLeaveOnClick = onWakeOnLeaveClicked,
            sleepScreenState = state,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun WakeButtons(
    wakeUpNowOnClick: () -> Unit,
    wakeOnLeaveOnClick: () -> Unit,
    sleepScreenState: SleepScreenState,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        WakeButton(
            text = stringResource(Res.string.sleep_wake_up_now),
            onClick = wakeUpNowOnClick,
            modifier = Modifier.fillMaxWidth(
                when (sleepScreenState) {
                    SleepScreenState.Sleeping -> 0.5f
                    is SleepScreenState.Snoozing -> 1.0f
                }
            )
        )
        if (sleepScreenState is SleepScreenState.Sleeping) {
            WakeButton(
                text = stringResource(Res.string.sleep_wake_on_leave),
                onClick = wakeOnLeaveOnClick,
                modifier = Modifier,
            )
        }
    }
}

@Composable
fun WakeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(spacing.targetSize * 4)
            .testTag("sleepWakeUpNow"),
        shape = RoundedCornerShape(spacing.tiny),
        colors = if (!LocalInspectionMode.current) currentAppButtonColors else ButtonDefaults.buttonColors(),
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.displaySmall,
        )
    }
}
