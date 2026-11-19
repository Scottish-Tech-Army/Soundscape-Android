package org.scottishtecharmy.soundscape.screens.onboarding.accessibility

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.view.accessibility.AccessibilityManager
import org.scottishtecharmy.soundscape.screens.onboarding.language.androidAppContext

/**
 * Whether a screen reader such as TalkBack is running: an enabled accessibility service that
 * gives spoken feedback, the same test the Android onboarding's AccessibilityOnboardingViewModel
 * makes. Read afresh on each call, so it follows TalkBack being turned on or off while the app is
 * open. False if called before the Application has installed its context.
 */
actual fun isScreenReaderEnabled(): Boolean {
    val context = androidAppContext ?: return false
    val manager =
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            ?: return false
    return manager.isEnabled &&
        manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN)
            .isNotEmpty()
}
