package org.scottishtecharmy.soundscape.network

import android.app.Application
import retrofit2.Retrofit
import retrofit2.converter.protobuf.ProtoConverterFactory

class ProtomapsTileClient(application: Application) : TileClient(application) {

    override fun buildRetrofit() : Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(ProtoConverterFactory.create())
            .client(okHttpClient)
            .build()
    }

    companion object {
        private const val BASE_URL = "https://d1wzlzgah5gfol.cloudfront.net"
    }
}
