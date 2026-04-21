package org.scottishtecharmy.soundscape.screens.home.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.resources.*

data class BottomButtonFunctions(
    val myLocation: () -> Unit = {},
    val aroundMe: () -> Unit = {},
    val aheadOfMe: () -> Unit = {},
    val nearbyMarkers: () -> Unit = {},
)

@Composable
fun SharedHomeBottomAppBar(
    buttonFunctions: BottomButtonFunctions,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ActionButton(
                icon = Icons.Rounded.MyLocation,
                label = Res.string.ui_action_button_my_location,
                contentDescription = Res.string.ui_action_button_my_location_acc_hint,
                onClick = buttonFunctions.myLocation,
            )
            ActionButton(
                icon = Icons.Rounded.Explore,
                label = Res.string.ui_action_button_around_me,
                contentDescription = Res.string.ui_action_button_around_me_acc_hint,
                onClick = buttonFunctions.aroundMe,
            )
            ActionButton(
                icon = Icons.Rounded.NorthEast,
                label = Res.string.ui_action_button_ahead_of_me,
                contentDescription = Res.string.ui_action_button_ahead_of_me_acc_hint,
                onClick = buttonFunctions.aheadOfMe,
            )
            ActionButton(
                icon = Icons.Rounded.PushPin,
                label = Res.string.ui_action_button_nearby_markers,
                contentDescription = Res.string.ui_action_button_nearby_markers_acc_hint,
                onClick = buttonFunctions.nearbyMarkers,
            )
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: StringResource,
    contentDescription: StringResource,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.padding(4.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = stringResource(contentDescription),
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
