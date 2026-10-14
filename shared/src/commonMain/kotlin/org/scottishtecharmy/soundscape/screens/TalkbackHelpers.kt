package org.scottishtecharmy.soundscape.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics

@Composable
expect fun Modifier.talkbackHint(hint: String): Modifier

@Composable
fun Modifier.talkbackDescription(contentDescription: String) =
    semantics {
        this.contentDescription = contentDescription
    }

@Composable
fun Modifier.talkbackHidden() =
    semantics {
        hideFromAccessibility()
    }

@Composable
fun Modifier.talkbackLive() =
    semantics {
        liveRegion = LiveRegionMode.Polite
    }

/**
 * Make a text field's screen reader paste call [onPaste] with the clipboard text, so that the
 * caller can act on what was pasted. [onPaste] isn't called when the clipboard holds no text.
 *
 * The two screen readers need opposite treatment. TalkBack has its own Paste action for any
 * Compose text field, so Android overrides it - the field's own paste does no more than insert
 * the text, and doesn't tell us that it happened. VoiceOver only offers Copy/Cut/Paste for a view
 * implementing UITextInput, and Compose Multiplatform draws its text fields on a Metal view and
 * describes them to VoiceOver as plain accessibility elements, so there's no Edit rotor to paste
 * from at all; iOS adds a custom action instead, which is the only editing action the Compose iOS
 * accessibility bridge passes on, and which appears in the rotor's Actions.
 *
 * Either way the text is appended to the field rather than inserted at the cursor, which is all
 * the string-valued [androidx.compose.foundation.text.BasicTextField] can tell us how to do.
 * Pasting by hand, from the touch text selection menu, is untouched and still inserts.
 */
@Composable
expect fun Modifier.talkbackPasteAction(onPaste: (String) -> Unit): Modifier
