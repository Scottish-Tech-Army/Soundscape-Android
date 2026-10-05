package org.scottishtecharmy.soundscape.platform

import android.view.WindowManager
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat

actual fun fullScreenDialogProperties(): DialogProperties =
    DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)

@Composable
actual fun Modifier.fullScreenDialogSystemBarsPadding(): Modifier =
    windowInsetsPadding(WindowInsets.systemBars)

@Composable
actual fun FullScreenDialogWindow() {
    val view = LocalView.current
    val window = (view.parent as? DialogWindowProvider)?.window
    val lightBackground = MaterialTheme.colorScheme.background.luminance() > 0.5f
    SideEffect {
        if (window != null) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = lightBackground
                isAppearanceLightNavigationBars = lightBackground
            }
        }
    }
}
