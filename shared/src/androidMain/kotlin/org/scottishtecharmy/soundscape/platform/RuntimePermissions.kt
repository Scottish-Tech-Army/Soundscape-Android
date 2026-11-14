package org.scottishtecharmy.soundscape.platform

actual fun requestLocationPermission() {
    // Android requests location permission outside of the shared onboarding flow
    // (see MainActivity / OnboardingActivity). No-op here.
}

actual fun requestMotionPermission() {
    // Android requests physical activity permission outside of the shared onboarding
    // flow (see NavigatingScreen). No-op here.
}

// MainActivity routes straight to the Android-native OnboardingActivity rather than the
// shared onboarding flow, so PermissionsScreen (and these three) never actually render
// on Android - see NavigatingScreen for the real Android permission flow. Stubbed so the
// shared screen still compiles for this target.
actual fun isLocationPermissionGranted(): Boolean = true

actual fun isMotionPermissionGranted(): Boolean = true

actual fun isMotionPermissionAvailable(): Boolean = false

actual fun isLocationPermissionDenied(): Boolean = false

actual fun isMotionPermissionDenied(): Boolean = false

actual fun openPermissionSettings() {
    // Unreachable on Android - see note above. No-op here.
}
