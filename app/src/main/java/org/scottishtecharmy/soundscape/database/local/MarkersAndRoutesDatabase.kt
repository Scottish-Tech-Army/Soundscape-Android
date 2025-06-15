package org.scottishtecharmy.soundscape.database.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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

    companion object {
        @Volatile
        private var INSTANCE: MarkersAndRoutesDatabase? = null
        fun getMarkersInstance(context: Context): MarkersAndRoutesDatabase {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context,
                        MarkersAndRoutesDatabase::class.java,
                        "markers_and_routes_database"
                    )
                        .allowMainThreadQueries()
                        .build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}
