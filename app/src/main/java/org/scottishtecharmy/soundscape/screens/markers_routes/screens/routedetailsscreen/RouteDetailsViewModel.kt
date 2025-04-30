package org.scottishtecharmy.soundscape.screens.markers_routes.screens.routedetailsscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.mongodb.kbson.ObjectId
import org.scottishtecharmy.soundscape.SoundscapeServiceConnection
import org.scottishtecharmy.soundscape.database.repository.RoutesRepository
import javax.inject.Inject

@HiltViewModel
class RouteDetailsViewModel @Inject constructor(
    private val routesRepository: RoutesRepository,
    private val soundscapeServiceConnection: SoundscapeServiceConnection
) : ViewModel() {
    private val _uiState = MutableStateFlow(RouteDetailsUiState())
    val uiState: StateFlow<RouteDetailsUiState> = _uiState.asStateFlow()

    fun getRouteById(routeId: ObjectId) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val routeResult = routesRepository.getRoute(routeId)
                val route = routeResult.firstOrNull() // Extract the first match if it exists
                if (route != null) {
                    _uiState.value = _uiState.value.copy(
                        route = route,
                        isLoading = false)
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Route not found",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "An error occurred",
                    isLoading = false
                )
            }
        }
    }

    fun startRoute(routeId: ObjectId) {
        soundscapeServiceConnection.routeStart(routeId)
    }

    fun stopRoute() {
        soundscapeServiceConnection.routeStop()
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
