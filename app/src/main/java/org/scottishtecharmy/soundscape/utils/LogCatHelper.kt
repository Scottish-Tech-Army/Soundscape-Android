package org.scottishtecharmy.soundscape.utils

import android.content.Context
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.lang.Thread.sleep
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * A helper object to save the application's Logcat output to a file.
 */
object LogcatHelper {

    private const val LOG_TAG = "LogcatHelper"

    // Regex to parse a line from `logcat -v threadtime`
    private val logcatPattern = Pattern.compile(
        "(\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}.\\d{3})\\s*\\d([0-9]*)\\s*\\d([0-9]*)\\s*([FEWIDV]) ([^:]*):\\s*(.*)"
    )
    enum class GroupId( val id: Int)
    {
        Datetime(1),
        Pid(2),
        Tid(3),
        Priority(4),
        Tag(5),
        Message(6)
    }

    /**
     * Parse a line from `logcat -v threadtime` and build a JSON object for it.
     */
    private fun buildLogJsonObject(matcher: java.util.regex.Matcher, currentYear: Int, formatter: SimpleDateFormat, startTime: Long): JSONObject? {
        val level = when (matcher.group(GroupId.Priority.id)) {
            "F" -> "FATAL"
            "E" -> "ERROR"
            "W" -> "WARN"
            "I" -> "INFO"
            "D" -> "DEBUG"
            "V" -> "VERBOSE"
            else -> "UNKNOWN"
        }

        val logcatTimestampStr = matcher.group(GroupId.Datetime.id)
        val fullTimestampStr = "$currentYear-$logcatTimestampStr"
        val timestampPair = try {
            val date = formatter.parse(fullTimestampStr)
            val timeMillis = date?.time ?: 0L
            // Pair of seconds and nanoseconds
            Pair(timeMillis / 1000, (timeMillis % 1000) * 1_000_000)
        } catch (e: Exception) {
            Pair(0L, 0L)
        }

        if(timestampPair.first < startTime) {
            return null
        }

        val timestampObject = JSONObject().apply {
            put("seconds", timestampPair.first)
            put("nanoseconds", timestampPair.second)
        }

        val headerObject = JSONObject().apply {
            put("logLevel", level)
            put("pid", matcher.group(GroupId.Pid.id)?.toIntOrNull() ?: 0)
            put("tid", matcher.group(GroupId.Tid.id)?.toIntOrNull() ?: 0)
            put("applicationId", "org.scottishtecharmy.soundscape")
            put("processName", "soundscape")
            put("tag", matcher.group(GroupId.Tag.id)?.trim())
            put("timestamp", timestampObject)
        }

        return JSONObject().apply {
            put("header", headerObject)
            put("message", matcher.group(GroupId.Message.id)?.trim())
        }
    }

    /**
     * Builds the metadata JSON object which describes the phone.
     */
    private fun buildMetadataObject(): JSONObject {
        val apiLevel = JSONObject().apply {
            put("majorVersion", SDK_INT)
            put("minorVersion", 0)
        }

        val physicalDeviceObject = JSONObject().apply {
            put("serialNumber", "xxxxxxx") // We don't have permissions for the
                                                          // serial number, and we don't need it
            put("isOnline", false)
            put("release", Build.VERSION.RELEASE)
            put("apiLevel", apiLevel)
            put("featureLevel", SDK_INT)
            put("manufacturer", Build.MANUFACTURER)
            put("model", Build.MODEL)
            put("type", "HANDHELD")
        }

        val projectIds = JSONArray().apply {
            put("org.scottishtecharmy.soundscape")
            put("org.scottishtecharmy.soundscape.test")
        }

        val deviceObject = JSONObject().put("physicalDevice", physicalDeviceObject)

        return JSONObject().apply {
            put("device", deviceObject)
            put("filter", "")  // The output is already filtered, so show everything
            put("projectApplicationIds", projectIds)
        }
    }

    private fun zipLog(writer: ZipOutputStream) {
        // The log file is inside a zip, always call logcat.txt
        val fileName = "logcat.txt"

        val zipEntry = ZipEntry(fileName)
        writer.putNextEntry(zipEntry)

        // Start writing the main JSON object structure
        writer.write("{\n".toByteArray())

        // Write Metadata Object
        val metadataObject = buildMetadataObject()
        writer.write("  \"metadata\": ${metadataObject.toString(2)},\n".toByteArray())

        // Start Logcat Messages Array
        writer.write("  \"logcatMessages\": [\n".toByteArray())

        // Run logcat and parse the last 30 minutes worth of logging
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, -30)
        val startTimeSeconds = calendar.timeInMillis / 1000

// TODO: Ideally we want to output the last 30 minutes of logs, but logcat fails to return anything!
//  Gemini claims that logcat can be quite broken on some devices?! Instead,we limit to the last
//  5000 lines of log.
//        val startTime = calendar.time
//        val argumentDateTime = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)
//        val startTimeArg = argumentDateTime.format(startTime)
//        val logcatCommand = "logcat -d -v threadtime -t \"$startTimeArg\""
//        println("logcatCommand: $logcatCommand")

        val process = Runtime.getRuntime().exec("logcat -d -v threadtime -t 5000")
        val reader = BufferedReader(InputStreamReader(process.inputStream))

        // Create timestamp parser
        val currentYear = calendar.get(Calendar.YEAR)
        val logcatDateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        var isFirstLogEntry = true

        reader.forEachLine { line ->
            val matcher = logcatPattern.matcher(line)
            if (matcher.matches()) {
                // Prepend a comma if this is not the first entry in the array
                if (!isFirstLogEntry) {
                    writer.write(",\n".toByteArray())
                }

                // Build the JSON object for the current log line
                val logObject = buildLogJsonObject(matcher, currentYear, logcatDateFormatter, startTimeSeconds)
                if(logObject != null) {
                    // Write the formatted JSON object string to the file
                    writer.write(logObject.toString(2).toByteArray())
                    isFirstLogEntry = false
                }
            }
            // Non-matching lines (like stack traces) are currently ignored to maintain valid JSON.
        }
        // Close the Logcat Messages Array and Main Object
        writer.write("\n  ]\n".toByteArray())
        writer.write("}\n".toByteArray())

        // Close the zip file entry
        writer.closeEntry()
    }

    /**
     * Saves the application's logcat output to a file in the app's file directory.
     * This function streams the JSON output to avoid high memory usage and zips it as it goes.
     *
     * @param context The application context.
     * @return The absolute path of the saved log file, or null if an error occurred.
     */
    suspend fun saveLogcatToFile(context: Context): String? = withContext(Dispatchers.IO) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "logcat_${timeStamp}.zip"

        // Create an in-memory zip file
        val byteArrayOutputStream = ByteArrayOutputStream()
        val zipOutputStream = ZipOutputStream(byteArrayOutputStream)
        try {
            zipLog(zipOutputStream)
            zipOutputStream.close()

            // Copy the zip out to a file in a folder that can be accessed by getUriForFile
            val storageDir = "${context.filesDir}/export/"
            val directory = File(storageDir)
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val zipFile = File(storageDir,fileName)
            FileOutputStream(zipFile).use { it.write(byteArrayOutputStream.toByteArray()) }
            return@withContext storageDir + fileName
        } catch (e: Exception) { // Catch broader exceptions
            Log.e(LOG_TAG, "Error streaming logcat to file", e)
            zipOutputStream.close()
            return@withContext null
        }
    }
}
