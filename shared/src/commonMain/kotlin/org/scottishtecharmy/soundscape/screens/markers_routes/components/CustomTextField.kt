package org.scottishtecharmy.soundscape.screens.markers_routes.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import org.jetbrains.compose.resources.stringResource
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.text_field_clear_text
import org.scottishtecharmy.soundscape.screens.talkbackDescription
import org.scottishtecharmy.soundscape.ui.theme.spacing

/**
 * A labelled, outlined text field with a button to clear it.
 *
 * This is per platform because of VoiceOver, which only offers its text editing rotors for a
 * native text field: while it is running iOS swaps one in. Everything else uses
 * [ComposeCustomTextField].
 */
@Composable
expect fun CustomTextField(
    fieldName: String,
    fieldHint: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    shape: Shape = RoundedCornerShape(spacing.extraSmall),
    testTagPreFix: String,
    isSingleLine: Boolean = true  // Optional single-line behavior
)

@Composable
internal fun ComposeCustomTextField(
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
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = textStyle,
        shape = shape,
        singleLine = isSingleLine,
        label = {
            Text(
                modifier = Modifier
                    .padding(top = spacing.small, bottom = spacing.small)
                    .talkbackDescription(fieldHint),
                text = fieldName,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        trailingIcon = {
            if (value.isNotEmpty()) {
                ClearTextFieldButton(onClear = { onValueChange("") }, testTagPreFix)
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            capitalization = KeyboardCapitalization.Sentences
        ),
        modifier = modifier
    )
}

@Composable
internal fun ClearTextFieldButton(onClear: () -> Unit, testTagPreFix: String) {
    IconButton(
        onClick = onClear,
        modifier = Modifier.testTag("$testTagPreFix-clearTextField")
    ) {
        Icon(
            Icons.Filled.Clear,
            contentDescription = stringResource(Res.string.text_field_clear_text),
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}
