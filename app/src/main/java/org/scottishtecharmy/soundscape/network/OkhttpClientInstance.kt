package org.scottishtecharmy.soundscape.network

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

// TODO I want to get to the Cache class which is described in these articles:
//  https://medium.com/@malikshahbaz213/fech-cache-api-data-in-android-kotlin-using-retrofit-91f83f36cde3
// https://stackoverflow.com/questions/70711512/context-getapplicationcontext-on-a-null-object-when-using-okhttp-cache
//https://proandroiddev.com/increase-performance-of-your-app-by-caching-api-calls-using-okhttp-1384a621c51f
// https://stackoverflow.com/questions/23429046/can-retrofit-with-okhttp-use-cache-data-when-offline?noredirect=1&lq=1
class OkhttpClientInstance(val application: Application) {

    private var retrofit : Retrofit? = null

    private val cacheSize = (5 * 1024 * 1024).toLong() //5MB cache size
    private val myCache = Cache(application.applicationContext.cacheDir, cacheSize)

    private val okHttpClient = OkHttpClient.Builder()
        .cache(myCache)
        .addInterceptor { chain ->

        // Get the request from the chain.
        var request = chain.request()

        request = if (hasNetwork()){
            val onlineCacheControl = CacheControl.Builder()
                .maxAge(1, TimeUnit.DAYS)
                .build()
            // request.newBuilder().header("Cache-Control", "public, max-age=" + 60 * 60 * 24).build()
            request.newBuilder()
                .header("Cache-Control", onlineCacheControl.toString())
                .removeHeader("Pragma")
                .build()
        } else {
            val offlineCacheControl = CacheControl.Builder()
                .onlyIfCached()
                .maxStale(7, TimeUnit.DAYS)
                .build()
            request.newBuilder()
                //.header("Cache-Control", "public, only-if-cached, max-stale=" + 60 * 60 * 24 * 7).build()
                .header("Cache-Control", offlineCacheControl.toString())
                .removeHeader("Pragma")
                .build()
        }

        // Add the modified request to the chain.
        chain.proceed(request)
    }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    val retrofitInstance : Retrofit?
        get() {
            // has this object been created yet?
            if (retrofit == null) {
                // create it
                retrofit = Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    // use it to output the string
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .client(okHttpClient)
                    .build()
            }
            return retrofit
        }

    private fun hasNetwork(): Boolean {
        //val connectivityManager =
        //    context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val connectivityManager =
            application.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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

    companion object {
        private const val BASE_URL = "https://soundscape.scottishtecharmy.org"
    }

}


