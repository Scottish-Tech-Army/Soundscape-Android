package org.scottishtecharmy.soundscape.screens.markers_routes.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle

@Composable
fun CustomAppBarTitle(
    title: String, // The title text to display
    modifier: Modifier = Modifier, // Modifier for the Box
    textModifier: Modifier = Modifier, // Modifier for the Text component
    contentAlignment: Alignment = Alignment.CenterStart, // Alignment for the Box content
    textStyle: TextStyle? = MaterialTheme.typography.headlineMedium // The style to apply to the title
) {
    Box(
        modifier = modifier,
        contentAlignment = contentAlignment
    ) {
        Text(
            modifier = textModifier,
            text = title,
            style = textStyle ?: MaterialTheme.typography.titleLarge // Default value "titleLarge"
        )
    }
}