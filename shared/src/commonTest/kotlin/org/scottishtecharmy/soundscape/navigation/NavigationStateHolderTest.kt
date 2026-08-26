package org.scottishtecharmy.soundscape.navigation

import org.scottishtecharmy.soundscape.database.local.model.RouteEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * navigateWithLocation / navigateWithOfflineMapsTarget / replaceLocation all
 * require a real androidx.navigation NavHostController / NavBackStackEntry.
 * Per `javap` on the resolved navigation-runtime artifact, the android-target
 * actual's only public constructor is `NavHostController(android.content.Context)`
 * (compiled from NavHostController.android.kt), and NavBackStackEntry has no
 * public constructor at all - both are normally only obtainable via
 * `rememberNavController()` inside a live composition. There's no Robolectric
 * or Compose UI test harness wired into shared/commonTest, and no mocking
 * library, so those three methods can't be exercised here without adding that
 * infrastructure or modifying production code.
 *
 * This suite covers what IS reachable without a NavHostController: the read
 * accessors on a fresh holder and the pendingImportRoute singleton slot.
 */
class NavigationStateHolderTest {

    @Test
    fun freshHolderHasEmptyState() {
        val holder = NavigationStateHolder()
        assertTrue(holder.selectedLocations.value.isEmpty())
        assertTrue(holder.offlineMapsTargets.value.isEmpty())
        assertNull(holder.pendingImportRoute.value)
    }

    @Test
    fun selectedLocationForReturnsNullForUnknownEntry() {
        val holder = NavigationStateHolder()
        assertNull(holder.selectedLocationFor("unknown"))
    }

    @Test
    fun offlineMapsTargetForReturnsNullForUnknownEntry() {
        val holder = NavigationStateHolder()
        assertNull(holder.offlineMapsTargetFor("unknown"))
    }

    @Test
    fun setPendingImportRouteStoresAndClears() {
        val holder = NavigationStateHolder()
        val route = RouteWithMarkers(RouteEntity(name = "My Route", description = "desc"), emptyList())

        holder.setPendingImportRoute(route)
        assertEquals(route, holder.pendingImportRoute.value)

        holder.setPendingImportRoute(null)
        assertNull(holder.pendingImportRoute.value)
    }

    @Test
    fun pruneOnEmptyMapsIsNoOp() {
        val holder = NavigationStateHolder()
        holder.prune(setOf("a", "b"))
        assertTrue(holder.selectedLocations.value.isEmpty())
        assertTrue(holder.offlineMapsTargets.value.isEmpty())
    }
}
