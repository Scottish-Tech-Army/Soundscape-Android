package org.scottishtecharmy.soundscape.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider.getUriForFile
import org.scottishtecharmy.soundscape.MainActivity
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun goToAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    intent.data = Uri.fromParts("package", "org.scottishtecharmy.soundscape", null)
    context.startActivity(intent)
}

fun shareLocation(context: Context, message: String, locationDescription: LocationDescription) {
    val body = buildShareLocationText(
        desc = locationDescription,
        messageTemplate = message,
        mapsName = "Google Maps",
        mapsUrlBuilder = { lat, lon, _ ->
            "https://www.google.com/maps/?q=$lat,$lon"
        },
    )
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TITLE, locationDescription.name)
        putExtra(Intent.EXTRA_TEXT, body)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, null))
}

fun shareRoute(context: Context, route: RouteWithMarkers) {
    val routeStorageDir = File("${context.filesDir}/route/").apply { if (!exists()) mkdirs() }
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
    val safeName = route.route.name.replace(Regex("[/\\\\:*?\"<>|\\x00]"), "_").take(100)
    val outputFile = File(routeStorageDir, "soundscape-route-$safeName-$timeStamp.json")
    outputFile.writeText(routeToShareJson(route))
    val uri: Uri = getUriForFile(context, "${context.packageName}.provider", outputFile)
    (context as MainActivity).shareRoute(uri)
}
