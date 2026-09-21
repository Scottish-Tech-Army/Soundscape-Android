package org.scottishtecharmy.soundscape.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.DarwinClientEngineConfig
import io.ktor.client.engine.darwin.Darwin
import platform.Foundation.NSURLCache
import platform.Foundation.NSURLRequestReturnCacheDataDontLoad
import platform.Foundation.NSURLRequestUseProtocolCachePolicy

// Timeouts, kept in step with the Android clients in HttpClientFactory.android.kt.
//
// Android gets these from OkHttp's defaults (10s connect, 10s read); NSURLSession's
// equivalent defaults are 60s to make progress and 7 days for the whole resource, which
// is how a request over a link that has gone away - WiFi walked out of range, with no
// cellular behind it - could sit there long after Android would have given up.
//
// timeoutIntervalForRequest is the closest analogue to OkHttp's connect+read pair: it
// bounds the initial connection and then re-arms every time more data arrives.
private const val REQUEST_TIMEOUT_SECONDS = 10.0

// Whole-request ceiling, the analogue of OkHttp's callTimeout. Only the search client
// sets one, because only createAndroidPhotonSearchClient does.
private const val SEARCH_RESOURCE_TIMEOUT_SECONDS = 10.0

/**
 * Applies the timeouts above to a Darwin engine's NSURLSession.
 *
 * waitsForConnectivity is pinned off rather than left to its default. It defaults to
 * false, but if it were ever true a request made with no usable network would wait for
 * connectivity to return - bounded by timeoutIntervalForResource, not by the request
 * timeout set here - which is the exact failure mode these timeouts exist to rule out.
 */
private fun DarwinClientEngineConfig.applySoundscapeTimeouts(
    resourceTimeoutSeconds: Double? = null,
) {
    configureSession {
        setTimeoutIntervalForRequest(REQUEST_TIMEOUT_SECONDS)
        setWaitsForConnectivity(false)
        resourceTimeoutSeconds?.let { setTimeoutIntervalForResource(it) }
    }
}

fun createIosVectorTileClient(
    baseUrl: String,
    hasNetwork: () -> Boolean = { true },
): VectorTileClient {
    val tileCache = NSURLCache(
        memoryCapacity = 10uL * 1024uL * 1024uL,
        diskCapacity = 100uL * 1024uL * 1024uL,
        diskPath = "vector_tiles"
    )
    val httpClient = HttpClient(Darwin) {
        engine {
            applySoundscapeTimeouts()
            configureSession {
                setURLCache(tileCache)
            }
            configureRequest {
                setCachePolicy(
                    if (hasNetwork()) NSURLRequestUseProtocolCachePolicy
                    else NSURLRequestReturnCacheDataDontLoad
                )
            }
        }
        expectSuccess = false
    }
    return VectorTileClient(httpClient, baseUrl)
}

fun createIosPhotonSearchClient(baseUrl: String): PhotonSearchClient {
    val httpClient = HttpClient(Darwin) {
        engine {
            applySoundscapeTimeouts(
                resourceTimeoutSeconds = SEARCH_RESOURCE_TIMEOUT_SECONDS,
            )
        }
        expectSuccess = false
    }
    return PhotonSearchClient(httpClient, baseUrl)
}

/**
 * Mirrors createAndroidManifestClient. Was built inline in IosSoundscapeService; it lives
 * here now so all three iOS clients pick up the same timeouts from one place.
 */
fun createIosManifestClient(baseUrl: String): ManifestClient {
    val httpClient = HttpClient(Darwin) {
        engine {
            applySoundscapeTimeouts()
        }
        expectSuccess = false
    }
    return ManifestClient(httpClient, baseUrl)
}
