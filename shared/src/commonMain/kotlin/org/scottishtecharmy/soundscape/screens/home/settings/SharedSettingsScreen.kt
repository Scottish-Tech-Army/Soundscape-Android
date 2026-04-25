package org.scottishtecharmy.soundscape.screens.home.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.listPreference
import me.zhanghai.compose.preference.rememberPreferenceState
import me.zhanghai.compose.preference.sliderPreference
import me.zhanghai.compose.preference.switchPreference
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.preferences.PreferenceDefaults
import org.scottishtecharmy.soundscape.preferences.PreferenceKeys
import org.scottishtecharmy.soundscape.resources.*
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomAppBar
import org.scottishtecharmy.soundscape.ui.theme.spacing

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SharedSettingsScreen(
    onNavigateUp: () -> Unit,
    beaconTypes: List<String>,
    platformAccessibilityContent: (LazyListScope.() -> Unit)? = null,
    platformStorageContent: (LazyListScope.() -> Unit)? = null,
    platformAudioContent: (LazyListScope.() -> Unit)? = null,
    platformLanguageContent: (LazyListScope.() -> Unit)? = null,
    platformMediaControlsContent: (LazyListScope.() -> Unit)? = null,
    platformDebugContent: (LazyListScope.() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val expandedSection = rememberSaveable { mutableStateOf<String?>(null) }

    val textColor = MaterialTheme.colorScheme.onBackground
    val backgroundColor = MaterialTheme.colorScheme.background
    val expandedSectionModifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)

    // Value/description lists for shared list preferences
    val relativeDirectionDescriptions = listOf(
        stringResource(Res.string.settings_relative_directions_clockface),
        stringResource(Res.string.settings_relative_directions_degrees),
        stringResource(Res.string.settings_relative_directions_left_right),
    )
    val relativeDirectionValues = listOf("ClockFace", "Degrees", "LeftRight")

    val unitsDescriptions = listOf(
        stringResource(Res.string.settings_theme_auto),
        stringResource(Res.string.settings_units_imperial),
        stringResource(Res.string.settings_units_metric),
    )
    val unitsValues = listOf("Auto", "Imperial", "Metric")

    val searchLanguageDescriptions = listOf(
        stringResource(Res.string.settings_theme_auto),
        "Français", "English", "Deutsch",
    )
    val searchLanguageValues = listOf("auto", "fr", "en", "de")

    val geocoderDescriptions = listOf(
        stringResource(Res.string.settings_search_auto),
        stringResource(Res.string.settings_search_offline),
    )
    val geocoderValues = listOf("Auto", "Offline")

    ProvidePreferenceLocals {
        // Track allowCallouts reactively for enabling/disabling child settings
        val allowCallouts by rememberPreferenceState(
            PreferenceKeys.ALLOW_CALLOUTS,
            PreferenceDefaults.ALLOW_CALLOUTS,
        )

        LazyColumn(modifier = modifier.background(backgroundColor).fillMaxSize()) {
            stickyHeader {
                Surface {
                    CustomAppBar(
                        stringResource(Res.string.settings_screen_title),
                        onNavigateUp = onNavigateUp,
                        navigationButtonTitle = stringResource(Res.string.ui_back_button_title),
                    )
                }
            }

            item(key = "header_explanation") {
                Text(
                    text = stringResource(Res.string.settings_explanation),
                    color = textColor,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(spacing.small),
                )
            }

            // ── Callouts Section ─────────────────────────────────────────
            item(key = "header_callouts") {
                ExpandableSectionHeader(
                    title = stringResource(Res.string.menu_manage_callouts),
                    expanded = expandedSection.value == "callouts",
                    onToggle = { expandedSection.value = if (expandedSection.value == "callouts") null else "callouts" },
                    textColor = textColor,
                )
            }
            if (expandedSection.value == "callouts") {
                switchPreference(
                    key = PreferenceKeys.ALLOW_CALLOUTS,
                    defaultValue = PreferenceDefaults.ALLOW_CALLOUTS,
                    modifier = expandedSectionModifier,
                    title = { SettingDetails(Res.string.callouts_allow_callouts, Res.string.callouts_allow_callouts_description, textColor) },
                )
                switchPreference(
                    key = PreferenceKeys.PLACES_AND_LANDMARKS,
                    defaultValue = PreferenceDefaults.PLACES_AND_LANDMARKS,
                    modifier = expandedSectionModifier,
                    title = { SettingDetails(Res.string.callouts_places_and_landmarks, Res.string.callouts_places_and_landmarks_description, textColor) },
                    enabled = { allowCallouts },
                )
                switchPreference(
                    key = PreferenceKeys.MOBILITY,
                    defaultValue = PreferenceDefaults.MOBILITY,
                    modifier = expandedSectionModifier,
                    title = { SettingDetails(Res.string.callouts_mobility, Res.string.callouts_mobility_description, textColor) },
                    enabled = { allowCallouts },
                )
                switchPreference(
                    key = PreferenceKeys.DISTANCE_TO_BEACON,
                    defaultValue = PreferenceDefaults.DISTANCE_TO_BEACON,
                    modifier = expandedSectionModifier,
                    title = { SettingDetails(Res.string.callouts_audio_beacon, Res.string.callouts_audio_beacon_description, textColor) },
                    enabled = { allowCallouts },
                )
                switchPreference(
                    key = PreferenceKeys.POSITION_INCLUDES_HEADING_AND_DISTANCE,
                    defaultValue = PreferenceDefaults.POSITION_INCLUDES_HEADING_AND_DISTANCE,
                    modifier = expandedSectionModifier,
                    title = { SettingDetails(Res.string.callout_settings_position_text, Res.string.callout_settings_position_description, textColor) },
                    enabled = { allowCallouts },
                )
                listPreference(
                    key = PreferenceKeys.RELATIVE_DIRECTION,
                    defaultValue = PreferenceDefaults.RELATIVE_DIRECTION,
                    values = relativeDirectionValues,
                    modifier = expandedSectionModifier,
                    title = { SettingDetails(Res.string.settings_relative_directions_text, Res.string.settings_relative_directions_description, textColor) },
                    item = { value, currentValue, onClick ->
                        ListPreferenceItem(relativeDirectionDescriptions[relativeDirectionValues.indexOf(value)], value, currentValue, onClick, relativeDirectionValues.indexOf(value), relativeDirectionValues.size)
                    },
                    summary = { ClickableOption(relativeDirectionDescriptions[relativeDirectionValues.indexOf(it)], textColor) },
                )
            }

            // ── Search Section ───────────────────────────────────────────
            item(key = "header_search") {
                ExpandableSectionHeader(
                    title = stringResource(Res.string.menu_manage_search),
                    expanded = expandedSection.value == "search",
                    onToggle = { expandedSection.value = if (expandedSection.value == "search") null else "search" },
                    textColor = textColor,
                )
            }
            if (expandedSection.value == "search") {
                listPreference(
                    key = PreferenceKeys.GEOCODER_MODE,
                    defaultValue = PreferenceDefaults.GEOCODER_MODE,
                    values = geocoderValues,
                    modifier = expandedSectionModifier,
                    title = { SettingDetails(Res.string.settings_section_search_network, Res.string.settings_section_search_network_description, textColor) },
                    item = { value, currentValue, onClick ->
                        ListPreferenceItem(geocoderDescriptions[geocoderValues.indexOf(value)], value, currentValue, onClick, geocoderValues.indexOf(value), geocoderValues.size)
                    },
                    summary = { ClickableOption(geocoderDescriptions[geocoderValues.indexOf(it)], textColor) },
                )
                listPreference(
                    key = PreferenceKeys.SEARCH_LANGUAGE,
                    defaultValue = PreferenceDefaults.SEARCH_LANGUAGE,
                    values = searchLanguageValues,
                    modifier = expandedSectionModifier,
                    title = { SettingDetails(Res.string.settings_search_results_language, Res.string.settings_search_results_language_description, textColor) },
                    item = { value, currentValue, onClick ->
                        ListPreferenceItem(searchLanguageDescriptions[searchLanguageValues.indexOf(value)], value, currentValue, onClick, searchLanguageValues.indexOf(value), searchLanguageValues.size)
                    },
                    summary = { ClickableOption(searchLanguageDescriptions[searchLanguageValues.indexOf(it)], textColor) },
                )
            }

            // ── Accessibility Section (platform-only) ────────────────────
            if (platformAccessibilityContent != null) {
                item(key = "header_accessibility") {
                    ExpandableSectionHeader(
                        title = stringResource(Res.string.menu_manage_accessibility),
                        expanded = expandedSection.value == "accessibility",
                        onToggle = { expandedSection.value = if (expandedSection.value == "accessibility") null else "accessibility" },
                        textColor = textColor,
                    )
                }
                if (expandedSection.value == "accessibility") {
                    platformAccessibilityContent()
                }
            }

            // ── Storage Section (platform-only) ──────────────────────────
            if (platformStorageContent != null) {
                item(key = "header_storage") {
                    ExpandableSectionHeader(
                        title = stringResource(Res.string.offline_map_storage_title),
                        expanded = expandedSection.value == "storage",
                        onToggle = { expandedSection.value = if (expandedSection.value == "storage") null else "storage" },
                        textColor = textColor,
                    )
                }
                if (expandedSection.value == "storage") {
                    platformStorageContent()
                }
            }

            // ── Audio Section ────────────────────────────────────────────
            item(key = "header_audio") {
                ExpandableSectionHeader(
                    title = stringResource(Res.string.menu_manage_audio),
                    expanded = expandedSection.value == "audio",
                    onToggle = { expandedSection.value = if (expandedSection.value == "audio") null else "audio" },
                    textColor = textColor,
                )
            }
            if (expandedSection.value == "audio") {
                if (beaconTypes.isNotEmpty()) {
                    listPreference(
                        key = PreferenceKeys.BEACON_TYPE,
                        defaultValue = PreferenceDefaults.BEACON_TYPE,
                        values = beaconTypes,
                        modifier = expandedSectionModifier,
                        title = { SettingDetails(Res.string.beacon_settings_style, Res.string.beacon_settings_style_description, textColor) },
                        item = { value, currentValue, onClick ->
                            ListPreferenceItem(value, value, currentValue, onClick, beaconTypes.indexOf(value), beaconTypes.size)
                        },
                        summary = { ClickableOption(it, textColor) },
                    )
                }

                platformAudioContent?.invoke(this)

                sliderPreference(
                    key = PreferenceKeys.SPEECH_RATE,
                    defaultValue = PreferenceDefaults.SPEECH_RATE,
                    modifier = expandedSectionModifier,
                    title = { Text(text = stringResource(Res.string.voice_settings_speaking_rate), color = textColor) },
                    valueRange = 0.5f..2.0f,
                    valueSteps = 10,
                    valueText = { Text(text = "${((it * 10).toInt() / 10.0)}x", color = textColor) },
                )

                switchPreference(
                    key = PreferenceKeys.MIX_AUDIO,
                    defaultValue = PreferenceDefaults.MIX_AUDIO,
                    modifier = expandedSectionModifier,
                    title = { SettingDetails(Res.string.settings_mix_audio, Res.string.settings_mix_audio_description, textColor) },
                )
            }

            // ── Language Section ──────────────────────────────────────────
            item(key = "header_language") {
                ExpandableSectionHeader(
                    title = stringResource(Res.string.menu_manage_language),
                    expanded = expandedSection.value == "language",
                    onToggle = { expandedSection.value = if (expandedSection.value == "language") null else "language" },
                    textColor = textColor,
                )
            }
            if (expandedSection.value == "language") {
                listPreference(
                    key = PreferenceKeys.MEASUREMENT_UNITS,
                    defaultValue = PreferenceDefaults.MEASUREMENT_UNITS,
                    values = unitsValues,
                    modifier = expandedSectionModifier,
                    title = { SettingDetails(Res.string.settings_section_units, Res.string.settings_section_units_description, textColor) },
                    item = { value, currentValue, onClick ->
                        ListPreferenceItem(unitsDescriptions[unitsValues.indexOf(value)], value, currentValue, onClick, unitsValues.indexOf(value), unitsValues.size)
                    },
                    summary = { ClickableOption(unitsDescriptions[unitsValues.indexOf(it)], textColor) },
                )

                platformLanguageContent?.invoke(this)
            }

            // ── Media Controls Section (platform-only) ───────────────────
            if (platformMediaControlsContent != null) {
                item(key = "header_media_control") {
                    ExpandableSectionHeader(
                        title = stringResource(Res.string.menu_media_controls),
                        expanded = expandedSection.value == "media_controls",
                        onToggle = { expandedSection.value = if (expandedSection.value == "media_controls") null else "media_controls" },
                        textColor = textColor,
                    )
                }
                if (expandedSection.value == "media_controls") {
                    platformMediaControlsContent()
                }
            }

            // ── Debug Section ────────────────────────────────────────────
            item(key = "header_debug") {
                ExpandableSectionHeader(
                    title = stringResource(Res.string.settings_debug_heading),
                    expanded = expandedSection.value == "debug",
                    onToggle = { expandedSection.value = if (expandedSection.value == "debug") null else "debug" },
                    textColor = textColor,
                )
            }
            if (expandedSection.value == "debug") {
                switchPreference(
                    key = PreferenceKeys.RECORD_TRAVEL,
                    defaultValue = PreferenceDefaults.RECORD_TRAVEL,
                    modifier = expandedSectionModifier,
                    title = { Text(text = stringResource(Res.string.settings_travel_recording), color = textColor) },
                )
                switchPreference(
                    key = PreferenceKeys.ACCESSIBLE_MAP,
                    defaultValue = PreferenceDefaults.ACCESSIBLE_MAP,
                    modifier = expandedSectionModifier,
                    title = { Text(text = stringResource(Res.string.settings_accessible_map), color = textColor) },
                )

                platformDebugContent?.invoke(this)
            }
        }
    }
}
