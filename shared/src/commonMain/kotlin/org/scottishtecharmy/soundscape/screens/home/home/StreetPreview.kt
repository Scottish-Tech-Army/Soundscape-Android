package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.components.NavigationButton
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewEnabled
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewState
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.directions_at_poi
import org.scottishtecharmy.soundscape.resources.general_loading_start
import org.scottishtecharmy.soundscape.resources.last_entry_in_list
import org.scottishtecharmy.soundscape.resources.preview_go_hint
import org.scottishtecharmy.soundscape.resources.preview_go_title
import org.scottishtecharmy.soundscape.screens.talkbackHint
import org.scottishtecharmy.soundscape.ui.theme.mediumPadding

@Composable
fun StreetPreview(
    state: StreetPreviewState,
    streetPreviewFunctions: StreetPreviewFunctions,
) {
    Column {
        if (state.enabled == StreetPreviewEnabled.INITIALIZING) {
            Text(
                text = stringResource(Res.string.general_loading_start),
                Modifier.mediumPadding(),
            )
        } else {
            val roads = remember(state.choices) {
                state.choices.map { it.name }.distinct()
            }

            val lastEntry = roads.lastOrNull()?.let { stringResource(Res.string.last_entry_in_list, it) }
            val intersectionText = joinRoadNames(roads, lastEntry)

            if (intersectionText.isNotEmpty()) {
                Text(text = stringResource(Res.string.directions_at_poi, intersectionText))
            }
            NavigationButton(
                onClick = { streetPreviewFunctions.go() },
                text = stringResource(Res.string.preview_go_title),
                modifier = Modifier
                    .talkbackHint(stringResource(Res.string.preview_go_hint))
                    .testTag("streetPreviewGo"),
            )
        }
    }
}

/**
 * "A, B and C": every road but the last joined with commas, then [lastEntry], which is
 * last_entry_in_list already filled in with the last road (" and C").
 *
 * The English string starts with a space, but about half the translations had lost it, which gave
 * "Bdan C" on screen. So the space is added here rather than trusted to the strings.
 */
internal fun joinRoadNames(roads: List<String>, lastEntry: String?): String = when {
    roads.isEmpty() -> ""
    roads.size == 1 || lastEntry == null -> roads.first()
    else -> roads.dropLast(1).joinToString(", ") + " " + lastEntry.trimStart()
}
