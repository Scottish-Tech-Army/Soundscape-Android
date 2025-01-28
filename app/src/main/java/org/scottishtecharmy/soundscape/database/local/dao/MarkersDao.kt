package org.scottishtecharmy.soundscape.database.local.dao

import android.util.Log
import io.realm.kotlin.Realm
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.ext.query
import io.realm.kotlin.query.RealmResults
import org.scottishtecharmy.soundscape.database.local.model.MarkerData

class MarkersDao(
    private val realm: Realm,
) {
    suspend fun insertMarker(route: MarkerData): Boolean {
        // If we don't catch the exception here, then it prevents write
        // from completing its transaction logic.
        var success = true
        realm.write {
            try {
                copyToRealm(route, updatePolicy = UpdatePolicy.ERROR)
            } catch (e: IllegalArgumentException) {
                Log.e("realm", e.message.toString())
                success = false
            }
        }
        return success
    }

    fun getMarker(addressName: String): RealmResults<MarkerData> = realm.query<MarkerData>("addressName == $0", addressName).find()

    fun getMarkers(): List<MarkerData> = realm.query<MarkerData>().find()

    suspend fun deleteMarker(addressName: String) =
        realm.write {
            val findMarker = query<MarkerData>("addressName == $0", addressName).find()
            Log.d("markerData", "Deleting marker \"" + addressName + "\" size " + findMarker.size)
            delete(findMarker)
        }

    suspend fun updateMarker(marker: MarkerData) =
        realm.write {
            val findMarker = query<MarkerData>("addressName == $0", marker.addressName).first().find()

            try {
                findMarker?.apply {
                    // name = route.name - not possible to update primary key
                    fullAddress = marker.fullAddress
                    location = marker.location
                }
            } catch (e: IllegalArgumentException) {
                Log.e("realm", e.message.toString())
            }
        }
}
