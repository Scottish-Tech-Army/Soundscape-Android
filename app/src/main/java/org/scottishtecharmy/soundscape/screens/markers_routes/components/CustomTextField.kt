package org.scottishtecharmy.soundscape.screens.markers_routes.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import org.scottishtecharmy.soundscape.ui.theme.spacing

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
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
        colors = TextFieldDefaults.colors(
            focusedContainerColor = focusedBgColor,
            unfocusedContainerColor = unfocusedBgColor,
            focusedTextColor = focusedTextColor,
            unfocusedTextColor = unfocusedTextColor
        ),
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = "Clear Text")
                }
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            capitalization = KeyboardCapitalization.Sentences
        ),
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun CustomTextFieldPreview() {
    var textValue by remember { mutableStateOf("Hello") }

    CustomTextField(
        value = textValue,
        onValueChange = { textValue = it },
        textStyle = TextStyle(fontSize = 18.sp),
        shape = RoundedCornerShape(spacing.extraSmall),
        focusedBgColor = MaterialTheme.colorScheme.onPrimary,
        unfocusedBgColor = MaterialTheme.colorScheme.onPrimary,
        focusedTextColor = MaterialTheme.colorScheme.onSecondary,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        isSingleLine = true
    )
}




