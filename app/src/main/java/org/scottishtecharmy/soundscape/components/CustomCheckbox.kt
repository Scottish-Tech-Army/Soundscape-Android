package org.scottishtecharmy.soundscape.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp

@Composable
fun CustomCheckbox(
    checkedState: MutableState<Boolean>,
    boxSize: Dp,
    borderWidth: Dp,
    boxShape: CornerBasedShape,
    borderColor: Color,
    iconSize: Dp,
    iconColor: Color
) {
    Box(
        modifier = Modifier
            .semantics { contentDescription = "Check box" }
            .size(boxSize)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = boxShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (checkedState.value) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = iconColor
            )
        }
    }
}