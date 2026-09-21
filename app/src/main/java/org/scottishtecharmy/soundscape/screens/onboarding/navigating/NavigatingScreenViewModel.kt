package org.scottishtecharmy.soundscape.screens.onboarding.navigating

import android.os.Build
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

fun requiredPermissionsGranted(permissionsStatus: Map<Permission, Boolean>): Boolean =
    permissionsStatus.filterKeys {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            it == Permission.ACCESS_FINE_LOCATION || it == Permission.POST_NOTIFICATIONS
        } else {
            it == Permission.ACCESS_FINE_LOCATION
        }
    }.none { !it.value }

data class NavigatingScreenState(
    val permissionsStatus: Map<Permission, Boolean>
) {
    val continueEnabled: Boolean
        get() = requiredPermissionsGranted(permissionsStatus)
}

class NavigatingScreenViewModel : ViewModel() {
    private val _state: MutableStateFlow<NavigatingScreenState> = MutableStateFlow(
        NavigatingScreenState(emptyMap())
    )
    val state: StateFlow<NavigatingScreenState> = _state

    fun permissionsRequired(permissions: Map<Permission, Boolean>) {
        // Log.d("NavScreen", "Permissions Required: $permissions")

        _state.value = _state.value.copy(
            permissionsStatus = permissions
        )
    }

    fun onPermissionResult(permission: Permission, isGranted: Boolean) {
        // Log.d("NavScreen", "Permission Result: $permission: $isGranted")
        val snapshot = _state.value
        val permSnapshot = snapshot.permissionsStatus
        val permMap = permSnapshot.toMutableMap()

        permMap[permission] = isGranted

        _state.value = _state.value.copy(
            permissionsStatus = permMap
        )
    }
}
