package org.scottishtecharmy.soundscape

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import org.scottishtecharmy.soundscape.locationprovider.IosDirectionProvider
import org.scottishtecharmy.soundscape.locationprovider.IosLocationProvider

fun MainViewController() = ComposeUIViewController {
    val locationProvider = remember { IosLocationProvider() }
    val directionProvider = remember { IosDirectionProvider() }

    App(
        locationFlow = locationProvider.locationFlow,
        directionFlow = directionProvider.orientationFlow,
    )
}
