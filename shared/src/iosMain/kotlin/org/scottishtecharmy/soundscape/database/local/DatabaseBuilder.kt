package org.scottishtecharmy.soundscape.database.local

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import platform.Foundation.NSHomeDirectory

fun getDatabaseBuilder(): RoomDatabase.Builder<MarkersAndRoutesDatabase> {
    val dbFilePath = NSHomeDirectory() + "/Documents/markers_and_routes_database"
    return Room.databaseBuilder<MarkersAndRoutesDatabase>(
        name = dbFilePath
    )
}

object MarkersAndRoutesDatabaseProvider {
    private var INSTANCE: MarkersAndRoutesDatabase? = null

    fun getInstance(): MarkersAndRoutesDatabase {
        return INSTANCE ?: getDatabaseBuilder()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()
            .also { INSTANCE = it }
    }
}
