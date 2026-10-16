package org.scottishtecharmy.soundscape.platform

import androidx.compose.ui.Modifier

/**
 * Exposes the testTags in this subtree as Android resource-ids, so that UiAutomator-based tools
 * such as Maestro can find them. The app theme sets this once at its root, but a Dialog is a
 * separate window with its own composition root, so content shown in one (the full-screen search,
 * a setting's list of choices) has to set it again. Does nothing on iOS.
 */
expect fun Modifier.testTagsAsResourceId(): Modifier
