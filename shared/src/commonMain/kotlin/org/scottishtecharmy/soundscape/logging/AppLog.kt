package org.scottishtecharmy.soundscape.logging

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.platformLogWriter

/**
 * Entry point for app logging, shared by commonMain, androidMain, iosMain and :app.
 *
 * Call sites get a tagged logger via [AppLog.tag], mirroring the per-class `TAG` constant
 * convention this codebase already used for android.util.Log. [initialize] wires up two
 * writers: the platform console (Logcat/os_log), gated by [isDebugBuild] since there's
 * otherwise no severity gating at all, and [SupportLogBuffer], which always records
 * everything so a release-build user's Contact Support email still has useful detail.
 */
object AppLog {
    fun tag(tag: String): Logger = Logger.withTag(tag)

    fun initialize(isDebugBuild: Boolean) {
        // The floor stays open; each writer below does its own severity gating.
        Logger.setMinSeverity(Severity.Verbose)
        val consoleMinSeverity = if (isDebugBuild) Severity.Verbose else Severity.Warn
        Logger.setLogWriters(
            SupportLogBuffer,
            SeverityGatedLogWriter(platformLogWriter(), consoleMinSeverity),
        )
    }
}

private class SeverityGatedLogWriter(
    private val delegate: LogWriter,
    private val minSeverity: Severity,
) : LogWriter() {
    override fun isLoggable(tag: String, severity: Severity): Boolean =
        severity >= minSeverity && delegate.isLoggable(tag, severity)

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        delegate.log(severity, message, tag, throwable)
    }
}
