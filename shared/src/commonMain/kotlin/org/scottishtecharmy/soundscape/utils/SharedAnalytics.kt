package org.scottishtecharmy.soundscape.utils

import kotlin.concurrent.Volatile

/**
 * The app's [Analytics] for shared code that has no other route to it - ViewModels, the nav
 * graph, OfflineMapManager, the audio tour and so on. Each platform installs its instance here
 * when it creates it: Android's AnalyticsProvider and iOS's IosSoundscapeService. Until then
 * (and in unit tests) events go nowhere.
 */
object SharedAnalytics {
    @Volatile
    var instance: Analytics = NoOp

    fun logEvent(name: String, params: Map<String, Any?>? = null) = instance.logEvent(name, params)

    private object NoOp : Analytics {
        override fun logEvent(name: String, params: Map<String, Any?>?) {}
        override fun logCostlyEvent(name: String, params: Map<String, Any?>?) {}
        override fun crashSetCustomKey(key: String, value: String) {}
        override fun crashLogNotes(name: String) {}
    }
}
