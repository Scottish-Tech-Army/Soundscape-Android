package org.scottishtecharmy.soundscape.screens.home.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.zhanghai.compose.preference.Preference
import me.zhanghai.compose.preference.rememberPreferenceState
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.general_alert_cancel
import org.scottishtecharmy.soundscape.resources.general_alert_done
import org.scottishtecharmy.soundscape.resources.settings_keep_value
import org.scottishtecharmy.soundscape.resources.settings_theme_auto
import org.scottishtecharmy.soundscape.resources.settings_use_value
import org.scottishtecharmy.soundscape.resources.voice_voices
import org.scottishtecharmy.soundscape.screens.onboarding.accessibility.isScreenReaderEnabled
import org.scottishtecharmy.soundscape.screens.talkbackHint
import org.scottishtecharmy.soundscape.platform.testTagsAsResourceId
import org.scottishtecharmy.soundscape.ui.theme.extraSmallPadding
import org.scottishtecharmy.soundscape.ui.theme.spacing

/**
 * A voice as the picker shows it. Each platform builds these from its own TTS voices, in the order
 * they should be listed: the app's own language first, and within a language the ordinary voices
 * before any provider's.
 */
data class TtsVoiceOption(
    /** What [VoicesPreference] stores when the voice is chosen. */
    val identifier: String,
    /** The voice's name. Only needs to be unique within its language and provider. */
    val name: String,
    /** The voice's language named in the app's own language, e.g. "Finnish (Finland)". */
    val languageDisplayName: String,
    /** Whether this voice speaks the language the app itself is in. */
    val inAppLanguage: Boolean,
    /**
     * The heading of the provider sub-list this voice folds away into, or null to list it directly
     * under its language.
     */
    val providerLabel: String? = null,
)

/**
 * The voice picker: Auto, then a collapsible section per language, and inside each language a
 * collapsible sub-list per provider.
 *
 * Auto stores [autoValue], which each platform takes to mean the default voice for the app's
 * language - so it follows the user when they switch the app's language, where a chosen voice
 * stays put.
 *
 * An iPhone reports upwards of sixty voices of its own, and a user who installs a speech provider
 * app like eSpeak can have many hundreds; an Android engine with every language pack installed
 * has hundreds too. Two levels of grouping are what keep that navigable, and they're the two
 * things a name on its own doesn't tell you:
 *
 * - **Language**, because names repeat across languages - iOS ships a Flo and an Eddy in every
 *   language Eloquence covers - so a name only identifies a voice once you know its language.
 * - **Provider**, because they sound nothing like each other and arrive in bulk. Ordinary voices
 *   stay directly under the language, where someone looking for one will meet them first;
 *   Eloquence, eSpeak and anything else the user installed on iOS each fold away into their own
 *   sub-list. This mirrors the Soundscape community app's voice settings screen. Android has no
 *   sub-lists, because the engine is a setting of its own there and only its voices are listed.
 *
 * Tapping a voice speaks a sample in it and leaves the picker open, so voices can be compared by
 * ear in one sitting - this is the legacy iOS app's preview-on-tap. The choice only reaches
 * preferences when the user confirms it, and cancelling puts the voice that was in use back, so
 * listening around costs nothing.
 *
 * The screen reader reads the tapped row's name whatever this screen does, which can't be helped - see
 * [PREVIEW_DELAY_FOR_SCREEN_READER_MS] for why, and for how the sample keeps out of its way.
 */
@Composable
fun VoicesPreference(
    voices: List<TtsVoiceOption>,
    preferenceKey: String,
    autoValue: String,
    onPreviewVoice: (String) -> Unit,
    onCancelPreviews: () -> Unit,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    var selectedId by rememberPreferenceState(preferenceKey, autoValue)
    var showDialog by rememberSaveable { mutableStateOf(false) }
    // What the ticks follow while the dialog is open, and what Done applies: tapping a voice
    // previews it and moves the tick, but the voice in use doesn't change until then.
    var previewedId by remember { mutableStateOf(selectedId) }
    val pendingPreview = remember { PendingPreview() }
    val scope = rememberCoroutineScope()

    fun previewVoice(voiceId: String) {
        previewedId = voiceId
        // Each tap replaces the one before it, so working down a list restarts the wait rather
        // than queueing up a sample per row.
        pendingPreview.job?.cancel()
        pendingPreview.job = scope.launch {
            if (isScreenReaderEnabled()) delay(PREVIEW_DELAY_FOR_SCREEN_READER_MS)
            onPreviewVoice(voiceId)
        }
    }

    val systemDefaultLabel = stringResource(Res.string.settings_theme_auto)

    // Language -> the voices in it, the ordinary ones first and then a run per provider, which is
    // the order the platform listed them in.
    val byLanguage = remember(voices) { voices.groupBy { it.languageDisplayName } }
    val selected = voices.firstOrNull { it.identifier == selectedId }

    // One language at a time, as the settings screen itself does with its sections: with a
    // language's voices running to hundreds once a provider app is installed, leaving two open
    // buries the headings a user navigates by.
    //
    // The one that starts open is the language of the chosen voice, so a user who went looking for
    // a Finnish voice last time is shown their own choice rather than a collapsed heading they
    // have to remember to open; failing that, the app's own, which shapeVoiceList put first.
    var expandedLanguage by remember(voices) {
        mutableStateOf(
            selected?.languageDisplayName
                ?: voices.firstOrNull { it.inAppLanguage }?.languageDisplayName,
        )
    }
    // Provider sub-lists are keyed by language as well as provider, because the same provider
    // appears under every language it covers and opening it under one says nothing about the rest.
    // Only the one holding the chosen voice starts open, for the same reason as above.
    var expandedProviders by remember(voices) {
        mutableStateOf(
            selected?.providerLabel
                ?.let { setOf(providerKey(selected.languageDisplayName, it)) }
                ?: emptySet(),
        )
    }

    // The row below the title has to name the voice on its own, away from the sections that would
    // otherwise say which language and provider it came from, so it carries both.
    val summaryText = selected?.let { voice ->
        listOfNotNull(
            voice.name,
            voice.providerLabel,
            voice.languageDisplayName,
        ).joinToString(", ")
    } ?: systemDefaultLabel

    fun dismissWithoutChoosing() {
        // Drop a sample still waiting on the screen reader before putting the voice back, or it
        // would land afterwards and speak in the one the user has just declined.
        pendingPreview.job?.cancel()
        showDialog = false
        onCancelPreviews()
    }

    Preference(
        title = { SettingTitle(Res.string.voice_voices, textColor) },
        modifier = modifier.testTag("settingsVoices"),
        summary = { ClickableOption(summaryText, textColor) },
        onClick = {
            previewedId = selectedId
            showDialog = true
        },
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { dismissWithoutChoosing() },
            title = {
                Text(
                    text = stringResource(Res.string.voice_voices),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            text = {
                val headingColor = MaterialTheme.colorScheme.onSurface
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item(key = "voice-system-default") {
                        VoiceRow(
                            label = systemDefaultLabel,
                            testTagSuffix = "auto",
                            isSelected = previewedId == autoValue,
                            onClick = { previewVoice(autoValue) },
                        )
                    }

                    byLanguage.forEach { (languageName, voicesInLanguage) ->
                        item(key = "voice-language-$languageName") {
                            val expanded = languageName == expandedLanguage
                            ExpandableSectionHeader(
                                title = languageName,
                                expanded = expanded,
                                onToggle = {
                                    expandedLanguage = if (expanded) null else languageName
                                },
                                textColor = headingColor,
                            )
                        }

                        if (languageName != expandedLanguage) return@forEach

                        val byProvider = voicesInLanguage.groupBy { it.providerLabel }
                        byProvider.forEach { (label, familyVoices) ->
                            if (label == null) {
                                // Ordinary voices: straight under the language, no sub-list.
                                items(
                                    familyVoices,
                                    key = { voice -> voice.identifier },
                                ) { voice ->
                                    VoiceRow(
                                        label = voice.name,
                                        testTagSuffix = voice.identifier,
                                        isSelected = previewedId == voice.identifier,
                                        onClick = { previewVoice(voice.identifier) },
                                    )
                                }
                                return@forEach
                            }

                            val key = providerKey(languageName, label)
                            item(key = "voice-provider-$key") {
                                val expanded = key in expandedProviders
                                ExpandableSectionHeader(
                                    title = label,
                                    expanded = expanded,
                                    onToggle = {
                                        expandedProviders = if (expanded) {
                                            expandedProviders - key
                                        } else {
                                            expandedProviders + key
                                        }
                                    },
                                    textColor = headingColor,
                                    modifier = Modifier.padding(start = spacing.medium),
                                )
                            }
                            if (key in expandedProviders) {
                                items(
                                    familyVoices,
                                    key = { voice -> voice.identifier },
                                ) { voice ->
                                    VoiceRow(
                                        label = voice.name,
                                        testTagSuffix = voice.identifier,
                                        isSelected = previewedId == voice.identifier,
                                        onClick = { previewVoice(voice.identifier) },
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    // Writing this is what the platform's audio service listens for, so the voice
                    // the previews left in the speech engine is now also the stored one.
                    // A sample still waiting has nothing left to say: writing the preference
                    // below is what puts the chosen voice into the renderer.
                    pendingPreview.job?.cancel()
                    selectedId = previewedId
                    showDialog = false
                }) {
                    Text(stringResource(Res.string.general_alert_done))
                }
            },
            dismissButton = {
                TextButton(onClick = { dismissWithoutChoosing() }) {
                    Text(stringResource(Res.string.general_alert_cancel))
                }
            },
        )
    }
}

/**
 * A voice in the picker. Deliberately not [ListPreferenceItem], because a choice here behaves
 * differently from every other list setting: tapping previews the voice rather than choosing it,
 * and the choice isn't made until Done. [isSelected] is therefore the voice last tapped - what
 * Done would apply - rather than the one the app is speaking with, which the row below the
 * setting's title names instead.
 *
 * The hint is the one the other list settings use, so that a choice here is announced like a
 * choice anywhere else in Settings.
 */
@Composable
private fun VoiceRow(
    label: String,
    testTagSuffix: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            // The choices are shown in a dialog, outside the theme's testTagsAsResourceId.
            .testTagsAsResourceId()
            .testTag("settingsVoiceOption-$testTagSuffix")
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .extraSmallPadding()
            .defaultMinSize(minHeight = spacing.targetSize)
            .semantics { selected = isSelected }
            .clickable(role = Role.Button) { onClick() }
            .talkbackHint(
                if (isSelected) stringResource(Res.string.settings_keep_value)
                else stringResource(Res.string.settings_use_value)
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.CenterVertically).weight(1f),
        )
        Icon(
            modifier = Modifier.align(Alignment.CenterVertically).width(spacing.targetSize),
            imageVector = if (isSelected) Icons.Filled.CheckBox
            else Icons.Filled.CheckBoxOutlineBlank,
            tint = MaterialTheme.colorScheme.onSurface,
            contentDescription = "",
        )
    }
}

private fun providerKey(languageName: String, providerLabel: String) = "$languageName|$providerLabel"

/** The sample waiting to be spoken - see [PREVIEW_DELAY_FOR_SCREEN_READER_MS]. */
private class PendingPreview {
    var job: Job? = null
}

/**
 * How long a sample waits for the screen reader to finish before it is spoken.
 *
 * TalkBack announces the tapped row and its new state itself, as it does for any control. On iOS
 * it is less obvious:
 *
 * Tapping a voice makes VoiceOver read the row's name, and there is no way to stop it from the
 * app. Compose Multiplatform's accessibility bridge finishes every sync of the semantics tree by
 * posting UIAccessibilityLayoutChangedNotification, whatever changed - and with VoiceOver focused
 * on a row that posts with a nil argument, which is iOS's cue to re-read the focused element's
 * label. Any layout change anywhere in the composition is enough to cause a sync, and speaking
 * the sample is itself enough to cause one, so nothing about this row or its semantics can avoid
 * it (AccessibilityMediator.sync/updateFocusedElement in Accessibility.ios.kt, 1.13.0-beta02).
 *
 * So the sample gives way instead of competing: the row is announced, then the voice speaks.
 *
 * Long enough for the row's name and its hint, which is more than the community app's own gap has
 * to cover. Only waits when a screen reader is actually running, so that comparing voices by ear
 * stays as quick as tapping for everyone else.
 */
private const val PREVIEW_DELAY_FOR_SCREEN_READER_MS = 2_000L
