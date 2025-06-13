package org.scottishtecharmy.soundscape.screens.markers_routes.screens.routedetailsscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.SoundscapeServiceConnection
import org.scottishtecharmy.soundscape.database.local.dao.RouteDao
import javax.inject.Inject

@HiltViewModel
class RouteDetailsViewModel @Inject constructor(
    private val routeDao: RouteDao,
    private val soundscapeServiceConnection: SoundscapeServiceConnection
) : ViewModel() {
    private val _uiState = MutableStateFlow(RouteDetailsUiState())
    val uiState: StateFlow<RouteDetailsUiState> = _uiState.asStateFlow()

    fun getRouteById(routeId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val route = routeDao.getRouteWithMarkers(routeId)
                _uiState.value = _uiState.value.copy(
                    route = route,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "An error occurred",
                    isLoading = false
                )
            }
        }
    }

    fun startRoute(routeId: Long) {
        soundscapeServiceConnection.routeStart(routeId)
    }

    fun stopRoute() {
        soundscapeServiceConnection.routeStop()
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
