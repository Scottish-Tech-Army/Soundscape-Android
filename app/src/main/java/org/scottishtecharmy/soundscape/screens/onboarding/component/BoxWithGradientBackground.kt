package org.scottishtecharmy.soundscape.screens.onboarding.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.scottishtecharmy.soundscape.ui.theme.gradientBackgroundIntro

@Composable
fun BoxWithGradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier
        .background(gradientBackgroundIntro)
        .fillMaxSize()
    ) {
        content()
    }
}