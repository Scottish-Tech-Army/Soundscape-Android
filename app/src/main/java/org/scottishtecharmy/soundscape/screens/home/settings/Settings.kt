package org.scottishtecharmy.soundscape.screens.home.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import me.zhanghai.compose.preference.listPreference
import me.zhanghai.compose.preference.switchPreference
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.scottishtecharmy.soundscape.MainActivity
import org.scottishtecharmy.soundscape.navigation.SharedRoutes
import org.scottishtecharmy.soundscape.preferences.PreferencesProvider
import org.scottishtecharmy.soundscape.resources.*
import org.scottishtecharmy.soundscape.screens.onboarding.offlinestorage.StorageDropDownMenu
import org.scottishtecharmy.soundscape.ui.theme.smallPadding
import org.scottishtecharmy.soundscape.ui.theme.spacing
import org.scottishtecharmy.soundscape.utils.StorageUtils
import org.scottishtecharmy.soundscape.viewmodels.SettingsViewModel

@Composable
fun Settings(
    navController: NavHostController,
    uiState: SettingsViewModel.SettingsUiState,
    modifier: Modifier = Modifier,
    storages: List<StorageUtils.StorageSpace>,
    onStorageSelected: (String) -> Unit,
    selectedStorageIndex: Int,
    onResetSettings: (() -> Unit)?,
    onBeaconPreviewStart: ((String) -> Unit)? = null,
    onBeaconPreviewUpdate: ((String) -> Unit)? = null,
    onBeaconPreviewStop: ((Boolean, String?) -> Unit)? = null,
    previewExpandedSection: String? = null,
) {
    val beaconValues = uiState.beaconValues
    val beaconDescriptions = uiState.beaconDescriptions.map { stringResource(it) }

    val textColor = MaterialTheme.colorScheme.onBackground
    val expandedSectionModifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)

    // Android-only value lists
    val themeContrastDescriptions = listOf(
        stringResource(Res.string.settings_theme_contrast_regular),
        stringResource(Res.string.settings_theme_contrast_medium),
        stringResource(Res.string.settings_theme_contrast_high),
    )
    val themeContrastValues = listOf("Regular", "Medium", "High")

    val themeLightnessDescriptions = listOf(
        stringResource(Res.string.settings_theme_auto),
        stringResource(Res.string.settings_theme_light),
        stringResource(Res.string.settings_theme_dark),
    )
    val themeLightnessValues = listOf("Auto", "Light", "Dark")

    val mediaControlsDescriptions = listOf(
        stringResource(Res.string.settings_media_controls_original),
        stringResource(Res.string.settings_media_controls_voice_command),
        stringResource(Res.string.settings_media_controls_audio_menu),
    )
    val mediaControlsValues = listOf("Original", "VoiceControl", "AudioMenu")

    val microphoneDescriptions = uiState.microphoneDescriptions
    val microphoneValues = uiState.microphoneValues

    val preferencesProvider: PreferencesProvider = koinInject()

    SharedSettingsScreen(
        onNavigateUp = { navController.navigateUp() },
        beaconTypes = beaconValues,
        preferencesProvider = preferencesProvider,
        mediaControlsValues = mediaControlsValues,
        mediaControlsDescriptions = mediaControlsDescriptions,
        onNavigateToAdvancedMarkersAndRoutes = {
            navController.navigate(SharedRoutes.ADVANCED_MARKERS_AND_ROUTES_SETTINGS)
        },
        onResetSettings = onResetSettings,
        onBeaconPreviewStart = onBeaconPreviewStart,
        onBeaconPreviewUpdate = onBeaconPreviewUpdate,
        onBeaconPreviewStop = onBeaconPreviewStop,
        modifier = modifier,

        platformAccessibilityContent = {
            listPreference(
                key = MainActivity.THEME_LIGHTNESS_KEY,
                defaultValue = MainActivity.THEME_LIGHTNESS_DEFAULT,
                values = themeLightnessValues,
                modifier = expandedSectionModifier,
                title = {
                    Text(
                        text = stringResource(Res.string.settings_theme_light_dark),
                        color = textColor
                    )
                },
                item = { value, currentValue, onClick ->
                    ListPreferenceItem(
                        themeLightnessDescriptions[themeLightnessValues.indexOf(value)],
                        value,
                        currentValue,
                        onClick,
                        themeLightnessValues.indexOf(value),
                        themeLightnessValues.size
                    )
                },
                summary = {
                    ClickableOption(
                        themeLightnessDescriptions[themeLightnessValues.indexOf(
                            it
                        )], textColor
                    )
                },
            )
            listPreference(
                key = MainActivity.THEME_CONTRAST_KEY,
                defaultValue = MainActivity.THEME_CONTRAST_DEFAULT,
                values = themeContrastValues,
                modifier = expandedSectionModifier,
                title = {
                    Text(
                        text = stringResource(Res.string.settings_theme_contrast),
                        color = textColor
                    )
                },
                item = { value, currentValue, onClick ->
                    ListPreferenceItem(
                        themeContrastDescriptions[themeContrastValues.indexOf(value)],
                        value,
                        currentValue,
                        onClick,
                        themeContrastValues.indexOf(value),
                        themeContrastValues.size
                    )
                },
                summary = {
                    ClickableOption(
                        themeContrastDescriptions[themeContrastValues.indexOf(it)],
                        textColor
                    )
                },
            )
        },

        platformStorageContent = {
            item {
                Column(modifier = expandedSectionModifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(Res.string.offline_map_storage_description),
                        color = textColor,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(spacing.small),
                    )
                    StorageDropDownMenu(
                        storages = storages,
                        onStorageSelected = onStorageSelected,
                        selectedStorageIndex = selectedStorageIndex,
                        modifier = Modifier.smallPadding(),
                        backgroundColor = MaterialTheme.colorScheme.surface,
                    )
                }
            }
        },

        platformAudioContent = {
            listPreference(
                key = MainActivity.SPEECH_ENGINE_KEY,
                defaultValue = MainActivity.SPEECH_ENGINE_DEFAULT,
                values = uiState.engineTypes,
                modifier = expandedSectionModifier,
                title = { Text(text = stringResource(Res.string.voice_engine), color = textColor) },
                item = { value, currentValue, onClick ->
                    ListPreferenceItem(
                        value.substringBefore(":::"),
                        value,
                        currentValue,
                        onClick,
                        uiState.engineTypes.indexOf(value),
                        uiState.engineTypes.size
                    )
                },
                summary = { ClickableOption(it.substringBefore(":::"), textColor) },
            )
            listPreference(
                key = MainActivity.VOICE_TYPE_KEY,
                defaultValue = MainActivity.VOICE_TYPE_DEFAULT,
                values = uiState.voiceTypes,
                modifier = expandedSectionModifier,
                title = { Text(text = stringResource(Res.string.voice_voices), color = textColor) },
                item = { value, currentValue, onClick ->
                    ListPreferenceItem(
                        value,
                        value,
                        currentValue,
                        onClick,
                        uiState.voiceTypes.indexOf(value),
                        uiState.voiceTypes.size
                    )
                },
                summary = { ClickableOption(it, textColor) },
            )
        },

        platformMediaControlsContent = {
            switchPreference(
                key = MainActivity.VOICE_COMMAND_LISTENING_PROMPT_KEY,
                defaultValue = MainActivity.VOICE_COMMAND_LISTENING_PROMPT_DEFAULT,
                modifier = expandedSectionModifier,
                title = {
                    SettingDetails(
                        Res.string.settings_voice_command_listening_prompt,
                        Res.string.settings_voice_command_listening_prompt_description,
                        textColor
                    )
                },
            )
            listPreference(
                key = MainActivity.VOICE_COMMAND_MICROPHONE_KEY,
                defaultValue = MainActivity.VOICE_COMMAND_MICROPHONE_DEFAULT,
                values = microphoneValues,
                modifier = expandedSectionModifier,
                title = {
                    SettingDetails(
                        Res.string.settings_voice_command_microphone,
                        Res.string.settings_voice_command_microphone_description,
                        textColor
                    )
                },
                item = { value, currentValue, onClick ->
                    val idx = microphoneValues.indexOf(value).coerceAtLeast(0)
                    ListPreferenceItem(
                        microphoneDescriptions[idx],
                        value,
                        currentValue,
                        onClick,
                        idx,
                        microphoneValues.size
                    )
                },
                summary = {
                    val idx = microphoneValues.indexOf(it)
                    val label = if (idx >= 0) microphoneDescriptions[idx] else it
                    ClickableOption(label, textColor)
                },
            )
        },

        )
}
