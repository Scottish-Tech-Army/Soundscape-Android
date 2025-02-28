package org.scottishtecharmy.soundscape.screens.markers_routes.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import org.scottishtecharmy.soundscape.ui.theme.spacing

@Composable
fun TextFieldWithLabel(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    shape: Shape = RoundedCornerShape(spacing.extraSmall),
    focusedBgColor: Color = MaterialTheme.colorScheme.onPrimary, // Default colour of white for TextField
    unfocusedBgColor: Color = MaterialTheme.colorScheme.onPrimary, // Default colour of white for TextField
    focusedTextColor: Color = MaterialTheme.colorScheme.onSecondary, // Default color of black
    unfocusedTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant, // Default color of grey
    isSingleLine: Boolean = true  // Optional single-line behavior
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = textStyle,
        shape = shape,
        singleLine = isSingleLine,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun TextFieldWithLabelPreview() {
    var textValue by remember { mutableStateOf("Dave") }
    TextFieldWithLabel(
        label = "Name",
        value = textValue,
        onValueChange = { textValue = it },
        textStyle = MaterialTheme.typography.bodyLarge,
        shape = RoundedCornerShape(spacing.extraSmall),
        focusedBgColor = MaterialTheme.colorScheme.onPrimary,
        unfocusedBgColor = MaterialTheme.colorScheme.onPrimary,
        focusedTextColor = MaterialTheme.colorScheme.onSecondary,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        isSingleLine = true
    )
}




