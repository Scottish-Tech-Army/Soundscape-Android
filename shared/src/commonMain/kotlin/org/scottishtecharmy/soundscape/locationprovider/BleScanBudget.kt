package org.scottishtecharmy.soundscape.locationprovider

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.scottishtecharmy.soundscape.platform.currentTimeMillis

/**
 * Keeps the app's BLE scan starts within Android's limit of 5 in any 30 seconds. Past that,
 * Android doesn't fail the scan: it quietly stops delivering results until the window has passed,
 * so a head tracker that keeps dropping out would go unfound for up to 30 seconds while the app
 * believed it was scanning.
 *
 * The limit is per app, so every client shares [shared]: the WitMotion starts two scans per
 * attempt once it knows its sensor's address (see BleImuClient.scanForSensor), and Bose Frames
 * scan too while both are searching. A client calls [awaitStarts] before starting its scans, and
 * waits there - for a time it knows - rather than scan for nothing.
 */
class BleScanBudget(
    private val maxStarts: Int = 5,
    // A second beyond Android's 30, as its clock and ours don't tick together
    private val windowMillis: Long = 31_000L,
    private val clock: () -> Long = ::currentTimeMillis,
) {
    private val mutex = Mutex()
    private val starts = ArrayDeque<Long>()

    /** Waits until [count] scans can start within the limit, then counts them as started. */
    suspend fun awaitStarts(count: Int) {
        require(count in 1..maxStarts)
        // Held while waiting, so that clients take their turns in order
        mutex.withLock {
            while (true) {
                val now = clock()
                while (starts.isNotEmpty() && now - starts.first() >= windowMillis) starts.removeFirst()
                if (starts.size + count <= maxStarts) {
                    repeat(count) { starts.addLast(now) }
                    return
                }
                delay(starts.first() + windowMillis - now)
            }
        }
    }

    companion object {
        /** The budget every BLE client in the app shares, as Android's limit is per app. */
        val shared = BleScanBudget()
    }
}
