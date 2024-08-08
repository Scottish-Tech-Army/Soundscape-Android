package org.scottishtecharmy.soundscape.database.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.scottishtecharmy.soundscape.database.local.dao.RoutesDao
import org.scottishtecharmy.soundscape.database.local.model.RouteData

class RoutesRepository(val routesDao: RoutesDao) {

    suspend fun insertRoute(route: RouteData) = withContext(Dispatchers.IO) {
        routesDao.insertRoute(route)
    }

    suspend fun getRoute(name: String) = withContext(Dispatchers.IO){
        routesDao.getRoute(name)
    }

    suspend fun deleteRoute(name: String) = withContext(Dispatchers.IO) {
        routesDao.deleteRoute(name)
    }

    suspend fun updateRoute(name: RouteData) = withContext(Dispatchers.IO) {
        routesDao.updateRoute(name)
    }
}