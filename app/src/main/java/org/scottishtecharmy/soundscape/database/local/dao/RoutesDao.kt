package org.scottishtecharmy.soundscape.database.local.dao

import android.util.Log
import io.realm.kotlin.Realm
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.annotations.ExperimentalGeoSpatialApi
import io.realm.kotlin.ext.query
import io.realm.kotlin.query.RealmResults
import io.realm.kotlin.types.geo.Distance
import io.realm.kotlin.types.geo.GeoCircle
import io.realm.kotlin.types.geo.GeoPoint
import org.scottishtecharmy.soundscape.database.local.model.Location
import org.scottishtecharmy.soundscape.database.local.model.RouteData
import org.scottishtecharmy.soundscape.database.local.model.RoutePoint

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

    fun getRoutes(): List<RouteData> {
        return realm.query<RouteData>().find()
    }

    fun getWaypoints(): List<RoutePoint> {
        return realm.query<RoutePoint>().find()
    }

    @OptIn(ExperimentalGeoSpatialApi::class)
    fun getWaypointsNear(location: Location?, kilometre: Double): RealmResults<RoutePoint> {
        if(location != null) {
            val circle1 = GeoCircle.create(
                center = GeoPoint.create(location.latitude, location.longitude),
                radius = Distance.fromKilometers(kilometre)
            )
            return realm.query<RoutePoint>("location GEOWITHIN $circle1").find()
        }
        return realm.query<RoutePoint>().find()
    }

    suspend fun deleteRoute(name: String) = realm.write {

        val findRoute = query<RouteData>("name == $0", name).find()
        Log.d("routeDao", "Deleting route \"" + name + "\" size " + findRoute.size)
        delete(findRoute)

        // Clean up all waypoints which have no route
        val waypoints = query<RoutePoint>("route.@count == 0").find()
        Log.d("routeDao", "Deleting waypoints with no route" + waypoints.size)
        delete(waypoints)
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