package org.scottishtecharmy.soundscape.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

fun createAndroidVectorTileClient(
    baseUrl: String,
    cacheDir: File,
    userAgent: String,
    hasNetwork: () -> Boolean,
    cacheSizeBytes: Long = 100L * 1024 * 1024,
): VectorTileClient {
    val okHttpClient = OkHttpClient.Builder()
        .cache(Cache(cacheDir, cacheSizeBytes))
        .addInterceptor(userAgentInterceptor(userAgent))
        .addInterceptor(cacheControlInterceptor(hasNetwork))
        .build()

    val httpClient = HttpClient(OkHttp) {
        engine { preconfigured = okHttpClient }
        expectSuccess = false
    }
    return VectorTileClient(httpClient, baseUrl)
}

private fun userAgentInterceptor(userAgent: String) = Interceptor { chain ->
    chain.proceed(
        chain.request().newBuilder()
            .header("User-Agent", userAgent)
            .build()
    )
}

fun createAndroidManifestClient(
    baseUrl: String,
    userAgent: String,
): ManifestClient {
    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(userAgentInterceptor(userAgent))
        .build()
    val httpClient = HttpClient(OkHttp) {
        engine { preconfigured = okHttpClient }
        expectSuccess = false
    }
    return ManifestClient(httpClient, baseUrl)
}

fun createAndroidPhotonSearchClient(
    baseUrl: String,
    userAgent: String,
    callTimeoutSeconds: Long = 5,
): PhotonSearchClient {
    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(userAgentInterceptor(userAgent))
        .callTimeout(callTimeoutSeconds, TimeUnit.SECONDS)
        .build()
    val httpClient = HttpClient(OkHttp) {
        engine { preconfigured = okHttpClient }
        expectSuccess = false
    }
    return PhotonSearchClient(httpClient, baseUrl)
}


private fun cacheControlInterceptor(hasNetwork: () -> Boolean) = Interceptor { chain ->
    val request = chain.request()
    val modified = if (hasNetwork()) {
        val control = CacheControl.Builder().maxAge(1, TimeUnit.DAYS).build()
        request.newBuilder()
            .header("Cache-Control", control.toString())
            .removeHeader("Pragma")
            .build()
    } else {
        val control = CacheControl.Builder().onlyIfCached().maxStale(7, TimeUnit.DAYS).build()
        request.newBuilder()
            .header("Cache-Control", control.toString())
            .removeHeader("Pragma")
            .build()
    }
    chain.proceed(modified)
}
