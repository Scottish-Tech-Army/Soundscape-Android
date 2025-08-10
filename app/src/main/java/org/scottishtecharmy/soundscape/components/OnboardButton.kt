package org.scottishtecharmy.soundscape.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.scottishtecharmy.soundscape.screens.onboarding.component.BoxWithGradientBackground
import org.scottishtecharmy.soundscape.ui.theme.currentAppButtonColors
import org.scottishtecharmy.soundscape.ui.theme.spacing

@Composable
fun OnboardButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(spacing.extraSmall),
        modifier = modifier,
        enabled = enabled,
        colors = currentAppButtonColors
    ) {
        Text(
            text = text,
        )
    }
}

@Preview
@Composable
fun PreviewButton() {
    BoxWithGradientBackground(
        modifier = Modifier.size(spacing.preview),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        OnboardButton(text = "Hello", onClick = {}, Modifier.width(spacing.preview))

    }
}

@Preview
@Composable
fun PreviewDisabledButton() {
    BoxWithGradientBackground(
        modifier = Modifier.size(spacing.preview),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        OnboardButton(
            text = "Hello", onClick = {},
            modifier = Modifier.width(spacing.preview),
            enabled = false
        )
    }
}