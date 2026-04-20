package org.scottishtecharmy.soundscape

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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

enum class AppScreen {
    WELCOME,
    HOME,
}

@Composable
fun App(
    locationFlow: StateFlow<SoundscapeLocation?>? = null,
    directionFlow: StateFlow<DeviceDirection?>? = null,
    onStartBeacon: ((Double, Double, String) -> Unit)? = null,
    onStopBeacon: (() -> Unit)? = null,
    onSpeak: ((String) -> Unit)? = null,
) {
    MaterialTheme {
        val buttonColors = defaultAppButtonColors(MaterialTheme.colorScheme)
        CompositionLocalProvider(LocalAppButtonColors provides buttonColors) {
            var currentScreen by remember { mutableStateOf(AppScreen.WELCOME) }

            when (currentScreen) {
                AppScreen.WELCOME -> {
                    Welcome(onNavigate = { currentScreen = AppScreen.HOME })
                }
                AppScreen.HOME -> {
                    HomeScreen(
                        locationFlow = locationFlow,
                        directionFlow = directionFlow,
                        onStartBeacon = onStartBeacon,
                        onStopBeacon = onStopBeacon,
                        onSpeak = onSpeak,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    locationFlow: StateFlow<SoundscapeLocation?>?,
    directionFlow: StateFlow<DeviceDirection?>?,
    onStartBeacon: ((Double, Double, String) -> Unit)?,
    onStopBeacon: (() -> Unit)?,
    onSpeak: ((String) -> Unit)?,
) {
    val location by locationFlow?.collectAsState() ?: remember { mutableStateOf(null) }
    val direction by directionFlow?.collectAsState() ?: remember { mutableStateOf(null) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                )
            }

            if (direction != null) {
                Text(
                    text = "Heading: ${direction!!.headingDegrees.toInt()}\u00B0",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                if (onSpeak != null) {
                    Button(onClick = { onSpeak("Hello from Soundscape") }) {
                        Text("Speak")
                    }
                }

                if (onStartBeacon != null && location != null) {
                    Button(onClick = {
                        val loc = location!!
                        onStartBeacon(loc.latitude, loc.longitude, "Test Beacon")
                    }) {
                        Text("Start Beacon")
                    }
                }

                if (onStopBeacon != null) {
                    Button(onClick = { onStopBeacon() }) {
                        Text("Stop Beacon")
                    }
                }
            }
        }
    }
}
