package org.scottishtecharmy.soundscape.utils

interface Analytics {
    fun logEvent(name: String, params: Map<String, Any?>? = null)
    fun logCostlyEvent(name: String, params: Map<String, Any?>? = null)

    fun crashSetCustomKey(key: String, value: String)
    fun crashLogNotes(name: String)
}
