package org.scottishtecharmy.soundscape.screens.onboarding.listening

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

// Listening composable is now in shared module

@Composable
fun ListeningScreen(
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Listening(
        modifier = modifier,
        onNavigate = onNavigate
    )
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Preview
@Composable
fun ListeningPreview() {
    Listening(onNavigate = {})
}
