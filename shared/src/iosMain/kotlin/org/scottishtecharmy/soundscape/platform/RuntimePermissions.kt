package org.scottishtecharmy.soundscape.platform

import org.scottishtecharmy.soundscape.IosSoundscapeService
import org.scottishtecharmy.soundscape.openExternalUrl
import platform.Foundation.NSURL
import platform.UIKit.UIApplicationOpenSettingsURLString

actual fun requestLocationPermission() {
    IosSoundscapeService.getInstance().iosLocationProvider.requestPermission()
}

actual fun requestMotionPermission() {
    IosSoundscapeService.getInstance().stepProvider.requestPermission()
}

actual fun isLocationPermissionGranted(): Boolean =
    IosSoundscapeService.getInstance().iosLocationProvider.isPermissionGranted()

actual fun isMotionPermissionGranted(): Boolean =
    IosSoundscapeService.getInstance().stepProvider.isPermissionGranted()

actual fun isMotionPermissionAvailable(): Boolean =
    IosSoundscapeService.getInstance().stepProvider.isAvailable()

actual fun isLocationPermissionDenied(): Boolean =
    IosSoundscapeService.getInstance().iosLocationProvider.isPermissionDenied()

actual fun isMotionPermissionDenied(): Boolean =
    IosSoundscapeService.getInstance().stepProvider.isPermissionDenied()

actual fun openPermissionSettings() {
    openExternalUrl(NSURL(string = UIApplicationOpenSettingsURLString)!!)
}
