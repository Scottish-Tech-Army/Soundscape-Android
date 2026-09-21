package org.scottishtecharmy.soundscape.screens.onboarding.accessibility

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Context.ACCESSIBILITY_SERVICE
import android.view.accessibility.AccessibilityManager
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class AccessibilityOnboardingUiState(
    val talkbackEnabled: Boolean = false,
)

class AccessibilityOnboardingViewModel(val appContext: Context) : ViewModel() {

    private val _uiState = MutableStateFlow(AccessibilityOnboardingUiState())
    val uiState: StateFlow<AccessibilityOnboardingUiState> = _uiState

    init {
        val am = appContext.getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        if (am.isEnabled) {
            val enabledServices =
                am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN)
            _uiState.value = AccessibilityOnboardingUiState(enabledServices.isNotEmpty())
        }
    }
}
