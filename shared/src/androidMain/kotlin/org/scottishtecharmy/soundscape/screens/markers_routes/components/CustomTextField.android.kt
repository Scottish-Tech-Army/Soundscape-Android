package org.scottishtecharmy.soundscape.screens.markers_routes.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle

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
) = ComposeCustomTextField(
    fieldName, fieldHint, value, onValueChange, modifier, textStyle, shape, testTagPreFix,
    isSingleLine,
)
