package org.scottishtecharmy.soundscape.migration

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.scottishtecharmy.soundscape.database.local.dao.RouteDao
import org.scottishtecharmy.soundscape.database.local.model.MarkerEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteMarkerCrossRef
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Exercises [importLegacyPayload] against a hand-rolled in-memory
 * [RouteDao] so the test stays in commonTest with zero platform deps.
 * The fake implements the four methods the importer touches and the few
 * accessors the assertions need; everything else throws so unexpected
 * usage surfaces loudly.
 */
class LegacyMigrationTest {

    private lateinit var dao: FakeRouteDao

    @BeforeTest
    fun setUp() {
        dao = FakeRouteDao()
    }

    @Test
    fun importsAllSavedMarkers() = runTest {
        val json = """
            {
              "markers": [
                {"legacyId": "a", "name": "Pub", "latitude": 55.95, "longitude": -3.19,
                 "fullAddress": "1 Royal Mile"},
                {"legacyId": "b", "name": "Castle", "latitude": 55.948, "longitude": -3.2,
                 "fullAddress": ""}
              ],
              "routes": []
            }
        """.trimIndent()

        val count = importedCount(json, dao)

        assertEquals(2, count)
        val markers = dao.getAllMarkers().sortedBy { it.name }
        assertEquals(listOf("Castle", "Pub"), markers.map { it.name })
        val pub = markers.first { it.name == "Pub" }
        assertEquals(55.95, pub.latitude)
        assertEquals(-3.19, pub.longitude)
        assertEquals("1 Royal Mile", pub.fullAddress)
    }

    @Test
    fun importsRoutesAndPreservesWaypointOrder() = runTest {
        val json = """
            {
              "markers": [
                {"legacyId": "m1", "name": "M1", "latitude": 0.0, "longitude": 0.0},
                {"legacyId": "m2", "name": "M2", "latitude": 0.0, "longitude": 0.0},
                {"legacyId": "m3", "name": "M3", "latitude": 0.0, "longitude": 0.0}
              ],
              "routes": [
                {"name": "Loop", "description": "scenic",
                 "waypointLegacyIds": ["m3", "m1", "m2"]}
              ]
            }
        """.trimIndent()

        val count = importedCount(json, dao)

        // 3 markers + 1 route
        assertEquals(4, count)
        val routes = dao.allRoutesWithMarkers()
        assertEquals(1, routes.size)
        val route = routes.single()
        assertEquals("Loop", route.route.name)
        assertEquals("scenic", route.route.description)
        assertEquals(listOf("M3", "M1", "M2"), route.markers.map { it.name })
    }

    @Test
    fun routesShareMarkersAndKeepIndependentWaypointOrders() = runTest {
        val json = """
            {
              "markers": [
                {"legacyId": "m1", "name": "M1", "latitude": 0.0, "longitude": 0.0},
                {"legacyId": "m2", "name": "M2", "latitude": 0.0, "longitude": 0.0}
              ],
              "routes": [
                {"name": "A", "description": "",
                 "waypointLegacyIds": ["m1", "m2"]},
                {"name": "B", "description": "",
                 "waypointLegacyIds": ["m2", "m1"]}
              ]
            }
        """.trimIndent()

        importLegacyPayload(json, dao)

        // Both markers are inserted exactly once and shared by both routes,
        // each in its own waypoint order.
        assertEquals(2, dao.getAllMarkers().size)
        val routes = dao.allRoutesWithMarkers().sortedBy { it.route.name }
        assertEquals(listOf("M1", "M2"), routes[0].markers.map { it.name })
        assertEquals(listOf("M2", "M1"), routes[1].markers.map { it.name })
    }

    @Test
    fun routeWithUnresolvableWaypointIsSkippedButOthersStillImport() = runTest {
        val json = """
            {
              "markers": [
                {"legacyId": "m1", "name": "M1", "latitude": 0.0, "longitude": 0.0}
              ],
              "routes": [
                {"name": "Broken", "description": "",
                 "waypointLegacyIds": ["m1", "ghost"]},
                {"name": "Fine", "description": "",
                 "waypointLegacyIds": ["m1"]}
              ]
            }
        """.trimIndent()

        val count = importedCount(json, dao)

        // 1 marker + 1 successful route. The "Broken" route is dropped.
        assertEquals(2, count)
        val routes = dao.allRoutesWithMarkers()
        assertEquals(1, routes.size)
        assertEquals("Fine", routes.single().route.name)
    }

    @Test
    fun emptyPayloadIsAcceptedAndImportsNothing() = runTest {
        val count = importedCount("""{"markers":[], "routes":[]}""", dao)
        assertEquals(0, count)
        assertEquals(0, dao.getAllMarkers().size)
        assertEquals(0, dao.routesById.size)
    }

    @Test
    fun missingMarkersOrRoutesArrayDefaultsToEmpty() = runTest {
        val count = importedCount("""{}""", dao)
        assertEquals(0, count)
    }

    @Test
    fun malformedJsonIsReportedAsUnreadable() = runTest {
        assertEquals(LegacyImportResult.Unreadable, importLegacyPayload("not json", dao))
    }

    @Test
    fun markersMissingRequiredFieldsAreSkippedSilently() = runTest {
        val json = """
            {
              "markers": [
                {"legacyId": "m1", "name": "ok", "latitude": 0.0, "longitude": 0.0},
                {"legacyId": "m2", "name": "no lat", "longitude": 0.0},
                {"legacyId": "m3", "latitude": 0.0, "longitude": 0.0}
              ],
              "routes": []
            }
        """.trimIndent()

        val count = importedCount(json, dao)

        // Only the well-formed marker is imported; partial rows are dropped.
        assertEquals(1, count)
        val all = dao.getAllMarkers()
        assertEquals(1, all.size)
        assertNotNull(all.firstOrNull { it.name == "ok" })
    }

    // MARK: naming markers the legacy app never stored a name for

    @Test
    fun markerWithoutNicknameIsNamedByTheResolver() = runTest {
        val resolver = FakeNameResolver("ft-443758688" to "Kilmardinny Loch")
        val json = """
            {
              "markers": [
                {"legacyId": "m1", "name": "", "latitude": 55.93, "longitude": -4.33,
                 "fullAddress": "Near Milngavie Road", "entityKey": "ft-443758688"}
              ],
              "routes": []
            }
        """.trimIndent()

        importLegacyPayload(json, dao, resolver)

        assertEquals(listOf("Kilmardinny Loch"), dao.getAllMarkers().map { it.name })
        // The resolver is asked about the marker's own coordinates, since that's where the
        // feature it refers to should be.
        assertEquals(listOf("ft-443758688" to LngLatAlt(-4.33, 55.93)), resolver.asked)
    }

    @Test
    fun nicknameWinsAndTheResolverIsNotAsked() = runTest {
        val resolver = FakeNameResolver("ft-1" to "Some POI")
        val json = """
            {
              "markers": [
                {"legacyId": "m1", "name": "Home", "latitude": 0.0, "longitude": 0.0,
                 "fullAddress": "", "entityKey": "ft-1"}
              ],
              "routes": []
            }
        """.trimIndent()

        importLegacyPayload(json, dao, resolver)

        assertEquals(listOf("Home"), dao.getAllMarkers().map { it.name })
        assertTrue(resolver.asked.isEmpty())
    }

    @Test
    fun unresolvableEntityKeyFallsBackToTheEstimatedAddress() = runTest {
        val resolver = FakeNameResolver()
        val json = """
            {
              "markers": [
                {"legacyId": "m1", "name": "", "latitude": 0.0, "longitude": 0.0,
                 "fullAddress": "12 Roman Road", "entityKey": "ft-999"}
              ],
              "routes": []
            }
        """.trimIndent()

        importLegacyPayload(json, dao, resolver)

        assertEquals(listOf("12 Roman Road"), dao.getAllMarkers().map { it.name })
    }

    @Test
    fun markerWithNothingToGoOnBecomesUnnamed() = runTest {
        val json = """
            {
              "markers": [
                {"legacyId": "m1", "name": "", "latitude": 0.0, "longitude": 0.0,
                 "fullAddress": "", "entityKey": ""}
              ],
              "routes": []
            }
        """.trimIndent()

        importLegacyPayload(json, dao, FakeNameResolver())

        assertEquals(listOf("Unnamed"), dao.getAllMarkers().map { it.name })
    }

    @Test
    fun markerWithoutAnEntityKeyIsNotLookedUp() = runTest {
        val resolver = FakeNameResolver()
        val json = """
            {
              "markers": [
                {"legacyId": "m1", "name": "", "latitude": 0.0, "longitude": 0.0,
                 "fullAddress": "12 Roman Road"}
              ],
              "routes": []
            }
        """.trimIndent()

        importLegacyPayload(json, dao, resolver)

        assertEquals(listOf("12 Roman Road"), dao.getAllMarkers().map { it.name })
        assertTrue(resolver.asked.isEmpty())
    }

    @Test
    fun aResolverThatThrowsDoesNotFailTheImport() = runTest {
        val json = """
            {
              "markers": [
                {"legacyId": "m1", "name": "", "latitude": 0.0, "longitude": 0.0,
                 "fullAddress": "12 Roman Road", "entityKey": "ft-1"}
              ],
              "routes": []
            }
        """.trimIndent()

        val count = importedCount(
            json,
            dao,
            object : LegacyMarkerNameResolver {
                override suspend fun resolve(
                    entityKey: String,
                    location: LngLatAlt,
                ): LegacyMarkerNameResult = throw IllegalStateException("broken")
            },
        )

        assertEquals(1, count)
        assertEquals(listOf("12 Roman Road"), dao.getAllMarkers().map { it.name })
    }

    // MARK: no map data to name markers with

    @Test
    fun noMapDataImportsNothingAtAll() = runTest {
        val json = """
            {
              "markers": [
                {"legacyId": "m1", "name": "Home", "latitude": 0.0, "longitude": 0.0,
                 "fullAddress": "", "entityKey": ""},
                {"legacyId": "m2", "name": "", "latitude": 55.93, "longitude": -4.33,
                 "fullAddress": "Near Milngavie Road", "entityKey": "ft-443758688"}
              ],
              "routes": [
                {"name": "Loop", "description": "", "waypointLegacyIds": ["m1", "m2"]}
              ]
            }
        """.trimIndent()

        val result = importLegacyPayload(json, dao, NoMapDataResolver)

        assertEquals(LegacyImportResult.NeedsMapData, result)
        // Not even the marker that was already named, nor the route: a half-imported database
        // can't be resumed without duplicating whatever landed, so the retry has to start clean.
        assertEquals(0, dao.getAllMarkers().size)
        assertEquals(0, dao.routesById.size)
    }

    @Test
    fun retryingAfterNoMapDataImportsEverythingExactlyOnce() = runTest {
        val json = """
            {
              "markers": [
                {"legacyId": "m1", "name": "", "latitude": 55.93, "longitude": -4.33,
                 "fullAddress": "Near Milngavie Road", "entityKey": "ft-443758688"}
              ],
              "routes": []
            }
        """.trimIndent()

        assertEquals(LegacyImportResult.NeedsMapData, importLegacyPayload(json, dao, NoMapDataResolver))

        val connected = FakeNameResolver("ft-443758688" to "Kilmardinny Loch")
        assertEquals(1, importedCount(json, dao, connected))
        assertEquals(listOf("Kilmardinny Loch"), dao.getAllMarkers().map { it.name })
    }

    @Test
    fun markersThatNeedNoLookupImportWithNoMapDataAtAll() = runTest {
        // Nothing asks the resolver anything, so being offline is neither here nor there.
        val json = """
            {
              "markers": [
                {"legacyId": "m1", "name": "Home", "latitude": 0.0, "longitude": 0.0,
                 "fullAddress": "", "entityKey": ""},
                {"legacyId": "m2", "name": "", "latitude": 0.0, "longitude": 0.0,
                 "fullAddress": "12 Roman Road", "entityKey": ""}
              ],
              "routes": []
            }
        """.trimIndent()

        assertEquals(2, importedCount(json, dao, NoMapDataResolver))
        assertEquals(listOf("12 Roman Road", "Home"), dao.getAllMarkers().map { it.name }.sorted())
    }

    @Test
    fun aKeyThatIsSimplyNotInTheMapDataFallsBackInsteadOfStopping() = runTest {
        // NotFound is the map data answering "no such thing", which no retry will change - unlike
        // NoTileData, which is not having asked at all.
        val json = """
            {
              "markers": [
                {"legacyId": "m1", "name": "", "latitude": 0.0, "longitude": 0.0,
                 "fullAddress": "12 Roman Road", "entityKey": "ft-1"}
              ],
              "routes": []
            }
        """.trimIndent()

        assertEquals(1, importedCount(json, dao, FakeNameResolver()))
        assertEquals(listOf("12 Roman Road"), dao.getAllMarkers().map { it.name })
    }

    // MARK: legacy entity key -> planetiler feature id

    @Test
    fun aLegacyKeyBecomesItsNodeAndWayCandidates() {
        // planetiler encodes an OSM object as id * 10 + type (1 node, 2 way). The legacy key
        // doesn't record which the object was, so both are tried.
        assertEquals(listOf(4437586881L, 4437586882L), protomapsIdsForLegacyEntityKey("ft-443758688"))
    }

    @Test
    fun keysThatArentOsmIdsHaveNoCandidates() {
        // Legacy Address and generic-location keys are UUIDs, with nothing in a tile to match.
        assertEquals(emptyList(), protomapsIdsForLegacyEntityKey("6E7B9A00-0000-0000-0000-000000000000"))
        assertEquals(emptyList(), protomapsIdsForLegacyEntityKey("ft-notanumber"))
        assertEquals(emptyList(), protomapsIdsForLegacyEntityKey(""))
    }

    @Test
    fun theDashIsPartOfThePrefix() {
        // "ft" alone isn't the prefix: without the dash the remainder parses as a negative id
        // and every candidate would be wrong.
        assertEquals(emptyList(), protomapsIdsForLegacyEntityKey("ft443758688"))
    }

    @Test
    fun routeWithEmptyWaypointListIsSkipped() = runTest {
        val json = """
            {
              "markers": [
                {"legacyId": "m1", "name": "M1", "latitude": 0.0, "longitude": 0.0}
              ],
              "routes": [
                {"name": "Empty", "description": "", "waypointLegacyIds": []}
              ]
            }
        """.trimIndent()

        val count = importedCount(json, dao)

        assertEquals(1, count) // marker only
        assertEquals(0, dao.routesById.size)
    }
}

/**
 * Runs the import and asserts it succeeded, returning the count. Most tests are about what lands
 * in the database rather than how the import reports itself finishing.
 */
private suspend fun importedCount(
    payloadJson: String,
    dao: RouteDao,
    resolver: LegacyMarkerNameResolver? = null,
): Int = assertIs<LegacyImportResult.Imported>(
    importLegacyPayload(payloadJson, dao, resolver),
).count

/** A resolver for a device with no connection and no offline map. */
private object NoMapDataResolver : LegacyMarkerNameResolver {
    override suspend fun resolve(entityKey: String, location: LngLatAlt) =
        LegacyMarkerNameResult.NoTileData
}

/**
 * Stands in for the tile lookup. Answers from a fixed map of entity key to name and records
 * what it was asked, so tests can assert that the importer only reaches for it when there's no
 * nickname to use.
 */
private class FakeNameResolver(
    private vararg val names: Pair<String, String>,
) : LegacyMarkerNameResolver {
    val asked = mutableListOf<Pair<String, LngLatAlt>>()

    override suspend fun resolve(
        entityKey: String,
        location: LngLatAlt,
    ): LegacyMarkerNameResult {
        asked.add(entityKey to location)
        val name = names.firstOrNull { it.first == entityKey }?.second
            ?: return LegacyMarkerNameResult.NotFound
        return LegacyMarkerNameResult.Named(name, "fake")
    }
}

/**
 * Minimal in-memory [RouteDao] for the import tests. Only the methods the
 * importer (and the test assertions) actually use are implemented; the
 * rest throw to flag unexpected usage early.
 */
private class FakeRouteDao : RouteDao {

    private var nextMarkerId: Long = 1
    private var nextRouteId: Long = 1
    private val markersById = mutableMapOf<Long, MarkerEntity>()
    val routesById = mutableMapOf<Long, RouteEntity>()
    private val crossRefs = mutableListOf<RouteMarkerCrossRef>()

    override suspend fun insertMarker(marker: MarkerEntity): Long {
        val id = nextMarkerId++
        // MarkerEntity is immutable on its primary key, so re-create with
        // the assigned id rather than mutating in place.
        markersById[id] = MarkerEntity(
            markerId = id,
            name = marker.name,
            longitude = marker.longitude,
            latitude = marker.latitude,
            fullAddress = marker.fullAddress,
        )
        return id
    }

    override suspend fun insertRoute(route: RouteEntity): Long {
        val id = nextRouteId++
        routesById[id] = RouteEntity(
            routeId = id,
            name = route.name,
            description = route.description,
        )
        return id
    }

    override suspend fun addMarkerToRoute(crossRef: RouteMarkerCrossRef) {
        crossRefs.add(crossRef)
    }

    override suspend fun getAllMarkers(): List<MarkerEntity> = markersById.values.toList()

    /** Convenience for assertions; not part of the production DAO surface. */
    fun allRoutesWithMarkers(): List<RouteWithMarkers> = routesById.values.map { route ->
        val orderedMarkers = crossRefs
            .filter { it.routeId == route.routeId }
            .sortedBy { it.markerOrder ?: 0 }
            .mapNotNull { markersById[it.markerId] }
        RouteWithMarkers(route, orderedMarkers)
    }

    // --- unused ---------------------------------------------------------
    private fun nope(): Nothing = error("FakeRouteDao: not implemented for tests")

    override suspend fun updateMarker(marker: MarkerEntity) = nope()
    override suspend fun getMarkerById(markerId: Long): MarkerEntity? = nope()
    override suspend fun getMarkerByLocation(longitude: Double, latitude: Double): MarkerEntity? =
        nope()

    override fun getAllMarkersFlow(): Flow<List<MarkerEntity>> = flowOf(emptyList())
    override suspend fun removeMarkerFromRoute(routeId: Long, markerId: Long) = nope()
    override suspend fun removeMarkersForRoute(routeId: Long) = nope()
    override suspend fun getAllRoutes(): List<RouteEntity> = routesById.values.toList()
    override suspend fun getRouteById(routeId: Long): RouteEntity? = routesById[routeId]
    override suspend fun getMarkerCrossReference(routeId: Long): List<RouteMarkerCrossRef> = nope()
    override fun getAllRoutesFlow(): Flow<List<RouteEntity>> = flowOf(emptyList())
    override suspend fun removeRoute(routeId: Long) = nope()
    override suspend fun removeMarker(markerId: Long) = nope()
    override suspend fun deleteAllRouteMarkerCrossRefs() = nope()
    override suspend fun deleteAllMarkers() = nope()
    override suspend fun deleteAllRoutes() = nope()
}
