package org.scottishtecharmy.soundscape.intents

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single-shot bus for inbound intents (URLs, file imports, deep links). The
 * MainActivity exposes the flow into AppFlows.pendingIntent; SharedNavHost's
 * dispatcher reads it and clears it via the paired handled callback.
 */
class IntentEventBus {
    private val _pendingIntent = MutableStateFlow<IncomingIntent?>(null)
    val pendingIntent: StateFlow<IncomingIntent?> = _pendingIntent.asStateFlow()

    fun publish(intent: IncomingIntent) {
        _pendingIntent.value = intent
    }

    fun handled() {
        _pendingIntent.value = null
    }
}
