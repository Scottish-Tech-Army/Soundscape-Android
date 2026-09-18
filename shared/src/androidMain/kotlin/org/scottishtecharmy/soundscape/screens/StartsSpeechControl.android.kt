package org.scottishtecharmy.soundscape.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
actual fun StartsSpeechControl(
    label: String,
    hint: String,
    identifier: String?,
    onActivate: () -> Unit,
    modifier: Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    // TalkBack yields to the app's own speech, and the wrapped control keeps its semantics
    // and testTag, so there is nothing platform-specific to do here. The Box is kept so the
    // layout tree matches iOS - a regression in the caller's sizing then shows up in Android
    // previews and Maestro rather than only on a device.
    // Centred: the wrapper takes the caller's sizing, so a control shorter than its
    // touch target would otherwise be top-aligned inside it.
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        content(Modifier.fillMaxSize())
    }
}
