package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview(showBackground = true)
@Composable
fun SharedSleepScreenPreview() {
    SharedSleepScreen(
        onWakeUp = {},
        onExit = {},
        onWakeOnLeaveClicked = { },
        wakeOnLeaveEnabled = false,
    )
}

@Preview(showBackground = true)
@Composable
fun SharedSleepScreenWakeOnLeaveEnabledPreview() {
    SharedSleepScreen(
        onWakeUp = {},
        onExit = {},
        onWakeOnLeaveClicked = { },
        wakeOnLeaveEnabled = true,
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
        showWakeOnLeave = true,
    )
}

@Preview(showBackground = true)
@Composable
fun WakeButtonsWakeOnLeaveNotVisiblePreview() {
    WakeButtons(
        wakeUpNowOnClick = {},
        wakeOnLeaveOnClick = {},
        showWakeOnLeave = false,
    )
}