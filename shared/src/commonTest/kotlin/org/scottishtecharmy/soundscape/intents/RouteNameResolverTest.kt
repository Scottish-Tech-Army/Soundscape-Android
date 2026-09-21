package org.scottishtecharmy.soundscape.intents

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import org.scottishtecharmy.soundscape.database.local.dao.RouteDao
import org.scottishtecharmy.soundscape.database.local.model.MarkerEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteMarkerCrossRef
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Minimal [RouteDao] fake: only [getAllRoutes] is exercised by [resolveRouteByName]. */
private class FakeRouteDao(
    private val routes: List<RouteEntity>,
    private val failIfCalled: Boolean = false,
) : RouteDao {
    var getAllRoutesCallCount = 0
        private set

    override suspend fun getAllRoutes(): List<RouteEntity> {
        if (failIfCalled) {
            throw AssertionError("getAllRoutes should not be called for a blank name")
        }
        getAllRoutesCallCount++
        return routes
    }

    private fun unused(): Nothing = throw NotImplementedError("not used by resolveRouteByName")

    override suspend fun insertMarker(marker: MarkerEntity): Long = unused()
    override suspend fun updateMarker(marker: MarkerEntity): Unit = unused()
    override suspend fun getMarkerById(markerId: Long): MarkerEntity? = unused()
    override suspend fun getMarkerByLocation(longitude: Double, latitude: Double): MarkerEntity? =
        unused()

    override suspend fun getAllMarkers(): List<MarkerEntity> = unused()
    override fun getAllMarkersFlow(): Flow<List<MarkerEntity>> = unused()
    override suspend fun insertRoute(route: RouteEntity): Long = unused()
    override suspend fun addMarkerToRoute(crossRef: RouteMarkerCrossRef): Unit = unused()
    override suspend fun removeMarkerFromRoute(routeId: Long, markerId: Long): Unit = unused()
    override suspend fun removeMarkersForRoute(routeId: Long): Unit = unused()
    override suspend fun getRouteById(routeId: Long): RouteEntity? = unused()
    override suspend fun getMarkerCrossReference(routeId: Long): List<RouteMarkerCrossRef> =
        unused()

    override fun getAllRoutesFlow(): Flow<List<RouteEntity>> = unused()
    override suspend fun removeRoute(routeId: Long): Unit = unused()
    override suspend fun removeMarker(markerId: Long): Unit = unused()
    override suspend fun deleteAllRouteMarkerCrossRefs(): Unit = unused()
    override suspend fun deleteAllMarkers(): Unit = unused()
    override suspend fun deleteAllRoutes(): Unit = unused()
}

private fun route(id: Long, name: String) = RouteEntity(routeId = id, name = name, description = "")

class RouteNameResolverTest {

    @Test
    fun blankName_returnsNullWithoutQueryingDao() = runTest {
        val dao = FakeRouteDao(emptyList(), failIfCalled = true)
        assertNull(resolveRouteByName(dao, ""))
        assertNull(resolveRouteByName(dao, "   "))
    }

    @Test
    fun noRoutes_returnsNull() = runTest {
        val dao = FakeRouteDao(emptyList())
        assertNull(resolveRouteByName(dao, "Tesco"))
    }

    @Test
    fun exactMatch_returnsThatRoutesId() = runTest {
        val dao = FakeRouteDao(
            listOf(route(1, "Riverside Walk"), route(2, "Tesco Express")),
        )
        assertEquals(2L, resolveRouteByName(dao, "Tesco Express"))
    }

    @Test
    fun prefixMatch_needleShorterThanRouteName_matches() = runTest {
        // fuzzyCompare is called with needleCanBeShorter = true, so a short spoken name should
        // match a longer saved route name that starts with it.
        val dao = FakeRouteDao(listOf(route(7, "Tesco Express")))
        assertEquals(7L, resolveRouteByName(dao, "Tesco"))
    }

    @Test
    fun pickBestOfMultipleCandidates() = runTest {
        val dao = FakeRouteDao(
            listOf(
                route(1, "Tesco Extra"),
                route(2, "Tesco Express"),
            ),
        )
        // Exact match beats the other fuzzy candidate.
        assertEquals(2L, resolveRouteByName(dao, "Tesco Express"))
    }

    @Test
    fun scoreExactlyAtThreshold_isExcluded() = runTest {
        // Both strings are length 10 (no needleCanBeShorter truncation applies), differing in
        // exactly 3 positions -> distance 3 / maxLen 10 = 0.30, which fails the strict `< 0.3`
        // threshold check in resolveRouteByName.
        val dao = FakeRouteDao(listOf(route(1, "XYZDEFGHIJ")))
        assertNull(resolveRouteByName(dao, "ABCDEFGHIJ"))
    }

    @Test
    fun scoreJustBelowThreshold_isIncluded() = runTest {
        // Same setup but only 2 positions differ -> distance 2 / maxLen 10 = 0.20 < 0.3.
        val dao = FakeRouteDao(listOf(route(1, "AYZDEFGHIJ")))
        assertEquals(1L, resolveRouteByName(dao, "ABCDEFGHIJ"))
    }

    @Test
    fun noCandidateBeatsThreshold_returnsNull() = runTest {
        val dao = FakeRouteDao(listOf(route(1, "Completely Unrelated Route Name")))
        assertNull(resolveRouteByName(dao, "Zzyzx"))
    }

    @Test
    fun caseSensitiveMismatch_canFailToMatch() = runTest {
        // fuzzyCompare is case-sensitive, so a differently-cased name can fall outside the
        // threshold even though a human would consider it the same route.
        val dao = FakeRouteDao(listOf(route(1, "AB")))
        assertNull(resolveRouteByName(dao, "ab"))
    }

    @Test
    fun tieBetweenEquallyGoodCandidates_returnsFirstInList() = runTest {
        val dao = FakeRouteDao(
            listOf(
                route(10, "Tesco Express"),
                route(11, "Tesco Extra"),
            ),
        )
        // Both share the "Tesco " prefix with the shorter needle, so both score identically;
        // minByOrNull is documented to return the first element achieving the minimum.
        assertEquals(10L, resolveRouteByName(dao, "Tesco"))
    }
}
