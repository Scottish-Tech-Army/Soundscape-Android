package org.scottishtecharmy.soundscape.network

import android.app.Application
import org.scottishtecharmy.soundscape.geoengine.utils.TileGrid.Companion.PROTOMAPS_SERVER_BASE
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
        private const val BASE_URL = PROTOMAPS_SERVER_BASE
    }
}
