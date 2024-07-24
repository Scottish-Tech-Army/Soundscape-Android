package org.scottishtecharmy.soundscape

import android.location.Location
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme


@Composable
internal fun ForegroundServiceScreen(
    serviceRunning: Boolean,
    currentLocation: String?,
    currentOrientation: String?,
    tileString: String?,
    location: Location?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    SoundscapeTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            ForegroundServiceSampleScreenContent(
                serviceRunning = serviceRunning,
                currentLocation = currentLocation,
                currentOrientation = currentOrientation,
                location = location,
                onClick = onClick,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun ForegroundServiceSampleScreenContent(
    serviceRunning: Boolean,
    currentLocation: String?,
    currentOrientation: String?,
    location: Location?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                ServiceStatusContent(
                    serviceRunning = serviceRunning,
                    onClick = onClick
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LocationUpdate(
                    visible = serviceRunning,
                    location = currentLocation,
                    orientation = currentOrientation
                )
            }
        }
    }
}

@Composable
private fun ServiceStatusContent(
    serviceRunning: Boolean,
    onClick: () -> Unit
) {
    ServiceStatusRow(serviceRunning = serviceRunning)

    Spacer(modifier = Modifier.height(16.dp))

    Button(onClick = onClick) {
        Text(
            text = stringResource(
                id = if (serviceRunning) {
                    R.string.foreground_service_sample_button_stop
                } else {
                    R.string.foreground_service_sample_button_start
                }
            )
        )
    }
}

@Composable
private fun ServiceStatusRow(
    serviceRunning: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(id = R.string.foreground_service_sample_status_title),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = if (serviceRunning) {
                stringResource(id = R.string.foreground_service_sample_status_running)
            } else {
                stringResource(id = R.string.foreground_service_sample_status_not_running)
            },
            color = if (serviceRunning) {
                Color.Green
            } else {
                Color.Red
            },
        )
    }
}

@Composable
private fun LocationUpdate(
    visible: Boolean,
    location: String?,
    orientation: String?
) {
    if (visible) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(id = R.string.foreground_service_sample_last_location_title),
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = location
                ?: stringResource(id = R.string.foreground_service_sample_last_location_fetching)
        )
        Text(
            text = orientation
                ?: stringResource(id = R.string.foreground_service_sample_last_location_fetching)
        )

    }
}