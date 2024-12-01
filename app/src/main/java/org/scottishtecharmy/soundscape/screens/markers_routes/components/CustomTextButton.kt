package org.scottishtecharmy.soundscape.screens.markers_routes.components

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme

@Composable
fun CustomTextButton(
    modifier: Modifier = Modifier, // Modifier for button
    onClick: () -> Unit,
    text: String, // Button text
    contentColor: Color = MaterialTheme.colorScheme.primary,
    textStyle: TextStyle? = MaterialTheme.typography.labelSmall,
    fontWeight: FontWeight
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.textButtonColors(
            contentColor = contentColor // Customize the text color
        )
    ) {
        Text(
            text = text,
            style = textStyle ?: MaterialTheme.typography.labelSmall,
            fontWeight = fontWeight,
            textAlign = TextAlign.Center
        )
    }
}

@Preview
@Composable
fun CustomTextButtonPreview() {
    SoundscapeTheme {
        CustomTextButton(
            onClick = { /*TODO*/ },
            text = "Done",
            contentColor = MaterialTheme.colorScheme.onPrimary,
            textStyle = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}