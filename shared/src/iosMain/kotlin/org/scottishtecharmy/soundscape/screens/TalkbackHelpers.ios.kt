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
    val fallback = stringResource(Res.string.talkback_default_activate)
    val effective = hint.ifEmpty { fallback }
    val label = stringResource(Res.string.talkback_double_tap_template, effective)
    return semantics {
        onClick(label = label, action = { false })
    }
}
