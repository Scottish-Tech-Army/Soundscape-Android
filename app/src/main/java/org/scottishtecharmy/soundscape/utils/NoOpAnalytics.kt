package org.scottishtecharmy.soundscape.utils

class NoOpAnalytics : Analytics {
    override fun logEvent(name: String, params: Map<String, Any?>?) {
    }
    override fun logCostlyEvent(name: String, params: Map<String, Any?>?) {
    }

    override fun crashSetCustomKey(key: String, value: String) {
    }
    override fun crashLogNotes(name: String) {
    }
}
