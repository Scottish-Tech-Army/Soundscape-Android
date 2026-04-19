package org.scottishtecharmy.soundscape.screens.markers_routes.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign

@Composable
fun CustomTextButton(
    modifier: Modifier = Modifier, // Modifier for button
    onClick: () -> Unit,
    text: String, // Button text
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    fontWeight: FontWeight
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = textStyle,
            fontWeight = fontWeight,
            textAlign = TextAlign.Center
        )
    }
}
