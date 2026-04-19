package org.scottishtecharmy.soundscape

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import org.scottishtecharmy.soundscape.screens.onboarding.welcome.Welcome
import org.scottishtecharmy.soundscape.ui.theme.LocalAppButtonColors
import org.scottishtecharmy.soundscape.ui.theme.defaultAppButtonColors

@Composable
fun App() {
    MaterialTheme {
        val buttonColors = defaultAppButtonColors(MaterialTheme.colorScheme)
        CompositionLocalProvider(LocalAppButtonColors provides buttonColors) {
            Welcome(onNavigate = {})
        }
    }
}
