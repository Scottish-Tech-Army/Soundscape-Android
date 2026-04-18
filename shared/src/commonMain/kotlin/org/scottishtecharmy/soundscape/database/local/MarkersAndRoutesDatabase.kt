package org.scottishtecharmy.soundscape.database.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import org.scottishtecharmy.soundscape.database.local.dao.RouteDao
import org.scottishtecharmy.soundscape.database.local.model.MarkerEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteMarkerCrossRef

@Database(
    entities = [RouteEntity::class, MarkerEntity::class, RouteMarkerCrossRef::class],
    version = 1,
    exportSchema = false
)
abstract class MarkersAndRoutesDatabase : RoomDatabase() {
    abstract fun routeDao(): RouteDao
}

// Room KMP generates the implementation via KSP
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object MarkersAndRoutesDatabaseConstructor : RoomDatabaseConstructor<MarkersAndRoutesDatabase>
