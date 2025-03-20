package org.scottishtecharmy.soundscape.network

import android.content.Context
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

class SoundscapeBackendTileClient(applicationContext: Context) : TileClient(applicationContext) {

    override fun buildRetrofit() : Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            // use it to output the string
            .addConverterFactory(ScalarsConverterFactory.create())
            .client(okHttpClient)
            .build()
    }

    companion object {
        private const val BASE_URL = "https://soundscape.scottishtecharmy.org"
    }
}
