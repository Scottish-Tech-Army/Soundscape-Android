package org.scottishtecharmy.soundscape.network

import android.content.Context
import org.scottishtecharmy.soundscape.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.protobuf.ProtoConverterFactory

class ProtomapsTileClient(applicationContext: Context) : TileClient(applicationContext) {

    override fun buildRetrofit() : Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(ProtoConverterFactory.create())
            .client(okHttpClient)
            .build()
    }

    companion object {
        private const val BASE_URL = BuildConfig.TILE_PROVIDER_URL
    }
}
