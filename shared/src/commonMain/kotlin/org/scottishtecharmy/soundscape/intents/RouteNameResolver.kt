package org.scottishtecharmy.soundscape.intents

import org.scottishtecharmy.soundscape.database.local.dao.RouteDao
import org.scottishtecharmy.soundscape.database.local.model.MarkerEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteEntity
import org.scottishtecharmy.soundscape.utils.fuzzyCompare

private const val FUZZY_MATCH_THRESHOLD = 0.3

/**
 * Best fuzzy match for [name] among the receiver, or null when no candidate beats
 * [FUZZY_MATCH_THRESHOLD]. [nameOf] pulls the comparable name out of each candidate.
 */
private fun <T> List<T>.bestFuzzyMatch(name: String, nameOf: (T) -> String): T? {
    if (name.isBlank()) return null
    return map { it to name.fuzzyCompare(nameOf(it), true) }
        .filter { it.second < FUZZY_MATCH_THRESHOLD }
        .minByOrNull { it.second }
        ?.first
}

/**
 * List-based matchers, for callers which already hold the candidates — the action
 * executor reads the full list anyway so it can tell "you have no routes saved"
 * apart from "no route matched", and shouldn't query the DAO twice to do it.
 */
fun List<RouteEntity>.bestRouteMatch(name: String): RouteEntity? =
    bestFuzzyMatch(name) { it.name }

fun List<MarkerEntity>.bestMarkerMatch(name: String): MarkerEntity? =
    bestFuzzyMatch(name) { it.name }

/**
 * Fuzzy-matches the supplied [name] against all saved routes and returns the
 * best match's routeId, or null when no candidate beats [FUZZY_MATCH_THRESHOLD].
 *
 * Mirrors the existing Android implementation in SoundscapeIntents so both
 * platforms resolve `soundscape://route/{name}` consistently.
 */
suspend fun resolveRouteByName(routeDao: RouteDao, name: String): Long? {
    if (name.isBlank()) return null
    return routeDao.getAllRoutes().bestRouteMatch(name)?.routeId
}

/**
 * Marker equivalent of [resolveRouteByName], returning the whole entity because
 * callers need its coordinates as well as its identity.
 */
suspend fun resolveMarkerByName(routeDao: RouteDao, name: String): MarkerEntity? {
    if (name.isBlank()) return null
    return routeDao.getAllMarkers().bestMarkerMatch(name)
}
