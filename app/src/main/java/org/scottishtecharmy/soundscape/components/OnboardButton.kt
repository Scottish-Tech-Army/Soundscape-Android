package org.scottishtecharmy.soundscape.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import org.scottishtecharmy.soundscape.screens.onboarding.component.BoxWithGradientBackground
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme
import org.scottishtecharmy.soundscape.ui.theme.spacing

@Composable
fun OnboardButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults
            .buttonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
            ),
        shape = RoundedCornerShape(spacing.tiny),
        modifier = modifier,
        enabled = enabled
    ) {
        Text(
            text = text,
            color = textColor,
        )
    }
}

@Preview
@Composable
fun PreviewButton() {
    SoundscapeTheme {
        BoxWithGradientBackground(
            modifier = Modifier.size(spacing.preview)
        ) {
            OnboardButton(text = "Hello", onClick = {}, Modifier.width(spacing.preview))

        }
    }
}

@Preview
@Composable
fun PreviewDisabledButton() {
    SoundscapeTheme {
        BoxWithGradientBackground(
            modifier = Modifier.size(spacing.preview)
        ) {
            OnboardButton(
                text = "Hello", onClick = {},
                modifier = Modifier.width(spacing.preview),
                enabled = false
            )
        }
    }
}