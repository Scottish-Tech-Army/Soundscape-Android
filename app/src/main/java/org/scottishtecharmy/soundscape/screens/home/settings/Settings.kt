package org.scottishtecharmy.soundscape.screens.home.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.preference.PreferenceManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.listPreference
import me.zhanghai.compose.preference.sliderPreference
import me.zhanghai.compose.preference.switchPreference
import org.scottishtecharmy.soundscape.MainActivity
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.screens.home.HomeRoutes
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomAppBar
import org.scottishtecharmy.soundscape.screens.markers_routes.components.CustomButton
import org.scottishtecharmy.soundscape.screens.onboarding.language.Language
import org.scottishtecharmy.soundscape.screens.onboarding.language.LanguageDropDownMenu
import org.scottishtecharmy.soundscape.screens.onboarding.language.MockLanguagePreviewData
import org.scottishtecharmy.soundscape.screens.onboarding.offlinestorage.MockStoragePreviewData
import org.scottishtecharmy.soundscape.screens.onboarding.offlinestorage.StorageDropDownMenu
import org.scottishtecharmy.soundscape.screens.talkbackHint
import org.scottishtecharmy.soundscape.ui.theme.extraSmallPadding
import org.scottishtecharmy.soundscape.ui.theme.mediumPadding
import org.scottishtecharmy.soundscape.ui.theme.smallPadding
import org.scottishtecharmy.soundscape.ui.theme.spacing
import org.scottishtecharmy.soundscape.utils.StorageUtils
import org.scottishtecharmy.soundscape.viewmodels.SettingsViewModel

// This code uses the library https://github.com/zhanghai/ComposePreference
// The UI changes the SharedPreference reference by the `key` which can then be accessed
// anywhere else in the app.

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
            .extraSmallPadding()
            .defaultMinSize(minHeight = spacing.targetSize)
            .clickable(role = Role.RadioButton) {
                onClick()
            }
            .talkbackHint(
                if (value == currentValue) stringResource(R.string.settings_keep_value)
                else stringResource(R.string.settings_use_value)
            )
//            .talkbackDescription(
//                stringResource(R.string.settings_list_item_description).format(
//                    value,
//                    index + 1,
//                    listSize
//                )
//            )
            ,
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
                .width(spacing.targetSize),
            imageVector =
                if(value == currentValue) Icons.Filled.CheckBox
                else Icons.Filled.CheckBoxOutlineBlank,
            tint = MaterialTheme.colorScheme.onSurface,
            contentDescription = ""
        )
    }
}

/**
 * A clickable header that expands/collapses a section
 */
@Composable
fun ExpandableSectionHeader(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    textColor: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                onClickLabel = if (expanded) "Collapse section" else "Expand section"
            ) { onToggle() }
            .extraSmallPadding()
            .defaultMinSize(minHeight = spacing.targetSize),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = textColor,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .semantics { heading() }
                .weight(1f)
        )
        Icon(
            imageVector = if (expanded) Icons.Filled.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = if (expanded) "Collapse" else "Expand",
            tint = textColor
        )
    }
}
@Composable
fun SettingDetails(title: Int, description: Int, textColor: Color) {
    Column {
        Text(
            text = stringResource(title),
            color = textColor,
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = stringResource(description),
            color = textColor,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Settings(
    navController: NavHostController,
    uiState: SettingsViewModel.SettingsUiState,
    modifier: Modifier = Modifier,
    supportedLanguages: List<Language>,
    onLanguageSelected: (Language) -> Unit,
    selectedLanguageIndex: Int,
    storages: List<StorageUtils.StorageSpace>,
    onStorageSelected: (String) -> Unit,
    selectedStorageIndex: Int,
    resetSettings: () -> Unit,
    previewExpandedSection: String? = null
)
{
    val showConfirmationDialog = remember { mutableStateOf(false) }

    // Track which section is expanded (null = none, only one can be expanded at a time)
    val expandedSection = rememberSaveable { mutableStateOf(previewExpandedSection) }

    val beaconValues = uiState.beaconValues
    val beaconDescriptions = uiState.beaconDescriptions.map { stringResource(it) }

    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val expandedSectionModifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
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

    val searchLanguageDescriptions = listOf(
        stringResource(R.string.settings_theme_auto),
        "Français",
        "English",
        "Deutsch"
    )
    val searchLanguageValues = listOf(
        "auto",
        "fr",
        "en",
        "de"
    )

    val geocoderDescriptions = listOf(
        stringResource(R.string.settings_search_auto),
        stringResource(R.string.settings_search_offline),
    )
    val geocoderValues = listOf(
        "Auto",
        "Offline"
    )

    if (showConfirmationDialog.value) {
        AlertDialog(
            onDismissRequest = { showConfirmationDialog.value = false },
            title = { Text(stringResource(R.string.settings_reset_dialog_title)) },
            text = { Text(stringResource(R.string.settings_reset_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        resetSettings()
                        showConfirmationDialog.value = false
                    }
                ) {
                    Text(
                        text = stringResource(R.string.ui_continue),
                        modifier = Modifier
                            .talkbackHint(stringResource(R.string.settings_reset_button_hint))
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmationDialog.value = false }
                ) {
                    Text(stringResource(R.string.general_alert_cancel))
                }
            }
        )
    }
    ProvidePreferenceLocals {
        // Track the ALLOW_CALLOUTS preference to enable/disable child switches
        val context = LocalContext.current
        val sharedPreferences = remember { PreferenceManager.getDefaultSharedPreferences(context) }
        val allowCallouts = remember {
            mutableStateOf(sharedPreferences.getBoolean(MainActivity.ALLOW_CALLOUTS_KEY, MainActivity.ALLOW_CALLOUTS_DEFAULT))
        }
        DisposableEffect(sharedPreferences) {
            val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == MainActivity.ALLOW_CALLOUTS_KEY) {
                    allowCallouts.value = sharedPreferences.getBoolean(MainActivity.ALLOW_CALLOUTS_KEY, MainActivity.ALLOW_CALLOUTS_DEFAULT)
                }
            }
            sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
            onDispose {
                sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }

        LazyColumn(modifier = modifier.background(backgroundColor).fillMaxSize()) {
            stickyHeader {
                Surface {
                    CustomAppBar(
                        stringResource(R.string.settings_screen_title),
                        onNavigateUp =  { navController.navigateUp() },
                        navigationButtonTitle = stringResource(R.string.ui_back_button_title)
                    )
                }
            }

            item(key = "header_explanation") {
                Text(
                    text = stringResource(R.string.settings_explanation),
                    color = textColor,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(spacing.small)
                )
            }

            // Callouts Section
            item(key = "header_callouts") {
                ExpandableSectionHeader(
                    title = stringResource(R.string.menu_manage_callouts),
                    expanded = expandedSection.value == "callouts",
                    onToggle = { expandedSection.value = if (expandedSection.value == "callouts") null else "callouts" },
                    textColor = textColor
                )
            }
            if (expandedSection.value == "callouts") {
                switchPreference(
                    key = MainActivity.ALLOW_CALLOUTS_KEY,
                    defaultValue = MainActivity.ALLOW_CALLOUTS_DEFAULT,
                    modifier = expandedSectionModifier,
                    title = {
                        SettingDetails(
                            R.string.callouts_allow_callouts,
                            R.string.callouts_allow_callouts_description,
                            textColor
                        )
                    },
                )
                switchPreference(
                    key = MainActivity.PLACES_AND_LANDMARKS_KEY,
                    defaultValue = MainActivity.PLACES_AND_LANDMARKS_DEFAULT,
                    modifier = expandedSectionModifier,
                    title = {
                        SettingDetails(
                            R.string.callouts_places_and_landmarks,
                            R.string.callouts_places_and_landmarks_description,
                            textColor
                        )
                    },
                    enabled = { allowCallouts.value },
                )
                switchPreference(
                    key = MainActivity.MOBILITY_KEY,
                    defaultValue = MainActivity.MOBILITY_DEFAULT,
                    modifier = expandedSectionModifier,
                    title = {
                        SettingDetails(
                            R.string.callouts_mobility,
                            R.string.callouts_mobility_description,
                            textColor
                        )
                    },
                    enabled = { allowCallouts.value },
                )
                switchPreference(
                    key = MainActivity.DISTANCE_TO_BEACON_KEY,
                    defaultValue = MainActivity.DISTANCE_TO_BEACON_DEFAULT,
                    modifier = expandedSectionModifier,
                    title = {
                        SettingDetails(
                            R.string.callouts_audio_beacon,
                            R.string.callouts_audio_beacon_description,
                            textColor
                        )
                    },
                    enabled = { allowCallouts.value },
                )
//                switchPreference(
//                    key = MainActivity.UNNAMED_ROADS_KEY,
//                    defaultValue = MainActivity.UNNAMED_ROADS_DEFAULT,
//                    title = {
//                        SettingDetails(
//                            R.string.preview_include_unnamed_roads_title,
//                            R.string.preview_include_unnamed_roads_title_description,
//                            textColor
//                        )
//                    },
//                    enabled = { allowCallouts.value },
//                )
            }

            // Search Section
            item(key = "header_search") {
                ExpandableSectionHeader(
                    title = stringResource(R.string.menu_manage_search),
                    expanded = expandedSection.value == "search",
                    onToggle = { expandedSection.value = if (expandedSection.value == "search") null else "search" },
                    textColor = textColor
                )
            }
            if (expandedSection.value == "search") {
                listPreference(
                    key = MainActivity.GEOCODER_MODE_KEY,
                    defaultValue = MainActivity.GEOCODER_MODE_DEFAULT,
                    values = geocoderValues,
                    modifier = expandedSectionModifier,
                    title = {
                        SettingDetails(
                            R.string.settings_section_search_network,
                            R.string.settings_section_search_network_description,
                            textColor
                        )
                    },
                    item = { value, currentValue, onClick ->
                        ListPreferenceItem(geocoderDescriptions[geocoderValues.indexOf(value)], value, currentValue, onClick, geocoderValues.indexOf(value), geocoderValues.size)
                    },
                    summary = {
                        Text(
                            text = geocoderDescriptions[geocoderValues.indexOf(it)],
                            color = textColor,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                )

                listPreference(
                    key = MainActivity.SEARCH_LANGUAGE_KEY,
                    defaultValue = MainActivity.SEARCH_LANGUAGE_DEFAULT,
                    values = searchLanguageValues,
                    modifier = expandedSectionModifier,
                    title = {
                        SettingDetails(
                            R.string.settings_search_results_language,
                            R.string.settings_search_results_language_description,
                            textColor
                        )
                    },
                    item = { value, currentValue, onClick ->
                        ListPreferenceItem(searchLanguageDescriptions[searchLanguageValues.indexOf(value)], value, currentValue, onClick, searchLanguageValues.indexOf(value), searchLanguageValues.size)
                    },
                    summary = {
                        Text(
                            text = searchLanguageDescriptions[searchLanguageValues.indexOf(it)],
                            color = textColor,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                )
            }

            // Accessibility Section
            item(key = "header_accessibility") {
                ExpandableSectionHeader(
                    title = stringResource(R.string.menu_manage_accessibility),
                    expanded = expandedSection.value == "accessibility",
                    onToggle = { expandedSection.value = if (expandedSection.value == "accessibility") null else "accessibility" },
                    textColor = textColor
                )
            }
            if (expandedSection.value == "accessibility") {
                listPreference(
                    key = MainActivity.THEME_LIGHTNESS_KEY,
                    defaultValue = MainActivity.THEME_LIGHTNESS_DEFAULT,
                    values = themeLightnessValues,
                    modifier = expandedSectionModifier,
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
                    modifier = expandedSectionModifier,
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

                switchPreference(
                    key = MainActivity.SHOW_MAP_KEY,
                    defaultValue = MainActivity.SHOW_MAP_DEFAULT,
                    modifier = expandedSectionModifier,
                    title = {
                        Text(
                            text = stringResource(R.string.settings_show_map),
                            color = textColor
                        )
                    },
                )
            }

            // Storage Section
            item(key = "header_storage") {
                ExpandableSectionHeader(
                    title = stringResource(R.string.offline_map_storage_title),
                    expanded = expandedSection.value == "storage",
                    onToggle = { expandedSection.value = if (expandedSection.value == "storage") null else "storage" },
                    textColor = textColor
                )
            }
            if (expandedSection.value == "storage") {
                item {
                    Column(
                        modifier = expandedSectionModifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.offline_map_storage_description),
                            color = textColor,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(spacing.small)
                        )
                        StorageDropDownMenu(
                            storages = storages,
                            onStorageSelected = onStorageSelected,
                            selectedStorageIndex = selectedStorageIndex,
                            modifier = Modifier.smallPadding()
                        )
                    }
                }
            }

            // Audio Section
            item(key = "header_audio") {
                ExpandableSectionHeader(
                    title = stringResource(R.string.menu_manage_audio),
                    expanded = expandedSection.value == "audio",
                    onToggle = { expandedSection.value = if (expandedSection.value == "audio") null else "audio" },
                    textColor = textColor
                )
            }
            if (expandedSection.value == "audio") {
                listPreference(
                    key = MainActivity.BEACON_TYPE_KEY,
                    defaultValue = MainActivity.BEACON_TYPE_DEFAULT,
                    values = beaconValues,
                    modifier = expandedSectionModifier,
                    title = {
                        SettingDetails(
                            R.string.beacon_settings_style,
                            R.string.beacon_settings_style_description,
                            textColor
                        )
                    },
                    item = { value, currentValue, onClick ->
                        ListPreferenceItem(beaconDescriptions[beaconValues.indexOf(value)], value, currentValue, onClick, beaconValues.indexOf(value), beaconValues.size)
                    },
                    summary = { Text(text = it, color = textColor) },
                )

                listPreference(
                    key = MainActivity.SPEECH_ENGINE_KEY,
                    defaultValue = MainActivity.SPEECH_ENGINE_DEFAULT,
                    values = uiState.engineTypes,
                    modifier = expandedSectionModifier,
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
                    modifier = expandedSectionModifier,
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
                    modifier = expandedSectionModifier,
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

            // Language Section
            item(key = "header_language") {
                ExpandableSectionHeader(
                    title = stringResource(R.string.menu_manage_language),
                    expanded = expandedSection.value == "language",
                    onToggle = { expandedSection.value = if (expandedSection.value == "language") null else "language" },
                    textColor = textColor
                )
            }
            if (expandedSection.value == "language") {
                listPreference(
                    key = MainActivity.MEASUREMENT_UNITS_KEY,
                    defaultValue = MainActivity.MEASUREMENT_UNITS_DEFAULT,
                    values = unitsValues,
                    modifier = expandedSectionModifier,
                    title = {
                        SettingDetails(
                            R.string.settings_section_units,
                            R.string.settings_section_units_description,
                            textColor
                        )
                    },
                    item = { value, currentValue, onClick ->
                        ListPreferenceItem(unitsDescriptions[unitsValues.indexOf(value)], value, currentValue, onClick, unitsValues.indexOf(value), unitsValues.size)
                    },
                    summary = {
                        Text(
                            text = unitsDescriptions[unitsValues.indexOf(it)],
                            color = textColor,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                )

                item {
                    Column(
                        modifier = expandedSectionModifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.first_launch_change_language),
                            color = textColor,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        LanguageDropDownMenu(
                            allLanguages = supportedLanguages,
                            onLanguageSelected = onLanguageSelected,
                            selectedLanguageIndex = selectedLanguageIndex,
                            modifier = Modifier.smallPadding()
                        )
                    }
                }
            }

            // Debug Section
            item(key = "header_debug") {
                ExpandableSectionHeader(
                    title = stringResource(R.string.settings_debug_heading),
                    expanded = expandedSection.value == "debug",
                    onToggle = { expandedSection.value = if (expandedSection.value == "debug") null else "debug" },
                    textColor = textColor
                )
            }
            if (expandedSection.value == "debug") {
                switchPreference(
                    key = MainActivity.RECORD_TRAVEL_KEY,
                    defaultValue = MainActivity.RECORD_TRAVEL_DEFAULT,
                    modifier = expandedSectionModifier,
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
                    modifier = expandedSectionModifier,
                    title = {
                        Text(
                            text = stringResource(R.string.settings_accessible_map),
                            color = textColor
                        )
                    },
                )
                item {
                    Column(
                        modifier = expandedSectionModifier.fillMaxWidth()
                    ) {
                        CustomButton(
                            onClick = { navController.navigate(HomeRoutes.AdvancedMarkersAndRoutesSettings.route) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .mediumPadding(),
                            shape = RoundedCornerShape(spacing.extraSmall),
                            text = stringResource(R.string.menu_advanced_markers_and_routes),
                            textStyle = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        CustomButton(
                            onClick = {
                                showConfirmationDialog.value = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .mediumPadding()
                                .talkbackHint(stringResource(R.string.settings_reset_button_hint)),
                            buttonColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            shape = RoundedCornerShape(spacing.small),
                            text = stringResource(R.string.settings_reset_button),
                            textStyle = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsPreview(expandedSection: String) {
    Settings(
        rememberNavController(),
        SettingsViewModel.SettingsUiState(),
        modifier = Modifier,
        MockLanguagePreviewData.languages,
        {},
        4,
        MockStoragePreviewData.storages,
        {},
        0,
        resetSettings = {},
        previewExpandedSection = expandedSection
    )
}

@Preview
@Composable
fun SettingsPreviewCallouts() {
    SettingsPreview("callouts")
}
@Preview
@Composable
fun SettingsPreviewSearch() {
    SettingsPreview("search")
}
@Preview
@Composable
fun SettingsPreviewAccessibility() {
    SettingsPreview("accessibility")
}
@Preview
@Composable
fun SettingsPreviewOfflineMaps() {
    SettingsPreview("storage")
}
@Preview
@Composable
fun SettingsPreviewAudio() {
    SettingsPreview("audio")
}
@Preview
@Composable
fun SettingsPreviewLanguage() {
    SettingsPreview("language")
}
@Preview
@Composable
fun SettingsPreviewDebug() {
    SettingsPreview("debug")
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
