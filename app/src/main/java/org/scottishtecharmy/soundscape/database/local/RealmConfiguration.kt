package org.scottishtecharmy.soundscape.database.local

import android.util.Log
import org.scottishtecharmy.soundscape.database.local.model.TileData
import io.realm.kotlin.Realm
import io.realm.kotlin.Realm.Companion.deleteRealm
import io.realm.kotlin.RealmConfiguration
import org.scottishtecharmy.soundscape.database.local.model.Location
import org.scottishtecharmy.soundscape.database.local.model.RouteData
import org.scottishtecharmy.soundscape.database.local.model.RoutePoint
import org.scottishtecharmy.soundscape.utils.TileGrid.Companion.SOUNDSCAPE_TILE_BACKEND

object RealmConfiguration {
    private var tileDataRealm: Realm? = null
    private var markersRealm: Realm? = null

    fun getTileDataInstance(): Realm {
        // has this object been created or opened yet?
        if (tileDataRealm == null || tileDataRealm!!.isClosed()) {
            // create the realm db based on the TileData model/schema
            val config = RealmConfiguration.Builder(
                schema = setOf(TileData::class)
            ).name("TileData").build()

            // TODO: Whilst we are working on the protomap -> GeoJSON conversion we always want to
            //  start with a fresh tile database. Once the work is complete, then we can remove this
            //  code.
            if(!SOUNDSCAPE_TILE_BACKEND)
                deleteRealm(config)

            try {
                tileDataRealm = Realm.open(config)
            } catch(e: Exception) {
                Log.e("Realm", "Exception opening database: $e")

                // TODO: Is this really the best approach, deleting the database and trying again?
                // We're going to delete it and try again. This is likely due to a change in schema
                Log.e("Realm", "Deleting and re-trying")
                deleteRealm(config)
                tileDataRealm = Realm.open(config)
            }
        }
        return tileDataRealm!!
    }

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
            ).name("MarkersAndRoutes").inMemory().build()

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