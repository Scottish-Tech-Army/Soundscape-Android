package org.scottishtecharmy.soundscape.navigation

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription

/**
 * Per-NavBackStackEntry holder for transient values that travel between
 * destinations on navigation. Storage is keyed by entry id, so navigating
 * Home → A → B → A keeps A's data isolated from B's. The holder provides
 * a typed `setOnNext` helper that writes to the freshly-created entry's id
 * after a `navController.navigate(...)` so the destination composable reads
 * its own row by `entry.id`.
 *
 * Pruning: callers should invoke [prune] periodically (e.g. via a
 * `NavController.addOnDestinationChangedListener`) to drop ids that are no
 * longer on the back stack and avoid leaks.
 *
 * The remaining `pendingImportRoute` slot is a true singleton transient
 * (consumed and cleared by ADD_ROUTE on first read) — it is set by the
 * intent dispatcher *before* navigating, so there is no entry id to key
 * against at write time.
 */
class NavigationStateHolder {
    private val _selectedLocations =
        MutableStateFlow<Map<String, LocationDescription>>(emptyMap())
    val selectedLocations: StateFlow<Map<String, LocationDescription>> =
        _selectedLocations.asStateFlow()

    private val _offlineMapsTargets =
        MutableStateFlow<Map<String, LngLatAlt>>(emptyMap())
    val offlineMapsTargets: StateFlow<Map<String, LngLatAlt>> =
        _offlineMapsTargets.asStateFlow()

    private val _pendingImportRoute = MutableStateFlow<RouteWithMarkers?>(null)
    val pendingImportRoute: StateFlow<RouteWithMarkers?> = _pendingImportRoute.asStateFlow()

    fun selectedLocationFor(entryId: String): LocationDescription? =
        _selectedLocations.value[entryId]

    fun offlineMapsTargetFor(entryId: String): LngLatAlt? =
        _offlineMapsTargets.value[entryId]

    /**
     * Navigate to [route] and seed the new back-stack entry with [location] so
     * the destination composable reads it by its own entry id.
     */
    fun navigateWithLocation(
        navController: NavHostController,
        route: String,
        location: LocationDescription,
    ) {
        navController.navigate(route)
        val newEntry = navController.getBackStackEntry(route)
        _selectedLocations.update { it + (newEntry.id to location) }
    }

    /**
     * Replace the location currently stored for [entry] without navigating —
     * used when a destination wants to update its own seed (e.g. before a
     * re-navigate to itself).
     */
    fun replaceLocation(entry: NavBackStackEntry, location: LocationDescription) {
        _selectedLocations.update { it + (entry.id to location) }
    }

    fun navigateWithOfflineMapsTarget(
        navController: NavHostController,
        route: String,
        target: LngLatAlt?,
    ) {
        navController.navigate(route)
        val newEntry = navController.getBackStackEntry(route)
        if (target != null) {
            _offlineMapsTargets.update { it + (newEntry.id to target) }
        }
    }

    fun setPendingImportRoute(route: RouteWithMarkers?) {
        _pendingImportRoute.value = route
    }

    /**
     * Drop entries that aren't on the back stack any more. Call from a
     * `NavController.OnDestinationChangedListener` (or similar) so the maps
     * don't grow unboundedly.
     */
    fun prune(liveEntryIds: Set<String>) {
        _selectedLocations.update { current -> current.filterKeys { it in liveEntryIds } }
        _offlineMapsTargets.update { current -> current.filterKeys { it in liveEntryIds } }
    }
}
