package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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

            val intersectionText = when {
                roads.isEmpty() -> ""
                roads.size == 1 -> roads.first()
                else -> roads.dropLast(1).joinToString(", ") +
                    stringResource(Res.string.last_entry_in_list, roads.last())
            }

            if (intersectionText.isNotEmpty()) {
                Text(text = stringResource(Res.string.directions_at_poi, intersectionText))
            }
            NavigationButton(
                onClick = { streetPreviewFunctions.go() },
                text = stringResource(Res.string.preview_go_title),
                modifier = Modifier.talkbackHint(stringResource(Res.string.preview_go_hint)),
            )
        }
    }
}
