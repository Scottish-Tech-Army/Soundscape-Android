package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.components.NavigationButton
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewChoice
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewEnabled
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewState
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.utils.calculateHeadingOffset
import org.scottishtecharmy.soundscape.geoengine.utils.getCompassLabel
import org.scottishtecharmy.soundscape.screens.home.StreetPreviewFunctions
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme
import org.scottishtecharmy.soundscape.ui.theme.mediumPadding
import org.scottishtecharmy.soundscape.ui.theme.spacing

@Composable
fun StreetPreview(
    state: StreetPreviewState,
    heading: Float,
    streetPreviewFunctions: StreetPreviewFunctions
) {
    Column {
        if (state.enabled == StreetPreviewEnabled.INITIALIZING) {
            Text(
                text = stringResource(R.string.general_loading_start),
                Modifier.mediumPadding()
            )
        } else {
            // Find best match
            var bestChoice = -1
            var bestHeading = Double.POSITIVE_INFINITY
            val roads = emptySet<String>().toMutableSet()
            for ((index, choice) in state.choices.withIndex()) {
                val headingDelta = calculateHeadingOffset(choice.heading, heading.toDouble())
                if (headingDelta < bestHeading) {
                    bestHeading = headingDelta
                    bestChoice = index
                }
                roads.add(choice.name)
            }

            // Create intersection description
            var intersectionText = ""
            var lastName: String? = null
            for (road in roads) {
                if (lastName != null) {
                    if (intersectionText.isNotEmpty()) intersectionText += ", "
                    intersectionText += lastName
                }
                lastName = road
            }
            if (lastName != null) {
                if (intersectionText.isEmpty()) {
                    intersectionText = lastName
                } else {
                    intersectionText += stringResource(R.string.last_entry_in_list).format(lastName)
                }
            }

            val text = if (bestChoice != -1)
                stringResource(R.string.preview_go_title) + " - " + state.choices[bestChoice].name + " " + stringResource(
                    getCompassLabel(state.choices[bestChoice].heading.toInt())
                )
            else stringResource(R.string.preview_go_nearest_intersection)

            if (intersectionText.isNotEmpty()) {
                Text(text = stringResource(R.string.directions_at_poi).format(intersectionText))
            }
            NavigationButton(
                onClick = {
                    streetPreviewFunctions.go()
                },
                text = text
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
            90.0F,
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
                )
            ),
            179.0F,
            StreetPreviewFunctions(null)
        )
    }
}
