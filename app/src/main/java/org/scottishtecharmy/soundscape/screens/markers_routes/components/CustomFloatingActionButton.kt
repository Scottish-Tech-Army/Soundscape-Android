package org.scottishtecharmy.soundscape.screens.markers_routes.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun CustomFloatingActionButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceDim,
    iconTint: Color = MaterialTheme.colorScheme.onBackground,
    iconSize: Dp = 80.dp
) {
    FloatingActionButton(
        onClick = { onClick.invoke() },
        modifier = modifier
            .padding(bottom = 16.dp),
        containerColor = containerColor
    ) {
        Icon(
            imageVector = icon,
            tint = iconTint,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize)
        )
    }
}