package org.scottishtecharmy.soundscape

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow
import org.scottishtecharmy.soundscape.locationprovider.DeviceDirection
import org.scottishtecharmy.soundscape.locationprovider.SoundscapeLocation
import org.scottishtecharmy.soundscape.screens.onboarding.welcome.Welcome
import org.scottishtecharmy.soundscape.ui.theme.LocalAppButtonColors
import org.scottishtecharmy.soundscape.ui.theme.defaultAppButtonColors

@Composable
fun App(
    locationFlow: StateFlow<SoundscapeLocation?>? = null,
    directionFlow: StateFlow<DeviceDirection?>? = null,
) {
    MaterialTheme {
        val buttonColors = defaultAppButtonColors(MaterialTheme.colorScheme)
        CompositionLocalProvider(LocalAppButtonColors provides buttonColors) {
            var onboarded by remember { mutableStateOf(false) }

            if (!onboarded) {
                Welcome(onNavigate = { onboarded = true })
            } else {
                LocationScreen(locationFlow, directionFlow)
            }
        }
    }
}

@Composable
private fun LocationScreen(
    locationFlow: StateFlow<SoundscapeLocation?>?,
    directionFlow: StateFlow<DeviceDirection?>?,
) {
    val location by locationFlow?.collectAsState() ?: remember { mutableStateOf(null) }
    val direction by directionFlow?.collectAsState() ?: remember { mutableStateOf(null) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Soundscape",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (location != null) {
                val loc = location!!
                val latStr = ((loc.latitude * 100000).toLong() / 100000.0).toString()
                val lonStr = ((loc.longitude * 100000).toLong() / 100000.0).toString()
                Text(
                    text = "$latStr, $lonStr",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 16.dp)
                )
                if (loc.hasAccuracy) {
                    Text(
                        text = "Accuracy: ${loc.accuracy.toInt()} m",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            } else {
                Text(
                    text = "Waiting for location...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            if (direction != null) {
                Text(
                    text = "Heading: ${direction!!.headingDegrees.toInt()}\u00B0",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
