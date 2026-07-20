package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

@Preview(showBackground = true)
@Composable
fun SharedSleepScreenPreview() {
    SharedSleepScreen(
        onWakeUpNowClicked = {},
        onWakeOnLeaveClicked = { },
        state = SleepScreenState.Sleeping,
    )
}

@Preview(showBackground = true)
@Composable
fun SharedSleepScreenWakeOnLeaveEnabledPreview() {
    SharedSleepScreen(
        onWakeUpNowClicked = {},
        onWakeOnLeaveClicked = {},
        state = SleepScreenState.Snoozing(
            userLocation = LngLatAlt(),
            startLocation = LngLatAlt()
        )
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
        sleepScreenState = SleepScreenState.Snoozing(
            userLocation = LngLatAlt(),
            startLocation = LngLatAlt()
        ),
    )
}