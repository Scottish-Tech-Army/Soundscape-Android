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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.mongodb.kbson.ObjectId
import org.scottishtecharmy.soundscape.database.local.model.Location
import org.scottishtecharmy.soundscape.database.local.model.RouteData
import org.scottishtecharmy.soundscape.database.local.model.MarkerData

class RoutesDao(private val realm: Realm) {

    suspend fun insertRoute(route: RouteData) : Boolean
    {
        // If we don't catch the exception here, then it prevents write
        // from completing its transaction logic.
        var success = true
        realm.write {
            try {
                copyToRealm(route, updatePolicy = UpdatePolicy.ALL)
            } catch (e: IllegalArgumentException) {
                Log.e("realm", e.message.toString())
                success = false
            }
        }
        return success
    }

    suspend fun insertMarker(waypoint: MarkerData) : Boolean
    {
        // If we don't catch the exception here, then it prevents write
        // from completing its transaction logic.
        var success = true
        realm.write {
            try {
                copyToRealm(waypoint, updatePolicy = UpdatePolicy.ERROR)
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

    fun getRoute(objectId: ObjectId): RealmResults<RouteData> {
        return realm.query<RouteData>("objectId == $0", objectId).find()
    }

    fun getRoutes(): List<RouteData> {
        return realm.query<RouteData>().find()
    }

    fun getMarkers(): List<MarkerData> {
        return realm.query<MarkerData>().find()
    }

    fun getMarker(objectId: ObjectId): RealmResults<MarkerData> {
        return realm.query<MarkerData>("objectId == $0", objectId).find()
    }

    @OptIn(ExperimentalGeoSpatialApi::class)
    fun getMarkersNear(location: Location?, kilometre: Double): RealmResults<MarkerData> {
        if(location != null) {
            val circle1 = GeoCircle.create(
                center = GeoPoint.create(location.latitude, location.longitude),
                radius = Distance.fromKilometers(kilometre)
            )
            return realm.query<MarkerData>("location GEOWITHIN $circle1").find()
        }
        return realm.query<MarkerData>().find()
    }

    suspend fun deleteRoute(objectId: ObjectId) = realm.write {
        val findRoute = query<RouteData>("objectId == $0", objectId).find()
        Log.d("routeDao", "Deleting route \"" + objectId.toString() + "\" size " + findRoute.size)
        delete(findRoute)

        // We leave the markers in the database
    }

    suspend fun updateRoute(route: RouteData) = realm.write {
        val findRoute = query<RouteData>("objectId == $0", route.objectId).first().find()

        try {
            findRoute?.apply {
                // objectId = route.objectId - not possible to update primary key
                name = route.name
                description = route.description
                waypoints = route.waypoints
            }
        } catch (e: IllegalArgumentException) {
            Log.e("realm", e.message.toString())
        }
    }

    suspend fun deleteMarker(objectId: ObjectId) = realm.write {
        val findMarker = query<MarkerData>("objectId == $0", objectId).find()
        Log.d("routeDao", "Deleting marker \"" + objectId.toString() + "\" size " + findMarker.size)
        if(findMarker.isNotEmpty()) {
            val markers = getMarkers()
            for(marker in markers) {
                if(marker.objectId == objectId) {
                    Log.d("routeDao", "Marker found $objectId")
                } else {
                    Log.d("routeDao", "Marker ignored ${marker.addressName}, ${marker.objectId}")
                }
            }
        }
        delete(findMarker)
    }

    suspend fun updateMarker(marker: MarkerData) = realm.write {
        val findMarker = query<MarkerData>("objectId == $0", marker.objectId).first().find()

        try {
            findMarker?.apply {
                //objectId = marker.objectId            Not possible to update primary key
                addressName = marker.addressName
                fullAddress = marker.fullAddress
                location = marker.location
            }
        } catch (e: IllegalArgumentException) {
            Log.e("realm", e.message.toString())
        }
    }

    fun getRouteFlow() : Flow<List<RouteData>> {
        return realm.query<RouteData>().asFlow().map { changes ->
            changes.list
        }
    }

    fun getMarkerFlow() : Flow<List<MarkerData>> {
        return realm.query<MarkerData>().asFlow().map { changes ->
            changes.list
        }
    }
}