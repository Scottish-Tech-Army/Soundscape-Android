package org.scottishtecharmy.soundscape.locationprovider

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** Thrown when a head tracker that streams continuously has stopped sending anything. */
class StreamSilentException(silenceMillis: Long) :
    Exception("No notification for $silenceMillis ms")

/**
 * Collects a head tracker's [notifications] into [onBytes], and throws [StreamSilentException]
 * if none arrives for [silenceMillis].
 *
 * The trackers stream all the time, so silence means the device has been switched off or gone
 * out of range. Without this, the session only ends once Android's BLE supervision timeout gives
 * up on the link, typically 20 seconds later, and the user hears nothing until then.
 */
suspend fun collectUntilSilent(
    notifications: Flow<ByteArray>,
    silenceMillis: Long,
    onBytes: suspend (ByteArray) -> Unit,
) = coroutineScope {
    val received = MutableStateFlow(0L)
    launch {
        // Each notification restarts the wait
        received.collectLatest {
            delay(silenceMillis)
            throw StreamSilentException(silenceMillis)
        }
    }
    notifications.collect { bytes ->
        received.value++
        onBytes(bytes)
    }
}
