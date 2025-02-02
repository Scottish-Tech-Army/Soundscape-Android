package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.scottishtecharmy.soundscape.components.NavigationButton
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewState
import org.scottishtecharmy.soundscape.geoengine.utils.calculateHeadingOffset

@Composable
fun StreetPreview(
    state: StreetPreviewState,
    heading: Float,
    go: () -> Unit,
    exit: () -> Unit
) {
    // Find best match
    var bestChoice = -1
    var bestHeading = Double.POSITIVE_INFINITY
    val roads = emptySet<String>().toMutableSet()
    for((index, choice) in state.choices.withIndex()) {
        val headingDelta = calculateHeadingOffset(choice.heading, heading.toDouble())
        if(headingDelta < bestHeading){
            bestHeading = headingDelta
            bestChoice = index
        }
        roads.add(choice.name)
    }

    // Create intersection description
    var intersectionText = ""
    var lastName: String? = null
    for(road in roads) {
        if(lastName != null) {
            if(intersectionText.isNotEmpty()) intersectionText += ", "
            intersectionText += "$lastName"
        }
        lastName = road
    }
    if(lastName != null) {
        if (intersectionText.isEmpty()) {
            intersectionText = lastName
        }
        else {
            intersectionText += " and $lastName"
        }
    }
    // TODO: Internationalization
    var text = "Go"
    if(bestChoice != -1) {
        text += " - " + state.choices[bestChoice].name
    }

    // TODO: Internationalization
    Text(text = "At $intersectionText")

    NavigationButton(
        onClick = {
            go()
        },
        text = text
    )
    NavigationButton(
        onClick = {
            exit()
        },
        // TODO: Internationalization
        text = "Exit street preview"
    )
}