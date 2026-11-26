package org.scottishtecharmy.soundscape.utils

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import org.scottishtecharmy.soundscape.logging.AppLog
import java.time.Instant

/**
 * Logs how the app's previous processes ended, at the start of each new one.
 *
 * The support log is an in-memory buffer (SupportLogBuffer), so when the process dies everything
 * that led up to it is lost - and a process killed by the OS for memory, by an OEM battery
 * manager, or by an ANR or a crash is exactly what a "Soundscape stopped talking" report needs to
 * know about. Android keeps those exit records itself, so the next process can put them in its
 * own log for Contact Support to send.
 */
object ProcessExitLogger {
    private val logger = AppLog.tag("ProcessExit")

    private const val MAX_EXITS = 5

    fun logPreviousExits(context: Context) {
        val exits = describePreviousExits(context) ?: return
        if (exits.isEmpty()) {
            logger.i { "No previous process exits recorded" }
            return
        }
        logger.i { "Previous process exits, newest first:" }
        for (exit in exits) {
            logger.i { "  $exit" }
        }
    }

    /**
     * The last few process exits, newest first, one line each - also written into the Contact
     * Support report, since the lines logged at startup may have scrolled out of the buffer by
     * the time the user sends it. Null if Android wouldn't say.
     */
    fun describePreviousExits(context: Context): List<String>? {
        val exits = try {
            val activityManager = context.getSystemService(ActivityManager::class.java)
            activityManager.getHistoricalProcessExitReasons(context.packageName, 0, MAX_EXITS)
        } catch (e: Exception) {
            logger.w(e) { "Couldn't read the previous process exits" }
            return null
        }
        return exits.map { exit ->
            "${Instant.ofEpochMilli(exit.timestamp)} ${reasonName(exit.reason)}" +
                " while ${importanceName(exit.importance)}" +
                (exit.description?.takeIf { it.isNotBlank() }?.let { ": $it" } ?: "") +
                " (status ${exit.status}, pss ${exit.pss / 1024}MB)"
        }
    }

    // Literal values for the constants added after our minSdk (30), so no API check is needed to
    // name them.
    private fun reasonName(reason: Int): String = when (reason) {
        ApplicationExitInfo.REASON_UNKNOWN -> "UNKNOWN"
        ApplicationExitInfo.REASON_EXIT_SELF -> "EXIT_SELF"
        ApplicationExitInfo.REASON_SIGNALED -> "SIGNALED"
        ApplicationExitInfo.REASON_LOW_MEMORY -> "LOW_MEMORY"
        ApplicationExitInfo.REASON_CRASH -> "CRASH"
        ApplicationExitInfo.REASON_CRASH_NATIVE -> "CRASH_NATIVE"
        ApplicationExitInfo.REASON_ANR -> "ANR"
        ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "INITIALIZATION_FAILURE"
        ApplicationExitInfo.REASON_PERMISSION_CHANGE -> "PERMISSION_CHANGE"
        ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "EXCESSIVE_RESOURCE_USAGE"
        ApplicationExitInfo.REASON_USER_REQUESTED -> "USER_REQUESTED"
        ApplicationExitInfo.REASON_USER_STOPPED -> "USER_STOPPED"
        ApplicationExitInfo.REASON_DEPENDENCY_DIED -> "DEPENDENCY_DIED"
        ApplicationExitInfo.REASON_OTHER -> "OTHER"
        14 -> "FREEZER"
        15 -> "PACKAGE_STATE_CHANGE"
        16 -> "PACKAGE_UPDATED"
        else -> "reason $reason"
    }

    private fun importanceName(importance: Int): String = when (importance) {
        ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND -> "in the foreground"
        ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE ->
            "running a foreground service"
        ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE -> "visible"
        ActivityManager.RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE -> "perceptible"
        ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE -> "running a service"
        ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED -> "cached in the background"
        ActivityManager.RunningAppProcessInfo.IMPORTANCE_GONE -> "gone"
        else -> "importance $importance"
    }
}
