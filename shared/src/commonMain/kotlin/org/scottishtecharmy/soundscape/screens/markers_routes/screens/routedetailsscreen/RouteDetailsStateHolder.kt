package org.scottishtecharmy.soundscape.screens.markers_routes.screens.routedetailsscreen

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.scottishtecharmy.soundscape.database.local.dao.RouteDao
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.error_message_route_not_found
import org.scottishtecharmy.soundscape.services.ServiceConnection

class RouteDetailsStateHolder(
    private val routeDao: RouteDao,
    private val connection: ServiceConnection,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _uiState = MutableStateFlow(RouteDetailsUiState())
    val uiState: StateFlow<RouteDetailsUiState> = _uiState.asStateFlow()

    fun getRouteById(routeId: Long) {
        scope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val route = routeDao.getRouteWithMarkers(routeId)
                _uiState.value = _uiState.value.copy(route = route, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = runBlocking { getString(Res.string.error_message_route_not_found) },
                    isLoading = false,
                )
            }
        }
    }

    fun startRoute(routeId: Long) {
        connection.service?.routeStartById(routeId)
    }

    fun startRouteInReverse(routeId: Long) {
        connection.service?.routeStartReverse(routeId)
    }

    fun stopRoute() {
        connection.service?.routeStop()
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun dispose() {
        scope.cancel()
    }
}
