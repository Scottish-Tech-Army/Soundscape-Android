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
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.getSortFieldPreference
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.getSortOrderPreference
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.saveSortFieldPreference
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
        // Load the saved sort orders
        _uiState.value = _uiState.value.copy(
            isSortByName = getSortFieldPreference(context),
            isSortAscending = getSortOrderPreference(context))

        // Collect the flow of routes from the repository so that we update when routes are added
        // and deleted
        viewModelScope.launch {
            routesRepository.getRouteFlow().collect { routes ->
                _uiState.value =  uiState.value.copy(routes = sortRoutes(routes))
            }
        }
    }

    fun toggleSortOrder() {
        val sortByAscending = !_uiState.value.isSortAscending
        _uiState.value =  uiState.value.copy(isSortAscending = sortByAscending, routes = sortRoutes(uiState.value.routes))
        saveSortOrderPreference(context, sortByAscending)
    }

    fun toggleSortByName() {
        val sortByName = !_uiState.value.isSortByName
        _uiState.value =  uiState.value.copy(isSortByName = sortByName, routes = sortRoutes(uiState.value.routes))
        saveSortFieldPreference(context, sortByName)
    }

    private fun sortRoutes(routes: List<RouteData>) : List<RouteData> {
        return if(_uiState.value.isSortByName) {
            if(_uiState.value.isSortAscending)
                routes.sortedBy { it.name }
            else
                routes.sortedByDescending { it.name }

        } else {
            if(_uiState.value.isSortAscending)
                routes.sortedBy { it.objectId }
            else
                routes.sortedByDescending { it.objectId }
        }
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
