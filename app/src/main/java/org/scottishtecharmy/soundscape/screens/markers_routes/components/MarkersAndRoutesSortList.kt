package org.scottishtecharmy.soundscape.screens.markers_routes.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight


@Composable
fun MarkersAndRoutesListSort(
    isSortByName: Boolean,
    onToggleSortOrder: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .toggleable(
                value = isSortByName,
                role = Role.Button, // Role can be Button or Switch based on the use case
                onValueChange = { onToggleSortOrder() }
            ),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.size(48.dp),
            imageVector = Icons.Default.SwapVert,
            contentDescription = if (isSortByName) "Currently sorted by Name" else "Currently sorted by Distance"
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = if (isSortByName) "Name" else "Distance",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold

        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = if (isSortByName) "Sort by Distance" else "Sort by Name",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.surfaceBright
        )
    }
}
