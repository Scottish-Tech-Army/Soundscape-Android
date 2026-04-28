package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Preview(showBackground = true)
@Composable
fun SleepScreenPreview() {
    SharedSleepScreen(
        onWakeUp = {},
        onExit = {},
        modifier = Modifier,
    )
}
