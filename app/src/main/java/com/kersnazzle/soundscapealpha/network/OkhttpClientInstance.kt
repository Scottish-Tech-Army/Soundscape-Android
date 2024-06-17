package com.kersnazzle.soundscapealpha.network

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
import java.util.concurrent.TimeUnit

// TODO I want to get to the Cache class which is described in this article:
//  https://medium.com/@malikshahbaz213/fech-cache-api-data-in-android-kotlin-using-retrofit-91f83f36cde3
object OkhttpClientInstance {
    // trying to find a way to get around passing a context in the constructor
    // as not allowed constructor with object class.
    // Need context for the cacheDir and ConnectivityManager
    private lateinit var application: Application

    fun init(application: Application){
        this.application = application
    }

    private val cacheSize = (5 * 1024 * 1024).toLong() //5MB cache size
    private val myCache = Cache(application.applicationContext.cacheDir, cacheSize)

    val okHttpClient = OkHttpClient.Builder()
        .cache(myCache)
        .addInterceptor { chain ->

        // Get the request from the chain.
        var request = chain.request()

        request = if (hasNetwork()){
            /*
            *  If there is Internet, get the cache that was stored 1 day ago.
            *  If the cache is older than 1 day, then discard it,
            *  and indicate an error in fetching the response.
            *  The 'max-age' attribute is responsible for this behavior.
            */
            val onlineCacheControl = CacheControl.Builder()
                .maxAge(1, TimeUnit.DAYS)
                .build()
            // request.newBuilder().header("Cache-Control", "public, max-age=" + 60 * 60 * 24).build()
            request.newBuilder()
                .header("Cache-Control", onlineCacheControl.toString())
                .removeHeader("Pragma")
                .build()
        } else {
            /*
            *  If there is no Internet, get the cache that was stored 7 days ago.
            *  If the cache is older than 7 days, then discard it,
            *  and indicate an error in fetching the response.
            *  The 'max-stale' attribute is responsible for this behavior.
            *  The 'only-if-cached' attribute indicates to not retrieve new data; fetch the cache only instead.
            */
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
        .build()




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
            else -> false
        }
    }

}


