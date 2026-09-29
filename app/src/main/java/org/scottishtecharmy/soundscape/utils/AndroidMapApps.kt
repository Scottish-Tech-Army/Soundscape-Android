package org.scottishtecharmy.soundscape.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.locationDetails.MapApp

private const val TAG = "AndroidMapApps"

private fun geoUri(desc: LocationDescription): Uri {
    val lat = formatCoordinate5(desc.location.latitude)
    val lon = formatCoordinate5(desc.location.longitude)
    // The "(label)" suffix names the pin in Google Maps, OsmAnd, Organic Maps and others; apps
    // which don't understand it still use the coordinates.
    val label = desc.name.takeIf { it.isNotEmpty() }?.let { "(${Uri.encode(it)})" } ?: ""
    return "geo:$lat,$lon?q=$lat,$lon$label".toUri()
}

/**
 * Every installed app which handles geo: links, other than Soundscape itself, sorted by name.
 * Needs the geo <queries> entry in the manifest or package visibility hides them all.
 */
fun getMapApps(context: Context): List<MapApp> {
    val pm = context.packageManager
    val probe = Intent(Intent.ACTION_VIEW, "geo:0,0?q=0,0".toUri())
    return pm.queryIntentActivities(probe, PackageManager.MATCH_DEFAULT_ONLY)
        .map { it.activityInfo }
        .filter { it.packageName != context.packageName }
        .distinctBy { it.packageName }
        .map { MapApp(id = it.packageName, name = it.applicationInfo.loadLabel(pm).toString()) }
        .sortedBy { it.name.lowercase() }
}

fun openInMapApp(context: Context, app: MapApp, desc: LocationDescription) {
    val intent = Intent(Intent.ACTION_VIEW, geoUri(desc)).apply {
        setPackage(app.id)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // Uninstalled between listing and tapping - fall back to letting the system pick.
        Log.w(TAG, "${app.id} can't open geo: links, using the chooser", e)
        intent.setPackage(null)
        context.startActivity(Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
