package org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen

import androidx.lifecycle.ViewModel
import org.scottishtecharmy.soundscape.SoundscapeServiceConnection
import org.scottishtecharmy.soundscape.database.local.dao.RouteDao
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription

class AddAndEditRouteViewModel(
    routeDao: RouteDao,
    soundscapeServiceConnection: SoundscapeServiceConnection,
) : ViewModel() {

    val holder = AddAndEditRouteStateHolder(routeDao, soundscapeServiceConnection)
    val uiState = holder.uiState
    val logic get() = holder.logic
    var postInit
        get() = holder.postInit
        set(value) { holder.postInit = value }

    fun loadMarkers() = holder.loadMarkers()
    fun initializeRoute(routeData: RouteWithMarkers) = holder.initializeRoute(routeData)
    fun initializeRouteFromData(routeData: RouteWithMarkers) = holder.initializeRouteFromData(routeData)
    fun initializeRouteFromDatabase(routeId: Long) = holder.initializeRouteFromDatabase(routeId)
    fun onNameChange(newText: String) = holder.onNameChange(newText)
    fun onDescriptionChange(newText: String) = holder.onDescriptionChange(newText)
    fun deleteRoute(objectId: Long) = holder.deleteRoute(objectId)
    fun resetDoneActionState() = holder.resetDoneActionState()
    fun clearErrorMessage() = holder.clearErrorMessage()
    fun editComplete(members: List<LocationDescription>) = holder.editComplete(members)
    fun onClickBack() = holder.onClickBack()
    fun onSelectLocation(desc: LocationDescription) = holder.onSelectLocation(desc)
    fun toggleMember(desc: LocationDescription) = holder.toggleMember(desc)
    fun onClickFolder(filter: String, title: String) = holder.onClickFolder(filter, title)
    fun createAndAddMarker(
        desc: LocationDescription,
        successMessage: String,
        failureMessage: String,
        duplicateMessage: String,
    ) = holder.createAndAddMarker(desc, successMessage, failureMessage, duplicateMessage)

    override fun onCleared() {
        super.onCleared()
        holder.dispose()
    }
}
