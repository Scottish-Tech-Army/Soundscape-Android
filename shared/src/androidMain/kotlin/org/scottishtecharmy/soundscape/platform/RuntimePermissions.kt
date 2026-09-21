package org.scottishtecharmy.soundscape.platform

actual fun requestLocationPermission() {
    // Android requests location permission outside of the shared onboarding flow
    // (see MainActivity / OnboardingActivity). No-op here.
}
