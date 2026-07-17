package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview(showBackground = true)
@Composable
fun SharedSleepScreenPreview() {
    SharedSleepScreen(
        onWakeUp = {},
        onExit = {}
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