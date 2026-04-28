package org.scottishtecharmy.soundscape.intents

import org.scottishtecharmy.soundscape.database.local.dao.RouteDao
import org.scottishtecharmy.soundscape.utils.fuzzyCompare

private const val FUZZY_MATCH_THRESHOLD = 0.3

/**
 * Fuzzy-matches the supplied [name] against all saved routes and returns the
 * best match's routeId, or null when no candidate beats [FUZZY_MATCH_THRESHOLD].
 *
 * Mirrors the existing Android implementation in SoundscapeIntents so both
 * platforms resolve `soundscape://route/{name}` consistently.
 */
suspend fun resolveRouteByName(routeDao: RouteDao, name: String): Long? {
    if (name.isBlank()) return null
    return routeDao.getAllRoutes()
        .map { it to name.fuzzyCompare(it.name, true) }
        .filter { it.second < FUZZY_MATCH_THRESHOLD }
        .minByOrNull { it.second }
        ?.first?.routeId
}
