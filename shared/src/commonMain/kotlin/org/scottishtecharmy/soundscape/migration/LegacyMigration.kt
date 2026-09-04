package org.scottishtecharmy.soundscape.migration

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.scottishtecharmy.soundscape.database.local.dao.RouteDao
import org.scottishtecharmy.soundscape.database.local.model.MarkerEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteMarkerCrossRef
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

/**
 * Imports legacy markers and routes (read by the platform layer from the
 * legacy Realm database) into the new app's Room database.
 *
 * The platform layer encodes its findings as JSON of the form:
 *
 *   {
 *     "markers": [
 *       {"legacyId": "...uuid...", "name": "...", "latitude": 0.0,
 *        "longitude": 0.0, "fullAddress": "...", "entityKey": "ft-443758688"}
 *     ],
 *     "routes": [
 *       {"name": "...", "description": "...",
 *        "waypointLegacyIds": ["uuid1", "uuid2", ...]}
 *     ]
 *   }
 *
 * `name` is the legacy marker's nickname and is often empty: the legacy app
 * only stored a name for a marker the user typed one for, and looked
 * everything else up from `entityKey` against tile data each time it drew
 * the list. So when there's no nickname, [resolver] is asked to name the
 * marker from the feature that key refers to - see [TileLegacyMarkerNamer].
 * Failing that we fall back to the estimated address the legacy app
 * geocoded when the marker was created, and finally to "Unnamed".
 *
 * Inserts every marker, mapping its legacy UUID to the freshly generated
 * `marker_id` in a local map, then inserts each route and connects its
 * waypoints to the new marker ids in their original order. Routes whose
 * waypoints can't all be resolved are skipped rather than persisted in a
 * broken state.
 *
 * Returns the number of markers + routes successfully imported, or -1 on
 * parse failure. The platform caller uses a non-negative return as the
 * cue to delete the legacy artefacts.
 */
suspend fun importLegacyPayload(
    payloadJson: String,
    dao: RouteDao,
    resolver: LegacyMarkerNameResolver? = null,
    onProgress: ((done: Int, total: Int) -> Unit)? = null,
): LegacyImportResult {
    val root = try {
        Json.parseToJsonElement(payloadJson).jsonObject
    } catch (t: Throwable) {
        return LegacyImportResult.Unreadable
    }

    val markers = (root["markers"] as? JsonArray) ?: JsonArray(emptyList())
    val routes = (root["routes"] as? JsonArray) ?: JsonArray(emptyList())

    // Markers can take a second or two each to name, so the screen driving this needs to know
    // how much there is in total before the first one lands.
    val total = markers.size + routes.size
    onProgress?.invoke(0, total)

    // Naming comes first, and in full, before anything is written. It is the only part that can
    // fail for a reason worth retrying - a marker the legacy app never named can only be named
    // from map data - and a half-imported database is not something we can pick up from later:
    // re-running would duplicate whatever did land. So if the map data isn't there, nothing is
    // written at all and the user is asked to try again once they're back online.
    val named = mutableListOf<NamedLegacyMarker>()
    for ((index, element) in markers.withIndex()) {
        val marker = (element as? JsonObject)?.let(::readLegacyMarker) ?: continue

        val name = when (val result = nameForMarker(marker, resolver)) {
            is LegacyMarkerNameResult.Named -> result.name
            LegacyMarkerNameResult.NoTileData -> return LegacyImportResult.NeedsMapData
            LegacyMarkerNameResult.NotFound -> marker.fullAddress.ifBlank { UNNAMED }
        }

        named.add(NamedLegacyMarker(marker, name))
        onProgress?.invoke(index + 1, total)
    }

    val legacyToNewMarkerId = mutableMapOf<String, Long>()
    var imported = 0

    for ((marker, name) in named) {
        val newId = dao.insertMarker(
            MarkerEntity(
                name = name,
                longitude = marker.longitude,
                latitude = marker.latitude,
                fullAddress = marker.fullAddress,
            ),
        )
        legacyToNewMarkerId[marker.legacyId] = newId
        imported++
    }

    for ((index, element) in routes.withIndex()) {
        onProgress?.invoke(markers.size + index + 1, total)

        val obj = element as? JsonObject ?: continue
        val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: continue
        val description = obj["description"]?.jsonPrimitive?.contentOrNull ?: ""
        val waypointIdsJson = obj["waypointLegacyIds"]?.jsonArray ?: continue

        val waypointIds = waypointIdsJson.mapNotNull { it.jsonPrimitive.contentOrNull }
        val resolvedMarkerIds = waypointIds.mapNotNull { legacyToNewMarkerId[it] }
        if (resolvedMarkerIds.size != waypointIds.size || resolvedMarkerIds.isEmpty()) {
            // At least one waypoint refers to a marker we didn't import,
            // or the route has no resolvable waypoints — skip rather than
            // persist a broken route.
            continue
        }

        val newRouteId = dao.insertRoute(
            RouteEntity(name = name, description = description),
        )
        resolvedMarkerIds.forEachIndexed { order, markerId ->
            dao.addMarkerToRoute(
                RouteMarkerCrossRef(
                    routeId = newRouteId,
                    markerId = markerId,
                    markerOrder = order,
                ),
            )
        }
        imported++
    }

    onProgress?.invoke(total, total)
    return LegacyImportResult.Imported(imported)
}

/** How [importLegacyPayload] finished. */
sealed interface LegacyImportResult {
    /** Everything staged is now in the database. [count] is the markers plus routes written. */
    data class Imported(val count: Int) : LegacyImportResult

    /**
     * Nothing was written, because markers needing a name couldn't be looked up: no network and
     * no offline map covering them. Worth retrying once the user is back online.
     */
    data object NeedsMapData : LegacyImportResult

    /** The staged payload isn't valid JSON. Retrying won't help. */
    data object Unreadable : LegacyImportResult
}

/** A marker as the legacy app stored it, before we work out what to call it. */
private data class LegacyMarker(
    val legacyId: String,
    val nickname: String,
    val latitude: Double,
    val longitude: Double,
    val fullAddress: String,
    val entityKey: String,
)

private data class NamedLegacyMarker(val marker: LegacyMarker, val name: String)

/** Reads one marker out of the payload, or null if it's missing a field we can't do without. */
private fun readLegacyMarker(obj: JsonObject): LegacyMarker? = LegacyMarker(
    legacyId = obj["legacyId"]?.jsonPrimitive?.contentOrNull ?: return null,
    nickname = obj["name"]?.jsonPrimitive?.contentOrNull ?: return null,
    latitude = obj["latitude"]?.jsonPrimitive?.doubleOrNull ?: return null,
    longitude = obj["longitude"]?.jsonPrimitive?.doubleOrNull ?: return null,
    fullAddress = obj["fullAddress"]?.jsonPrimitive?.contentOrNull ?: "",
    entityKey = obj["entityKey"]?.jsonPrimitive?.contentOrNull ?: "",
)

/**
 * Works out what to call an imported marker.
 *
 * The legacy nickname wins whenever there is one - it's what the user typed. Otherwise the marker
 * was created from a POI and named by lookup, so we ask [resolver] to do the same lookup against
 * current tile data. Markers with no `entityKey` were plain coordinates in the legacy app and have
 * no feature to look up, so there is nothing to ask about.
 */
private suspend fun nameForMarker(
    marker: LegacyMarker,
    resolver: LegacyMarkerNameResolver?,
): LegacyMarkerNameResult {
    if (marker.nickname.isNotBlank()) {
        return LegacyMarkerNameResult.Named(marker.nickname, "legacy nickname")
    }

    if (marker.entityKey.isBlank() || resolver == null) {
        return LegacyMarkerNameResult.NotFound
    }

    return try {
        resolver.resolve(marker.entityKey, LngLatAlt(marker.longitude, marker.latitude))
    } catch (t: Throwable) {
        // A lookup that breaks isn't a lookup that couldn't reach the map - treat it as this one
        // marker having no name rather than stopping the whole import on it.
        println("LegacyMigration: naming ${marker.entityKey} failed: $t")
        LegacyMarkerNameResult.NotFound
    }
}

private const val UNNAMED = "Unnamed"
