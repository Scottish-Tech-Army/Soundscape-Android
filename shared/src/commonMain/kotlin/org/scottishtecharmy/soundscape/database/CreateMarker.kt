package org.scottishtecharmy.soundscape.database

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.database.local.dao.RouteDao
import org.scottishtecharmy.soundscape.database.local.model.MarkerEntity
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription

/**
 * Insert a new marker, or update an existing one if its `databaseId` is non-zero.
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
        var name = locationDescription.name
        if (name.isEmpty()) {
            name = locationDescription.description ?: "Unknown"
        }

        var updated = false
        if (locationDescription.databaseId != 0L) {
            val markerData = MarkerEntity(
                markerId = locationDescription.databaseId,
                name = name,
                fullAddress = locationDescription.description ?: "",
                longitude = locationDescription.location.longitude,
                latitude = locationDescription.location.latitude,
            )
            try {
                routeDao.updateMarker(markerData)
                onSuccess()
                updated = true
            } catch (e: Exception) {
                onFailure()
            }
        }
        if (!updated) {
            val marker = MarkerEntity(
                name = name,
                fullAddress = locationDescription.description ?: "",
                longitude = locationDescription.location.longitude,
                latitude = locationDescription.location.latitude,
            )
            try {
                locationDescription.databaseId = routeDao.insertMarker(marker)
                onSuccess()
            } catch (e: Exception) {
                onFailure()
            }
        }
    }
}
