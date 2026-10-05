package org.scottishtecharmy.soundscape.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties

actual fun fullScreenDialogProperties(): DialogProperties =
    DialogProperties(usePlatformDefaultWidth = false)

@Composable
actual fun Modifier.fullScreenDialogSystemBarsPadding(): Modifier = this

@Composable
actual fun FullScreenDialogWindow() {
}
