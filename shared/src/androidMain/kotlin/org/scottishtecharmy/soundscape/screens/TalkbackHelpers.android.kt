package org.scottishtecharmy.soundscape.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.pasteText
import androidx.compose.ui.semantics.semantics

@Composable
actual fun Modifier.talkbackHint(hint: String): Modifier =
    semantics {
        onClick(label = hint, action = { false })
    }

@Composable
actual fun Modifier.talkbackPasteAction(onPaste: (String) -> Unit): Modifier {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current.nativeClipboard
    return semantics {
        pasteText {
            val pasted = clipboard.primaryClip
                ?.takeIf { it.itemCount > 0 }
                ?.getItemAt(0)
                ?.coerceToText(context)
                ?.toString()
            if (pasted.isNullOrEmpty()) {
                false
            } else {
                onPaste(pasted)
                true
            }
        }
    }
}
