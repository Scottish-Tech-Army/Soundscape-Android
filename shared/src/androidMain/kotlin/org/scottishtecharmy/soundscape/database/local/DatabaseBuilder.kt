package org.scottishtecharmy.soundscape.database.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<MarkersAndRoutesDatabase> {
    return Room.databaseBuilder(
        context,
        MarkersAndRoutesDatabase::class.java,
        "markers_and_routes_database"
    )
}

object MarkersAndRoutesDatabaseProvider {
    @Volatile
    private var INSTANCE: MarkersAndRoutesDatabase? = null

    fun getInstance(context: Context): MarkersAndRoutesDatabase {
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: getDatabaseBuilder(context)
                .allowMainThreadQueries()
                .build()
                .also { INSTANCE = it }
        }
    }
}
