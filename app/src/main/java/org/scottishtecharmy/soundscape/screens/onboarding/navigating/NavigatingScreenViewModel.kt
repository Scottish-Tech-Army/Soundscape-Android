package org.scottishtecharmy.soundscape.screens.onboarding.navigating

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class NavigatingScreenState(
    // A map of <String, Boolean> representing the permissions required and their granted status
    val permissionsStatus: Map<String, Boolean>
)

class NavigatingScreenViewModel : ViewModel() {
    private val _state: MutableStateFlow<NavigatingScreenState> = MutableStateFlow(
        NavigatingScreenState(emptyMap())
    )
    val state: StateFlow<NavigatingScreenState> = _state

    fun permissionsRequired(permissions: List<String>) {
        //Log.d("NavScreen", "Permissions Requested: ${permissions.joinToString(", ")}")

        val snapshot = _state.value
        val permSnapshot = snapshot.permissionsStatus
        val permMap = permSnapshot.toMutableMap()

        permissions.forEach {
            if (!permMap.containsKey(it)) {
                permMap[it] = false
            }
        }

        _state.value = _state.value.copy(
            permissionsStatus = permMap
        )
    }

    fun onPermissionResult(permission: String, isGranted: Boolean) {
        //Log.d("NavScreen", "Permission Result: $permission: $isGranted")
        val snapshot = _state.value
        val permSnapshot = snapshot.permissionsStatus
        val permMap = permSnapshot.toMutableMap()

        permMap[permission] = isGranted

        _state.value = _state.value.copy(
            permissionsStatus = permMap
        )
    }
}
