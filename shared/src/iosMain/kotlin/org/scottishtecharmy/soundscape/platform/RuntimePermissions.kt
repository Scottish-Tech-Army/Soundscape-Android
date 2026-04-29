package org.scottishtecharmy.soundscape.platform

import org.scottishtecharmy.soundscape.IosSoundscapeService

actual fun requestLocationPermission() {
    IosSoundscapeService.getInstance().locationProvider.requestPermission()
}
