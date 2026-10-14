package org.scottishtecharmy.soundscape.database

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.database.local.dao.RouteDao
import org.scottishtecharmy.soundscape.database.local.model.MarkerEntity
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription

/**
 * Save a marker and return its id. A location with a non-zero `databaseId` updates that marker.
 * Otherwise, if a marker already exists at exactly this location, that marker is updated with the
 * new name and annotation rather than a duplicate being added - the same merge the archive restore
 * does. Only then is a new marker inserted.
 *
 * This is the one save path for Save as Marker, the beacon's Add to Markers action, the New Marker
 * screen and Add Waypoints, on both platforms.
 */
suspend fun saveMarker(locationDescription: LocationDescription, routeDao: RouteDao): Long {
    val name = locationDescription.name.ifEmpty { locationDescription.description ?: "Unknown" }
    val fullAddress = locationDescription.description ?: ""

    if (locationDescription.databaseId != 0L) {
        routeDao.updateMarker(
            MarkerEntity(
                markerId = locationDescription.databaseId,
                name = name,
                fullAddress = fullAddress,
                longitude = locationDescription.location.longitude,
                latitude = locationDescription.location.latitude,
            )
        )
        return locationDescription.databaseId
    }

    val existing = routeDao.getMarkerByLocation(
        locationDescription.location.longitude,
        locationDescription.location.latitude,
    )
    if (existing != null) {
        routeDao.updateMarker(
            MarkerEntity(
                markerId = existing.markerId,
                name = name,
                fullAddress = fullAddress,
                longitude = existing.longitude,
                latitude = existing.latitude,
            )
        )
        return existing.markerId
    }

    return routeDao.insertMarker(
        MarkerEntity(
            name = name,
            fullAddress = fullAddress,
            longitude = locationDescription.location.longitude,
            latitude = locationDescription.location.latitude,
        )
    )
}

/**
 * [saveMarker] on [scope], recording the saved marker's id in `locationDescription.databaseId`.
 * Calls `onSuccess` after a successful write or `onFailure` on any exception.
 */
fun createMarker(
    locationDescription: LocationDescription,
    routeDao: RouteDao,
    scope: CoroutineScope,
    onSuccess: () -> Unit,
    onFailure: () -> Unit,
) {
    scope.launch {
        try {
            locationDescription.databaseId = saveMarker(locationDescription, routeDao)
            onSuccess()
        } catch (e: Exception) {
            onFailure()
        }
    }
}
