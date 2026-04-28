package org.scottishtecharmy.soundscape.screens.markers_routes.screens.routedetailsscreen

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider.getUriForFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.MainActivity
import org.scottishtecharmy.soundscape.SoundscapeServiceConnection
import org.scottishtecharmy.soundscape.database.local.dao.RouteDao
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers
import org.scottishtecharmy.soundscape.utils.routeToShareJson
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

class RouteDetailsViewModel(
    private val routeDao: RouteDao,
    soundscapeServiceConnection: SoundscapeServiceConnection,
) : ViewModel() {

    val holder = RouteDetailsStateHolder(routeDao, soundscapeServiceConnection)
    val uiState = holder.uiState

    fun getRouteById(routeId: Long) = holder.getRouteById(routeId)
    fun startRoute(routeId: Long) = holder.startRoute(routeId)
    fun startRouteInReverse(routeId: Long) = holder.startRouteInReverse(routeId)
    fun stopRoute() = holder.stopRoute()
    fun clearErrorMessage() = holder.clearErrorMessage()

    fun shareRoute(context: Context, routeId: Long) {
        viewModelScope.launch {
            val route = routeDao.getRouteWithMarkers(routeId) ?: return@launch
            val shareUri = writeRouteAndReturnUri(context, route) ?: return@launch
            (context as MainActivity).shareRoute(shareUri)
        }
    }

    private fun writeRouteAndReturnUri(context: Context, route: RouteWithMarkers): Uri? {
        val routeStorageDir = File("${context.filesDir}/route/").apply { if (!exists()) mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm").format(Date())
        val safeName = route.route.name.replace(Regex("[/\\\\:*?\"<>|\\x00]"), "_").take(100)
        val outputFile = File(routeStorageDir, "soundscape-route-$safeName-$timeStamp.json")
        outputFile.writeText(routeToShareJson(route))
        return getUriForFile(context, "${context.packageName}.provider", outputFile)
    }

    override fun onCleared() {
        super.onCleared()
        holder.dispose()
    }
}
