package org.scottishtecharmy.soundscape.database.local.dao

import android.util.Log
import io.realm.kotlin.Realm
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.ext.query
import io.realm.kotlin.query.RealmResults
import org.scottishtecharmy.soundscape.database.local.model.RouteData

class RoutesDao(private val realm: Realm) {

    suspend fun insertRoute(route: RouteData) : Boolean
    {
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

    fun getRoute(name: String): RealmResults<RouteData> {
        return realm.query<RouteData>("name == $0", name).find()
    }

    suspend fun deleteRoute(name: String) = realm.write {

        val findRoute = query<RouteData>("name == $0", name).find()
        delete(findRoute)
    }

    suspend fun updateRoute(route: RouteData) = realm.write {
        val findRoute = query<RouteData>("name == $0", route.name).first().find()

        try {
            findRoute?.apply {
                //name = route.name - not possible to update primary key
                description = route.description
                waypoints = route.waypoints
            }
        } catch (e: IllegalArgumentException) {
            Log.e("realm", e.message.toString())
        }
    }

}