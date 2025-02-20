package org.scottishtecharmy.soundscape.screens.markers_routes.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme

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

@Preview(showBackground = true)
@Composable
fun LargeCustomFloatingActionButtonPreview() {
    SoundscapeTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CustomFloatingActionButton(
                onClick = { /* Handle click */ },
                icon = Icons.Rounded.AddCircleOutline, // Replace with a sample icon
                contentDescription = "Add Item",
                iconTint = MaterialTheme.colorScheme.onPrimary,
                iconSize = 80.dp // Example of customizing the icon size
            )
        } }

}


@Preview(showBackground = true)
@Composable
fun SmallCustomFloatingActionButtonPreview() {
    SoundscapeTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CustomFloatingActionButton(
                onClick = { /* Handle click */ },
                icon = Icons.Rounded.AddCircleOutline, // Replace with a sample icon
                contentDescription = "Add Item",
                iconTint = MaterialTheme.colorScheme.onPrimary,
                iconSize = 48.dp // Example of customizing the icon size
            )

        } }

}

@Preview(showBackground = true)
@Composable
fun SmallCustomFloatingActionButtonWithTextPreview() {
    SoundscapeTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CustomFloatingActionButton(
                onClick = { /* Handle click */ },
                icon = Icons.Rounded.AddCircleOutline, // Replace with a sample icon
                contentDescription = "Add Route",
                iconTint = MaterialTheme.colorScheme.onPrimary,
                iconSize = 48.dp // Example of customizing the icon size
            )
            CustomTextButton(
                text = stringResource(R.string.route_detail_action_create),
                modifier = Modifier,
                onClick = { },
                contentColor = MaterialTheme.colorScheme.onPrimary,
                textStyle = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        } }

}
