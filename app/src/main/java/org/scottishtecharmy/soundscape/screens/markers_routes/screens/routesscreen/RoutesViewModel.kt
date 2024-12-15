package org.scottishtecharmy.soundscape.screens.markers_routes.screens.routesscreen

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.database.local.model.RouteData
import org.scottishtecharmy.soundscape.database.repository.RoutesRepository
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.getSortOrderPreference
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.saveSortOrderPreference
import javax.inject.Inject

@HiltViewModel
class RoutesViewModel @Inject constructor(
    private val routesRepository: RoutesRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutesUiState())
    val uiState: StateFlow<RoutesUiState> = _uiState

    init {
        loadRoutes()
        // Load the saved sort order when initializing the ViewModel
        val isAscending = getSortOrderPreference(context)
        _uiState.value = _uiState.value.copy(isSortByName = isAscending)
        sortRoutes(isAscending)
    }

    fun toggleSortOrder() {
        val isAscending = !_uiState.value.isSortByName
        sortRoutes(isAscending)
        saveSortOrderPreference(context, isAscending)
    }

    private fun sortRoutes(isAscending: Boolean) {
        val sortedRoutes = if (isAscending) {
            _uiState.value.routes.sortedBy { it.name }
        } else {
            _uiState.value.routes
        }
        _uiState.value = _uiState.value.copy(routes = sortedRoutes, isSortByName = isAscending)
    }

    private fun loadRoutes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val routes = routesRepository.getRoutes()
                val isAscending = getSortOrderPreference(context)
                val sortedRoutes = if (isAscending) {
                    routes.sortedBy { it.name }
                } else {
                    routes
                }
                _uiState.value = _uiState.value.copy(routes = sortedRoutes, isLoading = false, isSortByName = isAscending)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to load routes: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
