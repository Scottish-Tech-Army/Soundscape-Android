package org.scottishtecharmy.soundscape.logging

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
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
object SupportLogBuffer : LogWriter() {

    // The most recent lines that fit in this many characters - the size of the saved log, as it
    // is almost all ASCII. GeoEngine logs a MapMatch line of about 60 characters for every fix,
    // about one a second, so this is roughly two and a half hours of walking - enough to reach
    // back past the start of most walks when the user sends the log afterwards. Capped by
    // characters rather than lines so that short lines don't cut it short.
    private const val MAX_CHARS = 800_000

    private val lines = CappedLineBuffer(MAX_CHARS)

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
        lines.add(line)
    }

    /** The buffer's current contents, oldest first, joined into one String for attachment. */
    fun dumpAsText(): String = lines.snapshot().joinToString("\n")

    fun clear() {
        lines.clear()
    }
}
