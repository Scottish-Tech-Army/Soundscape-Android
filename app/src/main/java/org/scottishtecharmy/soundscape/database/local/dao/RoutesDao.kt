package org.scottishtecharmy.soundscape.database.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.scottishtecharmy.soundscape.database.local.model.MarkerEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteMarkerCrossRef
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers


@Dao
interface RouteDao {

    // --- Marker Operations ---
    @Insert(onConflict = OnConflictStrategy.REPLACE) // Or IGNORE if you don't want to update existing
    suspend fun insertMarker(marker: MarkerEntity): Long // Returns the new markerId

    @Query("SELECT * FROM markers WHERE marker_id = :markerId")
    fun getMarkerById(markerId: Long): MarkerEntity?

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

    @Transaction
    @Query("SELECT * FROM routes")
    fun getAllRoutesWithMarkersFlow(): Flow<List<RouteWithMarkers>>

    @Transaction
    @Query("SELECT * FROM routes")
    fun getAllRoutesWithMarkers(): List<RouteWithMarkers>

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
        val routeId = insertRoute(route)
        markers.forEachIndexed { index, marker ->
            val markerId = insertMarker(marker)
            addMarkerToRoute(RouteMarkerCrossRef(routeId, markerId, index))
        }
        return routeId
    }
}