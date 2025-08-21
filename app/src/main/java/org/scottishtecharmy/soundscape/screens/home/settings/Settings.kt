package org.scottishtecharmy.soundscape.screens.home.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
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
import org.scottishtecharmy.soundscape.ui.theme.spacing
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

@Preview(fontScale = 2f)
@Composable
fun ListItemPreview() {
    ListPreferenceItem(
        "Speech synthesis and recognition by Google",
        value = 2,
        currentValue = 2,
        { },
        2,
        3
    )
}

/**
 * ListPreferenceItem is an attempt to make a more accessible list entry for the user.
 */
@Composable
fun ListPreferenceItem(description: String,
                       value: Any,
                       currentValue: Any,
                       onClick: () -> Unit,
                       index: Int,
                       listSize: Int) {

    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .smallPadding()
            .clickable {
                onClick()
            }
            .talkbackHint(
                if (value == currentValue) stringResource(R.string.settings_keep_value)
                else stringResource(R.string.settings_use_value)
            )
            .talkbackDescription(
                stringResource(R.string.settings_list_item_description).format(
                    value,
                    index + 1,
                    listSize
                )
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = description,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .weight(1f)
        )
        Icon(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .width(spacing.icon),
            imageVector =
                if(value == currentValue) Icons.Filled.CheckBox
                else Icons.Filled.CheckBoxOutlineBlank,
            tint = MaterialTheme.colorScheme.onSurface,
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
    val themeContrastDescriptions = listOf(
        stringResource(R.string.settings_theme_contrast_regular),
        stringResource(R.string.settings_theme_contrast_medium),
        stringResource(R.string.settings_theme_contrast_high)
    )
    val themeContrastValues = listOf(
        "Regular",
        "Medium",
        "High"
    )

    val themeLightnessDescriptions = listOf(
        stringResource(R.string.settings_theme_auto),
        stringResource(R.string.settings_theme_light),
        stringResource(R.string.settings_theme_dark),
    )
    val themeLightnessValues = listOf(
        "Auto",
        "Light",
        "Dark"
    )

    val unitsDescriptions = listOf(
        stringResource(R.string.settings_theme_auto),
        stringResource(R.string.settings_units_imperial),
        stringResource(R.string.settings_units_metric),
    )
    val unitsValues = listOf(
        "Auto",
        "Imperial",
        "Metric",
    )

    ProvidePreferenceLocals {
        LazyColumn (modifier = modifier.background(backgroundColor)){
            stickyHeader {
                Surface {
                    CustomAppBar(stringResource(R.string.settings_screen_title),
                        onNavigateUp = onNavigateUp,
                        navigationButtonTitle = stringResource(R.string.ui_back_button_title)
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

            listPreference(
                key = MainActivity.MEASUREMENT_UNITS_KEY,
                defaultValue = MainActivity.MEASUREMENT_UNITS_DEFAULT,
                values = unitsValues,
                title = {
                    Text(
                        text = stringResource(R.string.settings_section_units),
                        color = textColor
                    )
                },
                item = { value, currentValue, onClick ->
                    ListPreferenceItem(unitsDescriptions[unitsValues.indexOf(value)], value, currentValue, onClick, unitsValues.indexOf(value), unitsValues.size)
                },
                summary = { Text(text = unitsDescriptions[unitsValues.indexOf(it)], color = textColor) },
            )

            item {
                Text(
                    text = stringResource(R.string.menu_manage_accessibility),
                    style = MaterialTheme.typography.headlineSmall,
                    color = textColor,
                    modifier = Modifier.semantics { heading() },
                )
            }

            listPreference(
                key = MainActivity.THEME_LIGHTNESS_KEY,
                defaultValue = MainActivity.THEME_LIGHTNESS_DEFAULT,
                values = themeLightnessValues,
                title = {
                    Text(
                        text = stringResource(R.string.settings_theme_light_dark),
                        color = textColor
                    )
                },
                item = { value, currentValue, onClick ->
                    ListPreferenceItem(themeLightnessDescriptions[themeLightnessValues.indexOf(value)], value, currentValue, onClick, themeLightnessValues.indexOf(value), themeLightnessValues.size)
                },
                summary = { Text(text = themeLightnessDescriptions[themeLightnessValues.indexOf(it)], color = textColor) },
            )

            listPreference(
                key = MainActivity.THEME_CONTRAST_KEY,
                defaultValue = MainActivity.THEME_CONTRAST_DEFAULT,
                values = themeContrastValues,
                title = {
                    Text(
                        text = stringResource(R.string.settings_theme_contrast),
                        color = textColor
                    )
                },
                item = { value, currentValue, onClick ->
                    ListPreferenceItem(themeContrastDescriptions[themeContrastValues.indexOf(value)], value, currentValue, onClick, themeContrastValues.indexOf(value), themeContrastValues.size)
                },
                summary = { Text(text = themeContrastDescriptions[themeContrastValues.indexOf(it)], color = textColor) },
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
                    ListPreferenceItem(value, value, currentValue, onClick, beaconTypes.indexOf(value), beaconTypes.size)
                },
                summary = { Text(text = it, color = textColor) },
            )

            listPreference(
                key = MainActivity.SPEECH_ENGINE_KEY,
                defaultValue = MainActivity.SPEECH_ENGINE_DEFAULT,
                values = uiState.engineTypes,
                title = {
                    Text(
                        text = stringResource(R.string.voice_engine),
                        color = textColor
                    )
                },
                item = { value, currentValue, onClick ->
                    ListPreferenceItem(
                        value.substringBefore(":::"),
                        value,
                        currentValue,
                        onClick,
                        uiState.engineTypes.indexOf(value),
                        uiState.engineTypes.size)
                },
                summary = { Text(text = it.substringBefore(":::"), color = textColor) },
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
                    ListPreferenceItem(value, value, currentValue, onClick, uiState.voiceTypes.indexOf(value), uiState.voiceTypes.size)
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
            switchPreference(
                key = MainActivity.ACCESSIBLE_MAP_KEY,
                defaultValue = MainActivity.ACCESSIBLE_MAP_DEFAULT,
                title = {
                    Text(
                        text = stringResource(R.string.settings_accessible_map),
                        color = textColor
                    )
                },
            )
        }
    }
}