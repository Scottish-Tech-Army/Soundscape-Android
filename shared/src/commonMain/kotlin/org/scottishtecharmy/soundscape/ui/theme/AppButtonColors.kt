package org.scottishtecharmy.soundscape.ui.theme

import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

val LocalAppButtonColors: androidx.compose.runtime.ProvidableCompositionLocal<ButtonColors> = compositionLocalOf {
    error("No AppButtonColors provided.")
}

val currentAppButtonColors: ButtonColors
    @Composable
    get() = LocalAppButtonColors.current

fun defaultAppButtonColors(colorScheme: ColorScheme): ButtonColors {
    return ButtonColors(
        containerColor = colorScheme.surfaceContainer,
        contentColor = colorScheme.onSurface,
        disabledContainerColor = colorScheme.surfaceContainer.copy(alpha = 0.38f),
        disabledContentColor = colorScheme.onSurface.copy(alpha = 0.38f)
    )
}
