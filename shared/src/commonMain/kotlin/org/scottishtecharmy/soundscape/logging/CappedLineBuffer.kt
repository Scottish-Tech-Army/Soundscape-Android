package org.scottishtecharmy.soundscape.logging

import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * The most recent lines that fit in [maxChars] characters - SupportLogBuffer's storage.
 *
 * Capped by characters rather than lines so that a log of short lines reaches further back than
 * one full of long ones. Each line counts its length plus one, for the newline it is joined with
 * when the log is saved, so [maxChars] is the size of the saved text. Whole lines are dropped
 * from the front to stay within it; a single line longer than the whole budget (a huge stack
 * trace) is cut short rather than emptying the buffer.
 *
 * Adding a line is constant time on average - each line is added once and dropped once, and
 * nothing is copied - however full the buffer is.
 *
 * Kermit calls log writers from any thread, so access goes through a small spin lock: common
 * Kotlin has no lock of its own, and this one is only ever held for an append or a copy.
 */
@OptIn(ExperimentalAtomicApi::class)
class CappedLineBuffer(private val maxChars: Int) {

    init {
        require(maxChars > 0) { "maxChars must be positive" }
    }

    private val lines = ArrayDeque<String>()
    private var chars = 0L
    private val busy = AtomicBoolean(false)

    private inline fun <T> locked(block: () -> T): T {
        while (!busy.compareAndSet(expectedValue = false, newValue = true)) {
            // Spin: the holder is only appending a line or copying the list
        }
        try {
            return block()
        } finally {
            busy.store(false)
        }
    }

    fun add(line: String) {
        val stored = if (line.length + 1 > maxChars) truncate(line) else line
        locked {
            lines.addLast(stored)
            chars += stored.length + 1
            while (chars > maxChars) {
                chars -= lines.removeFirst().length + 1
            }
        }
    }

    /** The lines currently held, oldest first. */
    fun snapshot(): List<String> = locked { lines.toList() }

    fun clear() {
        locked {
            lines.clear()
            chars = 0L
        }
    }

    private fun truncate(line: String): String {
        val room = maxChars - 1
        return if (room > TRUNCATED.length) {
            line.take(room - TRUNCATED.length) + TRUNCATED
        } else {
            line.take(room)
        }
    }

    private companion object {
        const val TRUNCATED = " [truncated]"
    }
}
