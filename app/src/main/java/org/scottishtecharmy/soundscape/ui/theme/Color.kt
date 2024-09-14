package org.scottishtecharmy.soundscape.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val Primary = Color(0xFF604696) // Purple (medium-dark)
val Secondary = Color(0xFF243D61) // Dark Blue

val Tertiary = Color(0xFF332940) // Dark Purple
val Background = Color(0xFF261C33) // Very Dark Purple
val Surface = Color.White
val OnPrimary = Color.White
val OnSecondary = Color.Black
val OnBackground = Color.White
val OnSurface = Color.White
val OnSurfaceVariant = Color.DarkGray
val surfaceBright = Color.Cyan
val transparent = Color.Transparent

val Foreground1 = Color(0xFF93F7F6) // Light Cyan
val Foreground2 = Color(0xFFFFEE59) // Bright Yellow

val Background1 = Color(0xFF212233) // Very Dark Blue
val Background2 = Color(0xFF172642) // Very Dark Navy Blue

val BackgroundBlue = Color(0xFF165F91) // Deep Blue
val BackgroundBlue2 = Color(0xFF162542) // Dark Navy Blue
val Content = Color(0xFF2F4C79) // Medium Blue
val ContentDarker1 = Color(0xFF253D62) // Dark Blue
val ContentDarker2 = Color(0xFF1F3353) // Very Dark Blue

val PurpleGradientDark = Color(0xFF1C054A) // Very Dark Purple
val PurpleGradientLight = Color(0xFF7C84C8) // Light Purple

val LightBlue = Color(0xFF62D8FF) // Light Sky Blue
val IntroPrimary = Color(0xFFFFFFFF) // White
val IntroBlue = Color(0xFF196497) // Medium Blue
val IntroBlue2 = Color(0xFF083865) // Dark Blue

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