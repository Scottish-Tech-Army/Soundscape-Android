package org.scottishtecharmy.soundscape.screens.markers_routes.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

// MarkersAndRoutesListSort composable is now in shared module

@Preview(showBackground = true)
@Composable
fun MarkersAndRoutesListSortByNamePreview() {
    // Preview with sorting by name
    MarkersAndRoutesListSort(
        isSortByName = true,
        isAscending = true,
        onToggleSortOrder = { /* Handle toggle */ },
        onToggleSortByName = { /* Handle toggle */ }
    )
}

@Preview(showBackground = true)
@Composable
fun MarkersAndRoutesListSortByDistancePreview() {
    // Preview with sorting by distance
    MarkersAndRoutesListSort(
        isSortByName = false,
        isAscending = true,
        onToggleSortOrder = { /* Handle toggle */ },
        onToggleSortByName = { /* Handle toggle */ }
    )
}
