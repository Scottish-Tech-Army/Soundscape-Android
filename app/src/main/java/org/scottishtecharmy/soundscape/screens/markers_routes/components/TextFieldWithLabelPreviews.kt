package org.scottishtecharmy.soundscape.screens.markers_routes.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import org.scottishtecharmy.soundscape.ui.theme.spacing

// TextFieldWithLabel composable is now in shared module

@Preview(showBackground = true)
@Composable
fun TextFieldWithLabelPreview() {
    var textValue by remember { mutableStateOf("Dave") }
    TextFieldWithLabel(
        label = "Name",
        value = textValue,
        onValueChange = { },
        textStyle = MaterialTheme.typography.bodyLarge,
        shape = RoundedCornerShape(spacing.extraSmall),
        isSingleLine = true
    )
}
