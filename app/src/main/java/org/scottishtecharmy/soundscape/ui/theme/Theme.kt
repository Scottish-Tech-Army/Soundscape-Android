package org.scottishtecharmy.soundscape.ui.theme

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val CommonColorScheme = darkColorScheme(
    primary = Primary,
    secondary = Secondary,
    tertiary = Content,
    background = Background1,
    surface = Surface,
    onPrimary = OnPrimary,
    onSecondary = OnSecondary,
    onBackground = OnBackground,
    onSurface = OnSurface,
    onSurfaceVariant = OnSurfaceVariant,
    surfaceBright = surfaceBright,
    surfaceDim = transparent,
)

val DarkColorScheme = CommonColorScheme
val LightColorScheme = CommonColorScheme

/**
 * Specifies amount of spacing that should be used through the application in a non-graphic
 * library specific amount.
 */
data class Spacing(
    val none: Dp = 0.dp,
    val tiny: Dp = 2.dp,
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 32.dp,
    val extraLarge: Dp = 64.dp,
    val default: Dp = small,

    val icon: Dp = 20.dp,
    val targetSize: Dp = 40.dp,

    val preview: Dp = 200.dp
)
val LocalSpacing = compositionLocalOf { Spacing() }

@Composable
fun SoundscapeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    CompositionLocalProvider(
        LocalSpacing provides Spacing(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

val spacing: Spacing
    @Composable
    @ReadOnlyComposable
    get() = LocalSpacing.current

@Composable
fun Modifier.largePadding(): Modifier =
    padding(
        top = spacing.large,
        bottom = spacing.large,
        start = spacing.large,
        end = spacing.large
    )

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

@Composable
fun Modifier.extraSmallPadding(): Modifier =
    padding(
        top = spacing.extraSmall,
        bottom = spacing.extraSmall,
        start = spacing.extraSmall,
        end = spacing.extraSmall
    )

@Composable
fun Modifier.tinyPadding(): Modifier =
    padding(
        top = spacing.tiny,
        bottom = spacing.tiny,
        start = spacing.tiny,
        end = spacing.tiny
    )


@Composable
fun IntroductionTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        typography = Typography
    ) {
        Box(modifier = Modifier
            .background(gradientBackgroundIntro)
            .fillMaxSize()
        ) {
            content()
        }
    }
}

