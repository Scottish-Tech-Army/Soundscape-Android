package org.scottishtecharmy.soundscape.screens.home.home

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.google.gson.GsonBuilder
import org.jetbrains.compose.resources.getString
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import org.scottishtecharmy.soundscape.network.DownloadStateCommon
import org.scottishtecharmy.soundscape.preferences.PreferencesProvider
import org.scottishtecharmy.soundscape.resources.Res
import org.scottishtecharmy.soundscape.resources.offline_map_download_cancelled
import org.scottishtecharmy.soundscape.resources.offline_map_download_complete
import org.scottishtecharmy.soundscape.resources.offline_map_download_error
import org.scottishtecharmy.soundscape.screens.home.HomeRoutes
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.offlinemaps.SharedOfflineMapsScreen
import org.scottishtecharmy.soundscape.viewmodels.OfflineMapsViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

fun generateOfflineMapScreenRoute(locationDescription: LocationDescription): String {
    val json = GsonBuilder().create().toJson(locationDescription)
    return "${HomeRoutes.OfflineMaps.route}/${URLEncoder.encode(json, StandardCharsets.UTF_8.toString())}"
}

@Composable
fun OfflineMapsScreenVM(
    navController: NavHostController,
    modifier: Modifier,
    locationDescription: LocationDescription,
) {
    val viewModel: OfflineMapsViewModel = koinViewModel { parametersOf(locationDescription) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()

    val downloading = downloadState is DownloadStateCommon.Downloading ||
        downloadState is DownloadStateCommon.Caching

    val context = LocalContext.current
    val userMessage = remember { mutableStateOf("") }

    LaunchedEffect(downloadState) {
        when (downloadState) {
            is DownloadStateCommon.Success -> {
                userMessage.value = getString(Res.string.offline_map_download_complete)
                viewModel.refreshExtracts()
            }
            is DownloadStateCommon.Error -> {
                userMessage.value = getString(Res.string.offline_map_download_error)
            }
            is DownloadStateCommon.Canceled -> {
                userMessage.value = getString(Res.string.offline_map_download_cancelled)
            }
            else -> Unit
        }
    }

    LaunchedEffect(userMessage.value) {
        if (userMessage.value.isNotEmpty()) {
            Toast.makeText(context, userMessage.value, Toast.LENGTH_LONG).show()
            userMessage.value = ""
        }
    }

    BackHandler(enabled = downloading) {
        // Swallow back-press while a download is in progress; user must tap Cancel in the app bar.
    }

    val preferencesProvider: PreferencesProvider = koinInject()
    SharedOfflineMapsScreen(
        uiState = uiState,
        downloadState = viewModel.downloadState,
        onBack = { navController.navigateUp() },
        onDownload = { name, feature -> viewModel.download(name, feature) },
        onDelete = { feature -> viewModel.delete(feature) },
        onCancelDownload = { viewModel.cancelDownload() },
        preferencesProvider = preferencesProvider,
        modifier = modifier,
    )
}
