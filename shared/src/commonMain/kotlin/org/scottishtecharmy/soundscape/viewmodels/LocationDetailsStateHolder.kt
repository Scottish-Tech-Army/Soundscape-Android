package org.scottishtecharmy.soundscape.viewmodels

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.audio.AudioTour
import org.scottishtecharmy.soundscape.audio.AudioType
import org.scottishtecharmy.soundscape.database.createMarker
import org.scottishtecharmy.soundscape.database.local.dao.RouteDao
import org.scottishtecharmy.soundscape.database.local.model.MarkerEntity
import org.scottishtecharmy.soundscape.geoengine.PositionedString
import org.scottishtecharmy.soundscape.geoengine.filters.TrackedCallout
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.services.ServiceConnection

class LocationDetailsStateHolder(
    private val connection: ServiceConnection,
    private val routeDao: RouteDao,
    private val audioTour: AudioTour,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        audioTour.onPlaceSelected()
    }

    fun startBeacon(location: LngLatAlt, name: String) {
        connection.service?.startBeacon(location, name)
        audioTour.onBeaconStarted()
    }

    fun enableStreetPreview(location: LngLatAlt) {
        connection.service?.setStreetPreviewMode(true, location)
    }

    fun createMarker(
        locationDescription: LocationDescription,
        successMessage: String,
        failureMessage: String,
        @Suppress("UNUSED_PARAMETER") duplicateMessage: String,
    ) {
        createMarker(
            locationDescription = locationDescription,
            routeDao = routeDao,
            scope = scope,
            onSuccess = {
                connection.service?.speakCallout(
                    TrackedCallout(
                        positionedStrings = listOf(
                            PositionedString(text = successMessage, type = AudioType.STANDARD)
                        ),
                        filter = false,
                    ),
                    false,
                )
                audioTour.onMarkerCreateDone()
            },
            onFailure = {
                connection.service?.speakCallout(
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

    fun deleteMarker(objectId: Long) {
        scope.launch {
            try {
                routeDao.removeMarker(objectId)
            } catch (e: Exception) {
                println("LocationDetailsStateHolder: Error deleting marker: ${e.message}")
            }
        }
    }

    fun getLocationDescription(location: LngLatAlt): LocationDescription? {
        return connection.service?.getLocationDescription(location)
    }

    suspend fun getMarkerAtLocation(location: LngLatAlt): MarkerEntity? {
        return routeDao.getMarkerByLocation(location.longitude, location.latitude)
    }

    fun showDialog() {
        audioTour.onMarkerCreateStarted()
    }

    fun dispose() {
        scope.cancel()
    }
}
