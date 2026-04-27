package org.scottishtecharmy.soundscape.screens.onboarding.accessibility

import platform.UIKit.UIAccessibilityIsVoiceOverRunning

actual fun isScreenReaderEnabled(): Boolean {
    return UIAccessibilityIsVoiceOverRunning()
}
