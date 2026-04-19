package org.scottishtecharmy.soundscape.screens.markers_routes.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.ui.theme.spacing
import org.scottishtecharmy.soundscape.resources.*

// CustomFloatingActionButton composable is now in shared module

@Preview(showBackground = true)
@Composable
fun LargeCustomFloatingActionButtonPreview() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(spacing.preview),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CustomFloatingActionButton(
            onClick = { /* Handle click */ },
            icon = Icons.Rounded.AddCircleOutline, // Replace with a sample icon
            contentDescription = stringResource(Res.string.fab_add_item),
            iconSize = spacing.targetSize * 2 // Example of customizing the icon size
        )
    }
}


@Preview(showBackground = true)
@Composable
fun SmallCustomFloatingActionButtonPreview() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(spacing.preview),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CustomFloatingActionButton(
            onClick = { /* Handle click */ },
            icon = Icons.Rounded.AddCircleOutline, // Replace with a sample icon
            contentDescription = stringResource(Res.string.fab_add_item),
            iconSize = spacing.targetSize // Example of customizing the icon size
        )

    }
}

@Preview(showBackground = true)
@Composable
fun SmallCustomFloatingActionButtonWithTextPreview() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(spacing.preview),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CustomFloatingActionButton(
            onClick = { /* Handle click */ },
            icon = Icons.Rounded.AddCircleOutline, // Replace with a sample icon
            contentDescription = stringResource(Res.string.fab_add_route),
            iconSize = spacing.targetSize // Example of customizing the icon size
        )
        CustomTextButton(
            text = stringResource(Res.string.route_detail_action_create),
            modifier = Modifier,
            onClick = { },
            textStyle = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
