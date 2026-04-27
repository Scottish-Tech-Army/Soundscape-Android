package org.scottishtecharmy.soundscape.screens.onboarding.accessibility

actual fun isScreenReaderEnabled(): Boolean {
    // Android uses its own AccessibilityOnboardingViewModel for TalkBack detection
    return false
}
