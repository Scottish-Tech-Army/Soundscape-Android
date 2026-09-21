package org.scottishtecharmy.soundscape.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.talkback_default_activate
import org.scottishtecharmy.soundscape.resources.talkback_double_tap_template

@Composable
actual fun Modifier.talkbackHint(hint: String): Modifier {
    val label = activationHint(hint)
    return semantics {
        onClick(label = label, action = { false })
    }
}

/**
 * Phrases a bare hint fragment ("hear about your current location") as a full VoiceOver hint
 * ("Double tap to hear about your current location"), falling back to a generic verb when the
 * caller has no fragment.
 *
 * TalkBack composes this phrasing itself from an onClick action's label, but VoiceOver reads
 * the label verbatim, so iOS has to supply it. Shared with [StartsSpeechControl], whose native
 * accessibility proxy sets the hint on a UIView instead of through Compose semantics - keeping
 * both spellings of "the iOS hint" in one place.
 */
@Composable
internal fun activationHint(hint: String): String {
    val fallback = stringResource(Res.string.talkback_default_activate)
    return stringResource(Res.string.talkback_double_tap_template, hint.ifEmpty { fallback })
}
