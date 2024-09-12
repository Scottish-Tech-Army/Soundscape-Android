package org.scottishtecharmy.soundscape.screens.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.switchPreference
import org.scottishtecharmy.soundscape.R

// This code uses the library https://github.com/zhanghai/ComposePreference
// The UI changes the SharedPreference reference by the `key` which can then be accessed
// anywhere else in the app.

@Composable
fun Settings(onNavigate: (String) -> Unit) {

    Column(
        modifier = Modifier
            .fillMaxHeight(),
    ) {
        IconButton(
            onClick = {
                onNavigate(HomeScreens.Home.route)
            },
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                modifier = Modifier
                    .size(32.dp)
                    .padding(start = 4.dp),
                contentDescription = stringResource(R.string.ui_menu_close),
                tint = Color.White
            )
        }

        Text(
            text = "MANAGE CALLOUTS",
            textAlign = TextAlign.Left,
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        )
        ProvidePreferenceLocals {
            LazyColumn(modifier = Modifier.fillMaxSize()) {

                // TODO : Add strings translations and use them

                switchPreference(
                    key = "allow_callouts",
                    defaultValue = true,
                    title = { Text(text = "Allow Callouts") },
                    summary = { Text(text = if (it) "On" else "Off") }
                )
                switchPreference(
                    key = "places_and_landmarks",
                    defaultValue = true,
                    title = { Text(text = "Places and Landmarks") },
                    summary = { Text(text = if (it) "On" else "Off") }
                )
                switchPreference(
                    key = "mobility",
                    defaultValue = true,
                    title = { Text(text = "Mobility") },
                    summary = { Text(text = if (it) "On" else "Off") }
                )
                switchPreference(
                    key = "distance_to_beacon",
                    defaultValue = true,
                    title = { Text(text = "Distance to the Audio Beacon") },
                    summary = { Text(text = if (it) "On" else "Off") }
                )
            }
        }
    }
}