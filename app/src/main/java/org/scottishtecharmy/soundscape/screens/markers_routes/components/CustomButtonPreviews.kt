package org.scottishtecharmy.soundscape.screens.markers_routes.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.ui.theme.spacing
import org.scottishtecharmy.soundscape.resources.*

// CustomButton composable is now in shared module

@Preview
@Composable
fun CustomButtonPreview() {
    CustomButton(
        onClick = { /*TODO*/ },
        fontWeight = FontWeight.Bold,
        shape = RoundedCornerShape(spacing.small),
        text = stringResource(Res.string.route_detail_edit_waypoints_button),
        textStyle = MaterialTheme.typography.titleLarge
    )
}
