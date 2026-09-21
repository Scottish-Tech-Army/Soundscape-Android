package org.scottishtecharmy.soundscape.screens.markers_routes.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.markers_sort_action_distance_ascending
import org.scottishtecharmy.soundscape.resources.markers_sort_action_distance_descending
import org.scottishtecharmy.soundscape.resources.markers_sort_action_name_ascending
import org.scottishtecharmy.soundscape.resources.markers_sort_action_name_descending
import org.scottishtecharmy.soundscape.resources.markers_sort_distance_ascending
import org.scottishtecharmy.soundscape.resources.markers_sort_distance_descending
import org.scottishtecharmy.soundscape.resources.markers_sort_name_ascending
import org.scottishtecharmy.soundscape.resources.markers_sort_name_descending
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.markersscreen.nextSort
import org.scottishtecharmy.soundscape.ui.theme.spacing

/**
 * A single button that describes the current sort order ("Sorted by name, A to Z") and moves to
 * the next one when tapped. TalkBack announces what the tap will do, e.g. "Double tap to sort by
 * name, Z to A", and reads out the new order after the tap. The orders cycle name A-Z, name Z-A,
 * nearest first, furthest first.
 */
@Composable
fun MarkersAndRoutesListSort(
    isSortByName: Boolean,
    isAscending: Boolean,
    onCycleSort: () -> Unit
) {
    val sortState = stringResource(sortStateString(isSortByName, isAscending))
    val (nextByName, nextAscending) = nextSort(isSortByName, isAscending)
    val sortAction = stringResource(sortActionString(nextByName, nextAscending))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = spacing.small, bottom = spacing.tiny)
            // Before clearAndSetSemantics, which would otherwise clear the tag along with
            // everything else inside it.
            .testTag("SortOption")
            .background(color = MaterialTheme.colorScheme.surfaceContainer)
            .clickable(
                role = Role.Button,
                onClickLabel = sortAction,
                onClick = onCycleSort
            )
            // The current order is the button's state rather than its text, as with a switch.
            // TalkBack reads out a state change on the focused node once the screen has been
            // updated, whereas a live region or text change is announced from the click event,
            // which still carries the previous order.
            .clearAndSetSemantics { stateDescription = sortState },
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.SwapVert,
            tint = MaterialTheme.colorScheme.onSurface,
            contentDescription = null
        )

        Spacer(modifier = Modifier.width(spacing.small))

        Text(
            text = sortState,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun sortStateString(byName: Boolean, ascending: Boolean) = when {
    byName && ascending -> Res.string.markers_sort_name_ascending
    byName -> Res.string.markers_sort_name_descending
    ascending -> Res.string.markers_sort_distance_ascending
    else -> Res.string.markers_sort_distance_descending
}

private fun sortActionString(byName: Boolean, ascending: Boolean) = when {
    byName && ascending -> Res.string.markers_sort_action_name_ascending
    byName -> Res.string.markers_sort_action_name_descending
    ascending -> Res.string.markers_sort_action_distance_ascending
    else -> Res.string.markers_sort_action_distance_descending
}
