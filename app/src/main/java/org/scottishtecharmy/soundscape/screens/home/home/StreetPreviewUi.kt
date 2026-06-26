package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.components.NavigationButton
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewChoice
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewEnabled
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewState
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.utils.getCompassLabel
import org.scottishtecharmy.soundscape.screens.home.StreetPreviewFunctions
import org.scottishtecharmy.soundscape.screens.talkbackHint
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme
import org.scottishtecharmy.soundscape.ui.theme.mediumPadding

@Composable
fun StreetPreview(
    state: StreetPreviewState,
    streetPreviewFunctions: StreetPreviewFunctions
) {
    Column {
        if (state.enabled == StreetPreviewEnabled.INITIALIZING) {
            Text(
                text = stringResource(R.string.general_loading_start),
                Modifier.mediumPadding()
            )
        } else {
            val roads = remember(state.choices) {
                state.choices.map { it.name }.distinct()
            }

            val intersectionText = when {
                roads.isEmpty() -> ""
                roads.size == 1 -> roads.first()
                else -> roads.dropLast(1).joinToString(", ") +
                    stringResource(R.string.last_entry_in_list).format(roads.last())
            }

            if (intersectionText.isNotEmpty()) {
                Text(text = stringResource(R.string.directions_at_poi).format(intersectionText))
            }
            NavigationButton(
                onClick = {
                    streetPreviewFunctions.go()
                },
                text = stringResource(R.string.preview_go_title),
                modifier = Modifier.talkbackHint(stringResource(R.string.preview_go_hint))
            )
        }
    }
}


@Preview
@Composable
fun StreetPreviewInitializingPreview() {
    SoundscapeTheme {
        StreetPreview(
            StreetPreviewState(
                StreetPreviewEnabled.INITIALIZING,
                emptyList()
            ),
            StreetPreviewFunctions(null)
        )
    }
}

@Preview
@Composable
fun StreetPreviewOnPreview() {
    SoundscapeTheme {
        StreetPreview(
            StreetPreviewState(
                StreetPreviewEnabled.ON,
                listOf(
                    StreetPreviewChoice(90.0, "Main Street", Way()),
                    StreetPreviewChoice(-90.0, "Main Street", Way()),
                    StreetPreviewChoice(0.0, "1st Street", Way()),
                ),
                bestChoice = StreetPreviewChoice(90.0, "Main Street", Way())
            ),
            StreetPreviewFunctions(null)
        )
    }
}
