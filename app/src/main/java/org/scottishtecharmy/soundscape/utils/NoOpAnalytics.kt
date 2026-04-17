package org.scottishtecharmy.soundscape.utils

import javax.inject.Inject

class NoOpAnalytics @Inject constructor() : Analytics {
    override fun logEvent(name: String, params: Map<String, Any?>?) {
    }
    override fun logCostlyEvent(name: String, params: Map<String, Any?>?) {
    }

    override fun crashSetCustomKey(key: String, value: String) {
    }
    override fun crashLogNotes(name: String) {
    }
}
