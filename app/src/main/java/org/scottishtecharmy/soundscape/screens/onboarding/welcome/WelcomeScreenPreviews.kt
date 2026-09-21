package org.scottishtecharmy.soundscape.screens.onboarding.welcome

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

// Re-export from shared module
// The Welcome composable is now in shared/src/commonMain

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Preview
@Preview(fontScale = 2.0f)
@Composable
fun PreviewWelcome() {
    Welcome(onNavigate = {})
}
