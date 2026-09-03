package org.scottishtecharmy.soundscape.database.local

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.concurrent.Volatile
import kotlinx.coroutines.Dispatchers
import platform.Foundation.NSLock
import platform.Foundation.NSHomeDirectory

fun getDatabaseBuilder(): RoomDatabase.Builder<MarkersAndRoutesDatabase> {
    val dbFilePath = NSHomeDirectory() + "/Documents/markers_and_routes_database"
    return Room.databaseBuilder<MarkersAndRoutesDatabase>(
        name = dbFilePath
    )
}

object MarkersAndRoutesDatabaseProvider {

    // The unguarded `INSTANCE ?: build()` this replaces was safe only while the database
    // was reached from the UI on the main thread. It no longer is: an App Intent runs on
    // a system thread and can call in while the app is opening the database for itself,
    // and losing that race builds two Room instances over one SQLite file.
    private val lock = NSLock()

    @Volatile
    private var INSTANCE: MarkersAndRoutesDatabase? = null

    fun getInstance(): MarkersAndRoutesDatabase {
        INSTANCE?.let { return it }
        lock.lock()
        try {
            INSTANCE?.let { return it }
            return getDatabaseBuilder()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.Default)
                .build()
                .also { INSTANCE = it }
        } finally {
            lock.unlock()
        }
    }
}
