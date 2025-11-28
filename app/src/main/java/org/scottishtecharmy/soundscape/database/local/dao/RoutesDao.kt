package org.scottishtecharmy.soundscape.database.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.scottishtecharmy.soundscape.database.local.model.MarkerEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteMarkerCrossRef
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers


@Dao
interface RouteDao {

    // --- Marker Operations ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarker(marker: MarkerEntity): Long // Returns the new markerId

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateMarker(marker: MarkerEntity)

    @Query("SELECT * FROM markers WHERE marker_id = :markerId")
    fun getMarkerById(markerId: Long): MarkerEntity?

    @Query("SELECT * FROM markers WHERE latitude = :latitude AND longitude = :longitude")
    fun getMarkerByLocation(longitude: Double, latitude: Double): MarkerEntity?

    @Query("SELECT * FROM markers")
    fun getAllMarkers(): List<MarkerEntity>

    @Query("SELECT * FROM markers")
    fun getAllMarkersFlow(): Flow<List<MarkerEntity>>

    // --- Route Operations ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: RouteEntity): Long // Returns the new routeId

    // --- RouteMarkerCrossRef Operations ---
    @Insert(onConflict = OnConflictStrategy.REPLACE) // Ignore if the marker is already in the route
    suspend fun addMarkerToRoute(crossRef: RouteMarkerCrossRef)

    @Query("DELETE FROM route_marker_cross_ref WHERE route_id = :routeId AND marker_id = :markerId")
    suspend fun removeMarkerFromRoute(routeId: Long, markerId: Long)

    @Query("DELETE FROM route_marker_cross_ref WHERE route_id = :routeId")
    suspend fun removeMarkersForRoute(routeId: Long)

    @Query("SELECT * FROM routes")
    fun getAllRoutes(): List<RouteEntity>

    // --- Querying Routes With Their Markers ---
    @Query("SELECT * FROM routes WHERE route_id = :routeId")
    fun getRouteById(routeId: Long): RouteEntity?

    @Query("SELECT * FROM route_marker_cross_ref WHERE route_id = :routeId")
    fun getMarkerCrossReference(routeId: Long): List<RouteMarkerCrossRef>

    @Transaction
    fun getRouteWithMarkers(routeId: Long): RouteWithMarkers? {
        val route = getRouteById(routeId)
        val crossReferences = getMarkerCrossReference(routeId)

        if(route == null) return null

        val markers = mutableListOf<MarkerEntity>()
        for(crossRef in crossReferences.sortedBy { it.markerOrder }) {
            val id = getMarkerById(crossRef.markerId)
            if(id != null)
                markers.add(id)
        }
        return RouteWithMarkers(route, markers)
    }

    @Query("SELECT * FROM routes")
    fun getAllRoutesFlow(): Flow<List<RouteEntity>>

    fun getAllRoutesWithMarkersFlow(): Flow<List<RouteWithMarkers>> {
        return getAllRoutesFlow().map { routes ->
            routes.mapNotNull { route ->
                getRouteWithMarkers(route.routeId)
            }
        }
    }

    @Transaction
    fun getAllRoutesWithMarkers(): List<RouteWithMarkers> {
        val routes = getAllRoutes()
        val result = mutableListOf<RouteWithMarkers>()

        for (route in routes) {
            val routeWithMarkers = getRouteWithMarkers(route.routeId)
            if (routeWithMarkers != null) {
                result.add(routeWithMarkers)
            }
        }
        return result
    }

    @Query("DELETE FROM routes WHERE route_id = :routeId")
    suspend fun removeRoute(routeId: Long)

    @Query("DELETE FROM markers WHERE marker_id = :markerId")
    suspend fun removeMarker(markerId: Long)

    /**
     * insertRouteWithExistingMarkers is used from the app to create a route using markers which
     * are already in the database.
     */
    @Transaction
    suspend fun insertRouteWithExistingMarkers(route: RouteEntity, markers: List<MarkerEntity>) : Long {
        val routeId = insertRoute(route)
        markers.forEachIndexed { index, marker ->
            addMarkerToRoute(RouteMarkerCrossRef(routeId, marker.markerId, index))
        }
        return routeId
    }

    /**
     * insertRouteWithNewMarkers is used from test code and creates a route AND all of the markers
     * within it.
     */
    @Transaction
    suspend fun insertRouteWithNewMarkers(route: RouteEntity, markers: List<MarkerEntity>) : Long {

        var duplicateId = 0L
        val existingRoutes = getAllRoutesWithMarkers()
        for(existingRoute in existingRoutes) {
            if(route == existingRoute.route) {
                if(markers == existingRoute.markers) {
                    duplicateId = existingRoute.route.routeId
                    break
                }
            }
        }
        if(duplicateId == 0L) {
            val routeId = insertRoute(route)
            markers.forEachIndexed { index, marker ->
                // Check if the marker already exists
                val existingMarker = getMarkerByLocation(marker.longitude, marker.latitude)
                val markerId = existingMarker?.markerId ?: insertMarker(marker)
                addMarkerToRoute(RouteMarkerCrossRef(routeId, markerId, index))
            }
            return routeId
        } else {
            return duplicateId
        }
    }
}