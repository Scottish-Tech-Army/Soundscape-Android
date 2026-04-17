package org.scottishtecharmy.soundscape.screens.onboarding.navigating

import android.Manifest
import org.junit.Test

class NavigatingScreenViewModelTest {
    private val viewModel = NavigatingScreenViewModel()

    @Test
    fun navigatingScreenViewModel_PermissionRequired_StateUpdatedWithRequiredPermission() {
        var currentState = viewModel.state.value

        assert(currentState.permissionsStatus.isEmpty())

        viewModel.permissionsRequired(mapOf(Permission.ACCESS_FINE_LOCATION to false))

        currentState = viewModel.state.value

        assert(currentState.permissionsStatus.containsKey(Permission.ACCESS_FINE_LOCATION))
        assert(currentState.permissionsStatus[Permission.ACCESS_FINE_LOCATION] == false)
    }

    @Test
    fun navigatingScreenViewModel_MultiplePermissionsRequired_StateUpdatedWithRequiredPermissions() {
        var currentState = viewModel.state.value

        assert(currentState.permissionsStatus.isEmpty())

        viewModel.permissionsRequired(
            mapOf(
                Permission.ACCESS_FINE_LOCATION to false,
                Permission.POST_NOTIFICATIONS to false,
            )
        )

        currentState = viewModel.state.value

        assert(currentState.permissionsStatus.containsKey(Permission.ACCESS_FINE_LOCATION))
        assert(currentState.permissionsStatus.containsKey(Permission.POST_NOTIFICATIONS))
        assert(currentState.permissionsStatus[Permission.ACCESS_FINE_LOCATION] == false)
        assert(currentState.permissionsStatus[Permission.POST_NOTIFICATIONS] == false)
    }

    @Test
    fun navigatingScreenViewModel_PermissionGranted_StateUpdatedWithGrantedPermission() {
        var currentState = viewModel.state.value

        assert(currentState.permissionsStatus.isEmpty())

        viewModel.permissionsRequired(mapOf(Permission.ACCESS_FINE_LOCATION to false))

        viewModel.onPermissionResult(Permission.ACCESS_FINE_LOCATION, true)

        currentState = viewModel.state.value

        assert(currentState.permissionsStatus[Permission.ACCESS_FINE_LOCATION] == true)
    }

    @Test
    fun navigatingScreenViewModel_PermissionDenied_StateUpdatedWithDeniedPermission() {
        var currentState = viewModel.state.value

        assert(currentState.permissionsStatus.isEmpty())

        viewModel.permissionsRequired(mapOf(Permission.ACCESS_FINE_LOCATION to false))

        viewModel.onPermissionResult(Permission.ACCESS_FINE_LOCATION, false)

        currentState = viewModel.state.value

        assert(currentState.permissionsStatus[Permission.ACCESS_FINE_LOCATION] == false)
    }
}