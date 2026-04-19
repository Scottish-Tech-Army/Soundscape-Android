package org.scottishtecharmy.soundscape.screens.markers_routes.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.resources.*

// CustomTextButton composable is now in shared module

@Preview(fontScale = 1.5f, showBackground = true)
@Preview(showBackground = true)
@Composable
fun CustomTextButtonPreview() {
    CustomTextButton(
        onClick = { /*TODO*/ },
        text = stringResource(Res.string.general_alert_done),
        textStyle = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
}
