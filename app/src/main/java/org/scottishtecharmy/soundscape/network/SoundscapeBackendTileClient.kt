package org.scottishtecharmy.soundscape.network

import android.app.Application
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

class SoundscapeBackendTileClient(application: Application) : TileClient(application) {

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
