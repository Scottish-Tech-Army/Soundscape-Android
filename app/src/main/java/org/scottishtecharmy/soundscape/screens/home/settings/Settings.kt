package org.scottishtecharmy.soundscape.screens.home.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.listPreference
import me.zhanghai.compose.preference.sliderPreference
import me.zhanghai.compose.preference.switchPreference
import org.scottishtecharmy.soundscape.MainActivity
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomAppBar
import org.scottishtecharmy.soundscape.viewmodels.SettingsViewModel

// This code uses the library https://github.com/zhanghai/ComposePreference
// The UI changes the SharedPreference reference by the `key` which can then be accessed
// anywhere else in the app.

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Preview
@Composable
fun SettingsPreview() {
    Settings({}, SettingsViewModel.SettingsUiState())
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Settings(
    onNavigateUp: () -> Unit,
    uiState: SettingsViewModel.SettingsUiState,
    modifier: Modifier = Modifier,
)
{
    val beaconTypes = uiState.beaconTypes.map { stringResource(it) }
    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground

    ProvidePreferenceLocals {
        LazyColumn (modifier = modifier.background(backgroundColor)){
            stickyHeader {
                Surface {
                    CustomAppBar(stringResource(R.string.general_alert_settings),
                        onNavigateUp = onNavigateUp,
                        navigationButtonTitle = "Back"
                    )
                }
            }

            item {
                Text(
                    text = stringResource(R.string.menu_manage_callouts),
                    color = textColor,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() },
                )
            }
            switchPreference(
                key = MainActivity.ALLOW_CALLOUTS_KEY,
                defaultValue = MainActivity.ALLOW_CALLOUTS_DEFAULT,
                title = {
                    Text(
                        text = stringResource(R.string.callouts_allow_callouts),
                        color = textColor
                    )
                },
            )
            switchPreference(
                key = MainActivity.PLACES_AND_LANDMARKS_KEY,
                defaultValue = MainActivity.PLACES_AND_LANDMARKS_DEFAULT,
                title = {
                    Text(
                        text = stringResource(R.string.callouts_places_and_landmarks),
                        color = textColor
                    )
                },
            )
            switchPreference(
                key = MainActivity.MOBILITY_KEY,
                defaultValue = MainActivity.MOBILITY_DEFAULT,
                title = {
                    Text(text = stringResource(R.string.callouts_mobility),
                    color = textColor
                    )
                },
            )
            switchPreference(
                key = MainActivity.DISTANCE_TO_BEACON_KEY,
                defaultValue = MainActivity.DISTANCE_TO_BEACON_DEFAULT,
                title = {
                    Text(
                        text = stringResource(R.string.callouts_audio_beacon),
                        color = textColor
                    )
                },
            )
            switchPreference(
                key = MainActivity.UNNAMED_ROADS_KEY,
                defaultValue = MainActivity.UNNAMED_ROADS_DEFAULT,
                title = {
                    Text(
                        text = stringResource(R.string.preview_include_unnamed_roads_title),
                        color = textColor
                    )
                },
            )

            item {
                Text(
                    text = stringResource(R.string.menu_manage_audio),
                    style = MaterialTheme.typography.headlineSmall,
                    color = textColor,
                    modifier = Modifier.semantics { heading() },
                )
            }
            listPreference(
                key = MainActivity.BEACON_TYPE_KEY,
                defaultValue = MainActivity.BEACON_TYPE_DEFAULT,
                values = beaconTypes,
                title = {
                    Text(
                        text = stringResource(R.string.beacon_settings_style),
                        color = textColor
                    )
                },
                summary = { Text(text = it, color = textColor) },
            )

            listPreference(
                key = MainActivity.VOICE_TYPE_KEY,
                defaultValue = MainActivity.VOICE_TYPE_DEFAULT,
                values = uiState.voiceTypes,
                title = {
                    Text(
                        text = stringResource(R.string.voice_voices),
                        color = textColor
                    )
                },
                summary = { Text(text = it, color = textColor) },
            )

            sliderPreference(
                key = MainActivity.SPEECH_RATE_KEY,
                defaultValue = MainActivity.SPEECH_RATE_DEFAULT,
                title = {
                    Text(
                        text = stringResource(R.string.voice_settings_speaking_rate),
                        color = textColor
                    )
                },
                valueRange = 0.5f..2.0f,
                valueSteps = 10,
                valueText = { Text(text = "%.1fx".format(it), color = textColor) },
            )
        }
    }
}