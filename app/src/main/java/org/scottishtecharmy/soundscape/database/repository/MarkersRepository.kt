package org.scottishtecharmy.soundscape.database.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.scottishtecharmy.soundscape.database.local.dao.MarkersDao
import org.scottishtecharmy.soundscape.database.local.model.MarkerData

class MarkersRepository(
    private val markersDao: MarkersDao,
) {
    suspend fun insertMarker(marker: MarkerData) =
        withContext(Dispatchers.IO) {
            markersDao.insertMarker(marker)
        }

    suspend fun getMarkers() =
        withContext(Dispatchers.IO) {
            markersDao.getMarkers()
        }
}
