package org.scottishtecharmy.soundscape.network

import kotlinx.coroutines.CoroutineScope

/**
 * Cross-platform download result.
 */
sealed class DownloadResultCommon {
    data object Success : DownloadResultCommon()
    data class HttpError(val code: Int, val message: String) : DownloadResultCommon()
}

/**
 * Cross-platform download state for UI observation.
 */
sealed class DownloadStateCommon {
    data object Idle : DownloadStateCommon()
    data object Caching : DownloadStateCommon()
    data class Downloading(val progress: Int) : DownloadStateCommon() // 0-1000 (per mil)
    data object Success : DownloadStateCommon()
    data class Error(val message: String) : DownloadStateCommon()
    data object Canceled : DownloadStateCommon()
}

/**
 * Cross-platform file downloader interface.
 */
interface FileDownloaderInterface {
    suspend fun download(
        url: String,
        destPath: String,
        scope: CoroutineScope,
        onProgress: (Int) -> Unit,
    ): DownloadResultCommon
}
