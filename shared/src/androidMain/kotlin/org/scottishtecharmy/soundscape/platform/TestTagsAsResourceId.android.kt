package org.scottishtecharmy.soundscape.platform

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId as testTagsAsResourceIdProperty

actual fun Modifier.testTagsAsResourceId(): Modifier =
    semantics { testTagsAsResourceIdProperty = true }
