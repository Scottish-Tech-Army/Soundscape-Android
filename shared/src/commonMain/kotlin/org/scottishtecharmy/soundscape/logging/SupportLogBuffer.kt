package org.scottishtecharmy.soundscape.logging

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.update
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * An in-memory, capped buffer of the app's own log lines, independent of the platform
 * console (Logcat/os_log). Feeds ContactSupport's log attachment on both platforms, so a
 * user's support email contains exactly what Soundscape itself logged - not the whole
 * system logcat, and not something that requires the READ_LOGS permission to capture.
 *
 * Deliberately in-memory only - lost on process death - rather than file-backed, trading
 * crash-time coverage for simplicity.
 */
@OptIn(ExperimentalAtomicApi::class)
object SupportLogBuffer : LogWriter() {

    // Oldest-recent window of formatted lines, comparable in spirit to the ~30 minute
    // window the previous logcat-based capture used, without needing wall-clock logic.
    private const val MAX_LINES = 2000

    private val lines = AtomicReference<List<String>>(emptyList())

    override fun isLoggable(tag: String, severity: Severity): Boolean = severity >= Severity.Debug

    @OptIn(ExperimentalTime::class)
    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        val timestamp = Clock.System.now()
        val severityLetter = when (severity) {
            Severity.Verbose -> 'V'
            Severity.Debug -> 'D'
            Severity.Info -> 'I'
            Severity.Warn -> 'W'
            Severity.Error -> 'E'
            Severity.Assert -> 'A'
        }
        var line = "$timestamp $severityLetter/$tag: $message"
        if (throwable != null) {
            line += "\n" + throwable.stackTraceToString()
        }
        appendLine(line)
    }

    private fun appendLine(line: String) {
        lines.update { current ->
            (current + line).let {
                if (it.size > MAX_LINES) it.subList(it.size - MAX_LINES, it.size) else it
            }
        }
    }

    /** The buffer's current contents, oldest first, joined into one String for attachment. */
    fun dumpAsText(): String = lines.load().joinToString("\n")

    fun clear() {
        lines.store(emptyList())
    }
}
