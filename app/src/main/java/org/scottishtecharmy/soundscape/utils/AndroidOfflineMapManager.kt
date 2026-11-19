package org.scottishtecharmy.soundscape.utils

import android.content.Context
import android.os.Environment
import androidx.preference.PreferenceManager
import org.scottishtecharmy.soundscape.BuildConfig
import org.scottishtecharmy.soundscape.MainActivity
import org.scottishtecharmy.soundscape.SoundscapeServiceConnection
import org.scottishtecharmy.soundscape.network.OfflineMapManager
import org.scottishtecharmy.soundscape.network.UserAgentInterceptor
import org.scottishtecharmy.soundscape.network.createAndroidFileDownloader
import org.scottishtecharmy.soundscape.network.createAndroidManifestClient
import java.io.File

/**
 * The shared [OfflineMapManager], with Android's HTTP client and extract directory. The
 * directory is read from the preferences on every use, as the user can move the extracts to
 * other storage while the app is running.
 */
fun createAndroidOfflineMapManager(
    appContext: Context,
    soundscapeServiceConnection: SoundscapeServiceConnection,
): OfflineMapManager = OfflineMapManager(
    manifestClient = createAndroidManifestClient(
        baseUrl = BuildConfig.EXTRACT_PROVIDER_URL,
        userAgent = UserAgentInterceptor.USER_AGENT,
    ),
    fileDownloader = createAndroidFileDownloader(userAgent = UserAgentInterceptor.USER_AGENT),
    extractBasePathProvider = { extractsDir(appContext).path },
    extractBaseUrl = BuildConfig.EXTRACT_PROVIDER_URL,
    onExtractsChanged = { soundscapeServiceConnection.refreshOfflineMaps() },
)

private fun extractsDir(appContext: Context): File {
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext)
    val path = sharedPreferences.getString(
        MainActivity.SELECTED_STORAGE_KEY,
        MainActivity.SELECTED_STORAGE_DEFAULT,
    ) ?: MainActivity.SELECTED_STORAGE_DEFAULT
    return File(path, Environment.DIRECTORY_DOWNLOADS)
}
