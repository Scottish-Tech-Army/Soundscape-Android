package org.scottishtecharmy.soundscape.screens.onboarding.navigating

import android.Manifest
import org.junit.Test

class NavigatingScreenViewModelTest {
    private val viewModel = NavigatingScreenViewModel()

    @Test
    fun navigatingScreenViewModel_PermissionRequired_StateUpdatedWithRequiredPermission() {
        var currentState = viewModel.state.value

        assert(currentState.permissionsStatus.isEmpty())

        viewModel.permissionsRequired(listOf(Manifest.permission.ACCESS_FINE_LOCATION))

        currentState = viewModel.state.value

        assert(currentState.permissionsStatus.containsKey(Manifest.permission.ACCESS_FINE_LOCATION))
        assert(currentState.permissionsStatus[Manifest.permission.ACCESS_FINE_LOCATION] == false)
    }

    @Test
    fun navigatingScreenViewModel_MultiplePermissionsRequired_StateUpdatedWithRequiredPermissions() {
        var currentState = viewModel.state.value

        assert(currentState.permissionsStatus.isEmpty())

        viewModel.permissionsRequired(
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS
            )
        )

        currentState = viewModel.state.value

        assert(currentState.permissionsStatus.containsKey(Manifest.permission.ACCESS_FINE_LOCATION))
        assert(currentState.permissionsStatus.containsKey(Manifest.permission.POST_NOTIFICATIONS))
        assert(currentState.permissionsStatus[Manifest.permission.ACCESS_FINE_LOCATION] == false)
        assert(currentState.permissionsStatus[Manifest.permission.POST_NOTIFICATIONS] == false)
    }
}