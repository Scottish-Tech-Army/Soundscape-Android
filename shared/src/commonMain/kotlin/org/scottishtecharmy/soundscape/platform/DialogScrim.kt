package org.scottishtecharmy.soundscape.platform

import androidx.compose.runtime.Composable

/**
 * Call from inside a Dialog's content to stop the platform dimming the screen behind it.
 * Used by the full-screen search, where the Android dim fading out on dismiss shows as a
 * flash over the home screen.
 */
@Composable
expect fun ClearDialogScrim()
