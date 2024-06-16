package com.kersnazzle.soundscapealpha.network

import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

// TODO The problem with this is that I can't see an obvious way to access caching using Retrofit.
//  Okhttp which Retrofit uses does have a Cache:
//  https://github.com/square/okhttp/blob/ea89504d57c8349db3537e86d940bf1f0c951e9c/okhttp/src/jvmMain/kotlin/okhttp3/internal/cache/DiskLruCache.kt
object RetrofitClientInstance {
    private var retrofit : Retrofit? = null
    private const val BASE_URL = "https://soundscape.scottishtecharmy.org"

    // define a getter property
    val retrofitInstance : Retrofit?
        get() {
            // has this object been created yet?
            if (retrofit == null) {
                // create it
                retrofit = Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    // use it to output the string
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .build()
            }
            return retrofit
        }
}