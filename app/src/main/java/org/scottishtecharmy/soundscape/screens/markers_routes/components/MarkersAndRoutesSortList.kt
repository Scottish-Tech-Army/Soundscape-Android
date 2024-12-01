package org.scottishtecharmy.soundscape.screens.markers_routes.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight


@Composable
fun MarkersAndRoutesListSort(
    isSortByName: Boolean,
    onToggleSortOrder: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.size(48.dp),
            imageVector = Icons.Default.SwapVert,
            contentDescription = ""
        )

        Text(
            text = if (isSortByName) "Name" else "Distance",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        CustomTextButton(
            onClick = { onToggleSortOrder() },
            text = if (isSortByName) "Sort by Distance" else "Sort by Name",
            fontWeight = FontWeight.Bold,
            textStyle = MaterialTheme.typography.headlineMedium,
            contentColor = MaterialTheme.colorScheme.surfaceBright
        )
    }
}
