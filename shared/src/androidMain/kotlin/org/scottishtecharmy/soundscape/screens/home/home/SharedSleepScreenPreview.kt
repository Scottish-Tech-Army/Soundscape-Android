package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview(showBackground = true)
@Composable
fun SharedSleepScreenPreview() {
    SharedSleepScreen(
        onWakeUp = {},
        onWakeUpNowClicked = {},
        onWakeOnLeaveClicked = { },
        state = SleepScreenState.Sleeping,
    )
}

@Preview(showBackground = true)
@Composable
fun SharedSleepScreenWakeOnLeaveEnabledPreview() {
    SharedSleepScreen(
        onWakeUp = {},
        onWakeUpNowClicked = {},
        onWakeOnLeaveClicked = { },
        state = SleepScreenState.Snoozing()
    )
}

@Preview(showBackground = true)
@Composable
fun WakeButtonPreview() {
    WakeButton(
        text = "Wake On Leave",
        onClick = { },
    )
}

@Preview(showBackground = true)
@Composable
fun WakeButtonsWakeOnLeaveVisiblePreview() {
    WakeButtons(
        wakeUpNowOnClick = {},
        wakeOnLeaveOnClick = {},
        sleepScreenState = SleepScreenState.Sleeping,
    )
}

@Preview(showBackground = true)
@Composable
fun WakeButtonsWakeOnLeaveNotVisiblePreview() {
    WakeButtons(
        wakeUpNowOnClick = {},
        wakeOnLeaveOnClick = {},
        sleepScreenState = SleepScreenState.Snoozing(),
    )
}