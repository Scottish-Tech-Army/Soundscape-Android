package org.scottishtecharmy.soundscape.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties

/**
 * The properties for a Dialog that fills the whole screen, like the full-screen search.
 *
 * On Android the dialog's window is laid out edge to edge. Otherwise, on Android 11 and 12, the
 * keyboard opening shrinks the window while Compose still measures the content at the full
 * screen height and centres it, which puts the top of the content - the search text field - off
 * the top of the window. The content has to keep clear of the system bars itself, with
 * [fullScreenDialogSystemBarsPadding].
 */
expect fun fullScreenDialogProperties(): DialogProperties

/**
 * Keeps the content of a Dialog using [fullScreenDialogProperties] clear of the system bars.
 * Apply it inside the dialog's background, so that the background is drawn behind the bars.
 */
@Composable
expect fun Modifier.fullScreenDialogSystemBarsPadding(): Modifier
