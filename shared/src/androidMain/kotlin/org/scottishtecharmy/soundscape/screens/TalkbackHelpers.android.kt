package org.scottishtecharmy.soundscape.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics

@Composable
actual fun Modifier.talkbackHint(hint: String): Modifier =
    semantics {
        onClick(label = hint, action = { false })
    }
