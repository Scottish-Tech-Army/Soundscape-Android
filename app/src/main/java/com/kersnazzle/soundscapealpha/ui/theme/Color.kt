package com.kersnazzle.soundscapealpha.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val Primary = Color(0xFF604696)
val Secondary = Color(0xFF243D61)

val Tertiary = Color(0xFF332940)
val Background = Color(0xFF261C33)
val Surface = Color.White
val OnPrimary = Color.White
val OnSecondary = Color.White
val OnBackground = Color.White
val OnSurface = Color.White

val Foreground1 = Color(0xFF93F7F6)
val Foreground2 = Color(0xFFFFEE59)

val Background1 = Color(0xFF212233)
val Background2 = Color(0xFF172642)

val BackgroundBlue = Color(0xFF165F91)
val BackgroundBlue2 = Color(0xFF162542)
val Content = Color(0xFF2F4C79)
val ContentDarker1 = Color(0xFF253D62)
val ContentDarker2 = Color(0xFF1F3353)

val PurpleGradientDark = Color(0xFF1C054A)
val PurpleGradientLight = Color(0xFF7C84C8)

val LightBlue = Color(0xFF62D8FF)
val IntroPrimary = Color(0xFFFFFFFF)
val IntroBlue = Color(0xFF196497)
val IntroBlue2 = Color(0xFF083865)

val gradientBackgroundIntro = Brush.linearGradient(
    colors = listOf(PurpleGradientDark, PurpleGradientLight),
    start = Offset(0f, 0f),
    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
)

val blueGradientBackgroundIntro = Brush.linearGradient(
    colors = listOf(IntroBlue, IntroBlue2),
    start = Offset(0f, 0f),
    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
)