package org.scottishtecharmy.soundscape.geoengine.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider.getUriForFile
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.scottishtecharmy.soundscape.locationprovider.SoundscapeLocation
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone


class GpxRecorder() {

    val bufferMutex = Mutex()
    val maxBufferSize = 3600        // 1 location per second for an hour
    private val buffer: MutableList<SoundscapeLocation> = mutableListOf()

    fun getShareUri(context: Context) : Uri? {

        // Write the current buffer to a file and share it
        val path = "${context.filesDir}/recordings/"
        val recordingsStorageDir = File(path)
        if (!recordingsStorageDir.exists()) {
            recordingsStorageDir.mkdirs()
        }
        val outputFile = File(recordingsStorageDir, "travel.gpx")
        runBlocking {
            generateGpxFile(outputFile)
        }
        return getUriForFile(context, "${context.packageName}.provider", outputFile)
    }

    private suspend fun generateGpxFile(outputFile: File) {
        val outputStream = FileOutputStream(outputFile, false)
        writeGpxHeader(outputStream)
        val timeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        bufferMutex.withLock {
            for (location in buffer) {
                val xmlString =
                    "<trkpt lat=\"${location.latitude}\" lon=\"${location.longitude}\">\n" +
                            "<ele>0.0</ele>\n" +
                            "<accuracy>${location.accuracy}</accuracy>\n" +
                            "<speed>${location.speed}</speed>\n" +
                            "<bearing>${location.bearing}</bearing>\n" +
                            "<bearingAccuracyDegrees>${location.bearingAccuracyDegrees}</bearingAccuracyDegrees>\n" +
                            "<time>${timeFormat.format(Date())}</time>\n" +
                            "</trkpt>\n"
                outputStream.write(xmlString.toByteArray())
            }
        }
        writeGpxFooter(outputStream)
    }

    private fun writeGpxHeader(outputStream: FileOutputStream) {
        outputStream.write(
            ("<?xml version='1.0' encoding='utf-8'?>\n" +
            "<gpx xmlns=\"http://www.topografix.com/GPX/1/0\" version=\"1.0\" creator=\"Soundscape\">\n" +
            "<trk>\n" +
            "<name>Track 0</name>\n" +
            "<number>0</number>\n" +
            "<trkseg>\n").toByteArray()
        )
    }

    private fun writeGpxFooter(outputStream: FileOutputStream) {
        outputStream.write(
            ("</trkseg>\n" +
            "</trk>\n" +
            "</gpx>").toByteArray()
        )
    }

    suspend fun storeLocation(location: SoundscapeLocation) {
        bufferMutex.withLock {
            buffer.add(location)
            if (buffer.size > maxBufferSize)
                buffer.drop(1)
        }
    }
}
