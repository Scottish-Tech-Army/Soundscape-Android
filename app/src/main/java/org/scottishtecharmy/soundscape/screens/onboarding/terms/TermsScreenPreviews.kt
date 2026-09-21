package org.scottishtecharmy.soundscape.screens.onboarding.terms

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

// TermsScreen and TermsItem composables are now in shared module

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Preview
@Composable
fun TermsPreview() {
    TermsScreen(onNavigate = {})
}
