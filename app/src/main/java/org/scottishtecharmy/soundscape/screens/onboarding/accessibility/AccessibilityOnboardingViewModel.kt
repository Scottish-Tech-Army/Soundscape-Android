package org.scottishtecharmy.soundscape.screens.onboarding.accessibility

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Context.ACCESSIBILITY_SERVICE
import android.view.accessibility.AccessibilityManager
import androidx.lifecycle.ViewModel
import androidx.preference.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.scottishtecharmy.soundscape.MainActivity
import javax.inject.Inject
import androidx.core.content.edit


data class AccessibilityOnboardingUiState(
    val talkbackEnabled: Boolean = false
)

@HiltViewModel
class AccessibilityOnboardingViewModel @Inject constructor(@param:ApplicationContext val appContext: Context): ViewModel() {

    private val _uiState = MutableStateFlow(AccessibilityOnboardingUiState())
    val uiState: StateFlow<AccessibilityOnboardingUiState> = _uiState

    init {
        val am = appContext.getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        if (am.isEnabled) {
            am.isTouchExplorationEnabled.toString()

            val enabledServices =
                am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN)

            // If we have a spoken feedback service enabled then disable the maps by default
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext)
            sharedPreferences.edit(commit = true) {
                putBoolean(MainActivity.SHOW_MAP_KEY, enabledServices.isEmpty())
            }
            _uiState.value = AccessibilityOnboardingUiState(enabledServices.isNotEmpty())
        }
    }

    fun enableGraphicalMaps(enable: Boolean) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext)
        sharedPreferences.edit(commit = true) { putBoolean(MainActivity.SHOW_MAP_KEY, enable) }
    }
}