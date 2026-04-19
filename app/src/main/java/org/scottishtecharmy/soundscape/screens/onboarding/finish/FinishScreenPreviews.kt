package org.scottishtecharmy.soundscape.screens.onboarding.finish

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

// FinishScreen composable is now in shared module

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Preview
@Composable
fun PreviewFinishScreen() {
    FinishScreen(onFinish = {})
}
