package org.scottishtecharmy.soundscape.platform

expect fun requestLocationPermission()

expect fun requestMotionPermission()

expect fun isLocationPermissionGranted(): Boolean

expect fun isMotionPermissionGranted(): Boolean

/** False on a device with no step-counting hardware - e.g. the simulator. */
expect fun isMotionPermissionAvailable(): Boolean

/** True once denied - the system prompt won't be shown again; only Settings can fix it. */
expect fun isLocationPermissionDenied(): Boolean

/** True once denied - the system prompt won't be shown again; only Settings can fix it. */
expect fun isMotionPermissionDenied(): Boolean

/** Opens this app's page in the system Settings app, so a denied permission can be re-allowed. */
expect fun openPermissionSettings()
