package org.scottishtecharmy.soundscape.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.general_loading_start
import org.scottishtecharmy.soundscape.screens.talkbackHidden
import org.scottishtecharmy.soundscape.ui.theme.spacing

/**
 * A small spinner drawn on top of the row or button that started a slow action, such as looking
 * up the address of the current location. Draw it over the row (in a Box) rather than in place
 * of it: if the row TalkBack is focused on disappears, focus jumps back to the top of the screen.
 *
 * The spinner is hidden from screen readers unless the action is still running after
 * [announceAfterMillis], when it announces "Getting things ready…" once, politely, without moving
 * focus. A quick action stays silent and the next screen announces itself.
 */
@Composable
fun SlowLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.circularColor,
    announceAfterMillis: Long = 3000,
) {
    var announceLoading by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(announceAfterMillis)
        announceLoading = true
    }
    val loadingLabel = stringResource(Res.string.general_loading_start)
    CircularProgressIndicator(
        color = color,
        modifier = modifier
            .size(spacing.medium)
            .then(
                if (announceLoading) Modifier.semantics {
                    contentDescription = loadingLabel
                    liveRegion = LiveRegionMode.Polite
                } else Modifier.talkbackHidden()
            ),
    )
}
