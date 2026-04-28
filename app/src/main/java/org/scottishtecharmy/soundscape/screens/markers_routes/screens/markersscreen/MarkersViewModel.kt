package org.scottishtecharmy.soundscape.screens.markers_routes.screens.markersscreen

import androidx.lifecycle.ViewModel
import org.scottishtecharmy.soundscape.SoundscapeServiceConnection
import org.scottishtecharmy.soundscape.database.local.dao.RouteDao
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.preferences.PreferencesProvider

class MarkersViewModel(
    routeDao: RouteDao,
    prefs: PreferencesProvider,
    soundscapeServiceConnection: SoundscapeServiceConnection,
) : ViewModel() {

    val holder = MarkersStateHolder(routeDao, prefs, soundscapeServiceConnection)
    val uiState = holder.uiState

    fun toggleSortByName() = holder.toggleSortByName()
    fun toggleSortOrder() = holder.toggleSortOrder()
    fun clearErrorMessage() = holder.clearErrorMessage()
    fun startBeacon(location: LngLatAlt, name: String) = holder.startBeacon(location, name)

    override fun onCleared() {
        super.onCleared()
        holder.dispose()
    }
}
