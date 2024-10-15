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
import androidx.compose.ui.unit.dp
import org.scottishtecharmy.soundscape.screens.onboarding.BoxWithGradientBackground
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme

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
        shape = RoundedCornerShape(3.dp),
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
            modifier = Modifier.size(300.dp)
        ) {
            OnboardButton(text = "Hello", onClick = {}, Modifier.width(200.dp))

        }
    }
}

@Preview
@Composable
fun PreviewDisabledButton() {
    SoundscapeTheme {
        BoxWithGradientBackground(
            modifier = Modifier.size(300.dp)
        ) {
            OnboardButton(
                text = "Hello", onClick = {},
                modifier = Modifier.width(200.dp),
                enabled = false
            )
        }
    }
}