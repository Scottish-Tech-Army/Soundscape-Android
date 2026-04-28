package org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.scottishtecharmy.soundscape.audio.AudioType
import org.scottishtecharmy.soundscape.database.createMarker
import org.scottishtecharmy.soundscape.database.local.dao.RouteDao
import org.scottishtecharmy.soundscape.database.local.model.MarkerEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers
import org.scottishtecharmy.soundscape.geoengine.PositionedString
import org.scottishtecharmy.soundscape.geoengine.filters.TrackedCallout
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.error_message_deleting_route
import org.scottishtecharmy.soundscape.resources.error_message_route_not_found
import org.scottishtecharmy.soundscape.resources.error_message_saving_route
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.placesnearby.PlacesNearbyStateHolder
import org.scottishtecharmy.soundscape.services.ServiceConnection

class AddAndEditRouteStateHolder(
    private val routeDao: RouteDao,
    private val connection: ServiceConnection,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _uiState = MutableStateFlow(AddAndEditRouteUiState())
    val uiState: StateFlow<AddAndEditRouteUiState> = _uiState

    val logic = PlacesNearbyStateHolder(connection)
    var postInit = false

    fun loadMarkers() {
        scope.launch {
            routeDao.getAllMarkersFlow().collect { markers ->
                val markerVMs = markers.map {
                    LocationDescription(
                        name = it.name,
                        location = LngLatAlt(it.longitude, it.latitude),
                        databaseId = it.markerId,
                    )
                }

                if (postInit) {
                    var newMarker: LocationDescription? = null
                    for (marker in markerVMs) {
                        if (!_uiState.value.markers.contains(marker)) newMarker = marker
                    }
                    if (newMarker != null) {
                        val updatedList = _uiState.value.toggledMembers.toMutableList()
                        updatedList.add(newMarker)
                        _uiState.value = _uiState.value.copy(
                            markers = markerVMs.toMutableList(),
                            toggledMembers = updatedList,
                        )
                        return@collect
                    }
                } else {
                    _uiState.value = _uiState.value.copy(markers = markerVMs.toMutableList())
                }
                postInit = true
            }
        }
    }

    fun initializeRoute(routeData: RouteWithMarkers) {
        val routeMembers = mutableListOf<LocationDescription>()
        for ((index, waypoint) in routeData.markers.withIndex()) {
            routeMembers.add(
                LocationDescription(
                    name = waypoint.name,
                    location = LngLatAlt(waypoint.longitude, waypoint.latitude),
                    orderId = index.toLong(),
                    databaseId = waypoint.markerId,
                )
            )
        }
        _uiState.value = _uiState.value.copy(
            name = routeData.route.name,
            description = routeData.route.description,
            routeMembers = routeMembers,
            routeObjectId = routeData.route.routeId,
        )
    }

    fun initializeRouteFromData(routeData: RouteWithMarkers) {
        scope.launch {
            try {
                initializeRoute(routeData)
            } catch (e: Exception) {
                println("AddAndEditRouteStateHolder: Error loading route: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    errorMessage = runBlocking { getString(Res.string.error_message_route_not_found) },
                )
            }
        }
    }

    /**
     * Pre-populates the holder from an imported (unsaved) RouteWithMarkers, e.g.
     * the result of opening a Soundscape JSON or GPX file. Markers are presented
     * as routeMembers with synthetic order ids so the user can review and save.
     */
    fun initializeFromImport(route: RouteWithMarkers) {
        val members = route.markers.mapIndexed { index, waypoint ->
            LocationDescription(
                name = waypoint.name,
                location = LngLatAlt(waypoint.longitude, waypoint.latitude),
                description = waypoint.fullAddress,
                orderId = index.toLong(),
                databaseId = 0L,
            )
        }
        val name = route.route.name
        val description = route.route.description
        _uiState.value = _uiState.value.copy(
            name = name,
            description = description,
            routeMembers = members,
            routeObjectId = 0L,
            showDoneButton = name.isNotBlank() && description.isNotBlank(),
        )
    }

    fun initializeRouteFromDatabase(routeId: Long) {
        scope.launch {
            try {
                val route = routeDao.getRouteWithMarkers(routeId) ?: throw Exception("Route not found")
                initializeRoute(route)
            } catch (e: Exception) {
                println("AddAndEditRouteStateHolder: Error loading route: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    errorMessage = runBlocking { getString(Res.string.error_message_route_not_found) },
                )
            }
        }
    }

    fun onNameChange(newText: String) {
        val showDoneButton = newText.isNotBlank() && _uiState.value.description.isNotBlank()
        _uiState.value = _uiState.value.copy(name = newText, showDoneButton = showDoneButton)
    }

    fun onDescriptionChange(newText: String) {
        val showDoneButton = _uiState.value.name.isNotBlank() && newText.isNotBlank()
        _uiState.value = _uiState.value.copy(description = newText, showDoneButton = showDoneButton)
    }

    fun deleteRoute(objectId: Long) {
        scope.launch {
            try {
                routeDao.removeRoute(objectId)
                _uiState.value = _uiState.value.copy(
                    doneActionCompleted = true,
                    actionType = ActionType.DELETE,
                )
            } catch (e: Exception) {
                println("AddAndEditRouteStateHolder: Error deleting route: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    errorMessage = runBlocking { getString(Res.string.error_message_deleting_route) },
                )
            }
        }
    }

    fun resetDoneActionState() {
        _uiState.value = _uiState.value.copy(doneActionCompleted = false, actionType = ActionType.NONE)
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun editComplete(members: List<LocationDescription>) {
        _uiState.value = _uiState.value.copy(routeMembers = members, toggledMembers = emptyList())
        scope.launch {
            val routeData = RouteEntity(
                routeId = _uiState.value.routeObjectId ?: 0L,
                name = _uiState.value.name,
                description = _uiState.value.description,
            )
            if (_uiState.value.actionType == ActionType.DELETE) {
                routeDao.removeRoute(_uiState.value.routeObjectId ?: 0L)
                return@launch
            }
            val markers = mutableListOf<MarkerEntity>()
            var newMarkers = false
            _uiState.value.routeMembers.forEach {
                markers.add(
                    MarkerEntity(
                        name = it.name,
                        longitude = it.location.longitude,
                        latitude = it.location.latitude,
                        markerId = it.databaseId,
                    )
                )
                if (it.databaseId == 0L) newMarkers = true
            }
            try {
                if (newMarkers) routeDao.insertRouteWithNewMarkers(routeData, markers)
                else routeDao.insertRouteWithExistingMarkers(routeData, markers)
                _uiState.value = _uiState.value.copy(
                    doneActionCompleted = true,
                    actionType = ActionType.UPDATE,
                )
            } catch (e: Exception) {
                println("AddAndEditRouteStateHolder: Error saving route: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    errorMessage = runBlocking { getString(Res.string.error_message_saving_route) },
                )
            }
        }
    }

    fun onClickBack() {
        var newLevel = logic.uiState.value.level
        if (newLevel > 0) newLevel -= 1
        logic.internalUiState.value = logic.uiState.value.copy(level = newLevel)
    }

    fun onSelectLocation(locationDescription: LocationDescription) {
        scope.launch {
            val existingMarker = routeDao.getMarkerByLocation(
                locationDescription.location.longitude,
                locationDescription.location.latitude,
            )
            if (existingMarker != null) {
                val existingDesc = LocationDescription(
                    name = existingMarker.name,
                    location = LngLatAlt(existingMarker.longitude, existingMarker.latitude),
                    databaseId = existingMarker.markerId,
                )
                logic.internalUiState.value = logic.uiState.value.copy(markerDescription = existingDesc)
            } else {
                logic.internalUiState.value = logic.uiState.value.copy(markerDescription = locationDescription)
            }
        }
    }

    fun toggleMember(locationDescription: LocationDescription) {
        val currentToggled = _uiState.value.toggledMembers
        val isToggled = currentToggled.any { it.databaseId == locationDescription.databaseId }
        val newToggled = if (isToggled) {
            currentToggled.filter { it.databaseId != locationDescription.databaseId }
        } else {
            currentToggled + locationDescription
        }
        _uiState.value = _uiState.value.copy(toggledMembers = newToggled)
    }

    fun onClickFolder(filter: String, title: String) {
        val newLevel = logic.uiState.value.level + 1
        logic.internalUiState.value = logic.uiState.value.copy(
            level = newLevel,
            filter = filter,
            title = title,
        )
    }

    fun createAndAddMarker(
        locationDescription: LocationDescription,
        successMessage: String,
        failureMessage: String,
        duplicateMessage: String,
    ) {
        scope.launch {
            val service = connection.service
            val existingMarker = routeDao.getMarkerByLocation(
                locationDescription.location.longitude,
                locationDescription.location.latitude,
            )
            if (existingMarker != null) {
                val existingDesc = LocationDescription(
                    name = existingMarker.name,
                    location = LngLatAlt(existingMarker.longitude, existingMarker.latitude),
                    databaseId = existingMarker.markerId,
                )
                val markerInRoute = _uiState.value.routeMembers.any { it.databaseId == existingDesc.databaseId }
                val markerToggled = _uiState.value.toggledMembers.any { it.databaseId == existingDesc.databaseId }
                if (markerInRoute != markerToggled) {
                    service?.speakCallout(
                        TrackedCallout(
                            positionedStrings = listOf(
                                PositionedString(text = duplicateMessage, type = AudioType.STANDARD)
                            ),
                            filter = false,
                        ),
                        false,
                    )
                } else {
                    val updatedList = _uiState.value.toggledMembers.toMutableList()
                    updatedList.add(existingDesc)
                    _uiState.value = _uiState.value.copy(toggledMembers = updatedList)
                    service?.speakCallout(
                        TrackedCallout(
                            positionedStrings = listOf(
                                PositionedString(text = successMessage, type = AudioType.STANDARD)
                            ),
                            filter = false,
                        ),
                        false,
                    )
                }
            } else {
                createMarker(
                    locationDescription, routeDao, scope,
                    onSuccess = {
                        service?.speakCallout(
                            TrackedCallout(
                                positionedStrings = listOf(
                                    PositionedString(text = successMessage, type = AudioType.STANDARD)
                                ),
                                filter = false,
                            ),
                            false,
                        )
                    },
                    onFailure = {
                        service?.speakCallout(
                            TrackedCallout(
                                positionedStrings = listOf(
                                    PositionedString(text = failureMessage, type = AudioType.STANDARD)
                                ),
                                filter = false,
                            ),
                            false,
                        )
                    },
                )
            }

            logic.internalUiState.value = logic.uiState.value.copy(
                markerDescription = null,
                level = 0,
            )
        }
    }

    fun dispose() {
        scope.cancel()
        logic.dispose()
    }
}
