package org.scottishtecharmy.soundscape.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.squareup.moshi.Moshi
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.GzipSource
import okio.buffer
import org.scottishtecharmy.soundscape.geoengine.MANIFEST_NAME
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeoMoshi
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

interface IManifestDAO {
    @GET(MANIFEST_NAME)
    fun getManifest(): Call<FeatureCollection>
}

class ManifestClient(val applicationContext: Context) {

    private val connectivityManager: ConnectivityManager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var retrofit : Retrofit? = null

    var redirect = ""

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val originalRequest = chain.request()

            val onlineCacheControl = CacheControl.Builder()
                .maxAge(1, TimeUnit.HOURS)
                .build()

            val request = originalRequest
                .newBuilder()
                .header("Cache-Control", onlineCacheControl.toString())
                .removeHeader("Pragma")
                .build()

            val response = chain.proceed(request)

            // Get the request associated with the final response.
            // Its URL will be the one after any redirects.
            val finalRequest = response.request
            val finalUrl = finalRequest.url
            println("Final (after redirect) URL: $finalUrl")
            redirect = finalUrl.toString().removeSuffix(MANIFEST_NAME)

            // 6. Return the response to continue the chain
            response
        }
        .addInterceptor { chain ->
            val response = chain.proceed(chain.request())

            // Check if this is the response we want to force-decompress
            if (response.isSuccessful && response.request.url.toString().endsWith(MANIFEST_NAME)) {
                // Despite what the headers say, we know it's gzipped.
                // Let's decompress it manually.
                val responseBody = response.body
                // Create a GzipSource to read the compressed data from the response body's source
                val gzipSource = GzipSource(responseBody.source())
                // The decompressed data is read from the buffered GzipSource
                val decompressedData = gzipSource.buffer().readUtf8()
                gzipSource.close()
                // Create a new response body with the decompressed string
                val decompressedBody = decompressedData.toResponseBody(responseBody.contentType())

                // Build a new response with the decompressed body
                response.newBuilder()
                    .body(decompressedBody)
                    // Remove the incorrect content-length header of the compressed body
                    .removeHeader("Content-Length")
                    .build()
            } else {
                // Not the response we're looking for, leave it untouched
                response
            }
        }
        .build()

    private var moshi: Moshi? = null
    fun buildRetrofit() : Retrofit {
        moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
        return Retrofit.Builder()
            .baseUrl("https://commcouncil.scot")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi!!))
            .build()
    }

    val retrofitInstance : Retrofit?
        get() {
            // has this object been created yet?
            if (retrofit == null) {
                // create it
                retrofit = buildRetrofit()
            }
            return retrofit
        }

    private fun hasNetwork(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val activeNetworkCapabilities =
            connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return when {
            activeNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            activeNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
            else -> false
        }
    }
}
