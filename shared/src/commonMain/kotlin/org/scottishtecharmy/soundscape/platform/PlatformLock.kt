package org.scottishtecharmy.soundscape.platform

/**
 * A reentrant mutual exclusion lock, for state shared between threads. Common Kotlin has no lock
 * of its own, and a coroutine Mutex can't be held by code that mustn't suspend.
 */
expect class PlatformLock() {
    fun lock()
    fun unlock()
}

inline fun <T> PlatformLock.withLock(block: () -> T): T {
    lock()
    try {
        return block()
    } finally {
        unlock()
    }
}
