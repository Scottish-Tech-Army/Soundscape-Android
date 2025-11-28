package org.scottishtecharmy.soundscape.viewmodels

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.SoundscapeServiceConnection
import org.scottishtecharmy.soundscape.database.local.MarkersAndRoutesDatabase
import org.scottishtecharmy.soundscape.database.local.dao.RouteDao
import org.scottishtecharmy.soundscape.database.local.model.MarkerEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers
import org.scottishtecharmy.soundscape.utils.parseGpxFile
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject

@HiltViewModel
class AdvancedMarkersAndRoutesSettingsViewModel @Inject constructor(
    private val soundscapeServiceConnection : SoundscapeServiceConnection,
    private val routeDao: RouteDao
): ViewModel() {

    // A SharedFlow to send import event to UI
    private val _importEvent = MutableSharedFlow<Unit>()
    val importEvent = _importEvent.asSharedFlow()

    // Feedback Flow to UI
    private val _userFeedback = MutableStateFlow("")
    val userFeedback: StateFlow<String> = _userFeedback

    fun deleteAllMarkersAndRoutes(context: Context, successString: String) {
        viewModelScope.launch {
            val roomDb = MarkersAndRoutesDatabase.getMarkersInstance(context)
            roomDb.clearAllTables()
            _userFeedback.emit(successString)
        }
    }


    private fun generateGpxString(route: RouteWithMarkers) : String {
        val gpxBuilder = StringBuilder()
        gpxBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        gpxBuilder.append("<gpx version=\"1.1\" creator=\"Soundscape\">\n")
        gpxBuilder.append("  <metadata>\n")
        gpxBuilder.append("    <name>${route.route.name}</name>\n")
        gpxBuilder.append("    <desc>${route.route.description}</desc>\n")
        gpxBuilder.append("  </metadata>\n")

        route.markers.forEach { marker ->
            gpxBuilder.append("      <wpt lat=\"${marker.latitude}\" lon=\"${marker.longitude}\">\n")
            gpxBuilder.append("        <name>${marker.name}</name>\n")
            gpxBuilder.append("        <desc>${marker.fullAddress}</desc>\n")
            gpxBuilder.append("      </wpt>\n")
        }
        gpxBuilder.append("</gpx>")

        return gpxBuilder.toString()
    }

    private fun zipGpx(zipOutputStream: ZipOutputStream, route: RouteWithMarkers, usedNames: MutableMap<String, Int>) {
        // Sanitize route name to create a valid filename
        var fileRoot = route.route.name.replace(Regex("[^a-zA-Z0-9.-]"), "_")
        val gpxContent = generateGpxString(route)

        // De-duplicate routes that have the same name
        val currentValue = usedNames[fileRoot]
        if(currentValue == null) {
            usedNames[fileRoot] = 0
        } else {
             usedNames[fileRoot] = currentValue + 1
            fileRoot = "${fileRoot}_${currentValue + 1}"
        }
        val fileName = "${fileRoot}.gpx"

        // 2. Create a new entry in the zip file
        val zipEntry = ZipEntry(fileName)
        zipOutputStream.putNextEntry(zipEntry)

        // 3. Write the GPX file content (as bytes) into the zip entry
        zipOutputStream.write(gpxContent.toByteArray(Charsets.UTF_8))

        // 4. Close the current entry
        zipOutputStream.closeEntry()
    }

    val globalMarkersName = "AllSoundscapeDatabaseMarkersInASingleRoute"
    fun exportMarkersAndRoutes(context: Context, message: String) {
        viewModelScope.launch {
            // Rather than export an opaque database file, the plan is to export a zip file containing
            // a GPX file for each of the routes that exists along with a GPX file which contains
            // all of the markers that are not contained in any routes. These are easy to look at in
            // other tools.

            // Get all the routes from the database
            val routes = routeDao.getAllRoutesWithMarkers()

            // Create an in-memory zip file
            val byteArrayOutputStream = ByteArrayOutputStream()
            val zipOutputStream = ZipOutputStream(byteArrayOutputStream)

            // Write out a GPX file that contains all of the markers so as to capture those
            // which are not within any routes. The order is unimportant as it's not really a GPX,
            // it's just a list of Markers.
            val markers = routeDao.getAllMarkers()
            val allMarkersRoute = RouteWithMarkers(
                route = RouteEntity(0, globalMarkersName, ""),
                markers = markers
            )
            val usedNames = mutableMapOf<String, Int>()
            zipGpx(zipOutputStream, allMarkersRoute, usedNames)

            // Write out each route as a separate GPX file within the Zip file
            for(route in routes) {
                zipGpx(zipOutputStream, route, usedNames)
            }

            zipOutputStream.close()

            try {
                // Write the Zip out to a file in a folder that can be accessed by getUriForFile
                val path = "${context.filesDir}/export/"
                val exportDir = File(path)
                if (!exportDir.exists()) {
                    exportDir.mkdirs()
                }
                // Include a timestamp in the file name
                val timeStampFormatter = SimpleDateFormat("yyyyMMdd_HHmm")
                val dateString = timeStampFormatter.format(Date())
                val zipFile = File(exportDir, "soundscape-routes-export-$dateString.zip")
                FileOutputStream(zipFile).use { it.write(byteArrayOutputStream.toByteArray()) }

                // Get the URI for the file and share it
                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    zipFile
                )
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    type = "application/zip"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, message))

            } catch (e: Exception) {
                Log.e(TAG, "Failed to create or share zip file", e)
            }
        }
    }

    fun triggerImport(context: Context) {
        viewModelScope.launch {
            _importEvent.emit(Unit)
        }
    }

    fun importMarkersAndRoutes(context: Context, uri: Uri, successString: String, failureString: String) {
        viewModelScope.launch {
            try {
                // The first thing we do is add all of the markers from the All Markers route.
                // Once they are in the database we can use insertRouteWithExistingMarkers to add
                // the other routes, confident that the markers are already present.
                var routeCount = 0
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val zipInputStream = ZipInputStream(inputStream)
                    var zipEntry: ZipEntry? = zipInputStream.nextEntry
                    while (zipEntry != null) {
                        if (!zipEntry.isDirectory && zipEntry.name.endsWith(".gpx")) {
                            try {
                                // Read the current zip entry into a byte array because parseGpxFile
                                // requires an InputStream and it closes the stream internally.
                                val entryBytes = zipInputStream.readBytes()
                                val byteArrayInputStream = entryBytes.inputStream()

                                // Parse GPX file
                                val route = parseGpxFile(byteArrayInputStream)
                                if (route != null) {
                                    if(zipEntry.name.contains(globalMarkersName)) {
                                        // This is all of the markers
                                        route.markers.forEach {
                                            val existingMarker = routeDao.getMarkerByLocation(it.longitude, it.latitude)
                                            if(existingMarker == null)
                                                // The marker doesn't exist, so add it as is
                                                routeDao.insertMarker(it)
                                            else {
                                                // The marker already exists - update it with the
                                                // name and fullAddress
                                                val updateData = MarkerEntity(
                                                    markerId = existingMarker.markerId,
                                                    name = it.name,
                                                    fullAddress = it.fullAddress,
                                                    longitude = existingMarker.longitude,
                                                    latitude = existingMarker.latitude
                                                )
                                                routeDao.updateMarker(updateData)
                                            }
                                        }
                                        if(route.markers.isNotEmpty())
                                            routeCount += 1
                                    } else {
                                        val routeName = route.route.name
                                        println("Import $routeName")
                                        val newRoute = RouteEntity(
                                            name = routeName,
                                            description = route.route.description
                                        )
                                        routeDao.insertRouteWithNewMarkers(
                                            newRoute,
                                            route.markers
                                        )
                                        if(route.markers.isNotEmpty())
                                            routeCount += 1
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Completed parsing.", e)
                            }
                        }
                        zipEntry = zipInputStream.nextEntry
                    }
                }
                Log.d(TAG, "GPX processing finished - routeCount $routeCount")
                _userFeedback.emit(if(routeCount > 0) successString else failureString)

            } catch (e: Exception) {
                Log.e(TAG, "Failed to import file.", e)
                _userFeedback.emit(failureString)
            }
        }
    }

    fun userFeedbackShown() {
        _userFeedback.value = ""
    }

    init {
        viewModelScope.launch {
            soundscapeServiceConnection.serviceBoundState.collect {
                Log.d(TAG, "serviceBoundState $it")
            }
        }
    }

    companion object {
        private const val TAG = "LocationDetailsViewModel"
    }
}
