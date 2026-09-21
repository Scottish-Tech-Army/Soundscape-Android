package org.scottishtecharmy.soundscape.screens.markers_routes.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.markers_sort_button_reverse_order
import org.scottishtecharmy.soundscape.resources.markers_sort_button_sort_by_distance
import org.scottishtecharmy.soundscape.resources.markers_sort_button_sort_by_distance_voiceover
import org.scottishtecharmy.soundscape.resources.markers_sort_button_sort_by_name
import org.scottishtecharmy.soundscape.resources.markers_sort_button_sort_by_name_voiceover
import org.scottishtecharmy.soundscape.resources.routes_sort_by_distance
import org.scottishtecharmy.soundscape.resources.routes_sort_by_name
import org.scottishtecharmy.soundscape.ui.theme.spacing

@Composable
fun MarkersAndRoutesListSort(
    isSortByName: Boolean,
    isAscending: Boolean,
    onToggleSortOrder: () -> Unit,
    onToggleSortByName: () -> Unit
) {
    val sortOrderState =
        if (isSortByName) stringResource(Res.string.markers_sort_button_sort_by_name_voiceover)
        else stringResource(Res.string.markers_sort_button_sort_by_distance_voiceover)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = spacing.small, bottom = spacing.tiny)
            .background(color = MaterialTheme.colorScheme.surfaceContainer),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier
                .testTag("SortOrder")
                .size(spacing.targetSize)
                .clickable(
                    role = Role.Button,
                    onClick = onToggleSortOrder
                ),
            imageVector = Icons.Default.SwapVert,
            tint = MaterialTheme.colorScheme.onSurface,
            contentDescription = stringResource(Res.string.markers_sort_button_reverse_order)
        )

        Spacer(modifier = Modifier.width(spacing.small))

        // The current sort field. Tapping it toggles the field for sighted users, but TalkBack
        // hears it as a plain statement ("Sorted by name") and uses the button alongside it to
        // change the sort, so that the same action isn't announced twice.
        Text(
            text = if (isSortByName) stringResource(Res.string.markers_sort_button_sort_by_name)
            else stringResource(Res.string.markers_sort_button_sort_by_distance),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .testTag("SortValue")
                .clearAndSetSemantics { contentDescription = sortOrderState }
                .clickable(onClick = onToggleSortByName)
        )

        Spacer(modifier = Modifier.width(spacing.small))

        Text(
            text = if (isSortByName) stringResource(Res.string.routes_sort_by_distance)
            else stringResource(Res.string.routes_sort_by_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .clickable(
                    role = Role.Button,
                    onClick = onToggleSortByName
                )
                .testTag("SortOption")
        )
    }
}
