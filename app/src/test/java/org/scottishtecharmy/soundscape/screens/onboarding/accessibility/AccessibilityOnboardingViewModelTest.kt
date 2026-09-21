package org.scottishtecharmy.soundscape.screens.onboarding.accessibility

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.view.accessibility.AccessibilityManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class AccessibilityOnboardingViewModelTest {

    private fun mockContextWithAccessibilityManager(am: AccessibilityManager): Context {
        val context = mock<Context>()
        whenever(context.getSystemService(Context.ACCESSIBILITY_SERVICE)).thenReturn(am)
        return context
    }

    @Test
    fun construction_withTalkbackServiceEnabled_reportsTalkbackEnabled() {
        val am = mock<AccessibilityManager>()
        whenever(am.isEnabled).thenReturn(true)
        whenever(am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN))
            .thenReturn(mutableListOf(mock()))

        val viewModel = AccessibilityOnboardingViewModel(mockContextWithAccessibilityManager(am))

        assertEquals(true, viewModel.uiState.value.talkbackEnabled)
    }

    @Test
    fun construction_accessibilityDisabled_defaultStateIsUnchanged() {
        val am = mock<AccessibilityManager>()
        whenever(am.isEnabled).thenReturn(false)

        val viewModel = AccessibilityOnboardingViewModel(mockContextWithAccessibilityManager(am))

        assertFalse(viewModel.uiState.value.talkbackEnabled)
    }

    @Test
    fun construction_enabledButNoSpokenFeedbackServices_reportsTalkbackDisabled() {
        val am = mock<AccessibilityManager>()
        whenever(am.isEnabled).thenReturn(true)
        whenever(am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN))
            .thenReturn(mutableListOf())

        val viewModel = AccessibilityOnboardingViewModel(mockContextWithAccessibilityManager(am))

        assertFalse(viewModel.uiState.value.talkbackEnabled)
    }
}
