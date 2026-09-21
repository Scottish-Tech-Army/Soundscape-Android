package org.scottishtecharmy.soundscape.geoengine

/**
 * Minimal analytics hook used by [GridState] and its subclasses to record events that have a
 * non-trivial cost (tile loads, offline-map state changes). Kept narrow so the engine code can
 * live in :shared without depending on platform analytics SDKs.
 */
fun interface GridStateAnalytics {
    fun logCostlyEvent(name: String)

    companion object {
        val NoOp = GridStateAnalytics { }
    }
}
