package org.scottishtecharmy.soundscape.screens.markers_routes.screens.routesscreen

import androidx.lifecycle.ViewModel
import org.scottishtecharmy.soundscape.SoundscapeServiceConnection
import org.scottishtecharmy.soundscape.database.local.dao.RouteDao
import org.scottishtecharmy.soundscape.preferences.PreferencesProvider

class RoutesViewModel(
    routeDao: RouteDao,
    prefs: PreferencesProvider,
    soundscapeServiceConnection: SoundscapeServiceConnection,
) : ViewModel() {

    val holder = RoutesStateHolder(routeDao, prefs, soundscapeServiceConnection)
    val uiState = holder.uiState

    fun toggleSortByName() = holder.toggleSortByName()
    fun toggleSortOrder() = holder.toggleSortOrder()
    fun clearErrorMessage() = holder.clearErrorMessage()
    fun startRoute(routeId: Long) = holder.startRoute(routeId)

    override fun onCleared() {
        super.onCleared()
        holder.dispose()
    }
}
