package org.scottishtecharmy.soundscape.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Wraps a control whose activation immediately starts Soundscape's own speech - a callout,
 * a beacon announcement - so a screen reader shouldn't be talking at the same time.
 *
 * On iOS this swaps the Compose accessibility element for a native UIView proxy carrying
 * `UIAccessibilityTraitStartsMediaSession`, which is how iOS is told to silence VoiceOver
 * for the media session the activation starts. Compose Multiplatform derives iOS traits
 * from semantics and has no hook for that trait, hence the interop proxy.
 *
 * A plain layout wrapper on Android: TalkBack stops speaking as soon as the app speaks.
 *
 * @param label what a screen reader announces, normally the control's visible text.
 * @param hint what activating the control does, as a bare fragment ("hear about your current
 * location") in the same style [Modifier.talkbackHint] takes. iOS phrases it as "Double tap
 * to ..." for VoiceOver, which reads a hint verbatim; TalkBack composes that phrasing itself
 * from the control's own onClick label.
 * @param identifier accessibilityIdentifier / testTag, for UI automation.
 * @param onActivate what a screen reader activation should do. Must match the wrapped
 * control's own onClick, because on iOS it replaces it.
 * @param content the real control. MUST apply the [Modifier] it is handed.
 */
@Composable
expect fun StartsSpeechControl(
    label: String,
    hint: String,
    identifier: String? = null,
    onActivate: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
)
