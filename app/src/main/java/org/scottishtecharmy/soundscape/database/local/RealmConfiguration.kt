package org.scottishtecharmy.soundscape.database.local

import android.util.Log
import io.realm.kotlin.Realm
import io.realm.kotlin.Realm.Companion.deleteRealm
import io.realm.kotlin.RealmConfiguration
import org.scottishtecharmy.soundscape.database.local.model.Location
import org.scottishtecharmy.soundscape.database.local.model.RouteData
import org.scottishtecharmy.soundscape.database.local.model.RoutePoint

object RealmConfiguration {
    private var markersRealm: Realm? = null

    /**
     * getMarkersInstance opens the Realm database which stores the markers and routes.
     * @param startAfresh if true results in the database being deleted and re-created.
     *
     * The database is current created inMemory so that it only exists when Soundscape is opened
     * with a GPX file. If it's opened in any other way then the database will be empty.
     */
    fun getMarkersInstance(startAfresh: Boolean = false): Realm {

        Log.d("RealmConfiguration", "getMarkersInstance: $markersRealm, $startAfresh")
        if(startAfresh && (markersRealm != null) && !markersRealm!!.isClosed()) {
            // The database is open, but we want to start afresh, so close it
            Log.d("RealmConfiguration", "Close the database so that we can re-open it")
            markersRealm?.close()
        }

        if (markersRealm == null || markersRealm!!.isClosed()) {
            val config = RealmConfiguration.Builder(
                schema = setOf(
                    RouteData::class,
                    RoutePoint::class,
                    Location::class
                )
            ).name("MarkersAndRoutes").build()

            // TODO: Once we are happy with the format of the database, this option should be removed
            if(startAfresh) {
                // Delete the realm so that we start a brand new database
                Log.d("RealmConfiguration", "Delete Markers database")
                deleteRealm(config)
            }
            markersRealm = Realm.open(config)
        }

        return markersRealm!!
    }
}