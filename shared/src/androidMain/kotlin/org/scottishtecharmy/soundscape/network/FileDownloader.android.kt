package org.scottishtecharmy.soundscape.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive
import okhttp3.OkHttpClient
import okhttp3.Protocol
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class DownloadResult {
    object Success : DownloadResult()
    data class HttpError(val code: Int, val message: String) : DownloadResult()
}

class FileDownloader internal constructor(private val httpClient: HttpClient) {
    suspend fun download(
        url: String,
        destFile: File,
        scope: CoroutineScope,
        onProgress: (Int) -> Unit,
    ): DownloadResult {
        return httpClient.prepareGet(url).execute { response ->
            if (!response.status.isSuccess()) {
                return@execute DownloadResult.HttpError(
                    response.status.value,
                    response.status.description,
                )
            }
            val total = response.contentLength() ?: -1L
            val channel = response.bodyAsChannel()
            val buffer = ByteArray(4096)
            var downloaded = 0L
            FileOutputStream(destFile).use { out ->
                while (!channel.isClosedForRead) {
                    scope.ensureActive()
                    val read = channel.readAvailable(buffer)
                    if (read <= 0) break
                    out.write(buffer, 0, read)
                    downloaded += read
                    if (total > 0) {
                        onProgress(((downloaded * 1000) / total).toInt())
                    }
                }
                out.flush()
            }
            // EOF is not the same as "got everything". If the stream ends short of the
            // advertised Content-Length (e.g. an intermediary truncates the response with
            // a clean close), reject it here so the caller retries rather than publishing a
            // truncated file.
            if (total >= 0 && downloaded != total) {
                throw IOException("Truncated download: $downloaded of $total bytes")
            }
            DownloadResult.Success
        }
    }
}

/** Wraps a caller-supplied [HttpClient] - e.g. one built on ktor's MockEngine for tests. */
fun createFileDownloader(httpClient: HttpClient): FileDownloader = FileDownloader(httpClient)

fun createAndroidFileDownloader(
    userAgent: String,
    connectTimeoutMinutes: Long = 3,
    readTimeoutMinutes: Long = 3,
): FileDownloader {
    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", userAgent)
                    .build()
            )
        }
        .connectTimeout(connectTimeoutMinutes, TimeUnit.MINUTES)
        .readTimeout(readTimeoutMinutes, TimeUnit.MINUTES)
        // Pin to HTTP/1.1. OkHttp only enforces Content-Length on HTTP/1.1 (a short
        // body throws "unexpected end of stream"); over HTTP/2 a premature END_STREAM
        // returns a clean EOF, which would let us save a silently truncated extract.
        .protocols(listOf(Protocol.HTTP_1_1))
        .build()
    val httpClient = HttpClient(OkHttp) {
        engine { preconfigured = okHttpClient }
        expectSuccess = false
    }
    return FileDownloader(httpClient)
}
