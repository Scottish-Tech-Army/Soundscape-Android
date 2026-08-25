package org.scottishtecharmy.soundscape.audio

import kotlin.coroutines.CoroutineContext
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import platform.darwin.dispatch_async
import platform.darwin.dispatch_queue_t

/**
 * [CoroutineDispatcher] backed by a Grand Central Dispatch queue. Coroutine
 * work is submitted via `dispatch_async`, so a serial queue gives sequential
 * execution and a concurrent queue gives parallel execution — the dispatcher
 * inherits whichever the passed queue was created with.
 */
@OptIn(ExperimentalForeignApi::class)
internal class DispatchQueueDispatcher(
    private val queue: dispatch_queue_t,
) : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        dispatch_async(queue) { block.run() }
    }
}
