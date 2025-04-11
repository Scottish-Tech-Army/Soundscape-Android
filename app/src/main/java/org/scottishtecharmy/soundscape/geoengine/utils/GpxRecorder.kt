package org.scottishtecharmy.soundscape.geoengine.utils

import android.content.Context
import android.location.Location
import android.net.Uri
import androidx.core.content.FileProvider.getUriForFile
import java.io.File
import java.io.FileOutputStream


class GpxRecorder(context: Context) {

    private var travelFile: File? = null

    init {
        val path = "${context.filesDir}/recordings/"
        val recordingsStorageDir = File(path)
        if (!recordingsStorageDir.exists()) {
            recordingsStorageDir.mkdirs()
        }
        travelFile = File(recordingsStorageDir, "travel.gpx")
        writeGpxHeader()
    }

    fun close() {
        writeGpxFooter()
    }

    fun getShareUri(context: Context) : Uri? {
        return if(travelFile != null) {
            getUriForFile(context, "org.scottishtecharmy.fileprovider", travelFile!!)
        }
        else
            null
    }

    private fun writeGpxHeader() {
        // Write header, erasing previous content by setting append to false
        FileOutputStream(travelFile, false).use { outputStream ->
            outputStream.write(
                ("<?xml version='1.0' encoding='utf-8'?>\n" +
                "<gpx xmlns=\"http://www.topografix.com/GPX/1/0\" version=\"1.0\" creator=\"Soundscape\">\n" +
                "<trk>\n" +
                "<name>Track 0</name>\n" +
                "<number>0</number>\n" +
                "<trkseg>\n").toByteArray()
            )
        }
    }

    private fun writeGpxFooter() {
        FileOutputStream(travelFile, true).use { outputStream ->
            outputStream.write(
                ("</trkseg>\n" +
                "</trk>\n" +
                "</gpx>").toByteArray()
            )
        }
    }

    fun write(location: Location) {
        val xmlString =
            "<trkpt lat=\"${location.latitude}\" lon=\"${location.longitude}\">\n" +
            "<ele>${location.altitude}></ele>\n" +
            "<accuracy>${location.accuracy}</accuracy>\n" +
            "<speed>${location.speed}</speed>\n" +
            "<bearing>${location.bearing}</bearing>\n" +
            "<bearingAccuracyDegrees>${location.bearingAccuracyDegrees}</bearingAccuracyDegrees>\n" +
            "<time>${location.time}</time>\n" +
            "</trkpt>\n"
        FileOutputStream(travelFile, true).use { outputStream ->
            outputStream.write(xmlString.toByteArray())
        }
    }
}