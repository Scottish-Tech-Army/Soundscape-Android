package org.scottishtecharmy.soundscape.ui.theme

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class Spacing(
    val none: Dp = 0.dp,
    val tiny: Dp = 2.dp,
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 32.dp,
    val extraLarge: Dp = 64.dp,
    val default: Dp = small,

    val icon: Dp = 32.dp,
    val targetSize: Dp = 48.dp,

    val preview: Dp = 200.dp
)

val LocalSpacing = compositionLocalOf { Spacing() }

val spacing: Spacing
    @Composable
    @ReadOnlyComposable
    get() = LocalSpacing.current

@Composable
fun Modifier.mediumPadding(): Modifier =
    padding(
        top = spacing.medium,
        bottom = spacing.medium,
        start = spacing.medium,
        end = spacing.medium
    )

@Composable
fun Modifier.smallPadding(): Modifier =
    padding(
        top = spacing.small,
        bottom = spacing.small,
        start = spacing.small,
        end = spacing.small
    )
