package org.scottishtecharmy.soundscape.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.squareup.moshi.Moshi
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeoMoshi
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

interface IManifestDAO {
    @GET("manifest.geojson")
    fun getManifest(): Call<FeatureCollection>
}

class ManifestClient(val applicationContext: Context) {

    private val connectivityManager: ConnectivityManager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var retrofit : Retrofit? = null

    private val cacheSize = (10 * 1024 * 1024).toLong() // 5MB cache size
    private val myCache = Cache(applicationContext.cacheDir, cacheSize)

    private val okHttpClient = OkHttpClient.Builder()
        .cache(myCache)
        .addInterceptor { chain ->
            val onlineCacheControl = CacheControl.Builder()
                .maxAge(1, TimeUnit.DAYS)
                .build()
            val request = chain
                .request()
                .newBuilder()
                .header("Cache-Control", onlineCacheControl.toString())
                .removeHeader("Pragma")
                .build()

            // Add the modified request to the chain.
            chain.proceed(request)
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
