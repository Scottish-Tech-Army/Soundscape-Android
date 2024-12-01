package org.scottishtecharmy.soundscape.screens.markers_routes.screens.routesscreen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.database.repository.RoutesRepository
import javax.inject.Inject

@HiltViewModel
class RoutesViewModel @Inject constructor(
    private val routesRepository: RoutesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutesUiState())
    val uiState: StateFlow<RoutesUiState> = _uiState

    init {
        loadRoutes()
    }

    private fun loadRoutes() {
        viewModelScope.launch {
            Log.d("RoutesViewModel", "Loading routes started")

            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val routes = routesRepository.getRoutes()
                Log.d("RoutesViewModel", "Routes loaded successfully: ${routes.size} routes found")
                _uiState.value = _uiState.value.copy(routes = routes, isLoading = false)
            } catch (e: Exception) {
                Log.e("RoutesViewModel", "Error loading routes: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to load routes: ${e.message}",
                    isLoading = false
                )
            }

            Log.d("RoutesViewModel", "Loading routes finished")
        }
    }


    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
