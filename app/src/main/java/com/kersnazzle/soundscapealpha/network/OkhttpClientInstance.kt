package com.kersnazzle.soundscapealpha.network

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

// TODO I want to get to the Cache class which is described in this article:
//  https://medium.com/@malikshahbaz213/fech-cache-api-data-in-android-kotlin-using-retrofit-91f83f36cde3
object OkhttpClientInstance {
    // trying to find a way to get around passing a context in the constructor
    // as not allowed constructor with object class
    private lateinit var application: Application

    fun init(application: Application){
        this.application = application
    }

    val cacheSize = (5 * 1024 * 1024).toLong() //5MB cache size
    val myCache = Cache(application.applicationContext.cacheDir, cacheSize)

    fun hasNetwork(): Boolean? {
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

class MyInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response: Response = chain.proceed(chain.request())
        val cacheControl = CacheControl.Builder()
            .maxAge(10, TimeUnit.DAYS)
            .build()
        return response.newBuilder()
            .header("Cache-Control", cacheControl.toString())
            .build()
    }
}

