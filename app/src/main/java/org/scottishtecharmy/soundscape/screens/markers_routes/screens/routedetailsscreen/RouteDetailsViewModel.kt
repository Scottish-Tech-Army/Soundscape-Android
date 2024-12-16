package org.scottishtecharmy.soundscape.screens.markers_routes.screens.routedetailsscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.database.repository.RoutesRepository
import javax.inject.Inject


@HiltViewModel
class RouteDetailsViewModel @Inject constructor(
    private val routesRepository: RoutesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RouteDetailsUiState())
    val uiState: StateFlow<RouteDetailsUiState> = _uiState.asStateFlow()

    init {
        loadRoutes()
    }

    private fun loadRoutes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val routes = routesRepository.getRoutes()
                _uiState.value = _uiState.value.copy(route = routes, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "An error occurred",
                    isLoading = false
                )
            }
        }
    }

    fun getRouteByName(routeName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val routeResult = routesRepository.getRoute(routeName)
                val route = routeResult.firstOrNull() // Extract the first match if it exists
                if (route != null) {
                    _uiState.value = _uiState.value.copy(selectedRoute = route, isLoading = false)
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

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
