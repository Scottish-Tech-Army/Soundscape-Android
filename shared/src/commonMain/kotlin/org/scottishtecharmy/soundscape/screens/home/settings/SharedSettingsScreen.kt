package org.scottishtecharmy.soundscape.screens.home.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import me.zhanghai.compose.preference.LocalPreferenceTheme
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.listPreference
import me.zhanghai.compose.preference.rememberPreferenceState
import me.zhanghai.compose.preference.sliderPreference
import me.zhanghai.compose.preference.switchPreference
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.preferences.PreferenceDefaults
import org.scottishtecharmy.soundscape.preferences.PreferenceKeys
import org.scottishtecharmy.soundscape.preferences.PreferencesProvider
import org.scottishtecharmy.soundscape.preferences.rememberBooleanPreferenceState
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.beacon_settings_style
import org.scottishtecharmy.soundscape.resources.beacon_settings_style_description
import org.scottishtecharmy.soundscape.resources.callout_settings_position_description
import org.scottishtecharmy.soundscape.resources.callout_settings_position_text
import org.scottishtecharmy.soundscape.resources.callouts_allow_callouts
import org.scottishtecharmy.soundscape.resources.callouts_allow_callouts_description
import org.scottishtecharmy.soundscape.resources.callouts_audio_beacon
import org.scottishtecharmy.soundscape.resources.callouts_audio_beacon_description
import org.scottishtecharmy.soundscape.resources.callouts_mobility
import org.scottishtecharmy.soundscape.resources.callouts_mobility_description
import org.scottishtecharmy.soundscape.resources.callouts_places_and_landmarks
import org.scottishtecharmy.soundscape.resources.callouts_places_and_landmarks_description
import org.scottishtecharmy.soundscape.resources.general_alert_cancel
import org.scottishtecharmy.soundscape.resources.menu_advanced_markers_and_routes
import org.scottishtecharmy.soundscape.resources.menu_manage_accessibility
import org.scottishtecharmy.soundscape.resources.menu_manage_audio
import org.scottishtecharmy.soundscape.resources.menu_manage_callouts
import org.scottishtecharmy.soundscape.resources.menu_manage_locale
import org.scottishtecharmy.soundscape.resources.menu_manage_search
import org.scottishtecharmy.soundscape.resources.menu_media_controls
import org.scottishtecharmy.soundscape.resources.offline_map_storage_title
import org.scottishtecharmy.soundscape.resources.settings_debug_heading
import org.scottishtecharmy.soundscape.resources.settings_explanation
import org.scottishtecharmy.soundscape.resources.settings_media_controls_audio_menu
import org.scottishtecharmy.soundscape.resources.settings_media_controls_original
import org.scottishtecharmy.soundscape.resources.settings_relative_directions_clockface
import org.scottishtecharmy.soundscape.resources.settings_relative_directions_degrees
import org.scottishtecharmy.soundscape.resources.settings_relative_directions_description
import org.scottishtecharmy.soundscape.resources.settings_relative_directions_left_right
import org.scottishtecharmy.soundscape.resources.settings_relative_directions_text
import org.scottishtecharmy.soundscape.resources.settings_reset_button
import org.scottishtecharmy.soundscape.resources.settings_reset_button_hint
import org.scottishtecharmy.soundscape.resources.settings_reset_dialog_message
import org.scottishtecharmy.soundscape.resources.settings_reset_dialog_title
import org.scottishtecharmy.soundscape.resources.settings_screen_title
import org.scottishtecharmy.soundscape.resources.settings_search_auto
import org.scottishtecharmy.soundscape.resources.settings_search_offline
import org.scottishtecharmy.soundscape.resources.settings_search_results_language
import org.scottishtecharmy.soundscape.resources.settings_search_results_language_description
import org.scottishtecharmy.soundscape.resources.settings_section_media_controls
import org.scottishtecharmy.soundscape.resources.settings_section_media_controls_description
import org.scottishtecharmy.soundscape.resources.settings_section_search_network
import org.scottishtecharmy.soundscape.resources.settings_section_search_network_description
import org.scottishtecharmy.soundscape.resources.settings_section_units
import org.scottishtecharmy.soundscape.resources.settings_section_units_description
import org.scottishtecharmy.soundscape.resources.settings_show_map
import org.scottishtecharmy.soundscape.resources.settings_theme_auto
import org.scottishtecharmy.soundscape.resources.settings_travel_recording
import org.scottishtecharmy.soundscape.resources.settings_units_imperial
import org.scottishtecharmy.soundscape.resources.settings_units_metric
import org.scottishtecharmy.soundscape.resources.ui_back_button_title
import org.scottishtecharmy.soundscape.resources.ui_continue
import org.scottishtecharmy.soundscape.resources.voice_settings_speaking_rate
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomAppBar
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomButton
import org.scottishtecharmy.soundscape.screens.onboarding.language.LanguageDropDownMenu
import org.scottishtecharmy.soundscape.screens.onboarding.language.getAppLocale
import org.scottishtecharmy.soundscape.screens.onboarding.language.getSystemLocale
import org.scottishtecharmy.soundscape.screens.onboarding.language.indexOfBestLanguageMatch
import org.scottishtecharmy.soundscape.screens.talkbackHint
import org.scottishtecharmy.soundscape.ui.theme.mediumPadding
import org.scottishtecharmy.soundscape.ui.theme.smallPadding
import org.scottishtecharmy.soundscape.ui.theme.spacing
import org.scottishtecharmy.soundscape.screens.onboarding.language.supportedLanguages as appSupportedLanguages

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SharedSettingsScreen(
    onNavigateUp: () -> Unit,
    beaconTypes: List<String>,
    preferencesProvider: PreferencesProvider? = null,
    mediaControlsValues: List<String>? = null,
    mediaControlsDescriptions: List<String>? = null,
    platformAccessibilityContent: (LazyListScope.() -> Unit)? = null,
    platformStorageContent: (LazyListScope.() -> Unit)? = null,
    platformAudioContent: (LazyListScope.() -> Unit)? = null,
    platformLanguageContent: (LazyListScope.() -> Unit)? = null,
    platformMediaControlsContent: (LazyListScope.() -> Unit)? = null,
    platformDebugContent: (LazyListScope.() -> Unit)? = null,
    onNavigateToAdvancedMarkersAndRoutes: (() -> Unit)? = null,
    /**
     * If non-null, the Debug section shows a "Reset settings to defaults" button
     * that pops a confirmation dialog and invokes this callback on confirmation.
     * The platform implementation is expected to clear preferences and restart
     * the app (Android relaunches the activity, iOS exits via `exit(0)`).
     */
    onResetSettings: (() -> Unit)? = null,
    /**
     * Beacon style audio preview. When all three are non-null the beacon style
     * row opens a dialog that asks the platform to play a temporary beacon at
     * the given style so the user can hear it before committing the choice.
     * iOS currently leaves these null; the dialog still lets the user pick a
     * style, just without an audio preview.
     *
     * - [onBeaconPreviewStart] is fired with the currently-saved style when the
     *   dialog opens.
     * - [onBeaconPreviewUpdate] is fired with the tapped style as the user
     *   moves through the list, without persisting the choice.
     * - [onBeaconPreviewStop] is fired with `(commit=true, chosen)` on OK or
     *   `(commit=false, null)` on Cancel/dismiss; persistence of the chosen
     *   style happens via the shared [PreferencesProvider] before this call.
     */
    onBeaconPreviewStart: ((String) -> Unit)? = null,
    onBeaconPreviewUpdate: ((String) -> Unit)? = null,
    onBeaconPreviewStop: ((Boolean, String?) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val showResetDialog = rememberSaveable { mutableStateOf(false) }
    if (showResetDialog.value && onResetSettings != null) {
        AlertDialog(
            onDismissRequest = { showResetDialog.value = false },
            title = { Text(stringResource(Res.string.settings_reset_dialog_title)) },
            text = { Text(stringResource(Res.string.settings_reset_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog.value = false
                        onResetSettings()
                    },
                ) {
                    Text(
                        text = stringResource(Res.string.ui_continue),
                        modifier = Modifier.talkbackHint(stringResource(Res.string.settings_reset_button_hint)),
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog.value = false }) {
                    Text(stringResource(Res.string.general_alert_cancel))
                }
            },
        )
    }
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

    val defaultMediaControlsDescriptions = listOf(
        stringResource(Res.string.settings_media_controls_original),
        stringResource(Res.string.settings_media_controls_audio_menu),
    )
    val defaultMediaControlsValues = listOf("Original", "AudioMenu")

    val effectiveMediaControlsValues = mediaControlsValues ?: defaultMediaControlsValues
    val effectiveMediaControlsDescriptions =
        mediaControlsDescriptions ?: defaultMediaControlsDescriptions

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
                    onToggle = {
                        expandedSection.value =
                            if (expandedSection.value == "callouts") null else "callouts"
                    },
                    textColor = textColor,
                )
            }
            if (expandedSection.value == "callouts") {
                switchPreference(
                    key = PreferenceKeys.ALLOW_CALLOUTS,
                    defaultValue = PreferenceDefaults.ALLOW_CALLOUTS,
                    modifier = expandedSectionModifier,
                    title = {
                        SettingDetails(
                            Res.string.callouts_allow_callouts,
                            Res.string.callouts_allow_callouts_description,
                            textColor
                        )
                    },
                )
                switchPreference(
                    key = PreferenceKeys.PLACES_AND_LANDMARKS,
                    defaultValue = PreferenceDefaults.PLACES_AND_LANDMARKS,
                    modifier = expandedSectionModifier,
                    title = {
                        SettingDetails(
                            Res.string.callouts_places_and_landmarks,
                            Res.string.callouts_places_and_landmarks_description,
                            textColor
                        )
                    },
                    enabled = { allowCallouts },
                )
                switchPreference(
                    key = PreferenceKeys.MOBILITY,
                    defaultValue = PreferenceDefaults.MOBILITY,
                    modifier = expandedSectionModifier,
                    title = {
                        SettingDetails(
                            Res.string.callouts_mobility,
                            Res.string.callouts_mobility_description,
                            textColor
                        )
                    },
                    enabled = { allowCallouts },
                )
                switchPreference(
                    key = PreferenceKeys.DISTANCE_TO_BEACON,
                    defaultValue = PreferenceDefaults.DISTANCE_TO_BEACON,
                    modifier = expandedSectionModifier,
                    title = {
                        SettingDetails(
                            Res.string.callouts_audio_beacon,
                            Res.string.callouts_audio_beacon_description,
                            textColor
                        )
                    },
                    enabled = { allowCallouts },
                )
                switchPreference(
                    key = PreferenceKeys.POSITION_INCLUDES_HEADING_AND_DISTANCE,
                    defaultValue = PreferenceDefaults.POSITION_INCLUDES_HEADING_AND_DISTANCE,
                    modifier = expandedSectionModifier,
                    title = {
                        SettingDetails(
                            Res.string.callout_settings_position_text,
                            Res.string.callout_settings_position_description,
                            textColor
                        )
                    },
                    enabled = { allowCallouts },
                )
                listPreference(
                    key = PreferenceKeys.RELATIVE_DIRECTION,
                    defaultValue = PreferenceDefaults.RELATIVE_DIRECTION,
                    values = relativeDirectionValues,
                    modifier = expandedSectionModifier,
                    title = {
                        SettingDetails(
                            Res.string.settings_relative_directions_text,
                            Res.string.settings_relative_directions_description,
                            textColor
                        )
                    },
                    item = { value, currentValue, onClick ->
                        ListPreferenceItem(
                            relativeDirectionDescriptions[relativeDirectionValues.indexOf(
                                value
                            )],
                            value,
                            currentValue,
                            onClick,
                            relativeDirectionValues.indexOf(value),
                            relativeDirectionValues.size
                        )
                    },
                    summary = {
                        ClickableOption(
                            relativeDirectionDescriptions[relativeDirectionValues.indexOf(
                                it
                            )], textColor
                        )
                    },
                )
            }

            // ── Search Section ───────────────────────────────────────────
            item(key = "header_search") {
                ExpandableSectionHeader(
                    title = stringResource(Res.string.menu_manage_search),
                    expanded = expandedSection.value == "search",
                    onToggle = {
                        expandedSection.value =
                            if (expandedSection.value == "search") null else "search"
                    },
                    textColor = textColor,
                )
            }
            if (expandedSection.value == "search") {
                listPreference(
                    key = PreferenceKeys.GEOCODER_MODE,
                    defaultValue = PreferenceDefaults.GEOCODER_MODE,
                    values = geocoderValues,
                    modifier = expandedSectionModifier,
                    title = {
                        SettingDetails(
                            Res.string.settings_section_search_network,
                            Res.string.settings_section_search_network_description,
                            textColor
                        )
                    },
                    item = { value, currentValue, onClick ->
                        ListPreferenceItem(
                            geocoderDescriptions[geocoderValues.indexOf(value)],
                            value,
                            currentValue,
                            onClick,
                            geocoderValues.indexOf(value),
                            geocoderValues.size
                        )
                    },
                    summary = {
                        ClickableOption(
                            geocoderDescriptions[geocoderValues.indexOf(it)],
                            textColor
                        )
                    },
                )
                listPreference(
                    key = PreferenceKeys.SEARCH_LANGUAGE,
                    defaultValue = PreferenceDefaults.SEARCH_LANGUAGE,
                    values = searchLanguageValues,
                    modifier = expandedSectionModifier,
                    title = {
                        SettingDetails(
                            Res.string.settings_search_results_language,
                            Res.string.settings_search_results_language_description,
                            textColor
                        )
                    },
                    item = { value, currentValue, onClick ->
                        ListPreferenceItem(
                            searchLanguageDescriptions[searchLanguageValues.indexOf(
                                value
                            )],
                            value,
                            currentValue,
                            onClick,
                            searchLanguageValues.indexOf(value),
                            searchLanguageValues.size
                        )
                    },
                    summary = {
                        ClickableOption(
                            searchLanguageDescriptions[searchLanguageValues.indexOf(
                                it
                            )], textColor
                        )
                    },
                )
            }

            // ── Accessibility Section ────────────────────────────────────
            // Section header always renders. The shared "Show map" toggle is
            // included here when a PreferencesProvider is supplied; the
            // platform slot adds platform-specific items (e.g. Android theme).
            if (preferencesProvider != null || platformAccessibilityContent != null) {
                item(key = "header_accessibility") {
                    ExpandableSectionHeader(
                        title = stringResource(Res.string.menu_manage_accessibility),
                        expanded = expandedSection.value == "accessibility",
                        onToggle = {
                            expandedSection.value =
                                if (expandedSection.value == "accessibility") null else "accessibility"
                        },
                        textColor = textColor,
                    )
                }
                if (expandedSection.value == "accessibility") {
                    if (preferencesProvider != null) {
                        item(key = "shared_show_map") {
                            val showMap = rememberBooleanPreferenceState(
                                preferencesProvider,
                                PreferenceKeys.SHOW_MAP,
                                PreferenceDefaults.SHOW_MAP,
                            )
                            me.zhanghai.compose.preference.SwitchPreference(
                                state = showMap,
                                title = {
                                    Text(
                                        text = stringResource(Res.string.settings_show_map),
                                        color = textColor,
                                    )
                                },
                                modifier = expandedSectionModifier,
                            )
                        }
                    }
                    if (platformAccessibilityContent != null) {
                        platformAccessibilityContent()
                    }
                }
            }

            // ── Storage Section (platform-only) ──────────────────────────
            if (platformStorageContent != null) {
                item(key = "header_storage") {
                    ExpandableSectionHeader(
                        title = stringResource(Res.string.offline_map_storage_title),
                        expanded = expandedSection.value == "storage",
                        onToggle = {
                            expandedSection.value =
                                if (expandedSection.value == "storage") null else "storage"
                        },
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
                    onToggle = {
                        expandedSection.value =
                            if (expandedSection.value == "audio") null else "audio"
                    },
                    textColor = textColor,
                )
            }
            if (expandedSection.value == "audio") {
                if (beaconTypes.isNotEmpty()) {
                    item(key = "beacon_style_preview") {
                        BeaconStylePreference(
                            beaconTypes = beaconTypes,
                            preferencesProvider = preferencesProvider,
                            modifier = expandedSectionModifier,
                            textColor = textColor,
                            onPreviewStart = onBeaconPreviewStart,
                            onPreviewUpdate = onBeaconPreviewUpdate,
                            onPreviewStop = onBeaconPreviewStop,
                        )
                    }
                }

                platformAudioContent?.invoke(this)

                sliderPreference(
                    key = PreferenceKeys.SPEECH_RATE,
                    defaultValue = PreferenceDefaults.SPEECH_RATE,
                    modifier = expandedSectionModifier,
                    title = {
                        Text(
                            text = stringResource(Res.string.voice_settings_speaking_rate),
                            color = textColor
                        )
                    },
                    valueRange = 0.5f..2.0f,
                    valueSteps = 10,
                    valueText = {
                        Text(
                            text = "${((it * 10).toInt() / 10.0)}x",
                            color = textColor
                        )
                    },
                )
            }

            // ── Language Section ──────────────────────────────────────────
            item(key = "header_language") {
                ExpandableSectionHeader(
                    title = stringResource(Res.string.menu_manage_locale),
                    expanded = expandedSection.value == "language",
                    onToggle = {
                        expandedSection.value =
                            if (expandedSection.value == "language") null else "language"
                    },
                    textColor = textColor,
                )
            }
            if (expandedSection.value == "language") {
                listPreference(
                    key = PreferenceKeys.MEASUREMENT_UNITS,
                    defaultValue = PreferenceDefaults.MEASUREMENT_UNITS,
                    values = unitsValues,
                    modifier = expandedSectionModifier,
                    title = {
                        SettingDetails(
                            Res.string.settings_section_units,
                            Res.string.settings_section_units_description,
                            textColor
                        )
                    },
                    item = { value, currentValue, onClick ->
                        ListPreferenceItem(
                            unitsDescriptions[unitsValues.indexOf(value)],
                            value,
                            currentValue,
                            onClick,
                            unitsValues.indexOf(value),
                            unitsValues.size
                        )
                    },
                    summary = {
                        ClickableOption(
                            unitsDescriptions[unitsValues.indexOf(it)],
                            textColor
                        )
                    },
                )

                platformLanguageContent?.invoke(this)
            }

            // ── Media Controls Section ───────────────────────────────────
            item(key = "header_media_control") {
                ExpandableSectionHeader(
                    title = stringResource(Res.string.menu_media_controls),
                    expanded = expandedSection.value == "media_controls",
                    onToggle = {
                        expandedSection.value =
                            if (expandedSection.value == "media_controls") null else "media_controls"
                    },
                    textColor = textColor,
                )
            }
            if (expandedSection.value == "media_controls") {
                listPreference(
                    key = PreferenceKeys.MEDIA_CONTROLS_MODE,
                    defaultValue = PreferenceDefaults.MEDIA_CONTROLS_MODE,
                    values = effectiveMediaControlsValues,
                    modifier = expandedSectionModifier,
                    title = {
                        SettingDetails(
                            Res.string.settings_section_media_controls,
                            Res.string.settings_section_media_controls_description,
                            textColor
                        )
                    },
                    item = { value, currentValue, onClick ->
                        ListPreferenceItem(
                            effectiveMediaControlsDescriptions[effectiveMediaControlsValues.indexOf(
                                value
                            )],
                            value,
                            currentValue,
                            onClick,
                            effectiveMediaControlsValues.indexOf(value),
                            effectiveMediaControlsValues.size
                        )
                    },
                    summary = {
                        ClickableOption(
                            effectiveMediaControlsDescriptions[effectiveMediaControlsValues.indexOf(
                                it
                            )], textColor
                        )
                    },
                )

                platformMediaControlsContent?.invoke(this)
            }

            // ── Debug Section ────────────────────────────────────────────
            item(key = "header_debug") {
                ExpandableSectionHeader(
                    title = stringResource(Res.string.settings_debug_heading),
                    expanded = expandedSection.value == "debug",
                    onToggle = {
                        expandedSection.value =
                            if (expandedSection.value == "debug") null else "debug"
                    },
                    textColor = textColor,
                )
            }
            if (expandedSection.value == "debug") {
                switchPreference(
                    key = PreferenceKeys.RECORD_TRAVEL,
                    defaultValue = PreferenceDefaults.RECORD_TRAVEL,
                    modifier = expandedSectionModifier,
                    title = {
                        Text(
                            text = stringResource(Res.string.settings_travel_recording),
                            color = textColor
                        )
                    },
                )

                if (onNavigateToAdvancedMarkersAndRoutes != null) {
                    item(key = "advanced_markers_and_routes") {
                        Column(modifier = expandedSectionModifier.fillMaxWidth()) {
                            CustomButton(
                                onClick = onNavigateToAdvancedMarkersAndRoutes,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .mediumPadding(),
                                shape = RoundedCornerShape(spacing.extraSmall),
                                text = stringResource(Res.string.menu_advanced_markers_and_routes),
                                textStyle = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                platformDebugContent?.invoke(this)

                if (onResetSettings != null) {
                    item(key = "reset_settings") {
                        Column(modifier = expandedSectionModifier.fillMaxWidth()) {
                            CustomButton(
                                onClick = { showResetDialog.value = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .smallPadding()
                                    .talkbackHint(stringResource(Res.string.settings_reset_button_hint)),
                                buttonColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                shape = RoundedCornerShape(spacing.small),
                                text = stringResource(Res.string.settings_reset_button),
                                textStyle = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Beacon style preference with an inline audio preview.
 *
 * Tapping the row opens a dialog that:
 *  - on open, fires [onPreviewStart] with the currently saved style so the
 *    platform can stop any running beacon and start a temporary preview
 *    beacon directly ahead of the listener;
 *  - on each option tap, swaps [selectedInDialog] and fires [onPreviewUpdate]
 *    without persisting the choice (so a Cancel cleanly reverts);
 *  - on OK, persists the selected style via [preferencesProvider] and fires
 *    [onPreviewStop] with `(true, chosen)` so the platform can restore the
 *    previously-running beacon (now using the new style);
 *  - on Cancel/dismiss, fires [onPreviewStop] with `(false, null)` so the
 *    platform reverts the engine and restores the previously-running beacon.
 *
 * Preview callbacks are optional: iOS leaves them null and gets a silent
 * picker; Android wires them through SoundscapeService.
 */
@Composable
private fun BeaconStylePreference(
    beaconTypes: List<String>,
    preferencesProvider: PreferencesProvider?,
    modifier: Modifier,
    textColor: androidx.compose.ui.graphics.Color,
    onPreviewStart: ((String) -> Unit)?,
    onPreviewUpdate: ((String) -> Unit)?,
    onPreviewStop: ((Boolean, String?) -> Unit)?,
) {
    val savedValueState = rememberPreferenceState(
        PreferenceKeys.BEACON_TYPE,
        PreferenceDefaults.BEACON_TYPE,
    )
    val savedValue by savedValueState

    var showDialog by rememberSaveable { mutableStateOf(false) }
    var selectedInDialog by rememberSaveable { mutableStateOf(savedValue) }

    // Match the indent of library-driven listPreference rows (e.g. Voices)
    // by wrapping the title+summary stack in the same theme.padding the
    // library applies inside its Preference composable.
    val theme = LocalPreferenceTheme.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button) {
                selectedInDialog = savedValue
                showDialog = true
            },
    ) {
        Column(modifier = Modifier.padding(theme.padding)) {
            SettingDetails(
                Res.string.beacon_settings_style,
                Res.string.beacon_settings_style_description,
                textColor,
            )
            ClickableOption(savedValue, textColor)
        }
    }

    if (showDialog) {
        // Start the preview once when the dialog opens. Using selectedInDialog
        // (rememberSaveable) means a config-change recreate resumes on the
        // last-tapped style rather than the persisted one. The platform side
        // is idempotent so a re-fire is safe.
        DisposableEffect(Unit) {
            onPreviewStart?.invoke(selectedInDialog)
            // Explicit OK/Cancel handlers fire onPreviewStop synchronously
            // before dismissing the dialog, so we don't need to call it from
            // onDispose.
            onDispose { }
        }

        AlertDialog(
            onDismissRequest = {
                onPreviewStop?.invoke(false, null)
                showDialog = false
            },
            title = {
                Text(
                    text = stringResource(Res.string.beacon_settings_style),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(beaconTypes.size) { index ->
                        val value = beaconTypes[index]
                        ListPreferenceItem(
                            description = value,
                            value = value,
                            currentValue = selectedInDialog,
                            onClick = {
                                if (selectedInDialog != value) {
                                    selectedInDialog = value
                                    onPreviewUpdate?.invoke(value)
                                }
                            },
                            index = index,
                            listSize = beaconTypes.size,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    preferencesProvider?.putString(PreferenceKeys.BEACON_TYPE, selectedInDialog)
                    onPreviewStop?.invoke(true, selectedInDialog)
                    showDialog = false
                }) {
                    Text(stringResource(Res.string.ui_continue))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onPreviewStop?.invoke(false, null)
                    showDialog = false
                }) {
                    Text(stringResource(Res.string.general_alert_cancel))
                }
            },
        )
    }
}
