package org.scottishtecharmy.soundscape.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.scottishtecharmy.soundscape.logging.AppLog
import org.scottishtecharmy.soundscape.logging.SupportLogBuffer
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A helper object to save the app's own buffered log output (see [SupportLogBuffer]) to a
 * file, for attaching to a Contact Support email.
 */
object LogcatHelper {

    private val logger = AppLog.tag("LogcatHelper")

    /**
     * Writes [SupportLogBuffer]'s current contents to a text file in the app's file directory.
     *
     * @param context The application context.
     * @return The absolute path of the saved log file, or null if an error occurred.
     */
    suspend fun saveLogcatToFile(context: Context): String? = withContext(Dispatchers.IO) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "soundscape_log_${timeStamp}.txt"

        val storageDir = "${context.filesDir}/export/"
        val directory = File(storageDir)
        try {
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val logFile = File(storageDir, fileName)
            logFile.writeText(SupportLogBuffer.dumpAsText())
            return@withContext storageDir + fileName
        } catch (e: Exception) {
            logger.e(e) { "Error writing support log to file" }
            return@withContext null
        }
    }
}
