package org.scottishtecharmy.soundscape.network

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionDownloadDelegateProtocol
import platform.Foundation.NSURLSessionDownloadTask
import platform.Foundation.NSURLSessionTask
import platform.darwin.NSObject
import kotlin.coroutines.resume

/**
 * iOS file downloader backed by NSURLSession's download task. The delegate's
 * `didWriteData` callback fires as each network chunk is received, so the
 * progress bar updates during the download (Ktor's `bodyAsChannel()` on the
 * Darwin engine doesn't reliably stream chunks to the consumer).
 */
@OptIn(ExperimentalForeignApi::class)
class IosFileDownloader : FileDownloaderInterface {

    override suspend fun download(
        url: String,
        destPath: String,
        scope: CoroutineScope,
        onProgress: (Int) -> Unit,
    ): DownloadResultCommon = suspendCancellableCoroutine { continuation ->
        val nsUrl = NSURL.URLWithString(url)
        if (nsUrl == null) {
            continuation.resume(DownloadResultCommon.HttpError(0, "Invalid URL"))
            return@suspendCancellableCoroutine
        }

        val config = NSURLSessionConfiguration.defaultSessionConfiguration
        config.timeoutIntervalForRequest = 300.0
        config.timeoutIntervalForResource = 3600.0

        var resumed = false
        val resumeOnce: (DownloadResultCommon) -> Unit = { result ->
            if (!resumed) {
                resumed = true
                continuation.resume(result)
            }
        }

        // Strong reference to the session so the session->delegate cycle stays alive
        // until we explicitly invalidate it.
        var sessionRef: NSURLSession? = null

        val delegate = DownloadTaskDelegate(
            destPath = destPath,
            onProgress = onProgress,
            onComplete = { result ->
                resumeOnce(result)
                sessionRef?.finishTasksAndInvalidate()
            },
        )

        val session = NSURLSession.sessionWithConfiguration(
            configuration = config,
            delegate = delegate,
            delegateQueue = NSOperationQueue(),
        )
        sessionRef = session

        val request = NSURLRequest.requestWithURL(nsUrl)
        val task = session.downloadTaskWithRequest(request)

        continuation.invokeOnCancellation {
            task.cancel()
            session.invalidateAndCancel()
        }

        task.resume()
    }
}

@OptIn(ExperimentalForeignApi::class)
private class DownloadTaskDelegate(
    private val destPath: String,
    private val onProgress: (Int) -> Unit,
    private val onComplete: (DownloadResultCommon) -> Unit,
) : NSObject(), NSURLSessionDownloadDelegateProtocol {

    private var finished = false

    override fun URLSession(
        session: NSURLSession,
        downloadTask: NSURLSessionDownloadTask,
        didWriteData: Long,
        totalBytesWritten: Long,
        totalBytesExpectedToWrite: Long,
    ) {
        if (totalBytesExpectedToWrite > 0L) {
            val perMil = ((totalBytesWritten * 1000L) / totalBytesExpectedToWrite).toInt()
            onProgress(perMil)
        }
    }

    override fun URLSession(
        session: NSURLSession,
        downloadTask: NSURLSessionDownloadTask,
        didFinishDownloadingToURL: NSURL,
    ) {
        val response = downloadTask.response as? NSHTTPURLResponse
        val statusCode = response?.statusCode?.toInt() ?: 0
        if (statusCode !in 200..299) {
            finished = true
            onComplete(
                DownloadResultCommon.HttpError(
                    statusCode,
                    response?.description ?: "HTTP $statusCode",
                )
            )
            return
        }

        val fileManager = NSFileManager.defaultManager
        val destUrl = NSURL.fileURLWithPath(destPath)
        // Remove any pre-existing file at the destination so move can succeed.
        fileManager.removeItemAtURL(destUrl, error = null)

        val moved = fileManager.moveItemAtURL(
            srcURL = didFinishDownloadingToURL,
            toURL = destUrl,
            error = null,
        )
        finished = true
        if (moved) {
            onComplete(DownloadResultCommon.Success)
        } else {
            onComplete(DownloadResultCommon.HttpError(statusCode, "Failed to save file"))
        }
    }

    override fun URLSession(
        session: NSURLSession,
        task: NSURLSessionTask,
        didCompleteWithError: NSError?,
    ) {
        if (finished) return
        if (didCompleteWithError != null) {
            onComplete(
                DownloadResultCommon.HttpError(
                    0,
                    didCompleteWithError.localizedDescription,
                )
            )
        }
    }
}
