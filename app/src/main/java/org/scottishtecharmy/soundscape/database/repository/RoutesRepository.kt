package org.scottishtecharmy.soundscape.database.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.mongodb.kbson.ObjectId
import org.scottishtecharmy.soundscape.database.local.dao.RoutesDao
import org.scottishtecharmy.soundscape.database.local.model.Location
import org.scottishtecharmy.soundscape.database.local.model.RouteData
import org.scottishtecharmy.soundscape.database.local.model.MarkerData

class RoutesRepository(private val routesDao: RoutesDao) {

    suspend fun insertRoute(routeData: RouteData) = withContext(Dispatchers.IO) {
        routesDao.insertRoute(routeData)
    }

    suspend fun insertMarker(markerData: MarkerData) = withContext(Dispatchers.IO) {
        routesDao.insertMarker(markerData)
    }

    suspend fun getRoute(name: String) = withContext(Dispatchers.IO){
        routesDao.getRoute(name)
    }

    suspend fun getRoutes() = withContext(Dispatchers.IO){
        routesDao.getRoutes()
    }

    suspend fun deleteRoute(objectId: ObjectId) = withContext(Dispatchers.IO) {
        routesDao.deleteRoute(objectId)
    }

    suspend fun deleteMarker(objectId: ObjectId) = withContext(Dispatchers.IO) {
        routesDao.deleteMarker(objectId)
    }

    suspend fun updateRoute(routeData: RouteData) = withContext(Dispatchers.IO) {
        routesDao.updateRoute(routeData)
    }

    suspend fun updateMarker(markerData: MarkerData) = withContext(Dispatchers.IO) {
        routesDao.updateMarker(markerData)
    }

    suspend fun getMarkers() = withContext(Dispatchers.IO){
        routesDao.getMarkers()
    }

    suspend fun getMarker(objectId: ObjectId) = withContext(Dispatchers.IO){
        routesDao.getMarker(objectId)
    }

    suspend fun getMarkersNear(location: Location?, kilometre: Double) = withContext(Dispatchers.IO){
        routesDao.getMarkersNear(location, kilometre)
    }

    fun getRouteFlow() : Flow<List<RouteData>> {
        return routesDao.getRouteFlow()
    }

    fun getMarkerFlow() : Flow<List<MarkerData>> {
        return routesDao.getMarkerFlow()
    }
}