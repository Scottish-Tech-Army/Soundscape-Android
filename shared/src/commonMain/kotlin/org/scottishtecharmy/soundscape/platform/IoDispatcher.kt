package org.scottishtecharmy.soundscape.platform

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Dispatcher for offloading blocking I/O (network, disk, tile parsing).
 * On JVM this maps to `Dispatchers.IO`'s elastic thread pool; on Kotlin/Native
 * it falls back to `Dispatchers.Default` since IO dispatcher semantics don't
 * apply there.
 */
expect val ioDispatcher: CoroutineDispatcher
