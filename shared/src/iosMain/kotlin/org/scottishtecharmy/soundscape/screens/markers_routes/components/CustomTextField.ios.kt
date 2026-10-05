package org.scottishtecharmy.soundscape.screens.markers_routes.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import org.scottishtecharmy.soundscape.components.NativeTextField
import org.scottishtecharmy.soundscape.components.usesNativeTextFields
import org.scottishtecharmy.soundscape.screens.activationHint
import org.scottishtecharmy.soundscape.screens.talkbackHidden
import org.scottishtecharmy.soundscape.ui.theme.spacing

@Composable
actual fun CustomTextField(
    fieldName: String,
    fieldHint: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    textStyle: TextStyle,
    shape: Shape,
    testTagPreFix: String,
    isSingleLine: Boolean,
) {
    // The native field is a single line one, so a multiple line field stays with Compose
    if (!isSingleLine || !usesNativeTextFields()) {
        ComposeCustomTextField(
            fieldName, fieldHint, value, onValueChange, modifier, textStyle, shape,
            testTagPreFix, isSingleLine,
        )
        return
    }

    // The native field can't sit inside an OutlinedTextField, so this draws the same thing
    // with the label fixed above the outline rather than floating into it.
    Column(modifier = modifier) {
        Text(
            text = fieldName,
            style = MaterialTheme.typography.bodyMedium,
            // The field itself speaks the name
            modifier = Modifier
                .padding(bottom = spacing.extraSmall)
                .talkbackHidden(),
        )
        Row(
            modifier = Modifier
                .border(spacing.tiny / 2, MaterialTheme.colorScheme.outline, shape)
                .padding(start = spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NativeTextField(
                value = value,
                onValueChange = onValueChange,
                label = fieldName,
                // VoiceOver reads a hint verbatim, so "Double tap to ..." is supplied here
                hint = activationHint(fieldHint),
                textStyle = textStyle,
                // These fields are in pages that scroll
                inScrollingContainer = true,
                modifier = Modifier.weight(1f),
            )
            if (value.isNotEmpty()) {
                ClearTextFieldButton(onClear = { onValueChange("") }, testTagPreFix)
            }
        }
    }
}
