package org.scottishtecharmy.soundscape.geoengine

/**
 * Platform-specific assertion that the current thread is the dispatcher created by
 * `newSingleThreadContext("TreeContext")`. Used by [GridState] to catch misuse of its
 * rtree fields outside that single-threaded context.
 */
internal expect fun assertRunningInTreeContext()
