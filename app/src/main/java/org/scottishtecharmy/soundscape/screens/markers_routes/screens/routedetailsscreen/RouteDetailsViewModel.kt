package org.scottishtecharmy.soundscape.screens.markers_routes.screens.routedetailsscreen

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider.getUriForFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.MainActivity
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.SoundscapeServiceConnection
import org.scottishtecharmy.soundscape.database.local.dao.RouteDao
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class RouteDetailsViewModel @Inject constructor(
    private val routeDao: RouteDao,
    private val soundscapeServiceConnection: SoundscapeServiceConnection
) : ViewModel() {
    private val _uiState = MutableStateFlow(RouteDetailsUiState())
    val uiState: StateFlow<RouteDetailsUiState> = _uiState.asStateFlow()

    fun getRouteById(routeId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val route = routeDao.getRouteWithMarkers(routeId)
                _uiState.value = _uiState.value.copy(
                    route = route,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = soundscapeServiceConnection.soundscapeService?.localizedContext?.getString(
                        R.string.error_message_route_not_found) ?: "",
                    isLoading = false
                )
            }
        }
    }

    fun startRoute(routeId: Long) {
        soundscapeServiceConnection.routeStart(routeId)
    }

    fun stopRoute() {
        soundscapeServiceConnection.routeStop()
    }

//
// Example JSON for shared route:
//
//    {
//        "name": "Test Route",
//        "id": "F41302B9-A91A-4D8F-8958-6C052E334076",
//        "routeDescription": "",
//        "waypoints": [
//        {
//            "marker": {
//                "nickname": "Home",
//                "location": {
//                    "name": "Home",
//                    "coordinate": {
//                        "latitude": 55.9553596,
//                        "longitude": -3.2031666
//                    }
//                },
//                "estimatedAddress": "28A Heriot Row, Edinburgh, Scotland, EH3 6EN",
//                "id": "489A82CE-EF43-46F8-85FB-8D79F88763F6",
//                "lastUpdatedDate": 759964150.733105
//            },
//            "index": 0,
//            "markerId": "489A82CE-EF43-46F8-85FB-8D79F88763F6"
//        },
//        ]
//    }
//
    private fun generateRouteJson(route: RouteWithMarkers?, outputFile: File) {

        val outputStream = FileOutputStream(outputFile, false)
        outputStream.write(
            (
                "{\n" +
                "\t\"name\": \"${route?.route?.name ?: ""}\",\n" +
                "\t\"id\": \"${route?.route?.routeId ?: 0L}\",\n" +
                "\t\"routeDescription\": \"${route?.route?.description ?: ""}\",\n" +
                "\t\"waypoints\": [\n"
            ).toByteArray()
        )

        for ((index, marker) in (route?.markers ?: emptyList()).withIndex()) {
            if (index > 0) {
                outputStream.write(",\n".toByteArray())
            }
            outputStream.write(
                (
                    "\t\t{\n" +
                    "\t\t\t\"marker\": {\n" +
                    "\t\t\t\t\"nickname\": \"${marker.name}\",\n" +
                    "\t\t\t\t\"location\": {\n" +
                    "\t\t\t\t\t\"name\": \"${marker.name}\",\n" +
                    "\t\t\t\t\t\"coordinate\": {\n" +
                    "\t\t\t\t\t\t\"latitude\": ${marker.latitude},\n" +
                    "\t\t\t\t\t\t\"longitude\": ${marker.longitude}\n" +
                    "\t\t\t\t\t}\n" +
                    "\t\t\t\t},\n" +
                    "\t\t\t\t\"estimatedAddress\": \"${marker.fullAddress}\",\n" +
                    "\t\t\t\t\"id\": \"${marker.markerId}\"\n" +
                    "\t\t\t},\n" +
                    "\t\t\t\"index\": $index,\n" +
                    "\t\t\t\"markerId\": \"${marker.markerId}\"\n" +
                    "\t\t}"
                ).toByteArray()
            )
        }
        outputStream.write(
            (
                "\n\t]\n" +
                "}\n"
            ).toByteArray()
        )
    }

    private fun writeRouteAndReturnUri(context: Context, route: RouteWithMarkers?) : Uri? {

        if(route == null) return null

        // Write the route to a file and share it
        val path = "${context.filesDir}/route/"
        val routeStorageDir = File(path)
        if (!routeStorageDir.exists()) {
            routeStorageDir.mkdirs()
        }

        // Include a timestamp in the file name
        val timeStampFormatter = SimpleDateFormat("yyyyMMdd_HHmm")
        val dateString = timeStampFormatter.format(Date())
        // Sanitize route name for use in filename - remove characters invalid in filenames
        val routeName = route.route.name
            .replace(Regex("[/\\\\:*?\"<>|\\x00]"), "_")
            .take(100) // Limit length to avoid overly long filenames
        val outputFile = File(routeStorageDir, "soundscape-route-$routeName-$dateString.json")
        generateRouteJson(route, outputFile)

        return getUriForFile(context, "${context.packageName}.provider", outputFile)
    }


    fun shareRoute(context: Context, routeId: Long) {
        // Get the route from the database
        val route = routeDao.getRouteWithMarkers(routeId)
        if(route != null) {
            // Write the route to a file, and then call the MainActivity shareRoute function with a
            // shared URI to the file.
            val shareUri = writeRouteAndReturnUri(context, route)
            (context as MainActivity).shareRoute(shareUri)
        }
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
