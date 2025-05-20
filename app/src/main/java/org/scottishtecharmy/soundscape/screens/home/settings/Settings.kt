package org.scottishtecharmy.soundscape.screens.home.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
import org.scottishtecharmy.soundscape.screens.talkbackDescription
import org.scottishtecharmy.soundscape.screens.talkbackHint
import org.scottishtecharmy.soundscape.ui.theme.smallPadding
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

/**
 * ListPreferenceItem is an attempt to make a more accessible list entry for the user.
 */
@Composable
fun ListPreferenceItem(value: String,
                       currentValue: String,
                       onClick: () -> Unit,
                       index: Int,
                       listSize: Int) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .smallPadding()
            .clickable {
                onClick()
            }
            .talkbackHint(
                if (value == currentValue) "keep"
                else "use"
            )
            .talkbackDescription("$value is ${index + 1} choice out of $listSize"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Icon(
            modifier = Modifier
                .align(Alignment.CenterVertically),
            imageVector =
                if(value == currentValue) Icons.Filled.CheckBox
                else Icons.Filled.CheckBoxOutlineBlank,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            contentDescription = ""
        )
    }
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
                    text = stringResource(R.string.menu_manage_accessibility),
                    style = MaterialTheme.typography.headlineSmall,
                    color = textColor,
                    modifier = Modifier.semantics { heading() },
                )
            }
            switchPreference(
                key = MainActivity.THEME_IS_LIGHT_KEY,
                defaultValue = MainActivity.THEME_IS_LIGHT_DEFAULT,
                title = {
                    Text(
                        text = stringResource(R.string.settings_theme_is_light),
                        color = textColor
                    )
                },
            )
            val themeValues = listOf("Regular", "Medium", "High")
            listPreference(
                key = MainActivity.THEME_CONTRAST_KEY,
                defaultValue = MainActivity.THEME_CONTRAST_DEFAULT,
                values = themeValues,
                title = {
                    Text(
                        text = stringResource(R.string.settings_theme_contrast),
                        color = textColor
                    )
                },
                item = { value, currentValue, onClick ->
                    ListPreferenceItem(value, currentValue, onClick, themeValues.indexOf(value), themeValues.size)
                },
                summary = { Text(text = it, color = textColor) },
            )

//          Disabling hints just results in the Android default "Double tap to Activate" being read
//          out instead. Our hints are better, so don't allow disabling them.
//            switchPreference(
//                key = MainActivity.HINTS_KEY,
//                defaultValue = MainActivity.HINTS_DEFAULT,
//                title = {
//                    Text(
//                        text = stringResource(R.string.settings_hints),
//                        color = textColor
//                    )
//                },
//            )

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
                item = { value, currentValue, onClick ->
                    ListPreferenceItem(value, currentValue, onClick, beaconTypes.indexOf(value), beaconTypes.size)
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
                item = { value, currentValue, onClick ->
                    ListPreferenceItem(value, currentValue, onClick, uiState.voiceTypes.indexOf(value), uiState.voiceTypes.size)
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

            item {
                Text(
                    text = stringResource(R.string.settings_debug_heading),
                    style = MaterialTheme.typography.headlineSmall,
                    color = textColor,
                    modifier = Modifier.semantics { heading() },
                    )
            }
            switchPreference(
                key = MainActivity.RECORD_TRAVEL_KEY,
                defaultValue = MainActivity.RECORD_TRAVEL_DEFAULT,
                title = {
                    Text(
                        text = stringResource(R.string.settings_travel_recording),
                        color = textColor
                    )
                },
            )
        }
    }
}